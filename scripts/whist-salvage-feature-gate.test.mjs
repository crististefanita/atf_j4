import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {salvageFeatureGate} from './whist-salvage-feature-gate.mjs';

async function fixture(exactLoss=false) {
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'ATF_J4_FEATURE_GATE_'));
    await fs.mkdir(path.join(root,'invocations'));await fs.mkdir(path.join(root,'quality-invocations'));
    const protocol={experiment:'CERTIFIED_PUBLIC_SALVAGE_FEATURE',roles:['expert'],opponents:['A','B'],heldout:{seeds:1}};
    const receipts=[];
    for(const family of protocol.opponents)for(let seat=0;seat<4;seat++){
        const id=family+'-'+seat,record={status:'PASS',parameters:{opponent:family},results:[
            {kind:'before',points:10,winShare:0,byHandSize:{2:{exact:1}}},
            {kind:'after',points:20,winShare:0,byHandSize:{2:{exact:exactLoss&&family==='B'&&seat===0?0:1}}}]};
        const bytes=JSON.stringify(record),file='invocations/'+id+'.json';
        await fs.writeFile(path.join(root,file),bytes);receipts.push({file,sha256:crypto.createHash('sha256').update(bytes).digest('hex')});
    }
    await fs.writeFile(path.join(root,'batch-provenance.json'),JSON.stringify({phase:'heldout',completedPairs:8,independentGameAudits:16,receipts}));
    const proof={status:'PASS',invocationId:'salvage-certified-three-rivals',checkpoints:[{comparisons:[
        {name:'New rival miss floor',actual:3,passed:true},{name:'Baseline rival miss ceiling',actual:2,passed:true}]}]};
    const protocolFile=path.join(root,'protocol.json'),proofFile=path.join(root,'proof.json');
    await fs.writeFile(protocolFile,JSON.stringify(protocol));await fs.writeFile(proofFile,JSON.stringify(proof));
    return {root,protocolFile,proofFile};
}
test('Feature gate permits non-regression without claiming strict general strength gain',async()=>{
    const f=await fixture();const result=await salvageFeatureGate(f.root,f.protocolFile,f.proofFile);
    assert.equal(result.status,'PASS');assert.equal(result.strictGeneralStrengthGainClaim,false);
});
test('A lost exact contract in one family cannot hide behind positive aggregate points',async()=>{
    const f=await fixture(true);const result=await salvageFeatureGate(f.root,f.protocolFile,f.proofFile);
    assert.equal(result.status,'REGRESSION');
    const source=JSON.parse(await fs.readFile(path.join(f.root,'quality-invocations/certified-salvage-feature-gate.json'),'utf8'));
    assert.equal(source.status,'FAIL');assert.ok(source.checkpoints.some(cp=>cp.comparisons.some(c=>!c.passed)));
});
test('A changed source receipt is rejected before any feature verdict is produced',async()=>{
    const f=await fixture();await fs.appendFile(path.join(f.root,'invocations/A-0.json'),' ');
    await assert.rejects(salvageFeatureGate(f.root,f.protocolFile,f.proofFile));
});

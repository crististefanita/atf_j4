import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';

/** Product feature gate, distinct from the unchanged strict strength gate for weighted policies. */
export async function salvageFeatureGate(folder, protocolFile, proofReceipt) {
    const startedAt=new Date().toISOString(), begin=performance.now();
    const protocol=JSON.parse(await fs.readFile(protocolFile,'utf8'));
    assert.equal(protocol.experiment,'CERTIFIED_PUBLIC_SALVAGE_FEATURE');
    const integrity=JSON.parse(await fs.readFile(path.join(folder,'batch-provenance.json'),'utf8'));
    assert.equal(integrity.phase,'heldout');
    const expected=protocol.roles.length*protocol.opponents.length*protocol.heldout.seeds*4;
    assert.equal(integrity.completedPairs,expected);assert.equal(integrity.independentGameAudits,2*expected);
    const proofBytes=await fs.readFile(proofReceipt),proof=JSON.parse(proofBytes);
    assert.equal(proof.status,'PASS');assert.equal(proof.invocationId,'salvage-certified-three-rivals');
    assert.ok(proof.checkpoints.every(cp=>cp.comparisons.every(c=>c.passed)));
    const literalRows=proof.checkpoints.flatMap(cp=>cp.comparisons);
    assert.equal(literalRows.find(r=>r.name==='New rival miss floor').actual,3);
    assert.equal(literalRows.find(r=>r.name==='Baseline rival miss ceiling').actual,2);
    const families=new Map();
    for(const row of integrity.receipts){
        const bytes=await fs.readFile(path.join(folder,row.file));
        assert.equal(crypto.createHash('sha256').update(bytes).digest('hex'),row.sha256);
        const r=JSON.parse(bytes);assert.equal(r.status,'PASS');
        const family=families.get(r.parameters.opponent)??{opponent:r.parameters.opponent,pairs:0,points:0,winShare:0,exact2to8:0};
        const before=r.results.find(x=>x.kind==='before'),after=r.results.find(x=>x.kind==='after');
        family.pairs++;family.points+=after.points-before.points;family.winShare+=after.winShare-before.winShare;
        for(const [size,m]of Object.entries(after.byHandSize))if(+size>=2)family.exact2to8+=m.exact-before.byHandSize[size].exact;
        families.set(family.opponent,family);
    }
    assert.equal(families.size,protocol.opponents.length);
    const checkpoints=[{owner:'salvage/literal-feature',title:'A bounded feature improvement, not a universal strength claim',
        context:{rule:'The actual Hard decision changes7S to7H; rival misses rise2to3 with own penalty-4 in every legal continuation of the uniquely public world.',
            proofReceiptSha256:crypto.createHash('sha256').update(proofBytes).digest('hex'),
            scope:'General stronger play still requires a strict strength experiment. Zero random-trial deltas establish non-regression only.'},
        comparisons:[{name:'Actual independent feature proof passed',expected:'PASS',actual:proof.status,passed:true}]}];
    let accepted=true;
    for(const family of families.values()){
        assert.equal(family.pairs,protocol.heldout.seeds*4);
        const comparisons=[['No own point loss',family.points],['No win-share loss',family.winShare],['No2..8 exact-contract loss',family.exact2to8]]
            .map(([name,value])=>({name,expected:'>=0',actual:value,passed:value>=-1e-9}));
        accepted&&=comparisons.every(c=>c.passed);
        checkpoints.push({owner:'salvage/fresh/'+family.opponent,title:'Fresh paired feature non-regression / '+family.opponent,
            context:{...family,independentSeedClusters:protocol.heldout.seeds,scope:'Seats within a seed share the same deal world and are correlated.'},comparisons});
    }
    const summary={status:accepted?'PASS':'REGRESSION',kind:'FEATURE_CORRECTNESS_AND_NON_REGRESSION',pairs:expected,games:2*expected,
        strictGeneralStrengthGainClaim:false,families:[...families.values()]};
    const capture={schemaVersion:1,runId:path.basename(folder),invocationId:'certified-salvage-feature-gate',
        className:'scripts.whist-salvage-feature-gate',methodName:'publicDominanceAndFreshNonRegression',parameters:{collection:'regression'},
        startedAt,endedAt:new Date().toISOString(),durationNanos:Math.max(1,Math.round((performance.now()-begin)*1e6)),
        status:accepted?'PASS':'FAIL',executionKind:'NODE_COMPONENT_GATE',failure:accepted?'':'Prespecified salvage feature non-regression failed',checkpoints,results:[]};
    await fs.writeFile(path.join(folder,'feature-summary.json'),JSON.stringify(summary,null,2)+'\n',{flag:'wx'});
    await fs.writeFile(path.join(folder,'quality-invocations/certified-salvage-feature-gate.json'),JSON.stringify(capture,null,2)+'\n',{flag:'wx'});
    return summary;
}
if(process.argv[1]?.replaceAll('\\','/').endsWith('/whist-salvage-feature-gate.mjs')){
    try{const [folder,protocol,proof]=process.argv.slice(2);assert.ok(proof,'Usage: whist-salvage-feature-gate.mjs <complete-batch> <feature-protocol.json> <actual-proof-receipt>');
        const result=await salvageFeatureGate(folder,protocol,proof);console.log(JSON.stringify(result,null,2));if(result.status!=='PASS')process.exitCode=2;
    }catch(e){console.error(e.stack);process.exitCode=1;}
}

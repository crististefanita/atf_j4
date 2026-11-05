import test, {after} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
import {createInterface} from 'node:readline';

const probe = fileURLToPath(new URL('./candidate-source-probe.ps1', import.meta.url));
const sha = value => crypto.createHash('sha256').update(value).digest('hex');

let child, sequence=0, terminalError;
const requests=new Map();
function invoke(mode, root, manifest='') {
    if(terminalError)return Promise.reject(terminalError);
    if(!child){
        child=spawn(process.env.WHIST_PWSH??'pwsh',['-NoProfile','-File',probe,'-Server'],{stdio:['pipe','pipe','pipe']});
        let diagnostics='';
        child.stderr.on('data',bytes=>{diagnostics=(diagnostics+bytes).slice(-10000);});
        const rejectAll=error=>{terminalError=error;for(const {reject,timer}of requests.values()){clearTimeout(timer);reject(error);}requests.clear();};
        child.on('error',rejectAll);
        child.stdin.on('error',rejectAll);
        child.on('exit',code=>{if(requests.size)rejectAll(new Error(`Candidate probe exited ${code}: ${diagnostics}`));});
        createInterface({input:child.stdout}).on('line',line=>{
            let reply;try{reply=JSON.parse(line);}catch{return;}
            const pending=requests.get(reply.requestId);if(!pending)return;
            requests.delete(reply.requestId);clearTimeout(pending.timer);
            pending.resolve({exit:reply.exit,...reply.result});
        });
    }
    return new Promise((resolve,reject)=>{
        const requestId=++sequence;
        const timer=setTimeout(()=>{requests.delete(requestId);reject(new Error('Candidate probe exceeded its 60 second deadline'));child.kill();},60000);
        requests.set(requestId,{resolve,reject,timer});
        child.stdin.write(JSON.stringify({requestId,mode,root,manifest})+'\n');
    });
}
after(async()=>{
    if(!child||child.exitCode!==null||terminalError)return;
    await new Promise(resolve=>{child.once('exit',resolve);child.stdin.end();});
});

async function fixture(count, action) {
    const folder = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-candidate-contract-'));
    const root = path.join(folder, 'copie cu spații și diacritice');
    const manifest = path.join(folder, 'candidate.json');
    try {
        for (const tree of ['game-core','ai-lab']) await fs.mkdir(path.join(root,tree,'src/main/java'),{recursive:true});
        for (let i=0;i<count;i++) {
            const file=path.join(root,i%2?'ai-lab':'game-core','src/main/java',`Source${i}.java`);
            await fs.writeFile(file,`class Source${i} {}\n`);
        }
        const baseline=await invoke('baseline',root);
        assert.equal(baseline.exit,0,JSON.stringify(baseline));
        delete baseline.exit;
        const spec={schemaVersion:2,...baseline,files:[]};
        const save=()=>fs.writeFile(manifest,JSON.stringify(spec));
        await save();
        await action({root,manifest,spec,save,folder});
    } finally {
        assert.equal(path.resolve(path.dirname(folder)),path.resolve(os.tmpdir()));
        assert.match(path.basename(folder),/^whist-candidate-contract-/);
        await fs.rm(folder,{recursive:true,force:true});
    }
}

test('Complete source sets are accepted without a historical cardinality',async()=>{
    for(const count of [2,30,37,41])await fixture(count,async({root,manifest,spec})=>{
        const result=await invoke('accepted',root,manifest);
        assert.equal(result.exit,0,JSON.stringify(result));
        assert.equal(result.sourceCount,count);
        assert.equal(result.sha256,spec.baselineInventory.sha256);
    });
});

test('Legacy schema still validates the full current set and every byte',async()=>{
    await fixture(37,async({root,manifest,spec,save})=>{
        spec.schemaVersion=1;delete spec.baselineInventory;await save();
        assert.equal((await invoke('accepted',root,manifest)).exit,0);
        await fs.appendFile(path.join(root,spec.baselineGuards[0].target),'// changed\n');
        assert.notEqual((await invoke('accepted',root,manifest)).exit,0);
    });
});

test('Inventory identity is stable under reordered guards',async()=>{
    await fixture(5,async({root,manifest,spec,save})=>{
        spec.baselineGuards.reverse();await save();
        assert.equal((await invoke('accepted',root,manifest)).exit,0);
    });
});

test('Missing, extra, changed and same-count renamed sources are rejected',async()=>{
    for(const mutation of ['missing','extra','changed','renamed'])await fixture(4,async({root,manifest,spec})=>{
        const target=path.join(root,spec.baselineGuards[0].target);
        if(mutation==='missing')await fs.unlink(target);
        if(mutation==='extra')await fs.writeFile(path.join(root,'game-core/src/main/java/Extra.java'),'class Extra{}');
        if(mutation==='changed')await fs.appendFile(target,'// changed');
        if(mutation==='renamed')await fs.rename(target,target.replace(/\.java$/,'Other.java'));
        assert.notEqual((await invoke('accepted',root,manifest)).exit,0,mutation);
    });
});

test('Duplicate and escaping guard paths are rejected',async()=>{
    for(const mutation of ['duplicate','parent','backslash','absolute'])await fixture(4,async({root,manifest,spec,save})=>{
        if(mutation==='duplicate')spec.baselineGuards.push({...spec.baselineGuards[0]});
        if(mutation==='parent')spec.baselineGuards[0].target='game-core/src/main/java/../Source.java';
        if(mutation==='backslash')spec.baselineGuards[0].target='game-core/src/main/java/sub\\Source.java';
        if(mutation==='absolute')spec.baselineGuards[0].target='C:/outside.java';
        await save();assert.notEqual((await invoke('accepted',root,manifest)).exit,0,mutation);
    });
});

test('Versioned baseline metadata cannot be missing, relabeled or forged',async()=>{
    for(const mutation of ['missing','digest','roots','version'])await fixture(4,async({root,manifest,spec,save})=>{
        if(mutation==='missing')delete spec.baselineInventory;
        if(mutation==='digest')spec.baselineInventory.sha256='0'.repeat(64);
        if(mutation==='roots')spec.baselineInventory.roots=['game-core/src/main/java'];
        if(mutation==='version')spec.schemaVersion=99;
        await save();assert.notEqual((await invoke('accepted',root,manifest)).exit,0,mutation);
    });
});

test('A declared overlay replaces a guarded source and can add a declared main source',async()=>{
    await fixture(4,async({root,manifest,spec,save})=>{
        const replacement='class Replacement {}\n',addition='class Added {}\n';
        const replaced=spec.baselineGuards[0].target,added='game-core/src/main/java/Added.java';
        spec.files=[{target:replaced,sha256:sha(replacement)},{target:added,sha256:sha(addition)}];
        await fs.writeFile(path.join(root,replaced),replacement);await fs.writeFile(path.join(root,added),addition);await save();
        const result=await invoke('staged',root,manifest);
        assert.equal(result.exit,0,JSON.stringify(result));assert.equal(result.sourceCount,5);
        assert.notEqual(result.sha256,spec.baselineInventory.sha256);
        assert.notEqual((await invoke('accepted',root,manifest)).exit,0,'Overlay must never masquerade as its baseline');
    });
});

test('Staged qualification rejects unguarded changes and undeclared additions',async()=>{
    for(const mutation of ['extra','changed','wrong-overlay-hash','duplicate-target'])await fixture(4,async({root,manifest,spec,save})=>{
        const target=spec.baselineGuards[0].target;
        if(mutation==='extra')await fs.writeFile(path.join(root,'ai-lab/src/main/java/Unknown.java'),'class Unknown{}');
        if(mutation==='changed')await fs.appendFile(path.join(root,target),'// unexpected');
        if(mutation==='wrong-overlay-hash')spec.files=[{target,sha256:'0'.repeat(64)}];
        if(mutation==='duplicate-target')spec.files=[{target,sha256:spec.baselineGuards[0].sha256},{target,sha256:spec.baselineGuards[0].sha256}];
        await save();assert.notEqual((await invoke('staged',root,manifest)).exit,0,mutation);
    });
});

test('An empty guard set is rejected rather than treated as a new baseline',async()=>{
    await fixture(2,async({root,manifest,spec,save})=>{
        spec.baselineGuards=[];await save();
        assert.notEqual((await invoke('accepted',root,manifest)).exit,0);
    });
});

test('A linked source ancestor is rejected even when every guarded byte matches',async()=>{
    await fixture(4,async({root,manifest,folder})=>{
        const original=path.join(root,'ai-lab'),physical=path.join(folder,'same bytes outside the app');
        await fs.rename(original,physical);
        await fs.symlink(physical,original,process.platform==='win32'?'junction':'dir');
        const result=await invoke('accepted',root,manifest);
        assert.notEqual(result.exit,0);assert.match(result.error,/linked|physical|links/i);
    });
});

test('Returned metadata cannot mutate the validators source scope',async()=>{
    await fixture(4,async({root})=>{
        const result=await invoke('ownership',root);
        assert.equal(result.exit,0,JSON.stringify(result));
        assert.equal(result.baselineIsolated,true);assert.equal(result.proofIsolated,true);
        assert.equal(result.sourceCount,4);
    });
});

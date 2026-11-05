import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
import {initializeRegistry,reserveEvaluation} from './whist-evaluation-registry.mjs';

const cli = fileURLToPath(new URL('./whist-evaluation-registry.mjs',import.meta.url));
const history = () => ({reviewedBy:'component-fixture',exposures:[]});
const request = (extra={}) => ({candidate:'policy-a',phase:'heldout',firstSeed:100,seeds:10,
    sourceHashes:{'policy.java':'a'.repeat(64)},protocolSha256:'b'.repeat(64),freezeSha256:'c'.repeat(64),
    run:'first-output',corpus:'first-corpus',...extra});
async function fixture(action,initial=history()) {
    const folder = await fs.mkdtemp(path.join(os.tmpdir(),'whist-registry-component-'));
    const root = path.join(folder,'registru comun cu spații și diacritice');
    try { if(initial) await initializeRegistry(root,initial); await action(root,folder); }
    finally {
        assert.equal(path.resolve(path.dirname(folder)),path.resolve(os.tmpdir()));
        assert.match(path.basename(folder),/^whist-registry-component-/);
        await fs.rm(folder,{recursive:true,force:true});
    }
}
function invoke(args) {
    return new Promise((resolve,reject) => {
        const child=spawn(process.execPath,[cli,...args],{stdio:['ignore','pipe','pipe']});
        let output='',error='';
        const timer=setTimeout(()=>{child.kill();reject(Error('Registry subprocess exceeded 15 seconds'));},15000);
        child.stdout.on('data',bytes=>output+=bytes);
        child.stderr.on('data',bytes=>error+=bytes);
        child.on('error',failure=>{clearTimeout(timer);reject(failure);});
        child.on('exit',code=>{clearTimeout(timer);resolve({code,output,error});});
    });
}

test('Registry initialization is explicit and cannot reset an existing history',async()=>{
    await fixture(async root=>{
        await assert.rejects(reserveEvaluation(root,request()),/initialized/);
        await initializeRegistry(root,history());
        await assert.rejects(initializeRegistry(root,history()),/already initialized/);
    },null);
});
test('Exact boundaries, containment and partial overlap use the same seeded-deal namespace',async()=>{
    await fixture(async root=>{
        const first=await reserveEvaluation(root,request());
        assert.equal(first.disposition,'NEW_RESERVATION');
        for(const [firstSeed,seeds] of [[99,2],[109,2],[101,1],[90,30],[100,10]]) {
            await assert.rejects(reserveEvaluation(root,request({candidate:'another',firstSeed,seeds})),/overlaps/);
        }
        assert.equal((await reserveEvaluation(root,request({firstSeed:110}))).disposition,'NEW_RESERVATION');
        assert.equal((await reserveEvaluation(root,request({firstSeed:90}))).disposition,'NEW_RESERVATION');
    });
});
test('Renaming a batch, copying a corpus or changing protocol does not remove exposure',async()=>{
    await fixture(async root=>{
        await reserveEvaluation(root,request());
        await assert.rejects(reserveEvaluation(root,request({candidate:'renamed',corpus:'copied',run:'renamed-batch'})),/overlaps/);
        await assert.rejects(reserveEvaluation(root,request({protocolSha256:'d'.repeat(64)})),/overlaps/);
    });
});
test('An identical frozen evaluation may continue but is explicitly never a fresh independent lot',async()=>{
    await fixture(async root=>{
        const first=await reserveEvaluation(root,request());
        const next=await reserveEvaluation(root,request({run:'resume',corpus:'copy'}));
        assert.equal(next.reservationId,first.reservationId);
        assert.equal(next.disposition,'CONTINUATION');
        assert.equal(next.originallyReservedAt,first.originallyReservedAt);
        for(const change of [{freezeSha256:'e'.repeat(64)},{sourceHashes:{'policy.java':'f'.repeat(64)}}]) {
            await assert.rejects(reserveEvaluation(root,request(change)),/overlaps/);
        }
    });
});
test('Development may reuse a deal for learning but never turns it into a fresh heldout lot',async()=>{
    await fixture(async root=>{
        await reserveEvaluation(root,request({phase:'development'}));
        const next=await reserveEvaluation(root,request({phase:'development',candidate:'policy-b'}));
        assert.equal(next.disposition,'DEVELOPMENT_REUSE');
        await assert.rejects(reserveEvaluation(root,request()),/overlaps/);
    });
    await fixture(async root=>{
        await reserveEvaluation(root,request());
        assert.equal((await reserveEvaluation(root,request({phase:'development'}))).disposition,'DEVELOPMENT_REUSE');
        await assert.rejects(reserveEvaluation(root,request({candidate:'policy-b'})),/overlaps/);
    });
});
test('Reviewed historical exposure remains reserved without trusting corpus paths',async()=>{
    await fixture(async root=>{
        await assert.rejects(reserveEvaluation(root,request()),/historical/);
        assert.equal((await reserveEvaluation(root,request({firstSeed:200}))).disposition,'NEW_RESERVATION');
    },{reviewedBy:'history-import',exposures:[{firstSeed:105,seeds:20,evidenceSha256:'1'.repeat(64)}]});
});
test('Competing OS processes cannot both reserve overlapping intervals',async()=>{
    await fixture(async(root,folder)=>{
        const jobs=[];
        for(let i=0;i<6;i++) {
            const input=path.join(folder,`request-${i}.json`),output=path.join(folder,`receipt-${i}.json`);
            await fs.writeFile(input,JSON.stringify(request({candidate:`policy-${i}`,firstSeed:100+i})));
            jobs.push(invoke(['reserve',root,input,output]));
        }
        const results=await Promise.all(jobs);
        assert.equal(results.filter(r=>r.code===0).length,1,JSON.stringify(results));
        assert.equal(results.filter(r=>r.code!==0&&/overlaps/.test(r.error)).length,5,JSON.stringify(results));
        const state=JSON.parse(await fs.readFile(path.join(root,'registry.json'),'utf8'));
        assert.equal(state.reservations.length,1);
    });
});
test('Concurrent disjoint reservations are both retained, without lost writes',async()=>{
    await fixture(async root=>{
        await Promise.all([reserveEvaluation(root,request()),reserveEvaluation(root,request({firstSeed:200}))]);
        const state=JSON.parse(await fs.readFile(path.join(root,'registry.json'),'utf8'));
        assert.equal(state.reservations.length,2);
    });
});
test('Lock expiry fails closed and never steals an interrupted owners lock',async()=>{
    await fixture(async root=>{
        await fs.mkdir(path.join(root,'.lock'));
        const start=performance.now();
        await assert.rejects(reserveEvaluation(root,request(),{waitMs:80}),/lock deadline/);
        assert.ok(performance.now()-start<1500);
        assert.ok((await fs.stat(path.join(root,'.lock'))).isDirectory());
        await fs.rmdir(path.join(root,'.lock'));
    });
});
test('Truncated, changed and interrupted state cannot be silently reset',async()=>{
    for(const mode of ['truncated','changed','pending']) await fixture(async root=>{
        const file=path.join(root,'registry.json');
        if(mode==='truncated') await fs.writeFile(file,'{');
        if(mode==='changed') {
            const state=JSON.parse(await fs.readFile(file,'utf8'));state.history.exposures.push({firstSeed:1,seeds:1});
            await fs.writeFile(file,JSON.stringify(state));
        }
        if(mode==='pending') await fs.writeFile(path.join(root,'.pending.json'),'interrupted');
        await assert.rejects(reserveEvaluation(root,request()));
    });
});
test('Linked registry roots and directory ancestors are refused',async()=>{
    for(const mode of ['root','file']) await fixture(async(root,folder)=>{
        if(mode==='root') {
            const link=path.join(folder,'alias');await fs.symlink(root,link,process.platform==='win32'?'junction':'dir');
            await assert.rejects(reserveEvaluation(link,request()),/physical/);
        } else {
            // Hard links do not redirect path resolution; use a directory junction ancestor on Windows.
            const moved=path.join(folder,'real');await fs.rename(root,moved);
            await fs.symlink(moved,root,process.platform==='win32'?'junction':'dir');
            await assert.rejects(reserveEvaluation(root,request()),/physical/);
        }
    });
});
test('Malformed ranges, missing source identity and missing freeze fail before reserving',async()=>{
    await fixture(async root=>{
        for(const extra of [{firstSeed:-1},{firstSeed:1.2},{seeds:0},{seeds:Infinity},{firstSeed:2147483647},
            {sourceHashes:{}},{sourceHashes:{x:'bad'}},{freezeSha256:null},{phase:'renamed'},{candidate:'../escape'}]) {
            await assert.rejects(reserveEvaluation(root,request(extra)));
        }
        assert.equal((JSON.parse(await fs.readFile(path.join(root,'registry.json'),'utf8'))).reservations.length,0);
    });
});
test('Existing receipt is not overwritten and no reservation is made for that failed CLI attempt',async()=>{
    await fixture(async(root,folder)=>{
        const input=path.join(folder,'request.json'),output=path.join(folder,'existing.json');
        await fs.writeFile(input,JSON.stringify(request()));await fs.writeFile(output,'keep');
        const result=await invoke(['reserve',root,input,output]);
        assert.notEqual(result.code,0);assert.equal(await fs.readFile(output,'utf8'),'keep');
        assert.equal((JSON.parse(await fs.readFile(path.join(root,'registry.json'),'utf8'))).reservations.length,0);
    });
});

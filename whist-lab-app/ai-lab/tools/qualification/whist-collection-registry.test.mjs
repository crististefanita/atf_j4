import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import crypto from 'node:crypto';
import {initializeRegistry} from './whist-evaluation-registry.mjs';

const project = fileURLToPath(new URL('../../../../',import.meta.url));
const sourceHashes = {'game-core/src/main/java/Policy.java':crypto.createHash('sha256').update('class Policy {}').digest('hex')};
function run(args,cwd) {
    return new Promise((resolve,reject)=>{
        const child=spawn('pwsh',['-NoProfile','-File',...args],{cwd,stdio:['ignore','pipe','pipe']});
        let text='',expired=false;
        const timer=setTimeout(()=>{
            expired=true;
            if(process.platform==='win32') {
                const killer=spawn('taskkill',['/PID',String(child.pid),'/T','/F'],{stdio:'ignore'});
                killer.on('error',()=>child.kill());
            } else child.kill();
        },180000);
        child.stdout.on('data',bytes=>text+=bytes);child.stderr.on('data',bytes=>text+=bytes);
        child.on('error',error=>{clearTimeout(timer);reject(error);});
        child.on('close',code=>{
            clearTimeout(timer);
            if(expired)reject(Error('Controlled fixture exceeded its 180 second setup/execution safeguard: '+text.slice(-2000)));
            else resolve({code,text});
        });
    });
}
async function fixture(action) {
    const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-collection-registry-'));
    const root=path.join(folder,'project'),registry=path.join(folder,'canonical-registry');
    try {
        for(const dir of ['scripts','config','whist-lab-app/ai-lab/tools/qualification','whist-lab-app/game-core/src/main/java']) {
            await fs.mkdir(path.join(root,dir),{recursive:true});
        }
        await fs.copyFile(path.join(project,'scripts/run-whist-collection.ps1'),path.join(root,'scripts/run-whist-collection.ps1'));
        await fs.copyFile(path.join(project,'scripts/Register-WhistDevelopmentExposure.ps1'),path.join(root,'scripts/Register-WhistDevelopmentExposure.ps1'));
        await fs.copyFile(fileURLToPath(new URL('./whist-evaluation-registry.mjs',import.meta.url)),path.join(root,'whist-lab-app/ai-lab/tools/qualification/whist-evaluation-registry.mjs'));
        await fs.copyFile(fileURLToPath(new URL('./half-open-interval.mjs',import.meta.url)),path.join(root,'whist-lab-app/ai-lab/tools/qualification/half-open-interval.mjs'));
        await fs.copyFile(fileURLToPath(new URL('./WhistEvaluationInputs.psm1',import.meta.url)),path.join(root,'whist-lab-app/ai-lab/tools/qualification/WhistEvaluationInputs.psm1'));
        await fs.writeFile(path.join(root,'whist-lab-app/gradlew.bat'),'@echo off\r\necho CONTROLLED_GRADLE_STARTED\r\necho %*\r\nexit /b 0\r\n');
        await fs.writeFile(path.join(root,'whist-lab-app/game-core/src/main/java/Policy.java'),'class Policy {}');
        await fs.writeFile(path.join(root,'config/test-collections.json'),JSON.stringify({collections:[{id:'learning',task:':fixture:test'},{id:'arena',task:':fixture:test'},{id:'cost',task:':game-core:aiCostTest'}]}));
        await fs.writeFile(path.join(root,'config/whist-evaluation-policy.json'),JSON.stringify({schemaVersion:1,registryRoot:registry}));
        await initializeRegistry(registry,{reviewedBy:'controlled-fixture',exposures:[]});
        const protocol=path.join(folder,'protocol.json');
        await fs.writeFile(protocol,JSON.stringify({version:7,roles:['expert'],opponents:['EXPERT_V6','PLAYER_V6'],
            baselines:{expert:'EXPERT_V6',player:'PLAYER_V6'},development:{firstSeed:100,seeds:3},heldout:{firstSeed:200,seeds:3}}));
        const invoke=async(name,extra=[])=>{
            const out=path.join(folder,name),corpus=path.join(folder,name+'-corpus');
            const defaults={Collection:'learning',Candidate:'fixture-a',CorpusRoot:corpus,ProtocolFile:protocol,RunRoot:out};
            const base=Object.entries(defaults).filter(([key])=>!extra.includes('-'+key)).flatMap(([key,value])=>['-'+key,value]);
            const result=await run([path.join(root,'scripts/run-whist-collection.ps1'),...base,...extra],root);
            return {...result,out,corpus};
        };
        await action({root,folder,registry,protocol,invoke});
    } finally {
        assert.equal(path.resolve(path.dirname(folder)),path.resolve(os.tmpdir()));
        assert.match(path.basename(folder),/^whist-collection-registry-/);
        await fs.rm(folder,{recursive:true,force:true,maxRetries:10,retryDelay:100});
    }
}

test('Cost runner freezes explicit profile bytes and stays outside strength exposure reservation',async()=>{
    await fixture(async({folder,registry,invoke})=>{
        const profile=path.join(folder,'cost-profile.json');
        const bytes=Buffer.from('{\n  "schemaVersion": 1, "scope": "controlled dispatch only"\n}\n');
        await fs.writeFile(profile,bytes);await fs.unlink(path.join(registry,'registry.json'));
        const result=await invoke('cost',['-Collection','cost','-CostProfile',profile]);
        assert.equal(result.code,0,result.text);
        assert.deepEqual(await fs.readFile(path.join(result.out,'cost-profile.json')),bytes);
        const execution=await fs.readFile(path.join(result.out,'execution.log'),'utf8');
        assert.match(execution,/:game-core:aiCostTest/);
        assert.ok(execution.includes('-Pwhist.aiCost.profile='+path.join(result.out,'cost-profile.json')));
        assert.ok(execution.includes('-Pwhist.aiCost.output='+result.out));
        await assert.rejects(fs.access(path.join(result.out,'evaluation-reservation.json')));
        await assert.rejects(fs.access(result.corpus));
        const refused=await invoke('misrouted-profile',['-CostProfile',profile]);
        assert.notEqual(refused.code,0);assert.match(refused.text,/CostProfile requires/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});

test('Real runner identifies continuation across corpus copies and independent UI or documentation edits',async()=>{
    await fixture(async({root,invoke})=>{
        const first=await invoke('first');assert.equal(first.code,0,first.text);
        const a=JSON.parse(await fs.readFile(path.join(first.out,'evaluation-reservation.json'),'utf8'));
        assert.equal(a.disposition,'NEW_RESERVATION');
        assert.match(await fs.readFile(path.join(first.out,'execution.log'),'utf8'),/CONTROLLED_GRADLE_STARTED/);
        await fs.mkdir(path.join(root,'whist-lab-app/app/src/main/java'),{recursive:true});
        await fs.writeFile(path.join(root,'whist-lab-app/app/src/main/java/Presentation.java'),'class Presentation {}');
        await fs.writeFile(path.join(root,'whist-lab-app/README.md'),'Documentație independentă');
        const second=await invoke('copied');assert.equal(second.code,0,second.text);
        const b=JSON.parse(await fs.readFile(path.join(second.out,'evaluation-reservation.json'),'utf8'));
        assert.equal(b.reservationId,a.reservationId);assert.equal(b.disposition,'CONTINUATION');
    });
});
test('Heldout reservation is canonical; its corpus file is only a source-bound compatibility projection',async()=>{
    await fixture(async({folder,invoke})=>{
        const freeze=path.join(folder,'freeze.json');
        await fs.writeFile(freeze,JSON.stringify({status:'FROZEN_BEFORE_HELDOUT',candidate:'fixture-a',sourceHashes}));
        const first=await invoke('held-first',['-Phase','heldout','-CandidateFreeze',freeze]);assert.equal(first.code,0,first.text);
        const receipt=JSON.parse(await fs.readFile(path.join(first.out,'evaluation-reservation.json'),'utf8'));
        const local=JSON.parse(await fs.readFile(path.join(first.corpus,'holdout-use/romanian4-200-202.json'),'utf8'));
        assert.equal(local.reservationId,receipt.reservationId);
        await fs.writeFile(freeze,JSON.stringify({status:'FROZEN_BEFORE_HELDOUT',candidate:'fixture-b',sourceHashes}));
        const refused=await invoke('held-copied',['-Phase','heldout','-CandidateFreeze',freeze,'-Candidate','fixture-b']);
        assert.notEqual(refused.code,0);assert.match(refused.text,/overlaps/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});
test('Development exposure blocks a later heldout lot even when its corpus and candidate are changed',async()=>{
    await fixture(async({folder,protocol,invoke})=>{
        assert.equal((await invoke('development')).code,0);
        const spec=JSON.parse(await fs.readFile(protocol,'utf8'));spec.heldout={firstSeed:102,seeds:3};
        await fs.writeFile(protocol,JSON.stringify(spec));
        const freeze=path.join(folder,'freeze.json');await fs.writeFile(freeze,JSON.stringify({status:'FROZEN_BEFORE_HELDOUT',candidate:'fixture-b',sourceHashes}));
        const refused=await invoke('heldout',['-Phase','heldout','-CandidateFreeze',freeze,'-Candidate','fixture-b']);
        assert.notEqual(refused.code,0);assert.match(refused.text,/overlaps/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});
test('An uninitialized registry refuses execution rather than inventing a clean exposure history',async()=>{
    await fixture(async({registry,invoke})=>{
        await fs.unlink(path.join(registry,'registry.json'));
        const refused=await invoke('uninitialized');
        assert.notEqual(refused.code,0);assert.match(refused.text,/initialized/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});
test('Arena uses the same canonical exposure namespace as learning',async()=>{
    await fixture(async({folder,invoke})=>{
        const arena=path.join(folder,'arena.json');
        await fs.writeFile(arena,JSON.stringify({schemaVersion:1,id:'fixture-arena',profile:'ROMANIAN4_SERIES10',
            partition:'development',firstSeed:200,seeds:1}));
        const first=await invoke('arena',['-Collection','arena','-ProtocolFile',arena]);
        assert.equal(first.code,0,first.text);
        const receipt=JSON.parse(await fs.readFile(path.join(first.out,'evaluation-reservation.json'),'utf8'));
        assert.equal(receipt.identity.phase,'development');assert.equal(receipt.identity.candidate,'arena-fixture-arena');
        const freeze=path.join(folder,'freeze.json');await fs.writeFile(freeze,JSON.stringify({status:'FROZEN_BEFORE_HELDOUT',candidate:'fixture-a',sourceHashes}));
        const refused=await invoke('held-after-arena',['-Phase','heldout','-CandidateFreeze',freeze]);
        assert.notEqual(refused.code,0);assert.match(refused.text,/overlaps/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});
test('Diagnostic entry points register development reuse before any algorithm execution',async()=>{
    await fixture(async({root,folder,invoke})=>{
        const out=path.join(folder,'diagnostic');await fs.mkdir(out);
        const registered=await run([path.join(root,'scripts/Register-WhistDevelopmentExposure.ps1'),
            '-Seed','200','-RunRoot',out,'-Kind','perspectives'],root);
        assert.equal(registered.code,0,registered.text);
        const freeze=path.join(folder,'freeze.json');await fs.writeFile(freeze,JSON.stringify({status:'FROZEN_BEFORE_HELDOUT',candidate:'fixture-a',sourceHashes}));
        const refused=await invoke('held-after-diagnostic',['-Phase','heldout','-CandidateFreeze',freeze]);
        assert.notEqual(refused.code,0);assert.match(refused.text,/overlaps/);
        await assert.rejects(fs.access(path.join(refused.out,'execution.log')));
    });
});
test('Executable policy and shared interval edits each change source identity without resetting exposure',async()=>{
    await fixture(async({root,invoke})=>{
        const first=await invoke('before-change');assert.equal(first.code,0,first.text);
        await fs.appendFile(path.join(root,'whist-lab-app/game-core/src/main/java/Policy.java'),'\n// modified policy');
        const next=await invoke('after-change');assert.equal(next.code,0,next.text);
        const a=JSON.parse(await fs.readFile(path.join(first.out,'evaluation-reservation.json'),'utf8'));
        const b=JSON.parse(await fs.readFile(path.join(next.out,'evaluation-reservation.json'),'utf8'));
        assert.notEqual(a.reservationId,b.reservationId);assert.equal(b.disposition,'DEVELOPMENT_REUSE');
        assert.notEqual(a.identity.sourceHashes['game-core/src/main/java/Policy.java'],b.identity.sourceHashes['game-core/src/main/java/Policy.java']);
        const relative='ai-lab/tools/qualification/half-open-interval.mjs';
        const file=path.join(root,'whist-lab-app',relative);
        assert.equal(b.identity.sourceHashes[relative],crypto.createHash('sha256').update(await fs.readFile(file)).digest('hex'));
        await fs.appendFile(file,'\n// controlled executable source identity change\n');
        const last=await invoke('after-shared-change');assert.equal(last.code,0,last.text);
        const c=JSON.parse(await fs.readFile(path.join(last.out,'evaluation-reservation.json'),'utf8'));
        assert.notEqual(b.reservationId,c.reservationId);assert.equal(c.disposition,'DEVELOPMENT_REUSE');
        assert.notEqual(b.identity.sourceHashes[relative],c.identity.sourceHashes[relative]);
        assert.equal(b.identity.sourceHashes['game-core/src/main/java/Policy.java'],c.identity.sourceHashes['game-core/src/main/java/Policy.java']);
    });
});

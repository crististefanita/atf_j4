import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {evaluateLearning} from './whist-learning-evaluate.mjs';

async function fixture(mutate,version=4){
  const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-quality-contract-'));await fs.mkdir(path.join(folder,'invocations'));
  for(const role of ['expert','player'])for(const opponent of (version===5?['EXPERT_V4','PLAYER_V4']:['EXPERT_V3','PLAYER_V3']))for(let seed=(version===5?5500:4500);seed<(version===5?5508:4508);seed++)for(let seat=0;seat<4;seat++){
    const id=`development-${role}-${opponent}-seed${seed}-seat${seat}`,r={invocationId:id,status:'PASS',durationNanos:1,parameters:{role,opponent,seed:String(seed),heroSeat:seat},checkpoints:[{comparisons:[{passed:true}]}],results:[{kind:'before',points:100,winShare:.25,exact:16},{kind:'after',points:101,winShare:.25,exact:17}]};
    if(version===5)for(const result of r.results){result.byHandSize={};for(let size=1;size<=8;size++)result.byHandSize[size]={hands:size===1?8:size===8?4:2,points:size===1?100:size===2&&result.kind==='after'?1:0,exact:size===1?4:size===8?2:size===2?(result.kind==='after'?2:1):2,absoluteBidError:1};result.exact=Object.values(result.byHandSize).reduce((s,m)=>s+m.exact,0);}
    if(mutate)mutate(r);await fs.writeFile(path.join(folder,'invocations',id+'.json'),JSON.stringify(r));
  }return folder;
}
async function dispose(folder){
  // Delete only individual files in the exact test-owned directories, without recursive shell operations.
  for(const sub of ['invocations','quality-invocations']){const target=path.join(folder,sub);for(const name of await fs.readdir(target).catch(()=>[]))await fs.unlink(path.join(target,name));await fs.rmdir(target).catch(()=>{});}
  for(const name of await fs.readdir(folder))await fs.unlink(path.join(folder,name));await fs.rmdir(folder);
}
test('all four fixed families and both strict role gains are required',async()=>{const f=await fixture();try{const result=await evaluateLearning(f);assert.equal(result.status,'PASS');assert.equal(result.pairs,128);assert.equal(result.roles.expert.pointDelta,64);assert.equal(result.roles.player.pointDelta,64);const manifest=JSON.parse(await fs.readFile(path.join(f,'development-composition-manifest.json')));assert.equal(manifest.receipts.length,129);}finally{await dispose(f);}});
test('score gain cannot hide a fixed-family win regression',async()=>{const f=await fixture(r=>{if(r.parameters.role==='player'&&r.parameters.opponent==='EXPERT_V3')r.results[1].winShare=0;});try{const result=await evaluateLearning(f);assert.equal(result.status,'REGRESSION');assert.equal(result.roles.player.accepted,false);const gate=JSON.parse(await fs.readFile(path.join(f,'quality-invocations/development.json')));assert.equal(gate.status,'FAIL');assert.ok(gate.checkpoints.some(c=>c.comparisons.some(a=>!a.passed)));}finally{await dispose(f);}});
test('missing source invocation does not silently shrink the evaluation',async()=>{const f=await fixture();try{const first=(await fs.readdir(path.join(f,'invocations')))[0];await fs.unlink(path.join(f,'invocations',first));await assert.rejects(evaluateLearning(f),/Expected 128/);}finally{await dispose(f);}});
test('contradictory captured PASS is rejected before composition',async()=>{const f=await fixture(r=>{if(r.parameters.seed==='4500'&&r.parameters.heroSeat===0)r.checkpoints[0].comparisons[0].passed=false;});try{await assert.rejects(evaluateLearning(f),/Contradictory captured outcome/);}finally{await dispose(f);}});

const round5={version:5,development:{firstSeed:5500,seeds:8},heldout:{firstSeed:81000,seeds:16}};
test('round5 requires independently measured large-hand gain, not only one-card points',async()=>{const f=await fixture(r=>{for(const m of Object.values(r.results[1].byHandSize))m.points=0;r.results[1].byHandSize[1].points=101;},5);try{const result=await evaluateLearning(f,'development',['expert','player'],round5);assert.equal(result.status,'REGRESSION');assert.equal(result.roles.expert.largeHandPointDelta,0);}finally{await dispose(f);}});
test('round5 refuses inconsistent size aggregates before accepting progress',async()=>{const f=await fixture(r=>{r.results[1].byHandSize[8].points=999;},5);try{await assert.rejects(evaluateLearning(f,'development',['expert','player'],round5),/Mismatched hand-size aggregates/);}finally{await dispose(f);}});
test('round5 accepts a complete explicit protocol with strict 2..8 gain',async()=>{const f=await fixture(null,5);try{const result=await evaluateLearning(f,'development',['expert','player'],round5);assert.equal(result.status,'PASS');assert.equal(result.roles.player.largeHandPointDelta,64);}finally{await dispose(f);}});

test('round6 uses its explicit frozen V4 opponent protocol without relabelling the gate',async()=>{const f=await fixture(null,5);try{const protocol={...round5,version:6,baselineRevision:4,opponents:['EXPERT_V4','PLAYER_V4']};const result=await evaluateLearning(f,'development',['expert','player'],protocol);assert.equal(result.status,'PASS');const gate=JSON.parse(await fs.readFile(path.join(f,'quality-invocations/development.json')));assert.equal(gate.methodName,'pairedProgressAgainstFrozenV4');}finally{await dispose(f);}});

test('a declared single-role protocol evaluates only that full cohort and refuses another role',async()=>{
 const f=await fixture(null,5);try{for(const name of await fs.readdir(path.join(f,'invocations')))if(name.startsWith('development-player-'))await fs.unlink(path.join(f,'invocations',name));const protocol={...round5,roles:['expert']};const r=await evaluateLearning(f,'development',['expert'],protocol);assert.equal(r.status,'PASS');assert.equal(r.pairs,64);assert.deepEqual(Object.keys(r.roles),['expert']);await assert.rejects(evaluateLearning(f,'development',['player'],protocol),/Roles differ/);}finally{await dispose(f);}
});

const safetyProtocol={...round5,acceptanceObjective:'SAFETY'};
test('safety accepts more exact contracts without requiring a positive point delta',async()=>{const f=await fixture(r=>{r.results[1].points=99;r.results[1].byHandSize[1].points=99;r.results[1].byHandSize[2].points=0;},5);try{const r=await evaluateLearning(f,'development',['expert','player'],safetyProtocol);assert.equal(r.status,'PASS');assert.ok(r.roles.expert.pointDelta<0);assert.ok(r.roles.expert.largeHandExactDelta>0);}finally{await dispose(f);}});
test('safety refuses apparent point progress without2..8 exact-contract progress',async()=>{const f=await fixture(r=>{r.results[1].byHandSize[2].exact=r.results[0].byHandSize[2].exact;r.results[1].exact=Object.values(r.results[1].byHandSize).reduce((n,m)=>n+m.exact,0);},5);try{assert.equal((await evaluateLearning(f,'development',['expert','player'],safetyProtocol)).status,'REGRESSION');}finally{await dispose(f);}});
test('impossible exact counts cannot become a safety gain',async()=>{const f=await fixture(r=>{r.results[1].byHandSize[2].exact=3;r.results[1].exact=Object.values(r.results[1].byHandSize).reduce((n,m)=>n+m.exact,0);},5);try{await assert.rejects(evaluateLearning(f,'development',['expert','player'],safetyProtocol),/Impossible hand-size counts/);}finally{await dispose(f);}});
test('a mismatched declared frozen hero baseline is rejected',async()=>{const f=await fixture(null,5);try{await assert.rejects(evaluateLearning(f,'development',['expert','player'],{...safetyProtocol,baselines:{expert:'EXPERT_V6',player:'PLAYER_V8'}}),/Hero baseline differs/);}finally{await dispose(f);}});

test('a positive aggregate safety gain cannot hide an exact-contract loss in one fixed family',async()=>{
 const f=await fixture(r=>{if(r.parameters.role==='player'){if(r.parameters.opponent==='EXPERT_V4')r.results[1].byHandSize[2].exact=0;else r.results[0].byHandSize[3].exact=1;for(const result of r.results)result.exact=Object.values(result.byHandSize).reduce((n,m)=>n+m.exact,0);}},5);
 try{const result=await evaluateLearning(f,'development',['expert','player'],safetyProtocol);assert.ok(result.roles.player.largeHandExactDelta>0);assert.equal(result.roles.player.accepted,false);const gate=JSON.parse(await fs.readFile(path.join(f,'quality-invocations/development.json')));assert.ok(gate.checkpoints.some(c=>c.comparisons.some(a=>a.name.includes('exact-contract loss')&&!a.passed)));}finally{await dispose(f);}
});

import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {evaluateLearning} from './whist-learning-evaluate.mjs';

async function fixture(mutate){
  const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-quality-contract-'));await fs.mkdir(path.join(folder,'invocations'));
  for(const role of ['expert','player'])for(const opponent of ['EXPERT_V3','PLAYER_V3'])for(let seed=4500;seed<4508;seed++)for(let seat=0;seat<4;seat++){
    const id=`development-${role}-${opponent}-seed${seed}-seat${seat}`,r={invocationId:id,status:'PASS',durationNanos:1,parameters:{role,opponent,seed:String(seed),heroSeat:seat},checkpoints:[{comparisons:[{passed:true}]}],results:[{kind:'before',points:100,winShare:.25,exact:16},{kind:'after',points:101,winShare:.25,exact:17}]};
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

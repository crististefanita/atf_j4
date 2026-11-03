import test from 'node:test';import assert from 'node:assert/strict';import {expectedCases,key,closedRecord} from './whist-learning-batches.mjs';
const p={version:7,opponents:['EXPERT_V6','PLAYER_V6'],development:{firstSeed:6700,seeds:6}},expected=expectedCases(p,'development'),hashes={'policy.java':'a'.repeat(64)};
const good=()=>({status:'PASS',parameters:{role:'expert',opponent:'EXPERT_V6',seed:'6700',heroSeat:0},sourceHashes:hashes,durationNanos:2,startedAt:'start',endedAt:'end',results:[{kind:'before'},{kind:'after'}],checkpoints:[{comparisons:[{passed:true}]}]});
test('declared six seeds contain96 unique pairs, not96 independent deals',()=>{assert.equal(expected.size,96);assert.equal(key(good().parameters),'expert/EXPERT_V6/6700/0');});
test('a real closed capture is eligible; an unclosed capture is never PASS',()=>{assert.equal(closedRecord(good(),expected,hashes),true);assert.equal(closedRecord({...good(),status:'RUNNING'},expected,hashes),false);});
test('completed FAIL and ERROR cannot be hidden by retry composition',()=>{for(const status of ['FAIL','ERROR'])assert.throws(()=>closedRecord({...good(),status},expected,hashes),/must remain visible/);});
test('changed source, missing timing, wrong seed and false assertions are refused',()=>{for(const mutate of [r=>r.sourceHashes={},r=>r.durationNanos=0,r=>r.parameters.seed='999',r=>r.checkpoints[0].comparisons[0].passed=false,r=>r.results.pop()]){const r=structuredClone(good());mutate(r);assert.throws(()=>closedRecord(r,expected,hashes));}});

test('role-specific protocols avoid rerunning an already validated other policy',()=>{
 assert.equal(expectedCases({...p,roles:['expert']},'development').size,48);
 for(const roles of [[],['expert','expert'],['other']])assert.throws(()=>expectedCases({...p,roles},'development'),/Declared roles/);
});

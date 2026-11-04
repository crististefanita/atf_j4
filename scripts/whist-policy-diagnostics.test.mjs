import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {decisionContextAt,summarizePair,diagnosePolicies} from './whist-policy-diagnostics.mjs';

// Minimal traces exercise only the pure contrast/context reducers. The file entry point requires complete legal games.
const hand=(number,size)=>({number,size,dealer:3,trump:'H',turnedCard:'AH',
    deal:[{seat:0,cards:['7S','8S','9S'].slice(0,size)},{seat:1,cards:['UNPLAYED_OPPONENT_1']},{seat:2,cards:['UNPLAYED_OPPONENT_2']},{seat:3,cards:['UNPLAYED_OPPONENT_3']}],
    bids:[{seat:0,bid:1},{seat:1,bid:0},{seat:2,bid:0},{seat:3,bid:0}],
    tricks:[{plays:[{seat:0,card:'7S'},{seat:1,card:'AS'},{seat:2,card:'KS'},{seat:3,card:'QS'}],winner:1},
        {plays:[{seat:1,card:'7H'},{seat:2,card:'8H'},{seat:3,card:'9H'},{seat:0,card:'8S'}],winner:3}],
    scores:Array.from({length:4},(_,seat)=>({seat,bid:seat===0?1:0,won:seat===0?1:0,total:10+seat,streak:0}))});
const game=()=>({id:'before-game',partition:'development',seed:'77',rules:{players:4,seriesEnabled:true,seriesBonus:10},hands:[hand(1,2),hand(2,3)]});
const changed=()=>{const before=game(),after=structuredClone(before);after.id='after-game';after.hands[0].tricks[0].plays[0].card='8S';return [before,after];};

test('newly lost same-bid overshoot and newly gained exact contracts are counted separately by size',()=>{
    const [before,after]=changed();after.hands[0].scores[0].won=2;before.hands[1].scores[0].won=0;
    const result=summarizePair(before,after,0,{invocationId:'pair77'});
    assert.equal(result.firstCardChanges,1);assert.equal(result.firstBidChanges,0);assert.equal(result.newlyLostExact,1);assert.equal(result.newlyGainedExact,1);
    assert.equal(result.overshoots,1);assert.equal(result.undershoots,0);assert.equal(result.lostWithSameOwnBid,1);assert.equal(result.lostWithChangedOwnBid,0);
    assert.equal(result.byHandSize[2].lost,1);assert.equal(result.byHandSize[3].gained,1);assert.equal(result.casebook[0].invocationId,'pair77');
});

test('later changed-bid undershoot is separate from the first earlier card difference',()=>{
    const [before,after]=changed();after.hands[1].bids[0].bid=2;after.hands[1].scores[0]={...after.hands[1].scores[0],bid:2,won:1};
    const result=summarizePair(before,after,0);assert.equal(result.firstCardChanges,1);assert.equal(result.undershoots,1);assert.equal(result.lostWithChangedOwnBid,1);
    const example=result.casebook[0];assert.equal(example.firstDifference.handNumber,1);assert.equal(example.lostContract.handNumber,2);assert.match(example.interpretation,/different hands/);
    assert.equal(example.firstDifference.context.players[0].bid,1,'Snapshot is the earlier card decision, not the later changed bid');
});

test('first direct bid change includes preceding bids only and is not an optimal-action label',()=>{
    const before=game(),after=structuredClone(before);after.hands[0].bids[0].bid=0;after.hands[0].scores[0].bid=0;
    const result=summarizePair(before,after,0);assert.equal(result.firstBidChanges,1);const example=result.casebook[0];
    assert.equal(example.firstDifference.kind,'bid');assert.deepEqual(example.firstDifference.context.players.map(p=>p.bid),[null,null,null,null]);assert.equal(example.firstDifference.context.history.length,0);
    assert.match(example.visibility,/no actual opponent holdings/);assert.match(example.interpretation,/does not prove/);
});

test('decision snapshot retains observer cards before the action and excludes future or opponent private cards',()=>{
    const g=game(),context=decisionContextAt(g,4,0),later=decisionContextAt(g,5,0),json=JSON.stringify(context);
    assert.deepEqual(context.observerOwnHand,['7S','8S']);assert.deepEqual(later.observerOwnHand,['8S']);
    assert.ok(!json.includes('UNPLAYED_OPPONENT'));assert.ok(!json.includes('AS'),'Future public play must not leak backwards');
    assert.deepEqual(context.table,[]);assert.deepEqual(later.table,[{seat:0,card:'7S'}]);assert.equal(later.players[0].remaining,1);
    assert.deepEqual(g.hands[0].deal[0].cards,['7S','8S'],'Snapshot construction must not mutate the corpus');
});

test('quota snapshot counts a completed trick once and keeps the current partial trick distinct',()=>{
    const context=decisionContextAt(game(),9,0);assert.deepEqual(context.players.map(p=>p.won),[0,1,0,0]);
    assert.equal(context.players[1].remainingNeed,-1);assert.equal(context.history.length,5);assert.deepEqual(context.table,[{seat:1,card:'7H'}]);
});

test('casebook is bounded to one lost example per hand size and ignores one-card terminal contrasts',()=>{
    const [before,after]=changed();before.hands[1].size=2;after.hands[1].size=2;after.hands[0].scores[0].won=2;after.hands[1].scores[0].won=2;
    const result=summarizePair(before,after,0);assert.equal(result.newlyLostExact,2);assert.equal(result.casebook.length,1);assert.equal(result.casebook[0].lostContract.handNumber,1);
    before.hands[0].size=1;after.hands[0].size=1;const smaller=summarizePair(before,after,0);assert.equal(smaller.newlyLostExact,1);assert.equal(smaller.casebook[0].lostContract.handNumber,2);
});

test('identical traces remain identical and an unexplained first actor change is refused',()=>{
    const before=game(),after=structuredClone(before);assert.equal(summarizePair(before,after,0).identicalGames,1);
    after.hands[0].scores[0].won=2;assert.throws(()=>summarizePair(before,after,0),/identical action traces/);after.hands[0].scores[0].won=1;
    after.hands[0].bids[1].bid=1;assert.throws(()=>summarizePair(before,after,0),/First changed action/);
});

test('invalid snapshot index and missing execution closure are rejected before diagnosis',async()=>{
    assert.throws(()=>decisionContextAt(game(),-1,0),/Action index/);assert.throws(()=>decisionContextAt(game(),999,0),/outside complete trace/);
    const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-policy-diagnosis-'));
    try{await fs.mkdir(path.join(folder,'invocations'));await fs.writeFile(path.join(folder,'candidate-source.json'),'{}');
        await fs.writeFile(path.join(folder,'invocations','development-unclosed.json'),JSON.stringify({status:'RUNNING'}));
        await assert.rejects(diagnosePolicies(folder,folder,'development'),/Closed passing/);
    }finally{await fs.unlink(path.join(folder,'invocations','development-unclosed.json'));await fs.rmdir(path.join(folder,'invocations'));await fs.unlink(path.join(folder,'candidate-source.json'));await fs.rmdir(folder);}
});

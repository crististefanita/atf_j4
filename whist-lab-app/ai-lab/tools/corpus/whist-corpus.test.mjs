import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {validateGame,decisionExamples,exportDecisionExamples} from './whist-corpus.mjs';
import {createLegalGame} from '../testing/legal-game-fixture.mjs';

const game=createLegalGame();

test('a generated complete legal game replays without an archive or emulator',()=>{
  const rows=[];const audit=validateGame(game,row=>rows.push(row));assert.equal(audit.cards,376);assert.equal(audit.tricks,94);assert.equal(audit.bids,96);
  assert.deepEqual(audit.finalScores,game.players.map(p=>game.hands.reduce((sum,h)=>sum+h.scores[p.seat].base+h.scores[p.seat].series,0)));
  assert.ok(rows.length>1000);assert.ok(rows.every(r=>r.passed));assert.equal(rows.filter(r=>r.name.startsWith('Legal card')).length,376);assert.ok(rows.some(r=>r.scope==='Hand 24'&&r.name==='Cumulative score'));
});
test('decision inputs contain own cards and contemporaneous public facts only',()=>{
  const examples=[...decisionExamples(game)];assert.equal(examples.length,472);
  assert.equal(examples.filter(x=>x.kind==='bid').length,96);assert.equal(examples.filter(x=>x.kind==='card').length,376);
  for(const d of examples){const o=d.observation;assert.deepEqual(Object.keys(o).sort(),['handSize','dealer','trump','turnedCard','ownHand','players','trick','history','futureHandSizes','seriesEnabled','seriesBonus'].sort());
    for(const p of o.players)assert.deepEqual(Object.keys(p).sort(),['seat','remaining','bid','won','total','streak'].sort());
    assert.equal(o.ownHand.length,o.players[d.seat].remaining);assert.equal(o.history.length%4,o.trick.length);
    if(d.kind==='card')assert.ok(o.ownHand.includes(d.action.card));
    if(d.kind==='bid'){assert.equal(o.players[d.seat].bid,null);assert.equal(o.history.length,0);assert.equal(o.players.filter(x=>x.bid!==null).length,game.hands[d.handNumber-1].bids.findIndex(x=>x.seat===d.seat));}
  }
  const first=examples[0];assert.ok(first.observation.players.every(p=>p.total===0&&p.streak===0));
  const secondHand=examples.find(d=>d.handNumber===2);assert.deepEqual(secondHand.observation.players.map(p=>p.total),game.hands[0].scores.map(p=>p.total));
  const eligible=examples.find(d=>d.handNumber===5);assert.equal(eligible.observation.handSize,2);assert.equal(eligible.observation.futureHandSizes.length,19);
});
test('corrupt data is refused before it becomes training input',()=>{
  const corruptions=[
    g=>{g.hands[0].deal[1].cards[0]=g.hands[0].deal[0].cards[0];},
    g=>{g.hands[0].tricks[0].plays[0].seat=(g.hands[0].tricks[0].plays[0].seat+1)%4;},
    g=>{g.hands[0].tricks[0].winner=(g.hands[0].tricks[0].winner+1)%4;},
    g=>{g.hands[11].scores[2].series=5;},
    g=>{g.hands[0].scores[0].total++;},
    g=>{g.hands[0].tricks[0].plays.pop();},
    g=>{g.hands.pop();},
    g=>{g.seed=19;},
    g=>{g.hands[0].bids[0].bid=2;},
    g=>{g.rules.seriesEnabled='true';},
    g=>{g.players[0].seat=1;},
    g=>{g.hands[0].dealer=-1;},
  ];
  for(const mutate of corruptions){const g=structuredClone(game);mutate(g);assert.throws(()=>validateGame(g));assert.throws(()=>[...decisionExamples(g)]);}
});
test('training exporter refuses heldout data before writing an output',async()=>{
  const folder=await fs.mkdtemp(path.join(os.tmpdir(),'whist-corpus-guard-'));const input=path.join(folder,'synthetic-heldout.json'),output=path.join(folder,'forbidden.jsonl');
  try{await fs.writeFile(input,JSON.stringify({...game,partition:'heldout'}));await assert.rejects(exportDecisionExamples(input,output),/development only/);await assert.rejects(fs.access(output));}
  finally{await fs.unlink(input);await fs.rmdir(folder);}
});

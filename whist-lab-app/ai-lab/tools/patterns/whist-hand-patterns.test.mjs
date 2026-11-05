import test from 'node:test';import assert from 'node:assert/strict';import {describeHolding,catalog} from './whist-hand-patterns.mjs';
const suit=(x,s)=>x.suits.find(c=>c.suit===s);
test('K+x and Q+x+x expose promotion conditions without claiming guaranteed off-suit tricks',()=>{
 const k=suit(describeHolding(['KH','7H'],'S',4,2),'H');assert.ok(k.patterns.includes('KING_WITH_LOW'));assert.deepEqual(k.honors[0].unseenHigher,['AH']);assert.equal(k.honors[0].promotionPossible,true);assert.equal(k.masterTrumps.length,0);
 const q=suit(describeHolding(['QH','7H','8H'],'S',4,3),'H');assert.ok(q.patterns.includes('QUEEN_TWO_LOW'));assert.deepEqual(q.honors[0].unseenHigher,['KH','AH']);assert.equal(q.honors[0].promotionPossible,true);
});
test('master trumps are distinct forced winners; a side ace is context-dependent',()=>{
 assert.equal(describeHolding(['AS','KS','QS'],'S',4,3).forcedMasterTrumpCount,3);assert.equal(describeHolding(['AH'],'S',4,1).forcedMasterTrumpCount,0);assert.equal(describeHolding(['KS'],'S',4,1,['AS']).forcedMasterTrumpCount,1);
});
test('own high cards and shared covers are represented jointly, never added as independent tricks',()=>{
 const s=suit(describeHolding(['KH','QH','7H'],'S',4,3),'H');assert.equal(s.honors.find(c=>c.card==='QH').higherHeld,1);assert.deepEqual(s.honors.find(c=>c.card==='QH').unseenHigher,['AH']);assert.equal(s.softHintsAreNotAdditive,true);assert.ok(s.patterns.includes('KING_QUEEN_WITH_LOW'));assert.ok(catalog().rules.every(r=>r.additive===false));
});
test('hand size changes dealt fraction without changing an honor into a certainty',()=>{
 const small=describeHolding(['QH','7H','8H'],'S',4,3),large=describeHolding(['QH','7H','8H'],'S',4,8);assert.equal(small.dealtFraction,3/8);assert.equal(large.dealtFraction,1);assert.equal(small.forcedMasterTrumpCount,0);assert.equal(large.forcedMasterTrumpCount,0);
});
test('reduced deck, duplicates and public/held overlap are rejected',()=>{
 for(const work of [()=>describeHolding(['7H'],'S',3,1),()=>describeHolding(['KH','KH'],'S',4,2),()=>describeHolding(['KH'],'S',4,1,['KH'])])assert.throws(work);
});

test('specific honor groups do not contaminate the simple K+x and Q+x+x priors',()=>{
 const ak=suit(describeHolding(['AH','KH','7H'],'S',4,3),'H');assert.ok(!ak.patterns.includes('KING_WITH_LOW'));
 const kqj=suit(describeHolding(['KH','QH','JH','7H'],'S',4,4),'H');assert.ok(kqj.patterns.includes('KING_QUEEN_JACK_WITH_LOW'));assert.ok(!kqj.patterns.includes('KING_WITH_LOW'));
 const qj=suit(describeHolding(['QH','JH','7H','8H'],'S',4,4),'H');assert.ok(qj.patterns.includes('QUEEN_JACK_TWO_LOW'));assert.ok(!qj.patterns.includes('QUEEN_TWO_LOW'));
});

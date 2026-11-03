import test from 'node:test';import assert from 'node:assert/strict';
import {features,featureNames,predict,ridge} from './whist-bid-forecast.mjs';
const card=s=>'SHCD'.indexOf(s.at(-1))*13+({A:14,K:13,Q:12,J:11}[s.slice(0,-1)]??Number(s.slice(0,-1)))-2;
test('Covers and joint honor sequences are distinct non-additive features',()=>{
 const f=hand=>Object.fromEntries(features(hand.map(card),4,hand.length,0,0,0).map((x,i)=>[featureNames[i],x]));
 assert.equal(f(['KS','7S']).coveredKing,1);assert.equal(f(['AS','KS','7S']).coveredKing,0);
 assert.equal(f(['QS','7S','8S']).coveredQueen,1);assert.equal(f(['KS','QS','7S','8S']).coveredQueen,0);
 assert.equal(f(['AS','KS','7S']).aceKing,1);
});
test('Auction prefix is explicit and a prediction is bounded to legal bid range',()=>{
 const a=features([card('AS'),card('7H')],0,2,3,2,3);
 assert.equal(a[19],1.5);assert.equal(a[20],2);assert.equal(a[21],3);
 assert.equal(predict(Array(featureNames.length).fill(100),a,2),2);
 assert.equal(predict(Array(featureNames.length).fill(-100),a,2),0);
});
test('Regularized fitting returns finite coefficients from collinear features',()=>{
 const rows=Array.from({length:50},(_,i)=>({x:[1,i/10,...Array(featureNames.length-2).fill(0)],y:1+i/10}));
 const coefficients=ridge(rows);assert.ok(coefficients.every(Number.isFinite));
 assert.ok(Math.abs(predict(coefficients,rows[20].x,8)-3)<0.1);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import {bidFeatures,bidDistribution,legalBids,FEATURE_NAMES} from './whist-bid-evidence.mjs';

const model=()=>({coefficients:FEATURE_NAMES.map((_,i)=>i===2?1:0),sigma:Array(9).fill(0.8),strength:0.25});
test('Turned ace promotes held KQ of trump without inventing a hidden ace',()=>{
    const x=bidFeatures(['KS','QS'],2,'S','AS',0,0);assert.equal(x[3],2/8);
    assert.equal(bidFeatures(['KS','QS'],2,'S','7S',0,0)[3],0);
});
test('Covered king and covered queen are contextual, not independent guaranteed tricks',()=>{
    assert.equal(bidFeatures(['KH','7H'],2,'S','8S',0,0)[8],1/8);
    assert.equal(bidFeatures(['AH','KH','7H'],3,'S','8S',0,0)[8],0);
    assert.equal(bidFeatures(['QH','7H','8H'],3,'S','8S',0,0)[9],1/8);
});
test('Last-bid restriction renormalizes likelihood over the remaining legal bids',()=>{
    assert.deepEqual(legalBids(2,3,1),[0,2]);const p=bidDistribution(model(),['AS','KS'],2,'S','7S',3,1);
    assert.equal(p[1],0);assert.ok(Math.abs(p.reduce((a,b)=>a+b,0)-1)<1e-12);
});
test('A zero bid does not prove the absence of master trumps',()=>{
    const p=bidDistribution(model(),['AS','KS'],2,'S','7S',0,0);assert.ok(p[0]>=0.75/3);
});
test('A real turned card cannot also be in the hypothetical own hand',()=>{
    assert.throws(()=>bidFeatures(['AS','KS'],2,'S','AS',0,0));
});

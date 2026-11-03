import test from 'node:test';import assert from 'node:assert/strict';
import {firstDivergence} from './whist-r13-diagnose.mjs';
const game=()=>({hands:[{number:1,size:2,bids:[{seat:0,bid:0}],tricks:[{plays:[{seat:0,card:'7S'}]}]},
    {number:2,size:3,bids:[{seat:0,bid:1}],tricks:[{plays:[{seat:0,card:'AS'}]}]}]});
test('A later changed bid is not mistaken for the first cause of an earlier changed card',()=>{const a=game(),b=game();b.hands[0].tricks[0].plays[0].card='8S';b.hands[1].bids[0].bid=2;assert.equal(firstDivergence(a,b).after.kind,'card');});
test('A direct bidding change is identified before any card is played',()=>{const a=game(),b=game();b.hands[0].bids[0].bid=1;assert.equal(firstDivergence(a,b).after.kind,'bid');});
test('Identical domain traces have no first divergence',()=>assert.equal(firstDivergence(game(),game()),null));

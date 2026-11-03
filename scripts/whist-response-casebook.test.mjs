import test from 'node:test';
import assert from 'node:assert/strict';
import {responseOracle} from './whist-response-casebook.mjs';

// Actual development holding, reduced to two tricks; a hypothetical world only.
// H is trump. Lead AD: KD, JS, 9H -> seat0; then 7C, 8H, QH, 10S -> seat2.
// Lead 8H: QH, JS, 9H -> seat2; then KD, 10S, 7C, AD -> hero.
// The second lead yields one trick rather than zero at bid1: +6 instead of -1.
const holding = () => ({hero: 1, hands: [[20, 31], [19, 51], [23, 50], [8, 9]],
    trick: [], won: [0, 0, 0, 0], goals: [1, 1, 1, 0], size: 2,
    trump: 1, seriesEnabled: true, seriesBonus: 10, streak: 0});

test('The independent response oracle reproduces both manually traced trick sequences at every seat', () => {
    for (let rotation = 0; rotation < 4; rotation++) {
        const original = holding(), p = structuredClone(original);
        p.hero = (original.hero + rotation) % 4;
        for (let s = 0; s < 4; s++) {
            p.hands[(s + rotation) % 4] = original.hands[s];
            p.goals[(s + rotation) % 4] = original.goals[s];
        }
        const result = responseOracle(p);
        assert.deepEqual(result.values, {'8H': 6, AD: -1});
        assert.deepEqual(result.optimalCards, ['8H']);
        assert.equal(result.baselineCard, 'AD'); assert.equal(result.baseline, -1);
    }
});

test('The same two-trick proof retains configurable fifth-success and fifth-miss scores', () => {
    for (const bonus of [5, 10]) {
        const p = {...holding(), streak: 4, seriesBonus: bonus};
        assert.equal(responseOracle(p).best, 6 + bonus);
        assert.equal(responseOracle(p).baseline, -1);
    }
    const result = responseOracle({...holding(), streak: -4});
    assert.equal(result.best, 6); assert.equal(result.baseline, -6);
});

test('An exhaustive analysis leaves the public/declared hypothetical input unchanged', () => {
    const p = holding(), before = structuredClone(p);
    responseOracle(p); assert.deepEqual(p, before);
});

test('A duplicated card cannot be accepted as a feasible hypothetical world', () => {
    const p = holding(); p.hands[2][0] = p.hands[0][0];
    assert.throws(() => responseOracle(p));
});

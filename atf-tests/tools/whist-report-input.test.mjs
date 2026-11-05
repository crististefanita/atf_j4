import test from 'node:test';
import assert from 'node:assert/strict';
import {selectWholeCaptures, nativeCounts, validateCapture} from './whist-report-input.mjs';

const capture = id => ({schemaVersion: 1, runId: 'run', invocationId: id, className: 'test.Component', methodName: 'case',
    startedAt: '2026-11-03T08:00:00Z', endedAt: '2026-11-03T08:00:01Z', durationNanos: 1000000000,
    status: 'PASS', parameters: {}, checkpoints: [{owner: id, title: 'rule', context: {hand: ['KS', '7S']},
        comparisons: [{name: 'rule', expected: 1, actual: 1, passed: true}]}]});

test('Select whole original captures in declared order, without rewriting evidence', () => {
    const a = capture('a'), b = capture('b');
    const selected = selectWholeCaptures([a, b], ['b']);
    assert.equal(selected[0], b); assert.deepEqual(a.checkpoints[0].context.hand, ['KS', '7S']);
});
test('A non-PASS or contradictory source cannot disappear through input selection', () => {
    const a = capture('a'), b = capture('b'); b.status = 'FAIL';
    assert.throws(() => selectWholeCaptures([a, b], ['a'])); b.status = 'PASS'; b.checkpoints[0].comparisons[0].passed = false;
    assert.throws(() => selectWholeCaptures([a, b], ['a']));
});
test('Missing, duplicate and reselected invocation identities are rejected', () => {
    assert.throws(() => selectWholeCaptures([capture('a')], ['missing']));
    assert.throws(() => selectWholeCaptures([capture('a'), capture('a')], ['a']));
    assert.throws(() => selectWholeCaptures([capture('a')], ['a', 'a']));
});
test('Duplicate checkpoint owners fail before report publication', () => {
    const r = capture('arena'); r.checkpoints.push(structuredClone(r.checkpoints[0]));
    assert.throws(() => selectWholeCaptures([r], ['arena']), /Repeated checkpoint owner/);
});
test('A historical arena format defect can be inventoried only under its exact declared shape', () => {
    const r = capture('arena');
    r.checkpoints[0].owner = 'arena/setup'; r.checkpoints[0].title = 'Four declared strategies and balanced seating';
    r.checkpoints.push({...structuredClone(r.checkpoints[0]), title: 'Complete Romanian schedule'});
    assert.throws(() => validateCapture(r)); assert.doesNotThrow(() => validateCapture(r, true));
    r.checkpoints[1].owner = 'different-owner'; assert.throws(() => validateCapture(r, true));
});
test('Native population counts require actual complete zero-skip suites', () => {
    const xml = '<testsuite name="contract" tests="7" skipped="0" failures="0" errors="0">';
    assert.equal(nativeCounts(xml).tests, 7);
    for (const key of ['skipped', 'failures', 'errors']) assert.throws(() => nativeCounts(xml.replace(key + '="0"', key + '="1"')));
    assert.throws(() => nativeCounts('<testsuite tests="0">'));
});

test('An explicitly declared rejected experiment retains its original failure and every comparison', () => {
    const r = capture('strict-gate'); r.status = 'FAIL'; r.failure = 'No strict gain';
    r.checkpoints[0].comparisons[0] = {name: 'strict gain', expected: true, actual: false, passed: false};
    const selected = selectWholeCaptures([r], ['strict-gate'], {'strict-gate': 'FAIL'});
    assert.equal(selected[0], r); assert.equal(selected[0].status, 'FAIL');
    assert.equal(selected[0].checkpoints[0].comparisons[0].passed, false);
    assert.throws(() => selectWholeCaptures([r], [], {'strict-gate': 'FAIL'}), /must be included whole/);
});

test('Outcome declarations cannot hide errors, invent failures or refer to absent records', () => {
    const r = capture('a');
    assert.throws(() => selectWholeCaptures([r], ['a'], {a: 'FAIL'}));
    assert.throws(() => selectWholeCaptures([r], ['a'], {missing: 'FAIL'}));
    r.status = 'ERROR'; r.failure = 'Interrupted';
    assert.throws(() => selectWholeCaptures([r], ['a'], {a: 'ERROR'}));
});

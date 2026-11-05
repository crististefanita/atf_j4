import test from 'node:test';
import assert from 'node:assert/strict';
import {intervalsOverlap} from './half-open-interval.mjs';

test('Integer interval intersection agrees with explicit set intersection', () => {
    const intervals = [];
    for (let start = -3; start <= 4; start++) for (let end = start; end <= 4; end++) {
        intervals.push(Object.freeze({start, end}));
    }
    for (const left of intervals) for (const right of intervals) {
        const leftMembers = new Set(Array.from({length: left.end - left.start}, (_, i) => left.start + i));
        const rightMembers = Array.from({length: right.end - right.start}, (_, i) => right.start + i);
        const expected = rightMembers.some(value => leftMembers.has(value));
        assert.equal(intervalsOverlap(left, right), expected, JSON.stringify({left, right}));
        assert.equal(intervalsOverlap(right, left), expected);
    }
});

test('Touching endpoints and empty ranges share no indexed item', () => {
    assert.equal(intervalsOverlap({start: 10, end: 20}, {start: 20, end: 30}), false);
    for (const point of [9, 10, 15, 20, 21]) {
        assert.equal(intervalsOverlap({start: point, end: point}, {start: 10, end: 20}), false);
        assert.equal(intervalsOverlap({start: 10, end: 20}, {start: point, end: point}), false);
    }
});

test('Safe integer extremes work without subtraction or endpoint rounding', () => {
    const min = Number.MIN_SAFE_INTEGER, max = Number.MAX_SAFE_INTEGER;
    assert.equal(intervalsOverlap({start: min, end: max}, {start: max - 1, end: max}), true);
    assert.equal(intervalsOverlap({start: min, end: min + 1}, {start: min + 1, end: max}), false);
    assert.equal(intervalsOverlap({start: min, end: min}, {start: min, end: max}), false);
});

test('Malformed or reversed intervals fail explicitly on either side', () => {
    const valid = {start: 0, end: 2};
    for (const interval of [null, undefined, [], 'range', 2, {}, {start: 0},
        {start: '0', end: 2}, {start: 0.5, end: 2}, {start: NaN, end: 2},
        {start: 0, end: Infinity}, {start: 0, end: Number.MAX_SAFE_INTEGER + 1}]) {
        assert.throws(() => intervalsOverlap(interval, valid), TypeError);
        assert.throws(() => intervalsOverlap(valid, interval), TypeError);
    }
    assert.throws(() => intervalsOverlap({start: 3, end: 2}, valid), RangeError);
    assert.throws(() => intervalsOverlap(valid, {start: 3, end: 2}), RangeError);
});

test('The generic comparison preserves caller-owned objects and metadata', () => {
    const left = Object.freeze({start: 1, end: 7, label: 'dataset-a'});
    const right = Object.freeze({start: 3, end: 5, label: 'workflow-b'});
    assert.equal(intervalsOverlap(left, right), true);
    assert.deepEqual(left, {start: 1, end: 7, label: 'dataset-a'});
    assert.deepEqual(right, {start: 3, end: 5, label: 'workflow-b'});
});

import assert from 'node:assert/strict';
import {test} from 'node:test';
import {QualificationTiming, mergeTiming, timingFromDocument} from './release-timing.mjs';

const stamp = value => new Date(value).toISOString();
const span = (stage, start, end, status = 'PASS', reason) => ({
  stage, status, startedUtc: stamp(start), endedUtc: stamp(end), elapsedMs: end - start, reason
});
const receipt = (owner, ...stages) => ({schemaVersion: 1, kind: 'ATF_RELEASE_TIMING', owner, stages});

test('overlapping stages are counted once and pauses remain visible', () => {
  const report = mergeTiming([
    receipt('maven', span('compile', 100, 300), span('tests', 300, 900)),
    receipt('android', span('preflight', 400, 700), span('reuse', 701, 701, 'REUSED', 'matching hashes')),
    receipt('github', span('download', 1200, 1550))
  ]);
  assert.equal(report.elapsedWallMs, 1450);
  assert.equal(report.elapsedCoveredMs, 1150);
  assert.equal(report.elapsedUnmeasuredMs, 300);
  assert.deepEqual(report.stages.map(stage => stage.stage), ['compile', 'tests', 'preflight', 'reuse', 'download']);
});

test('a failing phase retains a duration, and timestamps use a monotonic clock', async () => {
  const readings = [100, 115, 165];
  const utc = [1000, 1015, 1067];
  const timing = new QualificationTiming({now: () => utc.shift(), ticks: () => readings.shift()});
  const failure = new Error('build refused');
  await assert.rejects(timing.measure('build', async () => { throw failure; }), error => error === failure);
  const document = timingFromDocument({kind: 'ATF_RELEASE_RAG_VERDICT', timing: timing.receipt('release')});
  const [row] = mergeTiming([document]).stages;
  assert.deepEqual([row.status, row.startedUtc, row.endedUtc, row.elapsedMs], ['FAIL', stamp(1015), stamp(1067), 50]);
  assert.equal(row.wallClockDeltaMs, 2);
});

test('refuses an impossible duration or reuse without a reason', () => {
  assert.throws(() => mergeTiming([receipt('maven', {...span('compile', 100, 300), elapsedMs: -5})]), /INVALID_RELEASE_TIMING/);
  assert.throws(() => mergeTiming([receipt('maven', span('reuse', 701, 701, 'REUSED'))]), /INVALID_RELEASE_TIMING/);
  assert.equal(mergeTiming([receipt('maven', span('emulator', 701, 701, 'NOT_RUN', 'offline'))]).elapsedWallMs, 0);
});

import fs from 'node:fs/promises';
import path from 'node:path';

const statuses = new Set(['PASS', 'FAIL', 'BLOCKED', 'REUSED', 'NOT_RUN']);
const stageName = /^[a-z][a-z0-9_.:-]*$/i;

export function within(root, file) {
  const relative = path.relative(root, file);
  return !relative || (!path.isAbsolute(relative) && relative !== '..' && !relative.startsWith('..' + path.sep));
}

export async function writeTimingReceipt(file, document) {
  await fs.writeFile(file, JSON.stringify(document, null, 2) + '\n', {flag: 'wx'});
}

export class QualificationTiming {
  #stages = [];
  #clock;
  #cursor;

  constructor({now = () => Date.now(), ticks = () => performance.now()} = {}) {
    this.#clock = () => ({utc: now(), tick: ticks()});
    this.#cursor = this.#clock();
  }

  #record(stage, start, end, status) {
    this.#stages.push({stage, status, startedUtc: new Date(start.utc).toISOString(),
      endedUtc: new Date(end.utc).toISOString(), elapsedMs: Math.round(end.tick - start.tick)});
  }

  checkpoint(stage, status = 'PASS') {
    const end = this.#clock();
    this.#record(stage, this.#cursor, end, status);
    this.#cursor = end;
  }

  async measure(stage, action, outcome = () => 'PASS') {
    const start = this.#clock();
    let status = 'FAIL';
    try {
      const result = await action();
      status = outcome(result);
      return result;
    } finally { this.#record(stage, start, this.#clock(), status); }
  }

  receipt(owner) {
    return {schemaVersion: 1, kind: 'ATF_RELEASE_TIMING', owner, stages: [...this.#stages]};
  }
}

function interval({stage, status, startedUtc, endedUtc, elapsedMs, reason}) {
  const start = Date.parse(startedUtc), end = Date.parse(endedUtc);
  if (!stageName.test(stage ?? '') || !statuses.has(status) || !Number.isSafeInteger(elapsedMs)
      || elapsedMs < 0 || !Number.isFinite(start) || !Number.isFinite(end) || end < start
      || (['REUSED', 'NOT_RUN'].includes(status) && (elapsedMs !== 0 || end !== start || !reason))) throw Error('INVALID_RELEASE_TIMING:' + stage);
  return {start, end};
}

export function mergeTiming(receipts) {
  if (!receipts.length) throw Error('RELEASE_TIMING_RECEIPTS_REQUIRED');
  const stages = [];
  for (const receipt of receipts) {
    if (receipt?.schemaVersion !== 1 || receipt.kind !== 'ATF_RELEASE_TIMING' || !stageName.test(receipt.owner ?? '')
        || !Array.isArray(receipt.stages) || !receipt.stages.length) throw Error('INVALID_RELEASE_TIMING_RECEIPT');
    for (const row of receipt.stages) stages.push({...row, owner: receipt.owner, ...interval(row)});
  }
  stages.sort((a, b) => a.start - b.start || a.end - b.end);
  const measured = stages.filter(row => !['REUSED', 'NOT_RUN'].includes(row.status));
  let elapsedCoveredMs = 0, coveredUntil = -Infinity;
  for (const {start, end} of measured) {
    elapsedCoveredMs += Math.max(0, end - Math.max(start, coveredUntil));
    coveredUntil = Math.max(coveredUntil, end);
  }
  const elapsedWallMs = measured.length ? Math.max(...measured.map(row => row.end)) - measured[0].start : 0;
  return {schemaVersion: 1, kind: 'ATF_RELEASE_TIMELINE', elapsedWallMs, elapsedCoveredMs,
    elapsedUnmeasuredMs: elapsedWallMs - elapsedCoveredMs,
    stages: stages.map(({start, end, ...row}) => ({...row, wallClockDeltaMs: end - start - row.elapsedMs}))};
}

export function timingFromDocument(document) {
  if (document?.kind === 'ATF_RELEASE_TIMING') return document;
  if (['ATF_RELEASE_RAG_VERDICT', 'ATF_RELEASE_GIT_SNAPSHOT', 'github-release-retention'].includes(document?.kind)) return document.timing;
  throw Error('UNKNOWN_RELEASE_TIMING_DOCUMENT');
}

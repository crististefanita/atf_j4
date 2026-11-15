import {readFileSync} from 'node:fs';

const policy = JSON.parse(readFileSync(new URL('../../config/release-platform-rag-policy.json', import.meta.url), 'utf8'));
export const evidenceFreshness = Object.freeze(policy.ragEvaluator.evidenceFreshness);

const utcPattern = /^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)\.(\d{3})Z$/;

export function observationDeadline(observedAtUtc, maxAgeSeconds, check, now = Date.now()) {
  if (!Number.isSafeInteger(maxAgeSeconds) || maxAgeSeconds <= 0) throw Error(check + '_FRESHNESS_POLICY');
  if (observedAtUtc === undefined || observedAtUtc === null) throw Error(check + '_TIME_REQUIRED');
  const time = typeof observedAtUtc === 'string' && utcPattern.test(observedAtUtc) ? Date.parse(observedAtUtc) : NaN;
  if (!Number.isFinite(time) || new Date(time).toISOString() !== observedAtUtc) throw Error(check + '_TIME_INVALID');
  if (time > now) throw Error(check + '_TIME_FUTURE: ' + observedAtUtc);
  const expiresAtUtc = new Date(time + maxAgeSeconds * 1000).toISOString();
  if (now >= Date.parse(expiresAtUtc)) throw Error(check + '_EXPIRED: ' + expiresAtUtc);
  return expiresAtUtc;
}

import crypto from 'node:crypto';
import path from 'node:path';

const hash = /^[0-9a-f]{64}$/;
export const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

export function safeRelative(value) {
  if (typeof value !== 'string' || value.length === 0 || value.includes('\\') || path.posix.isAbsolute(value)
      || value.split('/').some(part => part === '' || part === '.' || part === '..')) throw Error('UNSAFE_RELATIVE_PATH:' + value);
  return value;
}

export function componentInputMap(rows) {
  if (!Array.isArray(rows) || rows.length === 0) throw Error('INPUT_LIST_REQUIRED');
  const result = new Map();
  for (const row of rows) {
    const name = safeRelative(row.path);
    if (!hash.test(row.sha256 ?? '') || result.has(name)) throw Error('INPUT_ROW_INVALID:' + name);
    result.set(name, row.sha256);
  }
  return result;
}

export function inputFingerprint(inputs) {
  const records = [...inputs.keys()].sort().map(file => `${file}\t${inputs.get(file)}\n`);
  return digest(records.join(''));
}

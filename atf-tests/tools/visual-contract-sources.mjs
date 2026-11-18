import fs from 'node:fs/promises';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {gitBlob} from './git-snapshot-blobs.mjs';
import {applyEol} from './git-snapshot-eol.mjs';
import {visualContractPaths} from './visual-change-contracts.mjs';

const digest = bytes => createHash('sha256').update(bytes).digest('hex');

export async function readVisualContractSources(project, source, commit, previous, candidate,
    {readBefore = file => gitBlob(project, commit, file), readAfter = file => fs.readFile(path.join(source, file))} = {}) {
  const result = new Map();
  for (const file of visualContractPaths) {
    if (!previous.has(file) || !candidate.has(file) || previous.get(file) === candidate.get(file)) continue;
    try {
      const raw = await readBefore(file), after = await readAfter(file);
      const before = digest(raw) === previous.get(file) ? raw : ['LF', 'CRLF']
        .map(kind => applyEol(raw, {kind})).find(bytes => digest(bytes) === previous.get(file));
      if (before) result.set(file, {before, after});
    } catch { /* Unavailable or non-reconstructible bytes retain all native flow gates. */ }
  }
  return result;
}

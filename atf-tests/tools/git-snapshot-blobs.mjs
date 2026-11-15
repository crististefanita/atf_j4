import {execFileSync} from 'node:child_process';

const objectId = /^[0-9a-f]{40}$/;
const git = (root, args, options = {}) =>
  execFileSync('git', ['--no-replace-objects', '-C', root, ...args], {timeout: 60000, ...options});

export function commitId(root, reference) {
  const commit = git(root, ['rev-parse', '--verify', '--end-of-options', reference + '^{commit}'], {encoding: 'utf8'}).trim();
  if (!objectId.test(commit)) throw Error('GIT_COMMIT_REQUIRED');
  return commit;
}

export function assertAncestor(root, ancestor, main) {
  git(root, ['merge-base', '--is-ancestor', ancestor, main]);
}

export function gitBlob(root, commit, name) {
  return git(root, ['cat-file', 'blob', commit + ':' + name], {maxBuffer: 32 * 1024 * 1024});
}

export function trackedEntries(root, commit, prefix = '') {
  const entries = new Map();
  const listing = git(root, ['ls-tree', '-rlz', '--full-tree', commit, ...(prefix ? ['--', prefix] : [])], {encoding: 'utf8', maxBuffer: 16 * 1024 * 1024});
  for (const record of listing.split('\0')) {
    if (!record) continue;
    const split = record.indexOf('\t');
    const name = record.slice(split + 1);
    if (!prefix && name.startsWith('release/')) continue;
    const fields = /^(100644|100755) blob ([0-9a-f]{40}) +([0-9]+)$/.exec(record.slice(0, split));
    // Non-file entries remain visible in the inventory and block the verification.
    entries.set(name, fields ? {id: fields[2], bytes: Number(fields[3])} : null);
  }
  return entries;
}

function* decodeBatch(raw, entries) {
  let cursor = 0;
  for (const [name, {id, bytes}] of entries) {
    const lineEnd = raw.indexOf(10, cursor);
    const start = lineEnd + 1, end = start + bytes;
    if (lineEnd < cursor || raw.toString('ascii', cursor, lineEnd) !== id + ' blob ' + bytes || raw[end] !== 10) {
      throw Error('GIT_OBJECT_PROTOCOL:' + name);
    }
    yield [name, raw.subarray(start, end)];
    cursor = end + 1;
  }
  if (cursor !== raw.length) throw Error('GIT_OBJECT_TRAILING_DATA');
}

export function* readGitBlobs(root, entries) {
  let batch = [], size = 0;
  const limit = 16 * 1024 * 1024;
  for (const entry of entries) {
    if (batch.length && (size + entry[1].bytes > limit || batch.length === 128)) {
      yield* readBatch(root, batch, size);
      batch = []; size = 0;
    }
    batch.push(entry); size += entry[1].bytes;
  }
  if (batch.length) yield* readBatch(root, batch, size);
}

function readBatch(root, batch, size) {
  const input = batch.map(([, entry]) => entry.id + '\n').join('');
  const raw = git(root, ['cat-file', '--batch'], {input, maxBuffer: size + 128 * batch.length});
  return decodeBatch(raw, batch);
}

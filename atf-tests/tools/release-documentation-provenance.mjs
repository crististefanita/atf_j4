import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {digest, regularFile, strictJson} from './evaluate-release-rag.mjs';
import {gitBlob} from './git-snapshot-blobs.mjs';

const sourcePaths = new Set(['README.md', 'release-notes.md', 'reports/README.md',
  'README.en.md', 'release-notes.en.md', 'reports/README.en.md']);
const receiptName = 'documentation-provenance.json';

/** Presentation may be translated; the original publication documents remain verifiable in Git. */
export async function verifyDocumentationProvenance(root, version, options = {}) {
  if (typeof version !== 'string' || !/^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:-[0-9A-Za-z.-]+)?$/.test(version))
    throw Error('DOC-RELEASE-VERSION');
  const folder = path.join(await fs.realpath(root), 'release', version);
  const receipt = path.join(folder, receiptName);
  try { await fs.lstat(receipt); }
  catch (error) { if (error.code === 'ENOENT') return {version, originals: 0}; throw error; }
  const {value} = await strictJson(await regularFile(folder, receiptName));
  if (value.schemaVersion !== 1 || value.kind !== 'RELEASE_DOCUMENTATION_PROVENANCE'
      || !/^[a-f0-9]{40}$/.test(value.sourceCommit ?? '') || !Array.isArray(value.files) || !value.files.length)
    throw Error('DOC-RELEASE-PROVENANCE');
  const readSource = options.readSource ?? ((commit, file) => gitBlob(options.gitRoot ?? root, commit, file));
  const seen = new Set();
  for (const row of value.files) {
    if (!sourcePaths.has(row.path) || seen.has(row.path) || !/^[a-f0-9]{64}$/.test(row.sha256 ?? '')
        || !Number.isSafeInteger(row.bytes) || row.bytes < 1) throw Error('DOC-RELEASE-ORIGINAL-ENTRY');
    seen.add(row.path);
    const original = await readSource(value.sourceCommit, 'release/' + version + '/' + row.path);
    if (original.length !== row.bytes || digest(original) !== row.sha256) throw Error('DOC-RELEASE-ORIGINAL-MISMATCH:' + row.path);
  }
  for (const name of ['README.md', 'release-notes.md', 'reports/README.md']) {
    if (!seen.has(name)) throw Error('DOC-RELEASE-ORIGINAL-MISSING:' + name);
    await regularFile(folder, name);
  }
  return {version, sourceCommit: value.sourceCommit, originals: seen.size};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [input, gitRoot = input] = process.argv.slice(2);
    const root = await fs.realpath(input);
    const {value: catalog} = await strictJson(await regularFile(root, 'release/index.json'));
    const versions = [];
    for (const version of catalog.versions) versions.push(await verifyDocumentationProvenance(root, version, {gitRoot}));
    console.log(JSON.stringify({status: 'PASS', versions}));
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}

import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {digest} from './evaluate-release-rag.mjs';
import {verifyDocumentationProvenance} from './release-documentation-provenance.mjs';

async function fixture(exercise) {
  const parent = await fs.realpath(os.tmpdir());
  const root = await fs.mkdtemp(path.join(parent, 'whist-doc-provenance-'));
  const folder = path.join(root, 'release/1.2.3');
  try {
    await fs.mkdir(path.join(folder, 'reports'), {recursive: true});
    const originals = new Map();
    const files = [];
    for (const name of ['README.md', 'release-notes.md', 'reports/README.md']) {
      const original = Buffer.from('Original document: ' + name);
      originals.set('release/1.2.3/' + name, original);
      files.push({path: name, bytes: original.length, sha256: digest(original)});
      await fs.writeFile(path.join(folder, name), 'Translated presentation');
    }
    const receipt = {schemaVersion: 1, kind: 'RELEASE_DOCUMENTATION_PROVENANCE', sourceCommit: 'a'.repeat(40), files};
    const save = () => fs.writeFile(path.join(folder, 'documentation-provenance.json'), JSON.stringify(receipt));
    await save();
    const options = {readSource(commit, name) {
      assert.equal(commit, 'a'.repeat(40));
      assert.ok(originals.has(name));
      return originals.get(name);
    }};
    await exercise({root, folder, receipt, originals, options, save});
  } finally {
    if (path.dirname(root) !== parent || !path.basename(root).startsWith('whist-doc-provenance-')) throw Error('Unsafe fixture cleanup');
    await fs.rm(root, {recursive: true, force: true});
  }
}

test('translation preserves independently verified Git originals', () => fixture(async f => {
  assert.deepEqual(await verifyDocumentationProvenance(f.root, '1.2.3', f.options),
    {version: '1.2.3', sourceCommit: 'a'.repeat(40), originals: 3});
}));

test('a changed original or false size cannot be relabelled as the same publication', () => fixture(async f => {
  f.originals.set('release/1.2.3/README.md', Buffer.from('changed'));
  await assert.rejects(verifyDocumentationProvenance(f.root, '1.2.3', f.options), /ORIGINAL-MISMATCH/);
}));

test('missing originals, duplicate entries and paths outside the release are rejected', async () => {
  for (const mutation of [r => r.files.pop(), r => r.files.push(r.files[0]), r => r.files[0].path = '../README.md']) {
    await fixture(async f => {
      mutation(f.receipt); await f.save();
      await assert.rejects(verifyDocumentationProvenance(f.root, '1.2.3', f.options), /ORIGINAL-(MISSING|ENTRY)/);
    });
  }
});

test('a receipt from another source commit is rejected by the source reader', () => fixture(async f => {
  f.receipt.sourceCommit = 'b'.repeat(40); await f.save();
  await assert.rejects(verifyDocumentationProvenance(f.root, '1.2.3', f.options), /Expected values/);
}));

test('a missing canonical guide and an invalid receipt cannot pass', async () => {
  await fixture(async f => {
    await fs.unlink(path.join(f.folder, 'README.md'));
    await assert.rejects(verifyDocumentationProvenance(f.root, '1.2.3', f.options));
  });
  await fixture(async f => {
    f.receipt.sourceCommit = 'main'; await f.save();
    await assert.rejects(verifyDocumentationProvenance(f.root, '1.2.3', f.options), /PROVENANCE/);
  });
});

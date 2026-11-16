import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {test} from 'node:test';
import {inputFingerprint, strictJson} from './evaluate-release-rag.mjs';
import {checkout} from './release-git-fixture.mjs';
import {verifyGitSnapshot} from './verify-release-git-snapshot.mjs';

test('compares original blob bytes despite edits in the live worktree', async t => {
  const f = await checkout(t);
  await fs.writeFile(path.join(f.repository, 'README.md'), 'parallel work');
  const withoutPublication = await verifyGitSnapshot({repository: f.repository, candidate: f.candidate});
  assert.equal(withoutPublication.status, 'PASS');
  assert.equal(withoutPublication.cleanupEligible, false);
  const verified = await verifyGitSnapshot(f);
  assert.equal(verified.reconstruction, 'GIT_EXACT');
  assert.equal(verified.cleanupEligible, true);
});

test('binds a mixed EOL recipe to the exact commit and reconstructs without the qualified directory', async t => {
  const f = await checkout(t, {mixed: true});
  const missing = await verifyGitSnapshot({...f, qualifiedRoot: undefined});
  assert.equal(missing.status, 'BLOCKED');
  const observed = await verifyGitSnapshot(f);
  assert.equal(observed.cleanupEligible, true);
  assert.equal(observed.endings[0].kind, 'MIXED');
  const replay = await verifyGitSnapshot({...f, qualifiedRoot: undefined, prior: observed});
  assert.deepEqual(replay.endings, observed.endings);
  assert.equal(replay.status, 'PASS');
  const changed = {...observed, endings: [{...observed.endings[0], crlfBits: 'Ag=='}]};
  const invalid = await verifyGitSnapshot({...f, qualifiedRoot: undefined, prior: changed});
  assert.equal(invalid.cleanupEligible, false);
  assert.equal(invalid.problems[0].reason, 'EOL_RECIPE_HASH');
});

test('refuses source-only, Git-only and changed content', async t => {
  const extra = await checkout(t, {sourceOnly: true}), changed = await checkout(t, {contentChanged: true});
  assert.equal((await verifyGitSnapshot(extra)).problems[0].reason, 'QUALIFIED_ONLY');
  assert.equal((await verifyGitSnapshot(changed)).problems[0].reason, 'GIT_BYTES_DIFFER');
  const manifest = (await strictJson(path.join(extra.candidate, 'release-validation.json'))).value;
  const input = (await strictJson(path.join(extra.candidate, manifest.source.inputManifest))).value;
  input.files = input.files.filter(row => row.path === 'README.md');
  input.fingerprint = inputFingerprint(new Map(input.files.map(item => [item.path, item.sha256])));
  manifest.source.workingTreeFingerprint = input.fingerprint;
  await fs.writeFile(path.join(extra.candidate, 'release-validation.json'), JSON.stringify(manifest));
  await fs.writeFile(path.join(extra.candidate, manifest.source.inputManifest), JSON.stringify(input));
  assert.equal((await verifyGitSnapshot(extra)).problems[0].reason, 'GIT_ONLY');
});

test('cleanup refuses a stale verdict or changed published report', async t => {
  const f = await checkout(t);
  const stale = {value: {...f.verdict.value, candidateManifestSha256: '0'.repeat(64)}};
  assert.equal((await verifyGitSnapshot({...f, verdict: stale})).problems[0].reason, 'CLEANUP_POSTPUBLICATION_VERDICT_MISMATCH');
  await fs.writeFile(path.join(f.candidate, 'reports/report.html'), '<html>tampered</html>');
  f.git('add', '.');
  f.git('commit', '-qm', 'changed report');
  f.git('update-ref', 'refs/remotes/origin/main', 'HEAD');
  const invalid = await verifyGitSnapshot(f);
  assert.equal(invalid.cleanupEligible, false);
  assert.match(invalid.problems[0].reason, /CLEANUP_GIT_PAYLOAD_HASH.*report.html/);
});

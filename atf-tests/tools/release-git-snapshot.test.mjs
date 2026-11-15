import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';
import {digest, inputFingerprint, strictJson} from './evaluate-release-rag.mjs';
import {verifyGitSnapshot} from './verify-release-git-snapshot.mjs';

async function checkout(t, {mixed = false, sourceOnly = false, contentChanged = false} = {}) {
  const fixture = await fs.mkdtemp(path.join(os.tmpdir(), 'release-git-'));
  t.after(() => fs.rm(fixture, {recursive: true, force: true}));
  const repository = path.join(fixture, 'repo'), qualifiedRoot = path.join(fixture, 'qualified');
  const candidate = path.join(repository, 'release', '0.21.4');
  const write = async (file, content) => {
    await fs.mkdir(path.dirname(file), {recursive: true});
    await fs.writeFile(file, typeof content === 'string' ? content : JSON.stringify(content));
  };
  await fs.mkdir(repository);
  const git = (...args) => execFileSync('git', args, {cwd: repository, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe']}).trim();
  git('init', '-q', '-b', 'main');
  git('config', 'core.autocrlf', 'false');
  git('config', 'user.name', 'Snapshot fixture');
  git('config', 'user.email', 'fixture@example.invalid');
  await write(path.join(repository, 'README.md'), 'test\nSecond line\n');
  await write(path.join(repository, 'lib.txt'), 'lib\n');
  git('add', '.');
  git('commit', '-qm', 'source');
  const sourceCommit = git('rev-parse', 'HEAD');
  const text = mixed ? 'test\r\nSecond line\n' : contentChanged ? 'different content\n' : 'test\nSecond line\n';
  await write(path.join(qualifiedRoot, 'README.md'), text);
  const rows = [{path: 'README.md', sha256: digest(text)}, {path: 'lib.txt', sha256: digest('lib\n')}];
  if (sourceOnly) rows.push({path: 'local.yml', sha256: digest('unsafe to delete')});
  const sourceFingerprint = inputFingerprint(new Map(rows.map(row => [row.path, row.sha256])));
  const apkName = 'WhistLab-0.21.4.apk', apk = 'APK fixture', checksum = digest(apk) + '  ' + apkName + '\n';
  const input = {sourceCommit, fingerprint: sourceFingerprint, files: rows};
  for (const [file, bytes] of [[apkName, apk], [apkName + '.sha256', checksum], ['reports/apk-inputs.json', input], ['reports/report.html', '<html></html>']]) {
    await write(path.join(candidate, file), bytes);
  }
  const payload = await Promise.all([apkName, apkName + '.sha256', 'reports/apk-inputs.json', 'reports/report.html'].map(async name => {
    const bytes = await fs.readFile(path.join(candidate, name));
    return {path: name, bytes: bytes.length, sha256: digest(bytes)};
  }));
  await write(path.join(candidate, 'release-validation.json'), {release: {versionName: '0.21.4', apkName, apkSha256: digest(apk), apkBytes: apk.length},
    source: {sourceCommit, workingTreeFingerprint: sourceFingerprint, inputManifest: 'reports/apk-inputs.json'},
    publication: {state: 'PUBLISHED'}, integrity: {files: payload}});
  git('tag', 'v0.21.4', sourceCommit);
  git('add', '.');
  git('commit', '-qm', 'published payload');
  git('update-ref', 'refs/remotes/origin/main', 'HEAD');
  const verdictFile = path.join(fixture, 'post.json');
  await write(verdictFile, {kind: 'ATF_RELEASE_RAG_VERDICT', verdict: 'READY_FOR_HUMAN_REVIEW',
    releaseVersion: '0.21.4', sourceCommit, workingTreeFingerprint: sourceFingerprint,
    candidateManifestSha256: (await strictJson(path.join(candidate, 'release-validation.json'))).sha256,
    checks: ['SOURCE_SNAPSHOT', 'PAYLOAD_INTEGRITY', 'APK_IDENTITY', 'PLATFORM_GATES', 'GITHUB_RETENTION'].map(code => ({code, status: 'PASS'}))});
  return {repository, candidate, qualifiedRoot, git, verdict: await strictJson(verdictFile)};
}

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

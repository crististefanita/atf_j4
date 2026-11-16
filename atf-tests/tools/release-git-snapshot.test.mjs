import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';
import {digest, inputFingerprint, strictJson} from './evaluate-release-rag.mjs';
import {verifyGitSnapshot} from './verify-release-git-snapshot.mjs';
import {syncLocalRelease} from './local-release-sync.mjs';

async function checkout(t, {mixed = false, sourceOnly = false, contentChanged = false, version = '0.21.4'} = {}) {
  const fixture = await fs.mkdtemp(path.join(os.tmpdir(), 'release-git-'));
  t.after(() => fs.rm(fixture, {recursive: true, force: true}));
  const repository = path.join(fixture, 'repo'), qualifiedRoot = path.join(fixture, 'qualified');
  const candidate = path.join(repository, 'release', version);
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
  const apkName = 'WhistLab-' + version + '.apk', apk = 'APK fixture', checksum = digest(apk) + '  ' + apkName + '\n';
  const input = {sourceCommit, fingerprint: sourceFingerprint, files: rows};
  const releaseFiles = [[apkName, apk], [apkName + '.sha256', checksum], ['reports/apk-inputs.json', input],
    ['reports/report.html', '<html></html>'], ['README.md', '# Edition'], ['reports/README.md', '# Audit']];
  await write(path.join(repository, 'release', 'index.json'), {
    schemaVersion: 2, currentVersion: version, versions: [version, '0.21.3']
  });
  for (const [file, bytes] of releaseFiles) await write(path.join(candidate, file), bytes);
  const payload = await Promise.all(releaseFiles.map(async ([name]) => {
    const bytes = await fs.readFile(path.join(candidate, name));
    return {path: name, bytes: bytes.length, sha256: digest(bytes)};
  }));
  await write(path.join(candidate, 'release-validation.json'), {release: {versionName: version, apkName, apkSha256: digest(apk), apkBytes: apk.length},
    source: {sourceCommit, workingTreeFingerprint: sourceFingerprint, inputManifest: 'reports/apk-inputs.json'},
    publication: {state: 'PUBLISHED'}, integrity: {files: payload}});
  git('tag', 'v' + version, sourceCommit);
  git('add', '.');
  git('commit', '-qm', 'published payload');
  git('update-ref', 'refs/remotes/origin/main', 'HEAD');
  const verdictFile = path.join(fixture, 'post.json');
  await write(verdictFile, {kind: 'ATF_RELEASE_RAG_VERDICT', verdict: 'READY_FOR_HUMAN_REVIEW',
    releaseVersion: version, sourceCommit, workingTreeFingerprint: sourceFingerprint,
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

test('materializes only the current published release and preserves bytes', async t => {
  const f = await checkout(t);
  const proof = await verifyGitSnapshot(f);
  const stagingParent = path.dirname(f.repository);
  const home = path.join(stagingParent, 'legacy with spaces');
  const destination = path.join(home, 'release');
  await fs.mkdir(destination, {recursive: true});
  const git = (...args) => execFileSync('git', args, {cwd: home, stdio: 'pipe'});
  git('init', '-q', '-b', 'legacy');
  git('config', 'user.name', 'Local release fixture');
  git('config', 'user.email', 'fixture@example.invalid');
  await fs.writeFile(path.join(home, 'work.txt'), 'tracked original');
  await fs.writeFile(path.join(destination, 'README.md'), 'old release');
  await fs.writeFile(path.join(destination, 'index.json'), '{"currentVersion":"0.19.1"}');
  git('add', '.');
  git('commit', '-qm', 'legacy worktree');
  await fs.writeFile(path.join(home, 'work.txt'), 'parallel work');
  const options = {...f, proof, destination, stagingParent};
  assert.equal((await syncLocalRelease(options)).status, 'SYNCED');
  assert.deepEqual((await fs.readdir(destination)).sort(), ['0.21.4', 'README.md', 'index.json']);
  assert.equal(await fs.readFile(path.join(home, 'work.txt'), 'utf8'), 'parallel work');
  assert.deepEqual(await fs.readFile(path.join(destination, '0.21.4', 'reports/report.html')),
    await fs.readFile(path.join(f.candidate, 'reports/report.html')));
  assert.equal((await syncLocalRelease(options)).status, 'UNCHANGED');
  await fs.writeFile(path.join(destination, '0.21.4', 'reports/report.html'), 'local notes');
  await assert.rejects(syncLocalRelease(options), /LOCAL_RELEASE_PAYLOAD_HASH/);
  assert.equal(await fs.readFile(path.join(destination, '0.21.4', 'reports/report.html'), 'utf8'), 'local notes');
});

test('refuses unpublished proof, a modified candidate, and unknown local data', async t => {
  const f = await checkout(t);
  const proof = await verifyGitSnapshot(f);
  const stagingParent = path.dirname(f.repository);
  const destination = path.join(stagingParent, 'release');
  await fs.mkdir(destination);
  const options = {...f, proof, destination, stagingParent};
  await assert.rejects(syncLocalRelease({...options, proof: {...proof, cleanupEligible: false}}), /LOCAL_RELEASE_PUBLICATION_REQUIRED/);
  await assert.rejects(syncLocalRelease({...options, destination: path.dirname(f.candidate)}), /LOCAL_RELEASE_PERIMETER/);
  await fs.writeFile(path.join(f.candidate, 'reports/report.html'), 'tampered copy');
  await assert.rejects(syncLocalRelease(options), /LOCAL_RELEASE_PAYLOAD_HASH/);
  assert.deepEqual(await fs.readdir(destination), []);
  await fs.mkdir(path.join(destination, 'unrelated-empty-directory'));
  await assert.rejects(syncLocalRelease(options), /LOCAL_RELEASE_EXISTING_CHANGES/);
  assert.deepEqual(await fs.readdir(destination), ['unrelated-empty-directory']);
});

test('rotates the local copy to the next current edition', async t => {
  const old = await checkout(t);
  const next = await checkout(t, {version: '0.21.5'});
  const stagingParent = path.dirname(old.repository);
  const destination = path.join(stagingParent, 'release');
  await syncLocalRelease({...old, proof: await verifyGitSnapshot(old), destination, stagingParent});
  const proof = await verifyGitSnapshot(next);
  assert.equal((await syncLocalRelease({...next, proof, destination, stagingParent})).status, 'SYNCED');
  assert.deepEqual((await fs.readdir(destination)).sort(), ['0.21.5', 'README.md', 'index.json']);
});

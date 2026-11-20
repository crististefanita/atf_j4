import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {releaseVersionPattern, publishedVersionPattern, releaseVersionAtLeast} from './release-version.mjs';
import {catalogVersions} from './github-release-retention.mjs';
import {loadPublication} from './github-release-journal.mjs';
import {digest, strictJson} from './evaluate-release-rag.mjs';
import {checkout} from './release-git-fixture.mjs';
import {verifyGitSnapshot} from './verify-release-git-snapshot.mjs';
import {syncLocalRelease} from './local-release-sync.mjs';

test('two-part ordering retains historical patch precision and rejects equivalent catalog entries', () => {
  for (const [left, right, expected] of [['0.22', '0.21.13', true], ['0.22', '0.22.1', false],
    ['0.21.13', '0.22', false], ['0.22', '0.22.0', true], ['0.22.0', '0.22', true],
    ['0.9', '0.10', false], ['0.9007199254740993', '0.9007199254740992', true]]) {
    assert.equal(releaseVersionAtLeast(left, right), expected, left + ' >= ' + right);
  }
  const catalog = versions => ({schemaVersion: 2, currentVersion: versions[0], versions});
  assert.deepEqual(catalogVersions(catalog(['0.22', '0.21.13'])), ['0.22', '0.21.13']);
  for (const versions of [['0.22', '0.22.0'], ['0.22', '0.22.1'], ['0.21.13', '0.22']])
    assert.throws(() => catalogVersions(catalog(versions)), /RETENTION_CATALOG_ORDER/);
});

test('invalid version shapes cannot enter a catalog or published asset path', () => {
  for (const value of ['0', '0.022', '00.22', '0.22.00', '0.22.1.1', '../0.22', '0.22/asset', ' 0.22']) {
    assert.equal(releaseVersionPattern.test(value), false, value);
    assert.equal(publishedVersionPattern.test(value), false, value);
    assert.equal(releaseVersionAtLeast(value, '0.22'), false, value);
    assert.throws(() => catalogVersions({schemaVersion: 2, currentVersion: value, versions: [value, '0.21.13']}));
  }
  assert.equal(releaseVersionPattern.test('0.22-rc.1'), true);
  assert.equal(publishedVersionPattern.test('0.22-rc.1'), false);
});

test('two-part publication keeps the exact APK name and checksum', async t => {
  const candidate = await fs.mkdtemp(path.join(os.tmpdir(), 'version-publication-'));
  t.after(() => fs.rm(candidate, {recursive: true, force: true}));
  const apk = 'WhistLab-0.22.apk', bytes = Buffer.from('controlled APK bytes');
  const notes = '# Whist 0.22\n\nA concise release.\n\n## Changes\n\n- Use the two-part release identity.\n';
  const payloads = new Map([[apk, bytes], [apk + '.sha256', Buffer.from(digest(bytes) + '  ' + apk + '\n')],
    ['release-notes.md', Buffer.from(notes)]]);
  const manifest = {publication: {state: 'PREPARED', repository: 'example/game', channel: 'TEST', accessMode: 'AUTHENTICATED',
    targetUrl: 'https://github.com/example/game/releases/download/v0.22/' + apk},
    release: {versionName: '0.22', artifactKind: 'apk', apkName: apk, apkRelativePath: apk, apkBytes: bytes.length, apkSha256: digest(bytes)},
    source: {sourceCommit: 'a'.repeat(40), workingTreeFingerprint: 'b'.repeat(64)},
    integrity: {files: [...payloads].map(([name, content]) => ({path: name, bytes: content.length, sha256: digest(content)}))}};
  for (const [name, content] of payloads) await fs.writeFile(path.join(candidate, name), content);
  const manifestFile = path.join(candidate, 'release-validation.json');
  await fs.writeFile(manifestFile, JSON.stringify(manifest));
  const verdictFile = path.join(candidate, 'verdict.json');
  await fs.writeFile(verdictFile, JSON.stringify({schemaVersion: 1, kind: 'ATF_RELEASE_RAG_VERDICT', verdict: 'READY_FOR_HUMAN_REVIEW',
    releaseVersion: '0.22', sourceCommit: manifest.source.sourceCommit, channel: 'TEST',
    workingTreeFingerprint: manifest.source.workingTreeFingerprint, candidateManifestSha256: (await strictJson(manifestFile)).sha256}));
  const admitted = await loadPublication(candidate, verdictFile);
  assert.equal(admitted.identity.tag, 'v0.22');
  assert.equal(admitted.identity.assets[0].name, apk);
  assert.deepEqual(admitted.payloads.get(apk), bytes);
});

test('two-part local delivery reconstructs Git and projects only the current version', async t => {
  const f = await checkout(t, {version: '0.22'});
  const proof = await verifyGitSnapshot(f);
  assert.equal(proof.status, 'PASS');
  const stagingParent = path.dirname(f.repository), destination = path.join(stagingParent, 'release');
  const result = await syncLocalRelease({...f, proof, stagingParent, destination});
  assert.equal(result.status, 'SYNCED');
  assert.deepEqual((await fs.readdir(destination)).sort(), ['0.22', 'README.md', 'index.json']);
  assert.equal(JSON.parse(await fs.readFile(path.join(destination, 'index.json'))).currentVersion, '0.22');
});

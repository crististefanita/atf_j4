import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import {test} from 'node:test';
import {checkout} from './release-git-fixture.mjs';
import {syncLocalRelease} from './local-release-sync.mjs';
import {verifyGitSnapshot} from './verify-release-git-snapshot.mjs';

test('materializes only the current published release and preserves bytes', async t => {
  const f = await checkout(t);
  const proof = await verifyGitSnapshot(f);
  const stagingParent = path.dirname(f.repository);
  const home = path.join(stagingParent, 'legacy with spaces');
  const destination = path.join(home, 'release');
  await fs.mkdir(destination, {recursive: true});
  const git = (...args) => execFileSync('git', ['-c', 'user.name=Local release fixture',
    '-c', 'user.email=fixture@example.invalid', ...args], {cwd: home, stdio: 'pipe'});
  git('init', '-q', '-b', 'legacy');
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

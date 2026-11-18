import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {collectGithubRetention, observeGithubRetention} from './collect-github-release-retention.mjs';
import {sealGithubRetentionCatalog} from './github-retention-seal.mjs';
import {githubRetentionProblems} from './github-release-retention.mjs';
import {fixture, main} from './github-retention-fixture.mjs';

async function published(t) {
  const f = fixture();
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'retention-seal-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  f.git = (...args) => execFileSync('git', ['--no-replace-objects', '-C', root,
    '-c', 'core.autocrlf=false', '-c', 'user.name=Seal fixture', '-c', 'user.email=fixture@example.invalid', ...args],
  {encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe']}).trim();
  f.git('init', '-q', '-b', 'main');
  for (const [name, bytes] of f.files) {
    await fs.mkdir(path.dirname(path.join(root, name)), {recursive: true});
    await fs.writeFile(path.join(root, name), bytes);
  }
  f.git('add', '.'); f.git('commit', '-qm', 'sealed publication');
  f.commit = f.git('rev-parse', 'HEAD');
  f.git('update-ref', 'refs/remotes/origin/main', f.commit);
  f.root = root;
  const fetchMock = f.fetchResponse;
  f.fetchResponse = async (url, options) => {
    if (url.endsWith('/branches/main')) return Response.json({commit: {sha: f.commit}});
    return fetchMock(url.replace('ref=' + f.commit, 'ref=' + main), options);
  };
  return f;
}

test('publication is sealed in Git before inventory and can resume without rebuilding artifacts', async t => {
  const f = await published(t);
  const {expected, observed} = await collectGithubRetention(f.root, 'fixture', f.fetchResponse);
  assert.equal(expected.publishedCommit, f.commit);
  assert.deepEqual(githubRetentionProblems(expected, observed), []);
  assert.equal(f.calls.filter(call => call.startsWith('releases/assets/')).length, 4);
  assert.equal(f.git('status', '--porcelain'), '');
  await fs.writeFile(path.join(f.root, 'notes.md'), 'unrelated work');
  assert.deepEqual(await sealGithubRetentionCatalog(f.root), expected);
});

test('uncommitted or unpushed manifest edits stop before any GitHub request', async t => {
  for (const commit of [false, true]) {
    const f = await published(t);
    await fs.appendFile(path.join(f.root, 'release/1.2.0/release-validation.json'), '\n');
    if (commit) { f.git('add', '.'); f.git('commit', '-qm', 'not pushed'); }
    await assert.rejects(collectGithubRetention(f.root, 'fixture', f.fetchResponse), /GITHUB_PAYLOAD_NOT_SEALED/);
    assert.deepEqual(f.calls, []);
  }
});

test('a missing published Git reference and an unsealed collector input are refused', async t => {
  const f = await published(t);
  f.git('update-ref', '-d', 'refs/remotes/origin/main');
  await assert.rejects(collectGithubRetention(f.root, 'fixture', f.fetchResponse), /GITHUB_SEAL_PUBLISHED_REF_REQUIRED/);
  await assert.rejects(observeGithubRetention({...f.expected, publishedCommit: null}, 'fixture', f.fetchResponse), /GITHUB_PAYLOAD_UNSEALED/);
  assert.deepEqual(f.calls, []);
});

test('a changed local manifest during observation cannot become accepted evidence', async t => {
  const f = await published(t);
  let changed = false;
  const fetchMock = async (url, options) => {
    if (!changed && url.includes('/releases/assets/')) {
      changed = true;
      await fs.appendFile(path.join(f.root, 'release/1.2.0/release-validation.json'), '\n');
    }
    return f.fetchResponse(url, options);
  };
  await assert.rejects(collectGithubRetention(f.root, 'fixture', fetchMock), /GITHUB_SEAL_CHANGED/);
  await fs.writeFile(path.join(f.root, 'release/1.2.0/release-validation.json'), f.files.get('release/1.2.0/release-validation.json'));
  const resumed = await collectGithubRetention(f.root, 'fixture', f.fetchResponse);
  assert.deepEqual(githubRetentionProblems(resumed.expected, resumed.observed), []);
});

test('a newer remote commit requires a refreshed seal before inventory', async t => {
  const f = await published(t);
  await assert.rejects(collectGithubRetention(f.root, 'fixture', async (url, options) =>
    url.endsWith('/branches/main') ? Response.json({commit: {sha: main}}) : f.fetchResponse(url, options)), /GITHUB_SEAL_REMOTE_COMMIT/);
  assert.ok(!f.calls.some(call => call.startsWith('releases') || call.startsWith('tags')));
});

test('a resealed manifest invalidates the old observation and preserves asset identity', async t => {
  const f = await published(t);
  const first = await collectGithubRetention(f.root, 'fixture', f.fetchResponse);
  const name = 'release/1.2.0/release-validation.json';
  f.files.set(name, f.files.get(name) + '\n');
  await fs.writeFile(path.join(f.root, name), f.files.get(name));
  f.git('add', '.'); f.git('commit', '-qm', 'final manifest');
  f.commit = f.git('rev-parse', 'HEAD');
  f.git('update-ref', 'refs/remotes/origin/main', f.commit);
  const sealed = await sealGithubRetentionCatalog(f.root);
  assert.match(githubRetentionProblems(sealed, first.observed).join('\n'), /RETENTION_OBSERVATION/);
  const resumed = await collectGithubRetention(f.root, 'fixture', f.fetchResponse);
  assert.deepEqual(githubRetentionProblems(resumed.expected, resumed.observed), []);
  assert.deepEqual(resumed.expected.editions, first.expected.editions);
  assert.notEqual(resumed.observed.mainFileSha256[name], first.observed.mainFileSha256[name]);
});

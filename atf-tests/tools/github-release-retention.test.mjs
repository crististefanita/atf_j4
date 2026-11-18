import assert from 'node:assert/strict';
import test from 'node:test';
import {catalogVersions, githubRetentionProblems} from './github-release-retention.mjs';
import {observeGithubRetention} from './collect-github-release-retention.mjs';

import {fixture, main} from './github-retention-fixture.mjs';

test('the collector reads all pages and verifies bytes for both APK and checksum assets', async () => {
  const f = fixture();
  const observed = await observeGithubRetention(f.expected, 'fixture', f.fetchResponse);
  assert.deepEqual(githubRetentionProblems(f.expected, observed), []);
  assert.ok(f.calls.includes('releases:2') && f.calls.includes('tags:2'));
  assert.equal(f.calls.filter(call => call.startsWith('releases/assets/')).length, 4);
  assert.ok(!JSON.stringify(observed).includes('Bearer'));
});

test('draft releases, orphan tags, extra assets and differing downloads block retention', async () => {
  const f = fixture();
  f.githubReleases.push({id: 3, tag_name: 'v1.0.0', draft: true, published_at: null});
  f.assetLists.set(3, []);
  f.githubTags.push({name: 'v0.9.0', commit: {sha: main}});
  f.githubTags[0].commit.sha = main;
  f.assetLists.get(1).push({id: 500, name: 'extra.apk', state: 'uploaded'});
  f.bytes.set(100, 'different apk');
  f.assetLists.get(2)[1].size++;
  const observed = await observeGithubRetention(f.expected, 'fixture', f.fetchResponse);
  assert.deepEqual(githubRetentionProblems(f.expected, observed).sort(), [
    'ASSETS v1.2.0: unexpected extra.apk', 'ASSET_DOWNLOAD: WhistLab-1.2.0.apk',
    'ASSET_METADATA: WhistLab-1.1.0.apk.sha256', 'RELEASES: unexpected v1.0.0',
    'TAG_COMMIT: v1.2.0', 'TAGS: unexpected v0.9.0'
  ].sort());
});

test('retention rejects missing, invalid, future and expired observations without changing the artifact bytes', async () => {
  const f = fixture();
  const instant = Date.parse('2026-11-15T10:00:00.000Z');
  const observed = await observeGithubRetention(f.expected, 'fixture', f.fetchResponse, () => instant);
  assert.equal(observed.observedAtUtc, '2026-11-15T10:00:00.000Z');
  assert.deepEqual(githubRetentionProblems(f.expected, observed, instant + 899999), []);
  assert.deepEqual(githubRetentionProblems(f.expected, observed, instant + 900000),
    ['RETENTION_OBSERVATION_EXPIRED: 2026-11-15T10:15:00.000Z']);
  assert.match(githubRetentionProblems(f.expected, observed, instant - 1)[0], /RETENTION_OBSERVATION_TIME_FUTURE/);
  assert.deepEqual(githubRetentionProblems(f.expected, {...observed, observedAtUtc: null}, instant),
    ['RETENTION_OBSERVATION_TIME_REQUIRED']);
  for (const timestamp of ['2026-02-30T10:00:00.000Z', '2026-11-15T10:00:00', '2026-11-15T10:00:00+00:00', '']) {
    assert.deepEqual(githubRetentionProblems(f.expected, {...observed, observedAtUtc: timestamp}, instant),
      ['RETENTION_OBSERVATION_TIME_INVALID']);
  }
});

test('inventory from an account unable to inspect drafts is rejected', async () => {
  const f = fixture();
  await assert.rejects(observeGithubRetention(f.expected, 'fixture', async (url, options) =>
    url.endsWith('/' + f.expected.repository) ? Response.json({full_name: f.expected.repository, default_branch: 'main', permissions: {push: false}})
      : f.fetchResponse(url, options)), /GITHUB_INVENTORY_ACCESS/);
});

test('the collector rejects a change to main or the remote catalog during observation', async () => {
  const f = fixture();
  let mainReads = 0;
  await assert.rejects(observeGithubRetention(f.expected, 'fixture', async (url, options) => {
    if (url.endsWith('/branches/main') && ++mainReads === 2) return Response.json({commit: {sha: 'b'.repeat(40)}});
    return f.fetchResponse(url, options);
  }), /GITHUB_INVENTORY_CHANGED/);
  f.expected.evidence[0].sha256 = '0'.repeat(64);
  await assert.rejects(observeGithubRetention(f.expected, 'fixture', f.fetchResponse), /GITHUB_MAIN_DIFFERS/);
});

test('the catalog must contain exactly two distinct editions in descending order', () => {
  const valid = {schemaVersion: 2, currentVersion: '1.2.0', versions: ['1.2.0', '1.1.0']};
  assert.deepEqual(catalogVersions(valid), valid.versions);
  for (const versions of [['1.2.0'], ['1.2.0', '1.2.0'], ['1.2.0', '1.2.1'], ['1.2.0', '1.1.0', '1.0.0']]) {
    assert.throws(() => catalogVersions({...valid, versions}), /RETENTION_CATALOG/);
  }
});

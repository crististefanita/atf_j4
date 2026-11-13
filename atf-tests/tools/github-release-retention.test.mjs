import assert from 'node:assert/strict';
import test from 'node:test';
import {digest} from './evaluate-release-rag.mjs';
import {catalogVersions, githubRetentionProblems} from './github-release-retention.mjs';
import {observeGithubRetention} from './collect-github-release-retention.mjs';

const main = 'a'.repeat(40);
const repository = 'example/game';
const files = new Map([['release/index.json', 'catalog'],
  ['release/1.2.0/release-validation.json', 'current'], ['release/1.1.0/release-validation.json', 'previous']]);

function fixture() {
  const bytes = new Map();
  const assetLists = new Map();
  const editions = ['1.2.0', '1.1.0'].map((version, index) => {
    const tag = 'v' + version, apk = 'WhistLab-' + version + '.apk';
    const assets = [apk, apk + '.sha256'].map((name, offset) => {
      const id = index * 2 + offset + 100;
      const payload = name;
      bytes.set(id, payload);
      return {id, name, size: payload.length, sha256: digest(payload),
        url: 'https://github.com/' + repository + '/releases/download/' + tag + '/' + name};
    });
    assetLists.set(index + 1, assets.map(asset => ({id: asset.id, name: asset.name, size: asset.size,
      digest: 'sha256:' + asset.sha256, browser_download_url: asset.url, state: 'uploaded'})));
    return {tag, commit: String(index + 1).repeat(40), assets};
  });
  const githubReleases = editions.map((edition, index) => ({id: index + 1, tag_name: edition.tag,
    draft: false, published_at: '2026-11-13T00:00:00Z'}));
  const githubTags = editions.map(edition => ({name: edition.tag, commit: {sha: edition.commit}}));
  const expected = {repository, catalogSha256: digest(files.get('release/index.json')), editions,
    evidence: [...files].map(([path, content]) => ({path, sha256: digest(content)}))};
  const calls = [];
  const fetchResponse = async (input, options) => {
    assert.equal(options.method, undefined);
    assert.equal(options.headers.Authorization, 'Bearer fixture');
    const url = new URL(input);
    assert.equal(url.origin, 'https://api.github.com');
    const resource = url.pathname.slice(('/repos/' + repository).length).replace(/^\//, '');
    const page = url.searchParams.get('page');
    calls.push(resource + (page ? ':' + page : ''));
    if (!resource) return Response.json({full_name: repository, default_branch: 'main', permissions: {push: true}});
    if (resource === 'branches/main') return Response.json({commit: {sha: main}});
    if (resource.startsWith('contents/')) {
      const name = resource.slice(9);
      assert.equal(url.searchParams.get('ref'), main);
      return Response.json({type: 'file', path: name, encoding: 'base64', content: Buffer.from(files.get(name)).toString('base64')});
    }
    if (resource === 'releases' || resource === 'tags') {
      const rows = resource === 'releases' ? githubReleases : githubTags;
      return page === '1' ? Response.json(rows.slice(0, 1), {headers: {link: '<https://api.github.com/next>; rel="next"'}})
        : Response.json(rows.slice(1));
    }
    const releaseAssets = /^releases\/(\d+)\/assets$/.exec(resource);
    if (releaseAssets) return Response.json(assetLists.get(Number(releaseAssets[1])));
    const asset = /^releases\/assets\/(\d+)$/.exec(resource);
    if (!asset) throw Error('Unexpected mock request: ' + resource);
    assert.equal(options.headers.Accept, 'application/octet-stream');
    return new Response(bytes.get(Number(asset[1])));
  };
  return {expected, calls, fetchResponse, githubReleases, githubTags, assetLists, bytes};
}

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

test('inventory from an account unable to inspect drafts is rejected', async () => {
  const f = fixture();
  await assert.rejects(observeGithubRetention(f.expected, 'fixture', async (url, options) =>
    url.endsWith('/' + repository) ? Response.json({full_name: repository, default_branch: 'main', permissions: {push: false}})
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

import assert from 'node:assert/strict';
import {digest} from './evaluate-release-rag.mjs';

export const main = 'a'.repeat(40);
const repository = 'example/game';


export function fixture() {
  const files = new Map([['release/index.json', JSON.stringify({schemaVersion: 2, currentVersion: '1.2.0', versions: ['1.2.0', '1.1.0']})]]);
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
    const commit = String(index + 1).repeat(40);
    files.set('release/' + version + '/release-validation.json', JSON.stringify({
      release: {versionName: version, apkName: apk, apkRelativePath: apk, apkBytes: assets[0].size, apkSha256: assets[0].sha256},
      source: {sourceCommit: commit},
      publication: {repository, state: 'PUBLISHED', targetUrl: assets[0].url},
      integrity: {files: assets.map(asset => ({path: asset.name, bytes: asset.size, sha256: asset.sha256}))}
    }));
    return {tag, commit, assets};
  });
  const githubReleases = editions.map((edition, index) => ({id: index + 1, tag_name: edition.tag,
    draft: false, published_at: '2026-11-13T00:00:00Z'}));
  const githubTags = editions.map(edition => ({name: edition.tag, commit: {sha: edition.commit}}));
  const expected = {repository, publishedCommit: main, catalogSha256: digest(files.get('release/index.json')), editions,
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
  return {expected, calls, fetchResponse, githubReleases, githubTags, assetLists, bytes, files};
}


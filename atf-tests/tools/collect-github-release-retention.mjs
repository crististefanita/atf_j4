import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {isDeepStrictEqual} from 'node:util';
import {digest, loadGithubRetentionCatalog} from './evaluate-release-rag.mjs';
import {githubRetentionProblems} from './github-release-retention.mjs';

export async function observeGithubRetention(expected, token, fetchResponse = fetch) {
  if (!token) throw Error('GITHUB_TOKEN_REQUIRED');
  const api = 'https://api.github.com/repos/' + expected.repository;
  const request = async (resource, accept = 'application/vnd.github+json') => {
    const response = await fetchResponse(resource ? api + '/' + resource : api, {
      headers: {Authorization: 'Bearer ' + token, Accept: accept, 'X-GitHub-Api-Version': '2022-11-28'},
      signal: AbortSignal.timeout(30000)
    });
    if (response.status !== 200) throw Error('GITHUB_HTTP_' + response.status + ': ' + resource);
    return response;
  };
  const json = async resource => (await request(resource)).json();
  const pages = async resource => {
    const rows = [];
    for (let page = 1; ; page++) {
      const response = await request(resource + '?per_page=100&page=' + page);
      const batch = await response.json();
      if (!Array.isArray(batch)) throw Error('GITHUB_LIST_INVALID: ' + resource);
      rows.push(...batch);
      if (!/<[^>]+>;\s*rel="next"/.test(response.headers.get('link') ?? '')) return rows;
    }
  };
  const editionOrder = (left, right) => left.tag.localeCompare(right.tag) || (left.id ?? 0) - (right.id ?? 0);
  const inventory = async () => {
    const [githubReleases, githubTags] = await Promise.all([pages('releases'), pages('tags')]);
    const releases = await Promise.all(githubReleases.map(async release => ({
      id: release.id, tag: release.tag_name, draft: release.draft, publishedAt: release.published_at,
      assets: (await pages('releases/' + release.id + '/assets')).map(asset => ({
        id: asset.id, name: asset.name, size: asset.size, state: asset.state, url: asset.browser_download_url,
        sha256: typeof asset.digest === 'string' && asset.digest.startsWith('sha256:') ? asset.digest.slice(7) : null
      })).sort((left, right) => left.id - right.id)
    })));
    const tags = githubTags.map(tag => ({tag: tag.name, commit: tag.commit?.sha})).sort(editionOrder);
    return {releases: releases.sort(editionOrder), tags};
  };
  const [repository, branch] = await Promise.all([json(''), json('branches/main')]);
  const mainCommit = branch.commit?.sha;
  if (repository.full_name !== expected.repository || repository.default_branch !== 'main' || repository.permissions?.push !== true
      || !/^[a-f0-9]{40}$/.test(mainCommit ?? '')) throw Error('GITHUB_INVENTORY_ACCESS: main and access to drafts are required');

  const remoteHashes = await Promise.all(expected.evidence.map(async file => {
    const content = await json('contents/' + file.path + '?ref=' + mainCommit);
    if (content.type !== 'file' || content.path !== file.path || content.encoding !== 'base64') throw Error('GITHUB_CONTENT: ' + file.path);
    if (digest(Buffer.from(content.content, 'base64')) !== file.sha256) throw Error('GITHUB_MAIN_DIFFERS: ' + file.path);
    return [file.path, file.sha256];
  }));
  const before = await inventory();
  const releases = await Promise.all(before.releases.map(async release => ({
    ...release,
    assets: await Promise.all(release.assets.map(async asset => {
      const edition = expected.editions.find(row => row.tag === release.tag);
      if (release.draft || !edition?.assets.some(row => row.name === asset.name)) return asset;
      const response = await request('releases/assets/' + asset.id + '?retention=' + mainCommit, 'application/octet-stream');
      const bytes = Buffer.from(await response.arrayBuffer());
      return {...asset, download: {httpStatus: response.status, bytes: bytes.length, sha256: digest(bytes)}};
    }))
  })));
  const [after, latestMain] = await Promise.all([inventory(), json('branches/main')]);
  if (!isDeepStrictEqual(before, after) || latestMain.commit?.sha !== mainCommit) throw Error('GITHUB_INVENTORY_CHANGED');
  return {schemaVersion: 1, kind: 'github-release-retention', repository: expected.repository, mainCommit,
    catalogSha256: expected.catalogSha256, mainFileSha256: Object.fromEntries(remoteHashes), releaseListAccess: 'AUTHENTICATED_WRITE', releases, tags: before.tags};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    if (process.argv.length !== 4) throw Error('Use: node collect-github-release-retention.mjs <project> <new-external-json>');
    const project = await fs.realpath(process.argv[2]);
    const output = path.resolve(process.argv[3]);
    const destination = path.join(await fs.realpath(path.dirname(output)), path.basename(output));
    const relative = path.relative(project, destination);
    if (!relative || (!path.isAbsolute(relative) && relative !== '..' && !relative.startsWith('..' + path.sep))) {
      throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
    }
    const expected = await loadGithubRetentionCatalog(project);
    const observed = await observeGithubRetention(expected, process.env.GH_TOKEN || process.env.GITHUB_TOKEN);
    await fs.writeFile(destination, JSON.stringify(observed, null, 2) + '\n', {flag: 'wx'});
    const problems = githubRetentionProblems(expected, observed);
    console.log(JSON.stringify({status: problems.length ? 'BLOCKED' : 'PASS', output: destination, problems}));
    if (problems.length) process.exitCode = 2;
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}

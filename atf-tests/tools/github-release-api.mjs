const objectHosts = new Set(['release-assets.githubusercontent.com', 'objects.githubusercontent.com']);
const jsonAccept = 'application/vnd.github+json';
const binaryAccept = 'application/octet-stream';

export const githubRepository = /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/;
export const gitCommit = /^[a-f0-9]{40}$/;
export const githubId = value => Number.isSafeInteger(value) && value > 0;

export function githubReleaseApi(repository, token, fetchResponse = fetch) {
  if (!githubRepository.test(repository) || repository.split('/').some(part => part === '.' || part === '..')) {
    throw Error('GITHUB_REPOSITORY');
  }
  if (!token) throw Error('GITHUB_TOKEN_REQUIRED');
  const repoPath = '/repos/' + repository;
  const base = 'https://api.github.com' + repoPath;

  async function send(url, {authorization, accept = jsonAccept, method, body, contentType, status = 200, missing = false, download = false} = {}) {
    let response;
    try {
      response = await fetchResponse(url, {
        method, body, redirect: 'manual', signal: AbortSignal.timeout(download || contentType === binaryAccept ? 120000 : 30000),
        headers: {
          Accept: accept,
          ...(authorization ? {Authorization: 'Bearer ' + token, 'X-GitHub-Api-Version': '2022-11-28'} : {}),
          ...(contentType ? {'Content-Type': contentType} : {})
        }
      });
    } catch (cause) {
      throw new Error('GITHUB_TRANSPORT', {cause});
    }
    if (download && [301, 302, 303, 307, 308].includes(response.status)) {
      const location = new URL(response.headers.get('location') ?? '', url);
      if (location.protocol !== 'https:' || location.port || location.username || location.password || !objectHosts.has(location.hostname)) {
        throw Error('GITHUB_DOWNLOAD_REDIRECT');
      }
      return send(location.href, {accept});
    }
    if (missing && response.status === 404) return null;
    if (response.status !== status) {
      const error = Error('GITHUB_HTTP_' + response.status + ': ' + (method ?? 'GET') + ' ' + new URL(url).pathname);
      error.status = response.status;
      throw error;
    }
    return response;
  }

  const request = (resource, options) => send(base + (resource ? '/' + resource : ''), {authorization: true, ...options});
  const json = async (resource, options) => {
    const response = await request(resource, options);
    return response ? response.json() : null;
  };

  async function pages(resource) {
    const rows = [];
    for (let page = 1; ; page++) {
      const response = await request(resource + '?per_page=100&page=' + page);
      const batch = await response.json();
      if (!Array.isArray(batch)) throw Error('GITHUB_LIST_INVALID: ' + resource);
      rows.push(...batch);
      if (!/<[^>]+>;\s*rel="next"/.test(response.headers.get('link') ?? '')) return rows;
    }
  }

  async function tagCommit(tag) {
    const ref = await json('git/ref/tags/' + encodeURIComponent(tag), {missing: true});
    let object = ref?.object;
    for (let depth = 0; object?.type === 'tag' && depth < 5; depth++) {
      if (!gitCommit.test(object.sha ?? '')) break;
      object = (await json('git/tags/' + object.sha)).object;
    }
    if (object?.type !== 'commit' || !gitCommit.test(object.sha ?? '')) throw Error('GITHUB_TAG_UNVERIFIED: ' + tag);
    return object.sha;
  }

  async function content(relative, commit) {
    const data = await json('contents/' + relative + '?ref=' + commit);
    if (data.type !== 'file' || data.path !== relative || data.encoding !== 'base64') throw Error('GITHUB_CONTENT: ' + relative);
    return Buffer.from(data.content, 'base64');
  }

  async function upload(release, name, bytes) {
    const endpoint = 'https://uploads.github.com' + repoPath + '/releases/' + release.id + '/assets';
    if (release.upload_url !== endpoint + '{?name,label}') throw Error('GITHUB_UPLOAD_URL');
    await send(endpoint + '?name=' + encodeURIComponent(name), {
      authorization: true, method: 'POST', contentType: binaryAccept, body: bytes, status: 201
    });
  }

  const mutate = (resource, method, payload, status) => json(resource, {
    method, body: JSON.stringify(payload), status, contentType: 'application/json'
  });
  return {
    json, pages, content, tagCommit, upload,
    create: payload => mutate('releases', 'POST', payload, 201),
    publish: (id, payload) => mutate('releases/' + id, 'PATCH', payload, 200),
    deleteStarter: id => request('releases/assets/' + id, {method: 'DELETE', status: 204, missing: true}),
    asset: async id => Buffer.from(await (await request('releases/assets/' + id, {accept: binaryAccept, download: true})).arrayBuffer()),
    publicAsset: async url => Buffer.from(await (await send(url, {accept: binaryAccept, download: true})).arrayBuffer())
  };
}

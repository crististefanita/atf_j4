import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {digest} from './evaluate-release-rag.mjs';
import {loadPublication, publicationJournalName, withPublicationJournal} from './github-release-journal.mjs';
import {runGithubPublication} from './publish-github-release.mjs';
import {validateReleaseNotes} from './release-notes-layout.mjs';

const repository = 'example/whist';
const source = 'a'.repeat(40);
const head = 'b'.repeat(40);
const previousSource = 'c'.repeat(40);
const fingerprint = 'd'.repeat(64);
const repoPath = '/repos/' + repository;
const slug = 'https://github.com/' + repository;

function edition(versionName, commit) {
  const tag = 'v' + versionName;
  const apk = 'WhistLab-' + versionName + '.apk';
  const bytes = Buffer.from('signed APK fixture for ' + versionName);
  const payloads = new Map([[apk, bytes], [apk + '.sha256', Buffer.from(digest(bytes) + '  ' + apk + '\n')]]);
  const files = [...payloads].map(([name, value]) => ({path: name, bytes: value.length, sha256: digest(value)}));
  const targetUrl = slug + '/releases/download/' + tag + '/' + apk;
  const manifest = {
    release: {versionName, artifactKind: 'apk', apkName: apk, apkRelativePath: apk, apkBytes: bytes.length, apkSha256: digest(bytes)},
    source: {sourceCommit: commit, workingTreeFingerprint: fingerprint},
    publication: {state: 'PUBLISHED', repository, accessMode: 'AUTHENTICATED', channel: 'TEST', targetUrl},
    integrity: {files}
  };
  return {versionName, tag, apk, payloads, manifest};
}

const json = (value, status = 200, headers = {}) => Response.json(value, {status, headers});
const status = code => new Response(null, {status: code});
const stamp = '2026-11-15T00:00:00Z';

function mockGithub(active, previous) {
  const remote = {calls: [], releases: new Map(), bytes: new Map(), assets: new Map(), contents: new Map(), private: false,
    writeAccess: true, compare: true, head, tagCommit: source, latest: 20};
  let assetId = 100;
  const tagRef = tag => tag === active.tag ? remote.tagCommit : previousSource;
  const urlFor = (tag, name) => slug + '/releases/download/' + tag + '/' + name;
  const uploadUrl = id => 'https://uploads.github.com' + repoPath + '/releases/' + id + '/assets{?name,label}';
  const prior = {id: 19, tag_name: previous.tag, target_commitish: previousSource, draft: false, prerelease: false, published_at: stamp};
  remote.releases.set(prior.id, prior);
  remote.contents.set('README.md', '[Descarcă APK](' + active.manifest.publication.targetUrl + ')\n');
  remote.contents.set('release/index.json', JSON.stringify({schemaVersion: 2, currentVersion: active.versionName,
    versions: [active.versionName, previous.versionName]}));
  for (const record of [active, previous]) {
    remote.contents.set('release/' + record.versionName + '/release-validation.json', JSON.stringify(record.manifest));
  }

  function putAsset(id, name, bytes, state = 'uploaded') {
    const release = remote.releases.get(id);
    const row = {id: assetId++, name, size: bytes.length, state, digest: 'sha256:' + digest(bytes),
      browser_download_url: urlFor(release.draft ? 'untagged' : release.tag_name, name)};
    remote.assets.set(id, [...(remote.assets.get(id) ?? []), row]);
    remote.bytes.set(row.id, bytes);
    return row;
  }
  for (const [name, bytes] of previous.payloads) putAsset(prior.id, name, bytes);

  remote.fetch = async (input, options) => {
    const url = new URL(input);
    const verb = options.method ?? 'GET';
    const call = [verb, url.hostname, url.pathname + url.search].join(' ');
    remote.calls.push(call);
    assert.equal(options.redirect, 'manual');
    if (['github.com', 'release-assets.githubusercontent.com', 'example.com'].includes(url.hostname)) {
      assert.equal(options.headers.Authorization, undefined);
      if (url.hostname !== 'github.com') {
        const bytes = remote.bytes.get(Number(url.pathname.slice(1)));
        return bytes ? new Response(bytes) : status(404);
      }
      const row = [...remote.assets.values()].flat().find(asset => asset.browser_download_url === input);
      if (!row || remote.downloadMissing === row.name) return status(404);
      if (remote.downloadCorrupt === row.name) return new Response(Buffer.alloc(row.size));
      const location = 'https://' + (remote.hostileRedirect ? 'example.com' : 'release-assets.githubusercontent.com') + '/' + row.id + '?signed=fixture';
      return new Response(null, {status: 302, headers: {location}});
    }

    assert.equal(options.headers.Authorization, 'Bearer fixture');
    assert.ok(url.pathname.startsWith(repoPath));
    const resource = url.pathname.slice(repoPath.length).replace(/^\//, '');
    if (url.hostname === 'uploads.github.com') {
      assert.equal(verb, 'POST');
      assert.equal(options.headers['Content-Type'], 'application/octet-stream');
      const id = Number(resource.split('/')[1]), name = url.searchParams.get('name');
      const bytes = Buffer.from(options.body);
      assert.deepEqual(bytes, active.payloads.get(name));
      if (remote.uploadFailure?.name === name) {
        const mode = remote.uploadFailure.mode;
        remote.uploadFailure = null;
        if (mode === 'starter') {
          putAsset(id, name, Buffer.alloc(0), 'starter');
          return status(502);
        }
        if (mode === 'before') throw Error('connection interrupted before upload');
        putAsset(id, name, mode === 'wrong' ? Buffer.alloc(bytes.length) : bytes);
        if (mode === 'duplicate') return status(422);
        throw Error('connection interrupted after upload');
      }
      return json(putAsset(id, name, bytes), 201);
    }

    assert.equal(url.hostname, 'api.github.com');
    const match = /^releases\/(\d+)(\/assets)?$/.exec(resource);
    const asset = /^releases\/assets\/(\d+)$/.exec(resource);
    if (!resource) return json({full_name: repository, default_branch: 'main', permissions: {push: remote.writeAccess}, private: remote.private});
    if (resource === 'branches/main') return json({commit: {sha: remote.head}});
    if (resource.startsWith('compare/')) {
      return remote.compare ? json({status: 'ahead', merge_base_commit: {sha: source}}) : status(404);
    }
    if (resource.startsWith('git/ref/tags/')) return json({object: {type: 'commit', sha: tagRef(resource.split('/').at(-1))}});
    if (resource.startsWith('contents/')) {
      const relative = resource.slice('contents/'.length);
      assert.equal(url.searchParams.get('ref'), head);
      const value = remote.contents.get(relative);
      if (!value) return status(404);
      if (remote.changeHeadOnRead && relative === 'README.md') remote.head = 'e'.repeat(40);
      return json({path: relative, type: 'file', encoding: 'base64', content: Buffer.from(value).toString('base64')});
    }
    if (resource === 'releases/latest') {
      return remote.releases.has(remote.latest) ? json(remote.releases.get(remote.latest)) : status(404);
    }
    if (resource.startsWith('releases/tags/')) {
      const result = [...remote.releases.values()].find(row => row.tag_name === resource.split('/').at(-1) && !row.draft);
      return result ? json(result) : status(404);
    }
    if (resource === 'releases' && verb === 'GET') {
      const rows = [...remote.releases.values()].filter(row => row.id !== remote.hiddenId);
      return url.searchParams.get('page') === '1'
        ? json(rows.slice(0, 1), 200, {link: '<https://api.github.com/next>; rel="next"'})
        : json(rows.slice(1));
    }
    if (resource === 'releases' && verb === 'POST') {
      const body = JSON.parse(options.body);
      assert.equal(body.target_commitish, source);
      assert.equal(body.tag_name, active.tag);
      assert.equal(body.draft, true);
      const failure = remote.createFailure;
      remote.createFailure = null;
      if (failure === '422') return status(422);
      const release = {...body, id: 20, upload_url: uploadUrl(20)};
      remote.releases.set(release.id, release);
      if (failure === 'hidden') remote.hiddenId = release.id;
      if (failure === 'hidden' || failure === 'accepted') throw Error('connection interrupted after create');
      return json(release, 201);
    }
    if (match) {
      const id = Number(match[1]), release = remote.releases.get(id);
      if (!release) return status(404);
      if (match[2]) return json(remote.assets.get(id) ?? []);
      if (verb === 'GET') return json(release);
      assert.equal(verb, 'PATCH');
      assert.deepEqual(JSON.parse(options.body), {draft: false, tag_name: active.tag, target_commitish: source, make_latest: 'true'});
      release.draft = false;
      release.published_at = stamp;
      for (const row of remote.assets.get(id) ?? []) row.browser_download_url = urlFor(release.tag_name, row.name);
      if (remote.losePublish) {
        remote.losePublish = false;
        throw Error('connection interrupted after promotion');
      }
      return json(release);
    }
    if (asset) {
      const id = Number(asset[1]);
      if (verb === 'DELETE') {
        const rows = remote.assets.get(20);
        assert.equal(rows.find(row => row.id === id)?.state, 'starter');
        remote.assets.set(20, rows.filter(row => row.id !== id));
        remote.bytes.delete(id);
        return status(204);
      }
      assert.equal(options.headers.Accept, 'application/octet-stream');
      const bytes = remote.bytes.get(id);
      return bytes ? new Response(bytes) : status(404);
    }
    assert.fail('unexpected request ' + call);
  };
  remote.writes = () => remote.calls.filter(call => /^(POST|PATCH|DELETE) /.test(call));
  return remote;
}

async function fixture(t) {
  const parent = process.env.WHIST_PUBLICATION_TEST_ROOT
    || (process.platform === 'win32' ? 'C:/work/whist_back' : os.tmpdir());
  const root = await fs.mkdtemp(path.join(parent, 'github-publication-test-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const directory = path.join(root, 'journal'), candidateDir = path.join(root, 'candidate');
  await Promise.all([fs.mkdir(directory), fs.mkdir(candidateDir)]);
  const active = edition('1.2.0', source), previous = edition('1.1.0', previousSource);
  const notes = '# Whist\n\nNotele ediției.\n\n## Schimbări\n\n- Detaliul schimbării.\n';
  const manifest = {...active.manifest, publication: {...active.manifest.publication, state: 'PREPARED'}, integrity: {
    files: [...active.manifest.integrity.files, {path: 'release-notes.md', bytes: Buffer.byteLength(notes), sha256: digest(notes)}]
  }};
  const manifestText = JSON.stringify(manifest);
  const verdictFile = path.join(root, 'verdict.json');
  const verdict = {kind: 'ATF_RELEASE_RAG_VERDICT', schemaVersion: 1, verdict: 'READY_FOR_HUMAN_REVIEW',
    candidateManifestSha256: digest(manifestText), releaseVersion: active.versionName, sourceCommit: source,
    channel: 'TEST', workingTreeFingerprint: fingerprint};
  await Promise.all([
    ...[...active.payloads, ['release-validation.json', manifestText], ['release-notes.md', notes]]
      .map(([file, bytes]) => fs.writeFile(path.join(candidateDir, file), bytes)),
    fs.writeFile(verdictFile, JSON.stringify(verdict))
  ]);
  const candidate = await loadPublication(candidateDir, verdictFile);
  const remote = mockGithub(active, previous);
  return {
    active, previous, root, directory, candidate, candidateDir, verdict, verdictFile, remote,
    state: async () => JSON.parse(await fs.readFile(path.join(directory, publicationJournalName), 'utf8')),
    run: (mode = 'publish', now) => runGithubPublication({mode, candidate, journalDir: directory, token: 'fixture', fetchResponse: remote.fetch, now})
  };
}

test('qualification and candidate bytes are verified before publication', async t => {
  const x = await fixture(t);
  await fs.writeFile(x.verdictFile, JSON.stringify({...x.verdict, sourceCommit: previousSource}));
  await assert.rejects(loadPublication(x.candidateDir, x.verdictFile), /GITHUB_QUALIFICATION_MISMATCH/);
  await fs.writeFile(x.verdictFile, JSON.stringify(x.verdict));
  await fs.writeFile(path.join(x.candidateDir, x.active.apk), Buffer.alloc(x.active.manifest.release.apkBytes));
  await assert.rejects(loadPublication(x.candidateDir, x.verdictFile), /GITHUB_CANDIDATE_HASH/);
});

test('new release notes require a short summary and separate change bullets before any draft', async t => {
  const x = await fixture(t);
  for (const invalid of ['## Changes\n\n- Missing summary.', '# Whist\n\n' + 'x'.repeat(401) + '\n\n## Changes\n\n- Change.',
    '# Whist\n\nSummary.\n\n## Changes\n\nChanges merged into a dense paragraph.',
    '# Whist\n\nSummary.\n\n## Changes\n\n- ' + 'x'.repeat(301)]) {
    assert.throws(() => validateReleaseNotes(invalid), /GITHUB_NOTES_(SUMMARY|CHANGES)/);
  }
  for (const heading of ['Changes since 1.1.0', 'Schimbări față de 1.1.0']) {
    assert.deepEqual(validateReleaseNotes('# Whist\n\nReadable summary.\n\n## ' + heading + '\n\n- First change.\n- Second change.\n'),
      {summaryCharacters: 17, changeCount: 2});
  }
  const file = path.join(x.candidateDir, 'release-notes.md');
  const invalid = '# Whist\n\nSummary.\n\n## Changes\n\nA merged change paragraph.';
  const manifestFile = path.join(x.candidateDir, 'release-validation.json');
  const manifest = JSON.parse(await fs.readFile(manifestFile, 'utf8'));
  Object.assign(manifest.integrity.files.find(row => row.path === 'release-notes.md'), {bytes: Buffer.byteLength(invalid), sha256: digest(invalid)});
  const bytes = JSON.stringify(manifest);
  await Promise.all([fs.writeFile(file, invalid), fs.writeFile(manifestFile, bytes),
    fs.writeFile(x.verdictFile, JSON.stringify({...x.verdict, candidateManifestSha256: digest(bytes)}))]);
  await assert.rejects(loadPublication(x.candidateDir, x.verdictFile), /GITHUB_NOTES_CHANGES/);
  assert.deepEqual(x.remote.writes(), [], 'Editorial refusal precedes GitHub writes');
});

test('missing source or a tag on another commit stops before writes', async t => {
  const x = await fixture(t);
  x.remote.compare = false;
  await assert.rejects(x.run(), /GITHUB_SOURCE_NOT_ON_MAIN/);
  x.remote.compare = true;
  x.remote.tagCommit = previousSource;
  await assert.rejects(x.run(), /GITHUB_TAG_COMMIT/);
  assert.deepEqual(x.remote.writes(), []);
});

test('422 on draft creation is reconciled before a later retry', async t => {
  const x = await fixture(t);
  x.remote.createFailure = '422';
  await assert.rejects(x.run(), /GITHUB_HTTP_422/);
  assert.equal((await x.state()).createPending, false);
  assert.equal((await x.state()).releaseId, null);
  assert.equal(x.remote.writes().length, 1);
  assert.equal((await x.run()).releaseId, 20);
  assert.equal(x.remote.releases.size, 2);
});

test('lost create response recovers from authenticated pages even when lookup by tag has no draft', async t => {
  const x = await fixture(t);
  x.remote.createFailure = 'accepted';
  x.remote.uploadFailure = {name: x.active.apk, mode: 'before'};
  await assert.rejects(x.run(), /GITHUB_TRANSPORT/);
  assert.equal((await x.state()).releaseId, 20);
  assert.equal(x.remote.releases.get(20).draft, true);
  x.remote.hiddenId = 20;
  await x.run();
  assert.equal(x.remote.writes().filter(call => call === 'POST api.github.com ' + repoPath + '/releases').length, 1);
  assert.ok(x.remote.calls.every(call => !call.includes('/releases/tags/' + x.active.tag)));
});

test('unconfirmed creation waits for inventory instead of issuing a second draft', async t => {
  const x = await fixture(t);
  x.remote.createFailure = 'hidden';
  await assert.rejects(x.run(), /GITHUB_TRANSPORT/);
  assert.equal((await x.state()).createPending, true);
  await assert.rejects(x.run(), /GITHUB_CREATE_UNCONFIRMED/);
  assert.equal(x.remote.writes().length, 1);
  x.remote.hiddenId = null;
  await x.run();
  assert.equal((await x.state()).releaseId, 20);
});

test('a foreign draft or vanished known ID cannot be silently replaced', async t => {
  const x = await fixture(t);
  x.remote.releases.set(20, {id: 20, tag_name: x.active.tag, draft: true, body: 'foreign'});
  await assert.rejects(x.run(), /GITHUB_RELEASE_IDENTITY/);
  assert.deepEqual(x.remote.writes(), []);
  x.remote.releases.delete(20);
  x.remote.uploadFailure = {name: x.active.apk, mode: 'before'};
  await assert.rejects(x.run(), /GITHUB_TRANSPORT/);
  const writes = x.remote.writes().length;
  x.remote.releases.delete(20);
  await assert.rejects(x.run(), /GITHUB_RELEASE_IDENTITY/);
  assert.equal(x.remote.writes().length, writes);
});

test('an accepted upload with a lost response is hashed and reused without duplicate upload', async t => {
  const x = await fixture(t);
  x.remote.uploadFailure = {name: x.active.apk, mode: 'accepted'};
  await x.run();
  await x.run();
  assert.equal(x.remote.writes().filter(call => call.includes('?name=' + x.active.apk)
    && !call.endsWith('.sha256')).length, 1);
  assert.equal(x.remote.assets.get(20).length, 2);
});

test('422 from duplicate upload can be adopted only when the binary matches', async t => {
  const x = await fixture(t);
  x.remote.uploadFailure = {name: x.active.apk, mode: 'duplicate'};
  await x.run();
  assert.equal(x.remote.writes().filter(call => call.startsWith('PATCH ')).length, 1);
});

test('a wrong remote payload stops promotion even at the correct length', async t => {
  const x = await fixture(t);
  x.remote.uploadFailure = {name: x.active.apk, mode: 'wrong'};
  await assert.rejects(x.run(), /GITHUB_ASSET_MISMATCH/);
  const row = x.remote.assets.get(20)[0];
  row.digest = 'sha256:' + x.active.manifest.release.apkSha256;
  await assert.rejects(x.run(), /GITHUB_DOWNLOAD_MISMATCH/);
  assert.ok(x.remote.writes().every(call => /^(POST) /.test(call)));
});

test('502 starter is retried by ID only for the pending upload', async t => {
  const x = await fixture(t);
  x.remote.uploadFailure = {name: x.active.apk, mode: 'starter'};
  await assert.rejects(x.run(), /GITHUB_HTTP_502/);
  const id = x.remote.assets.get(20)[0].id;
  assert.equal((await x.state()).uploadPending, x.active.apk);
  await x.run();
  assert.ok(x.remote.writes().includes('DELETE api.github.com ' + repoPath + '/releases/assets/' + id));
  assert.equal((await x.state()).uploadPending, null);
});

test('a lost promotion response is reconciled by ID and PATCH is not repeated', async t => {
  const x = await fixture(t);
  x.remote.losePublish = true;
  await x.run();
  assert.equal((await x.run()).status, 'PUBLISHED');
  assert.equal(x.remote.writes().filter(call => call.startsWith('PATCH ')).length, 1);
});

test('an active journal lock and different candidate identity block all remote writes', async t => {
  const x = await fixture(t);
  await withPublicationJournal(x.directory, x.candidate.identity, async () => {
    await assert.rejects(x.run(), /GITHUB_JOURNAL_LOCKED/);
  });
  const identity = {...x.candidate.identity, manifestSha256: 'e'.repeat(64)};
  await assert.rejects(withPublicationJournal(x.directory, identity, () => assert.fail()), /GITHUB_JOURNAL_IDENTITY/);
  assert.deepEqual(x.remote.calls, []);
});

test('rotation requires the pinned README and complete downloads of both retained releases', async t => {
  const x = await fixture(t);
  await x.run();
  const writes = x.remote.writes().length;
  const receipt = await x.run('verify-rotation');
  assert.equal(receipt.status, 'READY_FOR_ROTATION');
  assert.equal(receipt.observedMain, head);
  assert.equal(Date.parse(receipt.expiresAtUtc) - Date.parse(receipt.observedAtUtc), 300000);
  assert.deepEqual(receipt.retained.map(row => row.tag), [x.active.tag, x.previous.tag]);
  assert.equal(receipt.retained[0].downloads[0].sha256, x.active.manifest.release.apkSha256);
  assert.equal(receipt.retained[0].downloads[0].url, x.active.manifest.publication.targetUrl);
  assert.equal(x.remote.calls.filter(call => call.startsWith('GET release-assets.githubusercontent.com ')).length, 4);
  assert.equal(x.remote.writes().length, writes);
  assert.deepEqual((await x.state()).rotation, receipt);
});

test('rotation denies a stale README, missing download, wrong bytes or changed main', async t => {
  const x = await fixture(t);
  await x.run();
  await x.run('verify-rotation');
  const writes = x.remote.writes().length;
  const readme = x.remote.contents.get('README.md');
  x.remote.contents.set('README.md', '[APK](' + x.previous.manifest.publication.targetUrl + ')\n');
  await assert.rejects(x.run('verify-rotation'), /GITHUB_README_APK_LINK/);
  assert.equal((await x.state()).rotation, null);
  x.remote.contents.set('README.md', readme);
  x.remote.downloadMissing = x.active.apk;
  await assert.rejects(x.run('verify-rotation'), /GITHUB_HTTP_404/);
  x.remote.downloadMissing = null;
  x.remote.downloadCorrupt = x.active.apk + '.sha256';
  await assert.rejects(x.run('verify-rotation'), /GITHUB_DOWNLOAD_MISMATCH/);
  x.remote.downloadCorrupt = null;
  x.remote.changeHeadOnRead = true;
  await assert.rejects(x.run('verify-rotation'), /GITHUB_ROTATION_CHANGED/);
  assert.equal((await x.state()).rotation, null);
  assert.equal(x.remote.writes().length, writes);
});

test('rotation correlates authenticated bytes for a private repository and blocks foreign CDN redirects', async t => {
  const x = await fixture(t);
  await x.run();
  x.remote.hostileRedirect = true;
  await assert.rejects(x.run('verify-rotation'), /GITHUB_DOWNLOAD_REDIRECT/);
  assert.ok(x.remote.calls.every(call => !call.startsWith('GET example.com ')));
  x.remote.private = true;
  const receipt = await x.run('verify-rotation');
  assert.equal(receipt.retained[0].downloads[0].channel, 'GITHUB_API_AUTHENTICATED');
});

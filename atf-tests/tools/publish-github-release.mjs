import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {digest, strictJson} from './evaluate-release-rag.mjs';
import {evidenceFreshness, observationDeadline} from './release-evidence-freshness.mjs';
import {githubReleaseApi, githubId, gitCommit} from './github-release-api.mjs';
import {catalogVersions, expectedEdition} from './github-release-retention.mjs';
import {loadPublication, publicationJournalName, sameBytes, withPublicationJournal} from './github-release-journal.mjs';

const downloadPrefix = repository => 'https://github.com/' + repository + '/releases/download/';

function publicationUrl(identity, name) {
  return downloadPrefix(identity.repository) + identity.tag + '/' + name;
}

async function mainCommit(api) {
  const commit = (await api.json('branches/main')).commit?.sha;
  if (!gitCommit.test(commit ?? '')) throw Error('GITHUB_MAIN_UNVERIFIED');
  return commit;
}

function candidateMarker(identity) {
  return '<!-- atf-publication:' + digest(JSON.stringify(identity)) + ' -->';
}

function hasCandidateMarker(release, identity) {
  return typeof release?.body === 'string' && release.body.split(/\r?\n/).includes(candidateMarker(identity));
}

function ownedRelease(release, identity, id) {
  if (!githubId(release?.id) || (id !== null && release.id !== id) || !hasCandidateMarker(release, identity)
      || release.tag_name !== identity.tag || release.prerelease !== false
      || typeof release.draft !== 'boolean' || release.target_commitish !== identity.commit) {
    throw Error('GITHUB_RELEASE_IDENTITY: ' + identity.tag);
  }
  return release;
}

async function findRelease(api, identity) {
  const rows = (await api.pages('releases')).filter(row => row.tag_name === identity.tag || hasCandidateMarker(row, identity));
  if (rows.length > 1) throw Error('GITHUB_RELEASE_AMBIGUOUS: ' + identity.tag);
  return rows.length ? ownedRelease(rows[0], identity, null) : null;
}

async function knownRelease(api, identity, id) {
  return ownedRelease(await api.json('releases/' + id, {missing: true}), identity, id);
}

async function releaseForPublication(api, candidate, current, save) {
  const {identity, notes} = candidate;
  if (current().releaseId) return knownRelease(api, identity, current().releaseId);
  const remember = async release => {
    await save({releaseId: release.id, createPending: false});
    return release;
  };
  const found = await findRelease(api, identity);
  if (found) return remember(found);
  if (current().createPending) throw Error('GITHUB_CREATE_UNCONFIRMED: ' + identity.tag);
  await save({createPending: true});
  let created;
  try {
    created = await api.create({
      tag_name: identity.tag, target_commitish: identity.commit, draft: true, prerelease: false,
      name: 'Whist Lab ' + identity.tag.slice(1), body: candidateMarker(identity) + '\n\n' + notes
    });
  } catch (error) {
    const recovered = await findRelease(api, identity);
    if (recovered) return remember(recovered);
    if (error.status === 422) await save({createPending: false});
    throw error;
  }
  return remember(ownedRelease(created, identity, null));
}

async function listedAssets(api, id, expected) {
  const rows = await api.pages('releases/' + id + '/assets');
  if (new Set(rows.map(row => row.name)).size !== rows.length || new Set(rows.map(row => row.id)).size !== rows.length
      || rows.some(row => !githubId(row.id) || !expected.some(asset => asset.name === row.name))) {
    throw Error('GITHUB_ASSET_INVENTORY: ' + id);
  }
  return rows;
}

async function verifyAsset(api, row, asset, url, useDirectDownload = false) {
  if (row?.state !== 'uploaded' || row.size !== asset.size
      || (row.digest != null && row.digest !== 'sha256:' + asset.sha256)
      || (url && row.browser_download_url !== url)) throw Error('GITHUB_ASSET_MISMATCH: ' + asset.name);
  const bytes = useDirectDownload ? await api.publicAsset(url) : await api.asset(row.id);
  if (!sameBytes(bytes, asset)) throw Error('GITHUB_DOWNLOAD_MISMATCH: ' + asset.name);
  return row;
}

async function attachAsset(api, release, {identity, payloads}, asset, current, save) {
  const lookup = async () => (await listedAssets(api, release.id, identity.assets)).find(row => row.name === asset.name);
  let row = await lookup();
  if (row?.state === 'starter' && row.size === 0 && current().uploadPending === asset.name) {
    if (!(await knownRelease(api, identity, release.id)).draft) throw Error('GITHUB_DRAFT_REQUIRED');
    await api.deleteStarter(row.id);
    row = await lookup();
  }
  if (!row) {
    if (!(await knownRelease(api, identity, release.id)).draft) throw Error('GITHUB_DRAFT_REQUIRED');
    await save({uploadPending: asset.name});
    try {
      await api.upload(release, asset.name, payloads.get(asset.name));
    } catch (error) {
      row = await lookup();
      if (row?.state !== 'uploaded') throw error;
    }
    row ??= await lookup();
  }
  await verifyAsset(api, row, asset);
  if (current().uploadPending === asset.name) await save({uploadPending: null});
}

async function verifiedDownloads(api, release, edition, publicRepository) {
  const rows = await listedAssets(api, release.id, edition.assets);
  const downloads = [];
  for (const asset of edition.assets) {
    const url = publicationUrl(edition, asset.name);
    const row = await verifyAsset(api, rows.find(item => item.name === asset.name), asset, url, publicRepository);
    downloads.push({assetId: row.id, url, bytes: asset.size, sha256: asset.sha256, httpStatus: 200,
      channel: publicRepository ? 'HTTPS_DIRECT' : 'GITHUB_API_AUTHENTICATED'});
  }
  return downloads;
}

async function publish(api, candidate, current, save) {
  const {identity} = candidate;
  const head = await mainCommit(api);
  const difference = await api.json('compare/' + identity.commit + '...' + head + '?per_page=1', {missing: true});
  if (!['ahead', 'identical'].includes(difference?.status) || difference.merge_base_commit?.sha !== identity.commit) {
    throw Error('GITHUB_SOURCE_NOT_ON_MAIN: ' + identity.commit);
  }
  if (await api.tagCommit(identity.tag) !== identity.commit) throw Error('GITHUB_TAG_COMMIT: ' + identity.tag);
  let release = await releaseForPublication(api, candidate, current, save);
  if (release.draft) {
    const pending = identity.assets.find(asset => asset.name === current().uploadPending);
    const ordered = pending ? [pending, ...identity.assets.filter(asset => asset !== pending)] : identity.assets;
    for (const asset of ordered) await attachAsset(api, release, candidate, asset, current, save);
    if (await api.tagCommit(identity.tag) !== identity.commit) throw Error('GITHUB_TAG_COMMIT: ' + identity.tag);
    try {
      await api.publish(release.id, {draft: false, tag_name: identity.tag, target_commitish: identity.commit, make_latest: 'true'});
    } catch (error) {
      if ((await knownRelease(api, identity, release.id)).draft) throw error;
    }
    release = await knownRelease(api, identity, release.id);
  }
  if (release.draft || !release.published_at) throw Error('GITHUB_PUBLICATION_UNCONFIRMED');
  const rows = await listedAssets(api, release.id, identity.assets);
  for (const asset of identity.assets) {
    await verifyAsset(api, rows.find(row => row.name === asset.name), asset, publicationUrl(identity, asset.name));
  }
  await save({uploadPending: null, publishedAtUtc: release.published_at});
  return {status: 'PUBLISHED', releaseId: release.id};
}

async function verifyRotation(api, identity, current, save, publicRepository, now) {
  if (!current().releaseId) throw Error('GITHUB_PUBLICATION_REQUIRED');
  const release = await knownRelease(api, identity, current().releaseId);
  const latest = await api.json('releases/latest', {missing: true});
  if (release.draft || !release.published_at || latest?.id !== release.id) throw Error('GITHUB_LATEST_MISMATCH');
  const observedMain = await mainCommit(api);
  const [readme, catalog] = await Promise.all([
    api.content('README.md', observedMain), api.content('release/index.json', observedMain)
  ]);
  const versions = catalogVersions(JSON.parse(catalog.toString('utf8')));
  if ('v' + versions[0] !== identity.tag) throw Error('GITHUB_ROTATION_CATALOG');
  const prefix = downloadPrefix(identity.repository);
  const apkUrl = publicationUrl(identity, identity.assets[0].name);
  const links = [...readme.toString('utf8').matchAll(/\]\((https:\/\/[^\s()]+\.apk)\)/g)]
    .map(match => match[1]).filter(url => url.startsWith(prefix));
  if (links.length === 0 || links.some(url => url !== apkUrl)) throw Error('GITHUB_README_APK_LINK');

  const manifests = await Promise.all(versions.map(async version => {
    const raw = await api.content('release/' + version + '/release-validation.json', observedMain);
    const manifest = JSON.parse(raw.toString('utf8'));
    return {edition: expectedEdition(version, manifest), accessMode: manifest.publication.accessMode};
  }));
  const [active, previous] = manifests.map(manifest => manifest.edition);
  if (active.repository !== identity.repository || active.commit !== identity.commit
      || manifests[0].accessMode !== identity.accessMode || previous.repository !== identity.repository
      || active.assets.some((asset, index) => asset.name !== identity.assets[index].name
        || asset.size !== identity.assets[index].size || asset.sha256 !== identity.assets[index].sha256)) {
    throw Error('GITHUB_ROTATION_MANIFEST');
  }
  const previousRelease = await api.json('releases/tags/' + previous.tag, {missing: true});
  if (!githubId(previousRelease?.id) || previousRelease.id === release.id || previousRelease.draft !== false
      || previousRelease.tag_name !== previous.tag || !previousRelease.published_at) throw Error('GITHUB_PREVIOUS_RELEASE');
  if (await api.tagCommit(active.tag) !== active.commit || await api.tagCommit(previous.tag) !== previous.commit) {
    throw Error('GITHUB_ROTATION_TAG');
  }
  const downloads = await verifiedDownloads(api, release, active, publicRepository);
  const previousDownloads = await verifiedDownloads(api, previousRelease, previous, publicRepository);
  if (await mainCommit(api) !== observedMain || (await api.json('releases/latest')).id !== release.id) {
    throw Error('GITHUB_ROTATION_CHANGED');
  }
  const checked = now();
  const observedAtUtc = new Date(checked).toISOString();
  const expiresAtUtc = observationDeadline(observedAtUtc, evidenceFreshness.githubRotationMaxAgeSeconds, 'GITHUB_ROTATION', checked);
  const receipt = {
    status: 'READY_FOR_ROTATION', observedAtUtc, expiresAtUtc, observedMain, readmeSha256: digest(readme),
    catalogSha256: digest(catalog), retained: [
      {tag: active.tag, releaseId: release.id, downloads}, {tag: previous.tag, releaseId: previousRelease.id, downloads: previousDownloads}
    ]
  };
  await save({rotation: receipt});
  return receipt;
}

export async function runGithubPublication({mode, candidate, journalDir, token, fetchResponse, now = Date.now}) {
  if (!['publish', 'verify-rotation'].includes(mode)) throw Error('GITHUB_PUBLICATION_MODE');
  const {identity} = candidate;
  const api = githubReleaseApi(identity.repository, token, fetchResponse);
  return withPublicationJournal(journalDir, identity, async (current, save) => {
    if (mode === 'verify-rotation') await save({rotation: null});
    const repo = await api.json('');
    if (repo.full_name !== identity.repository || repo.default_branch !== 'main'
        || repo.permissions?.push !== true || typeof repo.private !== 'boolean') throw Error('GITHUB_REPOSITORY_ACCESS');
    if (repo.private && identity.accessMode === 'ANONYMOUS') throw Error('GITHUB_ANONYMOUS_ACCESS');
    return mode === 'publish' ? publish(api, candidate, current, save) : verifyRotation(api, identity, current, save, !repo.private, now);
  });
}

function within(root, target) {
  const relative = path.relative(root, target);
  return relative === '' || (!path.isAbsolute(relative) && relative !== '..' && !relative.startsWith('..' + path.sep));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    if (process.argv.length !== 7) throw Error('Use: node publish-github-release.mjs <publish|verify-rotation> <project> <qualified-candidate> <verdict-json> <external-journal-dir>');
    const [mode, projectArg, candidateArg, verdictArg, journalArg] = process.argv.slice(2);
    const [project, candidateDir, verdictFile, journalDir] = await Promise.all(
      [projectArg, candidateArg, verdictArg, journalArg].map(file => fs.realpath(file)));
    if (within(project, journalDir) || within(candidateDir, journalDir)
        || path.join(journalDir, publicationJournalName) === verdictFile) throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
    const candidate = await loadPublication(candidateDir, verdictFile);
    const result = await runGithubPublication({mode, candidate, journalDir, token: process.env.GH_TOKEN || process.env.GITHUB_TOKEN});
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

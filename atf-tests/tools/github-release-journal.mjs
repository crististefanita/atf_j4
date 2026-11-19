import fs from 'node:fs/promises';
import path from 'node:path';
import {randomUUID} from 'node:crypto';
import {digest, regularFile, strictJson} from './evaluate-release-rag.mjs';
import {gitCommit, githubId, githubRepository} from './github-release-api.mjs';
import {validateReleaseNotes} from './release-notes-layout.mjs';

export const publicationJournalName = 'github-publication.json';
const sha256 = /^[a-f0-9]{64}$/;
const versionName = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)$/;
const accessModes = ['AUTHENTICATED', 'ANONYMOUS'];

export function sameBytes(bytes, asset) {
  return bytes.length === asset.size && digest(bytes) === asset.sha256;
}

export async function loadPublication(candidate, verdictFile) {
  const [manifest, {value: verdict}] = await Promise.all([
    strictJson(await regularFile(candidate, 'release-validation.json')), strictJson(verdictFile)
  ]);
  const {release, source, publication, integrity} = manifest.value;
  const version = release?.versionName;
  const apk = 'WhistLab-' + version + '.apk';
  const tag = 'v' + version;
  const commit = source?.sourceCommit;
  const repository = publication?.repository;
  const accessMode = publication?.accessMode;
  const url = 'https://github.com/' + repository + '/releases/download/' + tag + '/' + apk;
  if (!versionName.test(version ?? '') || release.apkName !== apk || release.apkRelativePath !== apk
      || release.artifactKind !== 'apk' || !gitCommit.test(commit ?? '') || !githubRepository.test(repository ?? '')
      || !accessModes.includes(accessMode) || publication.targetUrl !== url
      || publication.state !== 'PREPARED') throw Error('GITHUB_CANDIDATE');
  if (verdict.kind !== 'ATF_RELEASE_RAG_VERDICT' || verdict.schemaVersion !== 1 || verdict.verdict !== 'READY_FOR_HUMAN_REVIEW'
      || verdict.candidateManifestSha256 !== manifest.sha256 || verdict.releaseVersion !== version
      || verdict.sourceCommit !== commit || verdict.channel !== publication.channel
      || !sha256.test(source.workingTreeFingerprint ?? '') || verdict.workingTreeFingerprint !== source.workingTreeFingerprint) {
    throw Error('GITHUB_QUALIFICATION_MISMATCH');
  }

  const verifiedFile = async name => {
    const rows = integrity?.files?.filter(row => row.path === name) ?? [];
    const bytes = await fs.readFile(await regularFile(candidate, name));
    const asset = {name, size: rows[0]?.bytes, sha256: rows[0]?.sha256};
    if (rows.length !== 1 || !sameBytes(bytes, asset)) throw Error('GITHUB_CANDIDATE_HASH: ' + name);
    return {asset, bytes};
  };

  const [apkFile, checksum, notes] = await Promise.all([
    verifiedFile(apk), verifiedFile(apk + '.sha256'), verifiedFile('release-notes.md')
  ]);
  if (apkFile.asset.size !== release.apkBytes || apkFile.asset.sha256 !== release.apkSha256
      || checksum.bytes.toString('utf8').trim() !== release.apkSha256 + '  ' + apk
      || !notes.bytes.toString('utf8').trim()) throw Error('GITHUB_CANDIDATE_ASSETS');
  validateReleaseNotes(notes.bytes.toString('utf8'));
  return {
    identity: {repository, tag, commit, accessMode, manifestSha256: manifest.sha256, assets: [apkFile.asset, checksum.asset]},
    notes: notes.bytes.toString('utf8').trimEnd(),
    payloads: new Map([[apk, apkFile.bytes], [checksum.asset.name, checksum.bytes]])
  };
}

async function replaceJournal(file, state) {
  const temporary = file + '.' + randomUUID() + '.tmp';
  try {
    const handle = await fs.open(temporary, 'wx');
    try {
      await handle.writeFile(JSON.stringify(state, null, 2) + '\n');
      await handle.sync();
    } finally {
      await handle.close();
    }
    await fs.rename(temporary, file);
  } finally {
    await fs.unlink(temporary).catch(error => { if (error.code !== 'ENOENT') throw error; });
  }
}

export async function withPublicationJournal(directory, identity, operation) {
  const file = path.join(directory, publicationJournalName);
  const lock = file + '.lock';
  const lease = await fs.open(lock, 'wx').catch(error => {
    if (error.code === 'EEXIST') throw Error('GITHUB_JOURNAL_LOCKED');
    throw error;
  });
  try {
    await lease.writeFile(JSON.stringify({pid: process.pid, startedAtUtc: new Date().toISOString()}) + '\n');
    let state = await strictJson(file).then(document => document.value).catch(error => {
      if (error.code === 'ENOENT') return null;
      throw error;
    });
    if (state && (state.schemaVersion !== 1 || JSON.stringify(state.identity) !== JSON.stringify(identity)
        || (state.releaseId !== null && !githubId(state.releaseId))
        || typeof state.createPending !== 'boolean' || (state.releaseId !== null && state.createPending)
        || (state.uploadPending !== null && (!state.releaseId || !identity.assets.some(asset => asset.name === state.uploadPending))))) {
      throw Error('GITHUB_JOURNAL_IDENTITY');
    }
    const save = async updates => {
      const next = {...state, ...updates, updatedAtUtc: new Date().toISOString()};
      await replaceJournal(file, next);
      state = next;
    };
    if (!state) await save({schemaVersion: 1, identity, releaseId: null, createPending: false, uploadPending: null});
    return await operation(() => state, save);
  } finally {
    await lease.close();
    await fs.unlink(lock);
  }
}

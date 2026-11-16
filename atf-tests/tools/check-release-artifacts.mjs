import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {readApkArtifact, regularFile, strictJson} from './evaluate-release-rag.mjs';

const versionPattern = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:-[0-9A-Za-z.-]+)?$/;

export async function checkReleaseArtifacts(projectRoot) {
  const root = await fs.realpath(projectRoot);
  const {value: catalog} = await strictJson(await regularFile(root, 'release/index.json'));
  const versions = catalog.versions;
  if (catalog.schemaVersion !== 2 || !Array.isArray(versions) || !versions.length
      || catalog.currentVersion !== versions[0] || new Set(versions).size !== versions.length
      || !versions.every(version => typeof version === 'string' && versionPattern.test(version))) {
    throw Error('RELEASE-PREFLIGHT-CATALOG');
  }

  const artifacts = [];
  for (const version of versions) {
    const file = await regularFile(root, 'release/' + version + '/release-validation.json');
    artifacts.push(await verifyArtifact(version, path.dirname(file), await strictJson(file)));
  }
  if (catalog.candidateVersion !== undefined) {
    const version = catalog.candidateVersion;
    if (typeof version !== 'string' || !versionPattern.test(version) || versions.includes(version))
      throw Error('RELEASE-PREFLIGHT-CANDIDATE');
    const manifest = await strictJson(await regularFile(root, 'release/candidate.json'));
    const publication = manifest.value.publication;
    if (publication?.artifactSource === 'MAIN_BRANCH') {
      if (publication.state !== 'PREPARED' || publication.accessMode !== 'ANONYMOUS'
          || publication.targetUrl !== undefined) throw Error('RELEASE-PREFLIGHT-CANDIDATE');
      const guide = await regularFile(root, 'release/' + version + '/README.md');
      artifacts.push({ ...await verifyArtifact(version, path.dirname(guide), manifest), candidate: true });
    }
  }
  return artifacts;
}

async function verifyArtifact(version, directory, {value: manifest, sha256: manifestSha256}) {
  const apk = 'WhistLab-' + version + '.apk';
  if (manifest.release?.versionName !== version || manifest.release.apkName !== apk
      || manifest.release.apkRelativePath !== apk || manifest.release.artifactKind !== 'apk') {
    throw Error('RELEASE-PREFLIGHT-IDENTITY:' + version);
  }
  const artifact = await readApkArtifact(directory, manifest.release);
  const inventory = manifest.integrity?.files;
  for (const observed of [artifact.apk, artifact.sidecar]) {
    const matches = Array.isArray(inventory) ? inventory.filter(entry => entry?.path === observed.path) : [];
    if (matches.length !== 1 || matches[0].bytes !== observed.bytes || matches[0].sha256 !== observed.sha256) {
      throw Error('RELEASE-PREFLIGHT-INVENTORY:' + version + ':' + observed.path);
    }
  }
  return {version, manifestSha256, ...artifact};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    if (process.argv.length !== 3) throw Error('Use: node check-release-artifacts.mjs <qualification-snapshot>');
    console.log(JSON.stringify({status: 'PASS', artifacts: await checkReleaseArtifacts(process.argv[2])}));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

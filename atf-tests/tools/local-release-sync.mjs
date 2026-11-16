import fs from 'node:fs/promises';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {digest, regularFile, strictJson} from './evaluate-release-rag.mjs';
import {catalogVersions} from './github-release-retention.mjs';
import {gitBlob} from './git-snapshot-blobs.mjs';
import {within} from './release-timing.mjs';

const manifestName = 'release-validation.json';
const projectionKind = 'ATF_LOCAL_PUBLISHED_RELEASE';
const versionPattern = /^(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)$/;

function localIndex(version, manifestSha256) {
  return JSON.stringify({kind: projectionKind, schemaVersion: 2, currentVersion: version, versions: [version], manifestSha256}, null, 2) + '\n';
}

function localReadme(version) {
  return '# Versiunea locală Whist Lab\n\n## Pe scurt\n\n'
    + 'Acest dosar local conține ediția publicată curentă și dovezile ei.\n\n'
    + '[Dosarul ediției ' + version + '](' + version + '/README.md) · '
    + '[Rapoartele Extent](' + version + '/reports/README.md)\n';
}

async function inventory(root, prefix = '') {
  const files = [];
  for (const entry of await fs.readdir(path.join(root, prefix), {withFileTypes: true})) {
    const name = prefix + entry.name;
    if (entry.isDirectory()) {
      const children = await inventory(root, name + '/');
      files.push(...(children.length ? children : [name + '/']));
    }
    else if (entry.isFile()) files.push(name);
    else throw Error('LOCAL_RELEASE_NON_REGULAR:' + name);
  }
  return files.sort();
}

async function qualifiedPayload(root, expectedManifestHash) {
  const doc = await strictJson(await regularFile(root, manifestName));
  const version = doc.value.release?.versionName;
  if (doc.sha256 !== expectedManifestHash || !versionPattern.test(version ?? '')
      || doc.value.publication?.state !== 'PUBLISHED' || !Array.isArray(doc.value.integrity?.files)) {
    throw Error('LOCAL_RELEASE_MANIFEST');
  }
  const rows = doc.value.integrity.files;
  if (!['README.md', 'reports/README.md'].every(name => rows.some(row => row.path === name))) {
    throw Error('LOCAL_RELEASE_DOCUMENTATION');
  }
  const expected = [manifestName, ...rows.map(row => row.path)].sort();
  if (JSON.stringify(await inventory(root)) !== JSON.stringify(expected)) throw Error('LOCAL_RELEASE_PAYLOAD_INVENTORY');
  return {version, manifestHash: doc.sha256, rows};
}

async function verifyFile(root, row) {
  const bytes = await fs.readFile(await regularFile(root, row.path));
  if (bytes.length !== row.bytes || digest(bytes) !== row.sha256) throw Error('LOCAL_RELEASE_PAYLOAD_HASH:' + row.path);
  return bytes;
}

async function inspectTarget(target) {
  const stat = await fs.lstat(target).catch(error => error.code === 'ENOENT' ? null : Promise.reject(error));
  if (!stat) return null;
  if (!stat.isDirectory() || stat.isSymbolicLink() || await fs.realpath(target) !== target) throw Error('LOCAL_RELEASE_TARGET');
  const files = await inventory(target);
  if (!files.length) return 'EMPTY';
  const index = files.includes('index.json') ? (await strictJson(path.join(target, 'index.json'))).value : null;
  if (index?.kind === projectionKind) {
    const version = index.currentVersion;
    if (!versionPattern.test(version ?? '') ||
        (await fs.readFile(path.join(target, 'index.json'), 'utf8')) !== localIndex(version, index.manifestSha256) ||
        (await fs.readFile(path.join(target, 'README.md'), 'utf8')) !== localReadme(version)) throw Error('LOCAL_RELEASE_MODIFIED_INDEX');
    const previous = await qualifiedPayload(path.join(target, version), index.manifestSha256);
    if (previous.version !== version || files.some(name => name !== 'index.json' && name !== 'README.md' && !name.startsWith(version + '/'))) {
      throw Error('LOCAL_RELEASE_MODIFIED_INVENTORY');
    }
    for (const row of previous.rows) await verifyFile(path.join(target, version), row);
    return previous.manifestHash;
  }
  // One-time migration permits a tracked historical release with locally removed files.
  const cwd = path.dirname(target);
  const trackedCheckout = await fs.lstat(path.join(cwd, '.git')).catch(error => error.code === 'ENOENT' ? null : Promise.reject(error));
  if (!trackedCheckout) throw Error('LOCAL_RELEASE_EXISTING_CHANGES');
  const git = (...args) => execFileSync('git', ['--no-replace-objects', '-C', cwd, ...args], {encoding: 'utf8', timeout: 30000});
  const checkout = await fs.realpath(git('rev-parse', '--show-toplevel').trim());
  const changes = git('status', '--porcelain=v1', '-z', '--untracked-files=all', '--', 'release/').split('\0').filter(Boolean);
  const tracked = new Set(git('ls-files', '-z', '--', 'release/').split('\0'));
  if (checkout !== cwd || changes.some(record => !record.startsWith(' D release/'))
      || files.some(name => !tracked.has('release/' + name))) throw Error('LOCAL_RELEASE_EXISTING_CHANGES');
  return 'TRACKED';
}

export async function syncLocalRelease({repository, candidate, proof, destination, stagingParent}) {
  if (proof.status !== 'PASS' || proof.cleanupEligible !== true || !proof.publication) throw Error('LOCAL_RELEASE_PUBLICATION_REQUIRED');
  const current = catalogVersions(JSON.parse(gitBlob(repository, proof.publication.checkedOriginMain, 'release/index.json')))[0];
  if (current !== proof.releaseVersion) throw Error('LOCAL_RELEASE_NOT_CURRENT');
  const target = path.join(await fs.realpath(path.dirname(destination)), path.basename(destination));
  const stageRoot = await fs.realpath(stagingParent);
  if (path.basename(target) !== 'release' || within(repository, target) || within(target, repository)
      || within(target, stageRoot) || stageRoot === target || within(repository, stageRoot)
      || within(candidate, target) || within(target, candidate)) throw Error('LOCAL_RELEASE_PERIMETER');

  const previous = await inspectTarget(target);
  const payload = await qualifiedPayload(candidate, proof.manifestSha256);
  if (payload.version !== current) throw Error('LOCAL_RELEASE_MANIFEST_VERSION');
  if (previous === payload.manifestHash) return {status: 'UNCHANGED', version: current, destination: target};

  const stage = await fs.mkdtemp(path.join(stageRoot, 'atf-local-release-'));
  const next = path.join(stage, 'next'), old = path.join(stage, 'previous');
  const edition = path.join(next, current);
  let replaced = false;
  try {
    await fs.mkdir(edition, {recursive: true});
    await fs.writeFile(path.join(next, 'index.json'), localIndex(current, payload.manifestHash), {flag: 'wx'});
    await fs.writeFile(path.join(next, 'README.md'), localReadme(current), {flag: 'wx'});
    const manifest = await fs.readFile(await regularFile(candidate, manifestName));
    if (digest(manifest) !== payload.manifestHash) throw Error('LOCAL_RELEASE_MANIFEST_CHANGED');
    await fs.writeFile(path.join(edition, manifestName), manifest, {flag: 'wx'});
    for (const row of payload.rows) {
      const bytes = await verifyFile(candidate, row);
      const targetFile = path.join(edition, ...row.path.split('/'));
      await fs.mkdir(path.dirname(targetFile), {recursive: true});
      await fs.writeFile(targetFile, bytes, {flag: 'wx'});
    }
    if (await inspectTarget(target) !== previous) throw Error('LOCAL_RELEASE_CONCURRENT_CHANGE');
    if (previous !== null) {
      await fs.rename(target, old);
      replaced = true;
    }
    try {
      await fs.rename(next, target);
    } catch (error) {
      if (replaced) {
        await fs.rename(old, target).catch(rollback => {
          throw new AggregateError([error, rollback], 'LOCAL_RELEASE_RECOVERY_REQUIRED:' + old);
        });
        replaced = false;
      }
      throw error;
    }
    replaced = false;
    return {status: 'SYNCED', version: current, destination: target};
  } finally {
    if (!replaced) await fs.rm(stage, {recursive: true, force: true});
  }
}

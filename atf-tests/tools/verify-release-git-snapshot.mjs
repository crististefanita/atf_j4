import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {parseArgs} from 'node:util';
import {componentInputMap, digest, inputFingerprint, readApkArtifact, regularFile, strictJson} from './evaluate-release-rag.mjs';
import {assertAncestor, commitId, gitBlob, readGitBlobs, trackedEntries} from './git-snapshot-blobs.mjs';
import {applyEol, eolRecipe} from './git-snapshot-eol.mjs';
import {syncLocalRelease} from './local-release-sync.mjs';
import {within, writeTimingReceipt, QualificationTiming} from './release-timing.mjs';

const kind = 'ATF_RELEASE_GIT_SNAPSHOT';
const hashPattern = /^[a-f0-9]{64}$/;

async function exactEol(blob, expectedHash, recipe, qualifiedRoot, name) {
  if (recipe) {
    const restored = applyEol(blob, recipe);
    if (digest(restored) !== expectedHash) throw Error('EOL_RECIPE_HASH');
    return recipe;
  }
  if (blob.includes(0) || !blob.includes(10)) return null;
  for (const kind of ['LF', 'CRLF']) {
    try { if (digest(applyEol(blob, {kind})) === expectedHash) return {kind}; }
    catch { return null; }
  }
  if (!qualifiedRoot) return null;
  const qualified = await fs.readFile(await regularFile(qualifiedRoot, name));
  if (digest(qualified) !== expectedHash) throw Error('QUALIFIED_FILE_HASH');
  return eolRecipe(blob, qualified);
}

async function verifySources(repository, identity, expected, qualifiedRoot, prior) {
  if (prior && (prior.kind !== kind || prior.schemaVersion !== 1 || prior.sourceCommit !== identity.sourceCommit
      || prior.sourceFingerprint !== identity.sourceFingerprint)) throw Error('GIT_SNAPSHOT_RECIPE_IDENTITY');
  const supplied = new Map();
  for (const recipe of prior?.endings ?? []) {
    if (!expected.has(recipe.path) || supplied.has(recipe.path) || recipe.qualifiedSha256 !== expected.get(recipe.path)
        || !hashPattern.test(recipe.gitBlobSha256 ?? '')) throw Error('GIT_SNAPSHOT_RECIPE_PATH');
    supplied.set(recipe.path, recipe);
  }
  const tree = trackedEntries(repository, identity.sourceCommit);
  const problems = [], endings = [];
  const files = new Map();
  for (const [name, entry] of tree) {
    if (!expected.has(name)) problems.push({path: name, reason: 'GIT_ONLY'});
    else if (!entry) problems.push({path: name, reason: 'NON_REGULAR_GIT_ENTRY'});
    else files.set(name, entry);
  }
  for (const name of expected.keys()) if (!tree.has(name)) problems.push({path: name, reason: 'QUALIFIED_ONLY'});
  for (const [name, blob] of readGitBlobs(repository, files)) {
    const rawHash = digest(blob), wanted = expected.get(name), known = supplied.get(name);
    if (rawHash === wanted && !known) continue;
    try {
      if (known && known.gitBlobSha256 !== rawHash) throw Error('EOL_RECIPE_BLOB');
      const recipe = await exactEol(blob, wanted, known, qualifiedRoot, name);
      if (!recipe || digest(applyEol(blob, recipe)) !== wanted) throw Error('GIT_BYTES_DIFFER');
      endings.push({path: name, gitBlobSha256: rawHash, qualifiedSha256: wanted, kind: recipe.kind,
        ...(recipe.kind === 'MIXED' ? {crlfBits: recipe.crlfBits} : {})});
    } catch (error) {
      problems.push({path: name, reason: error.code ?? error.message, expectedSha256: wanted, gitSha256: rawHash});
    }
  }
  return {problems, endings};
}

async function verifyPublication(repository, candidate, manifestDoc, verdictDoc, publishedRef) {
  const manifest = manifestDoc.value, verdict = verdictDoc.value, version = manifest.release.versionName;
  const published = ['PUBLISHED', 'PUBLISHED_SITE'].includes(manifest.publication?.state);
  const requiredChecks = ['SOURCE_SNAPSHOT', 'PAYLOAD_INTEGRITY', 'APK_IDENTITY', 'PLATFORM_GATES'];
  if (manifest.publication?.state === 'PUBLISHED') requiredChecks.push('GITHUB_RETENTION');
  if (!published || verdict.kind !== 'ATF_RELEASE_RAG_VERDICT' || verdict.verdict !== 'READY_FOR_HUMAN_REVIEW'
      || verdict.releaseVersion !== version || verdict.candidateManifestSha256 !== manifestDoc.sha256
      || verdict.sourceCommit !== manifest.source.sourceCommit || verdict.workingTreeFingerprint !== manifest.source.workingTreeFingerprint
      || !Array.isArray(verdict.checks) || verdict.checks.some(check => check.status !== 'PASS')
      || !requiredChecks.every(code => verdict.checks.filter(check => check.code === code).length === 1))
    throw Error('CLEANUP_POSTPUBLICATION_VERDICT_MISMATCH');

  const main = commitId(repository, 'refs/remotes/origin/main');
  const evidenceCommit = publishedRef ? commitId(repository, publishedRef) : main;
  assertAncestor(repository, evidenceCommit, main);
  assertAncestor(repository, manifest.source.sourceCommit, evidenceCommit);
  if (manifest.publication.state === 'PUBLISHED' && commitId(repository, 'refs/tags/v' + version) !== manifest.source.sourceCommit)
    throw Error('CLEANUP_RELEASE_TAG_MISMATCH');
  const releasePath = 'release/' + version + '/';
  if (digest(gitBlob(repository, evidenceCommit, releasePath + 'release-validation.json')) !== manifestDoc.sha256)
    throw Error('CLEANUP_MANIFEST_ABSENT_FROM_PUBLISHED_GIT');
  const {apk, sidecar} = await readApkArtifact(candidate, manifest.release);
  const expected = new Map();
  for (const item of manifest.integrity?.files ?? []) {
    const name = item.path;
    if (typeof name !== 'string' || !name || name.includes('\\') || name.split('/').some(part => !part || part === '.' || part === '..')
        || !hashPattern.test(item.sha256 ?? '') || expected.has(name)) throw Error('CLEANUP_INVALID_PAYLOAD');
    expected.set(name, item.sha256);
  }
  if (expected.get(apk.path) !== apk.sha256 || expected.get(sidecar.path) !== sidecar.sha256)
    throw Error('CLEANUP_APK_PAYLOAD_BINDING');
  const tracked = trackedEntries(repository, evidenceCommit, releasePath);
  tracked.delete(releasePath + 'release-validation.json');
  if (tracked.size !== expected.size) throw Error('CLEANUP_GIT_PAYLOAD_INVENTORY');
  for (const [name, entry] of tracked) {
    if (!entry || !expected.has(name.slice(releasePath.length))) throw Error('CLEANUP_GIT_PAYLOAD_INVENTORY:' + name);
  }
  for (const [name, bytes] of readGitBlobs(repository, tracked)) {
    if (digest(bytes) !== expected.get(name.slice(releasePath.length))) throw Error('CLEANUP_GIT_PAYLOAD_HASH:' + name);
  }
  return {evidenceCommit, checkedOriginMain: main, postpublicationVerdictSha256: verdictDoc.sha256, apkSha256: apk.sha256};
}

export async function verifyGitSnapshot({repository, candidate, qualifiedRoot, prior, verdict, publishedRef}) {
  const timing = new QualificationTiming();
  const manifestDoc = await timing.measure('git-snapshot.identity', async () =>
    strictJson(await regularFile(candidate, 'release-validation.json')));
  const manifest = manifestDoc.value;
  const identity = {releaseVersion: manifest.release?.versionName, sourceCommit: manifest.source?.sourceCommit,
    sourceFingerprint: manifest.source?.workingTreeFingerprint};
  if (!/^[0-9a-f]{40}$/.test(identity.sourceCommit ?? '') || !/^\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?$/.test(identity.releaseVersion ?? ''))
    throw Error('GIT_SNAPSHOT_IDENTITY');
  const input = await strictJson(await regularFile(candidate, manifest.source.inputManifest));
  const expected = componentInputMap(input.value.files);
  if (input.value.sourceCommit !== identity.sourceCommit || input.value.fingerprint !== identity.sourceFingerprint
      || inputFingerprint(expected) !== identity.sourceFingerprint || commitId(repository, identity.sourceCommit) !== identity.sourceCommit)
    throw Error('GIT_SNAPSHOT_FINGERPRINT');
  const {problems, endings} = await timing.measure('git-snapshot.reconstruction', () =>
    verifySources(repository, identity, expected, qualifiedRoot, prior), result => result.problems.length ? 'BLOCKED' : 'PASS');
  let publication = null;
  if (verdict && !problems.length) {
    try {
      publication = await timing.measure('git-snapshot.publication', () =>
        verifyPublication(repository, candidate, manifestDoc, verdict, publishedRef));
    } catch (error) { problems.push({reason: error.message}); }
  }
  const parts = identity.releaseVersion.split('-')[0].split('.').map(Number);
  const localReleaseRequired = parts[0] > 0 || parts[1] > 21 || parts[1] === 21 && parts[2] >= 10;
  const cleanupEligible = publication !== null && !localReleaseRequired;
  return {schemaVersion: 1, kind, ...identity, manifestSha256: manifestDoc.sha256, inputCount: expected.size,
    status: problems.length ? 'BLOCKED' : 'PASS', reconstruction: endings.length ? 'GIT_WITH_EOL_RECIPE' : 'GIT_EXACT',
    endings, problems, publication, cleanupEligible, localReleaseRequired, timing: timing.receipt('git-snapshot')};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const {values: options} = parseArgs({options: Object.fromEntries(
      ['repo', 'candidate', 'output', 'qualified-root', 'recipe', 'published-verdict', 'published-commit', 'local-release']
        .map(name => [name, {type: 'string'}]))});
    const {repo, candidate, output} = options;
    if (!repo || !candidate || !output || ((options['published-commit'] || options['local-release']) && !options['published-verdict'])) {
      throw Error('Use --repo <git> --candidate <release-dir> --output <external-json> '
        + '[--qualified-root <original> | --recipe <json>] [--published-verdict <json> [--published-commit <git-ref>] [--local-release <directory>]]');
    }
    const roots = await Promise.all([repo, candidate, ...(options['qualified-root'] ? [options['qualified-root']] : [])]
      .map(dir => fs.realpath(dir)));
    const target = path.join(await fs.realpath(path.dirname(path.resolve(output))), path.basename(output));
    if (roots.some(root => within(root, target))) throw Error('OUTPUT_MUST_BE_OUTSIDE_INPUTS');
    const result = await verifyGitSnapshot({
      repository: roots[0], candidate: roots[1], qualifiedRoot: roots[2],
      prior: options.recipe ? (await strictJson(options.recipe)).value : undefined,
      verdict: options['published-verdict'] ? await strictJson(options['published-verdict']) : undefined,
      publishedRef: options['published-commit']
    });
    if (options['local-release']) {
      const destination = path.resolve(options['local-release']);
      if (within(destination, target)) throw Error('LOCAL_RELEASE_RECEIPT_MUST_BE_EXTERNAL');
      const existing = await fs.lstat(target).catch(error => error.code === 'ENOENT' ? null : Promise.reject(error));
      if (existing) throw Error('LOCAL_RELEASE_RECEIPT_ALREADY_EXISTS');
      result.localRelease = await syncLocalRelease({
        repository: roots[0], candidate: roots[1], proof: result,
        destination, stagingParent: path.dirname(target)
      });
      result.cleanupEligible = result.localRelease.verification.status === 'PASS';
    } else if (result.localReleaseRequired && result.publication) {
      result.status = 'BLOCKED';
      result.problems.push({reason:'LOCAL_RELEASE_REQUIRED: provide --local-release before completing delivery or cleanup'});
    }
    await writeTimingReceipt(target, result);
    console.log(JSON.stringify({status: result.status, files: result.inputCount, endings: result.endings.length,
      cleanupEligible: result.cleanupEligible, output: target, problems: result.problems}));
    if (result.status !== 'PASS') process.exitCode = 2;
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

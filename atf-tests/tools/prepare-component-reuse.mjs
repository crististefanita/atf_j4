import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {componentEvidenceScope} from './component-evidence-scope.mjs';
import {
  componentInputMap, digest, inlinePngCount, regularFile, strictJson, verifyComponentExecution, verifyComponentOrigin, verifyReleaseInputs
} from './evaluate-release-rag.mjs';
import {isOnlyAndroidVersionChange} from './release-test-scope.mjs';

const inputManifest = 'source-inputs.json';
const provenanceReceipt = 'source-provenance.json';
const runReceipt = 'source-component-run.json';
const releaseInputs = 'release-inputs.json';
const originFiles = [inputManifest, provenanceReceipt, runReceipt];
const reusePlan = 'component-reuse.json';
const groupPattern = /^[a-z0-9][a-z0-9-]{0,79}$/;
const shaPattern = /^[0-9a-f]{64}$/;

export async function prepareComponentReuse(runRoot, previousRoot, groups, {resume = false, sourceRoot = null} = {}) {
  if (!groups.length || new Set(groups).size !== groups.length || groups.some(group => !groupPattern.test(group))) {
    throw Error('INVALID_COMPONENT_GROUPS');
  }
  const target = await fs.realpath(runRoot);
  const previous = await fs.realpath(previousRoot);
  if (target === previous) throw Error('REUSE_SOURCE_EQUALS_DESTINATION');
  const currentInputs = await strictJson(await regularFile(target, inputManifest));
  const current = componentInputMap(currentInputs.value);
  const previousInputs = await strictJson(await regularFile(previous, inputManifest));
  const previousSource = componentInputMap(previousInputs.value);
  const currentRelease = await readReleaseInputs(target, '');
  if (resume) {
    const previousRelease = await readReleaseInputs(previous, '');
    if (previousInputs.sha256 !== currentInputs.sha256 || !currentRelease || previousRelease?.sha256 !== currentRelease.sha256) {
      throw Error('COMPONENT_RESUME_INPUTS_CHANGED');
    }
  }
  const androidVersionMetadataOnly = await verifyAndroidVersionMetadataOnly(previous, sourceRoot, previousSource, current);
  const previousPlan = await readPreviousPlan(previous);
  const origins = new Map();
  const published = new Set();
  const decisions = [];
  for (const group of groups) {
    let evidence;
    try {
      const previousOrigin = previousPlan.get(group);
      if (!origins.has(previousOrigin)) {
        // Cache the promise too so an invalid origin is read only once.
        origins.set(previousOrigin, readOrigin(previous, previousOrigin, resume));
      }
      const origin = await origins.get(previousOrigin);
      if (group === 'atf-tests' && currentRelease?.sha256 !== origin.releaseInputs?.sha256) {
        throw Error('GOVERNANCE_RELEASE_SNAPSHOT_CHANGED');
      }
      const execution = await strictJson(await regularFile(previous, group + '/execution.json'));
      const report = await fs.readFile(await regularFile(previous, group + '/component-report.html'));
      const html = report.toString('utf8');
      if (!/<html\b/i.test(html.slice(0, 8192)) || inlinePngCount(html) === 0) throw Error('INVALID_EXTENT');
      verifyComponentExecution(origin, group, execution.value, execution.value?.tests, digest(report));
      const scope = componentEvidenceScope(group, current, origin.sourceSet, {androidVersionMetadataOnly});
      const decision = {group, decision: scope.decision, reason: scope.reason,
        affectedGroups: scope.affectedGroups, changedInputs: scope.changed, affectedInputs: scope.affected};
      if (scope.decision === 'RUN') {
        decisions.push(decision);
        continue;
      }
      evidence = {origin, execution, report, decision};
    } catch (error) {
      decisions.push({group, decision: 'INVALID', reason: error.message});
      continue;
    }
    const {origin, execution, report, decision} = evidence;
    const id = origin.provenance.sha256;
    if (!published.has(id)) {
      const originDir = path.join(target, 'snapshots', id);
      await fs.mkdir(originDir, {recursive: true});
      for (const [index, name] of originFiles.entries()) {
        await fs.writeFile(path.join(originDir, name), origin.files[index].bytes, {flag: 'wx'});
      }
      if (origin.releaseInputs) await fs.writeFile(path.join(originDir, releaseInputs), origin.releaseInputs.bytes, {flag: 'wx'});
      published.add(id);
    }
    const destination = path.join(target, group);
    await fs.mkdir(destination);
    await fs.writeFile(path.join(destination, 'component-report.html'), report, {flag: 'wx'});
    await fs.writeFile(path.join(destination, 'execution.json'), execution.bytes, {flag: 'wx'});
    decisions.push({...decision, componentEvidenceOrigin: id, tests: execution.value.tests,
      reportSha256: execution.value.reportSha256, executionSha256: execution.sha256});
  }
  const planFile = path.join(target, reusePlan);
  const pending = planFile + '.writing';
  await fs.writeFile(pending, JSON.stringify({schemaVersion: 2,
    comparison: {baselineRelease: await findBaselineVersion(previous), androidVersionMetadataOnly}, decisions}, null, 2) + '\n', {flag: 'wx'});
  await fs.rename(pending, planFile);
  return decisions;
}

async function findBaselineManifest(previousRoot) {
  let directory = previousRoot;
  for (let depth = 0; depth < 5; depth++, directory = path.dirname(directory)) {
    const candidate = path.join(directory, 'release-validation.json');
    try {
      const entry = await fs.lstat(candidate);
      if (entry.isFile() && !entry.isSymbolicLink()) return candidate;
    } catch (error) { if (error.code !== 'ENOENT') throw error; }
    if (directory === path.dirname(directory)) break;
  }
  return null;
}

async function findBaselineVersion(previousRoot) {
  const manifest = await findBaselineManifest(previousRoot);
  if (!manifest) return null;
  return (await strictJson(manifest)).value.release?.versionName ?? null;
}

async function verifyAndroidVersionMetadataOnly(previousRoot, sourceRoot, previousSource, currentSource) {
  const build = 'whist-lab-app/app/build.gradle';
  if (!sourceRoot || previousSource.get(build) === currentSource.get(build)) return false;
  const baselineManifest = await findBaselineManifest(previousRoot);
  if (!baselineManifest) return false;
  try {
    const baseline = (await strictJson(baselineManifest)).value;
    if (!['PUBLISHED', 'PUBLISHED_SITE'].includes(baseline.publication?.state)
        || baseline.qualification?.state !== 'PASS') return false;
    const buildText = await fs.readFile(await regularFile(await fs.realpath(sourceRoot), build), 'utf8');
    const version = [...buildText.matchAll(/^\s*versionName\s+"([^"]+)"/gm)];
    return digest(buildText) === currentSource.get(build) && version.length === 1
        && isOnlyAndroidVersionChange(previousSource, currentSource, buildText,
      baseline.release, version[0][1], digest);
  } catch { return false; }
}

async function readPreviousPlan(root) {
  let plan;
  try {
    plan = (await strictJson(await regularFile(root, reusePlan))).value;
  } catch (error) {
    if (error.code === 'ENOENT') return new Map();
    throw error;
  }
  const origins = new Map();
  for (const row of plan.decisions) {
    if (row.componentEvidenceOrigin === undefined) continue;
    if (!groupPattern.test(row.group ?? '') || !shaPattern.test(row.componentEvidenceOrigin ?? '') || origins.has(row.group)) {
      throw Error('INVALID_PREVIOUS_REUSE_PLAN');
    }
    origins.set(row.group, row.componentEvidenceOrigin);
  }
  return origins;
}

async function readOrigin(root, id, resume) {
  const prefix = id === undefined ? '' : 'snapshots/' + id + '/';
  const files = await Promise.all(originFiles.map(async name => {
    try { return await strictJson(await regularFile(root, prefix + name)); }
    catch (error) {
      if (!resume || id !== undefined || name !== runReceipt || error.code !== 'ENOENT') throw error;
      return strictJson(await regularFile(root, 'component-checkpoint.json'));
    }
  }));
  const [inputs, provenance, run] = files;
  if (id !== undefined && id !== provenance.sha256) throw Error('INVALID_PREVIOUS_ORIGIN_HASH');
  const origin = verifyComponentOrigin(inputs, run.value, provenance);
  const release = await readReleaseInputs(root, prefix);
  if (release || provenance.value.releaseInputManifest !== undefined || run.value.releaseInputManifest !== undefined) {
    verifyReleaseInputs(origin, release);
  }
  return {...origin, files, releaseInputs: release};
}

async function readReleaseInputs(root, prefix) {
  try { return await strictJson(await regularFile(root, prefix + releaseInputs)); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [runRoot, previousRoot, ...arguments_] = process.argv.slice(2);
    let resume = false, sourceRoot = null;
    for (let index = 0; index < arguments_.length;) {
      if (arguments_[index] === '--resume') { resume = true; arguments_.splice(index, 1); }
      else if (arguments_[index] === '--source-root' && arguments_[index + 1]) {
        sourceRoot = arguments_[index + 1]; arguments_.splice(index, 2);
      } else index++;
    }
    await prepareComponentReuse(runRoot, previousRoot, arguments_, {resume, sourceRoot});
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

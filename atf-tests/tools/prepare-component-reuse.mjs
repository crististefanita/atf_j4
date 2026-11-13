import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {componentEvidenceScope} from './component-evidence-scope.mjs';
import {
  componentInputMap, digest, inlinePngCount, regularFile, strictJson, verifyComponentExecution, verifyComponentOrigin, verifyReleaseInputs
} from './evaluate-release-rag.mjs';

const inputManifest = 'source-inputs.json';
const provenanceReceipt = 'source-provenance.json';
const runReceipt = 'source-component-run.json';
const releaseInputs = 'release-inputs.json';
const originFiles = [inputManifest, provenanceReceipt, runReceipt];
const reusePlan = 'component-reuse.json';
const groupPattern = /^[a-z0-9][a-z0-9-]{0,79}$/;
const shaPattern = /^[0-9a-f]{64}$/;

export async function prepareComponentReuse(runRoot, previousRoot, groups) {
  if (!groups.length || new Set(groups).size !== groups.length || groups.some(group => !groupPattern.test(group))) {
    throw Error('INVALID_COMPONENT_GROUPS');
  }
  const target = await fs.realpath(runRoot);
  const previous = await fs.realpath(previousRoot);
  if (target === previous) throw Error('REUSE_SOURCE_EQUALS_DESTINATION');
  const current = componentInputMap((await strictJson(await regularFile(target, inputManifest))).value);
  const currentRelease = await readReleaseInputs(target, '');
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
        origins.set(previousOrigin, readOrigin(previous, previousOrigin));
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
      const scope = componentEvidenceScope(group, current, origin.sourceSet);
      const decision = {group, decision: scope.decision, changedInputs: scope.changed, affectedInputs: scope.affected};
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
  await fs.writeFile(path.join(target, reusePlan), JSON.stringify({schemaVersion: 1, decisions}, null, 2) + '\n', {flag: 'wx'});
  return decisions;
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

async function readOrigin(root, id) {
  const prefix = id === undefined ? '' : 'snapshots/' + id + '/';
  const files = await Promise.all(originFiles.map(async name => strictJson(await regularFile(root, prefix + name))));
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
    const [runRoot, previousRoot, ...groups] = process.argv.slice(2);
    await prepareComponentReuse(runRoot, previousRoot, groups);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

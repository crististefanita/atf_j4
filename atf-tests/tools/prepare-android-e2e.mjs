import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {androidExecutionProfile, disposablePaths, e2ePaths, scenarioFor, signedE2eProtocol} from './android-e2e-contract.mjs';
import {readE2eEvidence, verifyDisposableUser, verifyE2eReport} from './android-e2e-evidence.mjs';
import {inspectE2eImages, verifyPng} from './android-e2e-images.mjs';
import {componentInputMap, digest, inputFingerprint, regularFile, strictJson} from './evaluate-release-rag.mjs';

const doc = async (root, relative) => strictJson(await regularFile(root, relative));
const read = async (root, relative) => {
  const bytes = await fs.readFile(await regularFile(root, relative));
  return {bytes, text: bytes.toString('utf8'), sha256: digest(bytes)};
};
const write = (file, value) => fs.writeFile(file, JSON.stringify(value, null, 2) + '\n', {flag: 'wx'});
const entry = (relative, document) => ({path: relative, sha256: document.sha256, bytes: document.bytes.length});

export async function planAndroidE2e(candidate, manifest, request) {
  const {api, displayProfile, responsiveProfiles} = request.executionProfile ?? {};
  const profile = androidExecutionProfile(api, displayProfile, responsiveProfiles);
  const groups = request.groups;
  if (!Array.isArray(groups) || !groups.length || new Set(groups).size !== groups.length)
    throw Error('ANDROID_E2E_SELECT_DISTINCT_GROUPS');
  const files = manifest.integrity?.files ?? [];
  const declared = new Map(files.map(item => [item.path, item]));
  if (declared.size !== files.length) throw Error('ANDROID_E2E_DUPLICATE_PAYLOAD');
  const decisions = [];
  for (const group of groups) {
    const scenario = scenarioFor(group, profile);
    try {
      const accepted = await readE2eEvidence({
        manifest, name: group, declared, scenario, requireScenario: true,
        readDocument: relative => doc(candidate, relative), readReport: relative => read(candidate, relative)
      });
      decisions.push({group, scenario, decision: 'REUSE',
        evidence: [...accepted.documents].map(([relative, document]) => entry(relative, document))});
    } catch (error) {
      decisions.push({group, scenario, decision: 'RUN', reason: error.code === 'ENOENT' ? 'ANDROID_E2E_FILE_MISSING' : error.message});
    }
  }
  return decisions;
}

export async function verifyOriginalCaptures(root, group, report) {
  const trace = await read(root, 'ui-flow-' + group + '.properties');
  if (!trace.text.split(/\r?\n/).includes('flow.id=' + group) || !trace.text.split(/\r?\n/).includes('flow.status=PASS'))
    throw Error('ANDROID_E2E_TRACE_NOT_PASS');
  const screenshots = [...trace.text.matchAll(/^phase\.\d+\.step\.\d+\.screenshot=([\w./-]+\.png)\r?$/gm)];
  if (!screenshots.length || report.ownedCount > screenshots.length) throw Error('ANDROID_E2E_TRACE_SCREENSHOT_COUNT');
  const expected = new Set();
  for (const [, relative] of screenshots) {
    const capture = await read(root, relative);
    verifyPng(capture.bytes);
    expected.add(capture.bytes.toString('base64'));
  }
  if (expected.size !== report.checkpoints.size || [...expected].some(image => !report.checkpoints.has(image)))
    throw Error('ANDROID_E2E_SCREENSHOT_DIFFERS_FROM_ORIGINAL');
}

async function recordAttempt(root, manifest, attempt, group) {
  const {scenario, compilationReceiptSha256} = attempt;
  const raw = await fs.realpath(path.join(root, 'android-artifacts', group));
  if (raw !== path.join(root, 'android-artifacts', group)) throw Error('ANDROID_E2E_CAPTURE_PATH');
  const report = await read(raw, 'report.html');
  const images = inspectE2eImages(report.text, {requireCheckpoints: true});
  await verifyOriginalCaptures(raw, group, images);
  const [ready, cleanup] = await Promise.all([doc(root, 'data-isolation-ready.json'), doc(root, 'data-isolation-cleanup.json')]);
  const leaseId = cleanup.value.leaseId;
  if (!/^[a-f0-9]{32}$/.test(leaseId ?? '') || ready.value.testUserId !== attempt.testUserId
      || cleanup.value.serial !== attempt.serial || leaseId !== attempt.leaseId) throw Error('ANDROID_E2E_ATTEMPT_LEASE');
  const dataBoundary = {mode: 'DISPOSABLE_SECONDARY_USER', leaseId, testUserId: attempt.testUserId, ...disposablePaths(leaseId),
    readyReceiptSha256: ready.sha256, cleanupReceiptSha256: cleanup.sha256};
  const paths = e2ePaths(group);
  const counts = {tests: 1, failures: 0, errors: 0, skipped: 0};
  const result = {protocol: signedE2eProtocol, ...counts, ...paths};
  const receipt = {schemaVersion: 2, kind: 'android-signed-release-e2e', flow: group, status: 'PASS',
    protocol: signedE2eProtocol, ...counts, scenario, compilationReceiptSha256,
    releaseVersion: manifest.release.versionName, installedApkSha256: manifest.release.apkSha256,
    sourceCommit: manifest.source.sourceCommit, sourceFingerprint: manifest.source.workingTreeFingerprint,
    reportSha256: report.sha256, inlinePngCount: images.count, checkpointPngCount: images.ownedCount, dataBoundary};
  const identity = verifyE2eReport({manifest, name: group, group: result, receipt, report, expectedScenario: scenario});
  verifyDisposableUser(dataBoundary, ready, cleanup, identity, scenario);
  const receiptBytes = Buffer.from(JSON.stringify(receipt, null, 2) + '\n');
  const documents = new Map([[paths.report, report], [paths.executionReceipt, {bytes: receiptBytes, sha256: digest(receiptBytes)}],
    [dataBoundary.readyReceipt, ready], [dataBoundary.cleanupReceipt, cleanup]]);
  const bundle = path.join(root, 'evidence');
  await fs.mkdir(bundle);
  for (const [relative, document] of documents) {
    const target = path.join(bundle, relative);
    await fs.mkdir(path.dirname(target), {recursive: true});
    await fs.writeFile(target, document.bytes, {flag: 'wx'});
  }
  await write(path.join(bundle, 'e2e-result.json'), {flow: group, group: result,
    integrity: [...documents].map(([relative, document]) => entry(relative, document))});
}

async function main([command, candidateDir, runDir, ...args]) {
  const [candidate, root] = await Promise.all([candidateDir, runDir].map(dir => fs.realpath(dir)));
  if ([candidate, root].some((dir, i) => dir === [root, candidate][i] || dir.startsWith([root, candidate][i] + path.sep)))
    throw Error('ANDROID_E2E_EXTERNAL_RUN_ROOT_REQUIRED');
  const manifest = (await doc(candidate, 'release-validation.json')).value;
  if (command === 'plan') {
    const request = (await doc(root, 'e2e-request.json')).value, group = args[0];
    const inputs = (await doc(candidate, manifest.source.inputManifest)).value;
    const stagedInputs = (await doc(root, 'source-inputs.json')).value;
    const fingerprint = inputFingerprint(componentInputMap(stagedInputs));
    if (request.releaseVersion !== manifest.release.versionName || request.releaseVersion !== path.basename(candidate)
        || fingerprint !== manifest.source.workingTreeFingerprint || fingerprint !== inputFingerprint(componentInputMap(inputs.files))
        || fingerprint !== inputs.fingerprint || inputs.sourceCommit !== manifest.source.sourceCommit)
      throw Error('ANDROID_E2E_SOURCE_SNAPSHOT_MISMATCH');
    const decisions = await planAndroidE2e(candidate, manifest, request);
    if (!decisions.some(row => row.group === group)) throw Error('ANDROID_E2E_GROUP_NOT_IN_PLAN');
    await write(path.join(root, 'e2e-reuse.json'), {releaseVersion: request.releaseVersion,
      installedApkSha256: manifest.release.apkSha256, sourceFingerprint: fingerprint, decisions});
  } else if (command === 'record') {
    const group = args[0], attempt = (await doc(root, 'attempt.json')).value;
    const plan = (await doc(root, 'e2e-reuse.json')).value;
    const compilation = await doc(root, 'compilation.json');
    const selected = plan.decisions.find(row => row.group === group && row.decision === 'RUN');
    if (!selected || compilation.sha256 !== attempt.compilationReceiptSha256
        || compilation.value.sourceFingerprint !== plan.sourceFingerprint || compilation.value.selection !== selected.scenario.selection
        || !/^[0-9a-f]{64}$/.test(compilation.value.classFingerprint ?? '')
        || !/^[0-9a-f]{64}$/.test(compilation.value.libraryFingerprint ?? '')
        || plan.sourceFingerprint !== manifest.source.workingTreeFingerprint || plan.installedApkSha256 !== manifest.release.apkSha256
        || JSON.stringify(selected.scenario) !== JSON.stringify(attempt.scenario)) throw Error('ANDROID_E2E_ATTEMPT_IDENTITY');
    await recordAttempt(root, manifest, attempt, group);
  } else throw Error('Use plan or record <candidate> <run> <group>');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { await main(process.argv.slice(2)); }
  catch (error) { console.error(error.message); process.exitCode = 1; }
}

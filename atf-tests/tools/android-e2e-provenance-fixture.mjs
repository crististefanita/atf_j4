import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {digest, inputFingerprint, componentInputMap} from './evaluate-release-rag.mjs';
import {disposablePaths, e2ePaths, scenarioFor, signedE2eProtocol} from './android-e2e-contract.mjs';
import {exportAndroidEvidence} from './prepare-android-e2e.mjs';
import {applyAndroidEvidence} from './apply-android-e2e.mjs';
import {fileURLToPath} from 'node:url';

export const flow = 'signed-release-bidding';
export const profile = {api: 28, displayProfile: '1080x1920@440', responsiveProfiles: '1080x1920@440'};
export const request = {releaseVersion: '0.21.11', groups: [flow], executionProfile: profile};
export const gameFile = 'whist-lab-app/game-core/src/main/java/ro/whistlab/core/Game.java';
export const validatorFile = 'atf-tests/tools/evaluate-release-rag.mjs';
export const scenarioFile = 'atf-test-android/src/test/java/demo/NativeTest.java';
const png = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
const inputs = [gameFile, 'lib/ui.aar', scenarioFile, 'scripts/run-whist-signed-e2e.ps1', validatorFile, 'docs/Guide.md']
  .map(file => ({path: file, sha256: digest(file)}));

export async function writePayload(root, manifest, relative, value) {
  const file = path.join(root, relative);
  const bytes = Buffer.isBuffer(value) ? value : Buffer.from(typeof value === 'string' ? value : JSON.stringify(value));
  await fs.mkdir(path.dirname(file), {recursive: true});
  await fs.writeFile(file, bytes);
  manifest.integrity.files = manifest.integrity.files.filter(row => row.path !== relative);
  const entry = {path: relative, sha256: digest(bytes), bytes: bytes.length};
  manifest.integrity.files.push(entry);
  return entry.sha256;
}

export async function setSources(root, manifest, files, sourceCommit = 'b'.repeat(40)) {
  const fingerprint = inputFingerprint(componentInputMap(files));
  manifest.source = {...manifest.source, sourceCommit, workingTreeFingerprint: fingerprint, inputManifest: 'reports/apk-inputs.json'};
  await writePayload(root, manifest, manifest.source.inputManifest, {sourceCommit, fingerprint, files});
  await fs.writeFile(path.join(root, 'release-validation.json'), JSON.stringify(manifest));
}

async function bindRagCandidate(candidate, sourceRoot, planFile) {
  const pending = [];
  try {
    const f = await nativeFixture({after: cleanup => pending.push(cleanup)}, {exported: false});
    const target = JSON.parse(await fs.readFile(path.join(candidate, 'release-validation.json')));
    const source = JSON.parse(await fs.readFile(path.join(candidate, target.source.inputManifest)));
    const files = [...source.files];
    for (const file of [validatorFile, scenarioFile]) {
      const actual = path.join(sourceRoot, file);await fs.mkdir(path.dirname(actual), {recursive: true});
      await fs.writeFile(actual, 'original '+file);
      files.push({path: file, sha256: digest('original '+file)});
    }
    files.sort((a,b)=>a.path<b.path?-1:a.path>b.path?1:0);
    f.manifest.release = target.release;
    await setSources(f.origin, f.manifest, files, target.source.sourceCommit);
    const paths=e2ePaths(flow), receipt=JSON.parse(await fs.readFile(path.join(f.origin,paths.executionReceipt)));
    receipt.releaseVersion=target.release.versionName;receipt.installedApkSha256=target.release.apkSha256;
    receipt.sourceCommit=f.manifest.source.sourceCommit;receipt.sourceFingerprint=f.manifest.source.workingTreeFingerprint;
    const ready=JSON.parse(await fs.readFile(path.join(f.origin,receipt.dataBoundary.readyReceipt)));
    ready.installedApkSha256=target.release.apkSha256;
    receipt.dataBoundary.readyReceiptSha256=await writePayload(f.origin,f.manifest,receipt.dataBoundary.readyReceipt,ready);
    const cleanup=JSON.parse(await fs.readFile(path.join(f.origin,receipt.dataBoundary.cleanupReceipt)));
    cleanup.readyReceiptSha256=receipt.dataBoundary.readyReceiptSha256;
    receipt.dataBoundary.cleanupReceiptSha256=await writePayload(f.origin,f.manifest,receipt.dataBoundary.cleanupReceipt,cleanup);
    await writePayload(f.origin,f.manifest,paths.executionReceipt,receipt);
    await fs.writeFile(path.join(f.origin,'release-validation.json'),JSON.stringify(f.manifest));
    const actual=path.join(sourceRoot,validatorFile);await fs.writeFile(actual,'new validator revision');
    const current=files.map(row=>row.path===validatorFile?{...row,sha256:digest('new validator revision')}:row);
    await setSources(candidate,target,current,target.source.sourceCommit);
    const req={...request,releaseVersion:target.release.versionName};
    await fs.writeFile(path.join(f.run,'e2e-request.json'),JSON.stringify(req));
    await exportAndroidEvidence(f.origin,f.manifest,req,f.run,flow);
    await applyAndroidEvidence(candidate,f.run);
    const plan=JSON.parse(await fs.readFile(planFile));
    plan.scope.candidateFingerprint=target.source.workingTreeFingerprint;
    plan.e2e={status:'PASS',groups:[flow],executionProfile:profile};
    await fs.writeFile(planFile,JSON.stringify(plan));
  } finally {for(const cleanup of pending)await cleanup();}
}

if (process.argv[2] === 'bind-rag' && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url))
  await bindRagCandidate(...process.argv.slice(3));

export async function nativeFixture(t, {exported = true, extraInputs = []} = {}) {
  const base = await fs.mkdtemp(path.join(os.tmpdir(), 'native-origin-'));
  t.after(() => fs.rm(base, {recursive: true, force: true}));
  const origin = path.join(base, 'origin', request.releaseVersion), candidate = path.join(base, 'candidate', request.releaseVersion);
  const run = path.join(base, 'run');
  for (const directory of [origin, candidate, run]) await fs.mkdir(directory, {recursive: true});
  const manifest = {release: {versionName: request.releaseVersion, apkSha256: 'a'.repeat(64), applicationId: 'ro.whistlab.app'},
    publication: {state: 'PREPARED'}, qualification: {e2eGroups: {}}, integrity: {files: []}};
  const sourceInputs = inputs.concat(extraInputs);
  await setSources(origin, manifest, sourceInputs, 'c'.repeat(40));
  const paths = e2ePaths(flow), leaseId = 'd'.repeat(32), isolation = disposablePaths(leaseId);
  const common = {scope: 'ANDROID_SECONDARY_USER', applicationId: manifest.release.applicationId,
    originalUserId: 0, originalPackageWasInstalledBefore: true};
  const readyReceiptSha256 = await writePayload(origin, manifest, isolation.readyReceipt, {...common, status: 'READY',
    testUserId: 12, observedApi: profile.api, displayProfile: profile.displayProfile, createdByRun: true,
    packageWasInstalledBefore: false, screenOffTimeoutMs: 2147483647, screenUnlocked: true, cleanupRequired: true,
    accountsAdded: false, installedApkSha256: manifest.release.apkSha256, userReadiness: {state: 'RUNNING_UNLOCKED'}});
  const cleanupReceiptSha256 = await writePayload(origin, manifest, isolation.cleanupReceipt, {...common, status: 'PASS',
    removedUserId: 12, removedByRun: true, originalPackageStatePreserved: true, originalPackageRetained: true, leaseId, readyReceiptSha256});
  const reportSha256 = await writePayload(origin, manifest, paths.report,
    '<html><a data-atf-screenshot="base64" href="data:image/png;base64,' + png + '">Capture</a></html>');
  const counts = {tests: 1, failures: 0, errors: 0, skipped: 0};
  manifest.qualification.e2eGroups[flow] = {protocol: signedE2eProtocol, ...counts, ...paths};
  await writePayload(origin, manifest, paths.executionReceipt, {schemaVersion: 2, kind: 'android-signed-release-e2e',
    status: 'PASS', releaseVersion: request.releaseVersion, installedApkSha256: manifest.release.apkSha256,
    sourceCommit: manifest.source.sourceCommit, sourceFingerprint: manifest.source.workingTreeFingerprint,
    protocol: signedE2eProtocol, ...counts, flow, scenario: scenarioFor(flow, profile), reportSha256,
    inlinePngCount: 1, checkpointPngCount: 1, compilationReceiptSha256: 'e'.repeat(64),
    dataBoundary: {mode: 'DISPOSABLE_SECONDARY_USER', leaseId, testUserId: 12, ...isolation, readyReceiptSha256, cleanupReceiptSha256}});
  await fs.writeFile(path.join(origin, 'release-validation.json'), JSON.stringify(manifest));
  const current = structuredClone(manifest);
  current.qualification.e2eGroups = {};
  current.integrity.files = [];
  const files = sourceInputs.map(row => [validatorFile, 'docs/Guide.md'].includes(row.path) ? {...row, sha256: digest('new ' + row.path)} : {...row});
  await setSources(candidate, current, files);
  await fs.writeFile(path.join(run, 'e2e-request.json'), JSON.stringify(request));
  if (exported) {
    await exportAndroidEvidence(origin, manifest, request, run, flow);
    await applyAndroidEvidence(candidate, run);
  }
  return {base, origin, candidate, run, manifest, current: JSON.parse(await fs.readFile(path.join(candidate, 'release-validation.json'))), files};
}

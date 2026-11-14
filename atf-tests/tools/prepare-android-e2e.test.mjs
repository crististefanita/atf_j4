import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';
import {scenarioFor, disposablePaths, e2ePaths, signedE2eProtocol} from './android-e2e-contract.mjs';
import {digest} from './evaluate-release-rag.mjs';
import {inspectE2eImages, verifyPng} from './android-e2e-images.mjs';
import {planAndroidE2e, verifyOriginalCaptures} from './prepare-android-e2e.mjs';
import {applyAndroidEvidence} from './apply-android-e2e.mjs';

const png = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
const reportHtml = image => '<html><a data-atf-screenshot="base64" href="data:image/png;base64,' + image + '">Capture</a></html>';
const bidding = 'signed-release-bidding', display = 'responsive-ui-sanity', autoGame = 'signed-release-auto-game';
const profile = {api: 28, displayProfile: '1080x1920@440', responsiveProfiles: '1080x1920@440,1080x2448@440'};
const request = {groups: [bidding, display, autoGame], executionProfile: profile};
const apk = 'a'.repeat(64);
const source = 'b'.repeat(64);

async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'rag-e2e-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const manifest = {release: {versionName: '0.21.4', apkSha256: apk, applicationId: 'ro.whistlab.app'},
    source: {sourceCommit: 'c'.repeat(40), workingTreeFingerprint: source},
    qualification: {e2eGroups: {}}, integrity: {files: []}};
  async function write(relative, content) {
    const target = path.join(root, relative);
    await fs.mkdir(path.dirname(target), {recursive: true});
    const bytes = Buffer.from(typeof content === 'string' ? content : JSON.stringify(content));
    await fs.writeFile(target, bytes);
    const record = {path: relative, sha256: digest(bytes), bytes: bytes.length};
    manifest.integrity.files = manifest.integrity.files.filter(item => item.path !== relative);
    manifest.integrity.files.push(record);
    return record.sha256;
  }
  for (const [index, group] of request.groups.entries()) {
    const leaseId = String(index).repeat(32), boundary = disposablePaths(leaseId);
    const common = {scope: 'ANDROID_SECONDARY_USER', applicationId: 'ro.whistlab.app', originalUserId: 0,
      originalPackageWasInstalledBefore: false};
    const readyReceiptSha256 = await write(boundary.readyReceipt, {...common, status: 'READY', testUserId: 10,
      observedApi: profile.api, displayProfile: profile.displayProfile, createdByRun: true, packageWasInstalledBefore: false,
      screenOffTimeoutMs: 2147483647, screenUnlocked: true, cleanupRequired: true, accountsAdded: false, installedApkSha256: apk});
    const cleanupReceiptSha256 = await write(boundary.cleanupReceipt, {...common, status: 'PASS', removedUserId: 10,
      removedByRun: true, originalPackageStatePreserved: true, originalPackageRetained: false, leaseId, readyReceiptSha256});
    const paths = e2ePaths(group), counts = {tests: 1, failures: 0, errors: 0, skipped: 0};
    manifest.qualification.e2eGroups[group] = {protocol: signedE2eProtocol, ...counts, ...paths};
    const reportSha256 = await write(paths.report, reportHtml(png));
    await write(paths.executionReceipt, {schemaVersion: 2, kind: 'android-signed-release-e2e', status: 'PASS',
      releaseVersion: '0.21.4', installedApkSha256: apk, sourceCommit: 'c'.repeat(40), sourceFingerprint: source,
      protocol: signedE2eProtocol, ...counts, flow: group, scenario: scenarioFor(group, profile), reportSha256,
      inlinePngCount: 1, checkpointPngCount: 1, compilationReceiptSha256: 'd'.repeat(64),
      dataBoundary: {mode: 'DISPOSABLE_SECONDARY_USER', leaseId, testUserId: 10, ...boundary, readyReceiptSha256, cleanupReceiptSha256}});
  }
  return {root, manifest, write};
}

test('the signed report requires owned, complete, decodable PNGs', () => {
  assert.equal(inspectE2eImages(reportHtml(png), {requireCheckpoints: true}).ownedCount, 1);
  assert.throws(() => inspectE2eImages('<html><img src="data:image/png;base64,' + png + '"></html>', {requireCheckpoints: true}));
  assert.throws(() => inspectE2eImages(reportHtml(png).slice(0, -7)), /INCOMPLETE_EXTENT_HTML/);
  assert.throws(() => inspectE2eImages(reportHtml(png.slice(0, 16))));
  const bad = Buffer.from(png, 'base64');
  bad[48] ^= 1;
  assert.throws(() => verifyPng(bad), /INVALID_PNG/);
});

test('missing and corrupt material leaves sibling flows reusable', async t => {
  const {root, manifest, write} = await fixture(t);
  const decision = () => planAndroidE2e(root, manifest, request);
  assert.deepEqual((await decision()).map(row => row.decision), ['REUSE', 'REUSE', 'REUSE']);
  await fs.unlink(path.join(root, e2ePaths(bidding).report));
  await write(e2ePaths(autoGame).report, reportHtml(png.slice(0, 16)));
  const rows = await decision();
  assert.deepEqual(rows.map(row => row.decision), ['RUN', 'REUSE', 'RUN']);
  assert.equal(rows[0].reason, 'ANDROID_E2E_FILE_MISSING');
  assert.match(rows[2].reason, /INVALID_PNG/);
});

test('a changed responsive matrix only invalidates its owner', async t => {
  const {root, manifest} = await fixture(t);
  const rows = await planAndroidE2e(root, manifest, {...request,
    executionProfile: {...profile, responsiveProfiles: '720x1280@360'}});
  assert.deepEqual(rows.map(row => row.decision), ['REUSE', 'RUN', 'REUSE']);
  assert.equal(rows[1].reason, 'ANDROID_E2E_SCENARIO_CHANGED');
});

test('APK, source, device profile and cleanup are independent conditions', async t => {
  const {root, manifest, write} = await fixture(t);
  const cases = [
    {...manifest, release: {...manifest.release, apkSha256: '9'.repeat(64)}},
    {...manifest, source: {...manifest.source, workingTreeFingerprint: '0'.repeat(64)}}
  ];
  for (const invalid of cases) {
    assert.ok((await planAndroidE2e(root, invalid, request)).every(row => row.decision === 'RUN'));
  }
  const changedApi = {...request, executionProfile: {...profile, api: 35}};
  assert.ok((await planAndroidE2e(root, manifest, changedApi)).every(row => row.decision === 'RUN'));
  await write(disposablePaths('1'.repeat(32)).cleanupReceipt, {status: 'FAIL'});
  assert.deepEqual((await planAndroidE2e(root, manifest, request)).map(row => row.decision), ['REUSE', 'RUN', 'REUSE']);
});

test('import replaces only the selected owner and rejects published destinations', async t => {
  const fixtureRun = await fixture(t);
  const base = await fixture(t);
  const candidate = path.join(base.root, '0.21.4');
  const run = path.join(base.root, 'run');
  const bundle = path.join(run, 'evidence');
  await fs.mkdir(candidate);
  await fs.mkdir(bundle, {recursive: true});
  await fs.cp(path.join(fixtureRun.root, 'reports'), path.join(candidate, 'reports'), {recursive: true});
  const manifest = {...base.manifest, publication: {state: 'PREPARED'}};
  await fs.writeFile(path.join(candidate, 'release-validation.json'), JSON.stringify(manifest));
  await fs.writeFile(path.join(run, 'e2e-request.json'), JSON.stringify({...request, releaseVersion: '0.21.4'}));

  const group = manifest.qualification.e2eGroups[bidding];
  const receipt = JSON.parse(await fs.readFile(path.join(candidate, group.executionReceipt)));
  const paths = [group.report, group.executionReceipt, receipt.dataBoundary.readyReceipt, receipt.dataBoundary.cleanupReceipt];
  const integrity = [];
  for (const relative of paths) {
    const destination = path.join(bundle, relative);
    const original = await fs.readFile(path.join(candidate, relative));
    await fs.mkdir(path.dirname(destination), {recursive: true});
    await fs.writeFile(destination, original);
    integrity.push(manifest.integrity.files.find(item => item.path === relative));
  }
  await fs.writeFile(path.join(bundle, 'e2e-result.json'), JSON.stringify({flow: bidding, group, integrity}));
  const sibling = path.join(candidate, e2ePaths(display).report), original = await fs.readFile(sibling);
  await fs.writeFile(path.join(candidate, group.report), '');
  await applyAndroidEvidence(candidate, run);
  assert.deepEqual(await fs.readFile(sibling), original);
  assert.equal(await fs.readFile(path.join(candidate, group.report), 'utf8'), reportHtml(png));
  manifest.publication.state = 'PUBLISHED';
  await fs.writeFile(path.join(candidate, 'release-validation.json'), JSON.stringify(manifest));
  await assert.rejects(() => applyAndroidEvidence(candidate, run), /ANDROID_E2E_CANDIDATE_MUST_BE_PREPARED/);
});
test('the original checkpoint PNG must match its Extent link', async t => {
  const {root} = await fixture(t);
  const trace = path.join(root, 'ui-flow-' + bidding + '.properties');
  const screenshot = path.join(root, 'shot.png');
  await fs.writeFile(trace, 'flow.id=' + bidding + '\nflow.status=PASS\nphase.0.step.0.screenshot=shot.png\n');
  await fs.writeFile(screenshot, Buffer.from(png, 'base64'));
  const html = inspectE2eImages(reportHtml(png), {requireCheckpoints: true});
  await verifyOriginalCaptures(root, bidding, html);
  await fs.writeFile(screenshot, '');
  await assert.rejects(() => verifyOriginalCaptures(root, bidding, html), /INVALID_PNG/);
  await fs.writeFile(trace, 'flow.id=' + bidding + '\nflow.status=FAIL\nphase.0.step.0.screenshot=shot.png\n');
  await assert.rejects(() => verifyOriginalCaptures(root, bidding, html), /TRACE_NOT_PASS/);
});
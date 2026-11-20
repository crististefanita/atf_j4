import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {test} from 'node:test';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {digest} from './evaluate-release-rag.mjs';
import {e2ePaths} from './android-e2e-contract.mjs';
import {e2eSourcePath, measureNativeValidator} from './android-e2e-provenance.mjs';
import {planAndroidE2e, exportAndroidEvidence} from './prepare-android-e2e.mjs';
import {applyAndroidEvidence} from './apply-android-e2e.mjs';
import {nativeFixture, flow, request, gameFile, scenarioFile, setSources, writePayload} from './android-e2e-provenance-fixture.mjs';

test('validator-only maintenance reuses immutable native bytes and records both source identities', async t => {
  const f = await nativeFixture(t);
  const [row] = await planAndroidE2e(f.candidate, f.current, request);
  assert.equal(row.decision, 'REUSE', row.reason);
  assert.equal(row.provenance.executionSourceCommit, f.manifest.source.sourceCommit);
  assert.equal(row.provenance.candidateSourceCommit, f.current.source.sourceCommit);
  assert.notEqual(row.provenance.executionSourceFingerprint, row.provenance.candidateSourceFingerprint);
  assert.equal(row.provenance.validator.fingerprint, (await measureNativeValidator()).fingerprint);
  const inspected = JSON.parse(execFileSync(process.execPath, [fileURLToPath(new URL('./prepare-android-e2e.mjs', import.meta.url)),
    'inspect', f.candidate, f.run], {encoding: 'utf8', timeout: 30000, windowsHide: true}));
  assert.equal(inspected[0].decision, 'REUSE');
  for (const file of Object.values(e2ePaths(flow)))
    assert.deepEqual(await fs.readFile(path.join(f.origin, file)), await fs.readFile(path.join(f.candidate, file)));
  console.log('Native source admission: '+JSON.stringify({...row.provenance, validator: {fingerprint: row.provenance.validator.fingerprint}}));
});

for (const [label, file, expected] of [
  ['game', gameFile, 'RUNTIME_CHANGED'], ['binary', 'lib/ui.aar', 'RUNTIME_CHANGED'],
  ['asset', 'whist-lab-app/app/src/main/assets/cards.png', 'RUNTIME_CHANGED'],
  ['unknown', 'new-runtime.conf', 'RUNTIME_CHANGED'], ['scenario', scenarioFile, 'SCENARIO_INPUTS_CHANGED'],
  ['launcher', 'scripts/run-whist-signed-e2e.ps1', 'SCENARIO_INPUTS_CHANGED']]) {
  test('unchanged APK hash cannot conceal changed '+label+' inputs', async t => {
    const f = await nativeFixture(t);
    const files = f.files.filter(row => row.path !== file).concat({path: file, sha256: digest('changed')});
    await setSources(f.candidate, f.current, files);
    const [row] = await planAndroidE2e(f.candidate, f.current, request);
    assert.equal(row.decision, 'RUN');assert.match(row.reason, new RegExp(expected));
  });
}

test('missing origin and stale validator pins cannot be promoted to reuse', async t => {
  const f = await nativeFixture(t);
  const stale = await planAndroidE2e(f.candidate, f.current, {...request, validatorRevision: '0'.repeat(64)});
  assert.equal(stale[0].reason, 'ANDROID_E2E_VALIDATOR_REVISION_CHANGED');
  const current = (await measureNativeValidator()).fingerprint;
  assert.equal((await planAndroidE2e(f.candidate, f.current, {...request, validatorRevision: current}))[0].decision, 'REUSE');
  await fs.unlink(path.join(f.candidate, e2eSourcePath(flow)));
  assert.equal((await planAndroidE2e(f.candidate, f.current, request))[0].decision, 'RUN');
});

test('mismatched original validator provenance is rejected even after payload rehash', async t => {
  const f = await nativeFixture(t), file = e2eSourcePath(flow);
  const source = JSON.parse(await fs.readFile(path.join(f.candidate, file)));
  source.snapshotValidatorFingerprint = '0'.repeat(64);
  await writePayload(f.candidate, f.current, file, source);
  assert.equal((await planAndroidE2e(f.candidate, f.current, request))[0].reason,
    'ANDROID_E2E_EXECUTION_VALIDATOR_PROVENANCE');
});

test('original captures and device profiles retain their independent rejection checks', async t => {
  const f = await nativeFixture(t);
  const changed = {...request, executionProfile: {...request.executionProfile, api: 35}};
  assert.equal((await planAndroidE2e(f.candidate, f.current, changed))[0].reason, 'ANDROID_E2E_SCENARIO_CHANGED');
  const paths = e2ePaths(flow);
  const report = await fs.readFile(path.join(f.candidate, paths.report), 'utf8');
  await writePayload(f.candidate, f.current, paths.report, report.replace('Capture', 'Modified capture'));
  assert.equal((await planAndroidE2e(f.candidate, f.current, request))[0].reason, 'ANDROID_E2E_REPORT_CHANGED');
});

test('re-export flattens provenance and shares the original snapshot without rewriting receipts', async t => {
  const f = await nativeFixture(t);
  const run = path.join(f.base, 'second-run');await fs.mkdir(run);
  await fs.writeFile(path.join(run, 'e2e-request.json'), JSON.stringify(request));
  await exportAndroidEvidence(f.candidate, f.current, request, run, flow);
  const source = JSON.parse(await fs.readFile(path.join(run, 'evidence', e2eSourcePath(flow))));
  assert.equal(source.sourceFingerprint, f.manifest.source.workingTreeFingerprint);
  assert.equal(source.sourceCommit, f.manifest.source.sourceCommit);
  const next = path.join(f.base, 'second-candidate', request.releaseVersion);await fs.mkdir(next, {recursive: true});
  const manifest = structuredClone(f.current);manifest.qualification.e2eGroups = {};manifest.integrity.files = [];
  await setSources(next, manifest, f.files);
  await applyAndroidEvidence(next, run);
  const final = JSON.parse(await fs.readFile(path.join(next, 'release-validation.json')));
  assert.equal((await planAndroidE2e(next, final, request))[0].decision, 'REUSE');
});

import './android-e2e-runtime-scope.test.mjs';

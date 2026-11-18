import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {componentInputMap, digest, inputFingerprint} from './source-input-identity.mjs';

const hash = /^[a-f0-9]{64}$/;
const commit = /^[a-f0-9]{40}$/;
const validatorInput = file => /^atf-tests\/tools\/[^/]+\.mjs$/.test(file);
const editorial = file => /^docs\/.+\.md$/.test(file) || /(?:^|\/)README(?:\.[\w-]+)?\.md$/.test(file)
  || file === 'config/documentation-editions.json';
const scenarioInput = file => /^(?:atf-test-android|scripts)\//.test(file);
const select = (inputs, predicate) => new Map([...inputs].filter(([file]) => predicate(file)));
const revision = inputs => ({fingerprint: inputFingerprint(inputs),
  files: [...inputs].sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0).map(([file, sha256]) => ({path: file, sha256}))});

export const e2eSourcePath = group => 'reports/e2e/' + group + '/source-provenance.json';
export function e2eSourceSnapshotPath(fingerprint) {
  if (!hash.test(fingerprint ?? '')) throw Error('ANDROID_E2E_EXECUTION_SOURCE_CHANGED');
  return 'reports/e2e/sources/' + fingerprint + '/source-inputs.json';
}

export function nativeSourceIdentity(files) {
  const inputs = componentInputMap(files);
  const validators = select(inputs, validatorInput);
  const scenarios = select(inputs, file => !editorial(file) && scenarioInput(file));
  const runtime = select(inputs, file => !editorial(file) && !validatorInput(file) && !scenarioInput(file));
  if (!runtime.size || !scenarios.size || !validators.size) throw Error('ANDROID_E2E_SOURCE_PARTITIONS_REQUIRED');
  return {runtimeFingerprint: inputFingerprint(runtime), scenarioFingerprint: inputFingerprint(scenarios),
    snapshotValidatorFingerprint: inputFingerprint(validators)};
}

export function verifyNativeSourceDocument(manifest, document) {
  const value = document.value;
  const entry = manifest.integrity?.files?.filter(row => row.path === manifest.source?.inputManifest);
  if (entry?.length !== 1 || entry[0].sha256 !== document.sha256 || entry[0].bytes !== document.bytes.length
      || !commit.test(value.sourceCommit ?? '') || value.sourceCommit !== manifest.source?.sourceCommit
      || value.fingerprint !== manifest.source?.workingTreeFingerprint
      || inputFingerprint(componentInputMap(value.files)) !== value.fingerprint)
    throw Error('ANDROID_E2E_SOURCE_PROVENANCE');
  return value;
}

/** Content-address the running validator independently of the APK's source snapshot. */
export async function measureNativeValidator(directory = path.dirname(fileURLToPath(import.meta.url))) {
  const inputs = new Map();
  for (const entry of await fs.readdir(directory, {withFileTypes: true})) {
    if (!entry.name.endsWith('.mjs')) continue;
    if (!entry.isFile()) throw Error('ANDROID_E2E_VALIDATOR_INDIRECT');
    const file = path.join(directory, entry.name);
    if (await fs.realpath(file) !== path.resolve(file)) throw Error('ANDROID_E2E_VALIDATOR_INDIRECT');
    inputs.set('atf-tests/tools/' + entry.name, digest(await fs.readFile(file)));
  }
  if (!inputs.size) throw Error('ANDROID_E2E_VALIDATOR_EMPTY');
  return {kind: 'running-native-validator', ...revision(inputs)};
}

export async function verifyNativeValidator(expected) {
  const validator = await measureNativeValidator();
  if (expected !== undefined && (!hash.test(expected) || expected !== validator.fingerprint))
    throw Error('ANDROID_E2E_VALIDATOR_REVISION_CHANGED');
  return validator;
}

export function captureNativeSource(manifest, sourceDocument, receipt, receiptSha256) {
  const source = verifyNativeSourceDocument(manifest, sourceDocument);
  if (receipt.sourceCommit !== source.sourceCommit || receipt.sourceFingerprint !== source.fingerprint)
    throw Error('ANDROID_E2E_EXECUTION_SOURCE_CHANGED');
  return {schemaVersion: 1, kind: 'android-e2e-execution-source',
    releaseVersion: receipt.releaseVersion, installedApkSha256: receipt.installedApkSha256,
    executionReceiptSha256: receiptSha256, reportSha256: receipt.reportSha256,
    sourceCommit: source.sourceCommit, sourceFingerprint: source.fingerprint,
    sourceInputManifest: e2eSourceSnapshotPath(source.fingerprint), sourceInputManifestSha256: sourceDocument.sha256,
    ...nativeSourceIdentity(source.files)};
}

export async function admitNativeSource({manifest, receipt, receiptSha256, origin, originalSource, currentSource,
  expectedValidatorFingerprint}) {
  const source = verifyNativeSourceDocument(manifest, currentSource);
  if (origin.schemaVersion !== 1 || origin.kind !== 'android-e2e-execution-source'
      || origin.releaseVersion !== manifest.release?.versionName || origin.releaseVersion !== receipt.releaseVersion
      || origin.installedApkSha256 !== manifest.release?.apkSha256 || origin.installedApkSha256 !== receipt.installedApkSha256
      || origin.executionReceiptSha256 !== receiptSha256 || origin.reportSha256 !== receipt.reportSha256
      || !commit.test(origin.sourceCommit ?? '') || origin.sourceCommit !== receipt.sourceCommit
      || origin.sourceFingerprint !== receipt.sourceFingerprint
      || origin.sourceInputManifest !== e2eSourceSnapshotPath(origin.sourceFingerprint)
      || originalSource.sha256 !== origin.sourceInputManifestSha256
      || originalSource.value.sourceCommit !== origin.sourceCommit || originalSource.value.fingerprint !== origin.sourceFingerprint
      || inputFingerprint(componentInputMap(originalSource.value.files)) !== origin.sourceFingerprint)
    throw Error('ANDROID_E2E_EXECUTION_SOURCE_CHANGED');
  const original = nativeSourceIdentity(originalSource.value.files), current = nativeSourceIdentity(source.files);
  if (Object.entries(original).some(([key, value]) => origin[key] !== value))
    throw Error('ANDROID_E2E_EXECUTION_VALIDATOR_PROVENANCE');
  if (original.runtimeFingerprint !== current.runtimeFingerprint) throw Error('ANDROID_E2E_RUNTIME_CHANGED');
  if (original.scenarioFingerprint !== current.scenarioFingerprint) throw Error('ANDROID_E2E_SCENARIO_INPUTS_CHANGED');
  const validator = await verifyNativeValidator(expectedValidatorFingerprint);
  return {executionSourceCommit: origin.sourceCommit, executionSourceFingerprint: origin.sourceFingerprint,
    candidateSourceCommit: source.sourceCommit, candidateSourceFingerprint: source.fingerprint,
    runtimeFingerprint: current.runtimeFingerprint, scenarioFingerprint: current.scenarioFingerprint,
    executionSnapshotValidatorFingerprint: original.snapshotValidatorFingerprint, validator};
}

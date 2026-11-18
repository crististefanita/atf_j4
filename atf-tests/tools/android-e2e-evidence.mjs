import {isDeepStrictEqual} from 'node:util';
import {disposablePaths, e2ePaths} from './android-e2e-contract.mjs';
import {inspectE2eImages} from './android-e2e-images.mjs';

const hash = /^[0-9a-f]{64}$/;
const passing = result => Number.isSafeInteger(result?.tests) && result.tests > 0
    && result.failures === 0 && result.errors === 0 && result.skipped === 0;

export function verifyDisposableUser(boundary, ready, cleanup, identity, scenario) {
  const leaseId = boundary?.leaseId;
  const paths = disposablePaths(leaseId);
  const userId = boundary?.testUserId;
  if (boundary?.mode !== 'DISPOSABLE_SECONDARY_USER' || paths.readyReceipt !== boundary.readyReceipt
      || paths.cleanupReceipt !== boundary.cleanupReceipt || (leaseId !== undefined && !/^[a-f0-9]{32}$/.test(leaseId))
      || boundary.readyReceiptSha256 !== ready.sha256 || boundary.cleanupReceiptSha256 !== cleanup.sha256)
    throw Error('Chitanțele utilizatorului temporar nu au căile și hash-urile canonice.');

  const start = ready.value, end = cleanup.value;
  if (start.status !== 'READY' || start.scope !== 'ANDROID_SECONDARY_USER' || start.applicationId !== identity.applicationId
      || start.createdByRun !== true || start.packageWasInstalledBefore !== false
      || typeof start.originalPackageWasInstalledBefore !== 'boolean'
      || start.screenOffTimeoutMs !== 2147483647 || start.screenUnlocked !== true
      || start.cleanupRequired !== true || start.accountsAdded !== false
      || !Number.isSafeInteger(start.originalUserId) || start.originalUserId < 0
      || !Number.isSafeInteger(userId) || userId <= 0 || userId === start.originalUserId
      || start.installedApkSha256 !== identity.installedApkSha256)
    throw Error('Chitanța de pregătire nu dovedește un utilizator secundar nou, deblocat, pentru APK-ul candidat.');
  if (end.status !== 'PASS' || end.scope !== start.scope || end.applicationId !== start.applicationId
      || end.removedByRun !== true || end.originalPackageStatePreserved !== true
      || end.originalPackageWasInstalledBefore !== start.originalPackageWasInstalledBefore
      || end.originalPackageRetained !== start.originalPackageWasInstalledBefore
      || end.originalUserId !== start.originalUserId || end.removedUserId !== start.testUserId || end.removedUserId !== userId
      || end.readyReceiptSha256 !== ready.sha256 || (leaseId !== undefined && end.leaseId !== leaseId))
    throw Error('Chitanța teardown nu dovedește eliminarea aceluiași utilizator și păstrarea stării inițiale a instalării originale.');
  if (scenario && (start.observedApi !== scenario.api || start.displayProfile !== scenario.displayProfile))
    throw Error('ANDROID_E2E_PROFILE_CHANGED');
  const version = String(identity.releaseVersion ?? '0.0.0').split('-')[0].split('.').map(Number);
  const needsUnlockedUser = version[0] > 0 || version[1] > 21 || version[1] === 21 && version[2] >= 10;
  if (needsUnlockedUser && start.userReadiness?.state !== 'RUNNING_UNLOCKED')
    throw Error('ANDROID_E2E_USER_NOT_UNLOCKED');
}

export function verifyE2eReport({manifest, name, group, receipt, report, expectedScenario, requireScenario = false}) {
  const identity = {releaseVersion: manifest.release?.versionName, installedApkSha256: manifest.release?.apkSha256,
    sourceCommit: manifest.source?.sourceCommit, sourceFingerprint: manifest.source?.workingTreeFingerprint};
  const paths = e2ePaths(name);
  if (!passing(group) || !passing(receipt) || group.tests !== receipt.tests || !String(group.protocol).includes('E2E')
      || group.report !== paths.report || group.executionReceipt !== paths.executionReceipt
      || receipt.status !== 'PASS' || receipt.protocol !== group.protocol
      || receipt.kind !== 'android-signed-release-e2e' || ![1, 2].includes(receipt.schemaVersion)
      || !Object.entries(identity).every(([key, value]) => typeof value === 'string' && receipt[key] === value))
    throw Error('ANDROID_E2E_IDENTITY_OR_VERDICT');

  const current = receipt.schemaVersion === 2;
  if (requireScenario && !current) throw Error('ANDROID_E2E_SCENARIO_NOT_RECORDED');
  if (current && (receipt.flow !== name || !expectedScenario
      || !isDeepStrictEqual({...receipt.scenario}, {...expectedScenario})
      || !hash.test(receipt.compilationReceiptSha256 ?? '') || receipt.dataBoundary?.mode !== 'DISPOSABLE_SECONDARY_USER'))
    throw Error('ANDROID_E2E_SCENARIO_CHANGED');

  const images = inspectE2eImages(report.text, {requireCheckpoints: current});
  if (receipt.reportSha256 !== report.sha256 || receipt.inlinePngCount !== images.count
      || (current && receipt.checkpointPngCount !== images.ownedCount)) throw Error('ANDROID_E2E_REPORT_CHANGED');
  return {...identity, applicationId: manifest.release?.applicationId};
}

export async function readE2eEvidence({manifest, name, declared, readDocument, readReport, scenario, requireScenario}) {
  const paths = e2ePaths(name);
  const group = manifest.qualification?.e2eGroups?.[name];
  const verifyPayload = (relative, doc) => {
    const entry = declared.get(relative);
    if (!entry || entry.sha256 !== doc.sha256 || entry.bytes !== doc.bytes.length)
      throw Error('ANDROID_E2E_PAYLOAD_MISMATCH:' + relative);
    return doc;
  };
  const receipt = verifyPayload(paths.executionReceipt, await readDocument(paths.executionReceipt));
  const report = verifyPayload(paths.report, await readReport(paths.report));
  const identity = verifyE2eReport({manifest, name, group, receipt: receipt.value, report, expectedScenario: scenario, requireScenario});
  const boundary = receipt.value.dataBoundary;
  const documents = new Map([[paths.executionReceipt, receipt], [paths.report, report]]);
  if (boundary?.mode === 'DISPOSABLE_SECONDARY_USER') {
    const paths = disposablePaths(boundary.leaseId);
    const ready = verifyPayload(paths.readyReceipt, await readDocument(paths.readyReceipt));
    const cleanup = verifyPayload(paths.cleanupReceipt, await readDocument(paths.cleanupReceipt));
    verifyDisposableUser(boundary, ready, cleanup, identity, scenario);
    documents.set(paths.readyReceipt, ready);
    documents.set(paths.cleanupReceipt, cleanup);
  } else if ((boundary && boundary.mode !== 'BACKUP_AND_RESTORE') || requireScenario) {
    throw Error('ANDROID_E2E_DATA_BOUNDARY');
  }
  return {group, documents};
}

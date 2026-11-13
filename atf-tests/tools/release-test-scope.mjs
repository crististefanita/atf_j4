import {componentEvidenceScope, changedInputs, nonBuildDocumentation} from './component-evidence-scope.mjs';

const android = 'whist-lab-app/';
const app = android + 'app/';
const appBuild = app + 'build.gradle';
const smoke = 'signed-release-smoke';
const bidding = 'signed-release-bidding';
const autoGame = 'signed-release-auto-game';
const history = 'history-release-sanity';
const achievements = 'signed-release-achievements';
const profile = 'signed-release-home-profile';
const display = 'responsive-ui-sanity';
export const allFlows = [smoke, bidding, autoGame, history, achievements, profile, display];

const rules = [
  ['bidding', /^whist-lab-app\/app\/src\/main\/java\/ro\/whistlab\/app\/bidding\//, [bidding, autoGame]],
  ['achievements', /^whist-lab-app\/app\/(?:src\/main\/(?:java\/ro\/whistlab\/app\/achievements\/|achievement-rules\/)|achievement-catalog\.gradle$)/, [achievements]],
  ['home-profile', /^whist-lab-app\/app\/src\/main\/java\/ro\/whistlab\/app\/profile\//, [profile]],
  ['history', /^whist-lab-app\/app\/src\/main\/java\/ro\/whistlab\/app\/score\//, [history]],
  ['table', /^whist-lab-app\/app\/src\/main\/java\/ro\/whistlab\/app\/(?:table|cards|animation|seat|theme|window)\//, [display, autoGame]],
  ['checkpoint', /^whist-lab-app\/app\/src\/main\/java\/ro\/whistlab\/app\/(?:persistence|lifecycle)\//, [bidding, autoGame, history, achievements]],
  ['game-core', /^whist-lab-app\/game-core\/src\/main\//, [bidding, autoGame, history, achievements]],
  ['card-table-ui', /^whist-lab-app\/atf-card-table-ui\/src\/main\//, [profile, display, autoGame, achievements]],
  ['player-names', /^whist-lab-app\/card-table-names\/src\/main\//, [profile, display]]
];

const nonProduct = /^(?:atf-validation\/|atf-tests\/|atf-test-android\/|ios\/|whist-lab-app\/(?:ai-lab|ios-bridge|test-audit|algorithm-build)\/|whist-lab-app\/[^/]+\/src\/(?:test|androidTest|rasterTest)\/|\.github\/)/;
const testConfiguration = /^config\/(?:atf-governance-policy\.json|atf-test-suites\.json|release-platform-rag-policy\.json|gradle-(?:app|audit)-collections\.properties|test-collections\.json)$/;
const testLaunchers = /^scripts\/(?:Get-AtfSourceInputs\.ps1|Copy-AtfSourceInputs\.ps1|AtfQualificationSnapshot\.ps1|check-android-release-device\.ps1|run-(?:atf-governance|component-suites|whist-collection|whist-responsive-sanity)\.ps1)$/;

function classify(file, metadataOnly) {
  if (nonBuildDocumentation(file)) return {rule: 'documentation', e2eGroups: []};
  if (file === appBuild && metadataOnly) return {rule: 'android-version-metadata', e2eGroups: []};
  for (const [rule, pattern, e2eGroups] of rules) {
    if (pattern.test(file)) return {rule, e2eGroups};
  }
  if (nonProduct.test(file) || testConfiguration.test(file) || testLaunchers.test(file)) {
    return {rule: 'non-runtime', e2eGroups: []};
  }
  return {rule: 'unclassified-or-shared-runtime', e2eGroups: allFlows};
}

export function deriveReleaseTestScope(previous, candidate, componentGroups, metadataOnly = false) {
  const changes = changedInputs(candidate, previous).map(file => ({
    path: file,
    kind: !previous.has(file) ? 'ADDED' : !candidate.has(file) ? 'DELETED' : 'MODIFIED',
    ...classify(file, metadataOnly)
  }));
  const e2eGroups = [...new Set(changes.flatMap(row => row.e2eGroups))].sort();
  const components = componentGroups.map(group => {
    const {decision, affected} = componentEvidenceScope(group, candidate, previous);
    return {group, decision, affectedInputs: affected};
  });
  return {
    kind: !changes.length ? 'UNCHANGED' : changes.some(row => row.rule === 'documentation') && changes.every(row =>
      ['documentation', 'android-version-metadata'].includes(row.rule))
      ? 'DOCUMENTATION_ONLY' : e2eGroups.length ? 'USER_FLOW' : 'MAINTENANCE',
    userFlowChanged: e2eGroups.length > 0,
    changes, componentGroups: [...componentGroups], components,
    runComponentGroups: components.filter(row => row.decision === 'RUN').map(row => row.group),
    requiredE2eGroups: e2eGroups
  };
}

// Reverse only the two literal version values and verify the reconstructed bytes against the baseline.
export function isOnlyAndroidVersionChange(previous, candidate, currentText, baselineRelease, version, digest) {
  const previousHash = previous.get(appBuild);
  if (!previousHash || !candidate.has(appBuild) || candidate.get(appBuild) === previousHash
      || typeof currentText !== 'string' || digest(currentText) !== candidate.get(appBuild)
      || !Number.isSafeInteger(baselineRelease.versionCode) || baselineRelease.versionCode < 1) return false;
  const names = [...currentText.matchAll(/^(\s*versionName[ \t]+")([^"\r\n]+)(")/gm)];
  const codes = [...currentText.matchAll(/^(\s*versionCode[ \t]+)(\d+)\b/gm)];
  if (names.length !== 1 || codes.length !== 1 || names[0][2] !== version
      || Number(codes[0][2]) <= baselineRelease.versionCode) return false;
  const reconstructed = currentText
    .replace(names[0][0], () => names[0][1] + baselineRelease.versionName + names[0][3])
    .replace(codes[0][0], () => codes[0][1] + baselineRelease.versionCode);
  return digest(reconstructed) === previousHash;
}

export const nonBuildDocumentation = file => file === 'README.md'
    || (/^(?:docs\/|lib\/README\.)/.test(file) && file.endsWith('.md'))
    || (!file.includes('/src/') && file.endsWith('/README.md'));

export function changedInputs(candidate, previous) {
  const paths = new Set([...candidate.keys(), ...previous.keys()]);
  return [...paths].filter(file => candidate.get(file) !== previous.get(file)).sort();
}
const ragPolicy = 'config/release-platform-rag-policy.json';
const disposableAndroidUserRunner = 'scripts/Invoke-AtfAndroidDisposableUser.ps1';
const androidDeviceHarness = new Set([
  disposableAndroidUserRunner, 'scripts/run-complete-match.ps1', 'scripts/run-deep-tests.ps1',
  'scripts/run-whist-ai-validation.ps1', 'scripts/run-whist-responsive-sanity.ps1'
]);
const releaseTiming = /^scripts\/(?:AtfQualificationTiming|Measure-AtfReleaseCommand)\.ps1$/;
const releaseRulesDocs = /^docs\/game\/ATF_Rules(?:Ro|En)\.md$/;
const androidUi = file => file.startsWith('whist-lab-app/app/')
    || file.startsWith('whist-lab-app/atf-card-table-ui/')
    || file === 'config/gradle-audit-collections.properties';

// Each exclusion follows the build dependency boundary of the named primary suite.
// Unknown suites and paths retain whole-snapshot comparison.
const independentChange = new Map([
  ['whist-lab-app-rules', file => (nonBuildDocumentation(file) && !releaseRulesDocs.test(file))
      || androidUi(file) || androidDeviceHarness.has(file) || /^(?:atf-tests|atf-test-android|atf-validation)\//.test(file)],
  ['atf-validation', file => nonBuildDocumentation(file) || androidUi(file)
      || androidDeviceHarness.has(file) || file.startsWith('atf-test-android/') || file.startsWith('atf-tests/')],
  ['atf-test-android', file => nonBuildDocumentation(file) || androidUi(file)
      || file.startsWith('atf-tests/')],
  ['whist-lab-app-atf-card-table-ui', file => nonBuildDocumentation(file)
      || /^(whist-lab-app\/(app|game-core|ai-lab|ios-bridge|card-table-names|algorithm-build)\/|atf-test-android\/|atf-validation\/|atf-tests\/)/.test(file)
      || androidDeviceHarness.has(file)
      || file === 'config/gradle-app-collections.properties'],
  ...['achievements', 'bidding', 'commands', 'display', 'history']
    .map(name => ['whist-lab-app-' + name, file => nonBuildDocumentation(file)
      || androidDeviceHarness.has(file)
      || file.startsWith('atf-test-android/') || file.startsWith('atf-validation/') || file.startsWith('atf-tests/')])
]);

export function componentEvidenceScope(group, releaseInputs, executionInputs) {
  const changed = changedInputs(releaseInputs, executionInputs);
  const outsideSuite = independentChange.get(group) ?? (() => false);
  const affected = changed.filter(file => !(outsideSuite(file)
    || (independentChange.has(group) && (file === ragPolicy || releaseTiming.test(file) || file.startsWith('.atf-signing/')))));
  return {decision: affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH', changed, affected};
}

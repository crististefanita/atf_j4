export const nonBuildDocumentation = file => file === 'README.md'
    || (/^(?:docs\/|lib\/README\.)/.test(file) && file.endsWith('.md'))
    || (!file.includes('/src/') && file.endsWith('/README.md'));

export function changedInputs(candidate, previous) {
  const paths = new Set([...candidate.keys(), ...previous.keys()]);
  return [...paths].filter(file => candidate.get(file) !== previous.get(file)).sort();
}
const ragPolicy = 'config/release-platform-rag-policy.json';
const androidUi = file => file.startsWith('whist-lab-app/app/')
    || file.startsWith('whist-lab-app/atf-card-table-ui/')
    || file === 'config/gradle-audit-collections.properties';

// Each exclusion follows the build dependency boundary of the named primary suite.
// Unknown suites and paths retain whole-snapshot comparison.
const independentChange = new Map([
  ['atf-validation', file => nonBuildDocumentation(file) || androidUi(file)
      || file.startsWith('atf-test-android/') || file.startsWith('atf-tests/')],
  ['atf-test-android', file => nonBuildDocumentation(file) || androidUi(file)
      || file.startsWith('atf-tests/')],
  ['whist-lab-app-atf-card-table-ui', file => nonBuildDocumentation(file)
      || /^(whist-lab-app\/(app|game-core|ai-lab|ios-bridge|card-table-names|algorithm-build)\/|atf-test-android\/|atf-validation\/|atf-tests\/)/.test(file)
      || file === 'config/gradle-app-collections.properties'],
  ...['achievements', 'bidding', 'commands', 'display', 'history']
    .map(name => ['whist-lab-app-' + name, file => nonBuildDocumentation(file)
      || file.startsWith('atf-test-android/') || file.startsWith('atf-validation/') || file.startsWith('atf-tests/')])
]);

export function componentEvidenceScope(group, releaseInputs, executionInputs) {
  const changed = changedInputs(releaseInputs, executionInputs);
  const outsideSuite = independentChange.get(group) ?? (() => false);
  const affected = changed.filter(file => !(outsideSuite(file)
    || (file === ragPolicy && independentChange.has(group))));
  return {decision: affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH', changed, affected};
}

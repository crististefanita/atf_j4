const nonBuildDocumentation = file => file === 'README.md' || file.startsWith('docs/')
    || file.startsWith('release/') || file.endsWith('/README.md') || file.startsWith('lib/README.');
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
  const paths = new Set([...releaseInputs.keys(), ...executionInputs.keys()]);
  const changed = [...paths].filter(file => releaseInputs.get(file) !== executionInputs.get(file)).sort();
  const outsideSuite = independentChange.get(group) ?? (() => false);
  const affected = changed.filter(file => !outsideSuite(file));
  return {decision: affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH', changed, affected};
}

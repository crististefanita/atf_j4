const productDocumentation = file => file === 'README.md' || file.startsWith('docs/feedback/');
const androidUi = file => file.startsWith('whist-lab-app/app/')
    || file.startsWith('whist-lab-app/atf-card-table-ui/')
    || file === 'config/gradle-audit-collections.properties';

// Each exclusion follows the build dependency boundary of the named primary suite.
// Unknown suites and paths retain whole-snapshot comparison.
const independentChange = new Map([
  ['atf-validation', file => productDocumentation(file) || androidUi(file)
      || file.startsWith('atf-test-android/') || file.startsWith('atf-tests/')],
  ['atf-test-android', file => productDocumentation(file) || androidUi(file)
      || file.startsWith('atf-tests/')],
  ...['achievements', 'bidding', 'commands', 'display', 'history', 'atf-card-table-ui']
    .map(name => ['whist-lab-app-' + name, productDocumentation])
]);

export function componentEvidenceScope(group, releaseInputs, executionInputs) {
  const paths = new Set([...releaseInputs.keys(), ...executionInputs.keys()]);
  const changed = [...paths].filter(file => releaseInputs.get(file) !== executionInputs.get(file)).sort();
  const outsideSuite = independentChange.get(group) ?? (() => false);
  const affected = changed.filter(file => !outsideSuite(file));
  return {decision: affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH', changed, affected};
}

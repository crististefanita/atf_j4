import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {test} from 'node:test';
import {allFlows, deriveReleaseTestScope, isOnlyAndroidVersionChange} from './release-test-scope.mjs';

const groups = ['atf-tests', 'atf-test-android', 'whist-lab-app-bidding', 'whist-lab-app-atf-card-table-ui'];
const build = 'whist-lab-app/app/build.gradle';
const bid = 'whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidPolicy.java';
const digest = value => createHash('sha256').update(value).digest('hex');

test('a bidding change requires the native bidding and AutoGame selections', () => {
  const selection = deriveReleaseTestScope(new Map([[bid, 'old']]), new Map([[bid, 'new']]), groups);
  assert.equal(selection.kind, 'USER_FLOW');
  assert.deepEqual(selection.requiredE2eGroups, ['signed-release-auto-game', 'signed-release-bidding']);
  assert.deepEqual(selection.runComponentGroups, ['atf-tests', 'whist-lab-app-bidding']);
  assert.deepEqual(selection.changes[0], {
    path: bid, kind: 'MODIFIED', rule: 'bidding', e2eGroups: ['signed-release-bidding', 'signed-release-auto-game']
  });
});

test('a changed app package reruns its declared owner while unchanged suites reuse hash-bound evidence', () => {
  const all = ['atf-validation', 'atf-test-android', 'atf-tests', 'whist-lab-app-achievements',
    'whist-lab-app-atf-card-table-ui', 'whist-lab-app-bidding', 'whist-lab-app-commands',
    'whist-lab-app-display', 'whist-lab-app-history', 'whist-lab-app-rules'];
  const old = new Map([['whist-lab-app/app/src/main/java/ro/whistlab/app/navigation/GameCommand.java', 'old']]);
  const current = new Map([[[...old.keys()][0], 'new']]);
  const selection = deriveReleaseTestScope(old, current, all);
  const decisions = new Map(selection.components.map(row => [row.group, row]));
  assert.deepEqual(selection.runComponentGroups, ['atf-tests', 'whist-lab-app-commands']);
  assert.equal(decisions.get('whist-lab-app-commands').decision, 'RUN');
  assert.equal(decisions.get('whist-lab-app-commands').reason, 'OWNED_OR_SHARED_INPUT_CHANGED');
  for (const group of all.filter(name => !['atf-tests', 'whist-lab-app-commands'].includes(name))) {
    assert.equal(decisions.get(group).decision, 'REUSE', group);
    assert.deepEqual(decisions.get(group).affectedInputs, [], group);
  }
});

test('package ownership covers product, score and test sources and rejects an unmapped package conservatively', () => {
  const all = ['atf-validation', 'atf-test-android', 'atf-tests', 'whist-lab-app-achievements',
    'whist-lab-app-atf-card-table-ui', 'whist-lab-app-bidding', 'whist-lab-app-commands',
    'whist-lab-app-display', 'whist-lab-app-history', 'whist-lab-app-rules'];
  for (const [file, owner] of [
    ['whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/AchievementCatalog.java', 'whist-lab-app-achievements'],
    ['whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryTableComponentTest.java', 'whist-lab-app-history'],
    ['whist-lab-app/app/src/main/java/ro/whistlab/app/localization/WhistUiText.java', 'whist-lab-app-display']
  ]) {
    const scope = deriveReleaseTestScope(new Map(), new Map([[file, 'changed']]), all);
    assert.deepEqual(scope.runComponentGroups, ['atf-tests', owner], file);
    assert.equal(scope.components.find(row => row.group === owner).decision, 'RUN', file);
  }
  const unknown = deriveReleaseTestScope(new Map(), new Map([[
    'whist-lab-app/app/src/main/java/ro/whistlab/app/newpackage/UnknownContract.java', 'changed'
  ]]), all);
  assert.deepEqual(unknown.runComponentGroups, all);
});

test('shared Gradle dependencies invalidate their consumers and unknown paths fail closed', () => {
  const all = ['atf-validation', 'atf-test-android', 'atf-tests', 'whist-lab-app-achievements',
    'whist-lab-app-atf-card-table-ui', 'whist-lab-app-bidding', 'whist-lab-app-commands',
    'whist-lab-app-display', 'whist-lab-app-history', 'whist-lab-app-rules'];
  const core = deriveReleaseTestScope(new Map(), new Map([[
    'whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/GameSession.java', 'changed'
  ]]), all);
  assert.deepEqual(core.runComponentGroups, all.filter(group => !['atf-validation', 'atf-test-android',
    'whist-lab-app-atf-card-table-ui'].includes(group)));
  const unknown = deriveReleaseTestScope(new Map(), new Map([['new-runtime/Unknown.java', 'changed']]), all);
  assert.deepEqual(unknown.runComponentGroups, all);
  assert.ok(unknown.components.every(row => row.reason === 'UNKNOWN_INPUT_FAIL_CLOSED'));
});

test('editorial additions and removals run governance and preserve reusable evidence', () => {
  const old = new Map([['README.md', 'old']]);
  const current = new Map([['docs/testing/Guide.md', 'new']]);
  const selection = deriveReleaseTestScope(old, current, groups);
  assert.equal(selection.kind, 'DOCUMENTATION_ONLY');
  assert.deepEqual(selection.runComponentGroups, ['atf-tests']);
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.deepEqual(selection.changes.map(row => row.kind), ['DELETED', 'ADDED']);
  assert.deepEqual(selection.components.map(row => row.decision), ['RUN', 'REUSE', 'REUSE', 'REUSE']);
});

test('a qualification policy edit affects governance and leaves the game build evidence reusable', () => {
  const file = 'config/release-platform-rag-policy.json';
  const selection = deriveReleaseTestScope(new Map([[file, 'old']]), new Map([[file, 'new']]), groups);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.runComponentGroups, ['atf-tests']);
  assert.deepEqual(selection.requiredE2eGroups, []);
});

test('a bilingual review record reruns governance without replaying unchanged game or device tests', () => {
  const file='config/documentation-editions.json';
  const selection=deriveReleaseTestScope(new Map([[file,'old']]),new Map([[file,'new']]),groups);
  assert.deepEqual(selection.runComponentGroups,['atf-tests']);
  assert.deepEqual(selection.requiredE2eGroups,[]);
  const changedRuntime=deriveReleaseTestScope(new Map([[bid,'old']]),new Map([[bid,'new'],[file,'new']]),groups);
  assert.deepEqual(changedRuntime.requiredE2eGroups,['signed-release-auto-game','signed-release-bidding']);
});

test('Gradle component checkpoint and audit changes keep qualification offline', () => {
  const changes = ['whist-lab-app/gradle/component-audit.gradle',
    'whist-lab-app/test-audit/src/testFixtures/java/atf/gradle/audit/verification/GradleTaskCheckpoint.java'];
  const selection = deriveReleaseTestScope(new Map(), new Map(changes.map(file => [file, 'new'])), groups);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.ok(selection.changes.every(row => row.rule === 'non-runtime'));
  assert.ok(selection.runComponentGroups.includes('whist-lab-app-bidding'));
  assert.ok(selection.runComponentGroups.includes('whist-lab-app-atf-card-table-ui'));
});

test('standalone governance launchers rerun only the offline governance component', () => {
  const paths = ['scripts/AtfStandaloneJava.ps1', 'scripts/run-jvm-inventory.ps1',
    'scripts/run-documentation-governance.ps1', 'scripts/run-atf-governance.ps1', 'scripts/AtfTestChangeBatch.ps1'];
  const candidate = new Map(paths.map(file => [file, 'new']));
  const selection = deriveReleaseTestScope(new Map(), candidate, groups);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.runComponentGroups, ['atf-tests']);
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.ok(selection.changes.every(change => change.rule === 'non-runtime'));
});

test('qualification path and UI export helpers require offline governance without native replay', () => {
  const paths = ['scripts/AtfLocalPaths.ps1', 'scripts/export-card-table-ui.ps1'];
  const selection = deriveReleaseTestScope(new Map(), new Map(paths.map(file => [file, 'new'])), groups);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.runComponentGroups, ['atf-tests']);
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.ok(selection.changes.every(change => change.rule === 'non-runtime'));
});

for (const file of ['scripts/AtfComponentJvm.ps1', 'scripts/AtfSigningSourcePolicy.ps1']) {
test(`${file} requires offline components without selecting native game flows`, () => {
  const selection = deriveReleaseTestScope(new Map(), new Map([[file, 'new']]), groups);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.ok(selection.runComponentGroups.includes('atf-tests'));
  const runtime = deriveReleaseTestScope(new Map(), new Map([[file, 'new'], [bid, 'changed']]), groups);
  assert.deepEqual(runtime.requiredE2eGroups, ['signed-release-auto-game', 'signed-release-bidding']);
});
}


test('unclassified shared inputs and runtime assets named README retain conservative E2E', () => {
  for (const file of ['config/new-runtime.json', 'whist-lab-app/app/src/main/assets/README.md', 'docs/rules.json']) {
    const selection = deriveReleaseTestScope(new Map([[file, 'old']]), new Map(), groups);
    assert.equal(selection.userFlowChanged, true, file);
    assert.equal(selection.changes[0].kind, 'DELETED', file);
    assert.deepEqual(selection.requiredE2eGroups, [...allFlows].sort(), file);
  }
});

test('the version metadata exemption must reproduce the exact baseline build bytes', () => {
  const oldBuild = 'android {\r\n    versionCode 34\r\n    versionName "0.21.3"\r\n    targetSdk 36\r\n}\r\n';
  const currentBuild = oldBuild.replace('Code 34', 'Code 35').replace('0.21.3', '0.21.4');
  const old = new Map([[build, digest(oldBuild)], ['README.md', 'old']]);
  const release = {versionName: '0.21.3', versionCode: 34};
  const forBuild = text => new Map([[build, digest(text)], ['README.md', 'new']]);
  const current = forBuild(currentBuild);
  const exact = isOnlyAndroidVersionChange(old, current, currentBuild, release, '0.21.4', digest);
  assert.equal(exact, true);
  const selection = deriveReleaseTestScope(old, current, groups, exact);
  assert.equal(selection.kind, 'DOCUMENTATION_ONLY');
  assert.equal(selection.androidVersionMetadataOnly, true);
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.deepEqual(selection.runComponentGroups, ['atf-tests']);
  assert.equal(selection.components.find(row => row.group === 'atf-tests').decision, 'RUN');
  assert.ok(selection.components.filter(row => row.group !== 'atf-tests').every(row => row.decision === 'REUSE'));
  for (const changed of [
    currentBuild.replace('targetSdk 36', 'targetSdk 35'),
    currentBuild.replace('Code 35', 'Code 34'),
    currentBuild + '    versionCode 36\r\n'
  ]) {
    assert.equal(isOnlyAndroidVersionChange(old, forBuild(changed), changed, release, '0.21.4', digest), false);
    const unsafe = deriveReleaseTestScope(old, forBuild(changed), groups);
    assert.equal(unsafe.androidVersionMetadataOnly, false);
    assert.equal(unsafe.userFlowChanged, true);
    assert.deepEqual(unsafe.runComponentGroups, ['atf-tests', 'whist-lab-app-bidding']);
  }
});

test('bilingual help content selects its offline gate when the candidate requires it', () => {
  const help = 'whist-lab-app/game-core/src/main/java/ro/whistlab/core/rules/HowToPlay.java';
  const old = new Map([[help, 'old']]), changed = new Map([[help, 'new']]);
  const selection = deriveReleaseTestScope(old, changed, [...groups, 'whist-lab-app-rules']);
  assert.equal(selection.kind, 'MAINTENANCE');
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.ok(selection.runComponentGroups.includes('whist-lab-app-rules'));
  assert.equal(selection.changes[0].rule, 'help-content');
  assert.equal(deriveReleaseTestScope(old, changed, groups).userFlowChanged, true);
  const guide = 'docs/game/ATF_RulesRo.md';
  const editorial = deriveReleaseTestScope(new Map(), new Map([[guide, 'changed']]), ['whist-lab-app-rules']);
  assert.deepEqual(editorial.runComponentGroups, ['whist-lab-app-rules']);
});

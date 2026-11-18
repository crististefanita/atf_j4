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
  assert.equal(deriveReleaseTestScope(old, current, groups, exact).kind, 'DOCUMENTATION_ONLY');
  assert.deepEqual(deriveReleaseTestScope(old, current, groups, exact).requiredE2eGroups, []);
  for (const changed of [
    currentBuild.replace('targetSdk 36', 'targetSdk 35'),
    currentBuild.replace('Code 35', 'Code 34'),
    currentBuild + '    versionCode 36\r\n'
  ]) {
    assert.equal(isOnlyAndroidVersionChange(old, forBuild(changed), changed, release, '0.21.4', digest), false);
    assert.equal(deriveReleaseTestScope(old, forBuild(changed), groups).userFlowChanged, true);
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

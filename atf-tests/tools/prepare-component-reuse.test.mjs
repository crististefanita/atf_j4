import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {componentEvidenceScope} from './component-evidence-scope.mjs';
import {digest} from './evaluate-release-rag.mjs';
import {prepareComponentReuse} from './prepare-component-reuse.mjs';

const groups = ['atf-tests', 'whist-lab-app-display'];
const inputManifest = 'source-inputs.json';
const runReceipt = 'source-component-run.json';
const provenanceReceipt = 'source-provenance.json';
const extent = '<html><img src="data:image/png;base64,AA=="></html>';
const inputs = [
  {path: 'README.md', sha256: 'a'.repeat(64)},
  {path: 'whist-lab-app/app/src/main/Screen.java', sha256: 'b'.repeat(64)}
];
const componentRun = {kind: 'live-source-components', apkQualification: false};
const writeJson = (root, name, value) => fs.writeFile(path.join(root, name), JSON.stringify(value));
const read = (root, name) => fs.readFile(path.join(root, name));

async function fixture(t, options = {}) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-component-reuse-test-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const previous = await fs.mkdir(path.join(root, 'previous')).then(() => path.join(root, 'previous'));
  const target = await fs.mkdir(path.join(root, 'target')).then(() => path.join(root, 'target'));
  await writeJson(previous, inputManifest, inputs);
  await writeJson(target, inputManifest, options.current ?? inputs);
  const original = {...componentRun, sourceFingerprint: digest(await read(previous, inputManifest)),
    inputCount: inputs.length, inputManifest};
  if (options.release) {
    const rows = [{path: 'release/index.json', sha256: 'd'.repeat(64)}];
    await writeJson(previous, 'release-inputs.json', rows);
    await writeJson(target, 'release-inputs.json', options.release === 'changed'
      ? [...rows, {path: 'release/extra.json', sha256: 'e'.repeat(64)}] : rows);
    original.releaseInputManifest = 'release-inputs.json';
    original.releaseFingerprint = digest(await read(previous, original.releaseInputManifest));
  }
  await writeJson(previous, provenanceReceipt, original);
  const provenanceSha256 = digest(await read(previous, provenanceReceipt));
  const results = [];
  for (const module of groups) {
    await fs.mkdir(path.join(previous, module));
    const receipt = {...componentRun, tests: 1, outcomes: {PASS: 1}, coverageErrors: [], containerFailures: [],
      provenanceSha256, reportSha256: digest(extent)};
    results.push({module, exitCode: module === options.failed ? 1 : 0, receipt});
    await writeJson(previous, module + '/execution.json', receipt);
    await fs.writeFile(path.join(previous, module, 'component-report.html'), extent);
  }
  await writeJson(previous, runReceipt, {...original, provenanceReceiptSha256: provenanceSha256, sourceStable: true,
    status: options.failed ? 'FAIL' : 'PASS', results});
  return {root, previous, target, provenanceSha256};
}

test('identical inputs preserve the original bytes and a single snapshot across reuse runs', async t => {
  const {root, previous, target, provenanceSha256} = await fixture(t);
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['MATCH', 'MATCH']);
  const snapshots = await fs.readdir(path.join(target, 'snapshots'));
  assert.deepEqual(snapshots, [provenanceSha256]);
  for (const file of [inputManifest, provenanceReceipt, runReceipt]) {
    assert.deepEqual(await read(target, 'snapshots/' + provenanceSha256 + '/' + file), await read(previous, file));
  }
  const next = path.join(root, 'next');
  await fs.mkdir(next);
  await writeJson(next, inputManifest, inputs);
  const chained = await prepareComponentReuse(next, target, groups);
  assert.deepEqual(chained, results);
  for (const group of groups) {
    for (const file of ['execution.json', 'component-report.html']) {
      assert.deepEqual(await read(next, group + '/' + file), await read(previous, group + '/' + file));
    }
  }
});

test('editorial changes retain the independent suite and schedule the governance selection', async t => {
  const current = [{...inputs[0], sha256: 'c'.repeat(64)}, inputs[1]];
  const {previous, target} = await fixture(t, {current});
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['RUN', 'REUSE']);
  assert.deepEqual(results.map(row => row.affectedInputs), [['README.md'], []]);
  await assert.rejects(fs.stat(path.join(target, groups[0])), {code: 'ENOENT'});
});

test('only governance requires a matching release inventory for reuse', async t => {
  const {previous, target, provenanceSha256} = await fixture(t, {release: 'changed'});
  const decisions = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(decisions.map(row => row.decision), ['INVALID', 'MATCH']);
  assert.equal(decisions[0].reason, 'GOVERNANCE_RELEASE_SNAPSHOT_CHANGED');
  const original = 'snapshots/' + provenanceSha256 + '/release-inputs.json';
  assert.deepEqual(await read(target, original), await read(previous, 'release-inputs.json'));
});

test('a modified source file within the selection schedules fresh execution', async t => {
  const {previous, target} = await fixture(t, {current: [inputs[0]]});
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['RUN', 'RUN']);
  assert.deepEqual(results.map(row => row.affectedInputs), [[inputs[1].path], [inputs[1].path]]);
});

test('a failed task cannot be reused while a validated sibling can', async t => {
  const {previous, target} = await fixture(t, {failed: groups[0]});
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['INVALID', 'MATCH']);
});

test('a modified Extent or a receipt swapped across runs requires fresh execution', async t => {
  const {previous, target} = await fixture(t);
  await fs.appendFile(path.join(previous, groups[0], 'component-report.html'), '<!-- different -->');
  const file = groups[1] + '/execution.json';
  const receipt = JSON.parse(await read(previous, file));
  await writeJson(previous, file, {...receipt, provenanceSha256: '0'.repeat(64)});
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['INVALID', 'INVALID']);
  await assert.rejects(fs.stat(path.join(target, 'snapshots')), {code: 'ENOENT'});
});

test('a changed provenance document invalidates every linked receipt', async t => {
  const {previous, target} = await fixture(t);
  await fs.appendFile(path.join(previous, provenanceReceipt), '\n');
  const results = await prepareComponentReuse(target, previous, groups);
  assert.deepEqual(results.map(row => row.decision), ['INVALID', 'INVALID']);
});

test('the detached card-table suite can reuse consumer edits and invalidates its own code, resources and audit inputs', () => {
  const group = 'whist-lab-app-atf-card-table-ui';
  const original = new Map([['whist-lab-app/atf-card-table-ui/src/main/AndroidManifest.xml', 'original']]);
  for (const file of ['whist-lab-app/app/build.gradle', 'whist-lab-app/app/src/main/GameScreen.java',
    'whist-lab-app/game-core/src/main/Rules.java', 'config/gradle-app-collections.properties']) {
    const changed = new Map([...original, [file, 'new']]);
    assert.equal(componentEvidenceScope(group, changed, original).decision, 'REUSE', file);
    assert.equal(componentEvidenceScope('whist-lab-app-display', changed, original).decision, 'RUN', file);
  }
  for (const file of ['whist-lab-app/atf-card-table-ui/src/main/AndroidManifest.xml',
    'whist-lab-app/atf-card-table-ui/src/main/res/values/styles.xml',
    'whist-lab-app/atf-card-table-ui/src/test/SeatFactLayoutComponentTest.java',
    'whist-lab-app/test-audit/build.gradle', 'config/gradle-audit-collections.properties',
    'whist-lab-app/gradle/wrapper/gradle-wrapper.properties', 'lib/atf-reporting.jar', 'unknown-input']) {
    const changed = new Map([...original, [file, 'new']]);
    const scope = componentEvidenceScope(group, changed, original);
    assert.equal(scope.decision, 'RUN', file);
    assert.deepEqual(scope.affected, [file]);
  }
});

test('documentation and separate test-harness changes reuse unaffected Gradle game components', () => {
  const baseline = new Map([['whist-lab-app/app/src/main/GameScreen.java', 'unchanged']]);
  const unrelatedChanges = [
    'atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseBidE2eTest.java',
    'docs/testing/ATF_ReleaseQualification.md', 'release/0.21.2/README.md', 'lib/README.Reporting.md',
    'atf-tests/tools/component-evidence-scope.mjs'
  ];
  for (const file of unrelatedChanges) {
    const changed = new Map([...baseline, [file, 'updated']]);
    for (const group of ['whist-lab-app-achievements', 'whist-lab-app-bidding', 'whist-lab-app-commands',
      'whist-lab-app-display', 'whist-lab-app-history']) {
      const scope = componentEvidenceScope(group, changed, baseline);
      assert.equal(scope.decision, 'REUSE', `${group}: ${file}`);
      assert.deepEqual(scope.affected, [], `${group}: ${file}`);
    }
    if (file.startsWith('atf-tests/'))
      assert.equal(componentEvidenceScope('whist-lab-app-atf-card-table-ui', changed, baseline).decision, 'REUSE');
  }
  const changedTest = new Map([...baseline, [unrelatedChanges[0], 'updated']]);
  assert.equal(componentEvidenceScope('atf-test-android', changedTest, baseline).decision, 'RUN');
  assert.equal(componentEvidenceScope('atf-validation', changedTest, baseline).decision, 'REUSE');
  for (const file of unrelatedChanges.slice(1)) {
    const changed = new Map([...baseline, [file, 'updated']]);
    assert.equal(componentEvidenceScope('atf-validation', changed, baseline).decision, 'REUSE', `atf-validation: ${file}`);
    assert.equal(componentEvidenceScope('atf-test-android', changed, baseline).decision, 'REUSE', `atf-test-android: ${file}`);
  }
  const appChanged = new Map([...changedTest, ['whist-lab-app/app/src/main/GameScreen.java', 'updated']]);
  assert.equal(componentEvidenceScope('whist-lab-app-display', appChanged, baseline).decision, 'RUN');
});

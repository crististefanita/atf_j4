import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import test from 'node:test';
import {visualChangeContract, visualContractPaths} from './visual-change-contracts.mjs';
import {readVisualContractSources} from './visual-contract-sources.mjs';
import {loadReleaseTestScope, inputFingerprint} from './evaluate-release-rag.mjs';
import {allFlows} from './release-test-scope.mjs';
import {header, headerText, hash} from './visual-scope-fixture.mjs';

const project = fileURLToPath(new URL('../../', import.meta.url));

test('registered visual contracts retain live source and declared component owners', async () => {
  const policy = JSON.parse(await fs.readFile(path.join(project, 'config/release-platform-rag-policy.json')));
  const owners = [...policy.ragEvaluator.requiredComponentGroupsByPlatform.android,
    ...policy.ragEvaluator.introducedComponentGroupsByPlatform.android.map(row => row.group)];
  const collections = await fs.readFile(path.join(project, 'config/gradle-app-collections.properties'), 'utf8');
  const audit = await fs.readFile(path.join(project, 'config/gradle-audit-collections.properties'), 'utf8');
  const guide = await fs.readFile(path.join(project, 'docs/testing/ATF_ReleaseQualification.md'), 'utf8');
  assert.match(collections, /^theme=display\r?$/m);
  for (const owner of ['ro.whistlab.app.theme.GameHeaderTypographyComponentTest', 'ro.cardtable.ui.cards.CardTypographyComponentTest']) {
    assert.ok(audit.split(/\r?\n/).includes(owner + '=component'), owner);
  }
  for (const file of visualContractPaths) {
    const before = await fs.readFile(path.join(project, file));
    const field = file === header ? 'TITLE_SP' : 'RANK_SCALE';
    const after = Buffer.from(before.toString().replace(new RegExp('(' + field + ' = )([0-9.]+)f'),
      (_, prefix, value) => prefix + (Number(value) + 1) + 'f'));
    const selected = visualChangeContract(file, hash(before), hash(after), {before, after}, owners);
    assert.equal(selected.status, 'PROVEN', file);
    assert.ok(selected.e2eGroups.includes('signed-release-smoke'));
    assert.ok(selected.e2eGroups.includes('responsive-ui-sanity'));
    const row = guide.split(/\r?\n/).find(line => line.startsWith('|') && line.includes('`' + selected.allowedConstants[0] + '`'));
    assert.ok(row, 'The owning guide must document ' + selected.id);
    const codes = text => [...text.matchAll(/`([^`]+)`/g)].map(match => match[1]).sort();
    const documented = line => line.split('|').slice(1, -1).slice(1).map(codes);
    const expected = [selected.allowedConstants, selected.componentGroups, selected.e2eGroups].map(values => [...values].sort());
    assert.deepEqual(documented(row), expected, 'Documented fields, component owners and native gates must match live policy');
    assert.throws(() => assert.deepEqual(documented(row.replace('`signed-release-smoke`', '`wrong-native-flow`')), expected),
      {code: 'ERR_ASSERTION'}, 'A changed native gate in documentation cannot pass the factual contract');
  }
});

test('source reader binds both hashes and restores only verified uniform checkout EOL', async () => {
  const before = Buffer.from(headerText.replaceAll('\n', '\r\n')), after = Buffer.from(headerText.replace('14f', '16f'));
  const previous = new Map([[header, hash(before)]]), candidate = new Map([[header, hash(after)]]);
  const read = (readBefore, readAfter = async () => after) => readVisualContractSources('', '', 'a'.repeat(40), previous, candidate, {readBefore, readAfter});
  const result = await read(async () => Buffer.from(headerText));
  assert.deepEqual(result.get(header), {before, after});
  assert.equal((await read(async () => Buffer.from('wrong Git blob'))).size, 0);
  assert.equal((await read(async () => { throw Error('baseline unavailable'); })).size, 0);
  const wrongAfter = await read(async () => before, async () => Buffer.from('wrong candidate'));
  assert.equal(visualChangeContract(header, previous.get(header), candidate.get(header), wrongAfter.get(header), []).reason, 'SOURCE_HASH_MISMATCH');
});

async function planner(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'visual-scope-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const repository = path.join(root, 'project'), source = path.join(root, 'source');
  const write = async (root, name, bytes) => { const file = path.join(root, name); await fs.mkdir(path.dirname(file), {recursive: true}); await fs.writeFile(file, bytes); };
  await write(repository, header, headerText);
  const git = (...args) => execFileSync('git', ['-C', repository, '-c', 'core.autocrlf=false',
    '-c', 'user.name=Scope fixture', '-c', 'user.email=fixture@example.invalid', ...args], {encoding: 'utf8', stdio: ['ignore','pipe','pipe']}).trim();
  git('init', '-q'); git('add', '.'); git('commit', '-qm', 'source');
  const commit = git('rev-parse', 'HEAD'), fingerprint = inputFingerprint(new Map([[header, hash(headerText)]]));
  const input = {sourceCommit: commit, fingerprint, files: [{path: header, sha256: hash(headerText)}]};
  await write(repository, 'config/release-platform-rag-policy.json', await fs.readFile(path.join(project, 'config/release-platform-rag-policy.json')));
  await write(repository, 'release/index.json', JSON.stringify({schemaVersion: 2, currentVersion: '0.21.11', versions: ['0.21.11', '0.21.10']}));
  const manifest = {release: {versionName: '0.21.11'}, qualification: {state: 'PASS'}, publication: {state: 'PUBLISHED'},
    source: {sourceCommit: commit, workingTreeFingerprint: fingerprint, inputManifest: 'reports/apk-inputs.json'}};
  const baseline = 'release/0.21.11/';
  const saveBaseline = async () => {
    const bytes = Buffer.from(JSON.stringify(input));
    manifest.integrity = {files: [{path: 'reports/apk-inputs.json', bytes: bytes.length, sha256: hash(bytes)}]};
    await write(repository, baseline + 'release-validation.json', JSON.stringify(manifest));
    await write(repository, baseline + 'reports/apk-inputs.json', bytes);
  };
  await saveBaseline();
  const after = headerText.replace('14f', '16f');
  await write(source, header, after);
  const rows = [{path: header, sha256: hash(after)}];
  return {repository, source, write, manifest, input, baseline, rows, saveBaseline,
    run: () => loadReleaseTestScope(repository, source, '0.21.12', rows)};
}

test('the real planner narrows only a pinned numeric contract and retains native sanity', async t => {
  const f = await planner(t), scope = await f.run();
  assert.deepEqual(scope.requiredE2eGroups, ['responsive-ui-sanity', 'signed-release-smoke']);
  assert.equal(scope.changes[0].visualContract.status, 'PROVEN');
  assert.ok(scope.runComponentGroups.includes('whist-lab-app-display'));
  assert.equal(scope.userFlowChanged, true);
  await f.write(f.source, header, headerText);
  await assert.rejects(f.run(), /PINNED_SOURCE_HASH/);
});

test('a missing baseline Git object keeps every native flow instead of trusting a visual claim', async t => {
  const f = await planner(t);
  f.manifest.source.sourceCommit = f.input.sourceCommit = 'e'.repeat(40);
  await f.saveBaseline();
  const scope = await f.run();
  assert.deepEqual(scope.requiredE2eGroups, [...allFlows].sort());
  assert.equal(scope.changes[0].visualContract.reason, 'SOURCE_CONTENT_UNAVAILABLE');
});

import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import test from 'node:test';
import {checkout} from './release-git-fixture.mjs';
import {verifyPublicationAudit} from './publication-audit.mjs';

async function fixture(t, mutate = async () => {}) {
  const f = await checkout(t, {version: '0.22'});
  const file = path.join(f.candidate, 'release-validation.json');
  const manifest = JSON.parse(await fs.readFile(file));
  manifest.publication.state = 'QUALIFIED';
  manifest.qualification = {state: 'PASS', auditedGroups: {'atf-tests': {report: 'reports/report.html'}}};
  await fs.writeFile(path.join(f.candidate, 'reports/README.md'), '[Extent](report.html)\n');
  const original = structuredClone(manifest);
  await mutate(f, manifest);
  await fs.writeFile(file, JSON.stringify(manifest));
  f.git('add', '.'); f.git('commit', '-qm', 'source-bound qualified audit');
  const commit = f.git('rev-parse', 'HEAD');
  const url = 'https://github.com/example/game/blob/' + commit + '/release/0.22/reports/README.md';
  return {...f, commit, candidateData: {identity: {tag: 'v0.22', repository: 'example/game', commit: original.source.sourceCommit},
    manifest: original, notes: '[Extent](' + url + ')\n'}};
}

test('a source-bound qualified audit is accepted before publication', async t => {
  const f = await fixture(t);
  const result = verifyPublicationAudit(f.repository, f.candidateData);
  assert.equal(result.status, 'PASS');
  assert.equal(result.commit, f.commit);
  assert.equal(result.sourceCommit, f.candidateData.identity.commit);
});

for (const [label, mutate, diagnostic] of [
  ['absent prepared source', async (f, value) => {value.source.sourceCommit = null;}, /GITHUB_AUDIT_SOURCE/],
  ['foreign source', async (f, value) => {value.source.sourceCommit = 'a'.repeat(40);}, /GITHUB_AUDIT_SOURCE/],
  ['another APK', async (f, value) => {value.release.apkSha256 = 'b'.repeat(64);}, /GITHUB_AUDIT_SOURCE/],
  ['changed report', async f => {await fs.writeFile(path.join(f.candidate, 'reports/report.html'), 'changed');}, /GITHUB_AUDIT_REPORT/],
  ['missing report', async f => {await fs.unlink(path.join(f.candidate, 'reports/report.html'));}, /GITHUB_AUDIT_BLOB/],
  ['indirect index', async f => {await fs.writeFile(path.join(f.candidate, 'reports/README.md'), '[Other](other.html)');}, /GITHUB_AUDIT_INDEX/]
]) test('publication rejects ' + label + ' without GitHub access', async t => {
  const f = await fixture(t, mutate);
  assert.throws(() => verifyPublicationAudit(f.repository, f.candidateData), diagnostic);
});

test('foreign, mutable and duplicate audit URLs are rejected', async t => {
  const f = await fixture(t), original = f.candidateData.notes;
  for (const notes of [original.replace('example/game', 'other/game'), original.replace(f.commit, 'main'), original + original,
    original.replace('README.md)', 'README.md?query=1)')]) {
    assert.throws(() => verifyPublicationAudit(f.repository, {...f.candidateData, notes}), /GITHUB_AUDIT_LINK/);
  }
});

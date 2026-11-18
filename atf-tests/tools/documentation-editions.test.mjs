import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import test from 'node:test';
import {compareDocumentationEditions, documentationEditionsPolicy, matchesDocumentationEditionsPolicy, inspectDocumentationEditions, verifyReviewedDocumentation} from './documentation-editions.mjs';

test('the bilingual policy accepts secured JSON records and rejects missing or extra fields', () => {
  const secured = Object.assign(Object.create(null), documentationEditionsPolicy);
  assert.equal(matchesDocumentationEditionsPolicy(secured), true);
  assert.equal(matchesDocumentationEditionsPolicy({...documentationEditionsPolicy}), true);
  for (const invalid of [null, undefined, [], {...secured, englishBranch:'master'},
    {...secured, reviewPath:'elsewhere.json'}, {...secured, extra:true}, {...secured, requiredFromVersion:undefined}])
    assert.equal(matchesDocumentationEditionsPolicy(invalid), false);
});

const fence = '\x60'.repeat(3);
const markdown = (heading, label, target = 'docs/rules.md', command = 'node check.mjs') =>
  '# Whist\n\n## ' + heading + '\n\n' + label + ' [' + label + '](' + target + ').\n\n'
  + fence + 'sh\n' + command + '\n' + fence + '\n';
const files = (...rows) => new Map(rows.map(([path, text]) => [path, Buffer.from(text)]));
const english = () => files(['README.md', markdown('Overview', 'Rules')], ['docs/rules.md', '# Rules\n']);
const romanian = () => files(['README.md', markdown('Pe scurt', 'Reguli')], ['docs/rules.md', '# Reguli\n']);

test('compares linked documents and counts the English companion of a retained original once', () => {
  const en = english(), ro = romanian(), path = 'release/0.21.9/README.md';
  en.set(path, Buffer.from('# Dovezi\n'));
  ro.set(path, en.get(path));
  en.set('release/0.21.9/README.en.md', Buffer.from('# Evidence\n'));
  const audit = compareDocumentationEditions(en, ro);
  assert.deepEqual(audit.problems, []);
  assert.equal(audit.review.documents, 3);
});

test('canonical release guides can be translated without retaining duplicate language Markdown files', () => {
  const en = english(), ro = romanian(), path = 'release/0.21.9/README.md';
  en.set(path, Buffer.from('# Release\n\n## Overview\n\nPublished evidence.\n'));
  ro.set(path, Buffer.from('# Ediție\n\n## Pe scurt\n\nDovezi publicate.\n'));
  assert.deepEqual(compareDocumentationEditions(en, ro).problems, []);
  assert.equal(compareDocumentationEditions(en, ro).review.documents, 3);
});

test('rejects a missing translation, a changed link and a changed command', () => {
  const en = english(), ro = romanian();
  ro.delete('docs/rules.md');
  en.set('README.md', Buffer.from(markdown('Overview', 'Rules', 'docs/other.md', 'node other.mjs')));
  assert.deepEqual(compareDocumentationEditions(en, ro).problems.sort(), [
    'Link destinations differ: README.md', 'Markdown structure differs: README.md', 'Missing counterpart: docs/rules.md'
  ]);
});

test('localized control explanations do not permit changes to technical YAML fields', () => {
  const en = english(), ro = romanian();
  const guide = reason => '# Guide\n\n```yaml\nid: CHECK-01\nbase: abc123\nreason: ' + reason + '\n```\n';
  en.set('docs/control.md', Buffer.from(guide('Preserve the verified contract')));
  ro.set('docs/control.md', Buffer.from(guide('Păstrează contractul verificat')));
  assert.deepEqual(compareDocumentationEditions(en, ro).problems, []);
  ro.set('docs/control.md', Buffer.from(guide('Păstrează contractul verificat').replace('abc123','different')));
  assert.deepEqual(compareDocumentationEditions(en, ro).problems, ['Markdown structure differs: docs/control.md']);
});

test('checks the orientation of the actual editions', () => {
  const en = english(), ro = romanian();
  ro.set('README.md', en.get('README.md'));
  assert.deepEqual(compareDocumentationEditions(en, ro).problems, ['The Romanian README must use Pe scurt.']);
});

test('an unchanged layout cannot hide unreviewed Romanian or English prose', t => {
  const root = mkdtempSync(join(tmpdir(), 'whist-editions-'));
  t.after(() => rmSync(root, {recursive: true, force: true}));
  const git = (...args) => execFileSync('git', ['-C', root, ...args], {encoding: 'utf8', windowsHide: true}).trim();
  const write = (relative, contents) => writeFileSync(join(root, relative), contents);
  const commit = message => {
    git('add', '--all');
    git('-c', 'user.name=Component Test', '-c', 'user.email=component@example.invalid',
      '-c', 'commit.gpgSign=false', 'commit', '-m', message);
    return git('rev-parse', 'HEAD');
  };

  git('init', '-b', 'main_RO');
  mkdirSync(join(root, 'docs'));
  mkdirSync(join(root, 'config'));
  write('config/atf-governance-policy.json', '{"documentationLanguage":"ro"}');
  write('README.md', markdown('Pe scurt', 'Reguli'));
  write('docs/rules.md', '# Reguli\n');
  const ro = commit('Romanian edition');
  git('remote', 'add', 'origin', root);
  git('switch', '-c', 'main');
  write('config/atf-governance-policy.json', '{"documentationLanguage":"en"}');
  write('README.md', markdown('Overview', 'Rules'));
  write('docs/rules.md', '# Rules\n');
  const en = commit('English edition');
  const reviewed = inspectDocumentationEditions(root, en, ro);
  write('config/documentation-editions.json', JSON.stringify(reviewed.review));
  const pinned = commit('Record bilingual review');
  assert.deepEqual(verifyReviewedDocumentation(root, pinned).problems, []);

  write('docs/rules.md', '# Modified rules\n');
  const edited = commit('English-only edit');
  assert.deepEqual(verifyReviewedDocumentation(root, edited).problems,
    ['Documentation changed since the recorded bilingual review.']);
  write('config/atf-governance-policy.json', '{"documentationLanguage":"ro"}');
  assert.ok(verifyReviewedDocumentation(root, commit('Switch English governance to Romanian')).problems
    .includes('The English branch must configure English documentation governance.'));

  git('switch', 'main_RO');
  write('docs/rules.md', '# Reguli modificate\n');
  const newerRomanian = commit('Romanian-only edit');
  assert.deepEqual(verifyReviewedDocumentation(root, pinned).problems, [], 'A later branch edit cannot rewrite the reviewed release pair');
  assert.notDeepEqual(inspectDocumentationEditions(root,pinned,newerRomanian).review,reviewed.review,
    'The current pair still requires a new review after changing Romanian prose');
  const unrelated = git('-c','user.name=Component Test','-c','user.email=component@example.invalid',
    'commit-tree',git('rev-parse','HEAD^{tree}'),'-m','unrelated history');
  git('update-ref','refs/heads/main_RO',unrelated);
  assert.throws(()=>verifyReviewedDocumentation(root,pinned),/Cannot read documentation Git input/,
    'An unrelated replacement branch cannot publish the pinned Romanian commit');
});

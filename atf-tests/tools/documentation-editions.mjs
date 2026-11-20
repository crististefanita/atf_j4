import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {posix} from 'node:path';
import {fileURLToPath} from 'node:url';
import {isDeepStrictEqual} from 'node:util';

export const documentationEditionsPolicy = Object.freeze({
  englishBranch: 'main',
  romanianBranch: 'main_RO',
  reviewPath: 'config/documentation-editions.json',
  requiredFromVersion: '0.21.10'
});
export function matchesDocumentationEditionsPolicy(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    && isDeepStrictEqual({...value}, documentationEditionsPolicy);
}
const COMMIT = /^[a-f0-9]{40}$/;
const ARCHIVED = /^release\/\d+\.\d+(?:\.\d+)?(?:-[\w.-]+)?\/.+\.md$/;
const FENCE = /^\s*(~{3,}|\x60{3,})(\w*)\s*$/;
const LINK = /!?\[(?:[^[\]]|\[[^[\]]*\])*\]\(((?:[^()]|\([^()]*\))*)\)/g;
const REPO_LINK = /^https:\/\/github\.com\/crististefanita\/whist\/blob\/(?:main|main_RO)\//;
const utf8 = new TextDecoder('utf-8', {fatal: true});
const sha256 = bytes => createHash('sha256').update(bytes).digest('hex');

function git(project, args, input) {
  const run = spawnSync('git', ['-C', project, ...args], {
    input, windowsHide: true, maxBuffer: 32 * 1024 * 1024, timeout: 30_000,
    env: {...process.env, GIT_TERMINAL_PROMPT: '0'}
  });
  if (run.status !== 0) throw new Error('Cannot read documentation Git input: ' + args[0]);
  return run.stdout;
}

function resolveCommit(project, ref) {
  const commit = git(project, ['rev-parse', '--verify', '--end-of-options', ref + '^{commit}']).toString().trim();
  if (!COMMIT.test(commit)) throw new Error('Invalid documentation commit');
  return commit;
}

function filesAt(project, commit, paths) {
  if (!paths.length) return new Map();
  const requests = paths.map(path => commit + ':' + path);
  if (requests.some(request => /[\r\n]/.test(request))) throw new Error('Unsupported documentation path');
  const response = git(project, ['cat-file', '--batch'], requests.join('\n') + '\n');
  const files = new Map();
  let start = 0;
  for (const path of paths) {
    const end = response.indexOf('\n', start);
    const header = /^([a-f0-9]{40}) blob (\d+)$/.exec(response.subarray(start, end).toString());
    if (!header) throw new Error('Missing documentation blob: ' + path);
    const next = end + 1 + Number(header[2]);
    if (response[next] !== 10) throw new Error('Invalid documentation blob: ' + path);
    files.set(path, response.subarray(end + 1, next));
    start = next + 1;
  }
  return files;
}

function readEdition(project, commit) {
  const paths = git(project, ['ls-tree', '-r', '--name-only', '-z', commit]).toString().split('\0')
    .filter(path => path.endsWith('.md'));
  if (!paths.length) throw new Error('Empty documentation inventory at ' + commit);
  return filesAt(project, commit, paths);
}

function linkDestination(path, link) {
  if (REPO_LINK.test(link)) return link.replace(REPO_LINK, '').split('#', 1)[0];
  if (/^[a-z][a-z0-9+.-]*:/i.test(link)) return link;
  const [target] = link.split('#', 1);
  return target ? posix.normalize(posix.join(posix.dirname(path), target)).replace(/\.(en|ro)\.md$/, '.md') : path;
}

function diagramStructure(line) {
  return line.replace(/\|[^|]+\|/g, '||').replace(/\[[^[\]]*\]/g, '[]')
    .replace(/\{[^{}]*\}/g, '{}').replace(/\(\([^()]+\)\)/g, '(())')
    .replace(/^(\s*(?:participant|actor)\s+\S+\s+as\s+).+$/, '$1')
    .replace(/^(\s*(?:loop|opt|alt|else|par|and|critical|break)\s+).+$/, '$1')
    .replace(/^(\s*(?:Note|note)\s+[^:]+:\s*).+$/, '$1')
    .replace(/^(\s*[A-Za-z]\w*\s*[-]+[x>]+[^:]+:\s*).+$/, '$1').trimEnd();
}

function markdownContract(path, text) {
  const structure = [], destinations = [];
  let inside = null;
  for (const line of text.replaceAll('\r\n', '\n').split('\n')) {
    const fence = FENCE.exec(line);
    if (fence) {
      structure.push(['fence', fence[1], fence[2]]);
      inside = inside ? null : fence[2] || 'code';
    } else if (inside) {
      structure.push([inside, inside === 'mermaid' ? diagramStructure(line)
        : inside === 'yaml' ? line.replace(/^(\s*reason:\s*).+$/, '$1<localized explanation>') : line]);
    } else if (line.trim()) {
      const heading = /^(\s*#{1,6})\s+/.exec(line);
      const list = /^(\s*(?:[-+*]|\d+\.)\s+)/.exec(line);
      const html = /^\s*(<\/?(?:details|summary)\b)/.exec(line);
      if (heading) structure.push(['heading', heading[1]]);
      else if (list) structure.push(['list', list[1]]);
      else if (html) structure.push(['html', html[1]]);
      else if (line.trimStart().startsWith('|')) structure.push(['row', (line.match(/\|/g) ?? []).length]);
      else structure.push(['text']);
      for (const [, link] of line.matchAll(LINK)) destinations.push(linkDestination(path, link));
    }
  }
  return {structure, destinations};
}

function visibleEdition(files, language) {
  const visible = new Map(files);
  for (const [path, bytes] of files) {
    if (!ARCHIVED.test(path) || !path.endsWith('.' + language + '.md')) continue;
    const original = path.replace(/\.(en|ro)\.md$/, '.md');
    if (!files.has(original)) throw new Error('No archived original for ' + path);
    visible.set(original, bytes);
    visible.delete(path);
  }
  return visible;
}

function inventoryHash(files) {
  return sha256([...files].sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0)
    .map(([path, bytes]) => path + '\t' + sha256(bytes) + '\n').join(''));
}

export function compareDocumentationEditions(englishFiles, romanianFiles) {
  const english = visibleEdition(englishFiles, 'en');
  const romanian = visibleEdition(romanianFiles, 'ro');
  const problems = [];
  const orientation = (files, heading) => files.get('README.md')
    && new RegExp('^## ' + heading + '\\s*$', 'm').test(utf8.decode(files.get('README.md')));
  if (!orientation(english, 'Overview')) problems.push('The English README must use Overview.');
  if (!orientation(romanian, 'Pe scurt')) problems.push('The Romanian README must use Pe scurt.');

  for (const path of new Set([...english.keys(), ...romanian.keys()])) {
    if (!english.has(path) || !romanian.has(path)) {
      problems.push('Missing counterpart: ' + path);
      continue;
    }
    const en = markdownContract(path, utf8.decode(english.get(path)));
    const ro = markdownContract(path, utf8.decode(romanian.get(path)));
    if (!isDeepStrictEqual(en.structure, ro.structure)) problems.push('Markdown structure differs: ' + path);
    if (!isDeepStrictEqual(en.destinations, ro.destinations)) problems.push('Link destinations differ: ' + path);
  }

  return {problems, review: {schemaVersion: 1, englishBranch: 'main', romanianBranch: 'main_RO',
    documents: english.size, englishSha256: inventoryHash(englishFiles), romanianSha256: inventoryHash(romanianFiles)}};
}

export function inspectDocumentationEditions(project, englishRef, romanianRef) {
  const englishCommit = resolveCommit(project, englishRef);
  const romanianCommit = resolveCommit(project, romanianRef);
  const checked = compareDocumentationEditions(readEdition(project, englishCommit), readEdition(project, romanianCommit));
  const configuration = 'config/atf-governance-policy.json';
  const language = commit => JSON.parse(utf8.decode(filesAt(project, commit, [configuration]).get(configuration)))
    .documentationLanguage ?? 'ro';
  if (language(englishCommit) !== 'en') checked.problems.push('The English branch must configure English documentation governance.');
  if (language(romanianCommit) !== 'ro') checked.problems.push('The Romanian branch must configure Romanian documentation governance.');
  checked.review = {...checked.review, schemaVersion:2, romanianCommit};
  return {englishCommit, romanianCommit, ...checked};
}

export function verifyReviewedDocumentation(project, englishCommit) {
  if (!COMMIT.test(englishCommit ?? '')) throw new Error('A pinned English source commit is required.');
  const remote = git(project, ['ls-remote', '--exit-code', 'origin', 'refs/heads/main_RO']).toString().trim();
  const remoteCommit = remote.split(/\s+/)[0];
  if (!COMMIT.test(remoteCommit)) throw new Error('No published Romanian edition.');
  const file = documentationEditionsPolicy.reviewPath;
  const bytes = filesAt(project, englishCommit, [file]).get(file);
  const recorded = JSON.parse(utf8.decode(bytes));
  const romanianCommit = recorded.romanianCommit;
  if (recorded.schemaVersion !== 2 || !COMMIT.test(romanianCommit ?? '')) throw new Error('A pinned Romanian documentation commit is required.');
  try {
    git(project, ['cat-file', '-e', remoteCommit + '^{commit}']);
  } catch {
    git(project, ['fetch', '--no-tags', '--no-write-fetch-head', '--refmap=', 'origin', 'refs/heads/main_RO']);
  }
  git(project, ['merge-base', '--is-ancestor', romanianCommit, remoteCommit]);
  const checked = inspectDocumentationEditions(project, englishCommit, romanianCommit);
  if (!isDeepStrictEqual(recorded, checked.review)) {
    checked.problems.push('Documentation changed since the recorded bilingual review.');
  }
  return {...checked, reviewSha256: sha256(bytes)};
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const [verb, project, english, romanian] = process.argv.slice(2);
  if (!project || !english || !(['inspect', 'verify'].includes(verb)) || (verb === 'inspect' && !romanian)) {
    throw new Error('Use inspect <repo> <English ref> <Romanian ref> or verify <repo> <English commit>.');
  }
  const result = verb === 'inspect' ? inspectDocumentationEditions(project, english, romanian)
    : verifyReviewedDocumentation(project, resolveCommit(project, english));
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');
  if (result.problems.length) process.exitCode = 2;
}

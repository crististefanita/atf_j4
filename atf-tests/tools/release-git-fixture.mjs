import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {digest, inputFingerprint, strictJson} from './evaluate-release-rag.mjs';

export async function checkout(t, {mixed = false, sourceOnly = false, contentChanged = false, version = '0.21.4'} = {}) {
  const fixture = await fs.mkdtemp(path.join(os.tmpdir(), 'release-git-'));
  t.after(() => fs.rm(fixture, {recursive: true, force: true}));
  const repository = path.join(fixture, 'repo'), qualifiedRoot = path.join(fixture, 'qualified');
  const candidate = path.join(repository, 'release', version);
  const write = async (file, content) => {
    await fs.mkdir(path.dirname(file), {recursive: true});
    await fs.writeFile(file, typeof content === 'string' ? content : JSON.stringify(content));
  };
  await fs.mkdir(repository);
  const git = (...args) => execFileSync('git', ['-c', 'core.autocrlf=false',
    '-c', 'user.name=Snapshot fixture', '-c', 'user.email=fixture@example.invalid', ...args], {
    cwd: repository, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe']
  }).trim();
  git('init', '-q', '-b', 'main');
  await write(path.join(repository, 'README.md'), 'test\nSecond line\n');
  await write(path.join(repository, 'lib.txt'), 'lib\n');
  git('add', '.');
  git('commit', '-qm', 'source');
  const sourceCommit = git('rev-parse', 'HEAD');
  const text = mixed ? 'test\r\nSecond line\n' : contentChanged ? 'different content\n' : 'test\nSecond line\n';
  await write(path.join(qualifiedRoot, 'README.md'), text);
  const rows = [{path: 'README.md', sha256: digest(text)}, {path: 'lib.txt', sha256: digest('lib\n')}];
  if (sourceOnly) rows.push({path: 'local.yml', sha256: digest('unsafe to delete')});
  const sourceFingerprint = inputFingerprint(new Map(rows.map(row => [row.path, row.sha256])));
  const apkName = 'WhistLab-' + version + '.apk', apk = 'APK fixture', checksum = digest(apk) + '  ' + apkName + '\n';
  const input = {sourceCommit, fingerprint: sourceFingerprint, files: rows};
  const releaseFiles = [[apkName, apk], [apkName + '.sha256', checksum], ['reports/apk-inputs.json', input],
    ['reports/report.html', '<html></html>'], ['README.md', '# Edition'], ['reports/README.md', '# Audit']];
  await write(path.join(repository, 'release', 'index.json'), {
    schemaVersion: 2, currentVersion: version, versions: [version, '0.21.3']
  });
  for (const [file, bytes] of releaseFiles) await write(path.join(candidate, file), bytes);
  const payload = await Promise.all(releaseFiles.map(async ([name]) => {
    const bytes = await fs.readFile(path.join(candidate, name));
    return {path: name, bytes: bytes.length, sha256: digest(bytes)};
  }));
  await write(path.join(candidate, 'release-validation.json'), {release: {versionName: version, apkName, apkSha256: digest(apk), apkBytes: apk.length},
    source: {sourceCommit, workingTreeFingerprint: sourceFingerprint, inputManifest: 'reports/apk-inputs.json'},
    publication: {state: 'PUBLISHED'}, integrity: {files: payload}});
  git('tag', 'v' + version, sourceCommit);
  git('add', '.');
  git('commit', '-qm', 'published payload');
  git('update-ref', 'refs/remotes/origin/main', 'HEAD');
  const verdictFile = path.join(fixture, 'post.json');
  await write(verdictFile, {kind: 'ATF_RELEASE_RAG_VERDICT', verdict: 'READY_FOR_HUMAN_REVIEW',
    releaseVersion: version, sourceCommit, workingTreeFingerprint: sourceFingerprint,
    candidateManifestSha256: (await strictJson(path.join(candidate, 'release-validation.json'))).sha256,
    checks: ['SOURCE_SNAPSHOT', 'PAYLOAD_INTEGRITY', 'APK_IDENTITY', 'PLATFORM_GATES', 'GITHUB_RETENTION'].map(code => ({code, status: 'PASS'}))});
  return {repository, candidate, qualifiedRoot, git, verdict: await strictJson(verdictFile)};
}

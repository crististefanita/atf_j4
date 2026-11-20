import {digest} from './source-input-identity.mjs';
import {assertAncestor, gitBlob} from './git-snapshot-blobs.mjs';

/** Read-only admission, before journal creation or any GitHub mutation. */
export function verifyPublicationAudit(repository, {identity, manifest, notes}) {
  const version = identity.tag.slice(1), prefix = 'release/' + version + '/';
  const links = [...notes.matchAll(/\]\((https:\/\/[^\s()]+)\)/g)].map(match => match[1]);
  const auditLinks = links.filter(value => new URL(value).pathname.endsWith('/' + prefix + 'reports/README.md'));
  if (auditLinks.length !== 1) throw Error('GITHUB_AUDIT_LINK');
  const url = new URL(auditLinks[0]);
  const expectedPrefix = '/' + identity.repository + '/blob/';
  const commit = url.pathname.slice(expectedPrefix.length).split('/')[0];
  if (url.origin !== 'https://github.com' || url.username || url.password || url.search || url.hash
      || !/^[a-f0-9]{40}$/.test(commit) || url.pathname !== expectedPrefix + commit + '/' + prefix + 'reports/README.md')
    throw Error('GITHUB_AUDIT_LINK');
  const read = name => {
    try { return gitBlob(repository, commit, prefix + name); }
    catch (cause) { throw new Error('GITHUB_AUDIT_BLOB: ' + name, {cause}); }
  };
  const pinned = JSON.parse(read('release-validation.json'));
  if (!['QUALIFIED', 'PUBLISHED'].includes(pinned.publication?.state) || pinned.qualification?.state !== 'PASS'
      || pinned.source?.sourceCommit !== identity.commit || pinned.release?.versionName !== version
      || pinned.release.apkSha256 !== manifest.release.apkSha256 || pinned.release.apkBytes !== manifest.release.apkBytes)
    throw Error('GITHUB_AUDIT_SOURCE');
  assertAncestor(repository, identity.commit, commit);
  const report = manifest.qualification?.auditedGroups?.['atf-tests']?.report;
  if (typeof report !== 'string' || !report.startsWith('reports/') || report.includes('..') || report.includes('\\')
      || pinned.qualification?.auditedGroups?.['atf-tests']?.report !== report) throw Error('GITHUB_AUDIT_REPORT');
  const hash = value => value.integrity?.files?.filter(row => row.path === report);
  const current = hash(manifest), original = hash(pinned);
  if (current?.length !== 1 || original?.length !== 1 || current[0].sha256 !== original[0].sha256
      || digest(read(report)) !== current[0].sha256)
    throw Error('GITHUB_AUDIT_REPORT');
  if (!read('reports/README.md').toString('utf8').includes('](' + report.slice(8) + ')'))
    throw Error('GITHUB_AUDIT_INDEX');
  return {status: 'PASS', commit, sourceCommit: identity.commit, report, reportSha256: current[0].sha256, url: url.href};
}

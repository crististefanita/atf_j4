import {isDeepStrictEqual} from 'node:util';
import {digest, loadGithubRetentionCatalog} from './evaluate-release-rag.mjs';
import {commitId, gitBlob} from './git-snapshot-blobs.mjs';

// The final catalog and manifests must already exist byte-for-byte in published Git.
export async function sealGithubRetentionCatalog(project) {
  const expected = await loadGithubRetentionCatalog(project);
  let publishedCommit;
  try { publishedCommit = commitId(project, 'refs/remotes/origin/main'); }
  catch { throw Error('GITHUB_SEAL_PUBLISHED_REF_REQUIRED: fetch origin/main before collection'); }
  for (const file of expected.evidence) {
    let bytes;
    try { bytes = gitBlob(project, publishedCommit, file.path); }
    catch { throw Error('GITHUB_PAYLOAD_NOT_SEALED: ' + file.path); }
    if (digest(bytes) !== file.sha256) throw Error('GITHUB_PAYLOAD_NOT_SEALED: ' + file.path);
  }
  return {...expected, publishedCommit};
}

export async function assertGithubRetentionSeal(project, sealed) {
  let current;
  try { current = await sealGithubRetentionCatalog(project); }
  catch (error) { throw Error('GITHUB_SEAL_CHANGED: ' + error.message); }
  if (!isDeepStrictEqual(current, sealed)) throw Error('GITHUB_SEAL_CHANGED: catalog, manifests or published commit changed');
}

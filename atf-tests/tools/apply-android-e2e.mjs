import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {randomUUID} from 'node:crypto';
import {androidExecutionProfile, e2ePaths, scenarioFor} from './android-e2e-contract.mjs';
import {readE2eEvidence} from './android-e2e-evidence.mjs';
import {digest, regularFile, strictJson} from './evaluate-release-rag.mjs';

export async function applyAndroidEvidence(candidateDir, runDir) {
  const [candidate, run] = await Promise.all([candidateDir, runDir].map(dir => fs.realpath(dir)));
  if (run === candidate || run.startsWith(candidate + path.sep) || candidate.startsWith(run + path.sep))
    throw Error('ANDROID_E2E_EXTERNAL_RUN_ROOT_REQUIRED');
  const bundle = path.join(run, 'evidence');
  const result = (await strictJson(await regularFile(run, 'evidence/e2e-result.json'))).value;
  const request = (await strictJson(await regularFile(run, 'e2e-request.json'))).value;
  const profile = androidExecutionProfile(request.executionProfile?.api, request.executionProfile?.displayProfile,
    request.executionProfile?.responsiveProfiles);
  const name = result.flow;
  const scenario = scenarioFor(name, profile);
  if (!request.groups?.includes(name)) throw Error('ANDROID_E2E_GROUP_NOT_IN_PLAN');

  const manifestPath = await regularFile(candidate, 'release-validation.json');
  const original = await strictJson(manifestPath);
  const manifest = original.value;
  if (manifest.publication?.state !== 'PREPARED' || path.basename(candidate) !== request.releaseVersion
      || manifest.release?.versionName !== request.releaseVersion) throw Error('ANDROID_E2E_CANDIDATE_MUST_BE_PREPARED');
  manifest.qualification.e2eGroups ??= {};
  manifest.qualification.e2eGroups[name] = result.group;
  const declared = new Map(result.integrity.map(item => [item.path, item]));
  const accepted = await readE2eEvidence({
    manifest, name, declared, requireScenario: true, scenario,
    readCurrentSource: async relative => strictJson(await regularFile(candidate, relative)),
    readDocument: async relative => strictJson(await regularFile(bundle, relative)),
    readReport: async relative => {
      const bytes = await fs.readFile(await regularFile(bundle, relative));
      return {bytes, text: bytes.toString('utf8'), sha256: digest(bytes)};
    }
  });
  if (declared.size !== accepted.documents.size || declared.size !== result.integrity.length) throw Error('ANDROID_E2E_BUNDLE_INVENTORY');

  // Exclusive writer for this draft. Existing evidence owners are never modified.
  const lockPath = path.join(path.dirname(candidate), path.basename(candidate) + '.e2e-apply.lock');
  const lock = await fs.open(lockPath, 'wx');
  const temporary = manifestPath + '.' + randomUUID() + '.tmp';
  try {
    if (!original.bytes.equals(await fs.readFile(manifestPath))) throw Error('ANDROID_E2E_CANDIDATE_CHANGED');
    const entries = new Map((manifest.integrity?.files ?? []).map(row => [row.path, row]));
    for (const [relative, document] of accepted.documents) {
      const destination = path.join(candidate, relative);
      await fs.mkdir(path.dirname(destination), {recursive: true});
      if (await fs.realpath(path.dirname(destination)) !== path.dirname(destination)) throw Error('ANDROID_E2E_INDIRECT_DESTINATION');
      const existing = await fs.lstat(destination).catch(error => error.code === 'ENOENT' ? null : Promise.reject(error));
      if (existing && !existing.isFile()) throw Error('ANDROID_E2E_INDIRECT_DESTINATION');
      if (relative !== e2ePaths(name).report && relative !== e2ePaths(name).executionReceipt
          && existing && digest(await fs.readFile(destination)) !== document.sha256) throw Error('ANDROID_E2E_FOREIGN_ISOLATION');
      const staged = destination + '.' + randomUUID() + '.tmp';
      try {
        await fs.writeFile(staged, document.bytes, {flag: 'wx'});
        await fs.rename(staged, destination);
      } finally { await fs.unlink(staged).catch(error => { if (error.code !== 'ENOENT') throw error; }); }
      entries.set(relative, declared.get(relative));
    }
    manifest.integrity = {...manifest.integrity, schemaVersion: 1, kind: 'OBSERVED_PAYLOAD_HASHES',
      observedUtc: new Date().toISOString(), files: [...entries.values()].sort((a, b) => a.path < b.path ? -1 : a.path > b.path ? 1 : 0)};
    await fs.writeFile(temporary, JSON.stringify(manifest, null, 2) + '\n', {flag: 'wx'});
    await fs.rename(temporary, manifestPath);
    return {group: name, files: accepted.documents.size};
  } finally {
    await lock.close();
    await fs.unlink(lockPath);
    await fs.unlink(temporary).catch(error => { if (error.code !== 'ENOENT') throw error; });
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    if (process.argv.length !== 4) throw Error('Use <prepared-candidate> <accepted-run>');
    console.log(JSON.stringify(await applyAndroidEvidence(process.argv[2], process.argv[3])));
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}

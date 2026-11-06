import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import crypto from 'node:crypto';

// Preparation only: observing hashes neither runs tests nor promotes their verdict.
export async function recordIntegrity(project, version) {
  const root = await fs.realpath(project);
  if (!/^(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z.-]+)?$/.test(version)) throw Error('Invalid release version');
  const folder = path.join(root, 'release', version);
  if (await fs.realpath(folder) !== folder) throw Error('Release directory must have its canonical location');
  const manifest = path.join(folder, 'release-validation.json'), original = await fs.readFile(manifest);
  const value = JSON.parse(original);
  if (value.release?.versionName !== version) throw Error('Release identity differs');
  const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
  async function inventory() {
    const files = [];
    async function visit(directory) {
      for (const entry of await fs.readdir(directory, {withFileTypes: true})) {
        const file = path.join(directory, entry.name);
        if (entry.isSymbolicLink()) throw Error('Symbolic evidence is not canonical: ' + file);
        if (entry.isDirectory()) await visit(file);
        else if (file !== manifest) {
          if (!entry.isFile()) throw Error('Expected a regular file: ' + file);
          const bytes = await fs.readFile(file);
          files.push({path: path.relative(folder, file).split(path.sep).join('/'), bytes: bytes.length, sha256: hash(bytes)});
        }
      }
    }
    await visit(folder);return files.sort((left, right) => left.path < right.path ? -1 : left.path > right.path ? 1 : 0);
  }
  const files = await inventory();
  if (!files.length) throw Error('Empty release payload');
  const again = await inventory();
  if (JSON.stringify(again) !== JSON.stringify(files) || !original.equals(await fs.readFile(manifest))) throw Error('Concurrent release edit; freeze inputs first');
  value.integrity = {schemaVersion: 1, kind: 'OBSERVED_PAYLOAD_HASHES', observedUtc: new Date().toISOString(), files};
  const temporary = manifest + '.' + crypto.randomUUID() + '.tmp';
  try {
    await fs.writeFile(temporary, JSON.stringify(value, null, 2) + '\n', {flag: 'wx'});
    if (!original.equals(await fs.readFile(manifest))) throw Error('Concurrent manifest edit');
    await fs.rename(temporary, manifest);
  } finally { await fs.unlink(temporary).catch(error => { if (error.code !== 'ENOENT') throw error; }); }
  return {version, files: files.length, kind: value.integrity.kind};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const options = new Map();
    for (let index = 2; index < process.argv.length; index += 2) {
      const key = process.argv[index], value = process.argv[index + 1];
      if (!['--project', '--version'].includes(key) || !value || options.has(key)) throw Error('Use --project <root> --version <version>');
      options.set(key, value);
    }
    const root = path.resolve(options.get('--project') ?? fileURLToPath(new URL('../../', import.meta.url)));
    const version = options.get('--version') ?? JSON.parse(await fs.readFile(path.join(root, 'release/index.json'))).currentVersion;
    console.log(JSON.stringify(await recordIntegrity(root, version)));
  } catch (error) { console.error(error.message);process.exitCode = 1; }
}

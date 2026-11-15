import {lstatSync, realpathSync, statSync} from 'node:fs';
import path from 'node:path';

function physicalDirectory(directory, name) {
  try {
    if (!path.isAbsolute(directory)) throw new Error('expected an absolute path');
    const resolved = realpathSync.native(directory);
    if (!statSync(resolved).isDirectory()) throw new Error('expected a directory');
    return resolved;
  } catch (error) {
    throw new Error(`QUALIFICATION-TEMP-INVALID: ${name}=${directory} (${error.message})`);
  }
}

function hasGitMarker(directory) {
  try {
    lstatSync(path.join(directory, '.git'));
    return true;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    return false;
  }
}

function requireExternal(directory, source, name) {
  for (let current = path.normalize(directory); ; current = path.dirname(current)) {
    if (path.relative(source, current) === '' || hasGitMarker(current)) {
      throw new Error(`QUALIFICATION-TEMP-IN-CHECKOUT: ${name}=${directory} (checkout ${current})`);
    }
    if (current === path.dirname(current)) return;
  }
}

try {
  const [source, systemTemp] = process.argv.slice(2);
  const physicalSource = physicalDirectory(source, 'SourceRoot');
  const locations = [['TMP', process.env.TMP], ['TEMP', process.env.TEMP], ['GetTempPath', systemTemp]];
  let workRoot;
  for (const [name, directory] of locations) {
    if (name !== 'GetTempPath' && !directory?.trim()) continue;
    const physical = physicalDirectory(directory, name);
    requireExternal(directory, source, name);
    requireExternal(physical, physicalSource, name);
    if (name === 'GetTempPath') workRoot = physical;
  }
  process.stdout.write(workRoot);
} catch (error) {
  process.stderr.write(error.message + '\n');
  process.exitCode = 1;
}

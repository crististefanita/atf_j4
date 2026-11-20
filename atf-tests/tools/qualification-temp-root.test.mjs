import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const checker = path.join(project, 'atf-tests/tools/qualification-temp-root.mjs');

async function directories(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-portable-paths-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const checkout = path.join(root, 'checkout');
  for (const name of ['checkout', 'explicit', 'tmp', 'temp', 'system']) await fs.mkdir(path.join(root, name), {recursive: true});
  await fs.writeFile(path.join(checkout, '.git'), 'gitdir: protected');
  return {root, checkout, explicit: path.join(root, 'explicit'), tmp: path.join(root, 'tmp'),
    temp: path.join(root, 'temp'), system: path.join(root, 'system')};
}

function invoke(paths, configured = paths.explicit) {
  return spawnSync(process.execPath, [checker, paths.checkout, paths.system], {encoding: 'utf8',
    env: {...process.env, TMP: paths.tmp, TEMP: paths.temp, WHIST_QUALIFICATION_TEMP_ROOT: configured}});
}

test('an explicit qualification temp root selects that machine path while validating OS temp variables too', async t => {
  const paths = await directories(t);
  const result = invoke(paths);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(path.resolve(result.stdout.trim()), paths.explicit);
});

test('qualification rejects an explicit temp path inside a checkout before returning it', async t => {
  const paths = await directories(t);
  const generated = path.join(paths.checkout, 'generated');
  await fs.mkdir(generated);
  const result = invoke(paths, generated);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /QUALIFICATION-TEMP-IN-CHECKOUT: WHIST_QUALIFICATION_TEMP_ROOT/);
});

test('qualification rejects relative or missing machine paths instead of falling back silently', async t => {
  const paths = await directories(t);
  const relative = invoke(paths, '.\\scratch');
  assert.equal(relative.status, 1);
  assert.match(relative.stderr, /QUALIFICATION-TEMP-INVALID: WHIST_QUALIFICATION_TEMP_ROOT/);
  const missing = invoke(paths, path.join(paths.root, 'missing'));
  assert.equal(missing.status, 1);
  assert.match(missing.stderr, /QUALIFICATION-TEMP-INVALID: WHIST_QUALIFICATION_TEMP_ROOT/);
});

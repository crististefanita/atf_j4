import {TextDecoder} from 'node:util';

const utf8 = new TextDecoder('utf8', {fatal: true, ignoreBOM: true});
function decode(bytes) {
  if (bytes.includes(0)) return null;
  try { return utf8.decode(bytes); }
  catch { return null; }
}

export function eolRecipe(blob, qualified) {
  const raw = decode(blob), target = decode(qualified);
  if (raw === null || target === null || raw.replaceAll('\r\n', '\n') !== target.replaceAll('\r\n', '\n')) return null;
  const endings = [...target.matchAll(/\r?\n/g)];
  if (!endings.length) return null;
  if (endings.every(match => match[0].length === 1)) return {kind: 'LF'};
  if (endings.every(match => match[0].length === 2)) return {kind: 'CRLF'};
  const bits = Buffer.alloc(Math.ceil(endings.length / 8));
  endings.forEach((match, index) => {
    if (match[0].length === 2) bits[index >> 3] |= 1 << (index & 7);
  });
  return {kind: 'MIXED', crlfBits: bits.toString('base64')};
}

export function applyEol(blob, recipe) {
  const raw = decode(blob);
  if (raw === null) throw Error('GIT_SNAPSHOT_NOT_TEXT');
  const lf = raw.replaceAll('\r\n', '\n');
  if (recipe.kind === 'LF') return Buffer.from(lf);
  if (recipe.kind === 'CRLF') return Buffer.from(lf.replaceAll('\n', '\r\n'));
  if (recipe.kind !== 'MIXED' || typeof recipe.crlfBits !== 'string') throw Error('GIT_SNAPSHOT_EOL_RECIPE');
  const bits = Buffer.from(recipe.crlfBits, 'base64');
  const count = lf.split('\n').length - 1;
  if (bits.toString('base64') !== recipe.crlfBits || !count || bits.length !== Math.ceil(count / 8)
      || (count % 8 && bits.at(-1) >> (count % 8))) throw Error('GIT_SNAPSHOT_EOL_BITMAP');
  let index = 0;
  return Buffer.from(lf.replace(/\n/g, () => {
    const offset = index++;
    return bits[offset >> 3] & (1 << (offset & 7)) ? '\r\n' : '\n';
  }));
}

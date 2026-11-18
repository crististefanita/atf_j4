import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

/** The same expected face measurements are consumed by JVM and browser components. */
export async function verifyCardTypography(setup, fixture = new URL('./card-typography.tsv', import.meta.url)) {
  const rows = (await readFile(fixture, 'utf8')).trim().split(/\r?\n/).filter(line => !line.startsWith('#'));
  assert.equal(setup.cardFace.rankScale, 1.5, 'Shared rank scale, independent of menu typography');
  return rows.map(line => {
    const [height, width, rank, suit, invertedRank, invertedSuit, centerSuit] = line.split('\t').map(Number);
    assert.ok(Math.abs(rank - suit * setup.cardFace.rankScale) < .00001);
    assert.ok(Math.abs(invertedRank - invertedSuit * setup.cardFace.rankScale) < .00001);
    return { height, width, rank, suit, invertedRank, invertedSuit, centerSuit };
  });
}

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

export async function verifyTrumpTextFit(fitTextWidth, file = new URL("./whist-trump-text-fit.tsv", import.meta.url)) {
  const rows = (await readFile(file, "utf8")).trim().split(/\r?\n/u).slice(1);
  assert.ok(rows.length > 0, "Expected the common Android/Web trump cases");
  return rows.map(row => {
    const [copy, ...values] = row.split("\t");
    const [nominal, measured, available, expected] = values.map(Number);
    const fitted = fitTextWidth(nominal, measured, available);
    assert.ok(Math.abs(fitted - expected) < .001, copy);
    assert.ok(measured * fitted / nominal <= available + .001, copy);
    return { copy, nominal, measured, available, expected, fitted };
  });
}

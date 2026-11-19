import assert from "node:assert/strict";

/** One adapter contract for measured score actions; captions and callbacks remain game-owned. */
export function verifyScoreActionTypography(tokens, rootPixels, buttons) {
  assert.ok(Number.isFinite(rootPixels) && rootPixels > 0, 'A positive accessible root size is required');
  assert.ok(Array.isArray(buttons) && buttons.length > 0, 'Score actions must be measured');
  const rem = name => {
    assert.match(tokens[name] ?? '', /^\d+(?:\.\d+)?rem$/, 'The delivered native/Web role must exist: ' + name);
    return Number.parseFloat(tokens[name]);
  };
  const primary = rem('--score-text-action-primary'), secondary = rem('--score-text-action-secondary');
  assert.equal(primary, rem('--score-text-primary') * 1.5, 'The primary score action gains exactly 50%');
  assert.equal(secondary, rem('--score-text-quiet') * 1.5, 'The secondary score action gains exactly 50%');
  for (const button of buttons) {
    assert.equal(button.size, rootPixels * (button.quiet ? secondary : primary), 'One text scale: ' + button.label);
    assert.ok(button.height >= 48, 'Touch target: ' + button.label);
    assert.equal(button.overflow, false, 'Complete label: ' + button.label);
  }
  return {actions: buttons.length, primaryRem: primary, secondaryRem: secondary};
}

/** Executes the delivered renderers with game-owned captions; it never calculates a score. */
export function verifyScorePresentation(scoreGrid, historyGrid, element) {
  let cases = 0;
  for (const captions of [["Δ:", "Σ:"], ["B", "P"]]) for (const players of [3, 4, 5, 6]) {
    const names = Array.from({length: players}, (_, i) => "Player " + i);
    const rows = names.map((name, i) => [name, String(i * 25 - 50), String(100 - i * 50)]);
    const score = scoreGrid({headings: ["Player", ...captions], rows}, true);
    const header = score.children.find(child => child.tagName.toLowerCase() === "thead");
    const body = score.children.find(child => child.tagName.toLowerCase() === "tbody");
    assert.deepEqual(header.children[0].children.map(cell => cell.textContent), ["Player", ...captions]);
    assert.deepEqual(body.children.map(row => row.children.map(cell => cell.textContent)), rows);
    const history = historyGrid({players: names, cardsLabel: "Hand", bidLabel: captions[0], pointsLabel: captions[1],
      rows: [{cards: 8, status: "Confirmed", cells: rows.map(row => ({bid: row[1], total: row[2], description: row.join(" · "), missed: false, series: 0}))}]}, element);
    assert.deepEqual(history.children[0].children[1].children.slice(1).map(cell => cell.textContent),
      names.flatMap(() => captions));
    assert.equal(history.children[1].children[0].children.length, 1 + 2 * players);
    assert.deepEqual(history.children[1].children[0].children.slice(1).map(cell => cell.children[0].textContent), rows.flatMap(row => row.slice(1)));
    cases++;
  }
  return {cases, columnFamilies: 2, scoringCalculations: 0};
}

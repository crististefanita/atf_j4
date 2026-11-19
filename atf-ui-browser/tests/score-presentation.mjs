import assert from "node:assert/strict";

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

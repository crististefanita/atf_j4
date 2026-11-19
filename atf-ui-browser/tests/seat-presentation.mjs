import assert from "node:assert/strict";

export function verifySeatFitting(fitSeatLine) {
  let cases = 0;
  for (const width of [48, 68, 96, 116, 180]) for (const font of [1, 1.5, 2]) {
    for (const text of ["Alex", "Δ: 0", "Δ: -160", "Σ: -10000", "Σ: 99999"]) {
      const ink = {scrollWidth: Math.ceil(text.length * 7 * font), offsetWidth: 0, style: {}, append() {}};
      const line = {clientWidth: width, height: 16 * font, childNodes: [text], classList: {add() {}},
        ownerDocument: {createElement: () => ink}, querySelector() { return this.ink; }, append(value) { this.ink = value; }};
      fitSeatLine(line);
      const scale = Number(ink.style.transform.slice(7, -1));
      assert.ok(scale > 0 && scale <= 1);
      assert.ok(ink.scrollWidth * scale <= width);
      assert.equal(line.height, 16 * font);
      assert.deepEqual(line.childNodes, [text]);
      const first = ink.style.transform; fitSeatLine(line); assert.equal(ink.style.transform, first);
      cases++;
    }
  }
  return {cases, heightChanges: 0, textChanges: 0, idempotent: true};
}

export function verifyActorLabels(markSeat) {
  for (const label of ["ALEGE", "JOACĂ", "PASEAZĂ", "CHOOSING", "PLAYING"]) {
    const badge = {};
    const node = {dataset: {}, classList: {toggle() {}, contains: value => value === "south"},
      setAttribute() {}, removeAttribute() {}, querySelector: () => badge};
    markSeat(node, true, true, label); assert.equal(badge.hidden, false); assert.equal(badge.textContent, label);
    markSeat(node, true, false); assert.equal(badge.textContent, label);
    markSeat(node, false, false); assert.equal(badge.hidden, true);
  }
  const badge = {}; const node = {dataset: {}, classList: {toggle() {}, contains: () => true}, setAttribute() {}, removeAttribute() {}, querySelector: () => badge};
  markSeat(node, true, true); assert.equal(badge.hidden, true);
  return {localizedLabels: 5, nativeHumanLabelVisible: true, legacyBiddingPolicyRetained: true};
}

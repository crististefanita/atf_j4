import assert from "node:assert/strict";
import { runInNewContext } from "node:vm";

export function verifyCenterContinuity(shell) {
  const fragment = shell.slice(shell.indexOf('  const controls = element('), shell.indexOf('  const hand = element('));
  assert.ok(fragment.includes('controls.append(actions)'), 'Exercise the actual delivered render constructor');
  class Node {
    children = [];
    append(...nodes) { this.children.push(...nodes); }
    get childElementCount() { return this.children.length; }
  }
  {
      const build = (header, actions, bids = []) => {
        const center = new Node(); center.append(new Node());
        runInNewContext(fragment, { center, match: { header, prompt: 'Play', actions, history: { rows: [] } }, bids,
          element: () => new Node(), control: () => new Node(), button: () => new Node(),
          bidding: (_bids, target) => target.append(new Node()) });
        return center.children.length > 1;
      };
      // Model only the flex gap port: hiding a zero-height sibling changes the card origin by gap/2.
      for (const actions of [[], [{ key: 'auto' }], [{ key: 'auto-step' }], [{ key: 'auto' }, { key: 'auto-step' }]]) {
        const hasEmptyControls = build(true, actions);
        const top = playback => (400 - 260 - (!playback && hasEmptyControls ? 6 : 0)) / 2;
        assert.equal(top(false), top(true), 'Empty controls cannot shift the existing trick at a playback handoff');
      }
      assert.equal(build(true, [{ key: 'next' }]), true, 'Real actions remain attached');
      assert.equal(build(true, [{ key: 'bid:1' }], [{ key: 'bid:1' }]), true, 'Bidding stays visible');
      assert.equal(build(false, []), true, 'Legacy prompt-only controls remain attached');
  }
  return { emptyStates: 4, emptyControlDrift: 0, realActionsPreserved: true, legacyPromptPreserved: true };
}

export function verifySeatRetention(retainSeatFrames) {
  assert.equal(typeof retainSeatFrames, "function", "The delivered renderer must retain frame objects across AI/human handoffs");
  class Frame {
    constructor(id, rectangle, active, text) {
      this.values = new Map([["data-seat", id], ["style", rectangle], ["class", active ? "seat active" : "seat"]]);
      if (active) this.values.set("aria-current", "true");
      this.childNodes = [text];
      this.owner = {};
    }
    get dataset() { return {seat: this.values.get("data-seat")}; }
    get attributes() { return [...this.values].map(([name, value]) => ({name, value})); }
    setAttribute(name, value) { this.values.set(name, value); }
    removeAttribute(name) { this.values.delete(name); }
    replaceChildren(...children) { this.childNodes = children; }
    replaceWith(frame) {
      if (frame.parent) frame.parent.frames.splice(frame.parent.frames.indexOf(frame), 1);
      const parent = this.parent; parent.frames[parent.frames.indexOf(this)] = frame; frame.parent = parent;
    }
  }
  const host = frames => {
    const root = {frames, querySelectorAll(selector) {
      assert.equal(selector, ":scope > .board > .seat"); return this.frames;
    }};
    frames.forEach(frame => frame.parent = root); return root;
  };
  let tables = 0, transitions = 0;
  for (const players of [3, 4, 5, 6]) for (const width of [240, 280, 320, 360, 393, 414, 600, 800]) {
    for (const font of [1, 1.5, 2]) {
      const rectangle = seat => `${width}/${font}/${seat}`;
      let previous = host(Array.from({length: players}, (_, seat) => new Frame(String(seat), rectangle(seat), seat === 0, "0")));
      const original = [...previous.frames];
      const actors = [1, players - 1, 0, -1, 0];
      for (const [step, actor] of actors.entries()) {
        previous.frames[0].setAttribute("data-obsolete", "must be removed");
        const next = host(Array.from({length: players}, (_, seat) => new Frame(String(seat), rectangle(seat), seat === actor, `Δ: ${step * -160} Σ: ${step * 1000}`)));
        const measured = next.frames.map(frame => [...frame.values]);
        assert.equal(retainSeatFrames(previous, next), players);
        for (const [seat, frame] of next.frames.entries()) {
          assert.strictEqual(frame, original[seat], `Keep seat ${seat} through ${players}/${width}/${font}/${step}`);
          assert.deepEqual([...frame.values], measured[seat], "Use the prepared geometry and current accessible actor state");
          assert.deepEqual(frame.childNodes, [`Δ: ${step * -160} Σ: ${step * 1000}`]);
        }
        previous = next; transitions++;
      }
      const resized = host(original.map((frame, seat) => new Frame(String(seat), `resized/${seat}`, false, "next viewport")));
      assert.equal(retainSeatFrames(previous, resized), players);
      resized.frames.forEach((frame, seat) => { assert.strictEqual(frame, original[seat]); assert.equal(frame.values.get("style"), `resized/${seat}`); });
      tables++;
    }
  }
  for (const invalid of ["count", "missing", "duplicate", "foreign"]) {
    const previous = host([new Frame("0", "old", true, "old"), new Frame("1", "old", false, "old")]);
    const next = host([new Frame("0", "new", false, "new"), new Frame("1", "new", true, "new")]);
    if (invalid === "count") next.frames.pop();
    if (invalid === "missing") next.frames[1].removeAttribute("data-seat");
    if (invalid === "duplicate") next.frames[1].setAttribute("data-seat", "0");
    if (invalid === "foreign") next.frames[1].setAttribute("data-seat", "2");
    const before = [...previous.frames], after = [...next.frames];
    assert.equal(retainSeatFrames(previous, next), 0, "An incompatible table keeps fresh frames");
    assert.deepEqual(previous.frames, before); assert.deepEqual(next.frames, after);
    assert.equal(previous.frames[0].childNodes[0], "old", "Reject before changing the visible table");
  }
  const initial = host([new Frame("0", "first", true, "first")]);
  assert.equal(retainSeatFrames(null, initial), 0);
  assert.equal(retainSeatFrames(initial, initial), 0);
  return {tables, transitions, resizeAdmitted: true, incompatibleTablesUntouched: 4};
}

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

/** Fit measured ink horizontally; the outer line keeps its typography and height across turns. */
export function fitSeatLine(line) {
  line.classList.add("seat-line");
  let ink = line.querySelector(":scope > .seat-line-ink");
  if (!ink) {
    ink = line.ownerDocument.createElement("span");
    ink.className = "seat-line-ink";
    ink.append(...line.childNodes);
    line.append(ink);
  }
  const available = line.clientWidth;
  if (available <= 0) return;
  const natural = Math.max(ink.scrollWidth, ink.offsetWidth);
  const scale = natural <= 0 ? 1 : Math.min(1, Math.max(0, available - 1) / natural);
  ink.style.transform = `scaleX(${scale})`;
}

export function fitSeatLines(seats) {
  for (const seat of seats) {
    for (const line of seat.querySelectorAll(":scope > strong, :scope > .seat-bid, :scope > .seat-won")) fitSeatLine(line);
  }
}

/** Measure the external action line in CSS layout units, including when its real badge is hidden. */
export function measureSeatActionClearance(seat) {
  const badge = seat.querySelector(".seat-turn");
  if (!badge) return 0;
  const probe = badge.cloneNode(true);
  probe.hidden = false;
  probe.textContent = "M";
  probe.style.visibility = "hidden";
  seat.append(probe);
  try { return Math.max(0, -probe.offsetTop); }
  finally { probe.remove(); }
}

/** Commit measured content without replacing the existing per-seat frame objects. */
export function retainSeatFrames(previous, next) {
  if (!previous || previous === next) return 0;
  const before = [...(previous?.querySelectorAll(":scope > .board > .seat") ?? [])];
  const after = [...next.querySelectorAll(":scope > .board > .seat")];
  if (!before.length || before.length !== after.length) return 0;
  const keys = nodes => nodes.map(node => node.dataset.seat);
  const oldKeys = keys(before), newKeys = keys(after);
  if (oldKeys.some(key => !key) || new Set(oldKeys).size !== before.length
      || newKeys.some(key => !key) || new Set(newKeys).size !== after.length
      || newKeys.some(key => !oldKeys.includes(key))) return 0;
  const frames = new Map(before.map(node => [node.dataset.seat, node]));
  for (const measured of after) {
    const frame = frames.get(measured.dataset.seat);
    const attributes = new Map([...measured.attributes].map(attribute => [attribute.name, attribute.value]));
    for (const attribute of [...frame.attributes]) if (!attributes.has(attribute.name)) frame.removeAttribute(attribute.name);
    for (const [name, value] of attributes) frame.setAttribute(name, value);
    frame.replaceChildren(...measured.childNodes);
    measured.replaceWith(frame);
  }
  return after.length;
}

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

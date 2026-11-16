// Mirrors TrickCollectionMotion.frame. Coordinates are in the stage's unscaled logical pixels.
export function collectionFrame(dx, dy, fraction) {
  const value = Math.max(0, Math.min(1, fraction));
  const ease = 1 - (1 - value) ** 3;
  return { x: dx * ease, y: dy * ease, scale: 1 - .82 * ease,
    opacity: value < .8 ? 1 : Math.max(0, (1 - value) / .2) };
}

export function collect(stage, cue, drawCard) {
  const seat = stage.querySelectorAll(".seat")[cue.winningSeat];
  const table = stage.querySelector(".cards-on-table");
  if (!seat || !table || matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return { done: Promise.resolve(), cancel() {} };
  }
  const range = stage.getBoundingClientRect();
  const scale = range.width / stage.offsetWidth;
  const local = node => {
    const box = node.getBoundingClientRect();
    return { x: (box.left + box.width / 2 - range.left) / scale,
      y: (box.top + box.height / 2 - range.top) / scale + stage.scrollTop };
  };
  const target = local(seat);
  const start = local(table);
  const overlay = document.createElement("div");
  overlay.className = "collection";
  overlay.setAttribute("aria-hidden", "true");
  stage.append(overlay);
  stage.dataset.collecting = "true";
  const effects = cue.cards.map(({ card }, index) => {
    const face = drawCard(card);
    const left = start.x + (index - (cue.cards.length - 1) / 2) * 35;
    face.style.left = left + "px";
    face.style.top = start.y + "px";
    overlay.append(face);
    const keyframes = [0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1].map(offset => {
      const frame = collectionFrame(target.x - left, target.y - start.y, offset);
      return { offset, opacity: frame.opacity,
        transform: "translate(-50%, -50%) translate(" + frame.x + "px, " + frame.y + "px) scale(" + frame.scale + ")" };
    });
    return face.animate(keyframes, { delay: cue.displayMillis - cue.travelMillis, duration: cue.travelMillis, fill: "both" });
  });
  const remove = () => {
    overlay.remove();
    delete stage.dataset.collecting;
  };
  return {
    done: Promise.allSettled(effects.map(effect => effect.finished)).then(remove),
    cancel() { effects.forEach(effect => effect.cancel()); remove(); }
  };
}

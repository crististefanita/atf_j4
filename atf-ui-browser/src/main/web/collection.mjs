export function collectionFrame(dx, dy, sample) {
  return { x: dx * sample.travel, y: dy * sample.travel, scale: sample.scale, opacity: sample.alpha };
}

export function collect(stage, cue, drawCard, samples) {
  const seat = stage.querySelectorAll(".seat")[cue.winningSeat];
  const table = stage.querySelector(".cards-on-table");
  if (!seat || !table || !samples?.length || matchMedia("(prefers-reduced-motion: reduce)").matches) {
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
  const effects = cue.cards.map(({ seat, card }, index) => {
    const face = drawCard(card);
    const played = table.querySelector(`.played-card[data-seat="${seat}"] .card`);
    const origin = played ? local(played) : {
      x: start.x + (index - (cue.cards.length - 1) / 2) * 35,
      y: start.y
    };
    if (played) {
      face.style.width = face.style.minWidth = played.offsetWidth + "px";
      face.style.height = played.offsetHeight + "px";
    }
    const left = origin.x;
    face.style.left = left + "px";
    face.style.top = origin.y + "px";
    overlay.append(face);
    const keyframes = samples.map(sample => {
      const frame = collectionFrame(target.x - left, target.y - origin.y, sample);
      return { offset: sample.offset, opacity: frame.opacity,
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

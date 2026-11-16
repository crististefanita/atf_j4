/** DOM adapter for HandInput. Game commands keep their own snapshot validation. */
export function bindHandFeedback(fan, { timing, request, active, play, failed }) {
  fan.style.setProperty("--hand-rest-layer", timing.restLayer);
  fan.style.setProperty("--hand-lift-layer", timing.liftLayer);
  fan.dataset.handFeedback = "";
  const cards = [...fan.querySelectorAll("button[data-face]")];
  const initial = { rejected: "", selected: false, revision: 0, suggestions: [] };
  let feedback = initial;
  let generation = 0;
  let queue = Promise.resolve();
  let expiry;
  let lift;

  const paint = () => {
    const suggestions = new Set(feedback.suggestions);
    for (const card of cards) {
      card.classList.toggle("rejected-card", feedback.rejected === card.dataset.face);
      card.classList.toggle("suggested-card", suggestions.has(card.dataset.face));
    }
  };

  function cancel() {
    generation++;
    clearTimeout(expiry);
    lift?.cancel();
    lift = undefined;
    feedback = initial;
    paint();
    for (const card of cards) card.classList.remove("lifting-card");
  }

  function dispatch(operation, card, expires) {
    const owner = generation;
    const current = () => owner === generation && active();
    queue = queue.then(async () => {
      if (!current()) return;
      const result = await request({ operation, card: card?.dataset.face ?? "",
        rejected: feedback.rejected, selected: String(feedback.selected),
        hintRevision: feedback.revision, expires: expires ?? -1 });
      if (!current()) return;
      feedback = result;
      paint();
      if (result.action === "HINT") {
        clearTimeout(expiry);
        expiry = setTimeout(() => dispatch("clear", undefined, result.revision), timing.hintMillis);
      } else if (result.action === "PLAY" && card?.dataset.command) {
        clearTimeout(expiry);
        card.classList.add("lifting-card");
        const frames = timing.progress.map(progress => ({
          transform: "translateY(" + (-timing.liftDp * progress) + "px) scale(" +
            (1 + (timing.liftScale - 1) * progress) + ")"
        }));
        lift = card.animate(frames, {
          duration: matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : timing.liftMillis,
          fill: "forwards"
        });
        await lift.finished.catch(() => {});
        if (current()) await play(card.dataset.command);
        if (owner === generation) cancel();
      }
    }).catch(error => {
      if (current()) {
        cancel();
        failed(error);
      }
    });
  }

  fan.addEventListener("click", event => {
    const card = event.target.closest("button[data-face]");
    if (card && !card.disabled) dispatch("tap", card);
  });
  return { cancel };
}

import assert from "node:assert/strict";

function mockHand(cards) {
  let click;
  const layers = new Map();
  const buttons = Object.fromEntries(Object.entries(cards).map(([role, { face, command }]) => {
    const classes = new Set();
    const button = { dataset: { face, command }, classes,
      classList: { toggle: (name, enabled) => enabled ? classes.add(name) : classes.delete(name),
        add: name => classes.add(name), remove: name => classes.delete(name) },
      animate(frames, options) {
        let finish;
        const finished = new Promise(resolve => { finish = resolve; });
        this.effect = { frames, options, finish, finished, cancel: finish };
        return this.effect;
      }
    };
    button.closest = () => button;
    return [role, button];
  }));
  const fan = { dataset: {}, style: { setProperty: (key, value) => layers.set(key, value) },
    querySelectorAll: () => Object.values(buttons), addEventListener: (_, callback) => { click = callback; } };
  return { buttons, fan, layers, click: role => click({ target: buttons[role] }) };
}

/** Runs the same adapter assertions against Whist's live policy or a fixture in a consumer. */
export async function verifyHandPresentation(bindHandFeedback, timing, cards, request, handlePlay) {
  const originalMatchMedia = globalThis.matchMedia;
  globalThis.matchMedia = () => ({ matches: false });
  const mock = mockHand(cards);
  const tick = () => new Promise(resolve => setImmediate(resolve));
  let submitted = 0;
  let error;
  const adapter = bindHandFeedback(mock.fan, { timing, active: () => true, request,
    play: key => { submitted++; return handlePlay(key); },
    failed: failure => { error = failure; } });
  try {
    assert.equal(mock.layers.get("--hand-lift-layer"), -1);
    assert.equal(mock.layers.get("--hand-rest-layer"), 1);
    mock.click("wrong");
    await tick();
    assert.ok(mock.buttons.wrong.classes.has("rejected-card"));
    assert.ok(mock.buttons.first.classes.has("suggested-card"));
    assert.ok(mock.buttons.second.classes.has("suggested-card"));
    assert.ok(Object.values(mock.buttons).every(button => !button.effect));
    assert.equal(submitted, 0);

    mock.click("second");
    await tick();
    const interrupted = mock.buttons.second.effect;
    assert.equal(interrupted.options.duration, 150);
    assert.equal(interrupted.frames.length, 11);
    assert.equal(interrupted.frames[0].transform, "translateY(0px) scale(1)");
    assert.match(interrupted.frames[5].transform, /^translateY\(-10px\) scale\(1\.06/u);
    assert.match(interrupted.frames.at(-1).transform, /^translateY\(-20px\) scale\(1\.12/u);
    assert.ok(Math.abs(timing.progress[2] - 0.0954915) < 0.000001);
    assert.equal(submitted, 0);
    adapter.cancel();
    await tick();
    assert.equal(submitted, 0);
    mock.click("second");
    await tick();
    assert.notEqual(mock.buttons.second.effect, interrupted);
    mock.buttons.second.effect.finish();
    await tick();
    assert.ifError(error);
    assert.equal(submitted, 1);
    return { duration: interrupted.options.duration, layers: Object.fromEntries(mock.layers),
      start: interrupted.frames[0].transform, halfway: interrupted.frames[5].transform,
      end: interrupted.frames.at(-1).transform, submitted };
  } finally {
    adapter.cancel();
    globalThis.matchMedia = originalMatchMedia;
  }
}

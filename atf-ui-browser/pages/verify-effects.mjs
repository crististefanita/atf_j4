import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const [site, output] = process.argv.slice(2);
if (!site || !output) throw new Error("Expected compiled site and evidence directory");
const asset = file => import(pathToFileURL(join(resolve(site), file)));
const { initialize, commandLocal } = await asset("whist-engine.mjs");
const { collect } = await asset("collection.mjs");
const checks = [];
let profile = { name: null, progress: null, checkpoint: null, rankings: null };
let legalFixture;

function call(path, fields = {}) {
  const values = Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, String(value)]));
  const reply = JSON.parse(commandLocal("/api/" + path, JSON.stringify(values), profile.name, profile.progress,
    profile.checkpoint, profile.rankings));
  assert.equal(reply.status, undefined, reply.error);
  profile = Object.fromEntries(Object.keys(profile).map(key => [key, reply[key]]));
  return reply.result;
}
const act = (state, action) => call("action", { revision: state.revision, action });

async function check(name, expected, exercise) {
  try {
    checks.push({ id: "WEB-08", name, expected, status: "PASS", observed: await exercise() });
  } catch (failure) {
    checks.push({ id: "WEB-08", name, expected, status: "FAIL", observed: failure.stack });
  }
  console.log(checks.at(-1).status + " " + name);
}

let samples;
let cue;
await check("Proiecția APK pentru chenar, mână și efectul de levată",
  "Licitație și levate în chenar, marcaj legal din același model, aceeași ordine și durată a colectării", () => {
    initialize(31, 9);
    const setup = call("setup");
    samples = setup.collectionSamples;
    const colors = Object.fromEntries(setup.palette.map(token => [token.name, token.color]));
    assert.equal(colors["--whist-score-head"], "38px");
    assert.equal(colors["--whist-score-row"], "43px");
    assert.equal(colors["--whist-score-human"], "#ebf3e8");
    let state = call("start", { revision: 0, players: 4, mode: "ROMANIAN_WHIST" });
    assert.ok(state.view.seats.every(seat => /^Licitat: /.test(seat.firstFact) && /^Levate: /.test(seat.secondFact)));
    assert.equal(state.view.seats.filter(seat => seat.active).length, 1);
    state = act(state, "auto");
    assert.ok(state.playback.length);
    cue = state.collections[0];
    assert.equal(cue.displayMillis, 1500);
    assert.equal(cue.holdMillis, 1000);
    assert.equal(cue.travelMillis, 500);
    assert.deepEqual(cue.cards, state.playback.map(step => step.play));
    assert.equal(new Set(cue.cards.map(card => card.seat)).size, 4);
    assert.ok(cue.cards.some(play => play.seat === cue.winningSeat));
    const score = state.view.score.table;
    const saved = JSON.parse(profile.checkpoint);
    for (const [seat, row] of score.rows.entries()) {
      assert.equal(row[1], String(saved.players[seat].bid));
      assert.equal(Number(row[4]), saved.scores.hands.at(-1)[seat]);
    }

    for (let hand = 1; hand < 10 && !legalFixture; hand++) {
      state = act(state, "next");
      for (let decision = 0; !state.view.score && decision < 35; decision++) {
        const cards = state.view.hand;
        if (cards.some(card => card.action) && cards.some(card => !card.action)) {
          legalFixture = profile.checkpoint;
          assert.ok(cards.filter(card => card.action).every(card => /^card:/.test(card.action)));
          return { orderedSeats: cue.cards.map(play => play.seat), winner: cue.winningSeat,
            hold: cue.holdMillis, movement: cue.travelMillis, hand: hand + 1,
            legal: cards.filter(card => card.action).map(card => card.rank + card.suit),
            restricted: cards.filter(card => !card.action).map(card => card.rank + card.suit) };
        }
        state = act(state, "auto-step");
      }
    }
    assert.fail("Expected a hand with both legal and restricted cards");
  });

function surface(reduced) {
  globalThis.matchMedia = () => ({ matches: reduced });
  const animations = [];
  const clock = {
    timeout: undefined,
    setTimeout(work, delay) { this.timeout = { work, delay }; return 1; },
    clearTimeout() { this.timeout = undefined; }
  };
  const seat = { getBoundingClientRect: () => ({ left: 24, top: 88, width: 60, height: 40 }) };
  const table = { getBoundingClientRect: () => ({ left: 80, top: 160, width: 40, height: 40 }),
    querySelector: () => null };
  let attached = 0;
  globalThis.document = { createElement: () => ({
    setAttribute() {}, append() {}, remove() { attached--; }
  }) };
  const stage = { dataset: {}, offsetWidth: 414, scrollTop: 0, getBoundingClientRect: () => ({ left: 0, top: 0, width: 414 }),
    querySelectorAll: () => [seat, seat, seat, seat], querySelector: () => table, append() { attached++; } };
  function draw() {
    return { style: {}, animate(frames, timing) {
      const effect = { frames, timing, cancelled: false, cancel() { this.cancelled = true; } };
      animations.push(effect);
      return effect;
    } };
  }
  return { animations, clock, stage, draw, attached: () => attached };
}

await check("Redimensionarea păstrează fereastra de afișare a levatei",
  "Anularea desenului lasă temporizatorul activ și curăță o singură dată suprapunerea", async () => {
    assert.ok(cue);
    const screen = surface(false);
    const run = collect(screen.stage, cue, screen.draw, samples, screen.clock);
    let ended = false;
    run.done.then(() => { ended = true; });
    assert.equal(screen.attached(), 1);
    assert.equal(screen.clock.timeout.delay, 1500);
    assert.equal(screen.animations.length, cue.cards.length);
    assert.ok(screen.animations.every(animation => animation.timing.delay === 1000 && animation.timing.duration === 500));
    assert.equal(screen.animations[0].frames.at(-1).opacity, 0);
    run.hideOverlay();
    run.hideOverlay();
    assert.equal(screen.attached(), 0);
    assert.ok(screen.animations.every(animation => animation.cancelled));
    assert.equal(screen.stage.dataset.collecting, undefined);
    await Promise.resolve();
    assert.equal(ended, false);
    screen.clock.timeout.work();
    await run.done;
    assert.equal(ended, true);
    return { remainingTimerPreserved: true, effects: screen.animations.length };
  });

await check("Mișcare redusă și anulare fără cadre rămase",
  "Cărțile rămân vizibile pentru intervalul normal, fără suprapunere; anularea închide așteptarea", async () => {
    assert.ok(cue);
    const screen = surface(true);
    const run = collect(screen.stage, cue, screen.draw, samples, screen.clock);
    assert.equal(screen.animations.length, 0);
    assert.equal(screen.attached(), 0);
    assert.equal(screen.clock.timeout.delay, 1500);
    run.cancel();
    await run.done;
    assert.equal(screen.clock.timeout, undefined);
    return { movement: "reduced", displayMillis: cue.displayMillis };
  });

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(checks, null, 2));
if (legalFixture) await writeFile(join(output, "legal-fixture.json"), JSON.stringify({ checkpoint: legalFixture }));
if (checks.some(check => check.status !== "PASS")) process.exitCode = 1;

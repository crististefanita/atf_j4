import assert from "node:assert/strict";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const [site, output] = process.argv.slice(2);
if (!site || !output) throw new Error("Expected compiled site and evidence directory");
const asset = name => import(pathToFileURL(join(resolve(site), name)));
const { initialize, commandLocal } = await asset("whist-engine.mjs");
const { bindHandFeedback } = await asset("hand-feedback.mjs");
const scenarioFile = new URL("../../whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv", import.meta.url);
const scenario = (await readFile(scenarioFile, "utf8")).split(/\r?\n/u)
  .filter(line => line && !line.startsWith("#")).map(line => line.split("|"));
const cases = [];
let local = { name: null, progress: null, checkpoint: null, rankings: null };
let fixture, state, cards, style, timing;

function command(path, fields = {}, expectedStatus) {
  const inputs = Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, String(value)]));
  const reply = JSON.parse(commandLocal("/api/" + path, JSON.stringify(inputs), local.name, local.progress,
    local.checkpoint, local.rankings));
  assert.equal(reply.status, expectedStatus, reply.error);
  if (expectedStatus) return;
  local = Object.fromEntries(Object.keys(local).map(key => [key, reply[key]]));
  return reply.result;
}

const id = card => card.rank + card.suit;
const act = action => command("action", { revision: state.revision, action });
const initial = { rejected: "", selected: false, revision: 0, suggestions: [] };
const request = (current, operation, card, expires, revision = state.revision, status) => command("hand-feedback",
  { revision, operation, card: card ?? "", hintRevision: current.revision, selected: current.selected,
    rejected: current.rejected, expires: expires ?? -1 }, status);

async function check(name, expected, exercise) {
  try { cases.push({ id: "WEB-09", name, expected, status: "PASS", observed: await exercise() }); }
  catch (error) { cases.push({ id: "WEB-09", name, expected, status: "FAIL", observed: error.stack }); }
  console.log(cases.at(-1).status + " " + name);
}

await check("Același scenariu de reacție la atingere ca în testul Android",
  "Același fișier, aceleași revizii, refuz fără mutare și o singură alegere acceptată", () => {
    initialize(31, 9);
    const setup = command("setup");
    timing = setup.copy.ro.handFeedback;
    style = Object.fromEntries(setup.palette.map(token => [token.name, token.color]));
    assert.deepEqual([timing.hintMillis, timing.liftMillis, timing.liftDp], [1100, 150, 20]);
    assert.ok(Math.abs(timing.liftScale - 1.12) < 0.000001);
    state = command("start", { revision: 0, players: 4, mode: "ROMANIAN_WHIST" });
    assert.equal(request(initial, "tap", id(state.view.hand[0])).action, "IGNORE");
    for (let step = 0; step < 250; step++) {
      const good = state.view.hand.filter(card => card.action);
      const bad = state.view.hand.filter(card => !card.action);
      if (good.length === 2 && bad.length >= 2 && state.view.played.length > 0) {
        cards = { first: good[0], second: good[1], wrong: bad[0], other: bad[1] };
        break;
      }
      state = act(state.view.score ? "next" : "auto-step");
    }
    assert.ok(cards, "Expected two legal and two rejected faces in a real hand");
    fixture = local.checkpoint;
    const aliases = new Map(Object.entries(cards).map(([role, card]) => [id(card), role]));
    const seen = [];
    let current = initial;
    for (const [operation, input, outcome, rejected, selected, revision, suggestions] of scenario) {
      current = request(current, operation, operation === "tap" ? id(cards[input]) : "", operation === "clear" ? Number(input) : -1);
      const observed = [current.action, current.rejected ? aliases.get(current.rejected) : "",
        String(current.selected), String(current.revision), current.suggestions.map(card => aliases.get(card)).join(",")];
      assert.deepEqual(observed, [outcome, rejected, selected, revision, suggestions], operation + " " + input);
      assert.equal(local.checkpoint, fixture);
      seen.push(observed);
    }
    request(initial, "tap", id(cards.first), -1, state.revision - 1, 409);
    return { cards: Object.fromEntries(Object.entries(cards).map(([role, card]) => [role, id(card)])), seen };
  });

function mockHand(realCards) {
  let click;
  const buttons = Object.values(realCards).map(card => {
    const classes = new Set();
    const button = { disabled: false, dataset: { face: id(card), command: card.action }, classes,
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
    return button;
  });
  return { buttons, fan: { querySelectorAll: () => buttons, addEventListener: (_, callback) => { click = callback; } },
    click(role) { click({ target: buttons.find(button => button.dataset.face === id(realCards[role])) }); } };
}

await check("Ridicarea precede singura depunere; închiderea invalidează animația",
  "Marcaje pe loc la refuz, translație de 20 dp și selecție după intervalul APK", async () => {
    assert.ok(cards);
    globalThis.matchMedia = () => ({ matches: false });
    const mock = mockHand(cards);
    const tick = () => new Promise(resolve => setImmediate(resolve));
    let commands = 0, error;
    const adapter = bindHandFeedback(mock.fan, { timing, active: () => true,
      request: fields => command("hand-feedback", { ...fields, revision: state.revision }),
      play: key => { commands++; state = act(key); },
      failed: failure => { error = failure; } });
    mock.click("wrong");
    await tick();
    assert.ok(mock.buttons[2].classes.has("rejected-card"));
    assert.ok(mock.buttons[0].classes.has("suggested-card"));
    assert.ok(mock.buttons[1].classes.has("suggested-card"));
    assert.ok(mock.buttons.every(button => !button.effect));
    assert.equal(commands, 0);
    mock.click("second");
    await tick();
    const interrupted = mock.buttons[1].effect;
    assert.equal(interrupted.options.duration, 150);
    assert.match(interrupted.frames[1].transform, /^translateY\(-20px\) scale\(1\.12/u);
    assert.equal(commands, 0);
    adapter.cancel();
    await tick();
    assert.equal(commands, 0);
    mock.click("second");
    await tick();
    assert.notEqual(mock.buttons[1].effect, interrupted);
    mock.buttons[1].effect.finish();
    await tick();
    assert.ifError(error);
    assert.equal(commands, 1);
    assert.ok(state.playback.some(step => step.play.seat === 0 && id(step.play.card) === id(cards.second)));
    return { duration: interrupted.options.duration, transform: interrupted.frames[1].transform, submitted: commands };
  });

await check("Fața compactă și stilul păstrat în colectare",
  "Rang și simbol, text accesibil, culori și dimensiuni din modulul comun", () => {
    assert.ok(fixture);
    assert.equal(style["--whist-hint-rejected-fill"], "#ffeee8");
    assert.equal(style["--whist-hint-rejected-line"], "#b7544d");
    assert.equal(style["--whist-hint-legal-fill"], "#fff6d7");
    assert.equal(style["--whist-played-line"], "#dce3ee");
    const plays = [...state.playback.map(step => step.play),
      ...state.collections.flatMap(cue => cue.cards), ...state.view.played];
    assert.ok(plays.length > 0);
    for (const play of plays) {
      assert.equal(play.faceLabel, id(play.card));
      assert.match(play.description, /a jucat/u);
    }
    const layout = command("layout", { mode: "ROMANIAN_WHIST", width: 393, height: 400,
      players: 4, cards: 4, viewport: 660, seatHeight: 50, handWidth: 393 });
    assert.equal(layout.played.playerLabelHeightDp, 0);
    assert.equal(layout.playedStyle.faceTextSp, Math.max(13, Math.min(18, Math.floor(layout.played.cardWidthDp / 4))));
    return { compactCard: plays[0].faceLabel, faceTextSp: layout.playedStyle.faceTextSp, played: plays.length };
  });

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(cases, null, 2));
if (fixture) await writeFile(join(output, "hand-fixture.json"), JSON.stringify({ checkpoint: fixture }));
if (cases.some(item => item.status !== "PASS")) process.exitCode = 1;

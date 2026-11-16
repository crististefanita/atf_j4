import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const [site, output] = process.argv.slice(2);
if (!site || !output) throw new Error("Expected compiled site and external evidence directory");
const asset = file => import(pathToFileURL(join(resolve(site), file)));
const { initialize, commandLocal } = await asset("whist-engine.mjs");
const { autoGame } = await asset("auto-game.mjs");
const { dismissGameMenuOnOutsideClick } = await asset("profile.mjs");
const checks = [];
let stored;
let replayFixture;

function reset() {
  initialize(31, 9);
  stored = { name: null, progress: null, checkpoint: null, rankings: null };
}

function send(path, fields = {}, expectedStatus) {
  const values = Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, String(value)]));
  const reply = JSON.parse(commandLocal("/api/" + path, JSON.stringify(values),
    stored.name, stored.progress, stored.checkpoint, stored.rankings));
  assert.equal(reply.status, expectedStatus, reply.error);
  if (expectedStatus) return;
  for (const key of Object.keys(stored)) stored[key] = reply[key];
  return reply.result;
}

async function check(name, expected, exercise) {
  reset();
  try {
    checks.push({ id: "WEB-06", name, expected, status: "PASS", observed: await exercise() });
  } catch (failure) {
    checks.push({ id: "WEB-06", name, expected, status: "FAIL", observed: failure.stack });
  }
  console.log(checks.at(-1).status + " " + name);
}

const start = (fields = {}, revision = 0) => send("start", { revision, ...fields });
const act = (state, action, error) => send("action", { revision: state.revision, action }, error);

await check("Atingerea mesei închide meniul de joc",
  "Clickul în meniu îl păstrează deschis, clickul pe masă îl închide inclusiv după redesenare", () => {
    const item = {};
    const felt = {};
    const makeMenu = () => ({ open: true, contains: target => target === item });
    let menu = makeMenu();
    let click;
    dismissGameMenuOnOutsideClick({
      addEventListener(type, listener, options) {
        assert.equal(type, "click");
        assert.equal(options.capture, true);
        click = listener;
      },
      querySelector(selector) {
        assert.equal(selector, ".whist-navigation[open]");
        return menu.open ? menu : null;
      }
    });
    click({ target: item });
    assert.equal(menu.open, true);
    click({ target: felt });
    assert.equal(menu.open, false);
    click({ target: felt });
    assert.equal(menu.open, false);
    menu = makeMenu();
    click({ target: felt });
    assert.equal(menu.open, false);
    return { insideKeepsOpen: true, outsideDismisses: true, afterRerenderDismisses: true };
  });

await check("Valori implicite APK și reguli RO/EN pentru 3–6 jucători",
  "Oh Hell la 4 jucători, șase variante și cele 48 de previzualizări localizate", () => {
    const setup = send("setup");
    const fields = Object.fromEntries(setup.fields.map(field => [field.key, field]));
    assert.equal(fields.players.initial, "4");
    assert.equal(fields.mode.initial, "OH_HELL");
    assert.equal(fields.difficulty.initial, "NORMAL");
    assert.equal(fields.series.initial, "10");
    const checked = [];
    for (const { value } of fields.mode.options) {
      for (let seats = 3; seats <= 6; seats++) {
        const ro = setup.copy.ro.modeRules[value];
        const en = setup.copy.en.modeRules[value];
        assert.match(ro.schedule[seats], /Cărți pe mână.*\d/u);
        assert.match(en.schedule[seats], /Cards per hand.*\d/u);
        assert.notEqual(ro.schedule[seats], en.schedule[seats]);
        assert.equal(ro.extras.length, ro.romanian ? 2 : 0);
        assert.equal(en.extras.length, en.romanian ? 2 : 0);
        checked.push(value + "/" + seats);
      }
    }
    assert.equal(checked.length, 24);
    assert.match(setup.copy.en.modeRules.ROMANIAN_WHIST.schedule[4], /24 hands/);
    assert.match(setup.copy.en.modeRules.ROMANIAN_WHIST.schedule[4], /1×4.*8×4.*1×4/);
    return { initial: fields.mode.initial, localizations: checked.length * 2 };
  });

await check("Comutarea modului și a limbii fără schimbarea partidei",
  "Setările românești sunt aplicate numai când sunt eligibile și păstrează valoarea aleasă", () => {
    let state = start({ mode: "OH_HELL", series: 5, streaks: "yes", zero: "yes", players: 3 });
    let game = JSON.parse(stored.checkpoint);
    assert.equal(game.streakBonuses, false);
    assert.equal(game.allowZeroGame, false);
    assert.equal(game.streakBonusPoints, 5);
    state = start({ mode: "LONG_ROMANIAN", players: 5, series: 5, streaks: "no", zero: "yes" }, state.revision);
    game = JSON.parse(stored.checkpoint);
    assert.equal(game.streakBonuses, false);
    assert.equal(game.streakBonusPoints, 5);
    assert.equal(game.streakPenaltyPoints, 5);
    assert.equal(game.allowZeroGame, true);
    const checkpoint = stored.checkpoint;
    const score = state.view.scoreDetails;
    const translated = send("language", { revision: state.revision, language: "en" });
    assert.equal(stored.checkpoint, checkpoint);
    assert.notEqual(translated.view.scoreDetails.liveTitle, score.liveTitle);
    assert.match(translated.view.scoreDetails.liveTitle, /^Current scores/);
    assert.equal(translated.view.scoreDetails.liveTable.rows.length, 5);
    assert.ok(translated.view.scoreDetails.liveTable.rows.every(row => row.at(-1) === "0"));
    return { mode: game.mode, seats: game.playerCount, zero: game.allowZeroGame, keptBonus: game.streakBonusPoints };
  });

await check("Joacă din nou din ultimul scor și numai din ultimul scor",
  "Distribuire nouă, identificator nou, aceleași reguli și nicio creditare dublă", () => {
    let state = start({ mode: "ROMANIAN_WHIST", players: 3, difficulty: "HARD",
      series: 5, streaks: "yes", zero: "yes", name: "WebProbe" });
    const initial = stored.checkpoint;
    act(state, "again", 400);
    assert.equal(stored.checkpoint, initial);
    state = act(state, "auto");
    assert.ok(state.view.scoreDetails.rules.some(rule => rule.includes("+5")));
    assert.ok(state.view.scoreDetails.rules.some(rule => rule.includes("500")));
    act(state, "again", 400);
    let hands = 1;
    while (state.view.actions.some(move => move.key === "next")) {
      state = act(act(state, "next"), "auto");
      if (++hands > 40) throw new Error("Expected a finite schedule");
    }
    const savedFinal = stored.checkpoint;
    const completed = JSON.parse(savedFinal);
    const result = { rankings: stored.rankings, progress: stored.progress };
    assert.ok(state.view.scoreDetails.winner);
    assert.equal(state.view.actions[0].label, "Joacă din nou");
    initialize(31, 9);
    state = send("state");
    state = send("language", { revision: state.revision, language: "en" });
    assert.equal(state.view.actions[0].label, "Play again");
    const previous = state;
    state = act(state, "again");
    const freshCheckpoint = stored.checkpoint;
    const fresh = JSON.parse(freshCheckpoint);
    assert.notEqual(fresh.seed, completed.seed);
    assert.notEqual(fresh.matchId, completed.matchId);
    for (const key of ["mode", "playerCount", "difficulty", "streakBonuses", "streakBonusPoints",
      "streakPenaltyPoints", "allowZeroGame"]) assert.equal(fresh[key], completed[key], key);
    assert.equal(state.view.seats[0].name, "WebProbe");
    assert.equal(state.view.history.rows.length, 0);
    assert.equal(state.view.score, null);
    assert.equal(fresh.handIndex, 0);
    assert.equal(stored.progress, result.progress);
    assert.equal(stored.rankings, result.rankings);
    act(previous, "again", 409);
    initialize(61, 5);
    assert.equal(send("state").view.heading, "Mână 1");
    assert.equal(stored.checkpoint, freshCheckpoint);
    replayFixture = savedFinal;
    return { hands, mode: fresh.mode, players: fresh.playerCount, points: fresh.streakBonusPoints };
  });

await check("Oprirea temporară a AutoGame în meniuri și dialoguri",
  "Bifa rămâne activă, temporizatorul vechi se anulează, iar comanda pornește o singură dată după închidere", async () => {
    const timers = [], commands = [], changes = [];
    const tick = () => new Promise(resolve => setImmediate(resolve));
    const control = autoGame({
      available: () => true,
      wait: signal => new Promise(resolve => {
        timers.push({ signal, resolve });
        signal.addEventListener("abort", resolve, { once: true });
      }),
      step: () => new Promise(resolve => commands.push(resolve)),
      changed: selected => changes.push(selected)
    });
    control.set(true);
    control.pause(true);
    await tick();
    assert.ok(timers[0].signal.aborted);
    assert.equal(commands.length, 0);
    assert.equal(control.selected(), true);
    control.pause(false);
    timers[1].resolve();
    await tick();
    assert.equal(commands.length, 1);
    control.pause(true);
    commands[0](true);
    await tick();
    assert.equal(timers.length, 2);
    control.pause(false);
    await tick();
    assert.equal(timers.length, 3);
    control.set(false);
    await tick();
    assert.deepEqual(changes, [true, false]);
    return { submitted: commands.length, timerCount: timers.length, changes };
  });

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(checks, null, 2));
if (replayFixture) await writeFile(join(output, "replay-fixture.json"), JSON.stringify({ checkpoint: replayFixture }));
if (checks.some(check => check.status !== "PASS")) process.exitCode = 1;

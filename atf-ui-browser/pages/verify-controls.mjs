import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const [site, output, ...selected] = process.argv.slice(2);
if (!site || !output) throw new Error("Expected site and external evidence directories");
const asset = file => import(pathToFileURL(join(resolve(site), file)));
const { autoGame } = await asset("auto-game.mjs");
const { command, commandLocal, initialize } = await asset("whist-engine.mjs");
const checks = [];

async function check(id, name, expected, exercise) {
  if (selected.length && !selected.includes(id)) return;
  try {
    checks.push({ id, name, expected, status: "PASS", observed: await exercise() });
  } catch (error) {
    checks.push({ id, name, expected, status: "FAIL", observed: error.stack });
  }
  console.log(checks.at(-1).status + " " + name);
}

function invoke(path, values = {}, profile = {}) {
  const fields = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, String(value)]));
  return JSON.parse(command("/api/" + path, JSON.stringify(fields), profile.name ?? null, profile.progress ?? null));
}

await check("WEB-02", "Țintele de licitare și rândul derulabil", "Dimensiunile și politica Android la lățimi compacte și normale", () => {
  initialize(31, 9);
  const geometry = (width, viewport, mode, players, bidOptions) => {
    const reply = invoke("layout", { width, height: 450, seatHeight: 52, handWidth: width,
      viewport, mode, players, cards: 1, bidOptions });
    assert.equal(reply.status, undefined, reply.error);
    return reply.result;
  };
  const grid = [
    { layout: geometry(360, 800, "ROMANIAN_WHIST", 4, 8), columns: 5, strip: false, rows: 2 },
    { layout: geometry(414, 896, "ROMANIAN_WHIST", 4, 8), columns: 3, strip: false, rows: 3 },
    { layout: geometry(360, 640, "DESCENDING", 3, 18), columns: 6, strip: true, rows: 1 },
    { layout: geometry(414, 896, "DESCENDING", 3, 18), columns: 6, strip: false, rows: 3 }
  ];
  for (const { layout, columns, strip, rows } of grid) {
    const { bids, board } = layout;
    assert.equal(bids.columns, columns);
    assert.equal(bids.strip, strip);
    assert.equal(bids.choiceTarget, 48);
    assert.equal(bids.choiceTarget - 2 * bids.choiceInset, 34);
    assert.equal(bids.submitTarget, 48);
    assert.equal(bids.submitWidth - 2 * bids.submitInsetX, 101);
    assert.equal(bids.submitTarget - 2 * bids.submitInsetY, 38);
    assert.ok(board.center.bottom - board.center.top >= rows * bids.rowHeight + bids.submitTarget);
  }
  assert.equal(geometry(414, 896, "ROMANIAN_WHIST", 4, 0).bids, null);
  return grid.map(({ layout, rows }) => ({ ...layout.bids, rows }));
});

function automationFixture() {
  const timers = [];
  const commands = [];
  const selections = [];
  let available = true;
  const control = autoGame({
    available: () => available,
    wait: signal => new Promise(resolve => {
      signal.addEventListener("abort", resolve, { once: true });
      timers.push({ signal, resolve });
    }),
    step: () => new Promise(resolve => commands.push(resolve)),
    changed: value => selections.push(value)
  });
  return {
    control, timers, commands, selections,
    terminal: () => { available = false; },
    settle: () => new Promise(resolve => setImmediate(resolve))
  };
}

await check("WEB-03", "Debifare înaintea licitației automate", "O programare anulată lasă jucătorul la rând", async () => {
  const turn = automationFixture();
  turn.control.set(true);
  assert.equal(turn.timers.length, 1);
  turn.control.set(false);
  await turn.settle();
  assert.ok(turn.timers[0].signal.aborted);
  assert.equal(turn.commands.length, 0);
  return { selections: turn.selections, commands: turn.commands.length };
});

await check("WEB-03", "Debifare în timpul colectării și revenire la manual", "Comanda în curs se încheie și următoarea rămâne manuală", async () => {
  const turn = automationFixture();
  turn.control.set(true);
  turn.timers[0].resolve();
  await turn.settle();
  assert.equal(turn.commands.length, 1);
  turn.control.set(false);
  turn.commands[0](true);
  await turn.settle();
  assert.equal(turn.timers.length, 1);
  assert.equal(turn.control.selected(), false);
  return { selections: turn.selections, committed: turn.commands.length };
});

await check("WEB-03", "Rebifare în timpul cererii și resetare la scor", "Nicio comandă dublă, iar bifa se stinge la scor", async () => {
  const turn = automationFixture();
  turn.control.set(true);
  turn.timers[0].resolve();
  await turn.settle();
  turn.control.set(false);
  turn.control.set(true);
  assert.equal(turn.commands.length, 1);
  assert.equal(turn.timers.length, 1);
  turn.commands[0](true);
  await turn.settle();
  assert.equal(turn.timers.length, 2);
  turn.timers[1].resolve();
  await turn.settle();
  assert.equal(turn.commands.length, 2);
  turn.terminal();
  turn.commands[1](true);
  await turn.settle();
  assert.equal(turn.control.selected(), false);
  assert.equal(turn.timers.length, 2);
  return { selections: turn.selections, commands: turn.commands.length };
});

await check("WEB-03-ENGINE", "O singură decizie umană per comandă în motor", "Licitație automată, apoi mutare manuală legală și refuz la scor", () => {
  initialize(31, 9);
  let state = invoke("start", { revision: 0, mode: "ROMANIAN_WHIST" }).result;
  const suggested = state.view.actions.find(move => move.emphasized);
  assert.ok(suggested.key.startsWith("bid:"));
  const step = () => invoke("action", { revision: state.revision, action: "auto-step" });
  state = step().result;
  assert.ok(state.view.hand.some(card => card.action));
  assert.ok(state.playback.every(event => event.play.seat !== 0));
  const card = state.view.hand.find(face => face.action);
  assert.deepEqual(invoke("state").result.view, state.view);
  state = invoke("action", { revision: state.revision, action: card.action }).result;
  assert.equal(state.playback.filter(event => event.play.seat === 0).length, 1);
  assert.equal(state.view.history.rows[0][1].split(" / ")[0], suggested.label);
  assert.equal(step().status, 400);
  return { bid: suggested.label, manual: card.action, score: state.view.history.rows[0] };
});

await check("WEB-01-LOCALE", "Română, engleză și realizări locale după redeschidere",
  "Texte și reguli din APK, păstrarea profilului și a realizărilor, fără modificarea mâinii la schimbarea limbii", async () => {
  const values = new Map();
  globalThis.localStorage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  let request;
  globalThis.Worker = class {
    constructor() { initialize(31, 9); }
    addEventListener(kind, listener) { if (kind === "message") request = listener; }
    postMessage({ id, path, values: fields, profile }) {
      queueMicrotask(() => request({ data: { id, ...JSON.parse(commandLocal(path, JSON.stringify(fields),
        profile.name, profile.progress, profile.checkpoint, profile.rankings)) } }));
    }
  };
  const { localTransport } = await asset("local-transport.mjs");
  const { preferredLanguage, saveLanguage } = await asset("profile.mjs");
  let browser = localTransport();
  const config = await browser.request("/api/setup");
  assert.equal(config.copy.ro.achievements, "Realizări");
  assert.equal(config.copy.en.achievements, "Achievements");
  assert.equal(config.copy.en.bid, "Place bid");
  assert.ok(config.copy.en.rules.length > 1);
  assert.notEqual(config.copy.en.rules[0].body, config.copy.ro.rules[0].body);
  assert.notEqual(config.fields[1].label, config.englishFields[1].label);
  assert.ok(config.autoDelayMillis > 0);
  assert.equal((await browser.request("/api/state")).profile.name, "Tu");
  let state = await browser.request("/api/profile", { revision: 0, name: "Ștefan" });
  state = await browser.request("/api/start", { revision: state.revision, mode: "ROMANIAN_WHIST" });
  const romanian = state;
  await assert.rejects(browser.request("/api/language", { revision: state.revision, language: "other" }),
    { status: 400 });
  state = await browser.request("/api/language", { revision: state.revision, language: "en" });
  saveLanguage("en");
  assert.equal(preferredLanguage(), "en");
  assert.equal(state.view.prompt, "Choose your bid");
  assert.equal(state.view.actions.find(move => move.key === "auto-step").label, "Auto play");
  assert.deepEqual(state.view.hand.map(card => card.rank + card.suit), romanian.view.hand.map(card => card.rank + card.suit));
  const goals = catalogue => catalogue.groups.flatMap(group => group.goals);
  assert.deepEqual(goals(state.profile.catalogue).map(goal => goal.id), goals(romanian.profile.catalogue).map(goal => goal.id));
  assert.notEqual(goals(state.profile.catalogue)[0].title, goals(romanian.profile.catalogue)[0].title);
  for (let hand = 0; hand < 5 && !state.profile.notice; hand++) {
    if (hand) state = await browser.request("/api/action", { revision: state.revision, action: "next" });
    for (let decision = 0; decision < 3 && !state.view.score; decision++) {
      state = await browser.request("/api/action", { revision: state.revision, action: "auto-step" });
    }
    assert.ok(state.view.score);
  }
  assert.ok(state.profile.notice?.goals.length);
  const earned = goals(state.profile.catalogue).filter(goal => goal.complete);
  assert.ok(earned.length);
  state = await browser.request("/api/notice", { revision: state.revision, dismiss: "yes" });
  browser = localTransport();
  assert.equal(preferredLanguage(), "en");
  const resumed = await browser.request("/api/state");
  const reopened = await browser.request("/api/language", { revision: resumed.revision, language: preferredLanguage() });
  assert.equal(reopened.profile.name, "Ștefan");
  assert.equal(reopened.profile.notice, null);
  assert.deepEqual(reopened.profile.catalogue, state.profile.catalogue);
  assert.deepEqual(reopened.view.score, state.view.score);
  return { language: preferredLanguage(), profile: reopened.profile.name,
    ruleSections: config.copy.en.rules.length, achievementIds: earned.map(goal => goal.id) };
});

if (!checks.length) throw new Error("No matching component cases");
await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(checks, null, 2));
if (checks.some(check => check.status === "FAIL")) process.exitCode = 1;

import assert from "node:assert/strict";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const [site, output, fixturesPath] = process.argv.slice(2);
if (!site || !output || !fixturesPath) throw new Error("Expected site, external evidence directory and native fixtures");
const asset = file => pathToFileURL(join(resolve(site), file)).href;
const { localTransport } = await import(asset("local-transport.mjs"));
const fixtures = JSON.parse(await readFile(fixturesPath, "utf8"));
const key = "whistlab:pages:profile:v1";
const values = new Map();
let rejectWrite = false;
let workers = 0;
globalThis.localStorage = {
  getItem: name => values.get(name) ?? null,
  setItem(name, value) {
    if (rejectWrite) throw new Error("Storage unavailable");
    values.set(name, value);
  }
};
globalThis.Worker = class {
  constructor() {
    this.engine = import(asset("whist-engine.mjs") + "?worker=" + ++workers).then(engine => {
      engine.initialize(31, 9);
      return engine;
    });
  }
  addEventListener(kind, callback) { if (kind === "message") this.reply = callback; }
  async postMessage({ id, path, values: fields, profile }) {
    const { commandLocal } = await this.engine;
    const response = commandLocal(path, JSON.stringify(fields), profile.name, profile.progress,
      profile.checkpoint, profile.rankings);
    this.reply({ data: { id, ...JSON.parse(response) } });
  }
};

const checks = [];
async function check(id, name, expected, exercise) {
  values.clear();
  rejectWrite = false;
  try {
    checks.push({ id, name, expected, status: "PASS", observed: await exercise() });
  } catch (failure) {
    checks.push({ id, name, expected, status: "FAIL", observed: failure.stack });
  }
  console.log(checks.at(-1).status + " " + name);
}

function stored() { return JSON.parse(values.get(key)); }
const stateOf = browser => browser.request("/api/state");
const start = (browser, state, options = {}) => browser.request("/api/start", { revision: state.revision, ...options });
const step = (browser, state, action) => browser.request("/api/action", { revision: state.revision, action });

await check("WEB-04", "Checkpoint după licitare, în joc și la scor",
  "Aceleași cărți, scor, istoric și identitate după un worker nou; profilul vechi se păstrează", async () => {
    values.set(key, JSON.stringify({ name: "WebProbe", progress: null }));
    let browser = localTransport();
    let state = await start(browser, await stateOf(browser));
    const matchId = JSON.parse(stored().checkpoint).matchId;
    const stages = [];
    for (const stage of ["bidding", "playing", "settled"]) {
      const before = stored();
      browser = localTransport();
      assert.equal(browser.resumeOnHome, true);
      const resumed = await stateOf(browser);
      assert.deepEqual(resumed.view, state.view);
      assert.deepEqual(resumed.profile, state.profile);
      assert.deepEqual(resumed.playback, []);
      assert.equal(resumed.profile.name, "WebProbe");
      assert.equal(JSON.parse(before.checkpoint).matchId, matchId);
      assert.equal(stored().progress, before.progress);
      stages.push(JSON.parse(before.checkpoint).phase);
      if (stage !== "settled") state = await step(browser, resumed, "auto-step");
    }
    assert.deepEqual(stages, ["BIDDING", "PLAYING", "SCORE"]);
    assert.equal(state.view.history.rows.length, 1);
    return { stages, name: state.profile.name, historyRows: state.view.history.rows.length };
  });

await check("WEB-04", "Checkpoint străin, depășit sau deteriorat",
  "Fila depășită primește conflict și datele deteriorate nu afectează profilul", async () => {
    const a = localTransport();
    const before = await start(a, await stateOf(a));
    const b = localTransport();
    const old = await stateOf(b);
    await step(a, before, "auto-step");
    const active = stored().checkpoint;
    await assert.rejects(step(b, old, "auto-step"), { status: 409 });
    assert.equal(stored().checkpoint, active);
    const damaged = JSON.parse(active);
    damaged.players[0].hand[0].rank = 99;
    values.set(key, JSON.stringify({ name: "Local", progress: null, checkpoint: JSON.stringify(damaged) }));
    const recovered = await stateOf(localTransport());
    assert.equal(recovered.view, null);
    assert.equal(recovered.storageIssue, "savedGameError");
    assert.equal(recovered.profile.name, "Local");
    assert.equal(stored().checkpoint, null);
    return { stale: 409, damaged: recovered.storageIssue };
  });

await check("WEB-04", "Refuzul de scriere păstrează partida din fila curentă",
  "Când stocarea nu mai acceptă scrieri, următoarea mutare folosește starea din memorie", async () => {
    const browser = localTransport();
    const initial = await start(browser, await stateOf(browser));
    const durable = stored().checkpoint;
    rejectWrite = true;
    const playing = await step(browser, initial, "auto-step");
    assert.equal(playing.storageIssue, "storageError");
    const settled = await step(browser, playing, "auto-step");
    assert.ok(settled.view.score);
    assert.equal(stored().checkpoint, durable);
    return { warning: settled.storageIssue, score: settled.view.score.table.rows[0] };
  });

await check("WEB-05", "Serii și ratări în istoricul APK și web",
  "Ambele sensuri ale ajustării, bid tăiat și total cumulativ la 3–6 jucători, în română și engleză", async () => {
    const verified = [];
    for (const fixture of fixtures) {
      values.set(key, JSON.stringify({ checkpoint: fixture.checkpoint }));
      const browser = localTransport();
      const state = await stateOf(browser);
      const save = JSON.parse(fixture.checkpoint);
      const row = state.view.history.grid.rows[save.handIndex];
      assert.equal(row.status, "RECORDED");
      assert.equal(row.cells.length, fixture.seats);
      assert.equal(row.cells[0].series, -5);
      assert.equal(row.cells[0].missed, true);
      assert.equal(row.cells[1].series, 10);
      assert.equal(row.cells[1].missed, false);
      assert.ok(row.cells.every((cell, seat) => cell.total === String(save.scores.hands.at(-1)[seat])));
      assert.equal(state.view.history.grid.rows[save.handIndex + 1].cells[0].total, "—");
      const translated = await browser.request("/api/language", { revision: state.revision, language: "en" });
      assert.equal(translated.view.history.grid.cardsLabel, "Cards");
      assert.notEqual(translated.view.history.grid.legend, state.view.history.grid.legend);
      assert.equal(translated.view.history.grid.rows[save.handIndex].cells[0].series, -5);
      verified.push({ players: fixture.seats, hand: save.handIndex + 1, penalty: -5, bonus: 10 });
    }
    return verified;
  });

await check("WEB-05", "Clasament local din partida completă, fără dublarea rezultatelor",
  "Profilul include varianta, locurile, dificultatea, seriile și regula de zero; topul include AI și exemple marcate", async () => {
    const browser = localTransport();
    const initial = await stateOf(browser);
    const examples = await browser.request("/api/rankings");
    assert.ok(examples.boards.find(board => board.key === examples.preferred).entries.every(entry => entry.sample && entry.ai));
    let state = await start(browser, initial, { name: "WebProbe", players: 3, difficulty: "HARD", series: 5, zero: "yes" });
    do {
      state = await step(browser, state, "auto");
      if (!state.view.actions.some(move => move.key === "next")) break;
      state = await step(browser, state, "next");
    } while (true);
    assert.ok(state.view.score);
    const rankings = await browser.request("/api/rankings");
    const board = rankings.boards.find(candidate => candidate.key === rankings.preferred);
    assert.equal(board.key, "ROMANIAN_WHIST:3:HARD:5:5:zero");
    const entries = board.entries.filter(entry => !entry.sample);
    assert.equal(entries.length, 3);
    assert.equal(entries.filter(entry => entry.ai).length, 2);
    const expected = state.view.score.table.rows.map(row => Number(row.at(-1))).sort((a, b) => b - a);
    assert.deepEqual(entries.map(entry => entry.score), expected);
    const before = stored();
    const reopened = localTransport();
    const settled = await stateOf(reopened);
    assert.deepEqual(settled.view.score, state.view.score);
    assert.deepEqual(await reopened.request("/api/rankings"), rankings);
    assert.equal(stored().rankings, before.rankings);
    assert.equal(stored().progress, before.progress);
    return { profile: board.key, entries, hands: state.view.history.rows.length };
  });

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(checks, null, 2));
if (checks.some(check => check.status !== "PASS")) process.exitCode = 1;

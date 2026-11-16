import assert from "node:assert/strict";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { verifyHandPresentation } from "../tests/hand-presentation.mjs";

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
    timing = setup.handMotion;
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

await check("Ridicarea precede singura depunere; închiderea invalidează animația",
  "Marcaje pe loc la refuz, ritmul APK, ridicare sub vecini și o singură depunere", async () => {
    assert.ok(cards);
    const fixtureCards = Object.fromEntries(Object.entries(cards)
      .map(([role, card]) => [role, { face: id(card), command: card.action }]));
    const observed = await verifyHandPresentation(bindHandFeedback, timing, fixtureCards,
      fields => command("hand-feedback", { ...fields, revision: state.revision }),
      key => { state = act(key); });
    assert.ok(state.playback.some(step => step.play.seat === 0 && id(step.play.card) === id(cards.second)));
    return observed;
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

await check("AutoGame colectează fiecare levată, apoi publică doar cărțile curente",
  "Patru colectări în ordinea motorului, oprire la scor și fără cărți expirate în starea stabilă", () => {
    local.checkpoint = fixture;
    state = command("state");
    const gathered = [];
    const won = new Array(4).fill(0);
    for (let decision = 0; !state.view.score && decision < 4; decision++) {
      state = act("auto-step");
      assert.equal(state.playback.filter(step => step.play.seat === 0).length, 1);
      assert.deepEqual(state.playback.flatMap(step => step.collection ? [step.collection] : []), state.collections);
      for (const cue of state.collections) {
        assert.deepEqual([cue.displayMillis, cue.holdMillis, cue.travelMillis], [1500, 1000, 500]);
        assert.equal(new Set(cue.cards.map(play => play.seat)).size, 4);
        assert.ok(cue.cards.some(play => play.seat === cue.winningSeat));
        won[cue.winningSeat]++;
        assert.deepEqual(cue.seatFacts.map(fact => Number(fact.split("\n")[1].split(": ")[1])), won);
        gathered.push(cue.cards.map(play => play.faceLabel));
      }
      assert.ok(state.view.played.length < state.view.seats.length);
    }
    assert.ok(state.view.score);
    assert.deepEqual(gathered[0], ["9♦", "Q♥", "K♦", "8♦"]);
    assert.equal(gathered.length, 4);
    assert.equal(state.view.played.length, 0);
    assert.ok(!state.view.actions.some(action => action.key === "auto-step"));
    return { tricks: gathered, totals: state.view.score.table.rows.map(row => Number(row[4])) };
  });

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(cases, null, 2));
if (fixture) await writeFile(join(output, "hand-fixture.json"), JSON.stringify({ checkpoint: fixture }));
if (cases.some(item => item.status !== "PASS")) process.exitCode = 1;

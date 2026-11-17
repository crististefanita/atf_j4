import { verifyTrumpTextFit } from "../tests/trump-text-fit.mjs";
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
const { fitTextWidth } = await asset("viewport.mjs");
const scenarioFile = new URL("../../whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv", import.meta.url);
const scenario = (await readFile(scenarioFile, "utf8")).split(/\r?\n/u)
  .filter(line => line && !line.startsWith("#")).map(line => line.split("|"));
const cases = [];
let local = { name: null, progress: null, checkpoint: null, rankings: null };
let fixture, wideCheckpoint, state, cards, style, timing;

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

async function check(name, expected, exercise, id = "WEB-09") {
  try { cases.push({ id, name, expected, status: "PASS", observed: await exercise() }); }
  catch (error) { cases.push({ id, name, expected, status: "FAIL", observed: error.stack }); }
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

await check("Fața din mână și stilul păstrat în colectare",
  "Rang și simbol, text accesibil, culori și dimensiuni din modulul comun", () => {
    assert.ok(fixture);
    assert.equal(style["--whist-hint-rejected-fill"], "#ffeee8");
    assert.equal(style["--whist-hint-rejected-line"], "#b7544d");
    assert.equal(style["--whist-hint-legal-fill"], "#fff6d7");
    assert.equal(style["--whist-played-line"], "#dce3ee");
    assert.deepEqual([style["--whist-face-index-top"], style["--whist-face-suit-baseline"],
      style["--whist-face-suit-size"], style["--whist-face-inverted-size"]], ["5px", "66%", "28px", "9px"]);
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
    assert.equal(layout.played.cardWidthDp, Math.round(layout.played.cardHeightDp * 5 / 7));
    return { card: plays[0].faceLabel, faceHeight: layout.played.cardHeightDp, played: plays.length };
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

await check("Antetul browserului folosește măsurile de referință ale APK-ului",
  "Aceleași valori pentru titlu, progres, licitații, ținta meniului și chenarul de atu", async () => {
    const properties = new URL("../../whist-lab-app/app/src/test/resources/whist-header-presentation.properties", import.meta.url);
    const reference = Object.fromEntries((await readFile(properties, "utf8")).trim().split(/\r?\n/u)
      .map(line => line.split("=")).map(([key, value]) => [key, Number(value)]));
    const header = state.view.header;
    const px = name => Number.parseFloat(style["--whist-" + name]);
    const observed = {titleSp: header.titleSize, progressSp: header.progressSize, bidSp: header.bidSize,
      menuTouchDp: px("menu-touch"), trumpWidthDp: px("trump-width"), trumpMinHeightDp: px("trump-min-height"),
      trumpPaddingDp: px("trump-padding"), trumpGapDp: px("trump-gap"), trumpFrameRadiusDp: px("trump-frame-radius"),
      trumpFrameStrokeDp: px("trump-frame-stroke"),
      trumpHeadingSp: px("trump-heading-size") * 16, trumpValueSp: px("trump-value-size") * 16,
      trumpCardInsetDp: px("trump-card-inset"), trumpCardRadiusDp: px("trump-card-radius"),
      handLabelSp: px("hand-label") * 16, handWidthDp: px("hand-face-width"), handHeightDp: px("hand-face-height")};
    assert.deepEqual(Object.keys(observed).sort(), Object.keys(reference).sort());
    for (const [role, size] of Object.entries(observed)) assert.ok(Math.abs(size - reference[role]) < .001, role);
    const paletteFile = new URL("../../whist-lab-app/app/src/test/resources/whist-trump-palette.properties", import.meta.url);
    const palette = Object.fromEntries((await readFile(paletteFile, "utf8")).trim().split(/\r?\n/u).map(line => line.split("=")));
    const mapped = Object.fromEntries(Object.keys(palette).map(role => [role, style["--whist-" + role]]));
    assert.deepEqual(mapped, palette);
    return {reference, observed, palette, mapped};
  }, "WEB-12");

await check("Ranguri de carte și bandă măsurată pentru 17 cărți",
  "Scală moderată și uniformă pentru toate rangurile, cu lățimea măsurată rezervată în evantai și refuz la geometrie invalidă", () => {
    assert.equal(style["--whist-hand-label"], "0.8125rem");
    assert.equal(style["--whist-hand-rank-cap"], "26px");
    for (let seed = 31; seed < 96 && !wideCheckpoint; seed++) {
      initialize(seed, 9);
      local = { name: null, progress: null, checkpoint: null, rankings: null };
      const started = command("start", { revision: 0, players: 3, mode: "DOWN_AND_UP" });
      assert.equal(started.view.hand.length, 17);
      const ranks = new Set(started.view.hand.map(card => card.rank));
      if (started.view.hand.slice(0, -1).some(card => card.rank === "10")
          && ["A", "K", "Q", "J"].every(rank => ranks.has(rank))) wideCheckpoint = local.checkpoint;
    }
    assert.ok(wideCheckpoint, "Expected all face ranks and a 10 before the last card in a 17-card hand");
    const layout = { mode: "DOWN_AND_UP", players: 3, width: 336, height: 360, viewport: 800,
      seatHeight: 64, bidOptions: 18, cards: 17, handWidth: 336, handIndexWidth: 43 };
    const plan = command("layout", layout);
    assert.equal(plan.hand.advance, 43);
    assert.equal(plan.hand.width, 16 * 43 + plan.hand.cardWidth);
    command("layout", { ...layout, handIndexWidth: 64 }, 400);
    return { baseRankSize: style["--whist-hand-label"], handIndexWidth: 43, hand: plan.hand };
  }, "WEB-11");

await check("Atu lizibil în același chenar la font normal și mărit",
  "Aceleași cazuri ca în componenta Android, inclusiv la două cifre și fără atu", async () => {
    const file = new URL("../../whist-lab-app/app/src/test/resources/whist-trump-text-fit.tsv", import.meta.url);
    return verifyTrumpTextFit(fitTextWidth, file);
  }, "WEB-12");

await mkdir(output, { recursive: true });
await writeFile(join(output, "browser-checks.json"), JSON.stringify(cases, null, 2));
if (fixture) await writeFile(join(output, "hand-fixture.json"), JSON.stringify({ checkpoint: fixture, wideCheckpoint }));
if (cases.some(item => item.status !== "PASS")) process.exitCode = 1;

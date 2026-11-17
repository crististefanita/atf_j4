import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { isPhone, MIN_DESKTOP_SCALE, REFERENCE, viewportGeometry } from "../src/main/web/viewport.mjs";
import { collectionFrame } from "../src/main/web/collection.mjs";
import { deviceProfile, normalizedNameLength } from "../src/main/web/profile.mjs";
import { verifyViewportContract } from "./viewport-contract.mjs";
import { client, withServer } from "./fixtures.mjs";

const [output, whist, rentz, holdem, ...extra] = process.argv.slice(2);
if (!output || !whist || !rentz || !holdem || extra.length) {
  throw new Error("Expected evidence directory and Whist/Rentz/Hold’em classpaths. Use Run-BrowserUi.ps1 for browser UI.");
}
await mkdir(output, { recursive: true });
const web = fileURLToPath(new URL("../src/main/web/", import.meta.url));
const games = [
  { title: "Whist", web, classes: whist, main: "ro.whistlab.browser.WhistBrowser",
    arguments: [join(output, "component-profile-state")] },
  { title: "Rentz", web: rentz.endsWith(".jar") ? "-" : web, classes: rentz, main: "ro.rentzlab.browser.RentzBrowser" },
  { title: "Hold’em", web: holdem.endsWith(".jar") ? "-" : web, classes: holdem, main: "ro.atf.holdem.android.HoldemBrowser" }
];
const results = [];
let completedCareer;

async function audit(name, check) {
  try {
    const evidence = await check();
    results.push({ id: "WEB-ADAPTER", name, status: "PASS", ...evidence });
  } catch (error) {
    results.push({ id: "WEB-ADAPTER", name, status: "FAIL", expected: "Contract respectat", observed: error.stack });
  }
  console.log(results.at(-1).status + " " + name);
}

await audit("Modele de viewport portrait și centrare", () => {
  const observed = verifyViewportContract({ REFERENCE, MIN_DESKTOP_SCALE, isPhone, viewportGeometry });
  return { expected: "cadru telefon 1:1; desktop limitat de înălțimea utilă, cu lățime fără depășire și prag lizibil unde încape",
    observed };
});

await audit("Componente comune · profil Unicode și animația levatei", () => {
  assert.equal(normalizedNameLength("  Ștefan  "), 6);
  assert.equal(normalizedNameLength("S\u0326"), 1);
  assert.equal(normalizedNameLength("𐐀".repeat(10)), 10);
  assert.equal(normalizedNameLength("𐐀".repeat(11)), 11);
  const half = collectionFrame(100, -60, { travel: .875, scale: .2825, alpha: 1 });
  assert.equal(half.x, 87.5);
  assert.equal(half.y, -52.5);
  assert.ok(Math.abs(half.scale - .2825) < 1e-12);
  assert.equal(collectionFrame(100, -60, { travel: 1, scale: .18, alpha: 0 }).opacity, 0);
  return { expected: "Lungime NFC pe code point și convergență către câștigător", observed: { half, limit: 10 } };
});

await audit("Profil pe telefon · aceeași origine și stocare dezactivată", () => {
  const data = new Map();
  const storage = () => ({ getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value) });
  const first = deviceProfile("1".repeat(32), storage);
  first.remember({ savedName: true, name: "Bianca" });
  const reopened = deviceProfile("2".repeat(32), storage);
  reopened.remember({ savedName: false, name: "You" });
  assert.equal(reopened.id, first.id);
  assert.equal(reopened.name(), "Bianca");
  const isolated = deviceProfile("3".repeat(32), () => { throw new DOMException("Blocked", "SecurityError"); });
  isolated.remember({ savedName: true, name: "Ana" });
  assert.equal(isolated.id, "3".repeat(32));
  assert.equal(isolated.name(), null);
  return { expected: "Numele confirmat supraviețuiește filei; aplicația pornește când stocarea este refuzată",
    observed: { restoredName: reopened.name(), isolatedName: isolated.name() } };
});

await withServer(games[0], web, async url => {
  const request = client(url);
  await audit("Whist · salvarea profilului și refuzul editării nevalide", async () => {
    const profile = client(url, "d".repeat(32));
    const initial = await profile("/api/state");
    const accepted = "𐐀".repeat(10);
    const saved = await profile("/api/profile", { revision: initial.revision, name: accepted });
    await profile("/api/profile", { revision: saved.revision, name: accepted + "𐐀" }, 400);
    const current = await profile("/api/state");
    assert.equal(current.profile.name, accepted);
    assert.equal(current.revision, saved.revision);
    assert.ok(current.profile.catalogue.groups.length > 0);
    const second = client(url, "e".repeat(32), "d".repeat(32));
    assert.equal((await second("/api/state")).profile.name, accepted);
    assert.equal((await second("/api/state")).view, null);
    const firstMatch = await profile("/api/start", { revision: current.revision });
    assert.equal(firstMatch.view.seats[0].name, accepted);
    assert.equal((await second("/api/state")).view, null);
    assert.notEqual((await client(url, "f".repeat(32))("/api/state")).profile.name, accepted);
    return { expected: "10 code points validați; profil comun pe file, meci și profiluri independente",
      observed: { nameLength: normalizedNameLength(current.profile.name), groups: current.profile.catalogue.groups.length,
        secondTabMatch: (await second("/api/state")).view } };
  });
  await audit("Whist · revizie concurentă, refuz și stare fără animații reluate", async () => {
    await request("/api/state", null, 403, false);
    const fresh = await request("/api/state");
    assert.equal(fresh.revision, 0);
    assert.equal(fresh.view, null);
    const options = { revision: 0, players: 4, mode: "ROMANIAN_WHIST", difficulty: "HARD", name: "Ștefan" };
    const four = await request("/api/start", options);
    assert.deepEqual(four.view.seats.map(s => s.anchor), ["south", "north-west", "north", "north-east"]);
    assert.equal(four.view.hand.length, 1);
    assert.ok(four.view.seats.every(s => !("hand" in s)));
    assert.equal(four.view.seats.filter(s => s.active).length, 1);
    const bids = four.view.actions.filter(a => a.key.startsWith("bid:"));
    assert.equal(bids.filter(a => a.emphasized).length, 1);
    await request("/api/action", { revision: four.revision, action: "bid:100" }, 400);
    const valid = bids.find(a => !a.emphasized) ?? bids[0];
    assert.deepEqual(await request("/api/state"), four);
    const bid = { revision: four.revision, action: valid.key };
    const commands = await Promise.allSettled([request("/api/action", bid), request("/api/action", bid)]);
    const committed = commands.filter(command => command.status === "fulfilled");
    const refused = commands.filter(command => command.status === "rejected");
    assert.equal(committed.length, 1);
    assert.equal(refused.length, 1);
    assert.match(refused[0].reason.message, /returned 409/);
    const acted = committed[0].value;
    assert.equal(acted.revision, four.revision + 1);
    assert.equal(acted.view.hand.length, 1);
    assert.deepEqual(await request("/api/state"), { ...acted, collections: [], playback: [] });
    assert.deepEqual(await client(url, "b".repeat(32))("/api/state"), fresh);
    return { expected: "O singură licitație acceptată la aceeași revizie. Citirea păstrează starea și stinge evenimentele tranzitorii.",
      observed: { bidOptions: bids.map(b => b.key), committed: valid.key, revision: acted.revision, rejected: 409 } };
  });

  await audit("Whist · proiecția evantaiului pentru 17 cărți și refuzul dimensiunilor invalide", async () => {
    const layout = { mode: "DOWN_AND_UP", players: 3, width: 390, height: 430, viewport: 896,
      seatHeight: 64, bidOptions: 18, cards: 17, handWidth: 390 };
    const initial = await request("/api/state");
    const compact = await request("/api/layout", layout);
    const wide = await request("/api/layout", { ...layout, width: 760, handWidth: 760, bidOptions: 0 });
    const lastEdge = plan => plan.hand.first + 16 * plan.hand.advance + plan.hand.cardWidth;
    assert.ok(compact.hand.width > layout.handWidth);
    assert.ok(lastEdge(compact) <= compact.hand.width);
    assert.equal(wide.hand.width, 760);
    assert.ok(lastEdge(wide) <= wide.hand.width);
    await request("/api/layout", { ...layout, cards: 18 }, 400);
    await request("/api/layout", { ...layout, handWidth: 0 }, 400);
    assert.deepEqual(await request("/api/state"), initial);
    return { expected: "Ultima carte încape în evantai. Pe un ecran îngust evantaiul poate derula, iar cererile invalide nu schimbă jocul.",
      observed: { compact: compact.hand, wide: wide.hand, revision: initial.revision, invalidMeasurements: 400 } };
  });

  await audit("Whist · partidă integrală românească 1–8–1, patru locuri", async () => {
    const request = client(url, "c".repeat(32));
    let state = await request("/api/start", { revision: 0, players: 4, mode: "ROMANIAN_WHIST", difficulty: "NORMAL" });
    let notices = 0;
    for (let hand = 1; hand <= 24; hand++) {
      assert.equal(state.view.heading, "Mână " + hand);
      const expectedTricks = state.view.hand.length;
      state = await request("/api/action", { revision: state.revision, action: "auto" });
      assert.equal(state.collections.length, expectedTricks);
      for (const cue of state.collections) {
        assert.equal(cue.cards.length, 4);
        assert.equal(new Set(cue.cards.map(card => card.seat)).size, 4);
        assert.equal(cue.displayMillis, 1500);
        assert.equal(cue.travelMillis, 500);
      }
      assert.equal(state.view.history.rows.length, hand);
      assert.equal(state.view.history.rows.at(-1).length, 5);
      if (state.profile.notice) {
        const catalogue = state.profile.catalogue;
        assert.ok(state.profile.notice.goals.length > 0);
        state = await request("/api/notice", { revision: state.revision, dismiss: "yes" });
        assert.equal(state.profile.notice, null);
        assert.deepEqual(state.profile.catalogue, catalogue);
        notices++;
      }
      if (hand < 24) state = await request("/api/action", { revision: state.revision, action: "next" });
    }
    assert.equal(state.view.prompt, "Partidă încheiată");
    assert.deepEqual(state.view.actions.map(action => action.key), ["again"]);
    const settled = await request("/api/state");
    assert.deepEqual(settled.collections, []);
    assert.deepEqual(settled.playback, []);
    assert.ok(notices > 0);
    const replay = await request("/api/action", { revision: settled.revision, action: "again" });
    assert.equal(replay.view.heading, "Mână 1");
    assert.equal(replay.view.header.mode, "ROMANIAN_WHIST");
    assert.equal(replay.view.history.rows.length, 0);
    assert.deepEqual(replay.profile.catalogue, settled.profile.catalogue);
    completedCareer = settled.profile.catalogue.groups.flatMap(group => group.goals).filter(goal => goal.complete);
    return { expected: "24 de mâini, confirmarea notificărilor, citire fără animații vechi și reluare cu progres păstrat",
      observed: { hands: settled.view.history.rows.length, notices, finalRow: settled.view.history.rows.at(-1),
        earned: completedCareer.map(goal => goal.id), replay: replay.view.heading } };
  });

});

await withServer(games[0], web, async url => {
  await audit("Whist · profil și progres recuperate după repornirea serverului", async () => {
    const profile = await client(url, "d".repeat(32))("/api/state");
    const career = await client(url, "c".repeat(32))("/api/state");
    assert.equal(profile.profile.name, "𐐀".repeat(10));
    assert.ok(completedCareer?.length > 0, "The previous match must publish at least one achievement");
    const recovered = career.profile.catalogue.groups.flatMap(group => group.goals).filter(goal => goal.complete);
    assert.deepEqual(recovered, completedCareer);
    return { expected: "Date persistente în același profil de browser", observed: career.profile.catalogue.summary };
  });
});

await withServer(games[1], games[1].web, async url => {
  await audit("Rentz · dezvăluire, contract, levată și totaluri", async () => {
    const request = client(url);
    let state = await request("/api/start", { revision: 0, players: 4, difficulty: "NORMAL" });
    assert.ok(state.view.actions.some(action => action.key === "reveal"));
    state = await request("/api/action", { revision: state.revision, action: "reveal" });
    assert.ok(state.view.hand.length > 0);
    assert.ok(state.view.actions.some(action => action.key === "choose:TRICKS"));
    state = await request("/api/action", { revision: state.revision, action: "choose:TRICKS" });
    const firstCard = state.view.hand.find(card => card.action);
    assert.ok(firstCard, "Rentz has at least one legal card");
    state = await request("/api/action", { revision: state.revision, action: firstCard.action });
    let steps = 1;
    for (; steps < 30 && !state.view.history.grid?.rows.length; steps++) {
      const action = state.view.hand.find(card => card.action)?.action
        ?? state.view.actions.find(move => move.key === "auto-step")?.key;
      assert.ok(action, "Rentz must expose a legal card or an automatic step before scoring");
      state = await request("/api/action", { revision: state.revision, action });
    }
    const rows = state.view.history.grid?.rows;
    assert.equal(rows?.length, 1, "Exactly one hand must settle");
    assert.equal(rows[0].status, "Levate");
    assert.equal(rows[0].cells.length, 4);
    assert.ok(rows[0].cells.every(cell => cell.bid === cell.total), "First hand deltas must equal totals");
    return { expected: "Contractul Levate decontează o singură mână și publică totalul fiecărui jucător",
      observed: { steps, row: rows[0] } };
  });
});

await withServer(games[2], games[2].web, async url => {
  await audit("Hold’em · pariere încadrată de motor, showdown și mâna următoare", async () => {
    const request = client(url);
    let state = await request("/api/start", { revision: 0, players: 4, difficulty: "NORMAL", blinds: 3 });
    assert.equal(state.view.hand.length, 2);
    assert.equal(state.view.played.length, 0);
    const raise = state.view.actions.find(a => a.minimum !== null);
    assert.ok(raise, "The initial local actor can raise");
    await request("/api/action", { revision: state.revision, action: raise.key + "/" + (raise.maximum + 1) }, 400);
    state = await request("/api/action", { revision: state.revision, action: raise.key + "/" + raise.minimum });
    let steps = 0;
    while (state.view.actions.some(move => move.key === "auto") && steps++ < 12) {
      const passive = state.view.actions.find(action => ["CHECK", "CALL"].includes(action.key));
      assert.ok(passive, "There is a legal check/call");
      state = await request("/api/action", { revision: state.revision, action: passive.key });
    }
    assert.equal(state.view.prompt, "Mână încheiată");
    assert.equal(state.view.center.length, 5);
    const outcome = state.view.seats.map(seat => [seat.name, seat.secondFact]);
    const next = await request("/api/action", { revision: state.revision, action: "next" });
    assert.equal(next.view.heading, "Mâna 2");
    return { expected: "Limite de pariere, cinci cărți comune, decontare și mâna 2", observed: { steps, outcome } };
  });
});

await writeFile(join(output, "browser-checks.json"), JSON.stringify(results, null, 2) + "\n");
if (results.some(result => result.status === "FAIL")) process.exitCode = 1;

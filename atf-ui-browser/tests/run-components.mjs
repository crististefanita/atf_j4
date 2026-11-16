import assert from "node:assert/strict";
import { mkdir, readFile, unlink, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { isPhone, viewportGeometry } from "../src/main/web/viewport.mjs";
import { collectionFrame } from "../src/main/web/collection.mjs";
import { deviceProfile, normalizedNameLength } from "../src/main/web/profile.mjs";
import { client, until, withBrowser, withServer } from "./fixtures.mjs";

const [output, whist, rentz, holdem, edge, chrome] = process.argv.slice(2);
if (!output || !whist || !rentz || !holdem) {
  throw new Error("Expected evidence directory, Whist/Rentz/Hold’em classes, Edge and Chrome executables");
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

async function audit(name, check) {
  try {
    const evidence = await check();
    results.push({ name, status: "PASS", ...evidence });
  } catch (error) {
    results.push({ name, status: "FAIL", expected: "Assertions pass", observed: error.stack });
  }
  console.log(results.at(-1).status + " " + name);
}

await audit("Modele de viewport portrait și centrare", () => {
  assert.deepEqual(viewportGeometry(414, 896, true), { width: 414, height: 896, scale: 1, frameWidth: 414, frameHeight: 896 });
  const tall = viewportGeometry(1920, 1080, false);
  assert.equal(tall.frameHeight, 1080);
  assert.ok(Math.abs(tall.frameWidth - 414 * 1080 / 896) < 1e-9);
  assert.equal(viewportGeometry(320, 982, false).frameWidth, 320);
  assert.equal(viewportGeometry(220, 420, false).scale, 0.68);
  assert.throws(() => viewportGeometry(0, 896, true), RangeError);
  assert.ok(isPhone("Mozilla/5.0 (Linux; Android 14)"));
  assert.ok(!isPhone("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
  return { expected: "414×896 la 1:1, înălțime sau lățime limitantă, derulare sub prag", observed: tall };
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
  let four;
  await audit("Whist · stare privată, recomandare, refuzuri și reluare", async () => {
    await request("/api/state", null, 403, false);
    const fresh = await request("/api/state");
    assert.equal(fresh.revision, 0);
    assert.equal(fresh.view, null);
    const options = { revision: 0, players: 4, difficulty: "HARD", name: "Ștefan" };
    four = await request("/api/start", options);
    assert.deepEqual(four.view.seats.map(s => s.anchor), ["south", "north-west", "north", "north-east"]);
    assert.equal(four.view.hand.length, 1);
    assert.ok(four.view.seats.every(s => !("hand" in s)));
    assert.equal(four.view.seats.filter(s => s.active).length, 1);
    const bids = four.view.actions.filter(a => a.key.startsWith("bid:"));
    assert.equal(bids.filter(a => a.emphasized).length, 1);
    await request("/api/action", { revision: four.revision, action: "bid:100" }, 400);
    const valid = bids.find(a => !a.emphasized) ?? bids[0];
    const acted = await request("/api/action", { revision: four.revision, action: valid.key });
    await request("/api/action", { revision: four.revision, action: valid.key }, 409);
    assert.deepEqual(await request("/api/state"), acted);
    assert.deepEqual(await client(url, "b".repeat(32))("/api/state"), fresh);
    return { expected: "O recomandare legală, un actor, stare neschimbată la invalid sau duplicat",
      observed: { bidOptions: bids.map(b => b.key), committed: valid.key, revision: acted.revision } };
  });

  await audit("Whist · partidă integrală românească 1–8–1, patru locuri", async () => {
    const request = client(url, "c".repeat(32));
    let state = await request("/api/start", { revision: 0, players: 4, mode: "ROMANIAN_WHIST", difficulty: "NORMAL" });
    for (let hand = 1; hand <= 24; hand++) {
      assert.equal(state.view.heading, "Mâna " + hand);
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
      if (hand < 24) state = await request("/api/action", { revision: state.revision, action: "next" });
    }
    assert.equal(state.view.prompt, "Clasament final");
    assert.equal(state.view.actions.length, 0);
    const settled = await request("/api/state");
    assert.deepEqual(settled.collections, []);
    assert.ok(settled.profile.notice?.goals.length > 0);
    const acknowledged = await request("/api/notice", { revision: settled.revision, dismiss: "yes" });
    assert.equal(acknowledged.profile.notice, null);
    assert.deepEqual(acknowledged.profile.catalogue, settled.profile.catalogue);
    return { expected: "24 mâini, notificări confirmate fără dublare, animație nerepetată la reîncărcare",
      observed: { history: state.view.history.rows, earned: settled.profile.notice.goals.map(goal => goal.id) } };
  });

  for (const [name, executable] of [["Edge", edge], ["Chrome", chrome]]) {
    const title = name + " · Whist 414×896, mână de 17 cărți și scalare Windows";
    if (!executable) {
      results.push({ name: title, status: "SKIP", expected: "Executabil real", observed: "Executabil nespecificat" });
      continue;
    }
    await audit(title, () => withBrowser(executable, join(output, name + "-profile"), browser => ui(browser, url, name)));
  }
});

await withServer(games[0], web, async url => {
  await audit("Whist · profil și progres recuperate după repornirea serverului", async () => {
    const profile = await client(url, "d".repeat(32))("/api/state");
    const career = await client(url, "c".repeat(32))("/api/state");
    assert.equal(profile.profile.name, "𐐀".repeat(10));
    assert.ok(career.profile.catalogue.groups.flatMap(group => group.goals).some(goal => goal.complete));
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
    const legal = state.view.hand.find(card => card.action);
    assert.ok(legal, "Rentz has at least one legal card");
    state = await request("/api/action", { revision: state.revision, action: legal.action });
    if (state.view.history.rows.length === 0) {
      state = await request("/api/action", { revision: state.revision, action: "auto" });
    }
    assert.equal(state.view.history.rows.length, 1);
    assert.equal(state.view.history.rows[0][1], "Levate");
    return { expected: "Contractul Levate decontează o singură mână", observed: state.view.history.rows };
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

async function ui({ send, evaluate, version }, url, name) {
  const metrics = (width, height, mobile) => send("Emulation.setDeviceMetricsOverride", {
    width, height, deviceScaleFactor: 1, mobile, screenWidth: width, screenHeight: height
  });
  const measure = () => evaluate(`(() => {
    const rect = el => { const { x, y, width, height } = el.getBoundingClientRect(); return {x,y,width,height}; };
    const hand = document.querySelector(".hand");
    return { frame: rect(document.querySelector("#frame")), seats: [...document.querySelectorAll(".seat")].map(rect),
      hand: hand && { cards: hand.querySelectorAll(".fan .card").length, viewport: hand.clientWidth, total: hand.scrollWidth },
      mode: document.querySelector("#frame").dataset.viewport };
  })()`);
  const images = [];
  async function ready(expression, label, limit = 20000) {
    try {
      return await until(() => evaluate(expression), limit);
    } catch (failure) {
      const text = await evaluate("document.body?.innerText?.slice(0, 550)").catch(() => "");
      throw new Error(label + ": " + text, { cause: failure });
    }
  }
  async function snapshot(label) {
    const file = name + "-" + images.length + ".png";
    const png = await send("Page.captureScreenshot", { format: "png", fromSurface: true });
    await writeFile(join(output, file), Buffer.from(png.data, "base64"));
    images.push({ file, title: name + " · " + label });
  }
  await send("Page.enable");
  const desktopAgent = await evaluate("navigator.userAgent");
  await send("Emulation.setUserAgentOverride", { userAgent: "Mozilla/5.0 (Linux; Android 14; Mobile)" });
  await send("Emulation.setTouchEmulationEnabled", { enabled: true, maxTouchPoints: 1 });
  await metrics(414, 896, true);
  const page = await send("Page.navigate", { url: url.href });
  assert.ok(!page.errorText, page.errorText);
  await ready("!!document.querySelector('.whist-home')", "Meniul nu a apărut");
  {
    const clickText = text => evaluate(`[...document.querySelectorAll("button")].find(
      button => button.textContent.startsWith(${JSON.stringify(text)}))?.click()`);
    await clickText("Numele tău:");
    await ready("!!document.querySelector('.player-dialog[open] input')", "Profilul nu a apărut");
    await evaluate("document.querySelector('.player-dialog[open] input').value = 'Bianca'");
    await clickText("Anulează");
    await clickText("Numele tău:");
    assert.equal(await evaluate("document.querySelector('.player-dialog[open] input').value"), "You");
    await evaluate(`(() => {
      const form = document.querySelector(".player-dialog[open] form");
      form.querySelector('input').value = "Bianca";
      form.requestSubmit();
    })()`);
    await ready(`!document.querySelector('.player-dialog[open]') &&
      [...document.querySelectorAll('.whist-home button')].some(button => button.textContent === 'Numele tău: Bianca')`,
      "Profilul salvat nu a revenit în meniu");
    assert.equal(await evaluate("localStorage.getItem('table-profile-name')"), "Bianca");
    const firstTab = await evaluate("sessionStorage.getItem('table-client')");
    await evaluate("sessionStorage.removeItem('table-client')");
    await send("Page.reload");
    await ready(`[...document.querySelectorAll('.whist-home button')].some(button => button.textContent === 'Numele tău: Bianca')`,
      "Fila reinițializată nu a refolosit profilul de pe telefon");
    assert.notEqual(await evaluate("sessionStorage.getItem('table-client')"), firstTab);
    const deviceId = await evaluate("localStorage.getItem('table-profile-id')");
    assert.match(deviceId, /^[0-9a-f]{32}$/);
    const serverName = join(output, "component-profile-state", deviceId + "-name.txt");
    await unlink(serverName);
    await send("Page.reload");
    await until(async () => (await readFile(serverName, "utf-8")) === "Bianca");
    await ready(`document.querySelector('#table')?.getAttribute('aria-busy') === 'false' &&
      [...document.querySelectorAll('.whist-home button')].some(button => button.textContent === 'Numele tău: Bianca')`,
      "Browserul nu a reconstruit numele local");
    await clickText("Numele tău: Bianca");
    await snapshot("profil salvat");
    await clickText("Resetează numele");
    await ready(`!document.querySelector('.player-dialog[open]') &&
      [...document.querySelectorAll('.whist-home button')].some(button => button.textContent === 'Numele tău: You')`,
      "Resetarea profilului nu a revenit în meniu");
    assert.equal(await evaluate("localStorage.getItem('table-profile-name')"), "You");
    await clickText("Realizări");
    await ready("!!document.querySelector('.player-dialog[open] details')", "Catalogul nu a apărut");
    const catalogue = await evaluate(`(() => {
      const panel = document.querySelector('.achievement-catalogue');
      const { width, height } = panel.getBoundingClientRect();
      return { width, height, total: panel.querySelector('progress').max, rows: panel.querySelectorAll('details').length };
    })()`);
    assert.deepEqual(catalogue, { width: 414, height: 896, total: 55, rows: 55 });
    await evaluate("document.querySelector('.player-dialog[open] details summary').click()");
    assert.ok(await evaluate("!!document.querySelector('.player-dialog[open] details[open] p')"));
    await snapshot("catalog și detaliu");
    await evaluate("document.querySelector('.player-dialog[open]').close()");
  }
  await evaluate(`(() => {
      document.querySelector('.whist-home .primary').click();
      document.querySelector('[name=players]').value = "3";
      document.querySelector('[name=mode]').value = "DOWN_AND_UP";
      document.querySelector('.setup form').requestSubmit();
  })()`);
  await ready("!!document.querySelector('[data-bid][aria-pressed=true]') && !!document.querySelector('[data-layout=ready]')",
    "Licitarea nu a apărut");
  const portrait = await measure();
  assert.equal(portrait.mode, "phone");
  assert.deepEqual(portrait.frame, { x: 0, y: 0, width: 414, height: 896 });
  assert.equal(portrait.hand.cards, 17);
  assert.ok(portrait.hand.total > portrait.hand.viewport);
  const controls = await evaluate(`(() => {
    const table = document.querySelector("#table").getBoundingClientRect();
    const within = selector => {
      const rect = document.querySelector(selector).getBoundingClientRect();
      return rect.left >= table.left && rect.right <= table.right;
    };
    const bid = [...document.querySelectorAll('.actions button')].find(button => button.textContent === 'Licitează');
    return { prompt: within(".prompt"), actions: within(".actions"), button: bid.getBoundingClientRect().right <= table.right };
  })()`);
  assert.deepEqual(controls, { prompt: true, actions: true, button: true });
  const lastCard = await evaluate(`(() => {
    const hand = document.querySelector(".hand");
    hand.scrollLeft = hand.scrollWidth;
    const last = hand.querySelector(".fan").lastElementChild.getBoundingClientRect();
    return last.right <= hand.getBoundingClientRect().right + 1;
  })()`);
  assert.equal(lastCard, true);
  await evaluate("document.querySelector('.hand').scrollLeft = 0");
  await snapshot("414×896, licitare");
  await send("Page.bringToFront");
  assert.equal(await evaluate(`(() => {
    const button = [...document.querySelectorAll('.actions button')].find(b => b.textContent === 'Licitează');
    button.focus();
    return document.activeElement === button && !button.disabled;
  })()`), true);
  const space = { key: " ", code: "Space", windowsVirtualKeyCode: 32, nativeVirtualKeyCode: 32 };
  await send("Input.dispatchKeyEvent", { ...space, type: "keyDown", text: " ", unmodifiedText: " " });
  await send("Input.dispatchKeyEvent", { ...space, type: "keyUp" });
  await ready("!!document.querySelector('.hand button:enabled') && !!document.querySelector('[data-layout=ready]')",
    "Jocul nu a urmat după licitarea confirmată de la tastatură");
  const play = await measure();
  assert.deepEqual(play.seats, portrait.seats);
  await snapshot("414×896, joc");
  await send("Emulation.setUserAgentOverride", { userAgent: desktopAgent });
  const desktop = [];
  for (const [width, height] of [[1280, 720], [1920, 1080]]) {
    await metrics(width, height, false);
    const state = await until(async () => {
      const next = await measure();
      return Math.abs(next.frame.height - height) < 1 && next.frame.x > 0 ? next : null;
    });
    const expectedWidth = 414 * height / 896;
    assert.equal(state.mode, "desktop");
    assert.ok(Math.abs(state.frame.width - expectedWidth) < 1);
    assert.ok(Math.abs(state.frame.x - (width - expectedWidth) / 2) < 1);
    assert.ok(Math.abs(state.frame.y) < 1);
    desktop.push(state.frame);
    if (height === 1080) await snapshot("1920×1080, masă centrată");
  }
  if (name === "Edge") {
    await send("Emulation.setUserAgentOverride", { userAgent: "Mozilla/5.0 (Linux; Android 14; Mobile)" });
    await metrics(414, 896, true);
    await ready("document.querySelector('#frame').clientWidth === 414", "Revenirea la cadrul mobil nu s-a încheiat");
    await evaluate("document.querySelector('.whist-auto').click()");
    for (const step of [1, 2]) {
      await ready(`(() => {
        const stage = document.querySelector("#table[data-collection-step='${step}']");
        const elapsed = stage?.querySelector(".collection .card")?.getAnimations()[0]?.currentTime;
        return elapsed >= 1050 && elapsed < 1450;
      })()`, "Cadrele levatei nu au intrat în intervalul de deplasare");
      await snapshot("levata " + step + " din AutoGame în deplasare");
    }
    await ready("document.querySelector('#table').getAttribute('aria-busy') === 'false'",
      "Șirul de colectări nu s-a încheiat", 65000);
  }
  return { expected: "Afișare mobilă 1:1, 17 cărți accesibile, ancore fixe și scalare uniformă după înălțime",
    observed: { version, portrait, playedSeats: play.seats, desktop }, images };
}

import assert from "node:assert/strict";
import { readFile, readdir, mkdir, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { until, withBrowser } from "../tests/fixtures.mjs";

const [input, output, browserOrSelection, option] = process.argv.slice(2);
const executable = browserOrSelection?.startsWith("--") ? undefined : browserOrSelection;
const selection = option ?? (executable ? undefined : browserOrSelection);
if (!input || !output) throw new Error("Expected site and external evidence directories");
if (selection && !["--ui-only", "--browser-only", "--contract-only"].includes(selection)) {
  throw new Error("Unknown verification selection: " + selection);
}
const directory = resolve(input);
await mkdir(output, { recursive: true });
const checks = [];
async function check(name, expected, exercise) {
  try {
    checks.push({ name, expected, status: "PASS", ...await exercise() });
  } catch (error) {
    checks.push({ name, expected, status: "FAIL", observed: error.stack });
  }
  console.log(checks.at(-1).status + " " + name);
}

const files = [".nojekyll", "auto-game.mjs", "collection.mjs", "engine-worker.mjs", "hand-feedback.mjs", "index.html", "local-transport.mjs",
  "profile.mjs", "records.mjs", "table.css", "table.mjs", "viewport.mjs", "whist-engine.mjs"];

if (!selection) await check("Pages · inventarul public", "Doar resurse statice și legături relative în subdirectorul proiectului", async () => {
  assert.deepEqual((await readdir(directory)).sort(), files.toSorted());
  const markup = await readFile(join(directory, "index.html"), "utf8");
  assert.match(markup, /data-engine="whist"/);
  assert.match(markup, /connect-src 'none'/);
  assert.match(markup, /src="\.\/table\.mjs"/);
  assert.match(markup, /href="\.\/table\.css"/);
  assert.ok((await readFile(join(directory, "whist-engine.mjs"))).length < 2_000_000);
  return { observed: files };
});
if (!selection) await check("Pages · motorul compilat și profilul", "Partidă românească integrală, mutări legale, scor și profil restaurat", async () => {
  const { initialize, command } = await import(pathToFileURL(join(directory, "whist-engine.mjs")));
  initialize(31, 9);
  let name = null;
  let progress = null;
  function play(path, values = {}, status) {
    const body = JSON.stringify(Object.fromEntries(Object.entries(values).map(([key, value]) => [key, String(value)])));
    const reply = JSON.parse(command("/api/" + path, body, name, progress));
    assert.equal(reply.status, status, reply.error);
    if (status) return;
    ({ name, progress } = reply);
    return reply.result;
  }
  assert.equal(play("setup").title, "Whist Lab");
  let state = play("state");
  const chosen = "S\u0326tefan";
  play("profile", { revision: state.revision, name: "𐐀".repeat(11) }, 400);
  state = play("profile", { revision: state.revision, name: chosen });
  assert.equal(state.profile.name, "Ștefan");
  play("start", { revision: state.revision - 1 }, 409);
  play("start", { revision: state.revision, mode: "unknown" }, 400);
  state = play("start", { revision: state.revision, players: 4, difficulty: "NORMAL", mode: "ROMANIAN_WHIST" });
  assert.equal(state.view.seats[0].name, "Ștefan");
  const suggested = state.view.actions.find(action => action.emphasized);
  assert.ok(suggested?.key.startsWith("bid:"));
  state = play("action", { revision: state.revision, action: suggested.key });
  const valid = state.view.hand.find(card => card.action);
  assert.ok(valid);
  state = play("action", { revision: state.revision, action: valid.action });
  let animated = state.collections.length;
  for (let hand = 1; hand <= 24; hand++) {
    if (state.view.history.rows.length < hand) state = play("action", { revision: state.revision, action: "auto" });
    assert.equal(state.view.history.rows.length, hand);
    assert.equal(state.view.history.rows.at(-1).length, 5);
    animated += state.collections.length;
    if (hand < 24) state = play("action", { revision: state.revision, action: "next" });
  }
  assert.equal(state.view.prompt, "Partidă încheiată");
  assert.ok(animated > 80);
  assert.ok(state.profile.notice?.goals.length);
  const before = state.profile.catalogue;
  assert.ok(progress?.length);
  initialize(82, 22);
  const reopened = play("state");
  assert.equal(reopened.view, null);
  assert.equal(reopened.profile.name, "Ștefan");
  assert.deepEqual(reopened.profile.catalogue, before);
  return { observed: { hands: state.view.history.rows.length, animated, name: reopened.profile.name,
    catalogue: reopened.profile.catalogue.summary } };
});

if (selection !== "--contract-only" && selection !== "--browser-only") await check("Pages · geometrie și ordinea cărților", "Centre rezervate, ordine reală, întârziere nativă și levată completă", async () => {
  const { initialize, command } = await import(pathToFileURL(join(directory, "whist-engine.mjs")));
  initialize(41, 28);
  function invoke(path, fields) {
    const payload = Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, String(value)]));
    const reply = JSON.parse(command("/api/" + path, JSON.stringify(payload), null, null));
    assert.equal(reply.status, undefined, reply.error);
    return reply.result;
  }
  const reservations = [];
  for (const players of [3, 4, 5, 6]) {
    const width = players === 3 ? 320 : 374;
    const mode = players === 3 ? "DOWN_AND_UP" : "ROMANIAN_WHIST";
    const layout = invoke("layout", { width, height: 350, mode, players, seatHeight: 67, viewport: 680, cards: 8, handWidth: width });
    assert.equal(layout.board.seats.length, players);
    assert.equal(layout.played.slots.length, players);
    assert.ok(layout.played.centerHeightDp <= layout.board.center.bottom - layout.board.center.top);
    if (players === 4) assert.deepEqual(layout.played.slots.map(slot => slot.row), [2, 1, 0, 1]);
    for (const seat of layout.board.seats) {
      const center = layout.board.center;
      assert.ok(seat.bottom <= center.top || seat.top >= center.bottom ||
        seat.right <= center.left || seat.left >= center.right);
      assert.ok(seat.top >= 0 && seat.bottom <= layout.boardHeight);
    }
    reservations.push(layout.board.center.bottom - layout.board.center.top);
  }
  const match = invoke("start", { revision: 0, players: 5, mode: "ROMANIAN_WHIST" });
  const settled = invoke("action", { revision: match.revision, action: "auto" });
  const steps = settled.playback;
  assert.equal(steps.length, 5);
  assert.equal(steps[0].startsTrick, true);
  assert.equal(steps.filter(step => step.startsTrick).length, 1);
  assert.deepEqual(steps.map(step => step.delayMillis), Array(5).fill(550));
  assert.ok(steps.slice(0, -1).every(step => step.collection === null));
  assert.deepEqual(steps.at(-1).collection.cards, steps.map(step => step.play));
  assert.equal(steps.at(-1).collection.travelMillis, 500);
  assert.equal(steps.at(-1).collection.displayMillis, 1500);
  assert.equal(settled.view.score.table.rows.length, 5);
  assert.deepEqual(invoke("state", {}).playback, []);
  return { observed: { reservations, seatOrder: steps.map(step => step.play.seat), delay: steps[0].delayMillis } };
});

if (!selection || selection === "--contract-only") await check(
  "Contract comun · fazele mesei și cadrele de colectare",
  "Aceleași ancore la licitare și joc. Browserul aplică mostrele Java pentru deplasare, scară și opacitate.",
  async () => {
    const { initialize, command } = await import(pathToFileURL(join(directory, "whist-engine.mjs")));
    const { collectionFrame } = await import(pathToFileURL(join(directory, "collection.mjs")));
    initialize(73, 11);
    function read(path, values = {}) {
      const fields = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, String(value)]));
      const reply = JSON.parse(command("/api/" + path, JSON.stringify(fields), null, null));
      assert.equal(reply.status, undefined, reply.error);
      return reply.result;
    }
    const profiles = [];
    for (const players of [3, 4, 5, 6]) {
      const width = players === 3 ? 320 : 374;
      const input = { width, height: 280, players, seatHeight: 65, viewport: players === 3 ? 680 : 896,
        cards: players === 3 ? 17 : 8, handWidth: width, mode: players === 3 ? "DOWN_AND_UP" : "ROMANIAN_WHIST" };
      const bidding = read("layout", { ...input, bidOptions: players === 3 ? 18 : 9 });
      const playing = read("layout", { ...input, bidOptions: 0 });
      assert.deepEqual(bidding.board.seats, playing.board.seats);
      assert.deepEqual(bidding.played.slots, playing.played.slots);
      assert.equal(bidding.boardHeight, playing.boardHeight);
      assert.equal(playing.board.center.right, width);
      assert.ok(bidding.board.center.right <= width);
      assert.ok(bidding.board.center.left >= 0);
      profiles.push({ players, board: playing.boardHeight,
        bidWidth: bidding.board.center.right - bidding.board.center.left, playWidth: width });
    }
    assert.ok(profiles.some(profile => profile.bidWidth < profile.playWidth));
    const { collectionSamples: samples } = read("setup");
    assert.equal(samples.length, 11);
    for (const [index, sample] of samples.entries()) {
      assert.ok(Math.abs(sample.offset - index / 10) < 0.000001);
      assert.ok(sample.travel >= (samples[index - 1]?.travel ?? 0));
    }
    const frames = samples.map(sample => collectionFrame(100, -60, sample));
    assert.deepEqual(frames[0], { x: 0, y: -0, scale: 1, opacity: 1 });
    assert.equal(frames[5].x, 87.5);
    assert.equal(frames[5].y, -52.5);
    assert.ok(Math.abs(frames[5].scale - 0.2825) < 0.000001);
    assert.ok(Math.abs(frames[9].opacity - 0.5) < 0.000001);
    assert.equal(frames[10].opacity, 0);
    assert.equal(frames[10].x, 100);
    assert.equal(frames[10].y, -60);
    return { observed: { profiles, first: frames[0], middle: frames[5], last: frames[10] } };
  });

if (executable && selection !== "--contract-only") {
  await check("Pages · browser mobil la 414×896", "Joacă local din subdirector, închide pagina și recuperează profilul",
    () => phoneViewport(executable));
}
await writeFile(join(output, "whist-pages-checks.json"), JSON.stringify(checks, null, 2) + "\n");
if (checks.some(test => test.status === "FAIL")) process.exitCode = 1;

async function phoneViewport(chrome) {
  const requests = [];
  const server = createServer(async (request, response) => {
    const path = request.url;
    requests.push(path);
    const file = path === "/atf_j4/" ? "index.html" : path.slice("/atf_j4/".length);
    if (!path.startsWith("/atf_j4/") || !files.includes(file)) {
      response.writeHead(404).end();
      return;
    }
    const mime = file.endsWith(".mjs") ? "text/javascript" : file.endsWith(".css") ? "text/css" : "text/html";
    response.writeHead(200, { "content-type": mime + "; charset=utf-8" }).end(await readFile(join(directory, file)));
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try {
    return await withBrowser(chrome, join(output, "chrome-profile"), async ({ send, evaluate, version }) => {
      await send("Page.enable");
      await send("Emulation.setDeviceMetricsOverride", { width: 414, height: 896, mobile: true, deviceScaleFactor: 1 });
      await send("Emulation.setUserAgentOverride", { userAgent: "Mozilla/5.0 (Linux; Android 14; Mobile)" });
      const url = "http://127.0.0.1:" + server.address().port + "/atf_j4/";
      async function open() {
        assert.ok(!(await send("Page.navigate", { url })).errorText);
        await until(() => evaluate("!!document.querySelector('.whist-home')"));
        assert.equal(await evaluate("document.querySelector('#frame').dataset.viewport"), "phone");
      }
      const profileButton = `[...document.querySelectorAll('.whist-home button')].find(button => button.textContent.startsWith('Numele tău:'))`;
      const click = text => evaluate(`[...document.querySelectorAll('#table button')].find(button => button.textContent === ${JSON.stringify(text)}).click()`);
      const seats = () => evaluate(`[...document.querySelectorAll('.seat')].map(seat => {
        const rect = seat.getBoundingClientRect();
        return [rect.x, rect.y, rect.width, rect.height].map(Math.round);
      })`);
      await open();
      assert.equal(await evaluate("document.querySelector('.whist-home h1').textContent"), "Whist");
      assert.equal(await evaluate("getComputedStyle(document.querySelector('#table')).backgroundColor"), "rgb(246, 245, 237)");
      await evaluate(profileButton + ".click()");
      await evaluate(`(() => {
        const form = document.querySelector('.profile-form');
        form.elements.name.value = 'Bianca';
        form.requestSubmit();
      })()`);
      await until(() => evaluate(profileButton + "?.textContent === 'Numele tău: Bianca'"));
      await click("Joc nou");
      assert.equal(await evaluate("document.querySelector('.setup h1').textContent"), "Setări joc");
      assert.equal(await evaluate("document.querySelector('[name=zero]').type"), "checkbox");
      await evaluate(`(() => {
        const mode = document.querySelector('[name=mode]');
        mode.value = "OH_HELL";
        mode.dispatchEvent(new Event("change", { bubbles: true }));
      })()`);
      assert.equal(await evaluate("document.querySelector('.series-row').hidden"), true);
      await evaluate(`(() => {
        const mode = document.querySelector('[name=mode]');
        mode.value = "ROMANIAN_WHIST";
        mode.dispatchEvent(new Event("change", { bubbles: true }));
      })()`);
      assert.equal(await evaluate("document.querySelector('.series-row').hidden"), false);
      await evaluate("document.querySelector('.setup form').requestSubmit()");
      await until(() => evaluate("!!document.querySelector('.board[data-layout=ready]')"));
      assert.equal(await evaluate("document.querySelector('.whist-title-row h1').textContent"), "Whist · Românesc");
      assert.equal(await evaluate("getComputedStyle(document.querySelector('.whist-title-row h1')).fontSize"), "14px");
      assert.ok(await evaluate(`document.querySelector(".whist-header").clientWidth ===
        document.querySelector(".whist-title-row").clientWidth`));
      assert.equal(await evaluate("document.querySelector('.whist-trump .card').tagName"), "SPAN");
      const bidding = await seats();
      await evaluate(`(() => {
        window.cardFrames = [];
        new MutationObserver(changes => {
          for (const change of changes) {
            if (!change.target.matches?.(".cards-on-table") || !change.target.isConnected) continue;
            for (const node of change.addedNodes) {
              if (!node.matches?.(".played-card")) continue;
              const active = [...document.querySelectorAll(".seat")].findIndex(seat => seat.getAttribute("aria-current") === "true");
              window.cardFrames.push({ seat: Number(node.dataset.seat), active, at: performance.now(),
                bids: document.querySelector(".whist-bids").textContent.split("\\n")[0] });
            }
          }
        }).observe(document.querySelector("#table"), { childList: true, subtree: true });
      })()`);
      await click("Licitează");
      await until(() => evaluate("!!document.querySelector('.board[data-layout=ready]') && !document.querySelector('[data-bid]')"));
      assert.deepEqual(await seats(), bidding);
      const board = await send("Page.captureScreenshot", { format: "png" });
      await writeFile(join(output, "pages-mobile-board.png"), Buffer.from(board.data, "base64"));
      await evaluate("document.querySelector('.whist-auto input').click()");
      await until(() => evaluate("!!document.querySelector('.whist-score tbody tr')"));
      const frames = await evaluate("window.cardFrames");
      assert.equal(frames.length, 4);
      assert.equal(new Set(frames.map(frame => frame.seat)).size, 4);
      assert.deepEqual(frames.map(frame => frame.active), frames.map(frame => frame.seat));
      assert.ok(frames.every(frame => frame.bids === "Licitații depuse 4/4"));
      assert.ok(frames.slice(1).every((frame, index) => frame.at - frames[index].at >= 500));
      assert.deepEqual(await evaluate(`[document.querySelector('.whist-score h1').textContent,
        document.querySelectorAll('.whist-score tbody tr').length,
        document.querySelectorAll('.whist-score thead th').length]`), ["Scorul partidei", 4, 5]);
      const noticeColor = await evaluate(`(() => {
        const close = document.querySelector(".achievement-notice[open] > button");
        return close && getComputedStyle(close).color;
      })()`);
      if (noticeColor !== null) assert.equal(noticeColor, "rgb(29, 54, 43)");
      await evaluate("document.querySelector('.player-dialog[open]')?.close()");
      const score = await send("Page.captureScreenshot", { format: "png" });
      await writeFile(join(output, "pages-mobile-score.png"), Buffer.from(score.data, "base64"));
      await send("Page.navigate", { url: "about:blank" });
      await until(() => evaluate("location.href === 'about:blank'"));
      await open();
      await until(() => evaluate(profileButton + "?.textContent === 'Numele tău: Bianca'"));
      assert.ok(!requests.some(path => path.startsWith("/api/")));
      const reopened = await send("Page.captureScreenshot", { format: "png" });
      await writeFile(join(output, "pages-mobile-profile.png"), Buffer.from(reopened.data, "base64"));
      return { observed: { version, viewport: "414×896", profile: "Bianca", frames, requests: [...new Set(requests)] },
        images: [{ file: "pages-mobile-board.png", title: "Partidă pornită în browser" },
          { file: "pages-mobile-score.png", title: "Scorul mâinii" },
          { file: "pages-mobile-profile.png", title: "Profil recuperat după redeschidere" }] };
    });
  } finally {
    server.close();
  }
}

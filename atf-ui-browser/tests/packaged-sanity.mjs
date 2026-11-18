import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { client, until, withBrowser, withServer } from "./fixtures.mjs";
import { verifyTrumpTextFit } from "./trump-text-fit.mjs";
import { verifyHandPresentation } from "./hand-presentation.mjs";
import { verifyGameMenuDismissal } from "./menu-presentation.mjs";
import { verifyViewportContract } from "./viewport-contract.mjs";
import { verifyCardTypography } from "./card-typography.mjs";

async function loadAdapter(url, name) {
  const response = await fetch(new URL(name, url));
  assert.equal(response.status, 200, name);
  return import("data:text/javascript;base64," + Buffer.from(await response.arrayBuffer()).toString("base64"));
}

async function verifyMotion(url, setup) {
  const { bindHandFeedback } = await loadAdapter(url, "hand-feedback.mjs");
  const { collectionFrame } = await loadAdapter(url, "collection.mjs");
  const cards = { first: { face: "8♦", command: "first" }, second: { face: "10♦", command: "second" },
    wrong: { face: "8♣", command: "" } };
  const lift = await verifyHandPresentation(bindHandFeedback, setup.handMotion, cards, ({ card }) => {
    const wrong = card === cards.wrong.face;
    return { action: wrong ? "HINT" : "PLAY", rejected: wrong ? card : "", selected: !wrong, revision: 1,
      suggestions: wrong ? [cards.first.face, cards.second.face] : [] };
  }, command => assert.equal(command, cards.second.command));
  const frames = setup.collectionSamples;
  assert.equal(frames.length, 11);
  assert.deepEqual(collectionFrame(20, -40, frames[0]), { x: 0, y: -0, scale: 1, opacity: 1 });
  const target = collectionFrame(20, -40, frames.at(-1));
  assert.equal(target.x, 20);
  assert.equal(target.y, -40);
  assert.equal(target.opacity, 0);
  return { lift, collection: { first: frames[0], end: target } };
}

async function remeasureSeatFrames() {
  const board = document.querySelector("#table > .match:not([data-preparing]) .board[data-layout=ready]");
  if (!board) throw new Error("The adapter has no visible measured board");
  const seatFrames = () => [...board.querySelectorAll(".seat")].map(node => {
    const { x, y, width, height } = node.getBoundingClientRect();
    return { x, y, width, height };
  });
  const before = seatFrames();
  await new Promise((resolve, reject) => {
    const observer = new MutationObserver(() => {
      clearTimeout(timeout);
      observer.disconnect();
      resolve();
    });
    const timeout = setTimeout(() => {
      observer.disconnect();
      reject(new Error("The adapter did not finish the layout request"));
    }, 5000);
    observer.observe(board, { attributes: true, attributeFilter: ["data-layout"] });
    window.dispatchEvent(new Event("resize"));
  });
  return { before, after: seatFrames(), boardRetained: board.isConnected && board.dataset.layout === "ready" };
}

const [output, classes, main, title, executable] = process.argv.slice(2);
if (!output || !classes || !main || !title) {
  throw new Error("Expected evidence directory, adapter classpath, main class, title and optional browser");
}
await mkdir(output, { recursive: true });
const images = [];
const expected = "Resurse din JAR, API protejat și partidă pornită prin interfața comună";
let result;
try {
  result = await withServer({ classes, main }, "-", async url => {
    const page = await fetch(url);
    assert.equal(page.status, 200);
    const markup = await page.text();
    const revision = markup.match(/\.\/table\.mjs\?v=([\d.]+)/)?.[1];
    assert.ok(revision, "Versioned script URL is missing");
    assert.ok(markup.includes(`./table.css?v=${revision}`), "Versioned stylesheet must match the script");
    for (const file of ["table.mjs", "viewport.mjs", "profile.mjs", "auto-game.mjs", "collection.mjs",
      "hand-feedback.mjs", "table.css"]) {
      const asset = await fetch(new URL(file, url));
      assert.equal(asset.status, 200, file);
      assert.ok((await asset.text()).length > 0, file);
    }
    const font = await fetch(new URL("card-suits.woff2", url));
    assert.equal(font.status, 200);
    assert.equal(font.headers.get("content-type"), "font/woff2");
    assert.equal(Buffer.from(await font.arrayBuffer()).subarray(0, 4).toString("ascii"), "wOF2");
    const request = client(url);
    await request("/api/setup", undefined, 403, false);
    const setup = await request("/api/setup");
    assert.equal(setup.title, title);
    await verifyCardTypography(setup);
    const motion = await verifyMotion(url, setup);
    const { fitTextWidth } = await loadAdapter(url, "viewport.mjs");
    const viewportModule = await loadAdapter(url, "viewport.mjs");
    const viewport = verifyViewportContract(viewportModule);
    const trumpText = await verifyTrumpTextFit(fitTextWidth);
    const before = await request("/api/state");
    const started = await request("/api/start", { revision: before.revision });
    assert.ok(started.view.seats.length >= 2);
    await request("/api/start", { revision: before.revision }, 409);
    const hiddenHand = started.view.actions.find(action => action.key === "reveal");
    const playable = hiddenHand
      ? await request("/api/action", { revision: started.revision, action: hiddenHand.key }) : started;
    assert.ok(playable.view.hand.length > 0);
    const first = playable.view.hand[0];
    const tap = { operation: "tap", card: first.rank + first.suit };
    await request("/api/hand-feedback", { ...tap, revision: playable.revision - 1 }, 409);
    const feedback = await request("/api/hand-feedback", { ...tap, revision: playable.revision },
      playable.view.header ? 200 : 400);
    if (playable.view.header) {
      const expectedAction = first.action ? "PLAY" : playable.view.hand.some(card => card.action) ? "HINT" : "IGNORE";
      assert.equal(feedback.action, expectedAction);
    } else {
      assert.ok(feedback.error);
    }
    assert.equal((await request("/api/state")).revision, playable.revision);
    let browserCheck = null;
    if (executable) {
      browserCheck = await withBrowser(executable, join(output, "sanity-browser"), async browser => {
        const { send, evaluate, version } = browser;
        await send("Page.enable");
        await send("Emulation.setDeviceMetricsOverride", { width: 414, height: 896, deviceScaleFactor: 1, mobile: true });
        await send("Emulation.setUserAgentOverride", { userAgent: "Mozilla/5.0 (Linux; Android 14; Mobile)" });
        assert.ok(!(await send("Page.navigate", { url: url.href })).errorText);
        await until(() => evaluate("!!document.querySelector('.setup form, .whist-home button.primary')"));
        await evaluate("document.querySelector('.whist-home button.primary')?.click()");
        await until(() => evaluate("!!document.querySelector('.setup form')"));
        assert.equal(await evaluate("document.title"), title);
        const menu = await evaluate(`(async () =>
          (${verifyGameMenuDismissal})((await import("./profile.mjs?v=${revision}")).dismissGameMenuOnOutsideClick))()`);
        const dropdowns = await evaluate(`[...document.querySelectorAll(".setup select")].map(select => ({
          name: select.name,
          font: parseFloat(getComputedStyle(select).fontSize),
          inheritedFont: parseFloat(getComputedStyle(select.parentElement).fontSize)
        }))`);
        assert.ok(dropdowns.length > 0);
        const selectorScale = setup.surface === "whist" ? .75 : 1;
        assert.ok(dropdowns.every(select => Math.abs(select.font - selectorScale * select.inheritedFont) < .01),
          "The dropdown size must follow the selected presentation surface");
        await evaluate("document.querySelector('.setup form').requestSubmit()");
        await until(() => evaluate("!!document.querySelector('#table[aria-busy=false] .board[data-layout=ready] .seat')"));
        assert.equal(await evaluate("document.querySelector('#frame').dataset.viewport"), "phone");
        const geometry = await evaluate(`(${remeasureSeatFrames})()`);
        assert.ok(geometry.boardRetained);
        assert.equal(geometry.before.length, started.view.seats.length);
        assert.ok(geometry.before.every(seat => seat.width > 0 && seat.height > 0));
        assert.deepEqual(geometry.after, geometry.before, "Same-size remeasurement moved a player frame");
        const png = await send("Page.captureScreenshot", { format: "png" });
        const file = "sanity-" + main.split(".").at(-1) + ".png";
        await writeFile(join(output, file), Buffer.from(png.data, "base64"));
        images.push({ file, title: title + " · adaptor din JAR, 414×896" });
        return { version, menu, dropdowns, geometry };
      });
    }
    return { status: "PASS", observed: { title, seats: started.view.seats.length, revision, browserCheck, motion, trumpText,
      viewport, feedback }, images };
  });
} catch (error) {
  result = { status: "FAIL", observed: error.stack };
  process.exitCode = 1;
}
await writeFile(join(output, "sanity.json"), JSON.stringify({ name: title + " · integrare JAR", expected, ...result }, null, 2) + "\n");
console.log(result.status + " " + title);

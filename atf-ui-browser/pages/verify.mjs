import assert from "node:assert/strict";
import { readFile, readdir, mkdir, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { until, withBrowser } from "../tests/fixtures.mjs";

const [input, output, executable] = process.argv.slice(2);
if (!input || !output) throw new Error("Expected site and external evidence directories");
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

const files = [".nojekyll", "collection.mjs", "engine-worker.mjs", "index.html", "local-transport.mjs",
  "profile.mjs", "table.css", "table.mjs", "viewport.mjs", "whist-engine.mjs"];

await check("Pages · inventarul public", "Doar resurse statice și legături relative în subdirectorul proiectului", async () => {
  assert.deepEqual((await readdir(directory)).sort(), files.toSorted());
  const markup = await readFile(join(directory, "index.html"), "utf8");
  assert.match(markup, /data-engine="whist"/);
  assert.match(markup, /connect-src 'none'/);
  assert.match(markup, /src="\.\/table\.mjs"/);
  assert.match(markup, /href="\.\/table\.css"/);
  assert.ok((await readFile(join(directory, "whist-engine.mjs"))).length < 2_000_000);
  return { observed: files };
});

await check("Pages · motorul compilat și profilul", "Partidă românească integrală, mutări legale, scor și profil restaurat", async () => {
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
  assert.equal(state.view.prompt, "Clasament final");
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

if (executable) {
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
        await until(() => evaluate("!!document.querySelector('.setup form')"));
        assert.equal(await evaluate("document.querySelector('#frame').dataset.viewport"), "phone");
      }
      const profileButton = `[...document.querySelectorAll('.setup button')].find(button => button.textContent.startsWith('Numele tău:'))`;
      await open();
      await evaluate(profileButton + ".click()");
      await evaluate(`(() => {
        const form = document.querySelector('.profile-form');
        form.elements.name.value = 'Bianca';
        form.requestSubmit();
      })()`);
      await until(() => evaluate(profileButton + "?.textContent === 'Numele tău: Bianca'"));
      await evaluate("document.querySelector('.setup form').requestSubmit()");
      await until(() => evaluate("!!document.querySelector('.board[data-layout=ready]')"));
      await evaluate("[...document.querySelectorAll('.actions button')].find(button => button.textContent === 'Licitează').click()");
      await until(() => evaluate(`document.querySelector('#table').getAttribute('aria-busy') === 'false' &&
        !document.querySelector('[data-bid]')`));
      const board = await send("Page.captureScreenshot", { format: "png" });
      await writeFile(join(output, "pages-mobile-board.png"), Buffer.from(board.data, "base64"));
      await send("Page.navigate", { url: "about:blank" });
      await until(() => evaluate("location.href === 'about:blank'"));
      await open();
      await until(() => evaluate(profileButton + "?.textContent === 'Numele tău: Bianca'"));
      assert.ok(!requests.some(path => path.startsWith("/api/")));
      const reopened = await send("Page.captureScreenshot", { format: "png" });
      await writeFile(join(output, "pages-mobile-profile.png"), Buffer.from(reopened.data, "base64"));
      return { observed: { version, viewport: "414×896", profile: "Bianca", requests: [...new Set(requests)] },
        images: [{ file: "pages-mobile-board.png", title: "Partidă pornită în browser" },
          { file: "pages-mobile-profile.png", title: "Profil recuperat după redeschidere" }] };
    });
  } finally {
    server.close();
  }
}

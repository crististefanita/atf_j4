import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { client, until, withBrowser, withServer } from "./fixtures.mjs";

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
    assert.match(await page.text(), /table\.mjs/);
    for (const file of ["table.mjs", "viewport.mjs", "profile.mjs", "collection.mjs", "table.css"]) {
      const asset = await fetch(new URL(file, url));
      assert.equal(asset.status, 200, file);
      assert.ok((await asset.text()).length > 0, file);
    }
    const request = client(url);
    await request("/api/setup", undefined, 403, false);
    assert.equal((await request("/api/setup")).title, title);
    const before = await request("/api/state");
    const started = await request("/api/start", { revision: before.revision });
    assert.ok(started.view.seats.length >= 2);
    await request("/api/start", { revision: before.revision }, 409);
    const hiddenHand = started.view.actions.find(action => action.key === "reveal");
    const playable = hiddenHand
      ? await request("/api/action", { revision: started.revision, action: hiddenHand.key }) : started;
    assert.ok(playable.view.hand.length > 0);
    let browserVersion = null;
    if (executable) {
      browserVersion = await withBrowser(executable, join(output, "sanity-browser"), async browser => {
        const { send, evaluate, version } = browser;
        await send("Page.enable");
        await send("Emulation.setDeviceMetricsOverride", { width: 414, height: 896, deviceScaleFactor: 1, mobile: true });
        await send("Emulation.setUserAgentOverride", { userAgent: "Mozilla/5.0 (Linux; Android 14; Mobile)" });
        assert.ok(!(await send("Page.navigate", { url: url.href })).errorText);
        await until(() => evaluate("!!document.querySelector('.setup form')"));
        assert.equal(await evaluate("document.title"), title);
        await evaluate("document.querySelector('.setup form').requestSubmit()");
        await until(() => evaluate("!!document.querySelector('.board[data-layout=ready] .seat')"));
        assert.equal(await evaluate("document.querySelector('#frame').dataset.viewport"), "phone");
        const png = await send("Page.captureScreenshot", { format: "png" });
        const file = "sanity-" + main.split(".").at(-1) + ".png";
        await writeFile(join(output, file), Buffer.from(png.data, "base64"));
        images.push({ file, title: title + " · adaptor din JAR, 414×896" });
        return version;
      });
    }
    return { status: "PASS", observed: { title, seats: started.view.seats.length, browserVersion }, images };
  });
} catch (error) {
  result = { status: "FAIL", observed: error.stack };
  process.exitCode = 1;
}
await writeFile(join(output, "sanity.json"), JSON.stringify({ name: title + " · integrare JAR", expected, ...result }, null, 2) + "\n");
console.log(result.status + " " + title);

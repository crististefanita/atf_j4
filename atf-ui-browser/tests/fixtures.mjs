import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { setTimeout as delay } from "node:timers/promises";

export async function until(probe, limit = 10000) {
  const deadline = Date.now() + limit;
  let last;
  do {
    try {
      const result = await probe();
      if (result) return result;
    } catch (error) {
      last = error;
    }
    await delay(75);
  } while (Date.now() < deadline);
  throw new Error("Fixture was not ready in " + limit + " ms", { cause: last });
}

export async function withServer(game, webRoot, exercise) {
  const child = spawn("java", ["-XX:ActiveProcessorCount=2", "-cp", game.classes, game.main, webRoot,
    "127.0.0.1", "0", ...(game.arguments ?? [])], { windowsHide: true });
  let output = "";
  let error = "";
  let startupFailure;
  child.stdout.setEncoding("utf-8").on("data", text => output += text);
  child.stderr.setEncoding("utf-8").on("data", text => error += text);
  child.on("error", failure => startupFailure = failure);
  try {
    while (!output.includes("\n")) {
      if (startupFailure) throw startupFailure;
      if (child.exitCode !== null) throw new Error("Java host stopped: " + error);
      await until(() => output.includes("\n") || child.exitCode !== null || startupFailure);
    }
    const url = new URL(output.trim().split(/\r?\n/)[0]);
    return await exercise(url, error);
  } finally {
    child.kill();
  }
}

export function client(url, clientId = "a".repeat(32), profileId = clientId) {
  return async (path, form, expected = 200, authenticated = true) => {
    const headers = authenticated ? {
      "X-Table-Access": url.hash.slice(1),
      "X-Table-Client": clientId,
      "X-Table-Profile": profileId
    } : {};
    const response = await fetch(new URL(path, url), {
      headers,
      method: form ? "POST" : "GET",
      body: form ? new URLSearchParams(form) : undefined,
      signal: AbortSignal.timeout(120000)
    });
    const json = await response.json();
    if (response.status !== expected) {
      throw new Error(path + " returned " + response.status + " instead of " + expected + ": " + JSON.stringify(json));
    }
    return json;
  };
}

export async function withBrowser(executable, profile, exercise) {
  const child = spawn(executable, ["--headless=new", "--disable-gpu", "--disable-background-networking",
    "--mute-audio", "--no-first-run", "--no-default-browser-check", "--remote-debugging-port=0",
    "--user-data-dir=" + profile, "about:blank"], { windowsHide: true, stdio: "ignore" });
  let startupFailure;
  child.on("error", failure => startupFailure = failure);
  let socket;
  try {
    const port = await until(async () => {
      if (startupFailure) throw startupFailure;
      if (child.exitCode !== null) throw new Error("Browser stopped: " + child.exitCode);
      return (await readFile(join(profile, "DevToolsActivePort"), "utf-8")).split(/\r?\n/)[0];
    }, 22000);
    const endpoint = "http://127.0.0.1:" + port;
    const targets = await (await fetch(endpoint + "/json/list")).json();
    const version = await (await fetch(endpoint + "/json/version")).json();
    socket = new WebSocket(targets.find(target => target.type === "page").webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error("DevTools open timeout")), 5000);
      socket.addEventListener("open", () => { clearTimeout(timer); resolve(); }, { once: true });
      socket.addEventListener("error", () => { clearTimeout(timer); reject(new Error("DevTools unavailable")); }, { once: true });
    });
    const calls = new Map();
    let sequence = 0;
    socket.addEventListener("message", event => {
      const message = JSON.parse(event.data);
      if (!calls.has(message.id)) return;
      const { resolve, reject, timer } = calls.get(message.id);
      calls.delete(message.id);
      clearTimeout(timer);
      if (message.error) reject(new Error(message.error.message));
      else resolve(message.result);
    });
    const send = (method, params = {}) => new Promise((resolve, reject) => {
      const id = ++sequence;
      const timer = setTimeout(() => {
        calls.delete(id);
        reject(new Error("DevTools command timed out: " + method));
      }, 15000);
      calls.set(id, { resolve, reject, timer });
      socket.send(JSON.stringify({ id, method, params }));
    });
    const evaluate = async expression => {
      const result = await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
      if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description ?? "Page evaluation failed");
      return result.result.value;
    };
    return await exercise({ send, evaluate, version: version.Browser });
  } finally {
    socket?.close();
    child.kill();
  }
}

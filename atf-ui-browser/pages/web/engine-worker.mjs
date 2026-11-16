import { initialize, command } from "./whist-engine.mjs";

const seed = crypto.getRandomValues(new Int32Array(2));
initialize(seed[0], seed[1]);

globalThis.addEventListener("message", ({ data: { id, path, values, profile } }) => {
  try {
    const reply = JSON.parse(command(path, JSON.stringify(values), profile.name, profile.progress));
    globalThis.postMessage({ id, ...reply });
  } catch (failure) {
    console.error(failure);
    globalThis.postMessage({ id, status: 500, error: "Motorul Whist nu a putut procesa comanda." });
  }
});

const profileKey = "whistlab:pages:profile:v1";

export function localTransport() {
  const worker = new Worker(new URL("./engine-worker.mjs", import.meta.url), { type: "module" });
  const pending = new Map();
  let sequence = 0;
  let workerFailure;
  let profile = { name: null, progress: null };

  worker.addEventListener("message", ({ data }) => {
    const command = pending.get(data.id);
    if (!command) return;
    pending.delete(data.id);
    if (data.status) command.reject(Object.assign(new Error(data.error), { status: data.status }));
    else command.resolve(data);
  });
  worker.addEventListener("error", () => {
    workerFailure = new Error("Motorul Whist nu s-a încărcat. Reîncarcă pagina pentru a încerca din nou.");
    for (const command of pending.values()) command.reject(workerFailure);
    pending.clear();
  });

  function load() {
    try {
      const stored = JSON.parse(localStorage.getItem(profileKey));
      if (stored && [stored.name, stored.progress].every(value => value === null || typeof value === "string")) {
        profile = stored;
      }
    } catch { /* The current tab still works with storage disabled. */ }
    return profile;
  }

  async function request(path, values = {}) {
    if (workerFailure) throw workerFailure;
    const previous = load();
    const fields = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, String(value)]));
    const reply = await new Promise((resolve, reject) => {
      const id = ++sequence;
      pending.set(id, { resolve, reject });
      worker.postMessage({ id, path, values: fields, profile: previous });
    });
    if (previous.name !== reply.name || previous.progress !== reply.progress) {
      profile = { name: reply.name, progress: reply.progress };
      try { localStorage.setItem(profileKey, JSON.stringify(profile)); } catch { /* Tab memory remains available. */ }
    }
    return reply.result;
  }

  return {
    request: (path, values) => navigator.locks
      ? navigator.locks.request(profileKey, () => request(path, values)) : request(path, values)
  };
}

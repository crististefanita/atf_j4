/** Schedules one human decision at a time. A submitted decision stays committed. */
export function autoGame({ available, wait, step, changed }) {
  let selected = false;
  let current;

  function set(value) {
    if (selected === value) return;
    selected = value;
    changed(selected);
    if (selected) void run();
    else current?.abort();
  }

  async function run() {
    if (current || !selected) return;
    const turn = new AbortController();
    current = turn;
    try {
      while (!turn.signal.aborted && selected && available()) {
        await wait(turn.signal);
        if (turn.signal.aborted) return;
        if (!available() || !await step()) break;
      }
      if (!turn.signal.aborted) set(false);
    } finally {
      current = undefined;
      if (selected) void run();
    }
  }

  return { set, selected: () => selected };
}

export function waitForDecision(delay, signal) {
  return new Promise(resolve => {
    const complete = () => {
      clearTimeout(timer);
      signal.removeEventListener("abort", complete);
      resolve();
    };
    const timer = setTimeout(complete, delay);
    signal.addEventListener("abort", complete, { once: true });
  });
}

import { rankingPanel } from "./records.mjs";

const languageKey = "whistlab:web:language:v1";

export function preferredLanguage() {
  try { return localStorage.getItem(languageKey) === "en" ? "en" : "ro"; }
  catch { return "ro"; }
}

export function saveLanguage(value) {
  try { localStorage.setItem(languageKey, value); } catch { /* Tab state stays available. */ }
}

export function normalizedNameLength(value) {
  return Array.from(value.replace(/^[\x00-\x20]+|[\x00-\x20]+$/g, "").normalize("NFC")).length;
}

export function dismissGameMenuOnOutsideClick(stage) {
  stage.addEventListener("click", event => {
    const menu = stage.querySelector(".whist-navigation[open]");
    if (menu && !menu.contains(event.target)) menu.open = false;
  }, { capture: true });
}

export function createProfileDialogs(element, button, submit, copy = (_, text) => text) {
  const dialog = element("dialog");
  dialog.className = "player-dialog";
  document.body.append(dialog);

  function show(title, content, close, kind) {
    const heading = element("h2", title);
    heading.id = "player-dialog-title";
    dialog.setAttribute("aria-labelledby", heading.id);
    dialog.className = "player-dialog " + kind;
    dialog.replaceChildren(heading, content);
    if (close) dialog.append(button(close, () => dialog.close()));
    if (!dialog.open) dialog.showModal();
  }

  function edit(name) {
    const form = element("form");
    form.className = "profile-form";
    const label = element("label", copy("name", "Numele tău"));
    const input = element("input");
    input.name = "name";
    input.value = name;
    input.autocomplete = "off";
    label.append(input);
    const hint = element("p", copy("nameHint", "Maximum 10 caractere."));
    const error = element("p");
    error.setAttribute("role", "alert");
    const save = element("button", copy("save", "Salvează"), "primary");
    save.type = "submit";
    async function applyName(value) {
      save.disabled = true;
      reset.disabled = true;
      const failure = await submit("/api/profile", { name: value });
      if (failure) {
        error.textContent = failure;
        save.disabled = normalizedNameLength(input.value) > 10;
        reset.disabled = false;
      } else {
        dialog.close();
      }
    }
    const reset = button(copy("reset", "Resetează numele"), () => applyName(""));
    input.addEventListener("input", () => {
      save.disabled = normalizedNameLength(input.value) > 10;
      error.textContent = save.disabled ? copy("nameError", "Numele are mai mult de 10 caractere.") : "";
    });
    const actions = element("div", "", "profile-actions");
    actions.append(reset, button(copy("cancel", "Anulează"), () => dialog.close()), save);
    form.append(label, hint, error, actions);
    form.addEventListener("submit", event => {
      event.preventDefault();
      applyName(input.value);
    });
    show(copy("name", "Numele tău"), form, null, "profile-editor");
    input.focus();
    input.select();
  }

  function catalogue(data) {
    const body = element("div", "", "goals");
    const summary = element("p", data.summary, "achievement-summary");
    const goals = data.groups.flatMap(group => group.goals);
    const progress = element("progress");
    progress.max = goals.length || 1;
    progress.value = goals.filter(goal => goal.complete).length;
    progress.setAttribute("aria-label", copy("progress", "Progresul realizărilor"));
    for (const group of data.groups) {
      const heading = element("h3", group.heading);
      body.append(heading);
      for (const goal of group.goals) body.append(goalRow(goal, true));
    }
    const content = element("div", "", "achievement-content");
    content.append(summary, progress, body);
    show(copy("achievements", "Realizări"), content, copy("back", "Înapoi"), "achievement-catalogue");
  }

  function goalRow(goal, collapsible) {
    const title = [goal.marker, goal.title, goal.progress].join(" ");
    if (collapsible) {
      const row = element("details");
      const summary = element("summary");
      summary.append(element("strong", (goal.complete ? goal.marker + " " : "") + goal.title),
        element("span", goal.level + " · " + goal.progress));
      row.append(summary, element("p", goal.description));
      return row;
    }
    const row = element("div", "", "goal-notice");
    row.append(element("strong", title), element("p", goal.level + " · " + goal.description));
    return row;
  }

  function notice(data) {
    if (!data || dialog.open) return;
    const body = element("div", "", "goals");
    body.append(element("p", data.summary));
    for (const goal of data.goals) body.append(goalRow(goal, false));
    show(data.title, body, copy("close", "Închide"), "achievement-notice");
    dialog.addEventListener("close", () => submit("/api/notice", { dismiss: "yes" }), { once: true });
  }

  function message(title, paragraphs) {
    const body = element("div", "", "whist-dialog-copy");
    for (const paragraph of paragraphs) body.append(element("p", paragraph));
    show(title, body, copy("close", "Închide"), "game-rules");
  }

  function liveScore(data, table, showHistory) {
    const body = element("div", "", "whist-live-score");
    body.append(element("p", data.summary, "whist-score-status"));
    if (data.pendingBid) body.append(element("p", data.pendingBid, "pending-bid"));
    const scroll = element("div", "", "live-score-table");
    scroll.append(table);
    body.append(scroll);
    const actions = element("div", "", "live-score-actions");
    if (showHistory) actions.append(button(copy("history", "Istoricul scorului"), showHistory, "quiet"));
    actions.append(button(copy("close", "Închide"), () => dialog.close(), "live-score-close"));
    body.append(actions);
    show(data.liveTitle, body, null, "game-live-score");
  }

  function rankings(data) {
    show(copy("ranking", "Clasamente"), rankingPanel(data, element, copy),
      copy("close", "Închide"), "game-rankings");
  }

  return { edit, catalogue, notice, message, liveScore, rankings, dialog };
}
export function deviceProfile(tabId, storage = () => localStorage) {
  const idKey = "table-profile-id";
  const nameKey = "table-profile-name";
  function get(key) {
    try { return storage().getItem(key); } catch { return null; }
  }
  function put(key, value) {
    try { storage().setItem(key, value); } catch { /* Storage can be disabled. */ }
  }
  const savedId = get(idKey);
  const id = /^[0-9a-f]{32}$/.test(savedId) ? savedId : tabId;
  if (id !== savedId) put(idKey, id);
  return {
    id,
    name: () => get(nameKey),
    remember(profile) {
      if (profile?.savedName) put(nameKey, profile.name);
    }
  };
}

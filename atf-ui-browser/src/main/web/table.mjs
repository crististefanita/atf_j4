import { fitTextWidth, isPhone, viewportGeometry } from "./viewport.mjs";
import { collect } from "./collection.mjs";
import { autoGame, waitForDecision } from "./auto-game.mjs";
import { bindHandFeedback } from "./hand-feedback.mjs";
import { historyGrid } from "./records.mjs";
import { createProfileDialogs, deviceProfile, dismissGameMenuOnOutsideClick, preferredLanguage, saveLanguage } from "./profile.mjs?v=0.2.1";

const stage = document.querySelector("#table");
const frame = document.querySelector("#frame");
const viewport = document.querySelector(".viewport");
const history = document.querySelector("#history");
let animation;
const resize = () => {
  animation?.hideOverlay();
  const visual = window.visualViewport;
  const inset = getComputedStyle(viewport);
  const width = Math.min(document.documentElement.clientWidth, visual?.width ?? innerWidth)
    - parseFloat(inset.paddingLeft) - parseFloat(inset.paddingRight);
  const height = (visual?.height ?? innerHeight) - parseFloat(inset.paddingTop) - parseFloat(inset.paddingBottom);
  if (width <= 0 || height <= 0) return;
  const phone = isPhone(navigator.userAgent);
  const result = viewportGeometry(width, height, phone);
  frame.dataset.viewport = phone ? "phone" : "desktop";
  frame.style.width = result.frameWidth + "px";
  frame.style.height = result.frameHeight + "px";
  stage.style.setProperty("--stage-width", result.width + "px");
  stage.style.setProperty("--stage-height", result.height + "px");
  stage.style.setProperty("--stage-scale", result.scale);
  history.style.setProperty("--dialog-width", result.frameWidth + "px");
  history.style.setProperty("--dialog-scale", result.scale);
  document.documentElement.style.setProperty("--dialog-width", result.frameWidth + "px");
  document.documentElement.style.setProperty("--dialog-height", result.frameHeight + "px");
  document.documentElement.style.setProperty("--dialog-scale", result.scale);
  if (stage.querySelector(".board")) placeTable();
};
resize();
window.addEventListener("resize", resize);
window.visualViewport?.addEventListener("resize", resize);

function randomClient() {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, "0")).join("");
}

function httpTransport() {
  const linkKey = location.hash.slice(1);
  if (linkKey) sessionStorage.setItem("table-access", linkKey);
  const access = sessionStorage.getItem("table-access") ?? "";
  let client = sessionStorage.getItem("table-client");
  if (!client) {
    client = randomClient();
    sessionStorage.setItem("table-client", client);
  }
  const profile = deviceProfile(client);
  const headers = { "X-Table-Access": access, "X-Table-Client": client, "X-Table-Profile": profile.id };
  return {
    cachedName: profile.name,
    remember: profile.remember,
    async request(path, values) {
      const response = await fetch(path, values ? { method: "POST", headers, body: new URLSearchParams(values) } : { headers });
      const payload = await response.json();
      if (!response.ok) throw Object.assign(new Error(payload.error), { status: response.status });
      return payload;
    }
  };
}
let transport;
let setup;
let state;
let pending = false;
let handFeedback;
let rankingsOpening = false;
let language = "ro";

function copy(key, fallback) { return setup?.copy?.[language]?.[key] ?? fallback; }

function rememberLanguage(next) {
  language = next;
  document.documentElement.lang = next;
  saveLanguage(next);
}

let selectedBid;
let setupDraft = {};
let measuring = 0;
let playedLayout;
const errorBox = element("p", "", "error");
errorBox.setAttribute("role", "alert");
const profileDialogs = createProfileDialogs(element, button, issue, copy);
const automation = autoGame({
  available: () => !pending && !!stage.querySelector(".whist-match") &&
    state.view?.actions.some(move => move.key === "auto-step"),
  wait: signal => waitForDecision(state.view.actions.some(move => move.key.startsWith("bid:"))
    ? 0 : setup.autoDelayMillis, signal),
  step: async () => !await issue("/api/action", { action: "auto-step" }),
  changed: enabled => {
    stage.toggleAttribute("data-automatic", enabled);
    if (enabled) handFeedback?.cancel();
    const input = stage.querySelector(".whist-auto input");
    if (input) input.checked = enabled;
    const stop = stage.querySelector(".stop-auto");
    if (stop) stop.hidden = !enabled;
  }
});

function dialogBlocksPlay() {
  return rankingsOpening || history.open || profileDialogs.dialog.open || !!stage.querySelector(".whist-navigation[open]");
}

function refreshAutomationGate() {
  const blocked = dialogBlocksPlay();
  automation.pause(blocked);
  if (blocked) handFeedback?.cancel();
}
history.addEventListener("close", refreshAutomationGate);
profileDialogs.dialog.addEventListener("close", refreshAutomationGate);
dismissGameMenuOnOutsideClick(stage);

function element(tag, text, style) {
  const result = document.createElement(tag);
  if (text) result.textContent = text;
  if (style) result.className = style;
  return result;
}

function button(label, handler, style) {
  const result = element("button", label, style);
  result.type = "button";
  result.addEventListener("click", handler);
  return result;
}

function call(path, values) { return transport.request(path, values); }

async function issue(path, fields) {
  if (pending) return copy("busy", "O comandă este deja în curs.");
  pending = true;
  stage.setAttribute("aria-busy", "true");
  errorBox.textContent = "";
  try {
    const previous = stage.querySelector(".setup") ? menuSetup
      : stage.querySelector(".whist-help") ? helpPage
      : stage.querySelector(".whist-home") ? menu : null;
    const result = await call(path, { ...fields, revision: String(state.revision) });
    if (result.playback?.length && stage.querySelector(".board")) {
      stage.dataset.playingSteps = "true";
      await playCards(result.playback);
    } else if (result.collections.length && stage.querySelector(".board")) {
      await placeTable();
      for (const [index, cue] of result.collections.entries()) {
        stage.dataset.collectionStep = String(index + 1);
        animation = collect(stage, cue, collectionFace, setup.collectionSamples);
        await animation.done;
        animation = undefined;
      }
    }
    state = result;
    transport.remember?.(state.profile);
    selectedBid = undefined;
    if (path === "/api/language") rememberLanguage(fields.language);
    if (previous && ["/api/profile", "/api/language", "/api/notice"].includes(path)) previous();
    else render();
    stage.querySelector(".prompt, h1")?.focus({ preventScroll: true });
    profileDialogs.notice(state.profile?.notice);
    showStorageIssue();
    return "";
  } catch (failure) {
    if (failure.status === 409 || failure.status === 500) {
      try {
        state = await call("/api/state");
        render();
      } catch (reloadFailure) {
        errorBox.textContent = reloadFailure.message;
        return reloadFailure.message;
      }
    }
    errorBox.textContent = failure.message;
    return failure.message;
  } finally {
    animation?.cancel();
    animation = undefined;
    delete stage.dataset.collectionStep;
    delete stage.dataset.playingSteps;
    pending = false;
    stage.setAttribute("aria-busy", "false");
  }
}

async function playCards(steps) {
  const center = stage.querySelector(".cards-on-table");
  const seats = [...stage.querySelectorAll(".seat")];
  const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
  let collectionNumber = 0;
  await placeTable();
  for (const step of steps) {
    if (step.startsTrick) center.replaceChildren();
    stage.querySelector(".whist-bids").textContent = step.bids;
    for (const [seat, node] of seats.entries()) {
      applySeatFacts(node, step.seatFacts[seat]);
      markSeat(node, seat === step.play.seat, false);
    }
    if (step.delayMillis) {
      await new Promise(resolve => requestAnimationFrame(() => setTimeout(resolve, step.delayMillis)));
    }
    if (!center.isConnected) return;
    const face = step.play.card;
    if (step.play.seat === 0) {
      const source = [...stage.querySelectorAll(".fan .card")].find(node => node.dataset.face === face.rank + face.suit);
      if (source) {
        source.remove();
        await placeTable();
      }
    }
    center.append(playedCard(step.play, state.view.seats));
    placePlayed(center);
    if (!step.collection) {
      if (!reducedMotion) await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
      continue;
    }
    stage.dataset.collectionStep = String(++collectionNumber);
    for (const [seat, node] of seats.entries()) {
      if (step.collection.seatFacts?.[seat]) applySeatFacts(node, step.collection.seatFacts[seat]);
      markSeat(node, false, false);
    }
    animation = collect(stage, step.collection, collectionFace, setup.collectionSamples);
    await animation.done;
    animation = undefined;
    center.replaceChildren();
  }
}

function applySeatFacts(node, facts) {
  const [bid, won] = facts.split("\n");
  writeSeatFact(node.querySelector(".seat-bid"), bid);
  writeSeatFact(node.querySelector(".seat-won"), won);
}

function showStorageIssue() {
  if (state.storageIssue) errorBox.textContent = copy(state.storageIssue, "Salvarea locală nu este disponibilă.");
}

async function showRankings() {
  rankingsOpening = true;
  refreshAutomationGate();
  try {
    const data = await call("/api/rankings");
    if (data) profileDialogs.rankings(data);
  } catch {
    errorBox.textContent = copy("storageError", "Clasamentele locale nu au putut fi citite.");
  } finally {
    rankingsOpening = false;
    refreshAutomationGate();
  }
}

function menu() {
  handFeedback?.cancel();
  automation.set(false);
  if (setup.surface === "whist") {
    const home = element("section", "", "whist-home");
    const title = element("h1", copy("home", "Whist"));
    title.tabIndex = -1;
    home.append(title, languagePicker(),
      button(copy("name", "Numele tău") + ": " + state.profile.name, () => profileDialogs.edit(state.profile.name), "quiet"),
      button(copy("newGame", "Joc nou"), menuSetup, "primary"));
    const resume = button(copy("resume", "Reia partida"), render);
    resume.disabled = !state.view;
    home.append(resume,
      button(copy("ranking", "Clasamente"), showRankings, "quiet"),
      button(copy("achievements", "Realizări"), () => profileDialogs.catalogue(state.profile.catalogue), "quiet"),
      button(copy("help", "Reguli de joc"), helpPage, "quiet"));
    stage.replaceChildren(home, errorBox);
    return;
  }
  menuSetup();
}

function languagePicker() {
  const label = element("label", copy("language", "Limba"), "whist-language");
  const select = element("select");
  for (const [value, text] of [["en", "English"], ["ro", "Română"]]) {
    const option = element("option", text);
    option.value = value;
    select.append(option);
  }
  select.value = language;
  select.addEventListener("change", async () => {
    select.disabled = true;
    const failure = await issue("/api/language", { language: select.value });
    if (failure) {
      select.disabled = false;
      select.value = language;
    }
  });
  label.append(select);
  return label;
}

function helpPage() {
  const page = element("section", "", "whist-help");
  const title = element("h1", copy("instructionsSubtitle", "Reguli de joc"));
  title.tabIndex = -1;
  page.append(element("p", copy("instructions", ""), "whist-score-hand"), title, languagePicker());
  for (const section of copy("rules", [])) {
    const rule = element("section", "", "whist-rule");
    rule.append(element("h2", section.title), element("p", section.body));
    page.append(rule);
  }
  page.append(button(copy("back", "Înapoi"), () => { menu(); focusStart(); }, "primary"));
  stage.replaceChildren(page, errorBox);
  focusStart();
}

function focusStart() {
  stage.querySelector("h1")?.focus({ preventScroll: true });
  stage.scrollTop = 0;
}

function menuSetup() {
  const native = setup.surface === "whist";
  const content = element("section", "", "setup");
  const title = element("h1", native ? copy("setup", "Setări joc") : setup.title);
  title.tabIndex = -1;
  const form = element("form");
  const fields = (language === "en" ? setup.englishFields : setup.fields).filter(field => field.key !== "name" || !state.profile);
  let readSetup;
  if (native) {
    readSetup = whistSettings(form, Object.fromEntries(fields.map(field => [field.key, field])));
  } else {
    for (const field of fields) form.append(settingsField(field));
  }
  const start = element("button", native ? copy("start", "Începe partida") : copy("newGame", "Joc nou"), "primary");
  start.type = "submit";
  form.append(start);
  form.addEventListener("submit", event => {
    event.preventDefault();
    const configuration = native ? readSetup() : Object.fromEntries(new FormData(form));
    issue("/api/start", configuration);
  });
  content.append(title);
  if (native) content.append(languagePicker());
  content.append(form);
  if (native) content.append(button(copy("back", "Înapoi"), () => { menu(); focusStart(); }, "quiet"));
  if (!native && state.view) content.append(button("Continuă", render));
  if (!native && state.profile) {
    content.append(button(copy("name", "Numele tău") + ": " + state.profile.name, () => profileDialogs.edit(state.profile.name)),
      button("Realizări", () => profileDialogs.catalogue(state.profile.catalogue)));
  }
  stage.replaceChildren(content, errorBox);
  if (native) focusStart();
}

function settingsField(field, options = field.options, visibleLabel = true) {
  const label = element("label", visibleLabel ? field.label : "");
  const input = element(options.length ? "select" : "input");
  input.name = field.key;
  if (!visibleLabel) input.setAttribute("aria-label", field.label);
  for (const option of options) {
    const choice = element("option", option.label);
    choice.value = option.value;
    input.append(choice);
  }
  if (!options.length) input.autocomplete = "off";
  input.value = setupDraft[field.key] ?? field.initial;
  label.append(input);
  return label;
}

function whistSettings(form, fields) {
  const players = settingsField(fields.players);
  const modes = settingsField(fields.mode, fields.mode.options, false);
  const level = settingsField(fields.difficulty);
  modes.className = "mode-field";
  level.className = "difficulty-field";
  const toggle = (text, key) => {
    const field = element("label", "", "setup-toggle");
    const input = element("input");
    input.type = "checkbox";
    input.name = key;
    input.checked = (setupDraft[key] ?? fields[key].initial) === "yes";
    field.append(input, document.createTextNode(text));
    return field;
  };
  const series = toggle(copy("streaks", "Serii de cinci mâini"), "streaks");
  const seriesValue = settingsField(fields.series, fields.series.options.filter(option => option.value !== "0"), false);
  seriesValue.className = "series-value";
  const streaks = element("div", "", "series-row");
  streaks.append(series, seriesValue);
  const zero = toggle(fields.zero.label, "zero");
  const mode = modes.querySelector("select");
  const seats = players.querySelector("select");
  const enabled = series.querySelector("input");
  const points = seriesValue.querySelector("select");
  const metadata = () => copy("modeRules", {})[mode.value];
  const rules = button(copy("rulesAndSchedule", "Reguli și program"), () => {
    const details = metadata();
    if (details) profileDialogs.message(details.title,
      [details.description, details.schedule[seats.value], ...details.extras]);
  }, "quiet");
  const read = () => ({
    ...Object.fromEntries(new FormData(form)),
    series: points.value,
    streaks: enabled.checked ? "yes" : "no",
    zero: zero.querySelector("input").checked ? "yes" : "no"
  });
  const update = () => {
    streaks.hidden = zero.hidden = !metadata()?.romanian;
    points.disabled = !enabled.checked;
    setupDraft = read();
  };
  form.addEventListener("change", update);
  form.append(players, modes, rules, streaks, zero, level);
  update();
  return read;
}

function card(face, interactive = false, acceptsFeedback = false) {
  const node = element(interactive ? "button" : "span", "", "card");
  if (interactive) {
    node.type = "button";
    node.disabled = !face.action && !acceptsFeedback;
    if (acceptsFeedback) node.dataset.command = face.action;
    else if (face.action) node.addEventListener("click", () => action(face.action));
  }
  node.classList.toggle("red", face.suit === "♥" || face.suit === "♦");
  node.setAttribute("aria-label", face.label);
  node.dataset.face = face.rank + face.suit;
  if (interactive && face.action) node.classList.add("legal-card");
  const index = () => {
    const corner = element("span", "", "card-index");
    corner.append(element("span", face.rank, "card-rank"), element("span", face.suit));
    return corner;
  };
  const inverted = index();
  inverted.classList.add("inverted");
  node.append(index(), element("span", face.suit, "suit"), inverted);
  return node;
}

function compactFace(play) {
  const face = element("span", "", "card compact-play-face");
  face.append(element("span", play.card.rank, "card-rank"), element("span", play.card.suit, "compact-suit"));
  face.classList.toggle("red", play.card.suit === "♥" || play.card.suit === "♦");
  face.setAttribute("aria-hidden", "true");
  return face;
}

function collectionFace(face, play) {
  return play.faceLabel ? compactFace(play) : card(face);
}

function playedCard(play, seats) {
  const tile = element("div", "", "played-card");
  tile.dataset.seat = String(play.seat);
  tile.append(collectionFace(play.card, play), element("span", seats[play.seat].name, "played-name"));
  if (play.description) {
    tile.setAttribute("role", "group");
    tile.setAttribute("aria-label", play.description);
    tile.tabIndex = 0;
  }
  return tile;
}

function placePlayed(cards) {
  if (!playedLayout || !cards.childElementCount) return;
  cards.style.height = playedLayout.centerHeightDp + "px";
  cards.classList.add("positioned");
  for (const tile of cards.children) {
    const slot = playedLayout.slots[Number(tile.dataset.seat)];
    const face = tile.querySelector(".card");
    tile.style.left = slot.leftDp + "px";
    tile.style.top = (slot.row * playedLayout.rowStrideDp + playedLayout.verticalPaddingDp) + "px";
    face.style.width = face.style.minWidth = playedLayout.faceWidth + "px";
    face.style.height = playedLayout.faceHeight + "px";
    face.style.setProperty("--compact-rank-fit", Math.floor((playedLayout.faceWidth - 4) / 2) + "px");
    if (face.classList.contains("compact-play-face")) face.style.fontSize = playedLayout.faceTextSp / 16 + "rem";
    const player = tile.querySelector(".played-name");
    player.hidden = !playedLayout.playerLabelHeightDp;
    player.style.height = playedLayout.playerLabelHeightDp + "px";
    player.style.fontSize = playedLayout.playerTextSp / 16 + "rem";
  }
}

function scoreGrid(data, native = false) {
  const grid = element("table", "", native ? "whist-score-grid" : "");
  if (native) {
    const widths = element("colgroup");
    for (const index of data.headings.keys()) {
      const column = element("col");
      column.style.width = "var(--whist-score-col-" + index + ")";
      widths.append(column);
    }
    grid.append(widths);
  }
  function row(cells, tag) {
    const line = element("tr");
    for (const value of cells) line.append(element(tag, value));
    return line;
  }
  const header = element("thead");
  const body = element("tbody");
  header.append(row(data.headings, "th"));
  for (const cells of data.rows) body.append(row(cells, "td"));
  grid.append(header, body);
  return grid;
}

function showHistory(data) {
  history.querySelector("#history-title").textContent = copy("history", "Istoric punctaj");
  history.querySelector("button").textContent = copy("close", "Închide");
  const content = document.querySelector("#history-data");
  content.replaceChildren(data.grid ? historyGrid(data.grid, element) : scoreGrid(data));
  history.querySelector(".history-legend").textContent = data.grid?.legend ?? "";
  history.showModal();
  refreshAutomationGate();
}

function action(key) {
  automation.set(false);
  return issue("/api/action", { action: key });
}

function control(move) {
  if (move.minimum === null) return button(move.label, () => action(move.key), move.emphasized ? "primary" : "");
  const form = element("form", "", "amount");
  const amount = element("input");
  amount.type = "number";
  amount.min = move.minimum;
  amount.max = move.maximum;
  amount.value = move.minimum;
  amount.required = true;
  amount.setAttribute("aria-label", move.label + " · sumă");
  const submit = element("button", move.label);
  submit.type = "submit";
  form.append(amount, submit);
  form.addEventListener("submit", event => {
    event.preventDefault();
    action(move.key + "/" + amount.value);
  });
  return form;
}

function bidding(actions, content, native) {
  const suggested = actions.find(move => move.emphasized);
  const select = move => {
    selectedBid = move.key;
    for (const node of content.querySelectorAll("[data-bid]")) {
      node.setAttribute("aria-pressed", String(node.dataset.bid === selectedBid));
    }
    submit.disabled = !selectedBid;
  };
  const submit = button(copy("bid", "Licitează"), () => action(selectedBid), native ? "bid-submit" : "primary");
  if (native) {
    const face = element("span", submit.textContent, "bid-face");
    submit.replaceChildren(face);
    content.classList.add("whist-bidding");
  }
  const options = element("div", "", "bid-options");
  options.classList.toggle("bid-strip", actions.length > 10);
  for (const move of actions) {
    const option = button(native ? "" : move.label, () => select(move));
    if (native) option.append(element("span", move.label, "bid-face"));
    option.dataset.bid = move.key;
    options.append(option);
  }
  content.append(options, submit);
  select(actions.find(move => move.key === selectedBid) ?? suggested ?? { key: undefined });
}

const trumpText = document.createElement("canvas").getContext("2d");

function fitTrumpText() {
  const tile = stage.querySelector(".whist-trump");
  if (!tile) return;
  const tileStyle = getComputedStyle(tile);
  const width = tile.clientWidth - parseFloat(tileStyle.paddingLeft) - parseFloat(tileStyle.paddingRight);
  const scale = parseFloat(getComputedStyle(document.documentElement).fontSize) / 16;
  for (const line of tile.children) {
    const style = getComputedStyle(line);
    const nominal = (line.tagName === "STRONG" ? 9 : 20) * scale;
    trumpText.font = `${style.fontWeight} ${nominal}px ${style.fontFamily}`;
    const ink = trumpText.measureText(line.dataset.face ?? line.textContent).width;
    const available = width - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
    line.style.fontSize = fitTextWidth(nominal, ink, available) + "px";
  }
}

async function placeTable() {
  const board = stage.querySelector(".board");
  const hand = stage.querySelector(".hand");
  if (!board || !hand) return;
  fitTrumpText();
  const request = ++measuring;
  const seats = [...board.querySelectorAll(".seat")];
  const fan = hand.querySelector(".fan");
  const width = board.clientWidth;
  const seatWidth = Math.min(116, Math.round(width * (stage.clientHeight < 760 ? 0.25 : 0.29)),
    Math.floor((width - 16) / 3));
  for (const seat of seats) seat.style.width = Math.max(1, seatWidth) + "px";
  try {
    board.style.minHeight = "";
    const layout = await call("/api/layout", {
      width, height: board.clientHeight, mode: state.view.header?.mode ?? "", seatHeight: Math.max(...seats.map(seat => seat.offsetHeight)),
      bidOptions: stage.dataset.playingSteps ? 0 : board.querySelectorAll("[data-bid]").length,
      players: seats.length, viewport: stage.clientHeight, handWidth: hand.clientWidth, cards: fan.children.length,
      ...(state.view.header && { handIndexWidth: Math.max(1, ...[...fan.querySelectorAll(".card-index:not(.inverted)")]
        .map(index => index.offsetLeft + index.offsetWidth + 2)) })
    });
    if (request !== measuring || !board.isConnected) return;
    playedLayout = layout.played ? { ...layout.played, ...layout.playedStyle } : null;
    if (layout.board) {
      if (layout.boardHeight > board.clientHeight) board.style.minHeight = layout.boardHeight + "px";
      for (const [index, seat] of seats.entries()) place(seat, layout.board.seats[index]);
      place(board.querySelector(".center"), layout.board.center);
    }
    placePlayed(board.querySelector(".cards-on-table"));
    if (layout.hand) {
      fan.style.width = layout.hand.width + "px";
      fan.style.paddingLeft = layout.hand.first + "px";
      for (const [index, face] of [...fan.children].entries()) {
        face.style.position = "absolute";
        face.style.left = layout.hand.first + index * layout.hand.advance + "px";
      }
    }
    if (layout.bids) {
      const controls = board.querySelector(".whist-bidding");
      if (controls) {
        controls.querySelector(".bid-options").classList.toggle("bid-strip", layout.bids.strip);
        controls.style.setProperty("--bid-columns", layout.bids.columns);
        for (const [key, prop] of Object.entries({
          choiceTarget: "choice-target", rowHeight: "row", choiceInset: "choice-inset",
          submitWidth: "submit-width", submitTarget: "submit-target", submitInsetX: "submit-inset-x",
          submitInsetY: "submit-inset-y", textSize: "text-size"
        })) controls.style.setProperty("--bid-" + prop, layout.bids[key] + "px");
      }
    }
    if (!board.dataset.layout) {
      const strip = board.querySelector(".bid-strip");
      const selected = strip?.querySelector("[aria-pressed='true']");
      if (selected) {
        const row = strip.getBoundingClientRect();
        const face = selected.getBoundingClientRect();
        const delta = face.left + face.width / 2 - row.left - row.width / 2;
        strip.scrollLeft += delta * strip.clientWidth / row.width;
      }
    }
    board.dataset.layout = "ready";
  } catch (failure) {
    if (request === measuring) errorBox.textContent = failure.message;
  }
}

function place(element, rectangle) {
  element.style.inset = rectangle.top + "px auto auto " + rectangle.left + "px";
  element.style.width = rectangle.right - rectangle.left + "px";
  element.style.height = rectangle.bottom - rectangle.top + "px";
  element.style.transform = "none";
}

function whistHeader(match) {
  const header = element("header", "", "whist-header");
  header.style.setProperty("--title-size", match.header.titleSize / 16 + "rem");
  header.style.setProperty("--progress-size", match.header.progressSize / 16 + "rem");
  header.style.setProperty("--bid-size", match.header.bidSize / 16 + "rem");
  const top = element("div", "", "whist-title-row");
  const navigation = element("details", "", "whist-navigation");
  const trigger = element("summary");
  trigger.setAttribute("aria-label", copy("gameMenu", "Meniu joc"));
  trigger.append(element("span", "", "menu-lines"));
  navigation.append(trigger);
  const items = element("div", "", "whist-menu-items");
  const item = (label, handler, style) => button(label, () => {
    if (pending && style !== "stop-auto") return;
    navigation.open = false;
    handler();
    refreshAutomationGate();
  }, style);
  const stop = item(copy("stopAutoGame", "Oprește jocul automat"), () => automation.set(false), "stop-auto");
  stop.hidden = !automation.selected();
  items.append(item(copy("scoreSheet", "Scorul partidei"), () => {
    profileDialogs.liveScore(match.scoreDetails, scoreGrid(match.scoreDetails.liveTable, true),
      match.history.rows.length ? () => showHistory(match.history) : null);
  }), stop,
  item(copy("name", "Numele tău"), () => profileDialogs.edit(state.profile.name)),
  item(copy("ranking", "Clasamente"), () => { void showRankings(); }),
  item(copy("mainMenu", "Meniu principal"), menu));
  navigation.addEventListener("toggle", refreshAutomationGate);
  navigation.append(items);
  const title = element("h1", match.header.title);
  top.append(navigation, title);
  const automatic = match.actions.find(move => move.key === "auto-step");
  if (automatic) {
    const toggle = element("label", "", "whist-auto");
    const input = element("input");
    input.type = "checkbox";
    input.checked = automation.selected();
    input.addEventListener("change", () => automation.set(input.checked));
    toggle.append(input, document.createTextNode(automatic.label));
    top.append(toggle);
  }
  const details = element("div", "", "whist-header-details");
  const status = element("div");
  status.append(element("p", match.header.progress, "whist-progress"),
    element("p", match.header.bids, "whist-bids"));
  const trump = element("div", "", "whist-trump");
  trump.setAttribute("aria-label", match.header.trumpDescription);
  const trumpValue = match.center.length
    ? card(match.center[0]) : element("span", match.header.trump, "whist-trump-value");
  trump.append(element("strong", copy("trump", "Atu")), trumpValue);
  details.append(status, trump);
  header.append(top, details);
  return header;
}

function whistScore(match) {
  automation.set(false);
  const page = element("section", "", "whist-score");
  const prompt = element("p", match.prompt, "prompt");
  prompt.tabIndex = -1;
  const details = match.scoreDetails;
  page.append(element("p", match.heading, "whist-score-hand"), element("h1", match.score.title),
    element("p", details.summary, "whist-score-status"), prompt);
  if (details.winner) page.append(element("p", details.winner, "whist-score-winner"));
  if (details.awards) page.append(element("p", details.awards, "whist-score-awards"));
  page.append(scoreGrid(match.score.table, true),
    button(copy("scoringDetails", "Calculul punctajului"),
      () => profileDialogs.message(copy("scoringDetails", "Calculul punctajului"), details.rules), "quiet"));
  for (const move of match.actions) page.append(control(move));
  page.append(button(copy("history", "Istoric punctaj"), () => showHistory(match.history), "quiet"),
    button(copy("mainMenu", "Meniu principal"), menu, "quiet"));
  stage.replaceChildren(page, errorBox);
  refreshAutomationGate();
}

function writeSeatFact(node, content) {
  const separator = content.lastIndexOf(" ");
  if (separator < 0) node.textContent = content;
  else node.replaceChildren(content.slice(0, separator + 1), element("b", content.slice(separator + 1)));
}

function markSeat(node, active, bidding) {
  node.classList.toggle("active", active);
  if (active) node.setAttribute("aria-current", "true");
  else node.removeAttribute("aria-current");
  const badge = node.querySelector(".seat-turn");
  if (badge) {
    badge.hidden = !active || (bidding && node.classList.contains("south"));
    badge.textContent = bidding ? copy("bidNow", "LICITEAZĂ") : copy("playNow", "JOACĂ ACUM");
  }
}

function render() {
  handFeedback?.cancel();
  handFeedback = undefined;
  if (!state.view) {
    menu();
    return;
  }
  const match = state.view;
  if (match.score) {
    whistScore(match);
    return;
  }
  const content = element("section", "", match.header ? "match whist-match" : "match");
  const heading = match.header ? whistHeader(match) : element("header");
  if (!match.header) {
    const labels = element("div");
    labels.append(element("h1", match.heading, "heading"), element("p", match.subtitle, "subtitle"));
    heading.append(labels, button("Meniu", menu, "menu"));
  }

  const bids = match.actions.filter(move => move.key.startsWith("bid:"));
  const board = element("div", "", "board");
  for (const seat of match.seats) {
    const position = element("div", "", "seat " + seat.anchor);
    position.append(element("strong", seat.name), element("span", seat.firstFact, "seat-bid"),
      element("span", seat.secondFact, "seat-won"));
    if (match.header) {
      writeSeatFact(position.querySelector(".seat-bid"), seat.firstFact);
      writeSeatFact(position.querySelector(".seat-won"), seat.secondFact);
      position.append(element("span", "", "seat-turn"));
    }
    markSeat(position, seat.active, bids.length > 0);
    board.append(position);
  }
  const visible = element("div", "", "cards-on-table");
  if (!match.header) for (const face of match.center) visible.append(card(face));
  for (const play of match.played) visible.append(playedCard(play, match.seats));
  const center = element("div", "", "center");
  center.append(visible);
  board.append(center);

  const controls = element("div", "", "controls");
  const prompt = element("p", match.prompt, "prompt");
  prompt.tabIndex = -1;
  const actions = element("div", "", "actions");
  if (bids.length) bidding(bids, actions, !!match.header);
  for (const move of match.actions.filter(move => !move.key.startsWith("bid:") && !(match.header && ["auto", "auto-step"].includes(move.key)))) {
    actions.append(control(move));
  }
  const toolbar = element("div", "", "toolbar");
  if (!match.header && match.history.rows.length) toolbar.append(button("Istoric", () => showHistory(match.history)));
  if (!match.header) controls.append(prompt);
  controls.append(actions);
  center.append(controls);

  const hand = element("div", "", "hand");
  hand.setAttribute("role", "group");
  hand.setAttribute("aria-label", copy("hand", "Cărțile tale"));
  hand.tabIndex = 0;
  const fan = element("div", "", "fan");
  const timing = setup.handMotion;
  const acceptsFeedback = !!match.header && !!timing && match.hand.some(face => face.action);
  for (const face of match.hand) fan.append(card(face, true, acceptsFeedback));
  if (acceptsFeedback) {
    const revision = state.revision;
    handFeedback = bindHandFeedback(fan, {
      timing,
      request: fields => call("/api/hand-feedback", { ...fields, revision }),
      active: () => fan.isConnected && revision === state.revision && !pending && !automation.selected() && !dialogBlocksPlay(),
      play: action,
      failed: async error => {
        if (error.status === 409) {
          try {
            state = await call("/api/state");
            render();
          } catch (reload) {
            errorBox.textContent = reload.message;
            return;
          }
        }
        errorBox.textContent = error.message;
      }
    });
  }
  hand.append(fan);

  content.append(heading, board);
  if (toolbar.childElementCount) content.append(toolbar);
  content.append(hand, errorBox);
  stage.replaceChildren(content);
  refreshAutomationGate();
  placeTable();
}

try {
  transport = document.documentElement.dataset.engine === "whist"
    ? (await import("./local-transport.mjs")).localTransport() : httpTransport();
  [setup, state] = await Promise.all([call("/api/setup"), call("/api/state")]);
  const recoveryIssue = state.storageIssue;
  if (setup.surface === "whist" && preferredLanguage() === "en") {
    state = await call("/api/language", { revision: String(state.revision), language: "en" });
    rememberLanguage("en");
  }
  document.documentElement.dataset.surface = setup.surface ?? "table";
  for (const token of setup.palette ?? []) document.documentElement.style.setProperty(token.name, token.color);
  const savedName = transport.cachedName?.();
  if (state.profile && !state.profile.savedName && savedName) {
    try {
      state = await call("/api/profile", { revision: String(state.revision), name: savedName });
    } catch (failure) {
      if (failure.status !== 400 && failure.status !== 409) throw failure;
      state = await call("/api/state");
    }
  }
  transport.remember?.(state.profile);
  document.title = setup.title;
  if (transport.resumeOnHome) menu();
  else render();
  profileDialogs.notice(state.profile?.notice);
  state.storageIssue ??= recoveryIssue;
  showStorageIssue();
} catch (failure) {
  errorBox.textContent = failure.message;
  stage.append(errorBox);
} finally {
  stage.setAttribute("aria-busy", "false");
}

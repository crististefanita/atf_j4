import { isPhone, viewportGeometry } from "./viewport.mjs";

const stage = document.querySelector("#table");
const frame = document.querySelector("#frame");
const viewport = document.querySelector(".viewport");
const history = document.querySelector("#history");
const resize = () => {
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
  if (stage.querySelector(".board")) placeTable();
};
resize();
window.addEventListener("resize", resize);
window.visualViewport?.addEventListener("resize", resize);

function randomClient() {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, "0")).join("");
}

const linkKey = location.hash.slice(1);
if (linkKey) sessionStorage.setItem("table-access", linkKey);
const access = sessionStorage.getItem("table-access") ?? "";
let client = sessionStorage.getItem("table-client");
if (!client) {
  client = randomClient();
  sessionStorage.setItem("table-client", client);
}
const headers = { "X-Table-Access": access, "X-Table-Client": client };
let setup;
let state;
let pending = false;
let selectedBid;
let measuring = 0;
const errorBox = element("p", "", "error");
errorBox.setAttribute("role", "alert");

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

async function call(path, values) {
  const response = await fetch(path, values ? { method: "POST", headers, body: new URLSearchParams(values) } : { headers });
  const payload = await response.json();
  if (!response.ok) throw Object.assign(new Error(payload.error), { status: response.status });
  return payload;
}

async function issue(path, fields) {
  if (pending) return;
  pending = true;
  stage.setAttribute("aria-busy", "true");
  errorBox.textContent = "";
  try {
    state = await call(path, { ...fields, revision: String(state.revision) });
    selectedBid = undefined;
    render();
    stage.querySelector(".prompt, h1")?.focus({ preventScroll: true });
  } catch (failure) {
    if (failure.status === 409 || failure.status === 500) {
      try {
        state = await call("/api/state");
        render();
      } catch (reloadFailure) {
        errorBox.textContent = reloadFailure.message;
        return;
      }
    }
    errorBox.textContent = failure.message;
  } finally {
    pending = false;
    stage.setAttribute("aria-busy", "false");
  }
}

function menu() {
  const content = element("section", "", "setup");
  const title = element("h1", setup.title);
  title.tabIndex = -1;
  const form = element("form");
  for (const field of setup.fields) {
    const label = element("label", field.label);
    const input = element(field.options.length ? "select" : "input");
    input.name = field.key;
    if (field.options.length) {
      for (const option of field.options) {
        const node = element("option", option.label);
        node.value = option.value;
        input.append(node);
      }
    } else {
      input.autocomplete = "off";
    }
    input.value = field.initial;
    label.append(input);
    form.append(label);
  }
  const start = element("button", "Joc nou", "primary");
  start.type = "submit";
  form.append(start);
  form.addEventListener("submit", event => {
    event.preventDefault();
    issue("/api/start", Object.fromEntries(new FormData(form)));
  });
  content.append(title, form);
  if (state.view) content.append(button("Continuă", render));
  stage.replaceChildren(content, errorBox);
}

function card(face, interactive = false) {
  const node = element(interactive ? "button" : "span", "", "card");
  if (interactive) {
    node.type = "button";
    node.disabled = !face.action;
    if (face.action) node.addEventListener("click", () => action(face.action));
  }
  node.classList.toggle("red", face.suit === "♥" || face.suit === "♦");
  node.setAttribute("aria-label", face.label);
  const index = () => {
    const corner = element("span", "", "card-index");
    corner.append(element("span", face.rank), element("span", face.suit));
    return corner;
  };
  const inverted = index();
  inverted.classList.add("inverted");
  node.append(index(), element("span", face.suit, "suit"), inverted);
  return node;
}

function showHistory(data) {
  const grid = element("table");
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
  document.querySelector("#history-data").replaceChildren(grid);
  history.showModal();
}

function action(key) { issue("/api/action", { action: key }); }

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

function bidding(actions, content) {
  const suggested = actions.find(move => move.emphasized);
  const select = move => {
    selectedBid = move.key;
    for (const node of content.querySelectorAll("[data-bid]")) {
      node.setAttribute("aria-pressed", String(node.dataset.bid === selectedBid));
    }
    submit.disabled = !selectedBid;
  };
  const submit = button("Licitează", () => action(selectedBid), "primary");
  const options = element("div", "", "bid-options");
  options.classList.toggle("bid-strip", actions.length > 10);
  for (const move of actions) {
    const option = button(move.label, () => select(move));
    option.dataset.bid = move.key;
    options.append(option);
  }
  content.append(options, submit);
  select(actions.find(move => move.key === selectedBid) ?? suggested ?? { key: undefined });
}

async function placeTable() {
  const board = stage.querySelector(".board");
  const hand = stage.querySelector(".hand");
  if (!board || !hand) return;
  const request = ++measuring;
  const seats = [...board.querySelectorAll(".seat")];
  const fan = hand.querySelector(".fan");
  const width = board.clientWidth;
  const seatWidth = Math.min(116, Math.round(width * (stage.clientHeight < 760 ? 0.25 : 0.29)),
    Math.floor((width - 16) / 3));
  for (const seat of seats) seat.style.width = Math.max(1, seatWidth) + "px";
  try {
    const layout = await call("/api/layout", {
      width, height: board.clientHeight, seatHeight: Math.max(...seats.map(seat => seat.offsetHeight)),
      players: seats.length, viewport: stage.clientHeight, handWidth: hand.clientWidth, cards: fan.children.length
    });
    if (request !== measuring || !board.isConnected) return;
    if (layout.board) {
      for (const [index, seat] of seats.entries()) place(seat, layout.board.seats[index]);
      place(board.querySelector(".center"), layout.board.center);
    }
    if (layout.hand) {
      fan.style.width = layout.hand.width + "px";
      fan.style.paddingLeft = layout.hand.first + "px";
      for (const [index, face] of [...fan.children].entries()) {
        face.style.position = "absolute";
        face.style.left = layout.hand.first + index * layout.hand.advance + "px";
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

function render() {
  if (!state.view) {
    menu();
    return;
  }
  const match = state.view;
  const content = element("section", "", "match");
  const heading = element("header");
  const labels = element("div");
  labels.append(element("h1", match.heading, "heading"), element("p", match.subtitle, "subtitle"));
  heading.append(labels, button("Meniu", menu, "menu"));

  const board = element("div", "", "board");
  for (const seat of match.seats) {
    const position = element("div", "", "seat " + seat.anchor + (seat.active ? " active" : ""));
    if (seat.active) position.setAttribute("aria-current", "true");
    position.append(element("strong", seat.name), element("span", seat.firstFact), element("span", seat.secondFact));
    board.append(position);
  }
  const visible = element("div", "", "cards-on-table");
  for (const face of match.center) visible.append(card(face));
  for (const play of match.played) {
    const tile = element("div", "", "played-card");
    tile.append(card(play.card), element("span", match.seats[play.seat].name));
    visible.append(tile);
  }
  const center = element("div", "", "center");
  center.append(visible);
  board.append(center);

  const controls = element("div", "", "controls");
  const prompt = element("p", match.prompt, "prompt");
  prompt.tabIndex = -1;
  const actions = element("div", "", "actions");
  const bids = match.actions.filter(move => move.key.startsWith("bid:"));
  if (bids.length) bidding(bids, actions);
  for (const move of match.actions.filter(move => !move.key.startsWith("bid:"))) {
    actions.append(control(move));
  }
  const toolbar = element("div", "", "toolbar");
  if (match.history.rows.length) toolbar.append(button("Istoric", () => showHistory(match.history)));
  controls.append(prompt, actions);
  center.append(controls);

  const hand = element("div", "", "hand");
  hand.setAttribute("role", "group");
  hand.setAttribute("aria-label", "Cărțile tale");
  hand.tabIndex = 0;
  const fan = element("div", "", "fan");
  for (const face of match.hand) fan.append(card(face, true));
  hand.append(fan);

  content.append(heading, board, toolbar, hand, errorBox);
  stage.replaceChildren(content);
  placeTable();
}

try {
  [setup, state] = await Promise.all([call("/api/setup"), call("/api/state")]);
  document.title = setup.title;
  render();
} catch (failure) {
  errorBox.textContent = failure.message;
  stage.append(errorBox);
} finally {
  stage.setAttribute("aria-busy", "false");
}

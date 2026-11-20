const rankingLimitKey = "whistlab:web:ranking-limit:v1";

export function preferredRankingLimit() {
  try { return localStorage.getItem(rankingLimitKey) === "10" ? 10 : 20; }
  catch { return 20; }
}

export function saveRankingLimit(value) {
  try { localStorage.setItem(rankingLimitKey, String(value)); } catch { /* The dialog still updates. */ }
}

export function historyGrid(data, element) {
  const grid = element("table", "", "record-history");
  if (data.appearance && Object.keys(data.appearance).length) applyHistoryAppearance(grid, data.appearance);
  const header = element("thead");
  const names = element("tr");
  const headings = element("tr");
  const cards = element("th", data.cardsLabel);
  cards.scope = "col";
  names.append(cards);
  headings.append(element("td"));
  for (const player of data.players) {
    const name = element("th", player, "history-seat");
    name.colSpan = 2;
    name.scope = "colgroup";
    names.append(name);
    for (const [label, style] of [[data.bidLabel, "history-seat"], [data.pointsLabel, ""]]) {
      const field = element("th", label, style);
      field.scope = "col";
      headings.append(field);
    }
  }
  header.append(names, headings);
  const body = element("tbody");
  for (const record of data.rows) {
    const row = element("tr");
    row.dataset.status = record.status.toLowerCase();
    const size = element("th", String(record.cards));
    size.scope = "row";
    row.append(size);
    for (const cell of record.cells) {
      const bid = element("td", "", "history-seat");
      bid.append(element("span", cell.bid, cell.missed ? "history-bid is-missed" : "history-bid"));
      const score = element("td", "", "history-score");
      score.setAttribute("aria-label", cell.description);
      const value = element("span", cell.total, "history-points");
      if (cell.series) value.dataset.series = cell.series > 0 ? "bonus" : "penalty";
      if (cell.total.startsWith("-")) value.classList.add("is-negative");
      score.append(value);
      row.append(bid, score);
    }
    body.append(row);
  }
  grid.append(header, body);
  return grid;
}

export function applyHistoryAppearance(grid, appearance) {
  const keys = ["--history-heading-weight", "--history-divider-width", "--history-divider-color"];
  if (Object.keys(appearance).length === 0) {
    for (const key of keys) grid.style.removeProperty(key);
    return;
  }
  if (Object.keys(appearance).length !== keys.length || !keys.every(key => typeof appearance[key] === "string")
      || !/^(400|700)$/.test(appearance[keys[0]]) || !/^[1-4]px$/.test(appearance[keys[1]])
      || !/^#[0-9a-f]{6}$/i.test(appearance[keys[2]])) {
    throw new TypeError("Invalid shared history appearance");
  }
  for (const key of keys) grid.style.setProperty(key, appearance[key]);
}

export function rankingPanel(data, element, copy) {
  const content = element("div", "", "rankings");
  const form = element("div", "", "ranking-options");
  function selector(label, choices) {
    const input = element("select");
    input.setAttribute("aria-label", label);
    for (const [key, name] of choices) {
      const option = element("option", name);
      option.value = key;
      input.append(option);
    }
    form.append(input);
    return input;
  }
  const profile = selector(copy("gameType", "Varianta de joc"), data.boards.map(board => [board.key, board.label]));
  profile.value = data.preferred;
  const limit = selector(copy("ranking", "Clasamente") + " · Top 10 / Top 20", [["10", "Top 10"], ["20", "Top 20"]]);
  limit.value = String(preferredRankingLimit());
  const scores = element("ol", "", "ranking-list");
  function render() {
    const board = data.boards.find(item => item.key === profile.value);
    const rows = board.entries.slice(0, Number(limit.value)).map(entry => {
      const row = element("li");
      row.append(element("span", entry.label), element("strong", String(entry.score)));
      return row;
    });
    scores.replaceChildren(...rows);
    scores.scrollTop = 0;
  }
  profile.addEventListener("change", render);
  limit.addEventListener("change", () => {
    saveRankingLimit(Number(limit.value));
    render();
  });
  content.append(element("p", copy("rankingHint", "")), form, scores);
  render();
  return content;
}

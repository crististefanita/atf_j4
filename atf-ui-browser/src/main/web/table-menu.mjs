/** Menu intentions shared by the Whist and Rentz renderers; games own their callbacks. */
export function historyFirst(label, history, others) {
  if (typeof label !== "string" || !label.trim() || typeof history !== "function") {
    throw new TypeError("A visible history menu entry requires a label and navigation action");
  }
  const items = [{ id: "score-history", label, action: history }, ...others];
  const ids = new Set();
  for (const item of items) {
    if (!item || typeof item.id !== "string" || !item.id || typeof item.action !== "function" || ids.has(item.id)) {
      throw new TypeError("Duplicate or invalid table menu intention");
    }
    ids.add(item.id);
  }
  return Object.freeze(items.map(item => Object.freeze({ ...item })));
}

/** Labels and order come from the native contract; each platform binds its own callbacks. */
export function menuFromEntries(routes, entries) {
  if (!Array.isArray(entries)) throw new TypeError("Menu entries must be an array");
  const actions = new Map(routes.map(route => [route.id, route]));
  const ids = new Set();
  return Object.freeze(entries.map(entry => {
    const route = actions.get(entry?.id);
    if (!route || typeof route.action !== "function" || ids.has(entry.id)
        || typeof entry.label !== "string" || !entry.label.trim()) {
      throw new TypeError("Duplicate, unsupported or invalid menu entry");
    }
    ids.add(entry.id);
    return Object.freeze({ ...route, label: entry.label });
  }));
}

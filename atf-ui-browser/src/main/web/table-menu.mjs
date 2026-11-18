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

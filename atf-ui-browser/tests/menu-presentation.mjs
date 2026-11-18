export function verifyHistoryMenu(build) {
  for (const label of ["Score history", "Istoricul scorului"]) {
    const calls = [];
    const items = build(label, () => calls.push("history"), [
      { id: "stop-auto", label: "Stop", action: () => calls.push("stop") },
      { id: "home", label: "Home", action: () => calls.push("home") }
    ]);
    if (items.map(item => item.id).join(",") !== "score-history,stop-auto,home" || items[0].label !== label) {
      throw Error("History must be one localized direct route before the other menu actions");
    }
    if (calls.length) throw Error("Building the menu must not navigate");
    items[0].action();
    if (calls.join(",") !== "history") throw Error("History emitted another route or multiple intentions");
    for (const operation of [
      () => build(label, null, []),
      () => build(label, () => {}, [{ id: "score-history", label, action() {} }]),
      () => items.push({ id: "other", label, action() {} })
    ]) {
      let refused = false;
      try { operation(); } catch { refused = true; }
      if (!refused) throw Error("Invalid or mutable menu plan was accepted");
    }
  }
  return { languages: 2, historyRoutes: 1, constructorActions: 0, selectedActions: 1, invalidPlansRefused: true };
}

export function verifyGameMenuDismissal(bind) {
  const item = {};
  const felt = {};
  const makeMenu = () => ({ open: true, contains: target => target === item });
  let menu = makeMenu();
  let click;
  bind({
    addEventListener(type, listener, options) {
      if (type !== "click" || !options.capture) throw Error("Expected a captured table click");
      click = listener;
    },
    querySelector(selector) {
      if (selector !== ".whist-navigation[open]") throw Error("Expected only the open game menu");
      return menu.open ? menu : null;
    }
  });
  click({ target: item });
  if (!menu.open) throw Error("An inside click closed the game menu");
  click({ target: felt });
  if (menu.open) throw Error("The menu stayed open after clicking the table");
  click({ target: felt });
  if (menu.open) throw Error("A second click reopened the game menu");
  menu = makeMenu();
  click({ target: felt });
  if (menu.open) throw Error("The replacement menu stayed open after redrawing");
  return { insideKeepsOpen: true, outsideDismisses: true, afterRerenderDismisses: true };
}

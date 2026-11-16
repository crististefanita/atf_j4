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

export function normalizedNameLength(value) {
  return Array.from(value.replace(/^[\x00-\x20]+|[\x00-\x20]+$/g, "").normalize("NFC")).length;
}

export function createProfileDialogs(element, button, submit) {
  const dialog = element("dialog");
  dialog.className = "player-dialog";
  document.body.append(dialog);

  function show(title, content, close) {
    const heading = element("h2", title);
    heading.id = "player-dialog-title";
    dialog.setAttribute("aria-labelledby", heading.id);
    const dismiss = button(close ?? "Închide", () => dialog.close());
    dialog.replaceChildren(heading, content, dismiss);
    if (!dialog.open) dialog.showModal();
    return dismiss;
  }

  function edit(name) {
    const form = element("form");
    form.className = "profile-form";
    const label = element("label", "Numele tău");
    const input = element("input");
    input.name = "name";
    input.value = name;
    input.autocomplete = "off";
    label.append(input);
    const hint = element("p", "Maximum 10 caractere.");
    const error = element("p");
    error.setAttribute("role", "alert");
    const save = element("button", "Salvează", "primary");
    save.type = "submit";
    input.addEventListener("input", () => {
      save.disabled = normalizedNameLength(input.value) > 10;
      error.textContent = save.disabled ? "Numele are mai mult de 10 caractere." : "";
    });
    form.append(label, hint, error, save);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      save.disabled = true;
      const failure = await submit("/api/profile", { name: input.value });
      if (failure) {
        error.textContent = failure;
        save.disabled = false;
      } else {
        dialog.close();
      }
    });
    show("Numele tău", form, "Anulează");
    input.focus();
    input.select();
  }

  function catalogue(data) {
    const body = element("div", "", "goals");
    body.append(element("p", data.summary));
    for (const group of data.groups) {
      const heading = element("h3", group.heading);
      body.append(heading);
      for (const goal of group.goals) body.append(goalRow(goal, true));
    }
    show("Realizări", body);
  }

  function goalRow(goal, collapsible) {
    const title = [goal.marker, goal.title, goal.progress].join(" ");
    if (collapsible) {
      const row = element("details");
      row.append(element("summary", title), element("p", goal.level + " · " + goal.description));
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
    show(data.title, body);
    dialog.addEventListener("close", () => submit("/api/notice", { dismiss: "yes" }), { once: true });
  }

  return { edit, catalogue, notice, dialog };
}

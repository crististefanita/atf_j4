package ro.whistlab.app.navigation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Owner-thread routes and dialog leases; an obsolete dismissal cannot resume a new screen. */
public final class AppNavigation {
    public enum Screen { HOME, SETUP, INSTRUCTIONS, GAME }
    public static final class Dialog {
        private final AppNavigation owner;
        private final long route;
        private final long id;
        private Dialog(AppNavigation owner, long route, long id) {
            this.owner = owner;
            this.route = route;
            this.id = id;
        }
    }

    private Screen screen = Screen.HOME;
    private long route;
    private long nextDialog;
    private final Set<Long> dialogs = new HashSet<>();

    public Screen screen() { return screen; }
    public boolean gameVisible() { return screen == Screen.GAME; }
    public boolean blocksAutomation() { return !dialogs.isEmpty(); }
    public boolean backReturnsHome() { return screen != Screen.HOME; }

    public void show(Screen target) {
        screen = Objects.requireNonNull(target);
        route++;
        dialogs.clear();
    }

    public Dialog openDialog() {
        long id = ++nextDialog;
        dialogs.add(id);
        return new Dialog(this, route, id);
    }

    /** True only for the last current dialog on a game screen, once. */
    public boolean dismiss(Dialog dialog) {
        Objects.requireNonNull(dialog);
        return dialog.owner == this && dialog.route == route && dialogs.remove(dialog.id)
                && dialogs.isEmpty() && gameVisible();
    }
}

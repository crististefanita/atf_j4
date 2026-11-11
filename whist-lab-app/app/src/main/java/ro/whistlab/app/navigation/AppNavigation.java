package ro.whistlab.app.navigation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Owner-thread routes and dialog leases; an obsolete dismissal cannot resume a new screen. */
public final class AppNavigation {
    public enum Screen { HOME, SETUP, INSTRUCTIONS, ACHIEVEMENTS, GAME }
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
    private final Runnable suspend;
    private final Runnable resume;

    public AppNavigation() { this(() -> { }, () -> { }); }

    public AppNavigation(Runnable suspend, Runnable resume) {
        this.suspend = Objects.requireNonNull(suspend);
        this.resume = Objects.requireNonNull(resume);
    }

    public Screen screen() { return screen; }
    public boolean gameVisible() { return screen == Screen.GAME; }
    public boolean blocksAutomation() { return !dialogs.isEmpty(); }
    public boolean backReturnsHome() { return screen != Screen.HOME; }

    public void show(Screen target) {
        Objects.requireNonNull(target);
        if (screen == Screen.GAME && target == screen) return;
        suspend.run();
        screen = target;
        route++;
        dialogs.clear();
    }

    public Dialog openDialog() {
        suspend.run();
        long id = ++nextDialog;
        dialogs.add(id);
        return new Dialog(this, route, id);
    }

    /** True only for the last current dialog on a game screen, once. */
    public boolean dismiss(Dialog dialog) {
        Objects.requireNonNull(dialog);
        boolean lastGameDialog = dialog.owner == this && dialog.route == route && dialogs.remove(dialog.id)
                && dialogs.isEmpty() && gameVisible();
        if (lastGameDialog) resume.run();
        return lastGameDialog;
    }
}

package ro.whistlab.app.achievements.presentation;

import java.util.ArrayList;
import java.util.List;
import ro.cardtable.ui.achievements.AchievementScreen;
import ro.cardtable.ui.screens.ScreenViews;
import ro.whistlab.app.achievements.WhistAchievementCatalog;
import ro.whistlab.app.achievements.WhistAchievementState;
import ro.whistlab.app.localization.WhistUiText;

/** Whist groups and eligibility on the shared achievement browser. */
public final class WhistAchievementsScreen {
    private WhistAchievementsScreen() { }

    public static void show(ScreenViews views, WhistUiText copy, WhistAchievementState progress,
            AchievementScreen.State screenState, Runnable back) {
        var catalogue = WhistAchievementPresentation.catalogue(progress, copy);
        var rows = new ArrayList<AchievementScreen.Row>();
        long nextId = 0;
        for (var group : catalogue.groups()) {
            rows.add(AchievementScreen.Row.heading(-group.number(), group.heading()));
            for (var goal : group.goals()) {
                rows.add(new AchievementScreen.Row(nextId++, goal.id(), goal.title(),
                        goal.level() + " · " + goal.progress(), goal.complete(),
                        () -> new AchievementScreen.Detail(goal.progress(), List.of(
                                new AchievementScreen.Section("", goal.description())))));
            }
        }
        var labels = new AchievementScreen.Copy(copy.achievements(), catalogue.summary(),
                copy.romanian() ? "Nicio realizare" : "No achievements",
                copy.romanian() ? "Parcurs de învățare" : "Learning path", copy.close());
        var screen = new AchievementScreen(views, screenState, labels, progress.earnedCount(),
                WhistAchievementCatalog.goals().size(), List.of(), new AchievementScreen.Action(copy.back(), back));
        screen.update(rows, "");
        screen.restoreDetail();
    }
}

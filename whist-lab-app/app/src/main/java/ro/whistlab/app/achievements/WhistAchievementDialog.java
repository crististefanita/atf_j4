package ro.whistlab.app.achievements;

import android.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import ro.cardtable.ui.achievements.AchievementNoticeView;
import ro.cardtable.ui.screens.ScreenViews;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;
import ro.whistlab.app.localization.WhistUiText;

/** Translates the durable Whist notice without changing its acknowledgement. */
public final class WhistAchievementDialog {
    private WhistAchievementDialog() { }

    public static AlertDialog showUnlocked(ScreenViews views, WhistUiText copy,
            List<WhistAchievementCatalog.Goal> unlocked, WhistAchievementState state,
            int handNumber, boolean matchComplete) {
        var notice = WhistAchievementPresentation.notice(unlocked, state, handNumber, matchComplete, copy);
        var lines = new ArrayList<String>();
        lines.add(notice.summary());
        for (var goal : notice.goals()) lines.add(goal.title() + " · " + goal.progress());
        return AchievementNoticeView.dialog(views, notice.title(), lines, copy.close());
    }
}

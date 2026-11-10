package ro.whistlab.app.achievements;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.util.List;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;

/** Afișează progresul local în capitole de câte cinci obiective, toate accesibile simultan. */
public final class WhistAchievementDialog {
    private WhistAchievementDialog() { }

    public static AlertDialog show(Context context, WhistUiText copy, WhistAchievementRepository repository)
            throws IOException {
        WhistAchievementPresentation.Catalogue catalogue =
                WhistAchievementPresentation.catalogue(repository.state(), copy);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 16);
        content.setPadding(padding, dp(context, 8), padding, 0);
        TextView summary = label(context, catalogue.summary(), 13, false);
        summary.setPadding(0, 0, 0, dp(context, 10));
        content.addView(summary);

        ScrollView scroll = new ScrollView(context);
        LinearLayout groups = new LinearLayout(context);
        groups.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(groups);
        for (var group : catalogue.groups()) {
            TextView heading = label(context, group.heading(), 15, true);
            heading.setPadding(0, dp(context, 10), 0, dp(context, 4));
            groups.addView(heading);
            for (var goal : group.goals()) {
                TextView title = label(context, goal.marker() + "  " + goal.level() + " · "
                        + goal.title() + "  " + goal.progress(), 14, goal.complete());
                title.setGravity(Gravity.CENTER_VERTICAL);
                title.setPadding(dp(context, 4), dp(context, 5), 0, dp(context, 2));
                groups.addView(title);
                TextView detail = label(context, goal.description(), 12, false);
                detail.setPadding(dp(context, 24), 0, 0, dp(context, 5));
                groups.addView(detail);
            }
        }
        content.addView(scroll, new LinearLayout.LayoutParams(-1,
                Math.max(dp(context, 180), (int) (context.getResources().getDisplayMetrics().heightPixels * .62f))));
        return new AlertDialog.Builder(context).setTitle(copy.achievements()).setView(content)
                .setPositiveButton(copy.close(), null).show();
    }

    public static AlertDialog showUnlocked(Context context, WhistUiText copy,
            List<WhistAchievementCatalog.Goal> unlocked, WhistAchievementState state,
            int handNumber, boolean matchComplete) {
        WhistAchievementPresentation.Notice notice =
                WhistAchievementPresentation.notice(unlocked, state, handNumber, matchComplete, copy);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 20);
        content.setPadding(padding, dp(context, 8), padding, 0);
        TextView summary = label(context, notice.summary(), 14, true);
        summary.setPadding(0, 0, 0, dp(context, 12));
        content.addView(summary);

        for (var goal : notice.goals()) {
            TextView title = label(context, goal.marker() + "  " + goal.level() + " · "
                    + goal.title() + "  " + goal.progress(), 16, true);
            title.setPadding(0, dp(context, 6), 0, dp(context, 3));
            content.addView(title);
            TextView detail = label(context, goal.description(), 13, false);
            detail.setPadding(dp(context, 24), 0, 0, dp(context, 8));
            content.addView(detail);
        }
        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,
                Math.max(dp(context, 150), (int) (context.getResources().getDisplayMetrics().heightPixels * .55f))));
        return new AlertDialog.Builder(context).setTitle(notice.title()).setView(scroll)
                .setPositiveButton(copy.close(), null).create();
    }

    private static TextView label(Context context, String text, int size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(0xff33423a);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + .5f);
    }
}

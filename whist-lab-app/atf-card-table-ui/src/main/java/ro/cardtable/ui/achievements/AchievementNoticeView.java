package ro.cardtable.ui.achievements;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.List;
import ro.cardtable.ui.screens.ScreenViews;

/** Shared progress card. A game owns when it appears and how it is acknowledged. */
public final class AchievementNoticeView {
    static final int SURFACE = 0xFF315E4D;
    private static final int OUTLINE = 0xFF629478;

    private AchievementNoticeView() { }

    public static View show(ScreenViews views, FrameLayout slot, String title, List<String> lines,
            String close, String closeDescription, Runnable dismiss) {
        var card = card(views, title, lines);
        var button = views.actionButton(close, closeDescription, true, dismiss, false, 12);
        button.setContentDescription(closeDescription);
        card.addView(button, views.matchWrap());
        var layout = new FrameLayout.LayoutParams(-1, -2);
        layout.setMargins(views.dp(12), 0, views.dp(12), views.dp(8));
        slot.addView(card, layout);
        card.post(() -> { if (card.isAttachedToWindow()) card.announceForAccessibility(title); });
        return card;
    }

    public static AlertDialog dialog(ScreenViews views, String title, List<String> lines, String close) {
        var scroll = new ScrollView(views.activity());
        scroll.addView(card(views, title, lines));
        return new AlertDialog.Builder(views.activity()).setView(scroll).setPositiveButton(close, null).create();
    }

    private static LinearLayout card(ScreenViews views, String title, List<String> lines) {
        var card = new LinearLayout(views.activity());
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = views.dp(16);
        card.setPadding(padding, views.dp(10), padding, views.dp(8));
        card.setBackground(views.roundedBackground(SURFACE, OUTLINE, 16));
        views.addText(card, title, 15, Color.WHITE, true, 0, 3);
        for (String line : lines) views.addText(card, line, 13, Color.WHITE, false, 2, 0);
        card.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        return card;
    }
}

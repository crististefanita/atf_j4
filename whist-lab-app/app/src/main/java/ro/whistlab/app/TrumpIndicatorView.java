package ro.whistlab.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.Suit;

/** Persistent, accessible trump tile kept in the game header above the table. */
final class TrumpIndicatorView extends LinearLayout {
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int IVORY = Color.rgb(255, 254, 248);
    private static final int HEART_RED = Color.rgb(255, 177, 177);

    TrumpIndicatorView(Context context, GameSession.Snapshot state, WhistUiText copy) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(density, 6), dp(density, 3), dp(density, 6), dp(density, 3));
        setBackground(background(density));
        setTag("trump-indicator");
        setContentDescription(copy.trumpAccessibility(state));
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        TextView heading = new TextView(context);
        heading.setText(copy.trumpHeading());
        heading.setTextSize(9);
        heading.setTextColor(Color.rgb(211, 239, 226));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView value = new TextView(context);
        value.setText(trumpValue(state));
        value.setTextSize(22);
        value.setTextColor(isRed(state) ? HEART_RED : IVORY);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(value, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView status = new TextView(context);
        status.setText(copy.trumpIndicatorStatus(state));
        status.setTextSize(6);
        status.setTextColor(Color.rgb(211, 239, 226));
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(true);
        status.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(status, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private String trumpValue(GameSession.Snapshot state) {
        Card turned = state.turnedCard();
        if (turned != null) return turned.shortLabel();
        return state.trump().suit().map(Suit::symbol).orElse("—");
    }

    private boolean isRed(GameSession.Snapshot state) {
        Card turned = state.turnedCard();
        return turned != null ? turned.suit().red()
                : state.trump().suit().map(Suit::red).orElse(false);
    }

    private static GradientDrawable background(float density) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(28, 91, 67));
        shape.setCornerRadius(dp(density, 13));
        shape.setStroke(dp(density, 1), GOLD);
        return shape;
    }

    private static int dp(float density, int value) {
        return Math.round(density * value);
    }
}

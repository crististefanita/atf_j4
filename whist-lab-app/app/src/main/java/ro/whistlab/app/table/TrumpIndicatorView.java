package ro.whistlab.app.table;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.theme.WhistUiPalette;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.Suit;

/** Persistent, accessible trump tile kept in the game header above the table. */
public final class TrumpIndicatorView extends LinearLayout {
    private static final int IVORY = Color.rgb(255, 254, 248);
    private static final int HEART_RED = Color.rgb(255, 177, 177);
    private static final int DARK_SUIT_RED = Color.rgb(157, 45, 54);

    public TrumpIndicatorView(Context context, GameSession.Snapshot state, WhistUiText copy,
            boolean currentPlayerHighlighted) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setMinimumWidth(dp(density, 76));
        setMinimumHeight(dp(density, 48));
        setGravity(Gravity.CENTER);
        setPadding(dp(density, 6), dp(density, 4), dp(density, 6), dp(density, 4));
        setBackground(WhistUiPalette.seatFrame(context, currentPlayerHighlighted));
        setTag("trump-indicator");
        setContentDescription(copy.trumpAccessibility(state));
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        TextView heading = new TextView(context);
        heading.setText(copy.trumpHeading());
        heading.setTextSize(9);
        heading.setTextColor(frameText(currentPlayerHighlighted));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(heading, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        TextView value = new TextView(context);
        value.setText(trumpValue(state));
        value.setTextSize(20);
        if (state.turnedCard() != null) {
            GradientDrawable card = new GradientDrawable();
            card.setColor(IVORY);
            card.setCornerRadius(dp(density, 5));
            value.setBackground(card);
            value.setPadding(dp(density, 7), 0, dp(density, 7), 0);
        }
        value.setTextColor(valueColor(state, currentPlayerHighlighted));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams valueParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dp(density, 2);
        addView(value, valueParams);

        TextView status = new TextView(context);
        status.setText(copy.trumpIndicatorStatus(state));
        status.setTextSize(9);
        status.setTextColor(frameText(currentPlayerHighlighted));
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(true);
        status.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (state.turnedCard() != null) status.setVisibility(GONE);
        addView(status, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    private String trumpValue(GameSession.Snapshot state) {
        Card turned = state.turnedCard();
        if (turned != null) return turned.shortLabel();
        return state.trump().suit().map(Suit::symbol).orElse("—");
    }

    private static boolean isRed(GameSession.Snapshot state) {
        Card turned = state.turnedCard();
        return turned != null ? turned.suit().red()
                : state.trump().suit().map(Suit::red).orElse(false);
    }

    private static int frameText(boolean active) {
        return active ? WhistUiPalette.ACTIVE_SEAT_TEXT : WhistUiPalette.IDLE_SEAT_TEXT;
    }

    private static int valueColor(GameSession.Snapshot state, boolean active) {
        if (state.turnedCard() != null) {
            return isRed(state) ? DARK_SUIT_RED : WhistUiPalette.ACTIVE_SEAT_TEXT;
        }
        if (!active) return isRed(state) ? HEART_RED : IVORY;
        return isRed(state) ? DARK_SUIT_RED : WhistUiPalette.ACTIVE_SEAT_TEXT;
    }

    private static int dp(float density, int value) {
        return Math.round(density * value);
    }
}

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
    private final TextView heading;
    private final TextView value;
    private static final int IVORY = Color.rgb(255, 254, 248);
    private static final int HEART_RED = Color.rgb(255, 177, 177);
    private static final int DARK_SUIT_RED = Color.rgb(157, 45, 54);

    public TrumpIndicatorView(Context context, GameSession.Snapshot state, WhistUiText copy,
            boolean currentPlayerHighlighted) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setMinimumWidth(dp(density, TableLayoutMetrics.TRUMP_WIDTH_DP));
        setMinimumHeight(dp(density, TableLayoutMetrics.TRUMP_MIN_HEIGHT_DP));
        setGravity(Gravity.CENTER);
        int padding = dp(density, TableLayoutMetrics.TRUMP_PADDING_DP);
        setPadding(padding, padding, padding, padding);
        setBackground(WhistUiPalette.seatFrame(context, currentPlayerHighlighted));
        setTag("trump-indicator");
        setContentDescription(copy.trumpAccessibility(state));
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        heading = new TextView(context);
        heading.setText(copy.trumpHeading());
        heading.setTextSize(9);
        heading.setTextColor(frameText(currentPlayerHighlighted));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setSingleLine(true);
        heading.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(heading, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        value = new TextView(context);
        value.setText(copy.trumpIndicatorValue(state));
        value.setTextSize(20);
        if (state.turnedCard() != null) {
            GradientDrawable card = new GradientDrawable();
            card.setColor(IVORY);
            card.setCornerRadius(dp(density, 5));
            value.setBackground(card);
            value.setPadding(dp(density, 5), 0, dp(density, 5), 0);
        }
        value.setTextColor(valueColor(state, currentPlayerHighlighted));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams valueParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dp(density, TableLayoutMetrics.TRUMP_VALUE_GAP_DP);
        addView(value, valueParams);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        var metrics = getResources().getDisplayMetrics();
        int width = resolveSize(dp(metrics.density, TableLayoutMetrics.TRUMP_WIDTH_DP), widthSpec);
        int headingHeight = fitLine(heading, 9 * metrics.scaledDensity, width);
        int valueHeight = fitLine(value, 20 * metrics.scaledDensity, width);
        int height = resolveSize(TableLayoutMetrics.trumpHeightPx(metrics.density,
                headingHeight, valueHeight), heightSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private int fitLine(TextView line, float nominalSize, int width) {
        android.graphics.Paint paint = new android.graphics.Paint(line.getPaint());
        paint.setTextSize(nominalSize);
        android.graphics.Paint.FontMetricsInt font = paint.getFontMetricsInt();
        int height = font.bottom - font.top;
        int available = width - getPaddingLeft() - getPaddingRight()
                - line.getPaddingLeft() - line.getPaddingRight();
        line.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                TableLayoutMetrics.fitTextSizePx(nominalSize,
                        paint.measureText(line.getText().toString()), available));
        line.getLayoutParams().height = height;
        return height;
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

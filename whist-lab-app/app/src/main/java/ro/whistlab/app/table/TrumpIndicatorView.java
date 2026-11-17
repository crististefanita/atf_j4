package ro.whistlab.app.table;

import ro.cardtable.ui.table.TrumpTileMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.theme.TrumpTileStyle;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.Suit;

/** Persistent, accessible trump tile kept in the game header above the table. */
public final class TrumpIndicatorView extends LinearLayout {
    private final TextView heading;
    private final TextView value;

    public TrumpIndicatorView(Context context, GameSession.Snapshot state, WhistUiText copy) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setMinimumWidth(dp(density, TrumpTileMetrics.WIDTH_DP));
        setMinimumHeight(dp(density, TrumpTileMetrics.MIN_HEIGHT_DP));
        setGravity(Gravity.CENTER);
        int padding = dp(density, TrumpTileMetrics.PADDING_DP);
        setPadding(padding, padding, padding, padding);
        var frame = new GradientDrawable();
        frame.setColor(TrumpTileStyle.FILL);
        frame.setStroke(dp(density, TrumpTileStyle.FRAME_STROKE_DP), TrumpTileStyle.LINE);
        frame.setCornerRadius(dp(density, TrumpTileStyle.FRAME_RADIUS_DP));
        setBackground(frame);
        setTag("trump-indicator");
        setContentDescription(copy.trumpAccessibility(state));
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        heading = new TextView(context);
        heading.setText(copy.trumpHeading());
        heading.setTextSize(TrumpTileMetrics.HEADING_SP);
        heading.setTextColor(TrumpTileStyle.INK);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        heading.setSingleLine(true);
        heading.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(heading, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        value = new TextView(context);
        value.setText(copy.trumpIndicatorValue(state));
        value.setTextSize(TrumpTileMetrics.VALUE_SP);
        if (state.turnedCard() != null) {
            GradientDrawable card = new GradientDrawable();
            card.setColor(TrumpTileStyle.CARD_FILL);
            card.setCornerRadius(dp(density, TrumpTileMetrics.CARD_RADIUS_DP));
            value.setBackground(card);
            int inset = dp(density, TrumpTileMetrics.CARD_INSET_DP);
            value.setPadding(inset, 0, inset, 0);
        }
        Suit suit = state.turnedCard() == null ? state.trump().suit().orElse(null) : state.turnedCard().suit();
        value.setTextColor(TrumpTileStyle.valueInk(suit));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams valueParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dp(density, TrumpTileMetrics.VALUE_GAP_DP);
        addView(value, valueParams);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        var metrics = getResources().getDisplayMetrics();
        int width = resolveSize(dp(metrics.density, TrumpTileMetrics.WIDTH_DP), widthSpec);
        int headingHeight = fitLine(heading, TrumpTileMetrics.HEADING_SP * metrics.scaledDensity, width);
        int valueHeight = fitLine(value, TrumpTileMetrics.VALUE_SP * metrics.scaledDensity, width);
        int height = resolveSize(TrumpTileMetrics.heightPx(metrics.density,
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
                TrumpTileMetrics.fitTextSizePx(nominalSize,
                        paint.measureText(line.getText().toString()), available));
        line.getLayoutParams().height = height;
        return height;
    }

    private static int dp(float density, int value) {
        return Math.round(density * value);
    }
}

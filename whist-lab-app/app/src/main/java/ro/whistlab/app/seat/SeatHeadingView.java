package ro.whistlab.app.seat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.app.theme.WhistUiPalette;

/** Keep the complete turn action visible; a narrow frame stacks it below the name. */
public final class SeatHeadingView extends LinearLayout {
    private final TextView name;
    private final TextView badge;

    public SeatHeadingView(Context context, String playerName, String action, boolean active) {
        super(context);
        setGravity(Gravity.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        name = label(playerName, 13, active ? WhistUiPalette.ACTIVE_SEAT_TEXT
                : WhistUiPalette.IDLE_SEAT_TEXT);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        badge = label(action, 9, WhistUiPalette.ACTIVE_SEAT_TEXT);
        badge.setTag("seat-turn-badge");
        badge.setPadding(dp(4), dp(1), dp(4), dp(1));
        badge.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(220, 181, 105));
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), Color.rgb(105, 83, 45));
        badge.setBackground(shape);
        // Reserve the same measured space while the turn moves to another seat.
        badge.setVisibility(active ? VISIBLE : INVISIBLE);
        addView(name, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(badge, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int nameWidth = (int) Math.ceil(name.getPaint().measureText(name.getText().toString()));
        int badgeWidth = (int) Math.ceil(badge.getPaint().measureText(badge.getText().toString()))
                + badge.getPaddingLeft() + badge.getPaddingRight();
        int width = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? nameWidth + badgeWidth + dp(4) : MeasureSpec.getSize(widthSpec);
        var plan = SeatHeadingMetrics.arrange(Math.max(1, width), nameWidth, badgeWidth, dp(4), dp(28));
        setOrientation(plan.inline() ? HORIZONTAL : VERTICAL);
        LayoutParams nameParams = (LayoutParams) name.getLayoutParams();
        nameParams.width = plan.nameWidthPx();
        LayoutParams badgeParams = (LayoutParams) badge.getLayoutParams();
        badgeParams.width = plan.badgeWidthPx();
        badgeParams.leftMargin = plan.gapPx();
        badgeParams.topMargin = plan.inline() ? 0 : dp(2);
        // No fixed height or line limit: Android measures wrapped action text in full.
        super.onMeasure(widthSpec, heightSpec);
    }

    private TextView label(String text, int sizeSp, int color) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

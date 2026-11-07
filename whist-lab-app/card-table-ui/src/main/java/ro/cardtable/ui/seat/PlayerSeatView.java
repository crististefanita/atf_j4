package ro.cardtable.ui.seat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

/** Action text has its own measured region above the player's framed information. */
public class PlayerSeatView extends ViewGroup {
    private final TextView action;
    private final LinearLayout frame;
    private final boolean compact;
    private PlayerSeatMetrics.Plan plan;

    public PlayerSeatView(Context context, LinearLayout frame, String label, boolean active, int seat,
            boolean compact) {
        super(context);
        this.frame = frame;
        this.compact = compact;
        setTag("table-seat-region:" + seat);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        action = new TextView(context);
        action.setText(label);
        action.setTextSize(10);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setTextColor(Color.rgb(255, 220, 150));
        action.setGravity(Gravity.CENTER);
        action.setIncludeFontPadding(false);
        action.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        action.setTag("seat-turn-badge:" + seat);
        action.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        // INVISIBLE preserves the action region and stops the frames jumping between turns.
        action.setVisibility(active ? VISIBLE : INVISIBLE);
        addView(action);
        addView(frame);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int exactWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int naturalHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        action.measure(exactWidth, naturalHeight);
        frame.measure(exactWidth, naturalHeight);
        int reservedHeight = MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(heightSpec) : 0;
        int nameHeight = frame.getChildAt(0).getMeasuredHeight();
        int headingHeight = compact ? dp(PlayerSeatMetrics.COMPACT_NAME_REGION_DP)
                : nameHeight + dp(PlayerSeatMetrics.ROOMY_HEADING_RESERVE_DP);
        int referenceHeight = PlayerSeatMetrics.referenceFrameHeightPx(
                frame.getMeasuredHeight(), nameHeight, headingHeight);
        plan = PlayerSeatMetrics.arrange(referenceHeight, action.getMeasuredHeight(), dp(3), reservedHeight);
        frame.measure(exactWidth, MeasureSpec.makeMeasureSpec(plan.frameHeightPx(), MeasureSpec.EXACTLY));
        setMeasuredDimension(width, resolveSize(plan.heightPx(), heightSpec));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        action.layout(0, 0, right - left, plan.badgeHeightPx());
        frame.layout(0, plan.frameTopPx(), right - left, plan.frameTopPx() + plan.frameHeightPx());
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }
}

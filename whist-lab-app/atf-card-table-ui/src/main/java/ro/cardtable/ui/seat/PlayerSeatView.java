package ro.cardtable.ui.seat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

/** Action text has its own measured region above the player's framed information. */
public class PlayerSeatView extends ViewGroup {
    private final TextView action;
    private final LinearLayout frame;
    private final boolean compact;
    private final int reservedBadgeHeightPx;
    private PlayerSeatMetrics.Plan plan;

    public PlayerSeatView(Context context, LinearLayout frame, String label, boolean active, int seat,
            boolean compact) {
        this(context, frame, label, active, seat, compact, 0);
    }

    /** Reserves a common badge slot so phase-specific text cannot move the seat frame. */
    public PlayerSeatView(Context context, LinearLayout frame, String label, boolean active, int seat,
            boolean compact, int reservedBadgeHeightPx) {
        super(context);
        if (reservedBadgeHeightPx < 0) throw new IllegalArgumentException("Reserved badge height cannot be negative");
        this.frame = frame;
        this.compact = compact;
        this.reservedBadgeHeightPx = reservedBadgeHeightPx;
        setTag("table-seat-region:" + seat);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        action = actionView(context, label);
        action.setTag("seat-turn-badge:" + seat);
        action.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        // INVISIBLE preserves the action region and stops the frames jumping between turns.
        action.setVisibility(active ? VISIBLE : INVISIBLE);
        addView(action);
        addView(frame);
    }

    /** Measures the action with the same text settings used by the rendered seat. */
    public static int measureActionHeightPx(Context context, String label, int widthPx) {
        if (widthPx < 1) throw new IllegalArgumentException("Badge width must be positive");
        TextView measured = actionView(context, label);
        measured.measure(MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return measured.getMeasuredHeight();
    }

    private static TextView actionView(Context context, String label) {
        TextView action = new TextView(context);
        action.setText(label);
        action.setTextSize(10);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setTextColor(Color.rgb(255, 220, 150));
        action.setGravity(Gravity.CENTER);
        action.setIncludeFontPadding(false);
        action.setSingleLine(false);
        action.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        action.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        return action;
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
        int badgeHeight = Math.max(action.getMeasuredHeight(), reservedBadgeHeightPx);
        plan = PlayerSeatMetrics.arrange(referenceHeight, badgeHeight, dp(3), reservedHeight);
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

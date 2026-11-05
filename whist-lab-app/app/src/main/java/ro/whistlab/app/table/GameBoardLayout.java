package ro.whistlab.app.table;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Owns all occupied table regions so controls cannot silently cover a player's seat. */
final class GameBoardLayout extends ViewGroup {
    private final int players;
    private final int centerWidthDp;
    private final int minimumHeightDp;
    private final int regionGapDp;
    private final boolean compact;
    private final boolean centerUsesFullWidth;
    private final TableGeometry.CenterAlignment centerAlignment;
    private int seatWidth;
    private int seatHeight;

    GameBoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean centerUsesFullWidth, TableGeometry.CenterAlignment centerAlignment) {
        super(context);
        this.players = players;
        this.centerWidthDp = centerWidthDp;
        this.minimumHeightDp = viewport.minimumBoardHeightDp();
        this.regionGapDp = viewport.regionGapDp();
        this.compact = viewport.isCompact();
        this.centerUsesFullWidth = centerUsesFullWidth;
        this.centerAlignment = centerAlignment;
        setClipChildren(false);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int gap = dp(regionGapDp);
        seatWidth = dp(TableLayoutMetrics.seatWidthDp(
                Math.round(width / getResources().getDisplayMetrics().density), compact));
        seatHeight = 0;
        for (int index = 0; index < players; index++) {
            View seat = getChildAt(index);
            seat.measure(MeasureSpec.makeMeasureSpec(seatWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            seatHeight = Math.max(seatHeight, seat.getMeasuredHeight());
        }
        for (int index = 0; index < players; index++) {
            getChildAt(index).measure(MeasureSpec.makeMeasureSpec(seatWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(seatHeight, MeasureSpec.EXACTLY));
        }
        View center = getChildAt(players);
        int measuredCenterWidth = centerUsesFullWidth ? width : Math.min(width, dp(centerWidthDp));
        center.measure(MeasureSpec.makeMeasureSpec(measuredCenterWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int minimum = Math.max(dp(minimumHeightDp),
                TableGeometry.minimumHeight(players, seatHeight, center.getMeasuredHeight(), gap));
        setMeasuredDimension(width, Math.max(minimum, resolveSize(minimum, heightSpec)));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View center = getChildAt(players);
        var plan = TableGeometry.arrangeDisplay(right - left, bottom - top, players, seatWidth, seatHeight,
                dp(centerWidthDp), center.getMeasuredHeight(), dp(regionGapDp), centerUsesFullWidth,
                centerAlignment);
        for (int index = 0; index < players; index++) place(getChildAt(index), plan.seats().get(index));
        if (centerUsesFullWidth) {
            place(center, new TableGeometry.Rect(0, plan.center().top(), right - left,
                    plan.center().bottom()));
        } else {
            place(center, plan.center());
        }
    }

    private static void place(View view, TableGeometry.Rect box) {
        view.layout(box.left(), box.top(), box.right(), box.bottom());
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
}

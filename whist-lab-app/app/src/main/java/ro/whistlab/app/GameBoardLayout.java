package ro.whistlab.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Owns all occupied table regions so controls cannot silently cover a player's seat. */
final class GameBoardLayout extends ViewGroup {
    private final int players;
    private final int centerWidthDp;
    private final int minimumHeightDp;
    private final int regionGapDp;
    private int seatWidth;
    private int seatHeight;

    GameBoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport) {
        super(context);
        this.players = players;
        this.centerWidthDp = centerWidthDp;
        this.minimumHeightDp = viewport.minimumBoardHeightDp();
        this.regionGapDp = viewport.regionGapDp();
        setClipChildren(false);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int gap = dp(regionGapDp);
        seatWidth = dp(TableLayoutMetrics.seatWidthDp(Math.round(width / getResources().getDisplayMetrics().density)));
        seatHeight = 0;
        for (int index = 0; index < players; index++) {
            View seat = getChildAt(index);
            seat.measure(MeasureSpec.makeMeasureSpec(seatWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            seatHeight = Math.max(seatHeight, seat.getMeasuredHeight());
        }
        View center = getChildAt(players);
        center.measure(MeasureSpec.makeMeasureSpec(Math.min(width, dp(centerWidthDp)), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int minimum = Math.max(dp(minimumHeightDp),
                TableGeometry.minimumHeight(players, seatHeight, center.getMeasuredHeight(), gap));
        setMeasuredDimension(width, Math.max(minimum, resolveSize(minimum, heightSpec)));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View center = getChildAt(players);
        var plan = TableGeometry.arrange(right - left, bottom - top, players, seatWidth, seatHeight,
                center.getMeasuredWidth(), center.getMeasuredHeight(), dp(regionGapDp));
        for (int index = 0; index < players; index++) place(getChildAt(index), plan.seats().get(index));
        place(center, plan.center());
    }

    private static void place(View view, TableGeometry.Rect box) {
        view.layout(box.left(), box.top(), box.right(), box.bottom());
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
}

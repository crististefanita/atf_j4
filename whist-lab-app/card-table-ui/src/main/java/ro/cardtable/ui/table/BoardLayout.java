package ro.cardtable.ui.table;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Owns all occupied table regions so controls cannot silently cover a player's seat. */
public class BoardLayout extends ViewGroup {
    private final int players;
    private final int centerWidthDp;
    private final int minimumHeightDp;
    private final int regionGapDp;
    private final boolean compact;
    private final boolean centerUsesFullWidth;
    private final int reservedCenterHeightDp;
    private final boolean stableSeatAnchors;
    private final int requestedHandClearanceDp;
    private final TableGeometry.CenterAlignment centerAlignment;
    private int seatWidth;
    private int seatHeight;

    public BoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean centerUsesFullWidth, TableGeometry.CenterAlignment centerAlignment) {
        this(context, players, centerWidthDp, viewport, centerUsesFullWidth, 0, false, centerAlignment);
    }

    /** Reserves a phase-independent center band and optional play-phase seat anchors. */
    public BoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean centerUsesFullWidth, int reservedCenterHeightDp, boolean stableSeatAnchors,
            TableGeometry.CenterAlignment centerAlignment) {
        this(context, players, centerWidthDp, viewport, centerUsesFullWidth, reservedCenterHeightDp,
                stableSeatAnchors, centerAlignment, 0);
    }

    /** Keeps a requested gap above the player's hand when the measured board has spare height. */
    public BoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean centerUsesFullWidth, int reservedCenterHeightDp, boolean stableSeatAnchors,
            TableGeometry.CenterAlignment centerAlignment, int requestedHandClearanceDp) {
        super(context);
        if (reservedCenterHeightDp < 0 || requestedHandClearanceDp < 0) {
            throw new IllegalArgumentException("Reserved table dimensions cannot be negative");
        }
        this.players = players;
        this.centerWidthDp = centerWidthDp;
        this.minimumHeightDp = viewport.minimumBoardHeightDp();
        this.regionGapDp = viewport.regionGapDp();
        this.compact = viewport.isCompact();
        this.centerUsesFullWidth = centerUsesFullWidth;
        this.reservedCenterHeightDp = reservedCenterHeightDp;
        this.stableSeatAnchors = stableSeatAnchors;
        this.requestedHandClearanceDp = requestedHandClearanceDp;
        this.centerAlignment = centerAlignment;
        setClipChildren(false);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int gap = dp(regionGapDp);
        seatWidth = dp(TableSizing.seatWidthDp(
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
        int geometryCenterHeight = Math.max(center.getMeasuredHeight(), dp(reservedCenterHeightDp));
        int minimum = Math.max(dp(minimumHeightDp),
                TableGeometry.minimumHeight(players, seatHeight, geometryCenterHeight, gap));
        setMeasuredDimension(width, Math.max(minimum, resolveSize(minimum, heightSpec)));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View center = getChildAt(players);
        int geometryCenterHeight = Math.max(center.getMeasuredHeight(), dp(reservedCenterHeightDp));
        var plan = TableGeometry.arrangeDisplay(right - left, bottom - top, players, seatWidth, seatHeight,
                dp(centerWidthDp), geometryCenterHeight, dp(regionGapDp),
                stableSeatAnchors || centerUsesFullWidth,
                centerAlignment, dp(requestedHandClearanceDp));
        for (int index = 0; index < players; index++) place(getChildAt(index), plan.seats().get(index));
        int centerLeft = centerUsesFullWidth ? 0 : plan.center().left();
        int centerTop = plan.center().top() + (geometryCenterHeight - center.getMeasuredHeight()) / 2;
        place(center, new TableGeometry.Rect(centerLeft, centerTop,
                centerLeft + center.getMeasuredWidth(), centerTop + center.getMeasuredHeight()));
    }

    private static void place(View view, TableGeometry.Rect box) {
        view.layout(box.left(), box.top(), box.right(), box.bottom());
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
}

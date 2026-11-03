package ro.whistlab.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Places each player at a named table perimeter anchor without moving the human seat. */
final class TableSeatRingLayout extends ViewGroup {
    private final TableSeatLayout.Anchor[] anchors;
    private final boolean bidding;
    private final int bidOptionCount;
    private final int seatWidthPx;
    private final int tableWidthDp;

    TableSeatRingLayout(Context context, int playerCount, boolean bidding, int bidOptionCount,
            int seatWidthPx, int tableWidthDp) {
        super(context);
        this.anchors = TableSeatLayout.positions(playerCount, bidding, bidOptionCount);
        this.bidding = bidding;
        this.bidOptionCount = bidOptionCount;
        this.seatWidthPx = Math.max(1, seatWidthPx);
        this.tableWidthDp = Math.max(1, tableWidthDp);
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        int childWidth = Math.min(width, seatWidthPx);
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        for (int index = 0; index < getChildCount(); index++) {
            getChildAt(index).measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() != anchors.length) {
            throw new IllegalStateException("Expected " + anchors.length + " player seats, found " + getChildCount());
        }

        int width = right - left;
        int height = bottom - top;
        int sideInset = Math.round(height * TableSeatLayout.sideInsetFraction(
                bidding, anchors.length, bidOptionCount, tableWidthDp));
        for (int index = 0; index < anchors.length; index++) {
            View seat = getChildAt(index);
            int seatWidth = seat.getMeasuredWidth();
            int seatHeight = seat.getMeasuredHeight();
            int seatLeft = switch (horizontalAlignment(anchors[index])) {
                case START -> 0;
                case CENTER -> (width - seatWidth) / 2;
                case END -> width - seatWidth;
            };
            int seatTop = switch (anchors[index]) {
                case NORTH -> 0;
                case NORTH_WEST, NORTH_EAST -> sideInset;
                case WEST, EAST -> (height - seatHeight) / 2;
                case SOUTH_WEST, SOUTH_EAST -> height - seatHeight - sideInset;
                case SOUTH -> height - seatHeight;
            };
            seat.layout(seatLeft, seatTop, seatLeft + seatWidth, seatTop + seatHeight);
        }
    }

    private static HorizontalAlignment horizontalAlignment(TableSeatLayout.Anchor anchor) {
        return switch (anchor) {
            case NORTH_WEST, WEST, SOUTH_WEST -> HorizontalAlignment.START;
            case NORTH, SOUTH -> HorizontalAlignment.CENTER;
            case NORTH_EAST, EAST, SOUTH_EAST -> HorizontalAlignment.END;
        };
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private enum HorizontalAlignment { START, CENTER, END }
}

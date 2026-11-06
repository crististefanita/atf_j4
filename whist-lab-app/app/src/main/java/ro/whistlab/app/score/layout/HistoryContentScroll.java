package ro.whistlab.app.score.layout;

import android.content.Context;
import android.view.View;
import android.widget.ScrollView;
import ro.whistlab.app.score.ScoreHistoryView;

/** Keeps headers pinned normally; compact windows can scroll the complete content instead of clipping it. */
public final class HistoryContentScroll extends ScrollView {
    public HistoryContentScroll(Context context, ScoreHistoryView history) {
        super(context);
        setFillViewport(true);
        addView(history, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override protected void measureChildWithMargins(View child, int parentWidthSpec, int widthUsed,
                                                     int parentHeightSpec, int heightUsed) {
        if (MeasureSpec.getMode(parentHeightSpec) == MeasureSpec.UNSPECIFIED) {
            super.measureChildWithMargins(child, parentWidthSpec, widthUsed, parentHeightSpec, heightUsed);
            return;
        }
        var margins = (MarginLayoutParams) child.getLayoutParams();
        int width = getChildMeasureSpec(parentWidthSpec, getPaddingLeft() + getPaddingRight()
                + margins.leftMargin + margins.rightMargin + widthUsed, margins.width);
        int height = Math.max(0, MeasureSpec.getSize(parentHeightSpec) - getPaddingTop() - getPaddingBottom()
                - margins.topMargin - margins.bottomMargin - heightUsed);
        child.measure(width, MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        if (child instanceof ScoreHistoryView history && history.needsWholeScroll())
            child.measure(width, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
}

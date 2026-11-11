package ro.cardtable.ui.hand;

import android.content.Context;
import android.view.View;
import android.widget.HorizontalScrollView;
import java.util.function.IntFunction;

/** Fits the actual parent width. A wider fan initially exposes its middle and remains pannable. */
public class CenteredHandView extends HorizontalScrollView {
    private final int maximumWidth;
    private final IntFunction<View> createFan;
    private int viewport = -1;
    private boolean centerOnLayout;

    public CenteredHandView(Context context, int maximumWidth, IntFunction<View> createFan) {
        super(context);
        this.maximumWidth = maximumWidth;
        this.createFan = createFan;
        setFillViewport(true);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = Math.min(maximumWidth, MeasureSpec.getSize(widthSpec));
        if (width > 0 && viewport != width) {
            viewport = width;
            removeAllViews();
            addView(createFan.apply(width), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            centerOnLayout = true;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightSpec);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (centerOnLayout) {
            scrollTo(Math.max(0, (getChildAt(0).getWidth() - getWidth()) / 2), 0);
            centerOnLayout = false;
        }
    }
}

package ro.whistlab.app.score.award;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextView;

/** Owns only the points text and its optional series rectangle; the bid is a separate sibling view. */
public final class SeriesPointsView extends TextView {
    private final int seriesPoints;
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SeriesPointsView(Context context, int seriesPoints) {
        super(context);
        this.seriesPoints = seriesPoints;
        frame.setStyle(Paint.Style.STROKE);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (seriesPoints != 0) setMeasuredDimension(getMeasuredWidthAndState(), resolveSizeAndState(
                Math.max(getMeasuredHeight(), requiredHeight()), heightSpec,
                getMeasuredState() << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (seriesPoints == 0 || getText().length() == 0) return;
        int stroke = unit();
        var rectangle = SeriesPointsFrame.inside(getWidth(), getHeight(), requiredHeight(), stroke);
        if (rectangle == null) return;
        frame.setColor(SeriesPointsFrame.color(seriesPoints));
        frame.setStrokeWidth(stroke);
        canvas.drawRect(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom(), frame);
    }

    private int requiredHeight() {
        Paint.FontMetrics font = getPaint().getFontMetrics();
        return SeriesPointsFrame.requiredHeight((int) Math.ceil(font.bottom - font.top), unit());
    }

    private int unit() { return Math.max(1, Math.round(getResources().getDisplayMetrics().density)); }
}

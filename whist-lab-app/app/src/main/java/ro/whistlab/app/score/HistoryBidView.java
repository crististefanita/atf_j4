package ro.whistlab.app.score;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextView;
import ro.whistlab.app.score.layout.BidStrikeGeometry;

/** Keeps the bid as actual text; the cancellation mark is drawn across the glyphs. */
final class HistoryBidView extends TextView {
    private final boolean missed;
    private final Paint strike = new Paint(Paint.ANTI_ALIAS_FLAG);

    HistoryBidView(Context context, boolean missed) {
        super(context);
        this.missed = missed;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!missed || getText().length() == 0) return;
        float density = getResources().getDisplayMetrics().density;
        float width = getPaint().measureText(getText().toString());
        float left = (getWidth() - width) / 2f;
        Paint.FontMetrics font = getPaint().getFontMetrics();
        var line = BidStrikeGeometry.across(left, getBaseline() + font.ascent,
                left + width, getBaseline() + font.descent, getWidth(), density);
        strike.setColor(BidStrikeGeometry.COLOR);
        strike.setStrokeWidth(BidStrikeGeometry.strokeWidth(density));
        strike.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(line.left(), line.bottom(), line.right(), line.top(), strike);
    }
}

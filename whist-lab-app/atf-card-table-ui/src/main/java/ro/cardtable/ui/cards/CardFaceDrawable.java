package ro.cardtable.ui.cards;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/** Decorative face marks; the measured, accessible rank/suit index remains a separate TextView. */
public class CardFaceDrawable extends GradientDrawable {
    private final CardFace card;
    private final String suitText;
    private final float density;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CardFaceDrawable(CardFace card, float density, int border) {
        this.card = card;
        this.suitText = CardFaceInk.textPresentation(card.suitSymbol());
        this.density = density;
        restoreStyle(border);
        ink.setColor(CardFaceInk.color(card.red()));
        ink.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        ink.setTextAlign(Paint.Align.CENTER);
    }

    public void restoreStyle(int border) {
        setColor(Color.rgb(255, 254, 248));
        setStroke(Math.round(density), border);
        setCornerRadius(9 * density);
    }

    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        var bounds = getBounds();
        float unit = bounds.height() / (float) CardFaceMetrics.HAND_HEIGHT_DP;
        ink.setTextSize(CardFaceMetrics.CENTER_SUIT_SIZE_DP * unit);
        canvas.drawText(suitText, bounds.exactCenterX(),
                bounds.top + bounds.height() * CardFaceMetrics.CENTER_SUIT_BASELINE, ink);
        canvas.save();
        canvas.rotate(180, bounds.exactCenterX(), bounds.exactCenterY());
        ink.setTextSize(CardFaceMetrics.INVERTED_RANK_SIZE_DP * unit);
        float rankCenter = Math.max(9 * unit, 4 * unit + ink.measureText(card.rankLabel()) / 2);
        float rankBaseline = Math.max(12 * unit, 4 * unit - ink.ascent());
        canvas.drawText(card.rankLabel(), bounds.left + rankCenter, bounds.top + rankBaseline, ink);
        float rankBottom = rankBaseline + ink.descent();
        ink.setTextSize(CardFaceMetrics.INVERTED_SIZE_DP * unit);
        float suitBaseline = Math.max(22 * unit, rankBottom - ink.ascent());
        canvas.drawText(suitText, bounds.left + rankCenter, bounds.top + suitBaseline, ink);
        canvas.restore();
    }
}

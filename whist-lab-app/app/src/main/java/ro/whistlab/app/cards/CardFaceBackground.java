package ro.whistlab.app.cards;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import ro.whistlab.core.model.Card;

/** Decorative face marks; the measured, accessible rank/suit index remains a separate TextView. */
public final class CardFaceBackground extends GradientDrawable {
    private final Card card;
    private final float density;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CardFaceBackground(Card card, float density, int border) {
        this.card = card;
        this.density = density;
        setColor(Color.rgb(255, 254, 248));
        setStroke(Math.round(density), border);
        setCornerRadius(9 * density);
        ink.setColor(card.suit().red() ? Color.rgb(173, 57, 58) : Color.rgb(42, 53, 47));
        ink.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        ink.setTextAlign(Paint.Align.CENTER);
    }

    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        var bounds = getBounds();
        ink.setTextSize(28 * density);
        canvas.drawText(card.suit().symbol(), bounds.exactCenterX(), bounds.top + bounds.height() * .66f, ink);
        canvas.save();
        canvas.rotate(180, bounds.exactCenterX(), bounds.exactCenterY());
        ink.setTextSize(9 * density);
        canvas.drawText(card.rankLabel(), bounds.left + 9 * density, bounds.top + 12 * density, ink);
        canvas.drawText(card.suit().symbol(), bounds.left + 9 * density, bounds.top + 22 * density, ink);
        canvas.restore();
    }
}

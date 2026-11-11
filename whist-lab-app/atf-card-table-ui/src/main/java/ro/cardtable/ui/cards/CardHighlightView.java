package ro.cardtable.ui.cards;

import android.graphics.drawable.GradientDrawable;
import android.view.View;

/** Applies the highlight to a card face. The caller owns identity and accessibility text. */
public final class CardHighlightView {
    private CardHighlightView() { }

    public static void paint(GradientDrawable face, CardHighlight highlight, float density) {
        face.setColor(highlight.fill());
        face.setStroke(highlight.strokeWidthPx(density), highlight.outline());
        face.setCornerRadius(highlight.radiusPx(density));
    }

    public static void raise(View view, int cardHeightDp) {
        var highlight = CardHighlight.RAISED;
        int lift = highlight.liftDp(cardHeightDp);
        float density = view.getResources().getDisplayMetrics().density;
        var face = new GradientDrawable();
        paint(face, highlight, density);
        view.setBackground(face);
        view.setTranslationY(-Math.round(lift * density));
        view.setElevation(Math.round(highlight.elevationDp() * density));
    }
}

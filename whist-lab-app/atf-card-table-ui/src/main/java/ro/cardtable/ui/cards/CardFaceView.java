package ro.cardtable.ui.cards;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Composes the corner index and decorative marks at the size allocated to the card. */
public final class CardFaceView {
    private CardFaceView() { }

    public static int indexBandWidthPx(Context context, CardFaceMetrics metrics) {
        TextView label = index(context, metrics);
        float widest = label.getPaint().measureText("10");
        for (String suit : new String[]{"\u2660", "\u2665", "\u2663", "\u2666"}) {
            widest = Math.max(widest, label.getPaint().measureText(CardFaceInk.textPresentation(suit)));
        }
        return (int) Math.ceil(widest) + 2 * label.getPaddingLeft();
    }

    public static FrameLayout build(Context context, CardFace card, CardFaceDrawable background,
            CardFaceMetrics metrics, int indexWidthPx) {
        FrameLayout face = new FrameLayout(context);
        face.setBackground(background);
        TextView label = index(context, metrics);
        label.setText(CardFaceInk.textPresentation(card.rankLabel() + "\n" + card.suitSymbol()));
        label.setTextColor(CardFaceInk.color(card.red()));
        label.setTag("card-index");
        face.addView(label, new FrameLayout.LayoutParams(indexWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START));
        return face;
    }

    private static TextView index(Context context, CardFaceMetrics metrics) {
        float unit = context.getResources().getDisplayMetrics().density * metrics.scale();
        int side = Math.round(CardFaceMetrics.INDEX_SIDE_DP * unit);
        TextView label = new TextView(context);
        label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        label.setIncludeFontPadding(false);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextSize(metrics.indexSizeSp());
        label.setPadding(side, Math.round(CardFaceMetrics.INDEX_TOP_DP * unit), side, 0);
        return label;
    }
}

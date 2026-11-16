package ro.cardtable.ui.cards;

import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

/** Android sizes the rank from the shared scale and actual font metrics. */
public final class CardRankText {
    private CardRankText() { }

    public static float fit(TextView label, String rank, int width, int height, int maximumSize) {
        Paint paint = label.getPaint();
        var metrics = paint.getFontMetricsInt();
        float target = Math.min(CardFaceMetrics.RANK_SCALE, maximumSize / paint.getTextSize());
        return CardFaceMetrics.fitRank(target, paint.measureText(rank), metrics.descent - metrics.ascent,
                Math.max(1, width), Math.max(1, height));
    }

    public static void stack(TextView label, String face, int width, int height, int suitSizeCap) {
        int rankEnd = CardFaceMetrics.rankLength(face);
        if (rankEnd == 0 || rankEnd == face.length()) return;
        label.setSingleLine(false);
        label.setIncludeFontPadding(false);
        Paint paint = label.getPaint();
        var metrics = paint.getFontMetricsInt();
        int line = metrics.descent - metrics.ascent;
        float suitScale = Math.min(1f, suitSizeCap / paint.getTextSize());
        String rank = face.substring(0, rankEnd);
        String suit = face.substring(rankEnd);
        float rankScale = fit(label, "10", width, height - (int) Math.ceil(line * suitScale) - 2,
                Integer.MAX_VALUE);
        var styled = new SpannableString(rank + "\n" + suit);
        scale(styled, rankScale, 0, rankEnd);
        scale(styled, suitScale, rankEnd + 1, styled.length());
        label.setText(styled);
    }

    public static CharSequence hand(String rank, String suit, float rankScale, float suitScale) {
        var label = new SpannableString(rank + "\n" + suit);
        if (CardFaceMetrics.rankLength(rank) != 0) scale(label, rankScale, 0, rank.length());
        scale(label, suitScale, rank.length() + 1, label.length());
        return label;
    }

    private static void scale(SpannableString text, float factor, int start, int end) {
        text.setSpan(new RelativeSizeSpan(factor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}

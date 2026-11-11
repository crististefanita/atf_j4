package ro.cardtable.ui.seat;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Keeps one complete seat line inside its measured width while preserving the row height. */
final class FittedSeatLine extends TextView {
    FittedSeatLine(Context context, CharSequence text, int sizeSp, int color, boolean compact) {
        super(context);
        setText(text);
        setTextSize(sizeSp);
        setTextColor(color);
        setGravity(Gravity.CENTER);
        setIncludeFontPadding(!compact);
        setSingleLine(true);
        getPaint().setSubpixelText(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static FittedSeatLine fact(Context context, SeatContent.Fact fact, int color, boolean compact) {
        var text = new SpannableString(fact.inlineText());
        if (!fact.value().isEmpty())
            text.setSpan(new StyleSpan(Typeface.BOLD), fact.label().length() + 1, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return new FittedSeatLine(context, text, 11, color, compact);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        var paint = new TextPaint(getPaint());
        paint.setTextScaleX(1);
        int available = MeasureSpec.getSize(widthSpec) - getCompoundPaddingLeft() - getCompoundPaddingRight();
        float natural = Layout.getDesiredWidth(getText(), paint);
        float scale = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED || natural <= 0
                ? 1 : Math.min(1, Math.max(0, available - 1) / (float) Math.ceil(natural));
        if (scale != getTextScaleX()) setTextScaleX(scale);
        super.onMeasure(widthSpec, heightSpec);
        if (MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED && available > 0) {
            // A fallback font can round each glyph separately, even with fractional advances.
            int safetyPixels = Math.max(2, getText().length() / 2);
            for (int correction = 0; correction < 2 && getLayout().getLineWidth(0) > available; correction++) {
                float actual = getLayout().getLineWidth(0);
                setTextScaleX(getTextScaleX() * Math.max(0, available - safetyPixels) / actual);
                super.onMeasure(widthSpec, heightSpec);
            }
        }
    }
}

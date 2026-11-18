package ro.cardtable.ui.cards;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real Android text shaping and raster checks, without launching a game or an activity. */
public final class CardTypographyMeasurement {
    private CardTypographyMeasurement() { }

    public static JSONArray verify(Context target) throws Exception {
        var results = new JSONArray();
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        String[] suits = {"♠", "♥", "♣", "♦"};
        for (float font : new float[]{1, 1.5f, 2}) {
            var configuration = new Configuration(target.getResources().getConfiguration());
            configuration.fontScale = font;
            Context context = target.createConfigurationContext(configuration);
            float density = context.getResources().getDisplayMetrics().density;
            var sheet = Bitmap.createBitmap(Math.round(7 * 70 * density), Math.round(6 * 96 * density), Bitmap.Config.ARGB_8888);
            var canvas = new Canvas(sheet);
            canvas.drawColor(0xff194e3e);
            int role = 0;
            for (int height : new int[]{88, 70, 64}) {
                var metrics = CardFaceMetrics.atHeight(height);
                int band = CardFaceView.indexBandWidthPx(context, metrics);
                for (int index = 0; index < ranks.length; index++) {
                    for (String suit : suits) {
                        var card = new CardFace(ranks[index], suit, "♥♦".contains(suit));
                        var face = CardFaceView.build(context, card, new CardFaceDrawable(card, density, 0xff888888), metrics, band);
                        int widthPx = Math.round(metrics.widthDp() * density);
                        int heightPx = Math.round(height * density);
                        face.measure(exact(widthPx), exact(heightPx));
                        face.layout(0, 0, widthPx, heightPx);
                        TextView label = face.findViewWithTag("card-index");
                        Spanned text = (Spanned) label.getText();
                        var spans = text.getSpans(0, card.rankLabel().length(), MetricAffectingSpan.class);
                        require(spans.length == 1 && text.getSpanEnd(spans[0]) == card.rankLabel().length(), "Rank-only span " + card);
                        var draw = new TextPaint(label.getPaint());
                        var measure = new TextPaint(label.getPaint());
                        spans[0].updateDrawState(draw);
                        spans[0].updateMeasureState(measure);
                        float expected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 19.5f * height / 88,
                                context.getResources().getDisplayMetrics());
                        require(Math.abs(draw.getTextSize() - expected) < .01 && draw.getTextSize() == measure.getTextSize(),
                                "Draw/measure size " + card);
                        require(label.getLineCount() == 2 && label.getBottom() <= heightPx && band <= widthPx,
                                "Clipped corner " + card + " height=" + height + " font=" + font);
                        require(label.getLayout().getLineMax(0) <= band - label.getPaddingLeft() * 2 + 1,
                                "Clipped rank width " + card + " height=" + height + " font=" + font
                                        + " visibleLine=" + label.getLayout().getLineMax(0) + " band=" + band
                                        + " padding=" + label.getPaddingLeft());
                        if (suit.equals(suits[index % suits.length])) {
                            canvas.save();
                            canvas.translate((index % 7) * 70 * density, (role * 2 + index / 7) * 96 * density);
                            face.draw(canvas);
                            canvas.restore();
                        }
                    }
                }
                results.put(new JSONObject().put("heightDp", height).put("fontScale", font)
                        .put("faces", 52).put("indexWidthPx", band).put("rankSp", metrics.rankSizeSp()).put("passed", true));
                role++;
            }
            File file = new File(target.getExternalFilesDir(null), "card-ranks-" + font + ".png");
            try (var output = new FileOutputStream(file)) { sheet.compress(Bitmap.CompressFormat.PNG, 100, output); }
            sheet.recycle();
        }
        return results;
    }

    private static int exact(int pixels) { return View.MeasureSpec.makeMeasureSpec(pixels, View.MeasureSpec.EXACTLY); }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

package ro.whistlab.app.score.nativecomponent;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.whistlab.app.score.award.SeriesPointsView;

/** Native components only: actual production Views inside a parent, without an Activity or match. */
public final class HistoryMarksComponentInstrumentation extends Instrumentation {
    private final JSONArray cases = new JSONArray();
    private int failures;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            var executionFailure = new java.util.concurrent.atomic.AtomicReference<Exception>();
            runOnMainSync(() -> {
                try { verifyMarks(); }
                catch (Exception error) { executionFailure.set(error); }
            });
            if (executionFailure.get() != null) throw executionFailure.get();
            result.putString("stream", "\nATF_NATIVE_HISTORY=" + new JSONObject()
                    .put("cases", cases).put("failures", failures).toString() + "\n");
            finish(failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        } catch (Exception error) {
            result.putString("stream", "Native history component execution failed: " + error);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void verifyMarks() throws Exception {
        for (float scale : new float[]{1f, 1.3f, 1.5f}) {
            Configuration configuration = new Configuration(getTargetContext().getResources().getConfiguration());
            configuration.fontScale = scale;
            Context context = getTargetContext().createConfigurationContext(configuration);
            for (int width : new int[]{40, 80}) {
                for (String bid : new String[]{"0", "1", "8"}) {
                    for (boolean missed : new boolean[]{false, true})
                        observe(context, bidView(context, missed), bid, width, scale,
                                "bid", missed ? 1 : 0, 0xffb3261e);
                }
                for (int series : new int[]{0, 10, -10})
                    observe(context, new SeriesPointsView(context, series), "-123", width, scale,
                            "points", series, series < 0 ? 0xff97462e : 0xff29362e);
            }
        }
    }

    private static TextView bidView(Context context, boolean missed) throws Exception {
        Class<?> type = Class.forName("ro.whistlab.app.score.HistoryBidView", true, context.getClassLoader());
        var constructor = type.getDeclaredConstructor(Context.class, boolean.class);
        constructor.setAccessible(true);
        return (TextView) constructor.newInstance(context, missed);
    }

    private void observe(Context context, TextView value, String text, int widthDp, float scale,
                         String kind, int adjustment, int ink) throws Exception {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(widthDp * density), height = Math.round(72 * density);
        value.setText(text);
        value.setTextSize(16);
        value.setTextColor(0xff000000); // Isolate the decoration ink from the glyph ink.
        value.setGravity(Gravity.CENTER);
        value.setPadding(Math.round(density), Math.round(6 * density), Math.round(density), Math.round(6 * density));
        value.setSingleLine(true);
        FrameLayout parent = new FrameLayout(context);
        parent.addView(value, new FrameLayout.LayoutParams(width, height));
        parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, width, height);
        value.onPreDraw();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            parent.draw(new Canvas(bitmap));
            int pixels = 0;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                if (bitmap.getPixel(x, y) == ink) pixels++;
            boolean expected = adjustment != 0, passed = expected == (pixels > 0);
            if (!passed) failures++;
            var png = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)) throw new IllegalStateException("PNG encoding failed");
            cases.put(new JSONObject().put("kind", kind).put("value", text).put("adjustment", adjustment)
                    .put("widthDp", widthDp).put("fontScale", scale).put("scrollX", value.getScrollX())
                    .put("scrollY", value.getScrollY()).put("expectedDecoration", expected)
                    .put("actualInkPixels", pixels).put("inkArgb", Integer.toHexString(ink)).put("passed", passed)
                    .put("pngBase64", Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP)));
        } finally {
            bitmap.recycle();
        }
    }
}

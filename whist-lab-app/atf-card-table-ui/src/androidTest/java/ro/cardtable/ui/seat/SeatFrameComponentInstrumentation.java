package ro.cardtable.ui.seat;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.cardtable.ui.hand.HandHighlightMeasurement;
import ro.cardtable.ui.screens.GameTitleBar;
import ro.cardtable.ui.table.BoardLayout;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableViewportPolicy;

/** Measures actual shared Android widgets with real system fonts, independently of either game. */
public final class SeatFrameComponentInstrumentation extends Instrumentation {
    private final JSONArray cases = new JSONArray();
    private final JSONArray headers = new JSONArray();
    private int failures;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        var result = new Bundle();
        var error = new AtomicReference<Exception>();
        var highlights = new AtomicReference<JSONArray>();
        runOnMainSync(() -> {
            try {
                verifySeats();
                verifyTitleBars();
                highlights.set(HandHighlightMeasurement.verify(getTargetContext()));
            }
            catch (Exception failure) { error.set(failure); }
        });
        try {
            var output = new JSONObject().put("cases", cases).put("gameTitleBars", headers).put("failures", failures);
            if (highlights.get() != null) output.put("cardHighlights", highlights.get());
            if (error.get() != null) output.put("error", error.get().toString());
            result.putString("stream", "\nATF_NATIVE_SEATS=" + output + "\n");
        } catch (org.json.JSONException failure) {
            throw new IllegalStateException("Cannot report native seat measurements", failure);
        }
        finish(failures == 0 && error.get() == null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }

    private void verifySeats() throws Exception {
        for (float font : new float[]{1f, 1.5f, 2f}) {
            var configuration = new Configuration(getTargetContext().getResources().getConfiguration());
            configuration.fontScale = font;
            Context context = getTargetContext().createConfigurationContext(configuration);
            for (int width : new int[]{240, 320, 393}) {
                for (int count = 3; count <= 6; count++) measure(context, font, width, count);
            }
        }
    }

    private void verifyTitleBars() throws Exception {
        for (float font : new float[]{1f, 1.5f, 2f}) {
            var configuration = new Configuration(getTargetContext().getResources().getConfiguration());
            configuration.fontScale = font;
            Context context = getTargetContext().createConfigurationContext(configuration);
            int target = Math.round(GameTitleBar.TOUCH_TARGET_DP * context.getResources().getDisplayMetrics().density);
            for (int width : new int[]{320, 360, 393, 600}) {
                for (boolean romanian : new boolean[]{true, false}) {
                    var title = new TextView(context);
                    title.setText(romanian ? "Masă de joc · Turneu românesc" : "Game table · Practice tournament");
                    title.setTextSize(16);
                    var changes = new ArrayList<Boolean>();
                    var bar = new GameTitleBar(context, new View(context), title, new GameTitleBar.AutoPlay(
                            romanian ? "Automat" : "Auto play", "Start", "Stop", true, true, 0xffffffff, changes::add));
                    bar.measure(exact(context, width), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    bar.layout(0, 0, bar.getMeasuredWidth(), bar.getMeasuredHeight());
                    CheckBox toggle = bar.findViewWithTag(GameTitleBar.AUTO_PLAY_TAG);
                    boolean sameLine = toggle.getTop() < title.getBottom();
                    boolean initial = toggle.isChecked() && changes.isEmpty() && "Stop".contentEquals(toggle.getContentDescription());
                    toggle.performClick();
                    boolean fits = toggle.getRight() == bar.getWidth() && toggle.getWidth() >= target && toggle.getHeight() >= target
                            && (!sameLine || title.getRight() <= toggle.getLeft()) && toggle.getBottom() <= bar.getHeight()
                            && title.getLayout().getEllipsisCount(title.getLineCount() - 1) == 0
                            && toggle.getLayout().getEllipsisCount(toggle.getLineCount() - 1) == 0;
                    boolean passed = initial && fits && changes.equals(List.of(false))
                            && "Start".contentEquals(toggle.getContentDescription());
                    if (!passed) failures++;
                    headers.put(new JSONObject().put("widthDp", width).put("fontScale", font)
                            .put("language", romanian ? "RO" : "EN").put("inline", sameLine).put("passed", passed));
                }
            }
        }
    }

    private void measure(Context context, float font, int width, int count) throws Exception {
        var viewport = new TableViewportPolicy(530, font);
        var board = new BoardLayout(context, count, width, viewport, true, TableGeometry.CenterAlignment.BALANCED);
        var expected = new ArrayList<List<String>>();
        for (int seat = 0; seat < count; seat++) {
            var name = List.of("Ștefănescu", "Alexandru", "Crîștiană", "Ionuț", "Mihăiță", "Mădălina").get(seat);
            var facts = switch (seat % 3) {
                case 0 -> List.of(new SeatContent.Fact("Puncte:", "-1000"), new SeatContent.Fact("Total:", "-2147483648"));
                case 1 -> List.of(new SeatContent.Fact("Δ", "-1000"), new SeatContent.Fact("Σ", "-2147483648"));
                default -> List.of(new SeatContent.Fact("Licitat:", "13"), new SeatContent.Fact("Levate:", "0"));
            };
            var text = List.of(name, facts.get(0).inlineText(), facts.get(1).inlineText());
            expected.add(text);
            var content = new SeatContent(seat, name, facts, seat == 1, String.join(". ", text));
            var frame = new PlayerSeatFrame(context, viewport.isCompact()).buildInlineFacts(content, viewport.seatVerticalPaddingDp());
            board.addView(new PlayerSeatView(context, frame, "LA RÂND", content.active(), seat, viewport.isCompact()));
        }
        var center = new View(context);
        center.setMinimumHeight(Math.round(72 * context.getResources().getDisplayMetrics().density));
        board.addView(center);
        board.measure(exact(context, width), exact(context, 530));
        board.layout(0, 0, board.getMeasuredWidth(), board.getMeasuredHeight());
        var observations = new JSONArray();
        var boxes = new ArrayList<Rect>();
        boolean fits = true;
        for (int seat = 0; seat < count; seat++) {
            var region = board.getChildAt(seat);
            var box = new Rect(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
            fits &= box.left >= 0 && box.top >= 0 && box.right <= board.getWidth() && box.bottom <= board.getHeight()
                    && boxes.stream().noneMatch(other -> Rect.intersects(box, other));
            boxes.add(box);
            LinearLayout frame = board.findViewWithTag("table-seat:" + seat);
            fits &= frame.getChildCount() == 3 && frame.getContentDescription().toString().equals(String.join(". ", expected.get(seat)));
            for (int index = 0; index < frame.getChildCount(); index++) {
                var line = (TextView) frame.getChildAt(index);
                var layout = line.getLayout();
                float used = layout.getLineWidth(0);
                int available = line.getWidth() - line.getCompoundPaddingLeft() - line.getCompoundPaddingRight();
                boolean intact = line.getText().toString().equals(expected.get(seat).get(index))
                        && layout.getLineCount() == 1 && layout.getEllipsisCount(0) == 0 && used <= available
                        && line.getTop() >= 0 && line.getBottom() <= frame.getHeight();
                fits &= intact;
                observations.put(new JSONObject().put("seat", seat).put("text", line.getText())
                        .put("availablePx", available).put("usedPx", used).put("intact", intact));
            }
        }
        if (!fits) failures++;
        cases.put(new JSONObject().put("fontScale", font).put("tableWidthDp", width).put("players", count)
                .put("passed", fits).put("rows", observations));
    }

    private static int exact(Context context, int dp) {
        return View.MeasureSpec.makeMeasureSpec(Math.round(dp * context.getResources().getDisplayMetrics().density),
                View.MeasureSpec.EXACTLY);
    }
}

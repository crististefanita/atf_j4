package ro.cardtable.ui.score;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class HistoryGridComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void measuredColumnsStayCompleteAcrossCompactAndLargeTextWindows() {
        atf.gradle.audit.GradleInputs.fixture("Measured history fixtures", "seats", List.of(3,4,5,6), "widths dp", List.of(240,280,360,414,600,800), "density", List.of(1,1.5,2.75), "font scale", List.of(1,1.5,2), "numeric minima", "52/65 × density × font");
        for (int players : new int[]{3, 4, 5, 6}) for (int width : new int[]{240, 280, 360, 414, 600, 800}) {
            for (float density : new float[]{1, 1.5f, 2.75f}) for (float font : new float[]{1, 1.5f, 2}) {
                // Measured pixel fixtures: no font raster or Android touch simulation is claimed.
                int viewport = Math.round(width * density), separator = Math.max(1, Math.round(density));
                var minima = List.of(Math.round(52 * font * density), Math.round(65 * font * density));
                var grid = HistoryGridMetrics.fit(viewport, Math.round(180 * font * density),
                        Math.round(100 * font * density), minima, separator, players);
                String key = players + "/" + width + "/" + density + "/" + font;
                assertTrue("Pinned label leaves a score viewport " + key, grid.label() <= viewport * .4);
                assertTrue("Signed delta remains complete " + key, grid.columns().get(0) >= minima.get(0));
                assertTrue("Signed total remains complete " + key, grid.columns().get(1) >= minima.get(1));
                assertEquals("Last player remains in the horizontal extent " + key,
                        players * (separator + grid.columns().get(0) + grid.columns().get(1)), grid.scoresWidth());
                for (int height : new int[]{240, 568, 896}) {
                    int available = Math.round(height * density), heading = Math.round(70 * font * density);
                    int legend = Math.round(32 * font * density), first = Math.round(58 * font * density), all = players * 8 * first;
                    int body = HistoryGridMetrics.bodyHeight(available, heading, legend, all, first);
                    if (available < heading + legend + first) assertEquals("Outer scroller keeps last hand " + key + "/" + height, all, body);
                    else assertTrue("Fixed headings leave a complete row " + key + "/" + height, body >= first && body <= available - heading - legend);
                }
            }
        }
        assertEquals("Empty history has no phantom body", 0, HistoryGridMetrics.bodyHeight(400, 40, 30, 0, 0));
        assertThrows("Zero players is not a layout", IllegalArgumentException.class,
                () -> HistoryGridMetrics.fit(300, 40, 50, List.of(20, 30), 1, 0));
    }
    @Test public void scrollsFollowEitherPaneWithoutFeedbackLoopsAndRecoverAfterFailure() {
        atf.gradle.audit.GradleInputs.fixture("Bidirectional pane events", "drags px", List.of(140,270,270,0), "failure", "detached target");
        var sync = new HistoryScrollSync(); var events = new ArrayList<String>();
        class Pane implements HistoryScrollSync.Pane {
            int x; String id; Pane other; Pane(String id) { this.id = id; }
            public int position() { return x; }
            public void scrollTo(int value) { x = value; events.add(id + ":" + value); sync.follow(other, value); }
        }
        var header = new Pane("header"); var body = new Pane("body"); header.other = body; body.other = header;
        header.x = 140; sync.follow(body, 140);
        assertEquals("Header drag follows once", List.of("body:140"), events);
        body.x = 270; sync.follow(header, 270);
        assertEquals("Body drag follows once", List.of("body:140", "header:270"), events);
        sync.follow(header, 270); assertEquals("Equal positions do not trigger layout churn", 2, events.size());
        assertThrows("A failed target is surfaced", IllegalStateException.class, () -> sync.follow(new HistoryScrollSync.Pane() {
            public int position() { return 0; } public void scrollTo(int x) { throw new IllegalStateException("detached"); }
        }, 10));
        sync.follow(header, 0); assertEquals("Failure does not lock later scrolling", 0, header.x);
    }
}

package ro.cardtable.ui.table;

import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class TablePresentationComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private record DisplayProfile(int width, int height, float fontScale, boolean compact, int cardHeight) { }

    @Test public void displayProfilesPreserveSeatsAndTheCentralBand() {
        var profiles = List.of(new DisplayProfile(320, 599, 1f, true, 64),
                new DisplayProfile(360, 740, 1.5f, true, 70),
                new DisplayProfile(393, 800, 2f, true, 70),
                new DisplayProfile(600, 1140, 1.5f, false, 70));
        for (var profile : profiles) {
            var viewport = new TableViewportPolicy(profile.height(), profile.fontScale());
            int boardHeight = profile.height() - 200;
            int gap = viewport.regionGapDp();
            for (int players = 3; players <= 6; players++) {
                var cards = PlayedCardMetrics.forTable(players, profile.width(), profile.fontScale(), viewport);
                var plan = TableGeometry.arrangeDisplay(profile.width(), boardHeight, players, profile.width() / 4,
                        Math.round(40 * profile.fontScale()), profile.width(), cards.centerHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.BALANCED);
                var band = TableGeometry.fullWidthCardBand(plan, gap);
                atf.gradle.audit.GradleInputs.fixture("Measured table profile", "viewport", profile,
                        "board height dp", boardHeight, "players", players);
                assertEquals("Compact policy follows the height and font", profile.compact(), viewport.isCompact());
                assertEquals("Only a short window reduces the card face", profile.cardHeight(), cards.cardHeightDp());
                assertEquals("Every seat retains its own rectangle", players, plan.seats().size());
                assertTrue("The full trick fits in the measured free band", band.height() >= cards.centerHeightDp());
                assertTrue("Trick cards keep their clearance from every seat", plan.seats().stream().noneMatch(plan.center()::overlaps));
                assertEquals("The human seat stays at the bottom", boardHeight, plan.seats().get(0).bottom());
            }
        }
    }

    @Test public void playAnchorsAndFreeBandMatchFixedCoordinatesForThreeThroughSixSeats() {
        var human = new TableGeometry.Rect(135, 440, 225, 500);
        var topLeft = new TableGeometry.Rect(0, 25, 90, 85);
        var top = new TableGeometry.Rect(135, 25, 225, 85);
        var topRight = new TableGeometry.Rect(270, 25, 360, 85);
        var lowerLeft = new TableGeometry.Rect(0, 440, 90, 500);
        var lowerRight = new TableGeometry.Rect(270, 440, 360, 500);
        var expectedSeats = List.of(
                List.of(human, topLeft, topRight),
                List.of(human, new TableGeometry.Rect(0, 111, 90, 171), top,
                        new TableGeometry.Rect(270, 111, 360, 171)),
                List.of(human, lowerLeft, topLeft, topRight, lowerRight),
                List.of(human, lowerLeft, topLeft, top, topRight, lowerRight));
        for (int count = 3; count <= 6; count++) {
            var plan = TableGeometry.arrangeDisplay(360, 500, count, 90, 60, 360, 180, 1, true,
                    TableGeometry.CenterAlignment.BALANCED);
            var expected = new TableGeometry.VerticalBand(count == 4 ? 172 : 86, 439);
            atf.gradle.audit.GradleInputs.fixture("Full-width reference board", "seats", count,
                    "viewport", "360 × 500", "reference center height", 180);
            assertEquals("Seat rectangles and clockwise order", expectedSeats.get(count - 3), plan.seats());
            assertEquals("Reference trick region", new TableGeometry.Rect(0, 172, 360, 352), plan.center());
            var band = TableGeometry.fullWidthCardBand(plan, 1);
            assertEquals("Full-width band leaves the measured gap to both rows", expected, band);
            int centerTop = band.centeredTop(96);
            assertEquals("Alternate content is centered without moving seats", count == 4 ? 257 : 214, centerTop);
            var content = new TableGeometry.Rect(0, centerTop, 360, centerTop + 96);
            assertTrue("Alternate content clears all measured seats", plan.seats().stream().noneMatch(content::overlaps));
        }
    }

    @Test public void perimeterKeepsHumanBelowAndSeatOrderStableWithoutAssumingTheActor() {
        for (int count = 3; count <= 6; count++) {
            var plan = TableGeometry.arrangeDisplay(360, 500, count, 90, 60, 300, 180, 1, true,
                    TableGeometry.CenterAlignment.BALANCED);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "plan", plan);
            assertEquals("plan.seats().size() must equal count", count, plan.seats().size());
            assertEquals("plan.seats().get(0).left() must equal 135", 135, plan.seats().get(0).left());
            assertEquals("plan.seats().get(0).bottom() must equal 500", 500, plan.seats().get(0).bottom());
            assertEquals("TableSeatLayout.positions(count)[0] must equal TableSeatLayout.Anchor.SOUTH", TableSeatLayout.Anchor.SOUTH, TableSeatLayout.positions(count)[0]);
            assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, () -> plan.seats().clear());
            for (var box : plan.seats()) {
                assertTrue("box.top() >= 0 && box.bottom() <= 500 must hold", box.top() >= 0 && box.bottom() <= 500);
                assertFalse("box.overlaps(plan.center()) must remain false", box.overlaps(plan.center()));
            }
        }
    }

    @Test public void publicCardIdentityAndCompactSizingAreSeparateFromAnyGameRules() {
        var play = new PlayedCardContent(2, "A♠", false, "Ana", "Ana played A♠");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "play", play);
        assertEquals("play.seat() must equal 2", 2, play.seat());
        assertEquals("play.faceLabel() must equal \"A\\u2660\"", "A♠", play.faceLabel());
        for (int count = 3; count <= 6; count++) {
            var layout = PlayedCardMetrics.forTable(count, 360, 1f, new TableViewportPolicy(569));
            assertEquals("layout.cardHeightDp() must equal 64", 64, layout.cardHeightDp());
            assertEquals("layout.cardWidthDp() must equal 46", 46, layout.cardWidthDp());
            assertEquals("layout.slots().size() must equal count", count, layout.slots().size());
            for (int seat = 0; seat < count; seat++) assertEquals("layout.slotForSeat(seat).seat() must equal seat", seat, layout.slotForSeat(seat).seat());
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new PlayedCardContent(-1, "A♠", false, "Ana", ""));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableSeatLayout.positions(2));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableGeometry.arrange(10, 10, 4, 90, 60, 300, 180, 1));
    }
}

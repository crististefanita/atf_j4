package ro.whistlab.app.table;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import org.junit.Test;

public final class TrickCardMetricsComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void threeToSixPlayersKeepTheSamePortraitFacesIncludingTheFivePlayerRegression() {
        for (int width : new int[]{240, 280, 313, 353, 560, 760}) {
            for (int height : new int[]{569, 600, 760, 818}) {
                for (float scale : new float[]{1f, 1.3f, 1.5f, 2f}) {
                    var viewport = new TableViewportPolicy(height, scale);
                    var reference = TrickCardMetrics.forTable(4, width, scale, viewport);
                    for (int players = 3; players <= 6; players++) {
                        var layout = TrickCardMetrics.forTable(players, width, scale, viewport);

                        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "height", height, "layout", layout, "players", players, "reference", reference, "scale", scale, "viewport", viewport, "width", width);
                        assertEquals("Changing seats cannot resize the played faces", reference.cardWidthDp(), layout.cardWidthDp());
                        assertEquals("layout.cardHeightDp() must equal reference.cardHeightDp()", reference.cardHeightDp(), layout.cardHeightDp());
                        assertTrue("Played faces remain portrait, including at five seats", layout.cardWidthDp() * 4 <= layout.cardHeightDp() * 3);
                        assertEquals("layout.cardWidthDp() must equal Math.round(layout.cardHeightDp() * 5.0F / 7)", Math.round(layout.cardHeightDp()*5f/7),layout.cardWidthDp());
                    }
                }
            }
        }
        assertEquals("TrickCardMetrics.forTable(5, 353).cardWidthDp() must equal 50", 50, TrickCardMetrics.forTable(5, 353).cardWidthDp());
    }

    @Test public void shorterCardFacesKeepSeatAnchorsAndAvoidSeatsAcrossWidthsAndTextScales() {
        for (int width : new int[]{240, 280, 313, 353, 560, 760}) {
            for (int players = 3; players <= 6; players++) {
                for (float fontScale : new float[]{1f, 1.3f, 1.5f}) {
                    TrickCardMetrics.Layout layout = TrickCardMetrics.forTable(players, width, fontScale);
                    int expectedRows = players == 4 ? 3 : 2;

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "expectedRows", expectedRows, "fontScale", fontScale, "layout", layout, "players", players, "width", width);
                    assertEquals("Played-card faces retain the requested reduced height",
                            70, layout.cardHeightDp());
                    assertTrue("layout.cardWidthDp() >= TrickCardMetrics.MIN_CARD_WIDTH_DP must hold", layout.cardWidthDp() >= TrickCardMetrics.MIN_CARD_WIDTH_DP);
                    assertTrue("layout.cardWidthDp() <= TrickCardMetrics.MAX_CARD_WIDTH_DP must hold", layout.cardWidthDp() <= TrickCardMetrics.MAX_CARD_WIDTH_DP);
                    assertEquals("layout.centerWidthDp() must equal width", width, layout.centerWidthDp());
                    assertTrue("layout.safeCenterWidthDp() <= width must hold", layout.safeCenterWidthDp() <= width);
                    assertEquals("layout.rows() must equal expectedRows", expectedRows, layout.rows());
                    assertEquals("layout.centerHeightDp() must equal layout.rows() * (layout.cardHeightDp() + layout.playerLabelHeightDp() + layout.itemVerticalPaddingDp())", layout.rows() * (layout.cardHeightDp() + layout.playerLabelHeightDp()
                                    + layout.itemVerticalPaddingDp()),
                            layout.centerHeightDp());
                    assertTrue("layout.playerLabelHeightDp() >= Math.ceil(TrickCardMetrics.PLAYER_LABEL_HEIGHT_DP * fontScale) must hold", layout.playerLabelHeightDp()
                            >= Math.ceil(TrickCardMetrics.PLAYER_LABEL_HEIGHT_DP * fontScale));

                    int seatHeight = ro.whistlab.app.seat.PlayerSeatMetrics.arrange(
                            (int) Math.ceil(70 * fontScale), (int) Math.ceil(12 * fontScale), 3).heightPx();
                    int gap = 8;
                    int safeCenterWidth = layout.safeCenterWidthDp();
                    int boardHeight = TableGeometry.minimumHeight(players, seatHeight,
                            layout.centerHeightDp(), gap);
                    TableGeometry.Plan plan = TableGeometry.arrange(width, boardHeight, players,
                            TableLayoutMetrics.seatWidthDp(width), seatHeight,
                            safeCenterWidth, layout.centerHeightDp(), gap, true);

                    var seats = plan.seats();
                    for (int first = 0; first < layout.slots().size(); first++) {
                        TrickCardMetrics.Slot slot = layout.slotForSeat(first);
                        assertEquals("slot.seat() must equal first", first, slot.seat());
                        assertTrue("slot.row() >= 0 && slot.row() < layout.rows() must hold", slot.row() >= 0 && slot.row() < layout.rows());
                        assertTrue("slot.leftDp() >= 0 must hold", slot.leftDp() >= 0);
                        assertTrue("slot.leftDp() + layout.cardWidthDp() <= width must hold", slot.leftDp() + layout.cardWidthDp() <= width);
                        TableGeometry.Rect cardAndLabel = new TableGeometry.Rect(slot.leftDp(),
                                plan.center().top() + slot.row() * layout.cellHeightDp(),
                                slot.leftDp() + layout.cardWidthDp(),
                                plan.center().top() + (slot.row() + 1) * layout.cellHeightDp());
                        for (TableGeometry.Rect seat : seats) {
                            assertFalse("Played card or player label covers a seat at " + width
                                            + " dp, " + players + " players, font " + fontScale,
                                    cardAndLabel.overlaps(seat));
                        }
                        for (int second = first + 1; second < layout.slots().size(); second++) {
                            TrickCardMetrics.Slot other = layout.slotForSeat(second);
                            if (slot.row() != other.row()) continue;
                            TableGeometry.Rect otherCard = new TableGeometry.Rect(other.leftDp(),
                                    plan.center().top() + other.row() * layout.cellHeightDp(),
                                    other.leftDp() + layout.cardWidthDp(),
                                    plan.center().top() + (other.row() + 1) * layout.cellHeightDp());
                            assertFalse("Two cards share a slot at " + width + " dp, players="
                                            + players + ", seats=" + first + "/" + second,
                                    cardAndLabel.overlaps(otherCard));
                        }
                    }
                    assertEquals("layout.slotForSeat(0).row() must equal layout.rows() - 1", layout.rows() - 1, layout.slotForSeat(0).row());
                    assertEquals("The player's card remains centered at the bottom",
                            (width - layout.cardWidthDp()) / 2,
                            layout.slotForSeat(0).leftDp(), 1);
                }
            }
        }
    }

    @Test public void fourPlayersFormAStableCrossAroundTheHumanBottomSeat() {
        for (int width : new int[]{240, 280, 313, 353, 560}) {
            TrickCardMetrics.Layout layout = TrickCardMetrics.forTable(4, width);
            TrickCardMetrics.Slot you = layout.slotForSeat(0);
            TrickCardMetrics.Slot left = layout.slotForSeat(1);
            TrickCardMetrics.Slot north = layout.slotForSeat(2);
            TrickCardMetrics.Slot right = layout.slotForSeat(3);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "layout", layout, "left", left, "north", north, "right", right, "width", width, "you", you);
            assertEquals("you.row() must equal 2", 2, you.row());
            assertEquals("left.row() must equal 1", 1, left.row());
            assertEquals("north.row() must equal 0", 0, north.row());
            assertEquals("right.row() must equal 1", 1, right.row());
            assertTrue("left.leftDp() < right.leftDp() must hold", left.leftDp() < right.leftDp());
            assertEquals((width - layout.cardWidthDp()) / 2, you.leftDp(), 1);
            assertEquals((width - layout.cardWidthDp()) / 2, north.leftDp(), 1);
        }
    }

    @Test public void outerSeatsRemainMappedToTheirSideOfTheTrickForThreeFiveAndSixPlayers() {
        var three = TrickCardMetrics.forTable(3, 353);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "three", three);
        assertTrue("three.slotForSeat(1).leftDp() < three.slotForSeat(2).leftDp() must hold", three.slotForSeat(1).leftDp() < three.slotForSeat(2).leftDp());
        assertEquals("three.slotForSeat(1).row() must equal 0", 0, three.slotForSeat(1).row());
        assertEquals("three.slotForSeat(2).row() must equal 0", 0, three.slotForSeat(2).row());

        var five = TrickCardMetrics.forTable(5, 353);
        assertTrue("five.slotForSeat(2).leftDp() < five.slotForSeat(3).leftDp() must hold", five.slotForSeat(2).leftDp() < five.slotForSeat(3).leftDp());
        assertTrue("five.slotForSeat(1).leftDp() < five.slotForSeat(0).leftDp() must hold", five.slotForSeat(1).leftDp() < five.slotForSeat(0).leftDp());
        assertTrue("five.slotForSeat(0).leftDp() < five.slotForSeat(4).leftDp() must hold", five.slotForSeat(0).leftDp() < five.slotForSeat(4).leftDp());
        assertEquals("five.slotForSeat(0).row() must equal 1", 1, five.slotForSeat(0).row());

        var six = TrickCardMetrics.forTable(6, 353);
        assertTrue("six.slotForSeat(2).leftDp() < six.slotForSeat(3).leftDp() must hold", six.slotForSeat(2).leftDp() < six.slotForSeat(3).leftDp());
        assertTrue("six.slotForSeat(3).leftDp() < six.slotForSeat(4).leftDp() must hold", six.slotForSeat(3).leftDp() < six.slotForSeat(4).leftDp());
        assertTrue("six.slotForSeat(1).leftDp() < six.slotForSeat(0).leftDp() must hold", six.slotForSeat(1).leftDp() < six.slotForSeat(0).leftDp());
        assertTrue("six.slotForSeat(0).leftDp() < six.slotForSeat(5).leftDp() must hold", six.slotForSeat(0).leftDp() < six.slotForSeat(5).leftDp());
    }

    @Test public void rejectsUnsupportedPlayerCountsInvalidFontScalesAndNarrowTables() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 2, "declared constant 2", 353, "declared constant 3", 7, "declared constant 4", 4, "declared constant 5", 0, "declared constant 6", 239, "declared constant 7", 1f, "declared constant 8", 71, "declared constant 9", 63);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(2, 353));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(7, 353));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, Float.NaN));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, Float.POSITIVE_INFINITY));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 239));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, 1f, 71));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, 1f, 63));
    }

    @Test public void shortWindowsScaleBothDimensionsWithoutLosingSeatSlots() {
        for (int players : new int[]{3, 4, 5, 6}) {
            var layout = TrickCardMetrics.forTable(players, 320, 1f, 64, 2);
            var tall = TrickCardMetrics.forTable(players, 320, 1f, 70, 2);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "layout", layout, "players", players, "tall", tall);
            assertEquals("layout.cardHeightDp() must equal 64", 64, layout.cardHeightDp());
            assertEquals("layout.itemVerticalPaddingDp() must equal 2", 2, layout.itemVerticalPaddingDp());
            assertEquals("layout.cardWidthDp() must equal 46", 46, layout.cardWidthDp());
            assertEquals("tall.cardWidthDp() must equal 50", 50, tall.cardWidthDp());
            for (int seat=0;seat<players;seat++) {
                assertEquals("layout.slotForSeat(seat).row() must equal tall.slotForSeat(seat).row()", tall.slotForSeat(seat).row(), layout.slotForSeat(seat).row());
                assertEquals(tall.slotForSeat(seat).leftDp()+tall.cardWidthDp()/2.0,
                        layout.slotForSeat(seat).leftDp()+layout.cardWidthDp()/2.0,1);
            }
            assertEquals("layout.centerHeightDp() must equal layout.rows() * (layout.cardHeightDp() + layout.playerLabelHeightDp() + layout.itemVerticalPaddingDp())", layout.rows() * (layout.cardHeightDp() + layout.playerLabelHeightDp()
                            + layout.itemVerticalPaddingDp()), layout.centerHeightDp());
        }
    }
}

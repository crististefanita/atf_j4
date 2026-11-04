package ro.whistlab.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TrickCardMetricsComponentTest {
    @Test public void doubledCardFacesKeepSeatAnchorsAndAvoidSeatsAcrossWidthsAndTextScales() {
        for (int width : new int[]{240, 280, 313, 353, 560, 760}) {
            for (int players = 3; players <= 6; players++) {
                for (float fontScale : new float[]{1f, 1.3f, 1.5f}) {
                    TrickCardMetrics.Layout layout = TrickCardMetrics.forTable(players, width, fontScale);
                    int expectedRows = players == 4 ? 3 : 2;
                    assertEquals("Played-card face height must remain exactly doubled",
                            TrickCardMetrics.PREVIOUS_CARD_HEIGHT_DP * 2, layout.cardHeightDp());
                    assertTrue(layout.cardWidthDp() >= TrickCardMetrics.MIN_CARD_WIDTH_DP);
                    assertTrue(layout.cardWidthDp() <= TrickCardMetrics.MAX_CARD_WIDTH_DP);
                    assertEquals(width, layout.centerWidthDp());
                    assertTrue(layout.safeCenterWidthDp() <= width);
                    assertEquals(expectedRows, layout.rows());
                    assertEquals(layout.rows() * (layout.cardHeightDp() + layout.playerLabelHeightDp()
                                    + TrickCardMetrics.ITEM_VERTICAL_PADDING_DP),
                            layout.centerHeightDp());
                    assertTrue(layout.playerLabelHeightDp()
                            >= Math.ceil(TrickCardMetrics.PLAYER_LABEL_HEIGHT_DP * fontScale));

                    int seatHeight = (int) Math.ceil(70 * fontScale);
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
                        assertEquals(first, slot.seat());
                        assertTrue(slot.row() >= 0 && slot.row() < layout.rows());
                        assertTrue(slot.leftDp() >= 0);
                        assertTrue(slot.leftDp() + layout.cardWidthDp() <= width);
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
                    assertEquals(layout.rows() - 1, layout.slotForSeat(0).row());
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
            assertEquals(2, you.row());
            assertEquals(1, left.row());
            assertEquals(0, north.row());
            assertEquals(1, right.row());
            assertTrue(left.leftDp() < right.leftDp());
            assertEquals((width - layout.cardWidthDp()) / 2, you.leftDp(), 1);
            assertEquals((width - layout.cardWidthDp()) / 2, north.leftDp(), 1);
        }
    }

    @Test public void outerSeatsRemainMappedToTheirSideOfTheTrickForThreeFiveAndSixPlayers() {
        var three = TrickCardMetrics.forTable(3, 353);
        assertTrue(three.slotForSeat(1).leftDp() < three.slotForSeat(2).leftDp());
        assertEquals(0, three.slotForSeat(1).row());
        assertEquals(0, three.slotForSeat(2).row());

        var five = TrickCardMetrics.forTable(5, 353);
        assertTrue(five.slotForSeat(1).leftDp() < five.slotForSeat(2).leftDp());
        assertTrue(five.slotForSeat(4).leftDp() < five.slotForSeat(0).leftDp());
        assertTrue(five.slotForSeat(0).leftDp() < five.slotForSeat(3).leftDp());
        assertEquals(1, five.slotForSeat(0).row());

        var six = TrickCardMetrics.forTable(6, 353);
        assertTrue(six.slotForSeat(1).leftDp() < six.slotForSeat(2).leftDp());
        assertTrue(six.slotForSeat(2).leftDp() < six.slotForSeat(3).leftDp());
        assertTrue(six.slotForSeat(4).leftDp() < six.slotForSeat(0).leftDp());
        assertTrue(six.slotForSeat(0).leftDp() < six.slotForSeat(5).leftDp());
    }

    @Test public void rejectsUnsupportedPlayerCountsInvalidFontScalesAndNarrowTables() {
        assertThrows(IllegalArgumentException.class, () -> TrickCardMetrics.forTable(2, 353));
        assertThrows(IllegalArgumentException.class, () -> TrickCardMetrics.forTable(7, 353));
        assertThrows(IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, 0));
        assertThrows(IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 353, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> TrickCardMetrics.forTable(4, 239));
    }
}

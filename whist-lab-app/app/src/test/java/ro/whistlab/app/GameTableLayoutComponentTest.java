package ro.whistlab.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** JVM component contracts for the table, bid panel, and overlapping hand geometry. */
public final class GameTableLayoutComponentTest {
    @Test public void handsFromOneToSeventeenCardsStayOnOneRowAndReserveLiftSpace() {
        for (int count = 1; count <= 17; count++) {
            assertTrue("hand of " + count + " cards", HandLayoutMetrics.usesSingleRow(count));
            assertEquals(98, HandLayoutMetrics.cardHeightDp());
            assertEquals(126, HandLayoutMetrics.reservedHandHeightDp());
        }
        assertFalse(HandLayoutMetrics.usesSingleRow(0));
        assertFalse(HandLayoutMetrics.usesSingleRow(18));
        assertEquals(28, HandLayoutMetrics.LIFT_CLEARANCE_DP);
        assertEquals(20, HandLayoutMetrics.CARD_LIFT_DP);
        assertEquals(1.12f, HandLayoutMetrics.CARD_LIFT_SCALE, 0.001f);
        assertTrue(HandLayoutMetrics.LIFT_CLEARANCE_DP >= HandLayoutMetrics.CARD_LIFT_DP
                + (int) Math.ceil((HandLayoutMetrics.CARD_HEIGHT_DP
                        * (HandLayoutMetrics.CARD_LIFT_SCALE - 1f)) / 2f));
        assertTrue(HandLayoutMetrics.SELECTED_Z_DP < HandLayoutMetrics.NEIGHBOR_Z_DP);
    }

    @Test public void thirteenCardsFitTheReferenceViewportAndSeventeenScrollWithoutWrapping() {
        assertFalse(HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1f));
        assertTrue(HandLayoutMetrics.requiresHorizontalScroll(17, 353, 1f));
        assertTrue(HandLayoutMetrics.usesSingleRow(17));
        assertTrue(HandLayoutMetrics.requiresHorizontalScroll(17, 240, 1f));
        assertTrue(HandLayoutMetrics.horizontalAdvanceDp(13, 353, 1f)
                >= HandLayoutMetrics.MIN_VISIBLE_ADVANCE_DP);
        assertEquals(342, HandLayoutMetrics.contentWidthDp(13, 353, 1f));
    }

    @Test public void bidGridKeepsTheMockShapeAndUsesSixColumnsForLargeSetsWhenTheyFit() {
        assertEquals(0, BidGridMetrics.rowCount(0, 353));
        assertEquals(1, BidGridMetrics.rowCount(3, 353));
        assertEquals(3, BidGridMetrics.rowCount(9, 353));
        assertEquals(6, BidGridMetrics.rowCount(18, 240));
        assertEquals(6, BidGridMetrics.columnCount(18, 353));
        assertEquals(3, BidGridMetrics.columnCount(18, 240));
        assertEquals(48, BidGridMetrics.CHOICE_TARGET_DP);
        assertEquals(48, BidGridMetrics.SUBMIT_TARGET_DP);
        assertTrue(TableLayoutMetrics.biddingCenterRowHeightDp(18, 240)
                > TableLayoutMetrics.biddingCenterRowHeightDp(18, 353));
        assertTrue(TableLayoutMetrics.biddingHeightDp(3, 18, 240, 1f)
                > TableLayoutMetrics.playingHeightDp(3, 1f));
    }

    @Test public void humanSeatStaysSouthCenterAndEveryPlayerGetsAUniquePerimeterSlot() {
        assertEquals(360, TableLayoutMetrics.playingHeightDp(3, 1f));
        int previousBoardHeight = 0;
        for (int players = 3; players <= 6; players++) {
            TableSeatLayout.Anchor[] seats = TableSeatLayout.positions(players);
            TableSeatLayout.Anchor[] expected = switch (players) {
                case 3 -> new TableSeatLayout.Anchor[]{TableSeatLayout.Anchor.SOUTH,
                        TableSeatLayout.Anchor.NORTH_WEST, TableSeatLayout.Anchor.NORTH_EAST};
                case 4 -> new TableSeatLayout.Anchor[]{TableSeatLayout.Anchor.SOUTH,
                        TableSeatLayout.Anchor.WEST, TableSeatLayout.Anchor.NORTH, TableSeatLayout.Anchor.EAST};
                case 5 -> new TableSeatLayout.Anchor[]{TableSeatLayout.Anchor.SOUTH,
                        TableSeatLayout.Anchor.NORTH_WEST, TableSeatLayout.Anchor.NORTH_EAST,
                        TableSeatLayout.Anchor.SOUTH_EAST, TableSeatLayout.Anchor.SOUTH_WEST};
                case 6 -> new TableSeatLayout.Anchor[]{TableSeatLayout.Anchor.SOUTH,
                        TableSeatLayout.Anchor.NORTH_WEST, TableSeatLayout.Anchor.NORTH,
                        TableSeatLayout.Anchor.NORTH_EAST, TableSeatLayout.Anchor.SOUTH_WEST,
                        TableSeatLayout.Anchor.SOUTH_EAST};
                default -> throw new AssertionError("Unsupported seat count " + players);
            };
            assertEquals(players, seats.length);
            assertEquals(TableSeatLayout.Anchor.SOUTH, seats[0]);
            Set<TableSeatLayout.Anchor> unique = new HashSet<>();
            for (int index = 0; index < seats.length; index++) {
                assertEquals("seat order follows the accepted oval for " + players + " players",
                        expected[index], seats[index]);
                unique.add(seats[index]);
            }
            assertEquals("all seats are distinct for " + players + " players", players, unique.size());
            int boardHeight = TableLayoutMetrics.playingHeightDp(players, 1f);
            assertTrue(boardHeight > previousBoardHeight);
            assertTrue(TableLayoutMetrics.playingCenterRowHeightDp(players, 1f)
                    > TableLayoutMetrics.playingOuterRowHeightDp(players, 1f));
            previousBoardHeight = boardHeight;
        }
        assertEquals(0.18f, TableSeatLayout.sideInsetFraction(true), 0.001f);
        assertEquals(0.23f, TableSeatLayout.sideInsetFraction(false), 0.001f);
    }

    @Test public void trickFitsBetweenPerimeterSeatsAndKeepsTheMockTwoColumnShape() {
        assertEquals(162, TableLayoutMetrics.centerPlayWidthDp(353));
        assertEquals(148, TableLayoutMetrics.centerPlayWidthDp(280));
        assertEquals(320, TableLayoutMetrics.centerPlayWidthDp(760));
        assertEquals(102, TableLayoutMetrics.seatWidthDp(353));
        assertTrue(TableLayoutMetrics.seatWidthDp(760) < 760 / 3);
        assertTrue(TableLayoutMetrics.playingCenterRowHeightDp(6, 1f) >= 170);
    }
}

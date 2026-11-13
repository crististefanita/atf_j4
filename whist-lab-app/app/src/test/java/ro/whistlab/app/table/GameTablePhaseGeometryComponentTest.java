package ro.whistlab.app.table;

import org.junit.Test;
import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** A hand keeps the same measured table anchors while the center changes from bid to trick. */
public final class GameTablePhaseGeometryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void seatsStayAtIdenticalCoordinatesFromBiddingThroughPlay() {
        int[] widths = {280, 313, 353, 560, 760};
        int[] handSizes = {1, 2, 4, 8, 13};
        float[] fontScales = {1f, 1.3f, 1.5f, 2f};
        for (int players = 3; players <= 6; players++) for (int width : widths)
            for (int handSize : handSizes) for (float fontScale : fontScales) {
                var viewport = new TableViewportPolicy(569, fontScale);
                var reservation = TableLayoutMetrics.centerReservation(players, handSize, width,
                        fontScale, viewport);
                var bids = BidGridMetrics.actionLayout(handSize + 1, width, fontScale);
                var trick = TrickCardMetrics.forTable(players, width, fontScale, viewport);
                int seatWidth = ro.cardtable.ui.table.TableSizing.seatWidthDp(width, viewport.isCompact());
                int seatHeight = 82;
                int gap = viewport.regionGapDp();
                int height = TableGeometry.minimumHeight(players, seatHeight,
                        reservation.reservedHeightDp(), gap) + 120;
                var bidding = TableGeometry.arrangeDisplay(width, height, players, seatWidth, seatHeight,
                        bids.centerWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN,
                        TableLayoutMetrics.humanHandClearanceDp());
                var playing = TableGeometry.arrangeDisplay(width, height, players, seatWidth, seatHeight,
                        trick.safeCenterWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.BALANCED,
                        TableLayoutMetrics.humanHandClearanceDp());

                atf.gradle.audit.GradleInputs.fixture("One viewport and one reserved band across phases",
                        "players", players, "width", width, "hand size", handSize,
                        "font scale", fontScale, "bidding center", bids, "trick center", trick,
                        "reservation", reservation, "bidding plan", bidding, "playing plan", playing);
                assertEquals("The full ordered seat rectangles remain identical during the hand",
                        playing.seats(), bidding.seats());
                assertTrue("The reservation covers bidding and playing controls",
                        reservation.reservedHeightDp() >= bids.centerHeightDp()
                                && reservation.reservedHeightDp() >= trick.centerHeightDp());
                assertTrue("The bid area stays clear of every seat",
                        bidding.seats().stream().noneMatch(bidding.center()::overlaps));
                assertTrue("The trick area stays clear of every seat",
                        playing.seats().stream().noneMatch(playing.center()::overlaps));
                assertEquals("The human seat keeps the same breathing room above the separate hand",
                        height - TableLayoutMetrics.humanHandClearanceDp(), playing.seats().getFirst().bottom());
            }
    }

    @Test public void fourCardTableKeepsTheHumanHandClearanceAcrossViewportAndDensityChanges() {
        int[] widths = {280, 313, 353, 560, 760};
        int[] spareHeightsDp = {0, 8, 24, 72};
        float[] densities = {1f, 1.5f, 2f, 2.75f, 3.5f};
        float[] fontScales = {1f, 1.3f, 1.5f, 2f};
        for (int width : widths) for (float density : densities) for (float fontScale : fontScales) {
            var viewport = new TableViewportPolicy(569, fontScale);
            var reservation = TableLayoutMetrics.centerReservation(4, 4, width, fontScale, viewport);
            var bids = BidGridMetrics.actionLayout(5, width, fontScale);
            var trick = TrickCardMetrics.forTable(4, width, fontScale, viewport);
            int pixelWidth = Math.round(width * density);
            int seatWidth = Math.round(ro.cardtable.ui.table.TableSizing.seatWidthDp(
                    width, viewport.isCompact()) * density);
            int seatHeight = Math.round((52 + 30 * fontScale) * density);
            int centerHeight = Math.round(reservation.reservedHeightDp() * density);
            int gap = Math.round(viewport.regionGapDp() * density);
            int minimum = TableGeometry.minimumHeight(4, seatHeight, centerHeight, gap);
            int requestedClearance = Math.round(TableLayoutMetrics.humanHandClearanceDp() * density);

            for (int spareDp : spareHeightsDp) {
                int sparePx = Math.round(spareDp * density);
                int height = minimum + sparePx;
                var bidding = TableGeometry.arrangeDisplay(pixelWidth, height, 4, seatWidth, seatHeight,
                        Math.round(bids.centerWidthDp() * density), centerHeight, gap, true,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN, requestedClearance);
                var playing = TableGeometry.arrangeDisplay(pixelWidth, height, 4, seatWidth, seatHeight,
                        Math.round(trick.safeCenterWidthDp() * density), centerHeight, gap, true,
                        TableGeometry.CenterAlignment.BALANCED, requestedClearance);
                int actualClearance = Math.min(requestedClearance, sparePx);

                atf.gradle.audit.GradleInputs.fixture("Four-card four-seat table at the same viewport in both phases",
                        "widthDp", width, "heightPx", height, "density", density,
                        "fontScale", fontScale, "spareHeightDp", spareDp,
                        "reservation", reservation, "bidding", bidding, "playing", playing,
                        "requestedClearancePx", requestedClearance, "actualClearancePx", actualClearance);
                assertEquals("Bidding and playing preserve every seat rectangle",
                        bidding.seats(), playing.seats());
                assertEquals("The board uses only measured vertical slack for the hand gap",
                        actualClearance, height - playing.seats().getFirst().bottom());
                assertTrue("The playing center stays above the hand clearance",
                        playing.center().bottom() <= height - actualClearance);
                assertTrue("The playing center remains clear of all seat frames",
                        playing.seats().stream().noneMatch(playing.center()::overlaps));
                assertTrue("The full table remains within the viewport",
                        playing.seats().stream().allMatch(seat -> seat.top() >= 0 && seat.bottom() <= height)
                                && playing.center().top() >= 0 && playing.center().bottom() <= height);
            }
        }
    }

    @Test public void theFullPrivateFaceAndLiftFitInTheSameBandAcrossBothPhases() {
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.625f, 2.75f, 3.5f}) {
            int expectedFace = Math.round(88 * density);
            int expectedLiftSpace = Math.round(28 * density);
            int band = TableLayoutMetrics.privateHandHeightPx(density);
            atf.gradle.audit.GradleInputs.fixture("Private card face and lift in the fixed hand region",
                    "density", density, "facePx", expectedFace, "liftSpacePx", expectedLiftSpace);
            assertEquals("A full face and its top clearance fit with native pixel rounding",
                    expectedFace + expectedLiftSpace, band);
        }
    }

    @Test public void theLargerHandGapRaisesTheHumanSeatAcrossPlayerCountsAndDensities() {
        for (float density : new float[]{1f, 1.5f, 2f, 2.75f, 3.5f})
            for (int players = 3; players <= 6; players++) {
                int width = Math.round(353 * density);
                int seatWidth = Math.round(ro.cardtable.ui.table.TableSizing.seatWidthDp(353, true) * density);
                int seatHeight = Math.round(82 * density);
                int centerHeight = Math.round(145 * density);
                int gap = Math.max(1, Math.round(density));
                int minimum = TableGeometry.minimumHeight(players, seatHeight, centerHeight, gap);
                int height = minimum + Math.round(96 * density);
                int centerWidth = Math.round(170 * density);
                var previous = TableGeometry.arrangeDisplay(width, height, players, seatWidth,
                        seatHeight, centerWidth, centerHeight, gap, true,
                        TableGeometry.CenterAlignment.BALANCED, Math.round(16 * density));
                var raised = TableGeometry.arrangeDisplay(width, height, players, seatWidth,
                        seatHeight, centerWidth, centerHeight, gap, true,
                        TableGeometry.CenterAlignment.BALANCED,
                        Math.round(TableLayoutMetrics.humanHandClearanceDp() * density));

                atf.gradle.audit.GradleInputs.fixture("The larger card gap raises seats only into available space",
                        "players", players, "density", density, "previous", previous, "raised", raised);
                assertEquals("The south seat rises by the additional twelve dp",
                        Math.round(12 * density), previous.seats().getFirst().bottom()
                                - raised.seats().getFirst().bottom());
                for (int seat = 1; seat < players; seat++) {
                    assertTrue("No other seat moves lower when the south seat rises",
                            raised.seats().get(seat).top() <= previous.seats().get(seat).top()
                                    && raised.seats().get(seat).bottom() <= previous.seats().get(seat).bottom());
                }
                assertTrue("The raised seats remain clear of the trick area",
                        raised.seats().stream().noneMatch(raised.center()::overlaps));
            }
    }

    @Test public void aHandReservesTheLargerOfItsMaximumBidGridAndTrickBand() {
        for (int players = 3; players <= 6; players++) for (int handSize : new int[]{1, 8, 13}) {
            int width = 353;
            var viewport = new TableViewportPolicy(569);
            var reservation = TableLayoutMetrics.centerReservation(players, handSize, width, 1f, viewport);
            int expectedBid = BidGridMetrics.actionLayout(handSize + 1, width, 1f).centerHeightDp();
            int expectedPlay = TrickCardMetrics.forTable(players, width, 1f, viewport).centerHeightDp();
            atf.gradle.audit.GradleInputs.fixture("Phase-independent center reservation",
                    "players", players, "hand size", handSize, "reservation", reservation,
                    "expected bid height", expectedBid, "expected play height", expectedPlay);
            assertEquals("The reserved band is the larger measured phase", Math.max(expectedBid, expectedPlay),
                    reservation.reservedHeightDp());
            assertEquals("The bid band's source measurement remains explicit", expectedBid,
                    reservation.biddingHeightDp());
            assertEquals("The play band's source measurement remains explicit", expectedPlay,
                    reservation.playingHeightDp());
        }
    }

    @Test public void threePlayerSchedulesKeepAnchorsStableAcrossEveryModeAndHandSize() {
        int[] widths = {280, 360, 393};
        int[] heights = {440, 560, 620};
        float[] fontScales = {1f, 1.5f, 2f};
        for (GameMode mode : GameMode.values()) {
            var schedule = GameRules.handSizes(mode, 3);
            int largestHand = schedule.stream().mapToInt(Integer::intValue).max().orElseThrow();
            for (int width : widths) for (int height : heights) for (float fontScale : fontScales) {
                var viewport = new TableViewportPolicy(height, fontScale);
                var reservation = TableLayoutMetrics.matchCenterReservation(mode, 3, width,
                        fontScale, viewport);
                var trick = TrickCardMetrics.forTable(3, width, fontScale, viewport);
                int seatWidth = ro.cardtable.ui.table.TableSizing.seatWidthDp(width, viewport.isCompact());
                int seatHeight = Math.round(64 * Math.max(1f, fontScale));
                int gap = viewport.regionGapDp();
                int boardHeight = Math.max(height - 170,
                        TableGeometry.minimumHeight(3, seatHeight, reservation.reservedHeightDp(), gap));
                var baseline = TableGeometry.arrangeDisplay(width, boardHeight, 3, seatWidth, seatHeight,
                        width, reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.BALANCED,
                        TableLayoutMetrics.humanHandClearanceDp());

                atf.gradle.audit.GradleInputs.fixture("Three-player complete schedule geometry",
                        "mode", mode, "schedule", schedule, "widthDp", width,
                        "heightDp", height, "fontScale", fontScale,
                        "largestHand", largestHand, "reservation", reservation, "baseline", baseline);
                assertEquals("The reservation covers the largest bid grid in the full schedule",
                        BidGridMetrics.actionLayout(largestHand + 1, width, fontScale).centerHeightDp(),
                        reservation.biddingHeightDp());
                assertEquals("The reservation covers the current table's complete trick",
                        trick.centerHeightDp(), reservation.playingHeightDp());

                for (int handSize : schedule) {
                    var bids = BidGridMetrics.actionLayout(handSize + 1, width, fontScale);
                    var bidding = TableGeometry.arrangeDisplay(width, boardHeight, 3, seatWidth, seatHeight,
                            bids.centerWidthDp(), reservation.reservedHeightDp(), gap, true,
                            TableGeometry.CenterAlignment.TOWARD_HUMAN,
                            TableLayoutMetrics.humanHandClearanceDp());
                    var playing = TableGeometry.arrangeDisplay(width, boardHeight, 3, seatWidth, seatHeight,
                            trick.safeCenterWidthDp(), reservation.reservedHeightDp(), gap, true,
                            TableGeometry.CenterAlignment.BALANCED,
                            TableLayoutMetrics.humanHandClearanceDp());
                    assertEquals("Every scheduled hand preserves all three seat anchors",
                            baseline.seats(), bidding.seats());
                    assertEquals("The table stays fixed between bidding and playing",
                            bidding.seats(), playing.seats());
                    assertTrue("The bid selector stays clear of the seats",
                            bidding.seats().stream().noneMatch(bidding.center()::overlaps));
                    assertTrue("The trick stays clear of the seats",
                            playing.seats().stream().noneMatch(playing.center()::overlaps));
                }
            }
        }
    }

    @Test public void fivePlayerReverseOhHellKeepsItsAnchorsAcrossTheMatchSchedule() {
        int[] widths = {280, 360, 393};
        int[] heights = {440, 560, 620};
        float[] fontScales = {1f, 2f};
        var schedule = GameRules.handSizes(GameMode.REVERSE_OH_HELL, 5);
        int largestHand = schedule.stream().mapToInt(Integer::intValue).max().orElseThrow();
        for (int width : widths) for (int height : heights) for (float fontScale : fontScales) {
            var viewport = new TableViewportPolicy(height, fontScale);
            var reservation = TableLayoutMetrics.matchCenterReservation(GameMode.REVERSE_OH_HELL,
                    5, width, fontScale, viewport);
            var trick = TrickCardMetrics.forTable(5, width, fontScale, viewport);
            int seatWidth = ro.cardtable.ui.table.TableSizing.seatWidthDp(width, viewport.isCompact());
            int seatHeight = Math.round(64 * Math.max(1f, fontScale));
            int gap = viewport.regionGapDp();
            int boardHeight = Math.max(height - 170,
                    TableGeometry.minimumHeight(5, seatHeight, reservation.reservedHeightDp(), gap));
            var baseline = TableGeometry.arrangeDisplay(width, boardHeight, 5, seatWidth, seatHeight,
                    width, reservation.reservedHeightDp(), gap, true,
                    TableGeometry.CenterAlignment.BALANCED,
                    TableLayoutMetrics.humanHandClearanceDp());

            atf.gradle.audit.GradleInputs.fixture("Five-player Reverse Oh Hell schedule geometry",
                    "schedule", schedule, "widthDp", width, "boardHeightDp", boardHeight,
                    "fontScale", fontScale, "reservation", reservation, "baseline", baseline);
            assertEquals("All five seats reserve the maximum scheduled bid grid",
                    BidGridMetrics.actionLayout(largestHand + 1, width, fontScale).centerHeightDp(),
                    reservation.biddingHeightDp());
            assertEquals("All five seats reserve the complete trick band",
                    trick.centerHeightDp(), reservation.playingHeightDp());

            for (int handSize : schedule) {
                var bids = BidGridMetrics.actionLayout(handSize + 1, width, fontScale);
                var bidding = TableGeometry.arrangeDisplay(width, boardHeight, 5, seatWidth, seatHeight,
                        bids.centerWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN,
                        TableLayoutMetrics.humanHandClearanceDp());
                var playing = TableGeometry.arrangeDisplay(width, boardHeight, 5, seatWidth, seatHeight,
                        trick.safeCenterWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.BALANCED,
                        TableLayoutMetrics.humanHandClearanceDp());
                assertEquals("Every scheduled hand preserves the five seat anchors",
                        baseline.seats(), bidding.seats());
                assertEquals("The same player frames remain during bidding and play",
                        bidding.seats(), playing.seats());
            }
        }
    }

    @Test public void threePlayerScheduleAcceptsSeventeenCardsAndRejectsEighteen() {
        var viewport = new TableViewportPolicy(569);
        var largest = TableLayoutMetrics.centerReservation(3, 17, 353, 1f, viewport);
        atf.gradle.audit.GradleInputs.fixture("Three-player legal maximum hand", "cards", 17,
                "bid grid", BidGridMetrics.actionLayout(18, 353, 1f), "reservation", largest);
        assertEquals("The maximum legal three-player hand reserves its responsive bid grid",
                BidGridMetrics.actionLayout(18, 353, 1f).centerHeightDp(), largest.biddingHeightDp());
        assertThrows("An eighteen-card selector is outside a 52-card deal to three players",
                IllegalArgumentException.class,
                () -> TableLayoutMetrics.centerReservation(3, 18, 353, 1f, viewport));
    }
}

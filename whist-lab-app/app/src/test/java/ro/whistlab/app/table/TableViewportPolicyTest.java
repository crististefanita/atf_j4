package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public class TableViewportPolicyTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void tallPortraitsReclaimDecorationWhenLargerTextExhaustsTheRoomyBudget() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 818, "declared constant 2", 1f, "declared constant 3", 1.3f, "declared constant 4", 1.5f, "declared constant 5", "Only the physical short-window threshold changes card height", "declared constant 6", 70, "declared constant 7", 3, "declared constant 8", 6, "declared constant 9", 353, "declared constant 10", 42, "declared constant 11", 16, "declared constant 12", 20, "declared constant 13", 13, "declared constant 14", 90, "declared constant 15", 4, "declared constant 16", "Larger text retains the hand and all seat/action regions", "declared constant 17", true, "declared constant 18", 0, "declared constant 19", -1);
        assertFalse("new TableViewportPolicy(818, 1.0F).isCompact() must remain false", new TableViewportPolicy(818,1f).isCompact());
        for(float fontScale:new float[]{1.3f,1.5f}) {
            var viewport=new TableViewportPolicy(818,fontScale);
            assertTrue("viewport.isCompact() must hold", viewport.isCompact());
            assertFalse("viewport.showPlayHint() must remain false", viewport.showPlayHint());
            assertEquals("Only the physical short-window threshold changes card height",70,viewport.playedCardFaceHeightDp());
            for(int players=3;players<=6;players++) {
                var trick=TrickCardMetrics.forTable(players,353,fontScale,viewport);
                int reference=ro.whistlab.app.seat.PlayerSeatMetrics.referenceFrameHeightPx(
                        (int)Math.ceil(42*fontScale),(int)Math.ceil(16*fontScale),20);
                int seat=ro.whistlab.app.seat.PlayerSeatMetrics.arrange(reference,(int)Math.ceil(13*fontScale),3).heightPx();
                int available=818-(int)Math.ceil(90*fontScale)-HandLayoutMetrics.reservedHandHeightDp()-4;
                int minimum=TableGeometry.minimumHeight(players,seat,trick.centerHeightDp(),viewport.regionGapDp());
                assertTrue("Larger text retains the hand and all seat/action regions",minimum<=available);
                var plan=TableGeometry.arrangeDisplay(353,available,players,
                        TableLayoutMetrics.seatWidthDp(353,true),seat,trick.safeCenterWidthDp(),trick.centerHeightDp(),
                        viewport.regionGapDp(),true,TableGeometry.CenterAlignment.BALANCED);
                assertEquals("plan.seats().getFirst().bottom() must equal available", available,plan.seats().getFirst().bottom());
            }
        }
        for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}) {
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new TableViewportPolicy(818,invalid));
        }
    }

    @Test public void tallerFramesAndLowerCompositionKeepEveryRegionInsideTheBoard() {
        for (int width : new int[]{280, 313, 353, 560, 760}) {
            for (int players = 3; players <= 6; players++) {
                for (int naturalHeight : new int[]{48, 62, 82, 110, 156}) {
                    var seat = ro.whistlab.app.seat.PlayerSeatMetrics.arrange(naturalHeight, 12, 3);
                    int seatHeight = seat.heightPx();

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "naturalHeight", naturalHeight, "players", players, "seat", seat, "seatHeight", seatHeight, "width", width);
                    assertEquals("seat.frameHeightPx() must equal (int)Math.ceil(naturalHeight * 1.25)", (int) Math.ceil(naturalHeight * 1.25), seat.frameHeightPx());
                    var trick = TrickCardMetrics.forTable(players, width);
                    int minimum = TableGeometry.minimumHeight(players, seatHeight, trick.centerHeightDp(), 8);
                    for (int extra : new int[]{0, 80, 240}) {
                        int height = minimum + extra;
                        var plan = TableGeometry.arrangeDisplay(width, height, players,
                                TableLayoutMetrics.seatWidthDp(width), seatHeight,
                                trick.safeCenterWidthDp(), trick.centerHeightDp(), 8, true,
                                TableGeometry.CenterAlignment.BALANCED);
                        int inset = Math.min(Math.round(height * 0.05f), extra);
                        assertTrue("TableLayoutMetrics.topInsetPx(height) >= inset must hold", TableLayoutMetrics.topInsetPx(height) >= inset);
                        assertEquals("Human frame stays above the separate hand region",
                                height, plan.seats().getFirst().bottom());
                        var boxes = new java.util.ArrayList<>(plan.seats());
                        boxes.add(plan.center());
                        for (int first = 0; first < boxes.size(); first++) {
                            var box = boxes.get(first);
                            assertTrue("box.top() >= inset && box.bottom() <= height must hold", box.top() >= inset && box.bottom() <= height);
                            for (int second = first + 1; second < boxes.size(); second++) {
                                assertFalse("box.overlaps(boxes.get(second)) must remain false", box.overlaps(boxes.get(second)));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test public void narrowerTrumpTileFitsLongValuesWithoutEnlargingTheFrame() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 57, "declared constant 2", 1, "declared constant 3", 1.5f, "declared constant 4", 2.625f, "declared constant 5", 2.75f, "declared constant 6", 3.5f, "declared constant 7", 2, "declared constant 8", 4, "declared constant 9", 5, "declared constant 10", 1.3f, "declared constant 11", 20, "declared constant 12", 35, "declared constant 13", 48, "declared constant 14", 0.01f, "declared constant 15", "Repeated layout must start from the nominal size", "declared constant 16", 0);
        assertEquals("TableLayoutMetrics.TRUMP_WIDTH_DP must equal 57", 57, TableLayoutMetrics.TRUMP_WIDTH_DP);
        for (float density : new float[]{1, 1.5f, 2.625f, 2.75f, 3.5f}) {
            int available = Math.round(57 * density) - 2 * Math.round(4 * density)
                    - 2 * Math.round(5 * density);
            for (float fontScale : new float[]{1, 1.3f, 1.5f, 2}) {
                float nominal = 20 * fontScale * density;
                for (float measuredGlyphWidth : new float[]{20, 35, 48}) {
                    float measured = measuredGlyphWidth * fontScale * density;
                    float fitted = TableLayoutMetrics.fitTextSizePx(nominal, measured, available);
                    assertTrue("fitted <= nominal must hold", fitted <= nominal);
                    assertTrue("measured * fitted / nominal <= available + 0.01F must hold", measured * fitted / nominal <= available + 0.01f);
                    assertEquals("Repeated layout must start from the nominal size", fitted,
                            TableLayoutMetrics.fitTextSizePx(nominal, measured, available), 0);
                }
            }
        }
    }

    @Test public void shortWindowsReclaimDecorationAndReduceOnlyThePlayedCardFaces() {
        var compact = new TableViewportPolicy(497);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "compact", compact);
        assertTrue("compact.isCompact() must hold", compact.isCompact());
        assertEquals("compact.regionGapDp() must equal 1", 1, compact.regionGapDp());
        assertEquals("compact.minimumBoardHeightDp() must equal 0", 0, compact.minimumBoardHeightDp());
        assertFalse("compact.showPlayHint() must remain false", compact.showPlayHint());
        assertEquals("compact.handClearanceDp(false) must equal 28", 28, compact.handClearanceDp(false));
        assertEquals("compact.handClearanceDp(true) must equal 12", 12, compact.handClearanceDp(true));
        assertEquals("compact.seatVerticalPaddingDp() must equal 1", 1, compact.seatVerticalPaddingDp());
        assertEquals("compact.outerVerticalPaddingDp() must equal 0", 0, compact.outerVerticalPaddingDp());
        assertEquals("compact.playedCardFaceHeightDp() must equal 64", 64, compact.playedCardFaceHeightDp());
        assertEquals("compact.playedCardItemVerticalPaddingDp() must equal 2", 2, compact.playedCardItemVerticalPaddingDp());
        assertEquals("HandLayoutMetrics.cardHeightDp() must equal 88", 88, HandLayoutMetrics.cardHeightDp());
        assertEquals("BidGridMetrics.CHOICE_TARGET_DP must equal 48", 48, BidGridMetrics.CHOICE_TARGET_DP);
        var intermediate = new TableViewportPolicy(626);
        assertTrue("intermediate.isCompact() must hold", intermediate.isCompact());
        assertEquals("intermediate.playedCardFaceHeightDp() must equal 70", 70, intermediate.playedCardFaceHeightDp());
        var roomy = new TableViewportPolicy(818);
        assertFalse("roomy.isCompact() must remain false", roomy.isCompact());
        assertTrue("roomy.showPlayHint() must hold", roomy.showPlayHint());
        assertEquals("roomy.seatVerticalPaddingDp() must equal 4", 4, roomy.seatVerticalPaddingDp());
        assertEquals("roomy.outerVerticalPaddingDp() must equal 4", 4, roomy.outerVerticalPaddingDp());
        assertEquals("roomy.playedCardFaceHeightDp() must equal 70", 70, roomy.playedCardFaceHeightDp());
        assertEquals("roomy.playedCardItemVerticalPaddingDp() must equal TrickCardMetrics.ITEM_VERTICAL_PADDING_DP", TrickCardMetrics.ITEM_VERTICAL_PADDING_DP, roomy.playedCardItemVerticalPaddingDp());
    }

    @Test public void compactHandTrickAndEverySeatFitOneViewportWithoutIntersections() {
        var viewport = new TableViewportPolicy(497);
        int availableBoard = 497 - 4 - 60 - 2 * viewport.outerVerticalPaddingDp()
                - HandLayoutMetrics.reservedHandHeightDp();
        for (int width : new int[]{280, 313, 353}) for (int players = 3; players <= 6; players++) {
            var trick = TrickCardMetrics.forTable(players, width, 1f, viewport);
            int seatWidth = TableLayoutMetrics.seatWidthDp(width, true);
            int reference = ro.whistlab.app.seat.PlayerSeatMetrics.referenceFrameHeightPx(43, 15, 20);
            int seatHeight = ro.whistlab.app.seat.PlayerSeatMetrics.arrange(reference, 12, 3).heightPx();
            int minimum = TableGeometry.minimumHeight(
                    players, seatHeight, trick.centerHeightDp(), viewport.regionGapDp());

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "availableBoard", availableBoard, "minimum", minimum, "players", players, "reference", reference, "seatHeight", seatHeight, "seatWidth", seatWidth, "trick", trick, "viewport", viewport, "width", width);
            assertTrue("The whole table must fit a 497 dp viewport: " + players + " players", minimum <= availableBoard);
            var plan = TableGeometry.arrangeDisplay(width, availableBoard, players, seatWidth, seatHeight,
                    trick.safeCenterWidthDp(), trick.centerHeightDp(), viewport.regionGapDp(), true,
                    TableGeometry.CenterAlignment.BALANCED);
            var occupied = new java.util.ArrayList<>(plan.seats());
            for (var slot : trick.slots()) {
                int top = plan.center().top() + slot.row() * trick.rowStrideDp();
                occupied.add(new TableGeometry.Rect(slot.leftDp(), top,
                        slot.leftDp() + trick.cardWidthDp(), top + trick.cellHeightDp()));
            }
            for (int first = 0; first < occupied.size(); first++) {
                var box = occupied.get(first);
                assertTrue("box.top() >= 0 && box.bottom() <= availableBoard must hold", box.top() >= 0 && box.bottom() <= availableBoard);
                for (int second = first + 1; second < occupied.size(); second++) {
                    assertFalse("Compact regions intersect at " + width + " dp, " + players + " players",
                            box.overlaps(occupied.get(second)));
                }
            }
            assertEquals("trick.cardHeightDp() must equal 64", 64, trick.cardHeightDp());
            assertEquals("Names remain in the seat frames and accessible card descriptions", 0, trick.playerLabelHeightDp());
        }
    }

    @Test public void bidZoneUsesTheLowerPartOfTheAvailableSafeBand() {
        for (int width : new int[]{280, 320, 353, 560}) {
            int seatWidth = TableLayoutMetrics.seatWidthDp(width);
            int columns = BidGridMetrics.columnCount(9, width);
            int centerWidth = Math.max(132, Math.min(9, columns) * 52);
            int centerHeight = BidGridMetrics.rowCount(9, width) * BidGridMetrics.CHOICE_ROW_DP
                    + BidGridMetrics.SUBMIT_TARGET_DP;
            int gap = 3;
            int seatHeight = 62;
            int minimum = TableGeometry.minimumHeight(4, seatHeight, centerHeight, gap);
            for (int extra : new int[]{0, 40, 120}) {
                var plan = TableGeometry.arrange(width, minimum + extra, 4, seatWidth,
                        seatHeight, centerWidth, centerHeight, gap, false,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN);
                var human = plan.seats().getFirst();
                int humanGap = human.top() - plan.center().bottom();
                int upperGap = plan.seats().stream().skip(1)
                        .filter(seat -> seat.bottom() <= plan.center().top())
                        .mapToInt(seat -> plan.center().top() - seat.bottom())
                        .min().orElseThrow();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "centerHeight", centerHeight, "centerWidth", centerWidth, "columns", columns, "extra", extra, "gap", gap, "human", human, "humanGap", humanGap, "minimum", minimum, "plan", plan, "seatHeight", seatHeight, "seatWidth", seatWidth, "upperGap", upperGap, "width", width);
                assertTrue("Bids must sit closer to the human seat at width=" + width
                                + ", extra=" + extra + ": upper=" + upperGap + ", human=" + humanGap,
                        humanGap < upperGap);
                assertTrue("The measured bid area must remain clear of every seat",
                        plan.seats().stream().noneMatch(plan.center()::overlaps));
            }
        }
    }

    @Test public void pixelRoundingKeepsTheFourSeatOpponentArcClearOfThePlayingBand() {
        for (int width : new int[]{280, 313, 353, 560, 760}) for (float density : new float[]{1, 1.5f, 2, 2.25f, 2.75f, 3.5f}) {
            int w = Math.round(width * density), seat = Math.round(TableLayoutMetrics.seatWidthDp(width) * density);
            var plan = TableGeometry.arrange(w, Math.round(400 * density), 4, seat, Math.round(70 * density),
                    Math.round(TableLayoutMetrics.centerPlayWidthDp(width) * density), Math.round(144 * density), Math.round(8 * density));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "plan", plan, "seat", seat, "w", w, "width", width);
            assertTrue("plan.seats().get(1).bottom() <= plan.center().top() must hold", plan.seats().get(1).bottom() <= plan.center().top());
            assertTrue("plan.seats().get(3).bottom() <= plan.center().top() must hold", plan.seats().get(3).bottom() <= plan.center().top());
            assertEquals("plan.seats().get(3).top() must equal plan.seats().get(1).top()", plan.seats().get(1).top(), plan.seats().get(3).top());
            assertEquals("plan.seats().getFirst().bottom() must equal Math.round(400 * density)", Math.round(400*density),plan.seats().getFirst().bottom());
        }
    }
}

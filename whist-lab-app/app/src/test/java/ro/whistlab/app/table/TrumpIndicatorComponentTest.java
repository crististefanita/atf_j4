package ro.whistlab.app.table;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import org.junit.Test;
import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;

/** Presentation projections and injected font measurements, without an Android device. */
public final class TrumpIndicatorComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void everyModeUsesOneSuitOrTurnedCardValueInBothLanguages() {
        for (GameMode mode : GameMode.values()) {
            var base = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, mode, AiDifficulty.NORMAL, 417))).snapshot();
            for (HowToPlay.Language language : HowToPlay.Language.values()) {
                var copy = new WhistUiText(language);
                for (Trump trump : Trump.values()) {
                    if (trump.suit().isEmpty()) continue;
                    var suit = trump.suit().orElseThrow();
                    var suitOnly = withTrump(base, trump, null);

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "base", base, "copy", copy, "language", language, "mode", mode, "suit", suit, "suitOnly", suitOnly, "trump", trump);
                    assertEquals(mode + " " + language, suit.symbol(), copy.trumpIndicatorValue(suitOnly));
                    assertTrue("copy.trumpAccessibility(suitOnly).contains(copy.suitName(suit)) must hold", copy.trumpAccessibility(suitOnly).contains(copy.suitName(suit)));
                    for (int rank : new int[]{7, 10, 11, 12, 13, 14}) {
                        var card = new Card(suit, rank);
                        var turned = withTrump(base, trump, card);
                        assertEquals("copy.trumpIndicatorValue(turned) must equal card.shortLabel()", card.shortLabel(), copy.trumpIndicatorValue(turned));
                        assertTrue("copy.trumpAccessibility(turned).endsWith(copy.cardLabel(card)) must hold", copy.trumpAccessibility(turned).endsWith(copy.cardLabel(card)));
                        assertNotEquals("copy.trumpAccessibility(turned) must differ from copy.trumpAccessibility(suitOnly)", copy.trumpAccessibility(suitOnly), copy.trumpAccessibility(turned));
                    }
                }
            }
        }
    }

    @Test public void noTrumpHasAnExplicitLocalizedValueInsteadOfAnExtraStatusRow() {
        for (GameMode mode : GameMode.values()) {
            var base = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, mode, AiDifficulty.NORMAL, 418))).snapshot();
            var state = withTrump(base, Trump.NO_TRUMP, null);
            var english = new WhistUiText(HowToPlay.Language.EN);
            var romanian = new WhistUiText(HowToPlay.Language.RO);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "base", base, "english", english, "mode", mode, "romanian", romanian, "state", state);
            assertEquals("english.trumpIndicatorValue(state) must equal \"NO TRUMP\"", "NO TRUMP", english.trumpIndicatorValue(state));
            assertEquals("romanian.trumpIndicatorValue(state) must equal \"F\\u0102R\\u0102 ATU\"", "FĂRĂ ATU", romanian.trumpIndicatorValue(state));
            assertEquals("english.trumpAccessibility(state) must equal english.noTrump()", english.noTrump(), english.trumpAccessibility(state));
            assertEquals("romanian.trumpAccessibility(state) must equal romanian.noTrump()", romanian.noTrump(), romanian.trumpAccessibility(state));
        }
    }

    @Test public void aCommonFrameReservesTwoCompleteLinesAtEachDensityAndTextScale() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 57, "declared constant 2", 1f, "declared constant 3", 1.5f, "declared constant 4", 2.25f, "declared constant 5", 2.625f, "declared constant 6", 2.75f, "declared constant 7", 3f, "declared constant 8", 3.5f, "declared constant 9", 4f, "declared constant 10", 1.15f, "declared constant 11", 1.3f, "declared constant 12", 2f, "declared constant 13", 11, "declared constant 14", 24, "declared constant 15", 4, "declared constant 16", 2, "declared constant 17", 48, "declared constant 18", 80, "declared constant 19", 22);
        assertEquals("TableLayoutMetrics.TRUMP_WIDTH_DP must equal 57", 57, TableLayoutMetrics.TRUMP_WIDTH_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.625f, 2.75f, 3f, 3.5f, 4f}) {
            for (float scale : new float[]{1f, 1.15f, 1.3f, 1.5f, 2f}) {
                int heading = (int) Math.ceil(11 * density * scale);
                int value = (int) Math.ceil(24 * density * scale);
                int height = TableLayoutMetrics.trumpHeightPx(density, heading, value);
                int padding = Math.round(4 * density);
                int gap = Math.round(2 * density);
                assertTrue("height >= Math.round(48 * density) must hold", height >= Math.round(48 * density));
                assertTrue("height - 2 * padding - gap >= heading + value must hold", height - 2 * padding - gap >= heading + value);
            }
        }
        assertEquals("TableLayoutMetrics.trumpHeightPx(1.0F, 11, 24) must equal 48", 48, TableLayoutMetrics.trumpHeightPx(1f, 11, 24));
        assertEquals("TableLayoutMetrics.trumpHeightPx(1.0F, 22, 48) must equal 80", 80, TableLayoutMetrics.trumpHeightPx(1f, 22, 48));
    }

    @Test public void longValuesFitTheFrameWithoutChangingItsWidthOrGrowingSmallText() {
        for (float density : new float[]{1f, 2.25f, 2.75f, 3.5f}) {
            int available = Math.round(57 * density) - 2 * Math.round(4 * density);
            for (float nominal : new float[]{9f, 20f, 30f, 40f}) {
                float size = nominal * density;
                for (float measured : new float[]{8f, 35f, 68f, 140f}) {
                    float width = measured * density;
                    float fitted = TableLayoutMetrics.fitTextSizePx(size, width, available);

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "available", available, "density", density, "fitted", fitted, "measured", measured, "nominal", nominal, "size", size, "width", width);
                    assertTrue("fitted > 0 && fitted <= size must hold", fitted > 0 && fitted <= size);
                    assertTrue("width * fitted / size <= available + 0.001F must hold", width * fitted / size <= available + 0.001f);
                    if (width <= available) assertEquals(size, fitted, 0f);
                }
            }
        }
    }

    @Test public void invalidMeasurementsFailBeforeAnUnusableFrameIsDeclared() {
        for (float density : new float[]{0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density);
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(density, 11, 24));
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(1f, 0, 24));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(1f, 11, -1));
    }

    @Test public void bidSubmissionIsTenPercentNarrowerWithAFullTouchTarget() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 132, "declared constant 2", 0.9f, "declared constant 3", 14, "declared constant 4", 48, "declared constant 5", 44, "declared constant 6", 2, "declared constant 7", 1f, "declared constant 8", 1.5f, "declared constant 9", 2.25f, "declared constant 10", 2.75f, "declared constant 11", 3.5f, "declared constant 12", 0);
        assertEquals("BidGridMetrics.SUBMIT_WIDTH_DP must equal Math.round(132 * 0.9F)", Math.round(132 * 0.9f), BidGridMetrics.SUBMIT_WIDTH_DP);
        assertEquals("BidGridMetrics.SUBMIT_TEXT_SP must equal Math.round(14 * 0.9F)", Math.round(14 * 0.9f), BidGridMetrics.SUBMIT_TEXT_SP);
        assertEquals("BidGridMetrics.SUBMIT_TARGET_DP must equal 48", 48, BidGridMetrics.SUBMIT_TARGET_DP);
        assertEquals("BidGridMetrics.SUBMIT_TARGET_DP - 2 * BidGridMetrics.SUBMIT_VISUAL_INSET_DP must equal 44", 44, BidGridMetrics.SUBMIT_TARGET_DP - 2 * BidGridMetrics.SUBMIT_VISUAL_INSET_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.75f, 3.5f}) {
            int touchHeight = Math.round(BidGridMetrics.SUBMIT_TARGET_DP * density);
            int inset = Math.round(BidGridMetrics.SUBMIT_VISUAL_INSET_DP * density);
            assertTrue("touchHeight - 2 * inset > 0 must hold", touchHeight - 2 * inset > 0);
            assertTrue("touchHeight - 2 * inset < touchHeight must hold", touchHeight - 2 * inset < touchHeight);
            assertTrue("Math.round(BidGridMetrics.SUBMIT_WIDTH_DP * density) >= touchHeight must hold", Math.round(BidGridMetrics.SUBMIT_WIDTH_DP * density) >= touchHeight);
        }
    }

    private static GameSession.Snapshot withTrump(GameSession.Snapshot base, Trump trump, Card turned) {
        return new GameSession.Snapshot(base.phase(), base.handIndex(), base.handNumber(), base.handSize(),
                trump, base.dealerSeat(), base.biddingSeat(), base.currentSeat(), base.bidsSubmitted(),
                base.bidTotal(), base.players(), base.currentTrick(), base.lastTrick(), base.lastTrickWinnerSeat(),
                base.finalHand(), base.mode(), base.history(), turned, base.streakBonuses(),
                base.streakBonusPoints(), base.streakPenaltyPoints());
    }
}

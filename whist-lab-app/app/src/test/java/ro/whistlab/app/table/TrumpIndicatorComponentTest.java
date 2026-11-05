package ro.whistlab.app.table;

import static org.junit.Assert.*;

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
public final class TrumpIndicatorComponentTest {
    @Test public void everyModeUsesOneSuitOrTurnedCardValueInBothLanguages() {
        for (GameMode mode : GameMode.values()) {
            var base = GameSession.newGame(new GameConfig(4, mode, AiDifficulty.NORMAL, 417)).snapshot();
            for (HowToPlay.Language language : HowToPlay.Language.values()) {
                var copy = new WhistUiText(language);
                for (Trump trump : Trump.values()) {
                    if (trump.suit().isEmpty()) continue;
                    var suit = trump.suit().orElseThrow();
                    var suitOnly = withTrump(base, trump, null);
                    assertEquals(mode + " " + language, suit.symbol(), copy.trumpIndicatorValue(suitOnly));
                    assertTrue(copy.trumpAccessibility(suitOnly).contains(copy.suitName(suit)));
                    for (int rank : new int[]{7, 10, 11, 12, 13, 14}) {
                        var card = new Card(suit, rank);
                        var turned = withTrump(base, trump, card);
                        assertEquals(card.shortLabel(), copy.trumpIndicatorValue(turned));
                        assertTrue(copy.trumpAccessibility(turned).endsWith(copy.cardLabel(card)));
                        assertNotEquals(copy.trumpAccessibility(suitOnly), copy.trumpAccessibility(turned));
                    }
                }
            }
        }
    }

    @Test public void noTrumpHasAnExplicitLocalizedValueInsteadOfAnExtraStatusRow() {
        for (GameMode mode : GameMode.values()) {
            var base = GameSession.newGame(new GameConfig(4, mode, AiDifficulty.NORMAL, 418)).snapshot();
            var state = withTrump(base, Trump.NO_TRUMP, null);
            var english = new WhistUiText(HowToPlay.Language.EN);
            var romanian = new WhistUiText(HowToPlay.Language.RO);
            assertEquals("NO TRUMP", english.trumpIndicatorValue(state));
            assertEquals("FĂRĂ ATU", romanian.trumpIndicatorValue(state));
            assertEquals(english.noTrump(), english.trumpAccessibility(state));
            assertEquals(romanian.noTrump(), romanian.trumpAccessibility(state));
        }
    }

    @Test public void aCommonFrameReservesTwoCompleteLinesAtEachDensityAndTextScale() {
        assertEquals(57, TableLayoutMetrics.TRUMP_WIDTH_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.625f, 2.75f, 3f, 3.5f, 4f}) {
            for (float scale : new float[]{1f, 1.15f, 1.3f, 1.5f, 2f}) {
                int heading = (int) Math.ceil(11 * density * scale);
                int value = (int) Math.ceil(24 * density * scale);
                int height = TableLayoutMetrics.trumpHeightPx(density, heading, value);
                int padding = Math.round(4 * density);
                int gap = Math.round(2 * density);
                assertTrue(height >= Math.round(48 * density));
                assertTrue(height - 2 * padding - gap >= heading + value);
            }
        }
        assertEquals(48, TableLayoutMetrics.trumpHeightPx(1f, 11, 24));
        assertEquals(80, TableLayoutMetrics.trumpHeightPx(1f, 22, 48));
    }

    @Test public void longValuesFitTheFrameWithoutChangingItsWidthOrGrowingSmallText() {
        for (float density : new float[]{1f, 2.25f, 2.75f, 3.5f}) {
            int available = Math.round(57 * density) - 2 * Math.round(4 * density);
            for (float nominal : new float[]{9f, 20f, 30f, 40f}) {
                float size = nominal * density;
                for (float measured : new float[]{8f, 35f, 68f, 140f}) {
                    float width = measured * density;
                    float fitted = TableLayoutMetrics.fitTextSizePx(size, width, available);
                    assertTrue(fitted > 0 && fitted <= size);
                    assertTrue(width * fitted / size <= available + 0.001f);
                    if (width <= available) assertEquals(size, fitted, 0f);
                }
            }
        }
    }

    @Test public void invalidMeasurementsFailBeforeAnUnusableFrameIsDeclared() {
        for (float density : new float[]{0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(density, 11, 24));
        }
        assertThrows(IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(1f, 0, 24));
        assertThrows(IllegalArgumentException.class, () -> TableLayoutMetrics.trumpHeightPx(1f, 11, -1));
    }

    @Test public void bidSubmissionIsTenPercentNarrowerWithAFullTouchTarget() {
        assertEquals(Math.round(132 * 0.9f), BidGridMetrics.SUBMIT_WIDTH_DP);
        assertEquals(Math.round(14 * 0.9f), BidGridMetrics.SUBMIT_TEXT_SP);
        assertEquals(48, BidGridMetrics.SUBMIT_TARGET_DP);
        assertEquals(44, BidGridMetrics.SUBMIT_TARGET_DP - 2 * BidGridMetrics.SUBMIT_VISUAL_INSET_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.75f, 3.5f}) {
            int touchHeight = Math.round(BidGridMetrics.SUBMIT_TARGET_DP * density);
            int inset = Math.round(BidGridMetrics.SUBMIT_VISUAL_INSET_DP * density);
            assertTrue(touchHeight - 2 * inset > 0);
            assertTrue(touchHeight - 2 * inset < touchHeight);
            assertTrue(Math.round(BidGridMetrics.SUBMIT_WIDTH_DP * density) >= touchHeight);
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

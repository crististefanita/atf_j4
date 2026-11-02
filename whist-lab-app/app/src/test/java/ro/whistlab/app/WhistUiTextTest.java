package ro.whistlab.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import ro.whistlab.core.AiDifficulty;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameConfig;
import ro.whistlab.core.GameMode;
import ro.whistlab.core.GameRules;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.HowToPlay;
import ro.whistlab.core.Suit;

public final class WhistUiTextTest {
    @Test public void centralBidGridKeepsAllOptionsAndReservesSpaceForEveryRow() {
        assertEquals(0, BidGridMetrics.rowCount(0));
        assertEquals(1, BidGridMetrics.rowCount(1));
        assertEquals(1, BidGridMetrics.rowCount(6));
        assertEquals(2, BidGridMetrics.rowCount(7));
        assertEquals(2, BidGridMetrics.rowCount(12));
        assertEquals(3, BidGridMetrics.rowCount(18));
        assertEquals(0, BidGridMetrics.additionalTableHeightDp(12));
        assertEquals(96, BidGridMetrics.additionalTableHeightDp(18));
    }

    @Test public void fiveThroughEightCardsUseOneCompactRowAndLongerHandsKeepTheGrid() {
        for (int count = 5; count <= 8; count++) {
            assertTrue("hand of " + count, HandLayoutMetrics.usesSingleRow(count));
            assertEquals(count, HandLayoutMetrics.columnCount(count));
            assertEquals(58, HandLayoutMetrics.cardHeightDp(count));
            assertEquals(14, HandLayoutMetrics.cardLabelSizeSp(true));
            assertEquals(1, HandLayoutMetrics.horizontalMarginDp(count));
        }
        for (int count : new int[]{1, 4, 9, 17}) {
            assertFalse("hand of " + count, HandLayoutMetrics.usesSingleRow(count));
            assertEquals(4, HandLayoutMetrics.columnCount(count));
            assertEquals(72, HandLayoutMetrics.cardHeightDp(count));
            assertEquals(21, HandLayoutMetrics.cardLabelSizeSp(false));
            assertEquals(2, HandLayoutMetrics.horizontalMarginDp(count));
        }
    }

    @Test public void unsetOrInvalidPreferenceDefaultsToEnglish() {
        assertEquals(HowToPlay.Language.EN, WhistUiText.parseLanguage(null));
        assertEquals(HowToPlay.Language.EN, WhistUiText.parseLanguage("invalid"));
        assertEquals(HowToPlay.Language.EN, WhistUiText.parseLanguage("en"));
        assertEquals(HowToPlay.Language.RO, WhistUiText.parseLanguage("ro"));
    }

    @Test public void everyModeShowsTheExactScheduleLengthForThreeThroughSixPlayers() {
        for (GameMode mode : GameMode.values()) {
            for (int players = 3; players <= 6; players++) {
                int hands = GameRules.handSizes(mode, players).size();
                String english = new WhistUiText(HowToPlay.Language.EN)
                        .schedulePreview(mode, players);
                String romanian = new WhistUiText(HowToPlay.Language.RO)
                        .schedulePreview(mode, players);
                assertTrue(english, english.startsWith("Cards per hand (× means consecutive hands): "));
                assertTrue(english, english.endsWith(" · " + hands + (hands == 1 ? " hand" : " hands")));
                assertTrue(romanian, romanian.startsWith("Cărți pe mână (× indică mâini consecutive): "));
                assertTrue(romanian, romanian.endsWith(" · " + hands
                        + (hands == 1 ? " mână" : " de mâini")));
            }
        }
    }

    @Test public void setupLabelsAndRuleDescriptionsAreAvailableInBothLanguages() {
        for (GameMode mode : GameMode.values()) {
            String english = new WhistUiText(HowToPlay.Language.EN).modeDescription(mode);
            String romanian = new WhistUiText(HowToPlay.Language.RO).modeDescription(mode);
            assertFalse(mode.name(), english.isBlank());
            assertFalse(mode.name(), romanian.isBlank());
        }
        assertEquals("Whist romanesc (1-8-1)",
                new WhistUiText(HowToPlay.Language.EN).modeName(GameMode.ROMANIAN_WHIST));
        assertEquals("Whist românesc (1–8–1)",
                new WhistUiText(HowToPlay.Language.RO).modeName(GameMode.ROMANIAN_WHIST));
        assertEquals("Expert (simulation)",
                new WhistUiText(HowToPlay.Language.EN).difficultyName(AiDifficulty.EXPERT));
        assertEquals("Game menu", new WhistUiText(HowToPlay.Language.EN).gameMenu());
        assertEquals("Meniu joc", new WhistUiText(HowToPlay.Language.RO).gameMenu());
    }

    @Test public void biddingSummarySeparatesSubmittedCountFromBidTotal() {
        var game = GameSession.newGame(new GameConfig(4, GameMode.OH_HELL,
                AiDifficulty.NORMAL, 401));
        game.submitBid(game.snapshot().biddingSeat(), 2);
        GameSession.Snapshot state = game.snapshot();

        assertEquals("Bids submitted 1/4 · Bid total 2/10",
                new WhistUiText(HowToPlay.Language.EN).bidSummary(state));
        assertEquals("Licitații depuse 1/4 · Total licitat 2/10",
                new WhistUiText(HowToPlay.Language.RO).bidSummary(state));
    }

    @Test public void seatAndCardCopyNamesBidsTricksAndCardSuitExplicitly() {
        var game = GameSession.newGame(new GameConfig(3, GameMode.OH_HELL,
                AiDifficulty.NORMAL, 402));
        var player = game.snapshot().players().get(0);

        assertTrue(new WhistUiText(HowToPlay.Language.EN).seatFacts(player).contains("Bid"));
        assertTrue(new WhistUiText(HowToPlay.Language.EN).seatResult(player).contains("Tricks won"));
        assertTrue(new WhistUiText(HowToPlay.Language.RO).seatFacts(player).contains("Licitat"));
        assertTrue(new WhistUiText(HowToPlay.Language.RO).seatResult(player).contains("Levate câștigate"));
        assertEquals("Ace of spades", new WhistUiText(HowToPlay.Language.EN)
                .cardLabel(new Card(Suit.SPADES, 14)));
        assertEquals("As de pică", new WhistUiText(HowToPlay.Language.RO)
                .cardLabel(new Card(Suit.SPADES, 14)));
    }
}

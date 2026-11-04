package ro.whistlab.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
    @Test public void ohHellHelpUsesTheActualScheduleAtFourAndSixSeats() {
        for (int seats : new int[]{4, 6}) {
            List<Integer> schedule = GameRules.handSizes(GameMode.OH_HELL, seats);
            int midpoint = schedule.size() / 2;
            String example = schedule.get(0) + "–" + schedule.get(midpoint) + "–" + schedule.get(schedule.size() - 1);
            String en = new WhistUiText(HowToPlay.Language.EN).modeDescription(GameMode.OH_HELL);
            String ro = new WhistUiText(HowToPlay.Language.RO).modeDescription(GameMode.OH_HELL);
            assertTrue(en.contains(seats + " players: " + example));
            assertTrue(ro.contains(seats + " jucători: " + example));
        }
    }
    @Test public void centralBidGridAdaptsColumnsAndReservesSpaceForEveryRow() {
        assertEquals(0, BidGridMetrics.rowCount(0, 353));
        assertEquals(1, BidGridMetrics.rowCount(1, 353));
        assertEquals(1, BidGridMetrics.rowCount(3, 353));
        assertEquals(2, BidGridMetrics.rowCount(9, 353));
        assertEquals(3, BidGridMetrics.rowCount(9, 400));
        assertEquals(3, BidGridMetrics.rowCount(18, 353));
        assertEquals(5, BidGridMetrics.rowCount(18, 240));
    }

    @Test public void supportedHandsUseOneTallerFanRowUpToTheSeventeenCardRuleLimit() {
        for (int count = 1; count <= 17; count++) {
            assertTrue("hand of " + count, HandLayoutMetrics.usesSingleRow(count));
            assertEquals(98, HandLayoutMetrics.cardHeightDp());
            assertEquals(13, HandLayoutMetrics.cardLabelSizeSp());
        }
        assertFalse(HandLayoutMetrics.usesSingleRow(0));
        assertFalse(HandLayoutMetrics.usesSingleRow(18));
    }

    @Test public void cardFanStaysSingleRowAndScrollsOnlyWhenItsVisibleAdvanceCannotFit() {
        assertTrue(HandLayoutMetrics.usesSingleRow(8));
        assertFalse(HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1f));
        assertTrue(HandLayoutMetrics.requiresHorizontalScroll(17, 353, 1f));
        assertTrue(HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1.5f));
        assertEquals(126, HandLayoutMetrics.reservedHandHeightDp());
        assertEquals(760, HandLayoutMetrics.maxViewportWidthDp());
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
        assertEquals("Romanian (1–8–1)",
                new WhistUiText(HowToPlay.Language.EN).modeName(GameMode.ROMANIAN_WHIST));
        assertEquals("Românesc (1–8–1)",
                new WhistUiText(HowToPlay.Language.RO).modeName(GameMode.ROMANIAN_WHIST));
        String expectedExpertLabel = "HARD".equals(AiDifficulty.EXPERT.name())
                ? "Hard" : "Expert (simulation)";
        assertEquals(expectedExpertLabel,
                new WhistUiText(HowToPlay.Language.EN).difficultyName(AiDifficulty.EXPERT));
        assertEquals("Game menu", new WhistUiText(HowToPlay.Language.EN).gameMenu());
        assertEquals("Meniu joc", new WhistUiText(HowToPlay.Language.RO).gameMenu());
        assertEquals("Whist · Oh Hell", new WhistUiText(HowToPlay.Language.EN).gameTitle(GameMode.OH_HELL));
        assertEquals("Whist · Descrescător", new WhistUiText(HowToPlay.Language.RO).gameTitle(GameMode.DESCENDING));
    }

    @Test public void homeAndSetupHeadingsStayConciseInBothLanguages() {
        assertEquals("Whist", new WhistUiText(HowToPlay.Language.EN).homeTitle());
        assertEquals("Whist", new WhistUiText(HowToPlay.Language.RO).homeTitle());
        assertEquals("Game setup", new WhistUiText(HowToPlay.Language.EN).setupTitle());
        assertEquals("Setări joc", new WhistUiText(HowToPlay.Language.RO).setupTitle());
        assertEquals("Language", new WhistUiText(HowToPlay.Language.EN).languageLabel());
        assertEquals("Limba", new WhistUiText(HowToPlay.Language.RO).languageLabel());
        assertEquals("Language", new WhistUiText(HowToPlay.Language.EN).spinnerLabel("interface-language"));
        assertEquals("Limba", new WhistUiText(HowToPlay.Language.RO).spinnerLabel("interface-language"));
        assertTrue(new WhistUiText(HowToPlay.Language.EN).setupTitle().length() <= 12);
        assertTrue(new WhistUiText(HowToPlay.Language.RO).setupTitle().length() <= 12);
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

    @Test public void bidSubmissionButtonUsesTheShortActionLabelInEachLanguage() {
        assertEquals("Place bid", new WhistUiText(HowToPlay.Language.EN).placeBid());
        assertEquals("Licitează", new WhistUiText(HowToPlay.Language.RO).placeBid());
    }

    @Test public void trumpIndicatorShowsTheTurnedCardOrExplainsSuitOnlyRounds() {
        GameSession.Snapshot turnedCard = GameSession.newGame(new GameConfig(6, GameMode.OH_HELL,
                AiDifficulty.NORMAL, 401)).snapshot();
        GameSession.Snapshot suitOnly = GameSession.newGame(new GameConfig(4, GameMode.DESCENDING,
                AiDifficulty.NORMAL, 402)).snapshot();
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);

        assertNotNull(turnedCard.turnedCard());
        assertNull(suitOnly.turnedCard());
        assertEquals("TURNED CARD", english.trumpIndicatorStatus(turnedCard));
        assertTrue(english.trumpAccessibility(turnedCard).endsWith(english.cardLabel(turnedCard.turnedCard())));
        assertEquals("NO TURNED CARD", english.trumpIndicatorStatus(suitOnly));
        assertTrue(english.trumpAccessibility(suitOnly).contains("no turned card"));
        assertEquals("CARTE ÎNTOARSĂ", romanian.trumpIndicatorStatus(turnedCard));
        assertEquals("FĂRĂ CARTE", romanian.trumpIndicatorStatus(suitOnly));
    }

    @Test public void scoreTableUsesStableLocalizedColumnsAndAuditableRowValues() {
        var player = new GameSession.PlayerSnapshot(0, "Player", true, List.of(), 1, 1, 11);
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);

        assertEquals("Player", english.scoreColumnPlayer());
        assertEquals("Bid", english.scoreColumnBid());
        assertEquals("Won", english.scoreColumnTricks());
        assertEquals("Hand", english.scoreColumnHand());
        assertEquals("Total", english.scoreColumnTotal());
        assertEquals("You | Bid: 1 | Tricks won: 1 | Hand: +11 | Total: 11",
                english.scoreLine(player, 11));
        assertEquals("Jucător", romanian.scoreColumnPlayer());
        assertEquals("Lic.", romanian.scoreColumnBid());
        assertEquals("Pct.", romanian.scoreColumnHand());
        assertEquals("Lev.", romanian.scoreColumnTricks());
        assertEquals("Licitația nu a fost depusă", romanian.bidNotPlaced());
        assertTrue(romanian.scoreLine(player, "—").contains("Levate câștigate: 1"));
        assertTrue(romanian.scoreLine(player, -3).contains("Puncte în această mână: -3"));
    }

    @Test public void seatShowsNameSeparateFromBidAndTricksAndTurnBadgeIsLocalized() {
        var player = new GameSession.PlayerSnapshot(0, "Player", true, List.of(), 2, 1, 11);
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);

        assertEquals("Bid: 2\nWon: 1", english.seatFacts(player));
        assertEquals("Licitat: 2\nLevate: 1", romanian.seatFacts(player));
        assertEquals("BID NOW", english.seatTurnAction(true));
        assertEquals("PLAY NOW", english.seatTurnAction(false));
        assertEquals("LICITEAZĂ ACUM", romanian.seatTurnAction(true));
        assertEquals("JOACĂ ACUM", romanian.seatTurnAction(false));
        assertEquals("LICITEAZĂ", romanian.seatTurnBadge(true));
        assertEquals("JOACĂ ACUM", romanian.seatTurnBadge(false));
        assertTrue(english.seatAccessibility(player, false, true, false).contains("PLAY NOW"));
        assertTrue(romanian.seatAccessibility(player, false, true, true).contains("LICITEAZĂ ACUM"));
        assertEquals("Ace of spades", new WhistUiText(HowToPlay.Language.EN)
                .cardLabel(new Card(Suit.SPADES, 14)));
        assertEquals("As de pică", new WhistUiText(HowToPlay.Language.RO)
                .cardLabel(new Card(Suit.SPADES, 14)));
    }
}

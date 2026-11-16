package ro.whistlab.app.localization;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertNotNull;
import static atf.gradle.audit.assertions.GradleAssertions.assertNull;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.model.Suit;

public final class WhistUiTextTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void historyExplainsBidAndCumulativePointsAndBothSeriesMarkers() {
        var en = new WhistUiText(HowToPlay.Language.EN);
        var ro = new WhistUiText(HowToPlay.Language.RO);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "en", en, "ro", ro);
        assertTrue("en.historyLegend().startsWith(\"B Bid \\u00b7 P Total score\\n\") must hold", en.historyLegend().startsWith("B Bid · P Total score\n"));
        assertTrue("ro.historyLegend().startsWith(\"L Licitat \\u00b7 P Punctaj total\\n\") must hold", ro.historyLegend().startsWith("L Licitat · P Punctaj total\n"));
        for (var copy : List.of(en, ro)) {
            assertFalse("copy.historyLegend().contains(copy == en ? \"Hand points\" : \"Punctele m\\u00e2inii\") must remain false", copy.historyLegend().contains(copy == en ? "Hand points" : "Punctele mâinii"));
            assertTrue("copy.historyLegend().contains(copy == en ? \"Rectangle\" : \"Dreptunghi\") must hold", copy.historyLegend().contains(copy == en ? "Rectangle" : "Dreptunghi"));
            for (int points : List.of(5, 10)) {
                assertTrue("copy.historySeriesAdjustment(points).endsWith(\"+\" + points) must hold", copy.historySeriesAdjustment(points).endsWith("+" + points));
                assertTrue("copy.historySeriesAdjustment(-points).endsWith(\"-\" + points) must hold", copy.historySeriesAdjustment(-points).endsWith("-" + points));
                assertFalse("copy.historySeriesAdjustment(points).equals(copy.historySeriesAdjustment(-points)) must remain false", copy.historySeriesAdjustment(points).equals(copy.historySeriesAdjustment(-points)));
            }
        }
    }

    @Test public void ohHellHelpUsesTheActualScheduleAtFourAndSixSeats() {
        for (int seats : new int[]{4, 6}) {
            List<Integer> schedule = GameRules.handSizes(GameMode.OH_HELL, seats);
            int midpoint = schedule.size() / 2;
            String example = schedule.get(0) + "–" + schedule.get(midpoint) + "–" + schedule.get(schedule.size() - 1);
            String en = new WhistUiText(HowToPlay.Language.EN).modeDescription(GameMode.OH_HELL);
            String ro = new WhistUiText(HowToPlay.Language.RO).modeDescription(GameMode.OH_HELL);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "en", en, "example", example, "midpoint", midpoint, "ro", ro, "schedule", schedule, "seats", seats);
            assertTrue("en.contains(seats + \" players: \" + example) must hold", en.contains(seats + " players: " + example));
            assertTrue("ro.contains(seats + \" juc\\u0103tori: \" + example) must hold", ro.contains(seats + " jucători: " + example));
        }
    }
    @Test public void centralBidGridAdaptsColumnsAndReservesSpaceForEveryRow() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 353, "declared constant 3", 1, "declared constant 4", 3, "declared constant 5", 2, "declared constant 6", 9, "declared constant 7", 400, "declared constant 8", 18, "declared constant 9", 5, "declared constant 10", 240);
        assertEquals("BidGridMetrics.rowCount(0, 353) must equal 0", 0, BidGridMetrics.rowCount(0, 353));
        assertEquals("BidGridMetrics.rowCount(1, 353) must equal 1", 1, BidGridMetrics.rowCount(1, 353));
        assertEquals("BidGridMetrics.rowCount(3, 353) must equal 1", 1, BidGridMetrics.rowCount(3, 353));
        assertEquals("BidGridMetrics.rowCount(9, 353) must equal 2", 2, BidGridMetrics.rowCount(9, 353));
        assertEquals("BidGridMetrics.rowCount(9, 400) must equal 3", 3, BidGridMetrics.rowCount(9, 400));
        assertEquals("BidGridMetrics.rowCount(18, 353) must equal 3", 3, BidGridMetrics.rowCount(18, 353));
        assertEquals("BidGridMetrics.rowCount(18, 240) must equal 5", 5, BidGridMetrics.rowCount(18, 240));
    }

    @Test public void supportedHandsUseOneTallerFanRowUpToTheSeventeenCardRuleLimit() {
        for (int count = 1; count <= 17; count++) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count);
            assertTrue("hand of " + count, HandLayoutMetrics.usesSingleRow(count));
            assertEquals("HandLayoutMetrics.cardHeightDp() must equal 88", 88, HandLayoutMetrics.cardHeightDp());
            assertEquals("HandLayoutMetrics.cardLabelSizeSp() must equal 13", 13, HandLayoutMetrics.cardLabelSizeSp());
        }
        assertFalse("HandLayoutMetrics.usesSingleRow(0) must remain false", HandLayoutMetrics.usesSingleRow(0));
        assertFalse("HandLayoutMetrics.usesSingleRow(18) must remain false", HandLayoutMetrics.usesSingleRow(18));
    }

    @Test public void cardFanStaysSingleRowAndScrollsOnlyWhenItsVisibleAdvanceCannotFit() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 8, "declared constant 2", 13, "declared constant 3", 353, "declared constant 4", 1f, "declared constant 5", 17, "declared constant 6", 1.5f, "declared constant 7", 116, "declared constant 8", 760);
        assertTrue("HandLayoutMetrics.usesSingleRow(8) must hold", HandLayoutMetrics.usesSingleRow(8));
        assertFalse("HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1.0F) must remain false", HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1f));
        assertTrue("HandLayoutMetrics.requiresHorizontalScroll(17, 353, 1.0F) must hold", HandLayoutMetrics.requiresHorizontalScroll(17, 353, 1f));
        assertTrue("HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1.5F) must hold", HandLayoutMetrics.requiresHorizontalScroll(13, 353, 1.5f));
        assertEquals("HandLayoutMetrics.reservedHandHeightDp() must equal 116", 116, HandLayoutMetrics.reservedHandHeightDp());
        assertEquals("HandLayoutMetrics.maxViewportWidthDp() must equal 760", 760, HandLayoutMetrics.maxViewportWidthDp());
    }

    @Test public void unsetOrInvalidPreferenceDefaultsToEnglish() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "invalid", "declared constant 2", "en", "declared constant 3", "ro");
        assertEquals("WhistUiText.parseLanguage(null) must equal HowToPlay.Language.EN", HowToPlay.Language.EN, WhistUiText.parseLanguage(null));
        assertEquals("WhistUiText.parseLanguage(\"invalid\") must equal HowToPlay.Language.EN", HowToPlay.Language.EN, WhistUiText.parseLanguage("invalid"));
        assertEquals("WhistUiText.parseLanguage(\"en\") must equal HowToPlay.Language.EN", HowToPlay.Language.EN, WhistUiText.parseLanguage("en"));
        assertEquals("WhistUiText.parseLanguage(\"ro\") must equal HowToPlay.Language.RO", HowToPlay.Language.RO, WhistUiText.parseLanguage("ro"));
    }

    @Test public void everyModeShowsTheExactScheduleLengthForThreeThroughSixPlayers() {
        for (GameMode mode : GameMode.values()) {
            for (int players = 3; players <= 6; players++) {
                int hands = GameRules.handSizes(mode, players).size();
                String english = new WhistUiText(HowToPlay.Language.EN)
                        .schedulePreview(mode, players);
                String romanian = new WhistUiText(HowToPlay.Language.RO)
                        .schedulePreview(mode, players);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "hands", hands, "mode", mode, "players", players, "romanian", romanian);
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "mode", mode, "romanian", romanian);
            assertFalse(mode.name(), english.isBlank());
            assertFalse(mode.name(), romanian.isBlank());
        }
        assertEquals("new WhistUiText(HowToPlay.Language.EN).modeName(GameMode.ROMANIAN_WHIST) must equal \"Romanian (1\\u20138\\u20131)\"", "Romanian (1–8–1)",
                new WhistUiText(HowToPlay.Language.EN).modeName(GameMode.ROMANIAN_WHIST));
        assertEquals("new WhistUiText(HowToPlay.Language.RO).modeName(GameMode.ROMANIAN_WHIST) must equal \"Rom\\u00e2nesc (1\\u20138\\u20131)\"", "Românesc (1–8–1)",
                new WhistUiText(HowToPlay.Language.RO).modeName(GameMode.ROMANIAN_WHIST));
        String expectedExpertLabel = "HARD".equals(AiDifficulty.EXPERT.name())
                ? "Hard" : "Expert (simulation)";
        assertEquals("new WhistUiText(HowToPlay.Language.EN).difficultyName(AiDifficulty.EXPERT) must equal expectedExpertLabel", expectedExpertLabel,
                new WhistUiText(HowToPlay.Language.EN).difficultyName(AiDifficulty.EXPERT));
        assertEquals("new WhistUiText(HowToPlay.Language.EN).gameMenu() must equal \"Game menu\"", "Game menu", new WhistUiText(HowToPlay.Language.EN).gameMenu());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).gameMenu() must equal \"Meniu joc\"", "Meniu joc", new WhistUiText(HowToPlay.Language.RO).gameMenu());
        assertEquals("new WhistUiText(HowToPlay.Language.EN).gameTitle(GameMode.OH_HELL) must equal \"Whist \\u00b7 Oh Hell\"", "Whist · Oh Hell", new WhistUiText(HowToPlay.Language.EN).gameTitle(GameMode.OH_HELL));
        assertEquals("new WhistUiText(HowToPlay.Language.RO).gameTitle(GameMode.DESCENDING) must equal \"Whist \\u00b7 Descresc\\u0103tor\"", "Whist · Descrescător", new WhistUiText(HowToPlay.Language.RO).gameTitle(GameMode.DESCENDING));
    }

    @Test public void homeAndSetupHeadingsStayConciseInBothLanguages() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "Whist", "declared constant 2", "Game setup", "declared constant 3", "Setări joc", "declared constant 4", "Language", "declared constant 5", "Limba", "declared constant 6", "interface-language", "declared constant 7", 12);
        assertEquals("new WhistUiText(HowToPlay.Language.EN).homeTitle() must equal \"Whist\"", "Whist", new WhistUiText(HowToPlay.Language.EN).homeTitle());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).homeTitle() must equal \"Whist\"", "Whist", new WhistUiText(HowToPlay.Language.RO).homeTitle());
        assertEquals("new WhistUiText(HowToPlay.Language.EN).setupTitle() must equal \"Game setup\"", "Game setup", new WhistUiText(HowToPlay.Language.EN).setupTitle());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).setupTitle() must equal \"Set\\u0103ri joc\"", "Setări joc", new WhistUiText(HowToPlay.Language.RO).setupTitle());
        assertEquals("new WhistUiText(HowToPlay.Language.EN).languageLabel() must equal \"Language\"", "Language", new WhistUiText(HowToPlay.Language.EN).languageLabel());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).languageLabel() must equal \"Limba\"", "Limba", new WhistUiText(HowToPlay.Language.RO).languageLabel());
        assertEquals("new WhistUiText(HowToPlay.Language.EN).spinnerLabel(\"interface-language\") must equal \"Language\"", "Language", new WhistUiText(HowToPlay.Language.EN).spinnerLabel("interface-language"));
        assertEquals("new WhistUiText(HowToPlay.Language.RO).spinnerLabel(\"interface-language\") must equal \"Limba\"", "Limba", new WhistUiText(HowToPlay.Language.RO).spinnerLabel("interface-language"));
        assertTrue("new WhistUiText(HowToPlay.Language.EN).setupTitle().length() <= 12 must hold", new WhistUiText(HowToPlay.Language.EN).setupTitle().length() <= 12);
        assertTrue("new WhistUiText(HowToPlay.Language.RO).setupTitle().length() <= 12 must hold", new WhistUiText(HowToPlay.Language.RO).setupTitle().length() <= 12);
    }

    @Test public void biddingSummarySeparatesSubmittedCountFromBidTotal() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.OH_HELL,
                AiDifficulty.NORMAL, 401)));
        game.submitBid(game.snapshot().biddingSeat(), 2);
        GameSession.Snapshot state = game.snapshot();



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "state", state);
        assertEquals("new WhistUiText(HowToPlay.Language.EN).bidSummary(state) must equal \"Bids submitted 1/4\\nBid total 2/10\"", "Bids submitted 1/4\nBid total 2/10",
                new WhistUiText(HowToPlay.Language.EN).bidSummary(state));
        assertEquals("new WhistUiText(HowToPlay.Language.RO).bidSummary(state) must equal \"Licita\\u021bii depuse 1/4\\nTotal licitat 2/10\"", "Licitații depuse 1/4\nTotal licitat 2/10",
                new WhistUiText(HowToPlay.Language.RO).bidSummary(state));
    }

    @Test public void bidSubmissionButtonUsesTheShortActionLabelInEachLanguage() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "Place bid", "declared constant 2", "Licitează");
        assertEquals("new WhistUiText(HowToPlay.Language.EN).placeBid() must equal \"Place bid\"", "Place bid", new WhistUiText(HowToPlay.Language.EN).placeBid());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).placeBid() must equal \"Liciteaz\\u0103\"", "Licitează", new WhistUiText(HowToPlay.Language.RO).placeBid());
    }

    @Test public void unavailableAdviceKeepsTheManualChoiceExplicitInBothLanguages() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "Suggestion unavailable. Choose your bid.", "declared constant 2", "Estimarea nu este disponibilă. Alege licitația.");
        assertEquals("new WhistUiText(HowToPlay.Language.EN).bidAdviceUnavailable() must equal \"Suggestion unavailable. Choose your bid.\"", "Suggestion unavailable. Choose your bid.", new WhistUiText(HowToPlay.Language.EN).bidAdviceUnavailable());
        assertEquals("new WhistUiText(HowToPlay.Language.RO).bidAdviceUnavailable() must equal \"Estimarea nu este disponibil\\u0103. Alege licita\\u021bia.\"", "Estimarea nu este disponibilă. Alege licitația.", new WhistUiText(HowToPlay.Language.RO).bidAdviceUnavailable());
    }

    @Test public void trumpIndicatorShowsOneValueAndKeepsTheFullAccessibleIdentity() {
        GameSession.Snapshot turnedCard = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(6, GameMode.OH_HELL,
                AiDifficulty.NORMAL, 401))).snapshot();
        GameSession.Snapshot suitOnly = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.DESCENDING,
                AiDifficulty.NORMAL, 402))).snapshot();
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "romanian", romanian, "suitOnly", suitOnly, "turnedCard", turnedCard);
        assertNotNull("turnedCard.turnedCard() is available", turnedCard.turnedCard());
        assertNull("suitOnly.turnedCard() remains absent", suitOnly.turnedCard());
        assertEquals("english.trumpIndicatorValue(turnedCard) must equal turnedCard.turnedCard().shortLabel()", turnedCard.turnedCard().shortLabel(), english.trumpIndicatorValue(turnedCard));
        assertTrue("english.trumpAccessibility(turnedCard).endsWith(english.cardLabel(turnedCard.turnedCard())) must hold", english.trumpAccessibility(turnedCard).endsWith(english.cardLabel(turnedCard.turnedCard())));
        assertEquals("english.trumpIndicatorValue(suitOnly) must equal \"\\u2660\"", "♠", english.trumpIndicatorValue(suitOnly));
        assertTrue("english.trumpAccessibility(suitOnly).contains(\"no turned card\") must hold", english.trumpAccessibility(suitOnly).contains("no turned card"));
        assertEquals("romanian.trumpIndicatorValue(turnedCard) must equal turnedCard.turnedCard().shortLabel()", turnedCard.turnedCard().shortLabel(), romanian.trumpIndicatorValue(turnedCard));
        assertEquals("romanian.trumpIndicatorValue(suitOnly) must equal \"\\u2660\"", "♠", romanian.trumpIndicatorValue(suitOnly));
    }

    @Test public void scoreTableUsesStableLocalizedColumnsAndAuditableRowValues() {
        var player = new GameSession.PlayerSnapshot(0, "Player", true, List.of(), 1, 1, 11);
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "player", player, "romanian", romanian);
        assertEquals("english.scoreColumnPlayer() must equal \"Player\"", "Player", english.scoreColumnPlayer());
        assertEquals("english.scoreColumnBid() must equal \"Bid\"", "Bid", english.scoreColumnBid());
        assertEquals("english.scoreColumnTricks() must equal \"Won\"", "Won", english.scoreColumnTricks());
        assertEquals("english.scoreColumnHand() must equal \"Hand\"", "Hand", english.scoreColumnHand());
        assertEquals("english.scoreColumnTotal() must equal \"Total\"", "Total", english.scoreColumnTotal());
        assertEquals("english.scoreLine(player, 11) must equal \"Player | Bid: 1 | Tricks won: 1 | Hand: +11 | Total: 11\"", "Player | Bid: 1 | Tricks won: 1 | Hand: +11 | Total: 11",
                english.scoreLine(player, 11));
        assertEquals("romanian.scoreColumnPlayer() must equal \"Juc\\u0103tor\"", "Jucător", romanian.scoreColumnPlayer());
        assertEquals("romanian.scoreColumnBid() must equal \"Lic.\"", "Lic.", romanian.scoreColumnBid());
        assertEquals("romanian.scoreColumnHand() must equal \"Pct.\"", "Pct.", romanian.scoreColumnHand());
        assertEquals("romanian.scoreColumnTricks() must equal \"Lev.\"", "Lev.", romanian.scoreColumnTricks());
        assertEquals("romanian.bidNotPlaced() must equal \"Licita\\u021bia nu a fost depus\\u0103\"", "Licitația nu a fost depusă", romanian.bidNotPlaced());
        assertTrue("romanian.scoreLine(player, \"\\u2014\").contains(\"Levate c\\u00e2\\u0219tigate: 1\") must hold", romanian.scoreLine(player, "—").contains("Levate câștigate: 1"));
        assertTrue("romanian.scoreLine(player, -3).contains(\"Puncte \\u00een aceast\\u0103 m\\u00e2n\\u0103: -3\") must hold", romanian.scoreLine(player, -3).contains("Puncte în această mână: -3"));
    }

    @Test public void seatShowsNameSeparateFromBidAndTricksAndTurnBadgeIsLocalized() {
        var player = new GameSession.PlayerSnapshot(0, "Player", true, List.of(), 2, 1, 11);
        WhistUiText english = new WhistUiText(HowToPlay.Language.EN);
        WhistUiText romanian = new WhistUiText(HowToPlay.Language.RO);



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "player", player, "romanian", romanian);
        assertEquals("english.seatFacts(player) must equal \"Bid: 2\\nWon: 1\"", "Bid: 2\nWon: 1", english.seatFacts(player));
        assertEquals("romanian.seatFacts(player) must equal \"Licitat: 2\\nLevate: 1\"", "Licitat: 2\nLevate: 1", romanian.seatFacts(player));
        assertEquals("english.seatTurnAction(true) must equal \"BID NOW\"", "BID NOW", english.seatTurnAction(true));
        assertEquals("english.seatTurnAction(false) must equal \"PLAY NOW\"", "PLAY NOW", english.seatTurnAction(false));
        assertEquals("romanian.seatTurnAction(true) must equal \"LICITEAZ\\u0102 ACUM\"", "LICITEAZĂ ACUM", romanian.seatTurnAction(true));
        assertEquals("romanian.seatTurnAction(false) must equal \"JOAC\\u0102 ACUM\"", "JOACĂ ACUM", romanian.seatTurnAction(false));
        assertEquals("romanian.seatTurnBadge(true) must equal \"LICITEAZ\\u0102\"", "LICITEAZĂ", romanian.seatTurnBadge(true));
        assertEquals("romanian.seatTurnBadge(false) must equal \"JOAC\\u0102 ACUM\"", "JOACĂ ACUM", romanian.seatTurnBadge(false));
        assertTrue("english.seatAccessibility(player, false, true, false).contains(\"PLAY NOW\") must hold", english.seatAccessibility(player, false, true, false).contains("PLAY NOW"));
        assertTrue("romanian.seatAccessibility(player, false, true, true).contains(\"LICITEAZ\\u0102 ACUM\") must hold", romanian.seatAccessibility(player, false, true, true).contains("LICITEAZĂ ACUM"));
        assertEquals("new WhistUiText(HowToPlay.Language.EN).cardLabel(new Card(Suit.SPADES, 14)) must equal \"Ace of spades\"", "Ace of spades", new WhistUiText(HowToPlay.Language.EN)
                .cardLabel(new Card(Suit.SPADES, 14)));
        assertEquals("new WhistUiText(HowToPlay.Language.RO).cardLabel(new Card(Suit.SPADES, 14)) must equal \"As de pic\\u0103\"", "As de pică", new WhistUiText(HowToPlay.Language.RO)
                .cardLabel(new Card(Suit.SPADES, 14)));
    }

    @Test public void configuredHumanNamesSurviveLanguageChangesWhileResetNamesStayLocalized() {
        var named = new GameSession.PlayerSnapshot(0, "Cristi", true, List.of(), null, 0, 0);
        var reset = new GameSession.PlayerSnapshot(0, "You", true, List.of(), null, 0, 0);
        var english = new WhistUiText(HowToPlay.Language.EN);
        var romanian = new WhistUiText(HowToPlay.Language.RO);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "english", english, "named", named, "reset", reset, "romanian", romanian);
        assertEquals("english.playerName(named) must equal \"Cristi\"", "Cristi", english.playerName(named)); assertEquals("romanian.playerName(named) must equal \"Cristi\"", "Cristi", romanian.playerName(named));
        assertEquals("english.playerName(reset) must equal \"You\"", "You", english.playerName(reset)); assertEquals("romanian.playerName(reset) must equal \"Tu\"", "Tu", romanian.playerName(reset));
        assertTrue("english.tenPoints().contains(\"\\u00b110\") must hold", english.tenPoints().contains("±10")); assertTrue("romanian.fivePointsOriginal().contains(\"\\u00b15\") must hold", romanian.fivePointsOriginal().contains("±5"));
    }
}

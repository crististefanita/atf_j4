package ro.whistlab.core.rules;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class GameRulesTest extends atf.gradle.audit.GradleAuditedTest {
    @Test
    public void survivingProfilesKeepTheirHistoricalSamplingStreams() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 1, "declared constant 3", 3, "declared constant 4", 4, "declared constant 5", 6, "declared constant 6", 7);
        assertEquals("java.util.Arrays.stream(GameMode.values()).map(GameMode::samplingId).toList() must equal List.of(0, 1, 3, 4, 6, 7)", List.of(0, 1, 3, 4, 6, 7),
                java.util.Arrays.stream(GameMode.values()).map(GameMode::samplingId).toList());
    }

    @Test
    public void standardDeckContainsEveryUniqueCard() {
        List<Card> deck = GameRules.standardDeck();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "deck", deck);
        assertEquals("deck.size() must equal 52", 52, deck.size());
        assertEquals("Set.copyOf(deck).size() must equal 52", 52, Set.copyOf(deck).size());
    }

    @Test
    public void descendingModeFitsTheDeckForEachSupportedPlayerCount() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 13, "declared constant 2", 12, "declared constant 3", 11, "declared constant 4", 10, "declared constant 5", 9, "declared constant 6", 8, "declared constant 7", 7, "declared constant 8", 6, "declared constant 9", 5, "declared constant 10", 4, "declared constant 11", 3, "declared constant 12", 2, "declared constant 13", 1, "declared constant 14", 0);
        assertEquals("GameRules.handSizes(GameMode.DESCENDING, 4) must equal List.of(13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)", List.of(13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                GameRules.handSizes(GameMode.DESCENDING, 4));
        assertEquals("GameRules.handSizes(GameMode.DESCENDING, 5).get(0).intValue() must equal 10", 10, GameRules.handSizes(GameMode.DESCENDING, 5).get(0).intValue());
        assertEquals("GameRules.handSizes(GameMode.DESCENDING, 6).get(0).intValue() must equal 8", 8, GameRules.handSizes(GameMode.DESCENDING, 6).get(0).intValue());
    }

    @Test
    public void ohHellFourPlayerSequenceMatchesTheObservedOriginalScoreSheet() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 10, "declared constant 2", 9, "declared constant 3", 8, "declared constant 4", 7, "declared constant 5", 6, "declared constant 6", 5, "declared constant 7", 4, "declared constant 8", 3, "declared constant 9", 2, "declared constant 10", 1, "declared constant 11", 1L);
        assertEquals("GameRules.handSizes(GameMode.OH_HELL, 4) must equal List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)", List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                GameRules.handSizes(GameMode.OH_HELL, 4));
        assertEquals("GameConfig.defaults(1L).mode() must equal GameMode.OH_HELL", GameMode.OH_HELL, GameConfig.defaults(1L).mode());
    }

    @Test
    public void firstOhHellDealSizeMatchesOriginalScreenshotsAtEverySupportedTableSize() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 10, "declared constant 2", 8, "declared constant 3", 3, "declared constant 4", 6);
        assertEquals("java.util.stream.IntStream.rangeClosed(3, 6).mapToObj((players)->GameRules.handSizes(GameMode.OH_HELL, players).getFirst()).collect(Collectors.toList()) must equal List.of(10, 10, 10, 8)", List.of(10, 10, 10, 8), java.util.stream.IntStream.rangeClosed(3, 6)
                .mapToObj(players -> GameRules.handSizes(GameMode.OH_HELL, players).getFirst())
                .collect(Collectors.toList()));
    }

    @Test
    public void candidateOhHellDealsNeverExceedTheStandardDeckForSupportedTableSizes() {
        for (int players = 3; players <= 6; players++) {
            int firstHand = GameRules.handSizes(GameMode.OH_HELL, players).getFirst();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "firstHand", firstHand, "players", players);
            assertTrue("Oh Hell deal exceeds a standard deck for " + players + " players",
                    firstHand * players <= 52);
        }
    }

    @Test
    public void legalPlayMustFollowLedSuitWhenPossible() {
        Card hearts = new Card(Suit.HEARTS, 3);
        Card spades = new Card(Suit.SPADES, 14);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hearts", hearts, "spades", spades);
        assertEquals("GameRules.legalCards(List.of(hearts, spades), Suit.HEARTS) must equal List.of(hearts)", List.of(hearts), GameRules.legalCards(List.of(hearts, spades), Suit.HEARTS));
    }

    @Test
    public void playerMayDiscardOrTrumpWhenVoidInLedSuit() {
        List<Card> hand = List.of(new Card(Suit.SPADES, 2), new Card(Suit.CLUBS, 14));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
        assertEquals("GameRules.legalCards(hand, Suit.HEARTS) must equal hand", hand, GameRules.legalCards(hand, Suit.HEARTS));
    }

    @Test
    public void highestLedSuitWinsWhenNoTrumpIsShown() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 11),
                played(1, Suit.HEARTS, 13),
                played(2, Suit.CLUBS, 14),
                played(3, Suit.HEARTS, 12));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "trick", trick);
        assertEquals("GameRules.winningSeat(trick, Trump.NO_TRUMP) must equal 1", 1, GameRules.winningSeat(trick, Trump.NO_TRUMP));
    }

    @Test
    public void anyTrumpBeatsTheLedSuitAndHighestTrumpWins() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 14),
                played(1, Suit.SPADES, 3),
                played(2, Suit.SPADES, 12),
                played(3, Suit.CLUBS, 14));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "trick", trick);
        assertEquals("GameRules.winningSeat(trick, Trump.SPADES) must equal 2", 2, GameRules.winningSeat(trick, Trump.SPADES));
    }

    @Test
    public void offSuitNonTrumpCannotBeatTheLedSuit() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 9),
                played(1, Suit.CLUBS, 14),
                played(2, Suit.HEARTS, 10));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "trick", trick);
        assertEquals("GameRules.winningSeat(trick, Trump.SPADES) must equal 2", 2, GameRules.winningSeat(trick, Trump.SPADES));
    }

    @Test
    public void scoreAddsOnePerTrickAndTenForAnExactBid() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 11, "declared constant 2", 1, "declared constant 3", 3, "declared constant 4", 10, "declared constant 5", 0, "declared constant 6", -1);
        assertEquals("GameRules.scoreDelta(1, 1) must equal 11", 11, GameRules.scoreDelta(1, 1));
        assertEquals("GameRules.scoreDelta(3, 1) must equal 3", 3, GameRules.scoreDelta(3, 1));
        assertEquals("GameRules.scoreDelta(0, 0) must equal 10", 10, GameRules.scoreDelta(0, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.scoreDelta(-1, 0));
    }

    @Test
    public void finalBidCannotMakeTotalEqualTheHandSize() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 3, "declared constant 2", 2, "declared constant 3", 5, "declared constant 4", 1);
        assertTrue("GameRules.isForbiddenLastBid(3, 2, 5) must hold", GameRules.isForbiddenLastBid(3, 2, 5));
        assertFalse("GameRules.isForbiddenLastBid(3, 1, 5) must remain false", GameRules.isForbiddenLastBid(3, 1, 5));
    }

    @Test
    public void allowedLastBidListOmitsOnlyTheExactHandTotal() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 1, "declared constant 3", 3, "declared constant 4", 4, "declared constant 5", 5, "declared constant 6", true, "declared constant 7", 2, "declared constant 8", false);
        assertEquals("GameRules.allowedBids(5, 3, true) must equal List.of(0, 1, 3, 4, 5)", List.of(0, 1, 3, 4, 5), GameRules.allowedBids(5, 3, true));
        assertEquals("GameRules.allowedBids(5, 3, false) must equal List.of(0, 1, 2, 3, 4, 5)", List.of(0, 1, 2, 3, 4, 5), GameRules.allowedBids(5, 3, false));
    }

    @Test
    public void trumpOrderMatchesTheSequenceDeclaredInOriginalInstructionsAndRepeats() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 5);
        assertEquals("java.util.stream.IntStream.range(0, 5).mapToObj(Trump::forHandIndex).collect(Collectors.toList()) must equal List.of(Trump.SPADES, Trump.HEARTS, Trump.CLUBS, Trump.DIAMONDS, Trump.NO_TRUMP)", List.of(Trump.SPADES, Trump.HEARTS, Trump.CLUBS, Trump.DIAMONDS, Trump.NO_TRUMP),
                java.util.stream.IntStream.range(0, 5).mapToObj(Trump::forHandIndex).collect(Collectors.toList()));
        assertEquals("Trump.forHandIndex(5) must equal Trump.SPADES", Trump.SPADES, Trump.forHandIndex(5));
    }

    @Test
    public void lastBidRuleExcludesOnlyTheValueThatWouldEqualTheHandSize() {
        for (int handSize = 1; handSize <= 13; handSize++) {
            for (int previousTotal = 0; previousTotal <= handSize + 2; previousTotal++) {
                List<Integer> allowed = GameRules.allowedBids(handSize, previousTotal, true);
                for (int candidate = 0; candidate <= handSize; candidate++) {
                    boolean forbidden = previousTotal + candidate == handSize;

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "allowed", allowed, "candidate", candidate, "forbidden", forbidden, "handSize", handSize, "previousTotal", previousTotal);
                    assertEquals("hand=" + handSize + ", prior=" + previousTotal + ", bid=" + candidate,
                            !forbidden, allowed.contains(candidate));
                }
                int priorTotal = previousTotal;
                int currentHandSize = handSize;
                assertTrue("allowed.stream().allMatch((bid)->priorTotal + bid != currentHandSize) must hold", allowed.stream().allMatch(bid -> priorTotal + bid != currentHandSize));
            }
        }
    }

    @Test
    public void exactBidBonusIsAddedExactlyOnceAndNeverForAnInexactBid() {
        for (int tricks = 0; tricks <= 13; tricks++) {
            for (int bid = 0; bid <= 13; bid++) {

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid, "tricks", tricks);
                assertEquals("tricks=" + tricks + ", bid=" + bid,
                        tricks + (tricks == bid ? 10 : 0), GameRules.scoreDelta(tricks, bid));
            }
        }
    }

    @Test
    public void noTrumpNeverLetsAnOffSuitCardBeatTheLedSuit() {
        for (Suit led : Suit.values()) {
            Suit offSuit = java.util.Arrays.stream(Suit.values()).filter(suit -> suit != led).findFirst().orElseThrow();
            List<PlayedCard> trick = List.of(
                    played(0, led, 2),
                    played(1, offSuit, 14),
                    played(2, led, 14));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "led", led, "offSuit", offSuit, "trick", trick);
            assertEquals("led suit=" + led, 2, GameRules.winningSeat(trick, Trump.NO_TRUMP));
        }
    }

    private static PlayedCard played(int seat, Suit suit, int rank) {
        return new PlayedCard(seat, new Card(suit, rank));
    }
}

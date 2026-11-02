package ro.whistlab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class GameRulesTest {
    @Test
    public void standardDeckContainsEveryUniqueCard() {
        List<Card> deck = GameRules.standardDeck();
        assertEquals(52, deck.size());
        assertEquals(52, Set.copyOf(deck).size());
    }

    @Test
    public void descendingModeFitsTheDeckForEachSupportedPlayerCount() {
        assertEquals(List.of(13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                GameRules.handSizes(GameMode.DESCENDING, 4));
        assertEquals(10, GameRules.handSizes(GameMode.DESCENDING, 5).get(0).intValue());
        assertEquals(8, GameRules.handSizes(GameMode.DESCENDING, 6).get(0).intValue());
        assertEquals(List.of(1), GameRules.handSizes(GameMode.PRACTICE_HAND, 3));
    }

    @Test
    public void ohHellFourPlayerSequenceMatchesTheObservedOriginalScoreSheet() {
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                GameRules.handSizes(GameMode.OH_HELL, 4));
        assertEquals(GameMode.OH_HELL, GameConfig.defaults(1L).mode());
    }

    @Test
    public void firstOhHellDealSizeMatchesOriginalScreenshotsAtEverySupportedTableSize() {
        assertEquals(List.of(10, 10, 10, 8), java.util.stream.IntStream.rangeClosed(3, 6)
                .mapToObj(players -> GameRules.handSizes(GameMode.OH_HELL, players).getFirst())
                .collect(Collectors.toList()));
    }

    @Test
    public void candidateOhHellDealsNeverExceedTheStandardDeckForSupportedTableSizes() {
        for (int players = 3; players <= 6; players++) {
            int firstHand = GameRules.handSizes(GameMode.OH_HELL, players).getFirst();
            assertTrue("Oh Hell deal exceeds a standard deck for " + players + " players",
                    firstHand * players <= 52);
        }
    }

    @Test
    public void legalPlayMustFollowLedSuitWhenPossible() {
        Card hearts = new Card(Suit.HEARTS, 3);
        Card spades = new Card(Suit.SPADES, 14);
        assertEquals(List.of(hearts), GameRules.legalCards(List.of(hearts, spades), Suit.HEARTS));
    }

    @Test
    public void playerMayDiscardOrTrumpWhenVoidInLedSuit() {
        List<Card> hand = List.of(new Card(Suit.SPADES, 2), new Card(Suit.CLUBS, 14));
        assertEquals(hand, GameRules.legalCards(hand, Suit.HEARTS));
    }

    @Test
    public void highestLedSuitWinsWhenNoTrumpIsShown() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 11),
                played(1, Suit.HEARTS, 13),
                played(2, Suit.CLUBS, 14),
                played(3, Suit.HEARTS, 12));
        assertEquals(1, GameRules.winningSeat(trick, Trump.NO_TRUMP));
    }

    @Test
    public void anyTrumpBeatsTheLedSuitAndHighestTrumpWins() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 14),
                played(1, Suit.SPADES, 3),
                played(2, Suit.SPADES, 12),
                played(3, Suit.CLUBS, 14));
        assertEquals(2, GameRules.winningSeat(trick, Trump.SPADES));
    }

    @Test
    public void offSuitNonTrumpCannotBeatTheLedSuit() {
        List<PlayedCard> trick = List.of(
                played(0, Suit.HEARTS, 9),
                played(1, Suit.CLUBS, 14),
                played(2, Suit.HEARTS, 10));
        assertEquals(2, GameRules.winningSeat(trick, Trump.SPADES));
    }

    @Test
    public void scoreAddsOnePerTrickAndTenForAnExactBid() {
        assertEquals(11, GameRules.scoreDelta(1, 1));
        assertEquals(3, GameRules.scoreDelta(3, 1));
        assertEquals(10, GameRules.scoreDelta(0, 0));
        assertThrows(IllegalArgumentException.class, () -> GameRules.scoreDelta(-1, 0));
    }

    @Test
    public void finalBidCannotMakeTotalEqualTheHandSize() {
        assertTrue(GameRules.isForbiddenLastBid(3, 2, 5));
        assertFalse(GameRules.isForbiddenLastBid(3, 1, 5));
    }

    @Test
    public void allowedLastBidListOmitsOnlyTheExactHandTotal() {
        assertEquals(List.of(0, 1, 3, 4, 5), GameRules.allowedBids(5, 3, true));
        assertEquals(List.of(0, 1, 2, 3, 4, 5), GameRules.allowedBids(5, 3, false));
    }

    @Test
    public void trumpOrderMatchesTheSequenceDeclaredInOriginalInstructionsAndRepeats() {
        assertEquals(List.of(Trump.SPADES, Trump.HEARTS, Trump.CLUBS, Trump.DIAMONDS, Trump.NO_TRUMP),
                java.util.stream.IntStream.range(0, 5).mapToObj(Trump::forHandIndex).collect(Collectors.toList()));
        assertEquals(Trump.SPADES, Trump.forHandIndex(5));
    }

    @Test
    public void lastBidRuleExcludesOnlyTheValueThatWouldEqualTheHandSize() {
        for (int handSize = 1; handSize <= 13; handSize++) {
            for (int previousTotal = 0; previousTotal <= handSize + 2; previousTotal++) {
                List<Integer> allowed = GameRules.allowedBids(handSize, previousTotal, true);
                for (int candidate = 0; candidate <= handSize; candidate++) {
                    boolean forbidden = previousTotal + candidate == handSize;
                    assertEquals("hand=" + handSize + ", prior=" + previousTotal + ", bid=" + candidate,
                            !forbidden, allowed.contains(candidate));
                }
                int priorTotal = previousTotal;
                int currentHandSize = handSize;
                assertTrue(allowed.stream().allMatch(bid -> priorTotal + bid != currentHandSize));
            }
        }
    }

    @Test
    public void exactBidBonusIsAddedExactlyOnceAndNeverForAnInexactBid() {
        for (int tricks = 0; tricks <= 13; tricks++) {
            for (int bid = 0; bid <= 13; bid++) {
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
            assertEquals("led suit=" + led, 2, GameRules.winningSeat(trick, Trump.NO_TRUMP));
        }
    }

    private static PlayedCard played(int seat, Suit suit, int rank) {
        return new PlayedCard(seat, new Card(suit, rank));
    }
}

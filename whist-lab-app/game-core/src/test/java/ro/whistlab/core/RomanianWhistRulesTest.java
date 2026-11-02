package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** RW rule IDs map to game-core/README.md and its independently published sources. */
public class RomanianWhistRulesTest {
    private static final GameMode RO = GameMode.ROMANIAN_WHIST;

    @Test public void rw01ReducedDeckKeepsEightCardsPerSeatAndRemovesLowestRanks() {
        for (int players = 3; players <= 6; players++) {
            List<Card> deck = GameRules.deck(RO, players);
            assertEquals(8 * players, deck.size());
            assertEquals(deck.size(), new HashSet<>(deck).size());
            int expectedMinimum = new int[]{9, 7, 5, 3}[players - 3];
            for (Suit suit : Suit.values()) {
                List<Integer> ranks = deck.stream().filter(c -> c.suit() == suit).map(Card::rank).toList();
                assertEquals(2 * players, ranks.size());
                assertEquals(expectedMinimum, Collections.min(ranks).intValue());
                assertEquals(14, Collections.max(ranks).intValue());
            }
        }
        assertEquals(52, GameRules.deck(GameMode.OH_HELL, 4).size());
        assertThrows(IllegalArgumentException.class, () -> GameRules.deck(RO, 2));
        assertThrows(IllegalArgumentException.class, () -> GameRules.deck(RO, 7));
    }

    @Test public void rw02FourSeatScheduleHasTwentyFourHandsAndNinetyFourTricks() {
        assertEquals(List.of(1,1,1,1,2,3,4,5,6,7,8,8,8,8,7,6,5,4,3,2,1,1,1,1),
                GameRules.handSizes(RO, 4));
        assertEquals(94, GameRules.handSizes(RO, 4).stream().mapToInt(Integer::intValue).sum());
    }

    @Test public void rw02EverySeatDealsOnceAtEachExtremeBlock() {
        for (int n = 3; n <= 6; n++) {
            List<Integer> sizes = GameRules.handSizes(RO, n);
            assertEquals(3 * n + 12, sizes.size());
            assertEquals(10 * n + 54, sizes.stream().mapToInt(Integer::intValue).sum());
            for (int initialDealer = 0; initialDealer < n; initialDealer++) {
                for (int start : new int[]{0, n + 6, 2 * n + 12}) {
                    Set<Integer> dealers = new HashSet<>();
                    for (int h = start; h < start + n; h++) dealers.add((initialDealer + h) % n);
                    assertEquals(n, dealers.size());
                    int expectedSize = start == n + 6 ? 8 : 1;
                    assertTrue(sizes.subList(start, start + n).stream().allMatch(s -> s == expectedSize));
                }
            }
        }
    }

    @Test public void rw04FollowSuitTakesPriorityOverPlayingTrump() {
        Card heart = card(Suit.HEARTS, 7), trump = card(Suit.SPADES, 14);
        assertEquals(List.of(heart), legal(List.of(heart, trump), Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw04VoidWithTrumpMustCutAndCannotDiscardEvenAnAce() {
        Card lowTrump = card(Suit.SPADES, 7), highTrump = card(Suit.SPADES, 14);
        assertEquals(List.of(lowTrump, highTrump), legal(
                List.of(lowTrump, card(Suit.CLUBS, 14), highTrump), Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw04NoTrumpOrNoHeldTrumpAllowsDiscardWhenVoid() {
        List<Card> hand = List.of(card(Suit.SPADES, 7), card(Suit.CLUBS, 14));
        assertEquals(hand, legal(hand, Suit.HEARTS, Trump.NO_TRUMP));
        assertEquals(hand, legal(hand, Suit.HEARTS, Trump.DIAMONDS));
    }

    @Test public void rw04LeaderMayLeadAnyCardIncludingTrump() {
        List<Card> hand = List.of(card(Suit.SPADES, 7), card(Suit.HEARTS, 14));
        assertEquals(hand, legal(hand, null, Trump.SPADES));
    }

    @Test public void rw04OhHellRemainsOptionalTrumpAndRomanianWhistIsMandatory() {
        List<Card> hand = List.of(card(Suit.CLUBS, 14), card(Suit.SPADES, 7));
        assertEquals(hand, GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL));
        assertEquals(List.of(card(Suit.SPADES, 7)), legal(hand, Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw05LowestTrumpBeatsEveryOtherSuitAceForEachTrumpSuit() {
        for (Suit trump : Suit.values()) {
            List<Suit> others = Arrays.stream(Suit.values()).filter(s -> s != trump).toList();
            List<PlayedCard> trick = List.of(new PlayedCard(3, card(others.get(0), 14)),
                    new PlayedCard(0, card(trump, 7)), new PlayedCard(1, card(others.get(1), 14)),
                    new PlayedCard(2, card(others.get(2), 14)));
            assertEquals(0, GameRules.winningSeat(trick, Trump.values()[trump.ordinal()]));
        }
    }

    @Test public void rw05HighestTrumpWinsRegardlessOfSeatOrPlayPosition() {
        for (int leader = 0; leader < 4; leader++) {
            List<PlayedCard> trick = List.of(new PlayedCard(leader, card(Suit.SPADES, 9)),
                    new PlayedCard((leader+1)%4, card(Suit.SPADES, 14)),
                    new PlayedCard((leader+2)%4, card(Suit.SPADES, 13)),
                    new PlayedCard((leader+3)%4, card(Suit.SPADES, 7)));
            assertEquals((leader+1)%4, GameRules.winningSeat(trick, Trump.SPADES));
        }
    }

    @Test public void rw06AllEightyOneBidAndWonPairsHaveExactBonusOrSymmetricPenalty() {
        for (int won = 0; won <= 8; won++) for (int bid = 0; bid <= 8; bid++) {
            int expected = won == bid ? 5 + won : -Math.abs(won-bid);
            assertEquals("won=" + won + ", bid=" + bid, expected, GameRules.scoreDelta(won, bid, RO));
        }
        assertEquals(5, GameRules.scoreDelta(0, 0, RO));
        assertEquals(-8, GameRules.scoreDelta(0, 8, RO));
        assertEquals(-8, GameRules.scoreDelta(8, 0, RO));
        assertEquals(13, GameRules.scoreDelta(8, 8, RO));
        assertThrows(IllegalArgumentException.class, () -> GameRules.scoreDelta(-1, 0, RO));
        assertThrows(IllegalArgumentException.class, () -> GameRules.scoreDelta(9, 0, RO));
        assertThrows(IllegalArgumentException.class, () -> GameRules.scoreDelta(0, 9, RO));
        assertEquals(10, GameRules.scoreDelta(0, 0, GameMode.OH_HELL));
    }

    @Test public void rw03LastBidExhaustiveForEveryTableSizeAndPriorTotal() {
        for (int n = 3; n <= 6; n++) for (int size = 1; size <= 8; size++) {
            for (int prior = 0; prior <= (n-1)*size; prior++) {
                List<Integer> last = GameRules.allowedBids(size, prior, true);
                List<Integer> earlier = GameRules.allowedBids(size, prior, false);
                assertEquals(size+1, earlier.size());
                for (int bid = 0; bid <= size; bid++) assertEquals(
                        "players="+n+", size="+size+", prior="+prior+", bid="+bid,
                        prior+bid != size, last.contains(bid));
            }
        }
        assertEquals(List.of(1), GameRules.allowedBids(1, 1, true));
        assertEquals(List.of(0), GameRules.allowedBids(1, 0, true));
        assertEquals(List.of(0,1), GameRules.allowedBids(1, 2, true));
    }

    private static Card card(Suit suit, int rank) { return new Card(suit, rank); }
    private static List<Card> legal(List<Card> hand, Suit led, Trump trump) {
        return GameRules.legalCards(hand, led, trump, RO);
    }
}

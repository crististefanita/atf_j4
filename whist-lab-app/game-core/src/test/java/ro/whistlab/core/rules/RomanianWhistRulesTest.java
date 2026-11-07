package ro.whistlab.core.rules;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

/** RW rule IDs map to game-core/README.md and its independently published sources. */
public class RomanianWhistRulesTest extends atf.gradle.audit.GradleAuditedTest {
    private static final GameMode RO = GameMode.ROMANIAN_WHIST;

    @Test public void rw01ReducedDeckKeepsEightCardsPerSeatAndRemovesLowestRanks() {
        for (int players = 3; players <= 6; players++) {
            List<Card> deck = GameRules.deck(RO, players);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "deck", deck, "players", players);
            assertEquals("deck.size() must equal 8 * players", 8 * players, deck.size());
            assertEquals("new HashSet<>(deck).size() must equal deck.size()", deck.size(), new HashSet<>(deck).size());
            int expectedMinimum = new int[]{9, 7, 5, 3}[players - 3];
            for (Suit suit : Suit.values()) {
                List<Integer> ranks = deck.stream().filter(c -> c.suit() == suit).map(Card::rank).toList();
                assertEquals("ranks.size() must equal 2 * players", 2 * players, ranks.size());
                assertEquals("Collections.min(ranks).intValue() must equal expectedMinimum", expectedMinimum, Collections.min(ranks).intValue());
                assertEquals("Collections.max(ranks).intValue() must equal 14", 14, Collections.max(ranks).intValue());
            }
        }
        assertEquals("GameRules.deck(GameMode.OH_HELL, 4).size() must equal 52", 52, GameRules.deck(GameMode.OH_HELL, 4).size());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.deck(RO, 2));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.deck(RO, 7));
    }

    @Test public void rw02FourSeatScheduleHasTwentyFourHandsAndNinetyFourTricks() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 1, "declared constant 2", 2, "declared constant 3", 3, "declared constant 4", 4, "declared constant 5", 5, "declared constant 6", 6, "declared constant 7", 7, "declared constant 8", 8, "declared constant 9", 94);
        assertEquals("GameRules.handSizes(RO, 4) must equal List.of(1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1)", List.of(1,1,1,1,2,3,4,5,6,7,8,8,8,8,7,6,5,4,3,2,1,1,1,1),
                GameRules.handSizes(RO, 4));
        assertEquals("GameRules.handSizes(RO, 4).stream().mapToInt(Integer::intValue).sum() must equal 94", 94, GameRules.handSizes(RO, 4).stream().mapToInt(Integer::intValue).sum());
    }

    @Test public void rw02EverySeatDealsOnceAtEachExtremeBlock() {
        for (int n = 3; n <= 6; n++) {
            List<Integer> sizes = GameRules.handSizes(RO, n);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "n", n, "sizes", sizes);
            assertEquals("sizes.size() must equal 3 * n + 12", 3 * n + 12, sizes.size());
            assertEquals("sizes.stream().mapToInt(Integer::intValue).sum() must equal 10 * n + 54", 10 * n + 54, sizes.stream().mapToInt(Integer::intValue).sum());
            for (int initialDealer = 0; initialDealer < n; initialDealer++) {
                for (int start : new int[]{0, n + 6, 2 * n + 12}) {
                    Set<Integer> dealers = new HashSet<>();
                    for (int h = start; h < start + n; h++) dealers.add((initialDealer + h) % n);
                    assertEquals("dealers.size() must equal n", n, dealers.size());
                    int expectedSize = start == n + 6 ? 8 : 1;
                    assertTrue("sizes.subList(start, start + n).stream().allMatch((s)->s == expectedSize) must hold", sizes.subList(start, start + n).stream().allMatch(s -> s == expectedSize));
                }
            }
        }
    }

    @Test public void rw04FollowSuitTakesPriorityOverPlayingTrump() {
        Card heart = card(Suit.HEARTS, 7), trump = card(Suit.SPADES, 14);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "heart", heart, "trump", trump);
        assertEquals("legal(List.of(heart, trump), Suit.HEARTS, Trump.SPADES) must equal List.of(heart)", List.of(heart), legal(List.of(heart, trump), Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw04VoidWithTrumpMustCutAndCannotDiscardEvenAnAce() {
        Card lowTrump = card(Suit.SPADES, 7), highTrump = card(Suit.SPADES, 14);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "highTrump", highTrump, "lowTrump", lowTrump);
        assertEquals("legal(List.of(lowTrump, card(Suit.CLUBS, 14), highTrump), Suit.HEARTS, Trump.SPADES) must equal List.of(lowTrump, highTrump)", List.of(lowTrump, highTrump), legal(
                List.of(lowTrump, card(Suit.CLUBS, 14), highTrump), Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw04NoTrumpOrNoHeldTrumpAllowsDiscardWhenVoid() {
        List<Card> hand = List.of(card(Suit.SPADES, 7), card(Suit.CLUBS, 14));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
        assertEquals("legal(hand, Suit.HEARTS, Trump.NO_TRUMP) must equal hand", hand, legal(hand, Suit.HEARTS, Trump.NO_TRUMP));
        assertEquals("legal(hand, Suit.HEARTS, Trump.DIAMONDS) must equal hand", hand, legal(hand, Suit.HEARTS, Trump.DIAMONDS));
    }

    @Test public void rw04LeaderMayLeadAnyCardIncludingTrump() {
        List<Card> hand = List.of(card(Suit.SPADES, 7), card(Suit.HEARTS, 14));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
        assertEquals("legal(hand, null, Trump.SPADES) must equal hand", hand, legal(hand, null, Trump.SPADES));
    }

    @Test public void rw04OhHellRemainsOptionalTrumpAndRomanianWhistIsMandatory() {
        List<Card> hand = List.of(card(Suit.CLUBS, 14), card(Suit.SPADES, 7));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hand", hand);
        assertEquals("GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL) must equal hand", hand, GameRules.legalCards(hand, Suit.HEARTS, Trump.SPADES, GameMode.OH_HELL));
        assertEquals("legal(hand, Suit.HEARTS, Trump.SPADES) must equal List.of(card(Suit.SPADES, 7))", List.of(card(Suit.SPADES, 7)), legal(hand, Suit.HEARTS, Trump.SPADES));
    }

    @Test public void rw05LowestTrumpBeatsEveryOtherSuitAceForEachTrumpSuit() {
        for (Suit trump : Suit.values()) {
            List<Suit> others = Arrays.stream(Suit.values()).filter(s -> s != trump).toList();
            List<PlayedCard> trick = List.of(new PlayedCard(3, card(others.get(0), 14)),
                    new PlayedCard(0, card(trump, 7)), new PlayedCard(1, card(others.get(1), 14)),
                    new PlayedCard(2, card(others.get(2), 14)));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "others", others, "trick", trick, "trump", trump);
            assertEquals("GameRules.winningSeat(trick, Trump.values()[trump.ordinal()]) must equal 0", 0, GameRules.winningSeat(trick, Trump.values()[trump.ordinal()]));
        }
    }

    @Test public void rw05HighestTrumpWinsRegardlessOfSeatOrPlayPosition() {
        for (int leader = 0; leader < 4; leader++) {
            List<PlayedCard> trick = List.of(new PlayedCard(leader, card(Suit.SPADES, 9)),
                    new PlayedCard((leader+1)%4, card(Suit.SPADES, 14)),
                    new PlayedCard((leader+2)%4, card(Suit.SPADES, 13)),
                    new PlayedCard((leader+3)%4, card(Suit.SPADES, 7)));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "leader", leader, "trick", trick);
            assertEquals("GameRules.winningSeat(trick, Trump.SPADES) must equal (leader + 1) % 4", (leader+1)%4, GameRules.winningSeat(trick, Trump.SPADES));
        }
    }

    @Test public void rw06AllEightyOneBidAndWonPairsHaveExactBonusOrSymmetricPenalty() {
        for (int won = 0; won <= 8; won++) for (int bid = 0; bid <= 8; bid++) {
            int expected = won == bid ? 5 + won : -Math.abs(won-bid);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid, "expected", expected, "won", won);
            assertEquals("won=" + won + ", bid=" + bid, expected, GameRules.scoreDelta(won, bid, RO));
        }
        assertEquals("GameRules.scoreDelta(0, 0, RO) must equal 5", 5, GameRules.scoreDelta(0, 0, RO));
        assertEquals("GameRules.scoreDelta(0, 8, RO) must equal -8", -8, GameRules.scoreDelta(0, 8, RO));
        assertEquals("GameRules.scoreDelta(8, 0, RO) must equal -8", -8, GameRules.scoreDelta(8, 0, RO));
        assertEquals("GameRules.scoreDelta(8, 8, RO) must equal 13", 13, GameRules.scoreDelta(8, 8, RO));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.scoreDelta(-1, 0, RO));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.scoreDelta(9, 0, RO));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> GameRules.scoreDelta(0, 9, RO));
        assertEquals("GameRules.scoreDelta(0, 0, GameMode.OH_HELL) must equal 10", 10, GameRules.scoreDelta(0, 0, GameMode.OH_HELL));
    }

    @Test public void rw03LastBidExhaustiveForEveryTableSizeAndPriorTotal() {
        for (int n = 3; n <= 6; n++) for (int size = 1; size <= 8; size++) {
            for (int prior = 0; prior <= (n-1)*size; prior++) {
                List<Integer> last = GameRules.allowedBids(size, prior, true);
                List<Integer> earlier = GameRules.allowedBids(size, prior, false);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "earlier", earlier, "last", last, "n", n, "prior", prior, "size", size);
                assertEquals("earlier.size() must equal size + 1", size+1, earlier.size());
                for (int bid = 0; bid <= size; bid++) assertEquals(
                        "players="+n+", size="+size+", prior="+prior+", bid="+bid,
                        prior+bid != size, last.contains(bid));
            }
        }
        assertEquals("GameRules.allowedBids(1, 1, true) must equal List.of(1)", List.of(1), GameRules.allowedBids(1, 1, true));
        assertEquals("GameRules.allowedBids(1, 0, true) must equal List.of(0)", List.of(0), GameRules.allowedBids(1, 0, true));
        assertEquals("GameRules.allowedBids(1, 2, true) must equal List.of(0, 1)", List.of(0,1), GameRules.allowedBids(1, 2, true));
    }

    private static Card card(Suit suit, int rank) { return new Card(suit, rank); }
    private static List<Card> legal(List<Card> hand, Suit led, Trump trump) {
        return GameRules.legalCards(hand, led, trump, RO);
    }
}

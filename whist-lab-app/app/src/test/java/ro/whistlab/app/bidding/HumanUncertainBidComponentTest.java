package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

/** Public information only; probability checks describe the estimator, not calibrated win guarantees. */
public final class HumanUncertainBidComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void theSameUncertainQueenRecommendsOneAsTrumpAndZeroOffTrump() {
        var trump = uncertain(Trump.SPADES, 0, 0, 0, 10);
        var offTrump = uncertain(Trump.HEARTS, 0, 0, 0, 10);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "offTrump", offTrump, "trump", trump);
        assertEquals("offTrump.hand() must equal trump.hand()", trump.hand(), offTrump.hand());
        assertEquals("offTrump.seats() must equal trump.seats()", trump.seats(), offTrump.seats());
        assertEquals("new HumanBidSelection.Request(trump).evaluate() must equal 1", 1, new HumanBidSelection.Request(trump).evaluate());
        assertEquals("new HumanBidSelection.Request(offTrump).evaluate() must equal 0", 0, new HumanBidSelection.Request(offTrump).evaluate());
        double chance = option(evaluate(trump), 1).exactContractProbability();
        assertTrue("chance > 0 && chance < 1 must hold", chance > 0 && chance < 1);
    }

    @Test public void aStrongPublicBidChangesTheEstimateWithoutBecomingKnowledgeOfHiddenCards() {
        var quiet = uncertain(Trump.SPADES, 0, 0, 0, 10);
        var strong = uncertain(Trump.SPADES, 2, 0, 0, 10);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "quiet", quiet, "strong", strong);
        assertEquals("strong.hand() must equal quiet.hand()", quiet.hand(), strong.hand());
        assertEquals("strong.turnedCard() must equal quiet.turnedCard()", quiet.turnedCard(), strong.turnedCard());
        var quietDecision = evaluate(quiet); var strongDecision = evaluate(strong);
        assertTrue("option(quietDecision, 1).exactContractProbability() > option(strongDecision, 1).exactContractProbability() must hold", option(quietDecision, 1).exactContractProbability()
                > option(strongDecision, 1).exactContractProbability());
        // At 2–8 cards the published bid is a target in simulated continuations.
        // It does not yet activate a calibrated posterior over the bidder's private hand.
        assertEquals("MonteCarloAi.unknownCards(strong) must equal MonteCarloAi.unknownCards(quiet)", MonteCarloAi.unknownCards(quiet), MonteCarloAi.unknownCards(strong));
        assertEquals("new HumanBidSelection.Request(strong).evaluate() must equal 1", 1, new HumanBidSelection.Request(strong).evaluate());
    }

    @Test public void aTurnedHigherTrumpIsExcludedAndImprovesTheExactOneCardEstimate() {
        var lowTurned = oneCard(0, 10, 7);
        var aceTurned = oneCard(0, 10, 14);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "aceTurned", aceTurned, "lowTurned", lowTurned);
        assertFalse("MonteCarloAi.unknownCards(aceTurned).contains(aceTurned.turnedCard()) must remain false", MonteCarloAi.unknownCards(aceTurned).contains(aceTurned.turnedCard()));
        assertTrue("MonteCarloAi.unknownCards(lowTurned).contains(aceTurned.turnedCard()) must hold", MonteCarloAi.unknownCards(lowTurned).contains(aceTurned.turnedCard()));
        assertTrue("option(evaluate(aceTurned), 1).exactContractProbability() > option(evaluate(lowTurned), 1).exactContractProbability() must hold", option(evaluate(aceTurned), 1).exactContractProbability()
                > option(evaluate(lowTurned), 1).exactContractProbability());
    }

    @Test public void aCommonScoreTranslationPreservesTheCompleteUncertainBidEstimate() {
        var expected = evaluate(uncertain(Trump.SPADES, 0, 0, 0, 10));
        for (int offset : List.of(-100, 100)) {
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "expected", expected, "offset", offset);
            assertEquals("A common score offset must not replace the sampled card worlds", expected,
                    evaluate(uncertain(Trump.SPADES, 0, offset, 0, 10)));
        }
    }

    @Test public void theFifthSuccessAddsTheConfiguredBonusWeightedByExactProbability() {
        var five = evaluate(uncertain(Trump.SPADES, 0, 0, 4, 5));
        var ten = evaluate(uncertain(Trump.SPADES, 0, 0, 4, 10));
        for (var low : five.candidates()) {
            var high = option(ten, low.bid());
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "five", five, "high", high, "low", low, "ten", ten);
            assertEquals("high.samples() must equal low.samples()", low.samples(), high.samples());
            assertEquals(low.exactContractProbability(), high.exactContractProbability(), 0);
            assertEquals(5 * low.exactContractProbability(), high.expectedPoints() - low.expectedPoints(), 1e-12);
        }
        assertEquals("ten.bid() must equal 1", 1, ten.bid());
    }

    @Test public void theFifthMissAppliesTheConfiguredPenaltyWeightedByMissProbability() {
        var five = evaluate(uncertain(Trump.SPADES, 0, 0, -4, 5));
        var ten = evaluate(uncertain(Trump.SPADES, 0, 0, -4, 10));
        for (var low : five.candidates()) {
            var high = option(ten, low.bid());
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "five", five, "high", high, "low", low, "ten", ten);
            assertEquals("high.samples() must equal low.samples()", low.samples(), high.samples());
            assertEquals(low.exactContractProbability(), high.exactContractProbability(), 0);
            assertEquals(-5 * (1 - low.exactContractProbability()), high.expectedPoints() - low.expectedPoints(), 1e-12);
        }
        assertEquals("ten.bid() must equal 1", 1, ten.bid());
    }

    @Test public void oneCardHandsNeverPayEitherFiveHandSeriesAdjustment() {
        for (int streak : List.of(-4, 4)) {
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "streak", streak);
            assertEquals("evaluate(oneCard(streak, 10, 7)) must equal evaluate(oneCard(streak, 5, 7))", evaluate(oneCard(streak, 5, 7)), evaluate(oneCard(streak, 10, 7)));
        }
    }

    @Test public void theLeadingRivalIncludesCurrentTotalsAndEachPlayersOwnSeriesPayment() {
        var seats = List.of(new MonteCarloAi.Seat(2, 1, 0, 10, 4),
                new MonteCarloAi.Seat(2, 1, 0, 30, -4), new MonteCarloAi.Seat(2, 0, 0, 28, 4),
                new MonteCarloAi.Seat(2, 0, 0, 20, 4));
        var view = new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 0, 2, 2,
                uncertain(Trump.SPADES, 0, 0, 0, 10).hand(), seats, List.of(), List.of(),
                new Card(Suit.SPADES, 7), true, 10, 10);
        // Own exact: 6+10. Rival totals: 30-1-10, 28-1, 20+5+10.
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "seats", seats, "view", view);
        assertArrayEquals(new double[] {16, 35, 1, 2},
                MonteCarloAi.utility(view, new int[] {1, 0, 1, 0}, new int[] {1, 1, 0, 0}), 0);
    }

    private static MonteCarloAi.View uncertain(Trump trump, int publicBid, int offset, int streak, int magnitude) {
        var seats = new ArrayList<MonteCarloAi.Seat>();
        for (int seat = 0; seat < 4; seat++) seats.add(new MonteCarloAi.Seat(2, seat == 3 ? publicBid : null, 0,
                offset + seat * 9, seat == 0 ? streak : 0));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, trump, 0, 2, 2,
                List.of(new Card(Suit.SPADES, 12), new Card(Suit.CLUBS, 9)), seats, List.of(), List.of(),
                new Card(trump.suit().orElseThrow(), 7), true, magnitude, magnitude);
    }

    private static MonteCarloAi.View oneCard(int streak, int magnitude, int turnedRank) {
        var seats = new ArrayList<MonteCarloAi.Seat>();
        for (int seat = 0; seat < 4; seat++) seats.add(new MonteCarloAi.Seat(1, seat == 3 ? 0 : null, 0,
                seat * 9, seat == 0 ? streak : 0));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 0, 2, 1,
                List.of(new Card(Suit.SPADES, 12)), seats, List.of(), List.of(),
                new Card(Suit.SPADES, turnedRank), true, magnitude, magnitude);
    }

    private static MonteCarloAi.BidDecision evaluate(MonteCarloAi.View view) {
        return MonteCarloAi.chooseBid(view, HardStrategyProfile.CURRENT);
    }

    private static MonteCarloAi.BidOption option(MonteCarloAi.BidDecision decision, int bid) {
        return decision.candidates().stream().filter(option -> option.bid() == bid).findFirst().orElseThrow();
    }
}

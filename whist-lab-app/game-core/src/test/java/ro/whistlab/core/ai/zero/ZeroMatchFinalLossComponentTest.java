package ro.whistlab.core.ai.zero;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

public final class ZeroMatchFinalLossComponentTest extends GradleAuditedTest {
    @Test public void equalZeroSupportFavorsTheSmallerResidualInTheLastAuction() {
        var hand = List.of(card(Suit.SPADES, 5), card(Suit.SPADES, 6), card(Suit.SPADES, 8),
                card(Suit.CLUBS, 8), card(Suit.CLUBS, 12), card(Suit.DIAMONDS, 6),
                card(Suit.DIAMONDS, 8), card(Suit.DIAMONDS, 13));
        var seats = List.of(new Seat(8, 1, 0, 125, -1), new Seat(8, null, 0, 1, -3),
                new Seat(8, 3, 0, 79, 1), new Seat(8, 2, 0, 41, 1), new Seat(8, 1, 0, 68, 3));
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 1, 1, 8,
                hand, seats, List.of(), List.of(), null, true, 10, 10);
        var legal = GameRules.allowedBids(8, 7, true);
        int index = lastIndex(view);
        GradleInputs.fixture("Observed final auction at plus one, dealer cannot bid one",
                "own cards", hand, "public bids and scores", seats, "legal bids", legal);
        assertFalse("The dealer rule excludes bid one", legal.contains(1));
        var goal = ZeroMatchObjective.forTurn(view, index, legal).orElseThrow();
        assertEquals("Bid zero requires exactly one trick", List.of(1), goal.continuationTargets(0));
        assertEquals("Bid two can settle at one or three tricks", List.of(1, 3), goal.continuationTargets(2));
        assertEquals("The safer tie chooses bid two", Optional.of(2), ZeroMatchSearch.bid(view, index));
    }

    @Test public void aControlledLossCanLeaveOneOrTwoTricksEvenWithASeriesPenalty() {
        for (int penalty : List.of(0, 5, 10)) {
            for (int kept : List.of(0, 1, 2)) {
                int balance = 8 - kept + penalty;
                var seats = List.of(new Seat(8, 8, 0, balance, penalty == 0 ? 0 : -4),
                        new Seat(8, 1, 0, 100), new Seat(8, 1, 0, 110));
                var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 0, 2, 8,
                        List.of(), seats, List.of(), List.of(), null, penalty > 0, 10,
                        penalty == 0 ? 5 : penalty);
                GradleInputs.fixture("An eight-trick contract and a scheduled series penalty",
                        "score before hand", balance, "penalty", penalty, "tricks to retain", kept);
                var goal = ZeroMatchObjective.forTurn(view, lastIndex(view), List.of()).orElseThrow();
                assertEquals("Loss equals eight minus retained tricks, plus the series penalty",
                        List.of(kept), goal.continuationTargets(8));
            }
        }
    }

    @Test public void nextDealerCountsOnlyLegalNeighborsThatRemainLosses() {
        for (int penalty : List.of(0, 5, 10)) {
            for (int gap = 1; gap <= 8; gap++) {
                int balance = 1 + gap + penalty;
                var seats = List.of(new Seat(8, 2, 0, 110),
                        new Seat(8, 0, 0, balance, penalty == 0 ? 0 : -3), new Seat(8, 1, 0, 120));
                var dealer = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 1, 0, 8,
                        List.of(), seats, List.of(), List.of(), null, penalty > 0, 10, penalty == 0 ? 5 : penalty);
                var other = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 1, 2, 8,
                        List.of(), seats, List.of(), List.of(), null, penalty > 0, 10, penalty == 0 ? 5 : penalty);
                GradleInputs.fixture("Next auction with a possible one-trick error",
                        "miss gap", gap, "series penalty", penalty, "balance before the final hand", balance - 1);
                var dealerGoal = ZeroMatchObjective.forTurn(dealer, lastIndex(dealer) - 1, List.of()).orElseThrow();
                var otherGoal = ZeroMatchObjective.forTurn(other, lastIndex(other) - 1, List.of()).orElseThrow();
                int exactPaths = 0;
                int losingNeighbors = 0;
                for (int bid = 0; bid <= 8; bid++) {
                    for (int won = 0; won <= 8; won++) {
                        int points = penalty == 0 ? GameRules.scoreDelta(won, bid, GameMode.LONG_ROMANIAN)
                                : RomanianScoring.score(won, bid, 8, -4, 10, penalty).totalPoints();
                        if (points + balance - 1 != 0) continue;
                        exactPaths++;
                        if (won > 0 && won - 1 != bid) losingNeighbors++;
                        if (won < 8 && won + 1 != bid) losingNeighbors++;
                    }
                }
                assertEquals("The exact finishing routes are independent of auction order",
                        exactPaths, dealerGoal.finishingOptions(0, 1));
                assertEquals("The next dealer prefers a margin measured by legal one-trick losing neighbors",
                        losingNeighbors, 2 * dealerGoal.finishingPreference(0, 1));
                assertEquals("Other positions use the number of exact finishing routes",
                        exactPaths, otherGoal.finishingPreference(0, 1));
            }
        }
    }

    @Test public void theRecoveredControlledReplayFinishesAtZero() {
        var scenario = new ZeroMatchPair.Case(GameMode.LONG_ROMANIAN, 5, 202611073596L, 0, true, 1);
        GradleInputs.fixture("Controlled five-seat replay with the lost final conversion", "scenario", scenario);
        var match = ZeroMatchPair.play(scenario).on();
        var actor = match.seats().get(1);
        var finalHand = match.hands().get(match.hands().size() - 1);
        GradleInputs.fixture("Final contract in the repaired replay", "bid", finalHand.bids().get(1),
                "tricks", finalHand.tricks().get(1), "score", actor.raw());
        assertEquals("The focal player settles the score", 0, actor.raw());
        assertEquals("An exact zero converts to the configured final award", 500, actor.finalScore());
    }

    private static int lastIndex(View view) {
        return GameRules.handSizes(view.mode(), view.seats().size()).size() - 1;
    }

    private static Card card(Suit suit, int rank) {
        return new Card(suit, rank);
    }
}

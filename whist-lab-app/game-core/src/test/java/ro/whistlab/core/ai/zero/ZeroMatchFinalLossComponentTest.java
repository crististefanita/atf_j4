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
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

public final class ZeroMatchFinalLossComponentTest extends GradleAuditedTest {
    @Test public void hardCanConvertInANaturalAllHardMatch() {
        var scenario = new ZeroMatchPair.Case(GameMode.LONG_ROMANIAN, 4, 202611075962L, 3, true, null, true);
        var match = ZeroMatchPair.play(scenario).on();
        var actor = match.seats().get(0);
        GradleInputs.fixture("Natural four-seat replay with Hard at every seat", "scenario", scenario,
                "result", actor, "first zero decision",
                match.pivots().stream().filter(pivot -> pivot.seat() == 0 && pivot.selected()).findFirst());
        assertEquals("The public proof gives Hard a route to settle the raw score", 0, actor.raw());
        assertEquals("The natural match awards the exact-zero conversion", 500, actor.finalScore());
    }

    @Test public void followingWithTheZeroTargetAlreadyHeldPreservesTheWinningLine() {
        var lowDiamond = card(Suit.DIAMONDS, 5);
        var highDiamond = card(Suit.DIAMONDS, 8);
        var hand = List.of(card(Suit.SPADES, 9), card(Suit.SPADES, 11), card(Suit.HEARTS, 8),
                card(Suit.HEARTS, 10), card(Suit.HEARTS, 12), lowDiamond, highDiamond);
        var seats = List.of(new Seat(7, 5, 0, 5, -2), new Seat(7, 1, 0, 49, 3),
                new Seat(7, 2, 0, 82, 1), new Seat(6, 2, 1, 122, 2),
                new Seat(6, 1, 0, 104, 1), new Seat(6, 2, 0, 124, 3));
        var history = List.of(new PlayedCard(4, card(Suit.CLUBS, 13)), new PlayedCard(5, card(Suit.CLUBS, 9)),
                new PlayedCard(0, card(Suit.CLUBS, 11)), new PlayedCard(1, card(Suit.CLUBS, 5)),
                new PlayedCard(2, card(Suit.CLUBS, 6)), new PlayedCard(3, card(Suit.CLUBS, 14)),
                new PlayedCard(3, card(Suit.DIAMONDS, 13)), new PlayedCard(4, card(Suit.DIAMONDS, 6)),
                new PlayedCard(5, card(Suit.DIAMONDS, 14)));
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 0, 3, 8, hand, seats,
                history.subList(6, 9), history, null, true, 10, 10);
        GradleInputs.fixture("Six seats, zero tricks required, two losing cards follow an ace",
                "observation", view, "legal cards", List.of(lowDiamond, highDiamond));
        var goal = ZeroMatchObjective.forTurn(view, lastIndex(view), List.of()).orElseThrow();
        assertEquals("A five-point miss requires preserving the current zero tricks",
                List.of(0), goal.continuationTargets(5));
        assertEquals("Equal sampled support uses the conventional fallback while following",
                Optional.of(lowDiamond), ZeroMatchSearch.card(view, lastIndex(view)));
    }

    @Test public void leadingForTwoTricksStillUsesTheClosingBalanceOnEqualSupport() {
        var tenOfDiamonds = card(Suit.DIAMONDS, 10);
        var hand = List.of(card(Suit.SPADES, 5), card(Suit.HEARTS, 6), card(Suit.HEARTS, 13),
                card(Suit.HEARTS, 14), card(Suit.CLUBS, 8), card(Suit.CLUBS, 10),
                tenOfDiamonds, card(Suit.DIAMONDS, 12));
        var seats = List.of(new Seat(8, 2, 0, 86, 2), new Seat(8, 0, 0, 186, 2),
                new Seat(8, 1, 0, 73, -1), new Seat(8, 1, 0, 66, 1),
                new Seat(8, 1, 0, 64, 1), new Seat(8, 7, 0, 5, -2));
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 5, 4, 8, hand, seats,
                List.of(), List.of(), null, true, 10, 10);
        GradleInputs.fixture("Six seats, leading the final hand with two tricks required", "observation", view);
        assertEquals("The lead with the smaller closing balance remains preferred",
                Optional.of(tenOfDiamonds), ZeroMatchSearch.card(view, lastIndex(view)));
    }

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

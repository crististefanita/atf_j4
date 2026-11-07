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

public final class ZeroMatchBoundsComponentTest extends GradleAuditedTest {
    @Test public void penultimateHandDoesNotAssumeAnUnreachableExtraSeries() {
        var view = beforePenultimateHand(-4);
        int index = GameRules.handSizes(view.mode(), view.seats().size()).size() - 2;
        GradleInputs.fixture("Three-seat 8–1–8, public bids and scores at the penultimate hand",
                "seats", view.seats(), "remaining schedule", List.of(8, 8), "index", index);
        var objective = ZeroMatchObjective.forTurn(view, index, List.of());
        assertTrue("The second rival cannot go below 27, above the actor's ceiling of 23", objective.isPresent());
        assertTrue("A miss by one pays the negative series and leaves minus nine, recoverable in the final hand",
                objective.orElseThrow().weight(3, 2) > 0);
        assertEquals("An exact bid gives plus eight and leaves ten, out of the final hand's losing range",
                0.0, objective.orElseThrow().weight(3, 3), 0.0);
    }

    @Test public void aSeriesThatCanStillBePaidKeepsThePodiumOpen() {
        var view = beforePenultimateHand(3);
        int index = GameRules.handSizes(view.mode(), view.seats().size()).size() - 2;
        GradleInputs.fixture("Same public scores and bids, but the actor enters with three exact contracts",
                "seats", view.seats(), "remaining schedule", List.of(8, 8), "index", index);
        assertTrue("An exact result followed by a fifth exact contract raises the ceiling to 33",
                ZeroMatchObjective.forTurn(view, index, List.of()).isEmpty());
    }

    @Test public void finalHandRetainsBothWaysToMissByOne() {
        var seats = List.of(new Seat(8, 2, 0, 1), new Seat(8, 1, 0, 59),
                new Seat(8, 2, 0, 68), new Seat(8, 1, 0, 101), new Seat(8, 1, 0, 83));
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 0, 4, 8, List.of(),
                seats, List.of(), List.of());
        int index = GameRules.handSizes(view.mode(), seats.size()).size() - 1;
        GradleInputs.fixture("Final hand, bid two, score plus one, podium excluded", "public seats", seats);
        var objective = ZeroMatchObjective.forTurn(view, index, List.of()).orElseThrow();
        assertEquals("One or three tricks deduct exactly one point", List.of(1, 3), objective.continuationTargets(2));
        assertEquals("A bid of zero has no lower branch", List.of(1), objective.continuationTargets(0));
    }

    @Test public void aPossibleTieForSecondKeepsNormalScoring() {
        var tie = beforeLastTwoOneCardHands(2, 15);
        var excluded = beforeLastTwoOneCardHands(2, 16);
        int index = GameRules.handSizes(tie.mode(), tie.seats().size()).size() - 2;
        GradleInputs.fixture("Own ceiling 13, second rival floor 13 or 14",
                "tie seats", tie.seats(), "strictly excluded seats", excluded.seats());
        assertTrue("An equal lower bound leaves second place open",
                ZeroMatchObjective.forTurn(tie, index, List.of()).isEmpty());
        assertTrue("Two rivals strictly ahead permit the two one-point losses",
                ZeroMatchObjective.forTurn(excluded, index, List.of()).isPresent());
    }

    @Test public void anIntervalContainingZeroDoesNotProveExactReachability() {
        var view = beforeLastTwoOneCardHands(0, 100);
        int index = GameRules.handSizes(view.mode(), view.seats().size()).size() - 2;
        GradleInputs.fixture("Two one-card hands with deltas -1, +5, +6", "seats", view.seats());
        assertTrue("No pair of actual deltas sums to zero",
                ZeroMatchObjective.forTurn(view, index, List.of()).isEmpty());
    }

    @Test public void aFifthMissCanCreateTheOnlyExactPathOrOvershootIt() {
        var twelve = beforeLastTwoEightCardHands(13, -3);
        var six = beforeLastTwoEightCardHands(7, -3);
        int index = GameRules.handSizes(twelve.mode(), twelve.seats().size()).size() - 2;
        GradleInputs.fixture("Current miss advances the counter to minus four",
                "can use minus ten", twelve.seats(), "overshoots", six.seats());
        var objective = ZeroMatchObjective.forTurn(twelve, index, List.of()).orElseThrow();
        assertEquals("Fourteen bid and trick pairs lose two plus the ten-point series penalty",
                14, objective.finishingOptions(3, 2));
        assertTrue("Every possible future miss would overshoot the smaller positive residue",
                ZeroMatchObjective.forTurn(six, index, List.of()).isEmpty());
    }

    @Test public void aPenultimateDiscardProtectsTheSmallerControlledLoss() {
        var history = List.of(play(0, Suit.HEARTS, 10), play(1, Suit.HEARTS, 14),
                play(2, Suit.HEARTS, 13), play(1, Suit.DIAMONDS, 13), play(2, Suit.DIAMONDS, 9),
                play(0, Suit.DIAMONDS, 12), play(1, Suit.SPADES, 10), play(2, Suit.SPADES, 9),
                play(0, Suit.SPADES, 14), play(0, Suit.CLUBS, 14), play(1, Suit.CLUBS, 12),
                play(2, Suit.CLUBS, 13), play(0, Suit.DIAMONDS, 14), play(1, Suit.DIAMONDS, 11));
        var discard = new Card(Suit.CLUBS, 10);
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 2, 2, 8,
                List.of(discard, new Card(Suit.SPADES, 11), new Card(Suit.SPADES, 12), new Card(Suit.SPADES, 13)),
                List.of(new Seat(3, 3, 2, 77, 2), new Seat(3, 2, 2, 41, -1), new Seat(4, 2, 0, 4, 1)),
                history.subList(12, 14), history, null, true, 10, 10);
        GradleInputs.fixture("Observed public position before the decisive discard",
                "own cards", view.hand(), "public seats", view.seats(), "public history", history);
        assertEquals("The club discard keeps more ways to settle the final residue on equal zero support",
                Optional.of(discard), ZeroMatchSearch.card(view, 19));
    }

    @Test public void finalPlayCanUseTheFifthMissToSettleTheScore() {
        var history = List.of(
                play(0, Suit.SPADES, 14), play(1, Suit.SPADES, 9), play(2, Suit.SPADES, 10),
                play(0, Suit.SPADES, 11), play(1, Suit.SPADES, 12), play(2, Suit.SPADES, 13),
                play(2, Suit.DIAMONDS, 14), play(0, Suit.DIAMONDS, 9), play(1, Suit.DIAMONDS, 10),
                play(2, Suit.DIAMONDS, 11), play(0, Suit.DIAMONDS, 13), play(1, Suit.DIAMONDS, 12),
                play(0, Suit.CLUBS, 14), play(1, Suit.CLUBS, 10), play(2, Suit.CLUBS, 11),
                play(0, Suit.CLUBS, 13), play(1, Suit.CLUBS, 12), play(2, Suit.HEARTS, 10),
                play(0, Suit.HEARTS, 11), play(1, Suit.HEARTS, 12));
        var ace = new Card(Suit.HEARTS, 14);
        var view = new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 2, 2, 8,
                List.of(new Card(Suit.HEARTS, 9), ace),
                List.of(new Seat(1, 3, 4, 100), new Seat(1, 2, 0, 110), new Seat(2, 2, 2, 11, -4)),
                history.subList(18, 20), history, null, true, 10, 10);
        GradleInputs.fixture("Last two tricks, a balance of eleven and four consecutive misses",
                "own cards", view.hand(), "public seats", view.seats(), "public history", history);
        int index = GameRules.handSizes(view.mode(), view.seats().size()).size() - 1;
        assertEquals("Take the third trick to pursue the fifth miss", Optional.of(ace), ZeroMatchSearch.card(view, index));
        var outcome = RomanianScoring.score(3, 2, 8, -4, 10, 10);
        assertEquals("The one-trick miss pays the ten-point penalty", -10, outcome.streakPoints());
        assertEquals("The combined loss settles eleven exactly", 0, 11 + outcome.totalPoints());
    }

    private static PlayedCard play(int seat, Suit suit, int rank) {
        return new PlayedCard(seat, new Card(suit, rank));
    }

    private static View beforeLastTwoOneCardHands(int total, int secondRival) {
        return new View(GameMode.ROMANIAN_WHIST, Trump.NO_TRUMP, 0, 2, 1, List.of(),
                List.of(new Seat(1, 0, 0, total), new Seat(1, 0, 0, 100), new Seat(1, 0, 0, secondRival)),
                List.of(), List.of());
    }

    private static View beforeLastTwoEightCardHands(int total, int streak) {
        return new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 0, 2, 8, List.of(),
                List.of(new Seat(8, 3, 0, total, streak), new Seat(8, 1, 0, 125),
                        new Seat(8, 2, 0, 125)),
                List.of(), List.of(), null, true, 10, 10);
    }

    private static View beforePenultimateHand(int ownStreak) {
        return new View(GameMode.LONG_ROMANIAN, Trump.NO_TRUMP, 0, 2, 8, List.of(),
                List.of(new Seat(8, 3, 0, 2, ownStreak), new Seat(8, 1, 0, 70, 4),
                        new Seat(8, 2, 0, 41, 0)),
                List.of(), List.of(), null, true, 10, 10);
    }
}

package ro.whistlab.core.ai.zero;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import static ro.whistlab.core.model.Suit.*;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class ZeroMatchPolicyComponentTest extends GradleAuditedTest {
    @Test public void pivotsOnlyWhenTwoRivalsAreCertainToFinishAheadAndZeroIsReachable() {
        var opening = oneCardDeal(2).snapshot();
        var pivot = asMatch(opening, 0, true, 1, 100, 101);
        var tieIsPossible = asMatch(opening, 0, true, 1, 100, 8);
        var disabled = asMatch(opening, 0, false, 1, 100, 101);
        var zeroImpossible = asMatch(opening, 0, true, 2, 100, 101);
        var zeroNextHand = asMatch(opening, 1, true, 2, 100, 101);
        GradleInputs.fixture("One card left in the schedule, actor leads with ace of trump",
                "public positions", List.of(pivot, tieIsPossible, disabled, zeroImpossible, zeroNextHand));
        assertEquals("Bid zero and take a trick to lose precisely one point", List.of(0, 0), bids(pivot));
        assertEquals("A possible tie for second preserves contract pursuit", List.of(1, 1), bids(tieIsPossible));
        assertEquals("Disabled option preserves contract pursuit", List.of(1, 1), bids(disabled));
        assertEquals("Two points cannot be lost in the last one-card hand", List.of(1, 1), bids(zeroImpossible));
        assertEquals("Two successive one-point losses are still possible with two hands left",
                List.of(0, 0), bids(zeroNextHand));
    }

    @Test public void theZeroTargetCannotOverrideTheDealerBidRestriction() {
        for (int count = 3; count <= 6; count++) {
            int[] totals = rivalScores(count, 100);
            for (int prefix : List.of(0, 1, 2)) {
                var game = oneCardDeal(0, count);
                for (int seat = 1; seat < count; seat++) game.submitBid(seat, seat <= prefix ? 1 : 0);
                var state = asMatch(game.snapshot(), 0, true, totals);
                var classic = asMatch(game.snapshot(), 0, false, totals);
                GradleInputs.fixture("One-card dealer with a public prefix",
                        "players", count, "prior sum", prefix, "public state", state);
                if (prefix == 2) {
                    assertEquals("An unforced dealer can aim for zero by bidding zero", List.of(0, 0), bids(state));
                    assertEquals("The usual policy pursues the winning ace", List.of(1, 1), bids(classic));
                } else {
                    try (var work = SearchWork.measure(Long.MAX_VALUE)) {
                        assertEquals("Both levels must make the only legal bid", List.of(prefix, prefix), bids(state));
                        assertEquals("With the rule disabled the same bid is forced", List.of(prefix, prefix), bids(classic));
                        var events = work.snapshot().events();
                        assertEquals("No zero search is needed for a forced bid", 0L,
                                events.get(SearchWork.Event.ZERO_ELIGIBLE).longValue());
                        assertEquals("A forced bid does not count as a strategy selection", 0L,
                                events.get(SearchWork.Event.ZERO_SELECTED).longValue());
                    }
                }
            }
        }
    }

    @Test public void aCloseTableKeepsBothLevelsOnTheirOrdinaryPolicy() {
        for (int count = 3; count <= 6; count++) {
            var opening = oneCardDeal(count - 1, count).snapshot();
            int[] totals = rivalScores(count, 4);
            var near = asMatch(opening, 0, true, totals);
            var classic = asMatch(opening, 0, false, totals);
            GradleInputs.fixture("One-card auction with a reachable conventional finish", "public state", near);
            assertEquals("Normal and Hard keep their original bids while second place is open", bids(classic), bids(near));
        }
        var tail = legalTwoTrickTail().snapshot();
        var near = asMatch(tail, 0, true, 5, 5, 5);
        var classic = asMatch(tail, 0, false, 5, 5, 5);
        GradleInputs.fixture("Eight-card finale with a reachable conventional finish",
                "observation", MonteCarloAi.observe(near, 0));
        try (var work = SearchWork.measure(Long.MAX_VALUE)) {
            assertEquals("Normal and Hard keep their original play while second place is open", cards(classic), cards(near));
            assertEquals("The play never enters the zero planner", 0L,
                    work.snapshot().events().get(SearchWork.Event.ZERO_ELIGIBLE).longValue());
        }
    }

    @Test public void finalLongHandPursuesTheTrickCountThatMakesTheWholeMatchZero() {
        var game = legalTwoTrickTail();
        var tail = game.snapshot();
        var pivot = asMatch(tail, 0, true, 5, 100, 101);
        var classic = asMatch(tail, 0, false, 5, 100, 101);
        var pivotWithPenalty = withNegativeStreak(pivot, 15);
        var ace = new Card(HEARTS, 14);
        var nine = new Card(HEARTS, 9);
        GradleInputs.fixture("Legal eight-card deal with six tricks scored and the actor last to follow hearts",
                "observation", MonteCarloAi.observe(pivot, 0),
                "series observation", MonteCarloAi.observe(pivotWithPenalty, 0), "remaining target", 5);
        assertEquals("The ace wins the seventh trick and the remaining heart takes the eighth",
                List.of(ace, ace), cards(pivot));
        assertEquals("Without the rule, losing the trick preserves the smaller contract penalty",
                List.of(nine, nine), cards(classic));
        assertEquals("A fifth miss can make a loss of fifteen the exact required delta",
                List.of(ace, ace), cards(pivotWithPenalty));
        game.playCard(0, ace);
        playFirstLegalUntilScore(game);
        assertEquals("A separate legal completion confirms five tricks for a zero bid",
                5, game.snapshot().players().get(0).tricksWon());
        assertEquals("The final hand delta in that completion is minus five",
                -5, game.snapshot().players().get(0).totalScore());
    }

    private static List<Integer> bids(GameSession.Snapshot state) {
        return List.of(AiPolicy.chooseBid(state, 0, AiDifficulty.NORMAL), AiPolicy.chooseBid(state, 0, AiDifficulty.HARD));
    }

    private static List<Card> cards(GameSession.Snapshot state) {
        return List.of(AiPolicy.chooseCard(state, 0, AiDifficulty.NORMAL), AiPolicy.chooseCard(state, 0, AiDifficulty.HARD));
    }

    private static GameSession oneCardDeal(int dealer) {
        return oneCardDeal(dealer, 3);
    }

    private static GameSession oneCardDeal(int dealer, int count) {
        var mode = GameMode.ROMANIAN_WHIST;
        var hands = new ArrayList<List<Card>>();
        hands.add(List.of(new Card(HEARTS, 14)));
        GameRules.deck(mode, count).stream().filter(card -> card.suit() == CLUBS).limit(count - 1)
                .map(List::of).forEach(hands::add);
        return GameSession.fromDeal(new GameConfig(count, mode, AiDifficulty.NORMAL, 1), hands, Trump.HEARTS, dealer);
    }

    private static int[] rivalScores(int count, int rivalScore) {
        int[] totals = new int[count];
        java.util.Arrays.fill(totals, rivalScore);
        totals[0] = 1;
        return totals;
    }

    private static GameSession legalTwoTrickTail() {
        var reserved = List.of(List.of(new Card(HEARTS, 9), new Card(HEARTS, 14)),
                List.of(new Card(HEARTS, 11), new Card(CLUBS, 9)),
                List.of(new Card(HEARTS, 12), new Card(SPADES, 9)));
        var mode = GameMode.LONG_ROMANIAN;
        var deck = new ArrayList<>(GameRules.deck(mode, 3));
        reserved.forEach(deck::removeAll);
        Collections.shuffle(deck, new Random(95));
        var hands = new ArrayList<List<Card>>();
        for (int seat = 0; seat < 3; seat++) {
            var hand = new ArrayList<>(reserved.get(seat));
            hand.addAll(deck.subList(seat * 6, seat * 6 + 6));
            hands.add(hand);
        }
        var game = GameSession.fromDeal(new GameConfig(3, mode, AiDifficulty.NORMAL, 1), hands, Trump.NO_TRUMP, 0);
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 0);
        for (int move = 0; move < 18; move++) {
            var state = game.snapshot();
            var reserve = reserved.get(state.currentSeat());
            game.playCard(state.currentSeat(), legal(state).stream()
                    .filter(card -> !reserve.contains(card)).findFirst().orElseThrow());
        }
        game.playCard(1, reserved.get(1).get(0));
        game.playCard(2, reserved.get(2).get(0));
        return game;
    }

    private static void playFirstLegalUntilScore(GameSession game) {
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            game.playCard(state.currentSeat(), legal(state).get(0));
        }
    }

    private static List<Card> legal(GameSession.Snapshot state) {
        var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        return GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led, state.trump(), state.mode());
    }

    private static GameSession.Snapshot asMatch(GameSession.Snapshot state, int futureHands, boolean enabled, int... scores) {
        var players = state.players().stream().map(player -> new GameSession.PlayerSnapshot(player.seat(),
                player.name(), player.human(), player.hand(), player.bid(), player.tricksWon(), scores[player.seat()],
                player.streak(), player.streakPoints())).toList();
        int hand = GameRules.handSizes(state.mode(), players.size()).size() - futureHands;
        return new GameSession.Snapshot(state.phase(), hand - 1, hand, state.handSize(), state.trump(),
                state.dealerSeat(), state.biddingSeat(), state.currentSeat(), state.bidsSubmitted(), state.bidTotal(),
                players, state.currentTrick(), state.lastTrick(), state.lastTrickWinnerSeat(), futureHands == 0,
                state.mode(), state.history(), state.turnedCard(), state.streakBonuses(), 10, 10, enabled);
    }

    private static GameSession.Snapshot withNegativeStreak(GameSession.Snapshot state, int score) {
        var players = new ArrayList<>(state.players());
        var actor = players.get(0);
        players.set(0, new GameSession.PlayerSnapshot(0, actor.name(), actor.human(), actor.hand(), actor.bid(),
                actor.tricksWon(), score, -4, 0));
        return new GameSession.Snapshot(state.phase(), state.handIndex(), state.handNumber(), state.handSize(),
                state.trump(), state.dealerSeat(), state.biddingSeat(), state.currentSeat(), state.bidsSubmitted(),
                state.bidTotal(), players, state.currentTrick(), state.lastTrick(), state.lastTrickWinnerSeat(), true,
                state.mode(), state.history(), state.turnedCard(), true, 10, 10, true);
    }
}

package ro.whistlab.core.session;

import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession.PlayerState;
import ro.whistlab.core.session.GameSession.SaveState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Restores score history, including legacy omissions, while checking recorded contracts and totals. */
final class CheckpointScores {
    private CheckpointScores() {}

    static ScoreHistory restore(SaveState saved, List<Integer> handSizes) {
        var config = saved.config();
        int handIndex = saved.handIndex();
        int handSize = saved.handSize();
        var phase = saved.phase();
        var players = saved.players();
        var recorded = saved.scoreHistory();
        List<Integer> totals = players.stream()
                .map(PlayerState::totalScore)
                .collect(java.util.stream.Collectors.toList());
        int completed = handIndex + (phase == GamePhase.SCORE ? 1 : 0);
        if (recorded == null) {
            List<Integer> opening = new ArrayList<>(totals);
            if (phase == GamePhase.SCORE) {
                for (PlayerState player : players)
                    opening.set(
                            player.seat(),
                            Math.subtractExact(player.totalScore(), earned(saved, player)));
            }
            recorded = new ScoreHistory(handIndex, opening, Collections.emptyList());
            if (phase == GamePhase.SCORE)
                recorded =
                        recorded.finish(
                                completed,
                                totals,
                                players.stream()
                                        .map(PlayerState::bid)
                                        .collect(java.util.stream.Collectors.toList()),
                                players.stream().map(PlayerState::tricksWon).collect(java.util.stream.Collectors.toList()));
        }
        if (recorded.completedHands() != completed
                || !recorded.latestTotals().equals(totals)
                || (phase == GamePhase.SCORE && recorded.closingTotals().isEmpty())) {
            throw new IllegalArgumentException(
                    "Score history does not match the saved hand and totals");
        }
        if (phase == GamePhase.SCORE && recorded.tricks(completed, 0) == null) {
            recorded = new ScoreHistory(recorded.priorHands(), recorded.openingTotals(),
                    recorded.closingTotals(), recorded.recordedBids(),
                    List.of(players.stream().map(PlayerState::tricksWon).collect(java.util.stream.Collectors.toList())));
        }
        for (int hand = recorded.priorHands() + 1; hand <= completed; hand++) {
            int size = handSizes.get(hand - 1);
            boolean series = config.streakBonuses() && size > 1;
            int minimum = config.mode().isRomanian() ? -size - (series ? config.streakPenaltyPoints() : 0) : 0;
            int maximum =
                    size
                            + (config.mode().isRomanian() ? 5 : 10)
                            + (series ? config.streakBonusPoints() : 0);
            int bidTotalForHand = 0;
            int wonTotal = 0;
            for (PlayerState player : players) {
                Integer contract = recorded.bid(hand, player.seat());
                if (contract != null) {
                    if (contract < 0
                            || contract > size
                            || (phase == GamePhase.SCORE
                                    && hand == completed
                                    && !contract.equals(player.bid())))
                        throw new IllegalArgumentException("Invalid recorded bid");
                    bidTotalForHand += contract;
                }
                int points = recorded.points(hand, player.seat());
                Integer won = recorded.tricks(hand, player.seat());
                if (won != null) {
                    if (won > size || (phase == GamePhase.SCORE && hand == completed && won != player.tricksWon()))
                        throw new IllegalArgumentException("Invalid recorded trick count");
                    wonTotal += won;
                    if (contract != null) {
                        int base = GameRules.scoreDelta(won, contract, config.mode());
                        int seriesPoints = points - base;
                        if (seriesPoints != 0 && (!series || seriesPoints != (won.equals(contract)
                                ? config.streakBonusPoints() : -config.streakPenaltyPoints())))
                            throw new IllegalArgumentException("Recorded tricks contradict hand points");
                    }
                }
                if (points < minimum || points > maximum)
                    throw new IllegalArgumentException(
                            "Score history exceeds the hand's scoring range");
                if (phase == GamePhase.SCORE
                        && hand == completed
                        && points != earned(saved, player))
                    throw new IllegalArgumentException(
                            "Score history does not match the completed contract");
            }
            if (recorded.bid(hand, 0) != null && bidTotalForHand == size)
                throw new IllegalArgumentException("Recorded bids violate the last bidder rule");
            if (recorded.tricks(hand, 0) != null && wonTotal != size)
                throw new IllegalArgumentException("Recorded tricks do not conserve the hand size");
        }
        return recorded;
    }

    private static int earned(SaveState saved, PlayerState player) {
        return GameRules.scoreDelta(player.tricksWon(), player.bid(), saved.config().mode())
                + player.streakPoints();
    }
}

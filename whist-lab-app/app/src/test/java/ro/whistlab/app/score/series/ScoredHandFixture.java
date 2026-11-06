package ro.whistlab.app.score.series;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Controlled scoring fixture: predicts a bid-independent legal card order, then selects exact/missed contracts. */
public final class ScoredHandFixture {
    public record Result(GameSession game, List<Integer> bids, List<Integer> tricks) { }
    @FunctionalInterface public interface Checkpoint {
        GameSession restore(String stage, GameSession game) throws Exception;
    }

    public static Result finish(GameSession game, int missedSeat, Checkpoint checkpoint) throws Exception {
        if (game.snapshot().phase() != GamePhase.BIDDING) throw new IllegalArgumentException("A fresh hand is required");
        var probe = GameSession.restoreComplete(game.saveState());
        while (probe.snapshot().phase() == GamePhase.BIDDING) {
            var state = probe.snapshot();
            probe.submitBid(state.biddingSeat(), GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0));
        }
        while (probe.snapshot().phase() == GamePhase.PLAYING) playOne(probe);
        var wins = new ArrayList<Integer>();
        for (var player : probe.snapshot().players()) wins.add(player.tricksWon());
        var bids = new ArrayList<>(wins);
        int won = wins.get(missedSeat), size = game.snapshot().handSize();
        bids.set(missedSeat, won == size ? won - 1 : won + 1);
        boolean first = true;
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            int seat = game.snapshot().biddingSeat();
            game.submitBid(seat, bids.get(seat));
            if (first) { game = checkpoint.restore("bidding", game); first = false; }
        }
        first = true;
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            playOne(game);
            if (first) { game = checkpoint.restore("playing", game); first = false; }
        }
        game = checkpoint.restore("settled", game);
        return new Result(game, List.copyOf(bids), List.copyOf(wins));
    }

    private static void playOne(GameSession game) {
        var state = game.snapshot();
        var player = state.players().get(state.currentSeat());
        var lead = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        game.playCard(player.seat(), GameRules.legalCards(player.hand(), lead, state.trump(), state.mode()).get(0));
    }

    private ScoredHandFixture() { }
}

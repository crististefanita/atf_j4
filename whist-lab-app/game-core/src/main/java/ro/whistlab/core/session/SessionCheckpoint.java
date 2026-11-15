package ro.whistlab.core.session;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession.PlayerState;
import ro.whistlab.core.session.GameSession.SaveState;
import ro.whistlab.core.session.scoring.PlayerScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Captures and validates the persisted session contract before creating a live hand. */
final class SessionCheckpoint {
    private SessionCheckpoint() {}

    static SaveState capture(GameConfig config, int handIndex, List<String> names, HandPlay hand,
                             List<PlayerScore> scores, ScoreHistory scoreHistory) {
        var players = new ArrayList<PlayerState>(names.size());
        for (int seat = 0; seat < names.size(); seat++) {
            var score = scores.get(seat);
            players.add(new PlayerState(seat, names.get(seat), seat == 0, hand.cards(seat), hand.bid(seat),
                    hand.tricksWon(seat), score.total(), score.streak(), score.streakPoints()));
        }
        return new SaveState(config, hand.phase(), handIndex, hand.size(), hand.dealerSeat(), hand.biddingSeat(),
                hand.currentSeat(), hand.bidsSubmitted(), hand.bidTotal(), hand.trump(), hand.lastTrickWinnerSeat(),
                players, hand.currentTrick(), hand.lastTrick(), hand.history(), hand.turnedCard(), scoreHistory);
    }

    static GameSession restore(SaveState saved, boolean complete) {
        Objects.requireNonNull(saved, "saved");
        if (complete) {
            int remaining = saved.players().stream().mapToInt(player -> player.hand().size()).sum();
            if (saved.history().size() != saved.handSize() * saved.config().playerCount() - remaining)
                throw new IllegalArgumentException("Complete checkpoint is missing public card history");
        }
        var config = saved.config();
        var sizes = GameRules.handSizes(config.mode(), config.playerCount());
        CheckpointValidation.header(saved, sizes);
        int countedBids = 0;
        var names = new ArrayList<String>(config.playerCount());
        var scores = new ArrayList<PlayerScore>(config.playerCount());
        for (int seat = 0; seat < config.playerCount(); seat++) {
            var player = saved.players().get(seat);
            CheckpointValidation.player(saved, player, seat);
            if (player.bid() != null) countedBids++;
            names.add(player.name());
            scores.add(new PlayerScore(player.totalScore(), player.streak(), player.streakPoints()));
        }
        CheckpointValidation.progress(saved, countedBids);
        CheckpointValidation.rules(saved);
        ScoreHistory scoreHistory = CheckpointScores.restore(saved, sizes);
        return new GameSession(config, sizes, names, saved.handIndex(), HandPlay.restored(saved), scores, scoreHistory);
    }
}

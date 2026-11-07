package ro.whistlab.app.score.series;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import java.util.List;
import ro.whistlab.core.session.GameSession;

/** Legal preconditions for isolated bonuses and penalties, including a positive running balance after a penalty. */
public final class AwardRasterFixture {
    public enum Pattern { ISOLATED_BONUS, BONUS_AND_PENALTY, PENALTY_WITH_POSITIVE_TOTAL }
    public record Finished(GameSession game, int lastEligibleIndex, List<Integer> eligibleTotals,
                           List<Integer> eligibleSeries) { }

    public static GameSession create(int players, Pattern pattern) throws Exception {
        return create(players, pattern, 0);
    }

    public static GameSession create(int players, Pattern pattern, int missedSeat) throws Exception {
        if (missedSeat < 0 || missedSeat >= players) throw new IllegalArgumentException("Missed seat outside table");
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(players, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL,
                171 + players, true, 10, 10)), atf.gradle.audit.GradleInputs.created("Human name before creation", "Audit"));
        int eligible = 0, target = pattern == Pattern.PENALTY_WITH_POSITIVE_TOTAL ? 10 : 5;
        do {
            if (game.snapshot().handSize() > 1) eligible++;
            int missed = pattern == Pattern.PENALTY_WITH_POSITIVE_TOTAL && eligible <= 5
                    || pattern == Pattern.ISOLATED_BONUS && eligible == 5 ? (missedSeat + 1) % players : missedSeat;
            game = ScoredHandFixture.finish(game, missed, (stage, source) -> source).game();
            if (eligible == target) return game;
            game.startNextHand();
        } while (true);
    }

    public static Finished completed(int players, GameMode mode) throws Exception {
        if (!mode.isRomanian()) throw new IllegalArgumentException("Romanian scoring fixture required");
        var sizes = GameRules.handSizes(mode, players);
        int last = sizes.size() - 1;
        while (sizes.get(last) == 1) last--;
        int first = last, remaining = 4;
        while (remaining > 0) { first--; if (sizes.get(first) > 1) remaining--; }
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(players, mode, AiDifficulty.NORMAL, 281 + players,
                true, 10, 10)), atf.gradle.audit.GradleInputs.created("Human name before creation", "Audit"));
        List<Integer> totals = List.of(), series = List.of();
        do {
            int missed = game.snapshot().handIndex() >= first ? 0 : 1;
            game = ScoredHandFixture.finish(game, missed, (stage, source) -> source).game();
            if (game.snapshot().handIndex() == last) {
                totals = game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
                series = game.snapshot().players().stream().map(GameSession.PlayerSnapshot::streakPoints).toList();
            }
            if (game.snapshot().finalHand()) return new Finished(game, last, totals, series);
            game.startNextHand();
        } while (true);
    }
    private AwardRasterFixture() { }
}

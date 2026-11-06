package ro.whistlab.app.score.series;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

/** Legal preconditions for isolated bonuses and penalties, including a positive running balance after a penalty. */
public final class AwardRasterFixture {
    public enum Pattern { ISOLATED_BONUS, BONUS_AND_PENALTY, PENALTY_WITH_POSITIVE_TOTAL }

    public static GameSession create(int players, Pattern pattern) throws Exception {
        var game = GameSession.newGame(new GameConfig(players, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL,
                171 + players, true, 10, 10), "Audit");
        int eligible = 0, target = pattern == Pattern.PENALTY_WITH_POSITIVE_TOTAL ? 10 : 5;
        do {
            if (game.snapshot().handSize() > 1) eligible++;
            int missed = pattern == Pattern.PENALTY_WITH_POSITIVE_TOTAL && eligible <= 5
                    || pattern == Pattern.ISOLATED_BONUS && eligible == 5 ? 1 : 0;
            game = ScoredHandFixture.finish(game, missed, (stage, source) -> source).game();
            if (eligible == target) return game;
            game.startNextHand();
        } while (true);
    }
    private AwardRasterFixture() { }
}

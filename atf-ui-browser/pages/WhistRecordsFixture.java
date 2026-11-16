import org.json.JSONArray;
import org.json.JSONObject;
import ro.whistlab.app.persistence.GameCheckpointCodec;
import ro.whistlab.app.score.series.ScoredHandFixture;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

/** Creates native-format saves by playing legal hands with controlled contracts. */
class WhistRecordsFixture {
    public static void main(String[] args) throws Exception {
        var fixtures = new JSONArray();
        for (int seats = 3; seats <= 6; seats++) {
            var game = GameSession.newGame(new GameConfig(seats, GameMode.ROMANIAN_WHIST,
                    AiDifficulty.NORMAL, 941 + seats, true, 10, 5));
            for (int hand = 1; hand <= seats + 5; hand++) {
                game = ScoredHandFixture.finish(game, 0, (stage, current) -> {
                    var saved = current.saveState();
                    var restored = GameCheckpointCodec.fromJson(GameCheckpointCodec.encode(saved).toString());
                    if (!saved.equals(restored.saveState())) throw new AssertionError(stage);
                    return restored;
                }).game();
                if (hand < seats + 5) game.startNextHand();
            }
            String checkpoint = GameCheckpointCodec.encode(game.saveState()).put("matchId", "series-" + seats).toString();
            fixtures.put(new JSONObject().put("seats", seats).put("checkpoint", checkpoint));
        }
        System.out.println(fixtures);
    }
}

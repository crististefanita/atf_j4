package ro.whistlab.app.score;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.model.Suit;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;


public class ArchivedScoreHistoryReplayTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void publishedSixHandPilotRetainsEveryPlayersVisibleRowsAcrossResume() throws Exception {
        String folder = System.getProperty("whist.scoreHistoryTraceDir", "");
        Assume.assumeFalse("Supply the published six-hand pilot directory", folder.isBlank());
        Path run = Path.of(folder);
        JSONObject manifest = new JSONObject(new String(Files.readAllBytes(run.resolve("metadata.json")), java.nio.charset.StandardCharsets.UTF_8));
        JSONObject hashes = manifest.getJSONObject("files");
        Properties result = new Properties();
        result.load(new StringReader(readVerified(run, "game-result.properties", hashes)));
        JSONObject trace = new JSONObject(readVerified(run, "public-trace.json", hashes));
        JSONArray hands = trace.getJSONArray("hands");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "folder", folder, "hands", hands, "hashes", hashes, "manifest", manifest, "result", result, "run", run, "trace", trace);
        assertEquals("hands.length() must equal 6", 6, hands.length());
        assertEquals("trace.getString(\"sourceRunId\") must equal manifest.getString(\"runId\")", manifest.getString("runId"), trace.getString("sourceRunId"));
        JSONObject profile = manifest.getJSONObject("profile");
        GameConfig config = new GameConfig(profile.getInt("seats"), GameMode.ROMANIAN_WHIST,
                AiDifficulty.fromStoredName(profile.getString("difficulty")), Long.parseLong(result.getProperty("seed")),
                hands.getJSONObject(0).getBoolean("seriesEnabled"), profile.getInt("seriesBonus"), profile.optInt("seriesPenalty",5));
        GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config));
        int moves = 0;
        for (int index = 0; index < hands.length(); index++) {
            JSONObject hand = hands.getJSONObject(index);
            GameSession.Snapshot deal = game.snapshot();
            assertEquals("deal.handNumber() must equal hand.getInt(\"hand\")", hand.getInt("hand"), deal.handNumber());
            assertEquals("deal.handSize() must equal hand.getInt(\"size\")", hand.getInt("size"), deal.handSize());
            assertEquals("deal.dealerSeat() must equal hand.getInt(\"dealer\")", hand.getInt("dealer"), deal.dealerSeat());
            for (int seat = 0; seat < config.playerCount(); seat++)
                assertEquals("deal.players().get(seat).totalScore() must equal hand.getJSONArray(\"beforeTotals\").getInt(seat)", hand.getJSONArray("beforeTotals").getInt(seat), deal.players().get(seat).totalScore());
            JSONObject actualCapture = hand.getJSONObject("exPost");
            JSONArray bids = actualCapture.getJSONArray("bids");
            for (int i = 0; i < bids.length(); i++) {
                JSONArray bid = bids.getJSONArray(i);
                game.submitBid(bid.getInt(0), bid.getInt(1));
            }
            JSONArray plays = actualCapture.getJSONArray("plays");
            for (int i = 0; i < plays.length(); i++) {
                JSONArray play = plays.getJSONArray(i);
                game.playCard(play.getInt(0), new Card(Suit.valueOf(play.getString(1)), play.getInt(2)));
                moves++;
                if (i == 0) {
                    game = resume(game);
                    assertRows(hands, index, game);
                }
            }
            assertEquals("game.snapshot().phase() must equal GamePhase.SCORE", GamePhase.SCORE, game.snapshot().phase());
            game = resume(game);
            assertRows(hands, index + 1, game);
            if (index + 1 < hands.length()) {
                game.startNextHand();
                game = resume(game);
                assertRows(hands, index + 1, game);
            }
        }
        assertEquals("moves must equal Integer.parseInt(result.getProperty(\"moves\"))", Integer.parseInt(result.getProperty("moves")), moves);
    }

    private static void assertRows(JSONArray hands, int completed, GameSession game) throws Exception {
        WhistUiText copy = new WhistUiText(HowToPlay.Language.RO);
        for (int seat = 0; seat < game.config().playerCount(); seat++) {
            var column = ScoreHistoryColumn.forPlayer(game.scoreHistory(), seat, copy);
            assertEquals("column.opening() must equal \"\"", "", column.opening());
            assertEquals("column.rows().size() must equal completed", completed, column.rows().size());
            for (int h = completed; h > 0; h--) {
                JSONObject hand = hands.getJSONObject(h - 1);
                int total = hand.getJSONObject("exPost").getJSONArray("scores").getInt(seat);
                int points = total - hand.getJSONArray("beforeTotals").getInt(seat);
                var row = column.rows().get(completed - h);
                JSONArray contracts = hand.getJSONObject("exPost").getJSONArray("bids");
                java.util.Map<Integer, Integer> bySeat = new java.util.HashMap<>();
                for (int i = 0; i < contracts.length(); i++) {
                    JSONArray contract = contracts.getJSONArray(i);
                    assertNull("bySeat.put(contract.getInt(0), contract.getInt(1)) remains absent", bySeat.put(contract.getInt(0), contract.getInt(1)));
                }
                assertEquals("row.bid() must equal Integer.toString(bySeat.get(seat))", Integer.toString(bySeat.get(seat)), row.bid());
                assertEquals("row.hand() must equal Integer.toString(h)", Integer.toString(h), row.hand());
                assertEquals("row.points() must equal points > 0 ? \"+\" + points : Integer.toString(points)", points > 0 ? "+" + points : Integer.toString(points), row.points());
                assertEquals("row.total() must equal Integer.toString(total)", Integer.toString(total), row.total());
            }
        }
    }

    private static GameSession resume(GameSession game) throws Exception {
        return GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());
    }

    private static String readVerified(Path run, String name, JSONObject hashes) throws Exception {
        byte[] bytes = Files.readAllBytes(run.resolve(name));
        assertEquals(name, hashes.getString(name), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}

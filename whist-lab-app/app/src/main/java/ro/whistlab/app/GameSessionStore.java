package ro.whistlab.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ro.whistlab.core.AiDifficulty;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameConfig;
import ro.whistlab.core.GameMode;
import ro.whistlab.core.GamePhase;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.PlayedCard;
import ro.whistlab.core.ScoreHistory;
import ro.whistlab.core.Suit;
import ro.whistlab.core.Trump;

/** Versioned, synchronous checkpoint storage for resuming a local match after process death. */
final class GameSessionStore {
    private static final String PREFERENCES = "whist_lab_game";
    private static final String SAVE_KEY = "current_session_v1";
    private static final int VERSION = 5;

    private GameSessionStore() { }

    static void save(Context context, GameSession game) {
        try {
            JSONObject root = encode(game.saveState());
            boolean stored = preferences(context).edit().putString(SAVE_KEY, root.toString()).commit();
            if (!stored) throw new IllegalStateException("Android could not save the current game");
        } catch (JSONException failure) {
            throw new IllegalStateException("Could not encode the current game", failure);
        }
    }

    static GameSession load(Context context) {
        String encoded = preferences(context).getString(SAVE_KEY, null);
        if (encoded == null || encoded.isBlank()) return null;
        return fromJson(encoded);
    }

    static GameSession fromJson(String encoded) {
        try {
            JSONObject root = new JSONObject(encoded);
            if (root.getInt("version") < 1 || root.getInt("version") > VERSION) {
                throw new IllegalArgumentException("Unsupported saved-game version " + root.getInt("version"));
            }
            GameSession.SaveState state=decode(root);
            return root.getInt("version")>=2?GameSession.restoreComplete(state):GameSession.restore(state);
        } catch (JSONException | IllegalArgumentException | ArithmeticException failure) {
            throw new IllegalStateException("Saved game is invalid and cannot be resumed", failure);
        }
    }

    static void clear(Context context) {
        preferences(context).edit().remove(SAVE_KEY).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static JSONObject encode(GameSession.SaveState state) throws JSONException {
        JSONObject root = new JSONObject();
        int remaining=state.players().stream().mapToInt(p->p.hand().size()).sum();
        // A resumed v1 hand cannot invent its missing early history. Keep its legacy
        // marker until the next fresh deal. The score ledger is independent of this marker.
        root.put("version",state.history().size()==state.handSize()*state.config().playerCount()-remaining?VERSION:1);
        root.put("playerCount", state.config().playerCount());
        root.put("mode", state.config().mode().name());
        root.put("difficulty", state.config().aiDifficulty().name());
        root.put("seed", state.config().seed());
        root.put("streakBonuses", state.config().streakBonuses());
        root.put("streakBonusPoints", state.config().streakBonusPoints());
        root.put("phase", state.phase().name());
        root.put("handIndex", state.handIndex());
        root.put("handSize", state.handSize());
        root.put("dealerSeat", state.dealerSeat());
        root.put("biddingSeat", state.biddingSeat());
        root.put("currentSeat", state.currentSeat());
        root.put("bidsSubmitted", state.bidsSubmitted());
        root.put("bidTotal", state.bidTotal());
        root.put("trump", state.trump().name());
        root.put("lastTrickWinnerSeat", state.lastTrickWinnerSeat());

        JSONArray players = new JSONArray();
        for (GameSession.PlayerState player : state.players()) {
            JSONObject item = new JSONObject();
            item.put("seat", player.seat());
            item.put("name", player.name());
            item.put("human", player.human());
            item.put("hand", encodeCards(player.hand()));
            item.put("bid", player.bid() == null ? JSONObject.NULL : player.bid());
            item.put("tricksWon", player.tricksWon());
            item.put("totalScore", player.totalScore());
            item.put("streak", player.streak());
            item.put("streakPoints", player.streakPoints());
            players.put(item);
        }
        root.put("players", players);
        ScoreHistory ledger = state.scoreHistory();
        JSONObject scores = new JSONObject();
        scores.put("priorHands", ledger.priorHands());
        scores.put("opening", new JSONArray(ledger.openingTotals()));
        JSONArray rows = new JSONArray();
        for (List<Integer> totals : ledger.closingTotals()) rows.put(new JSONArray(totals));
        scores.put("hands", rows);
        JSONArray bids = new JSONArray();
        for (List<Integer> contracts : ledger.recordedBids()) bids.put(new JSONArray(contracts));
        scores.put("bids", bids);
        root.put("scores", scores);
        root.put("currentTrick", encodeTrick(state.currentTrick()));
        root.put("lastTrick", encodeTrick(state.lastTrick()));
        root.put("history", encodeTrick(state.history()));
        root.put("turnedCard", state.turnedCard() == null ? JSONObject.NULL
                : encodeCards(java.util.Collections.singletonList(state.turnedCard())).getJSONObject(0));
        return root;
    }

    private static GameSession.SaveState decode(JSONObject root) throws JSONException {
        GameConfig config = new GameConfig(root.getInt("playerCount"),
                GameMode.valueOf(root.getString("mode")),
                AiDifficulty.fromStoredName(root.getString("difficulty")), root.getLong("seed"),root.optBoolean("streakBonuses",false),root.optInt("streakBonusPoints",5));
        JSONArray encodedPlayers = root.getJSONArray("players");
        List<GameSession.PlayerState> players = new ArrayList<>(encodedPlayers.length());
        for (int index = 0; index < encodedPlayers.length(); index++) {
            JSONObject player = encodedPlayers.getJSONObject(index);
            Object encodedBid = player.get("bid");
            Integer bid = encodedBid == JSONObject.NULL ? null : ((Number) encodedBid).intValue();
            players.add(new GameSession.PlayerState(player.getInt("seat"), player.getString("name"),
                    player.getBoolean("human"), decodeCards(player.getJSONArray("hand")), bid,
                    player.getInt("tricksWon"), player.getInt("totalScore"),player.optInt("streak",0),player.optInt("streakPoints",0)));
        }
        List<PlayedCard> current = decodeTrick(root.getJSONArray("currentTrick"));
        List<PlayedCard> last = decodeTrick(root.getJSONArray("lastTrick"));
        List<PlayedCard> history = new ArrayList<>(last); history.addAll(current);
        if (root.has("history")) history = decodeTrick(root.getJSONArray("history"));
        Card turned = null;
        if (root.has("turnedCard") && !root.isNull("turnedCard")) {
            JSONObject item = root.getJSONObject("turnedCard");
            turned = new Card(Suit.valueOf(item.getString("suit")), item.getInt("rank"));
        }
        return new GameSession.SaveState(config, GamePhase.valueOf(root.getString("phase")),
                root.getInt("handIndex"), root.getInt("handSize"), root.getInt("dealerSeat"),
                root.getInt("biddingSeat"), root.getInt("currentSeat"), root.getInt("bidsSubmitted"),
                root.getInt("bidTotal"), Trump.valueOf(root.getString("trump")),
                root.getInt("lastTrickWinnerSeat"), players,
                current, last, history, turned, decodeScoreHistory(root, players));
    }

    private static ScoreHistory decodeScoreHistory(JSONObject root, List<GameSession.PlayerState> players) throws JSONException {
        if (root.getInt("version") < 4 && !root.has("scores")) return null;
        JSONObject scores = root.getJSONObject("scores");
        JSONArray hands = scores.getJSONArray("hands");
        List<List<Integer>> rows = new ArrayList<>(hands.length());
        for (int i = 0; i < hands.length(); i++) rows.add(scoreVector(hands.getJSONArray(i)));
        List<List<Integer>> bids = new ArrayList<>();
        boolean settled = GamePhase.SCORE.name().equals(root.getString("phase"));
        if (root.getInt("version") >= 5 || scores.has("bids")) {
            JSONArray contracts = scores.getJSONArray("bids");
            for (int i = 0; i < contracts.length(); i++) bids.add(scoreVector(contracts.getJSONArray(i)));
            if (settled && bids.isEmpty()) throw new JSONException("Missing completed hand bids");
        } else if (settled) {
            bids.add(players.stream().map(GameSession.PlayerState::bid).collect(java.util.stream.Collectors.toList()));
        }
        return new ScoreHistory(scoreInt(scores.get("priorHands")), scoreVector(scores.getJSONArray("opening")), rows, bids);
    }

    private static List<Integer> scoreVector(JSONArray values) throws JSONException {
        List<Integer> row = new ArrayList<>(values.length());
        for (int i = 0; i < values.length(); i++) row.add(scoreInt(values.get(i)));
        return row;
    }

    private static int scoreInt(Object value) throws JSONException {
        if (!(value instanceof Integer)) throw new JSONException("Score history requires integer values");
        return (Integer) value;
    }

    private static JSONArray encodeCards(List<Card> cards) throws JSONException {
        JSONArray encoded = new JSONArray();
        for (Card card : cards) {
            JSONObject item = new JSONObject();
            item.put("suit", card.suit().name());
            item.put("rank", card.rank());
            encoded.put(item);
        }
        return encoded;
    }

    private static List<Card> decodeCards(JSONArray encoded) throws JSONException {
        List<Card> cards = new ArrayList<>(encoded.length());
        for (int index = 0; index < encoded.length(); index++) {
            JSONObject item = encoded.getJSONObject(index);
            cards.add(new Card(Suit.valueOf(item.getString("suit")), item.getInt("rank")));
        }
        return cards;
    }

    private static JSONArray encodeTrick(List<PlayedCard> trick) throws JSONException {
        JSONArray encoded = new JSONArray();
        for (PlayedCard play : trick) {
            JSONObject item = new JSONObject();
            item.put("seat", play.seat());
            item.put("suit", play.card().suit().name());
            item.put("rank", play.card().rank());
            encoded.put(item);
        }
        return encoded;
    }

    private static List<PlayedCard> decodeTrick(JSONArray encoded) throws JSONException {
        List<PlayedCard> trick = new ArrayList<>(encoded.length());
        for (int index = 0; index < encoded.length(); index++) {
            JSONObject item = encoded.getJSONObject(index);
            trick.add(new PlayedCard(item.getInt("seat"),
                    new Card(Suit.valueOf(item.getString("suit")), item.getInt("rank"))));
        }
        return trick;
    }
}

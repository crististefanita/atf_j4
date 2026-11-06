package ro.whistlab.app.persistence;

import static org.junit.Assert.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.session.GameSession;

public class GameSessionNumbersComponentTest {
    @Test public void validCheckpointsRetainTheExactSeedAndAcceptedBid() throws Exception {
        for (long seed : new long[] {0, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var game = GameSession.newGame(GameConfig.defaults(seed));
            game.submitBid(game.snapshot().biddingSeat(), 0);
            var restored = GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());
            assertEquals(game.saveState(), restored.saveState());
        }
    }

    @Test public void structuralIntegersRejectFractionsOverflowStringsAndNullBeforeDomainValidation() throws Exception {
        for (String field : new String[] {"version", "playerCount", "handIndex", "handSize", "dealerSeat",
                "biddingSeat", "currentSeat", "bidsSubmitted", "bidTotal", "lastTrickWinnerSeat", "streakBonusPoints", "streakPenaltyPoints"}) {
            for (Object invalid : new Object[] {0.75, 4294967296L, "0", JSONObject.NULL}) {
                JSONObject json = fixture();
                json.put(field, invalid);
                var failure = assertThrows(field + " must remain exact", IllegalStateException.class,
                        () -> GameSessionStore.fromJson(json.toString()));
                assertTrue(field + ": " + failure, failure.getCause() instanceof JSONException);
                assertTrue(field, failure.getCause().getMessage().contains(field));
            }
        }
    }

    @Test public void playerAndCardNumbersRejectLossyCoercionAndPreserveAHealthyNeighbour() throws Exception {
        for (String field : new String[] {"seat", "bid", "tricksWon", "totalScore", "streak", "streakPoints"}) {
            for (Object invalid : new Object[] {0.75, 4294967296L, "0", true}) {
                JSONObject json = fixture();
                json.getJSONArray("players").getJSONObject(0).put(field, invalid);
                assertThrows(field, IllegalStateException.class, () -> GameSessionStore.fromJson(json.toString()));
            }
        }
        JSONObject card = fixture();
        card.getJSONArray("players").getJSONObject(0).getJSONArray("hand").getJSONObject(0).put("rank", 2.75);
        assertThrows(IllegalStateException.class, () -> GameSessionStore.fromJson(card.toString()));
        assertNotNull(GameSessionStore.fromJson(fixture().toString()));
    }

    @Test public void seedRejectsFractionsAndOutOfRangeNumbers() throws Exception {
        for (String invalid : new String[] {"0.75", "9223372036854775808", "\"17\"", "null"}) {
            String original = fixture().toString();
            String json = original.replaceFirst("\"seed\":31", "\"seed\":" + invalid);
            assertNotEquals(original, json);
            assertThrows(IllegalStateException.class, () -> GameSessionStore.fromJson(json));
        }
    }

    private static JSONObject fixture() throws Exception {
        return GameSessionStore.encode(GameSession.newGame(GameConfig.defaults(31)).saveState());
    }
}

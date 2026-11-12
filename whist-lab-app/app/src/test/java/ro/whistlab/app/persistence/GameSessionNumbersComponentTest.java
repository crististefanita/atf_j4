package ro.whistlab.app.persistence;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import atf.gradle.audit.GradleInputs;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import ro.whistlab.app.screens.MatchSetup;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

public class GameSessionNumbersComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void currentCheckpointsRequireAnExplicitBooleanRule() throws Exception {
        var json = fixture();
        atf.gradle.audit.GradleInputs.fixture("Current save with the rule disabled", "checkpoint", json.toString());
        assertFalse("The saved false value is respected", GameSessionStore.fromJson(json.toString()).config().allowZeroGame());
        json.remove("allowZeroGame");
        assertThrows("A current save may not silently lose its rule profile", IllegalStateException.class,
                () -> GameSessionStore.fromJson(json.toString()));
        for (Object invalid : new Object[]{"false", 0, JSONObject.NULL}) {
            json.put("allowZeroGame", invalid);
            assertThrows("Strings, numbers and null are invalid rule flags", IllegalStateException.class,
                    () -> GameSessionStore.fromJson(json.toString()));
        }
    }

    @Test public void validCheckpointsRetainTheExactSeedAndAcceptedBid() throws Exception {
        for (long seed : new long[] {0, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(seed)));
            game.submitBid(game.snapshot().biddingSeat(), 0);
            var restored = GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "restored", restored, "seed", seed);
            assertEquals("restored.saveState() must equal game.saveState()", game.saveState(), restored.saveState());
        }
    }

    @Test public void romanianSetupOptionsRoundTripThroughTheCurrentCheckpoint() throws Exception {
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            verifySeriesCheckpoint(mode, true, 5);
            verifySeriesCheckpoint(mode, true, 10);
            verifySeriesCheckpoint(mode, false, 10);
        }
    }

    private static void verifySeriesCheckpoint(GameMode mode, boolean enabled, int points) throws Exception {
        var setup = new MatchSetup();
        setup.mode(mode);
        setup.streaks(enabled);
        if (points == 5) {
            setup.streakBonus(5);
        }
        var game = GameSession.newGame(setup.config(31));
        game.submitBid(game.snapshot().biddingSeat(), 0);
        var saved = game.saveState();
        var json = GameSessionStore.encode(saved);
        GradleInputs.fixture("Real setup and serialized checkpoint", "variant", mode,
                "enabled", enabled, "points", points, "checkpoint", json.toString());

        assertEquals("New complete checkpoints use the current schema", 8, json.getInt("version"));
        assertEquals("The checkbox controls series scoring", enabled, json.getBoolean("streakBonuses"));
        assertEquals("The selected bonus is saved", points, json.getInt("streakBonusPoints"));
        assertEquals("The penalty has the same magnitude", points, json.getInt("streakPenaltyPoints"));
        assertEquals("The rules and every saved field survive decoding", saved,
                GameSessionStore.fromJson(json.toString()).saveState());

        for (String field : List.of("streakBonusPoints", "streakPenaltyPoints")) {
            var invalid = new JSONObject(json.toString()).put(field, 7);
            GradleInputs.fixture("Unsupported integer series magnitude", "field", field,
                    "invalid", invalid.toString());
            assertThrows("The codec rejects an unsupported magnitude in " + field, IllegalStateException.class,
                    () -> GameSessionStore.fromJson(invalid.toString()));
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

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "field", field, "invalid", invalid, "json", json);
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

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "field", field, "invalid", invalid, "json", json);
                assertThrows(field, IllegalStateException.class, () -> GameSessionStore.fromJson(json.toString()));
            }
        }
        JSONObject card = fixture();
        card.getJSONArray("players").getJSONObject(0).getJSONArray("hand").getJSONObject(0).put("rank", 2.75);
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> GameSessionStore.fromJson(card.toString()));
        assertNotNull("GameSessionStore.fromJson(fixture().toString()) is available", GameSessionStore.fromJson(fixture().toString()));
    }

    @Test public void seedRejectsFractionsAndOutOfRangeNumbers() throws Exception {
        for (String invalid : new String[] {"0.75", "9223372036854775808", "\"17\"", "null"}) {
            String original = fixture().toString();
            String json = original.replaceFirst("\"seed\":31", "\"seed\":" + invalid);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "invalid", invalid, "json", json, "original", original);
            assertNotEquals("json must differ from original", original, json);
            assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> GameSessionStore.fromJson(json));
        }
    }

    private static JSONObject fixture() throws Exception {
        return GameSessionStore.encode(GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31))).saveState());
    }
}

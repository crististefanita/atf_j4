package ro.whistlab.ios;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public final class WhistGameBridgeComponentTest extends GradleAuditedTest {
    private static final Pattern LEGAL_CARD = Pattern.compile(
            "\"legalCards\":\\[\\{\"suit\":\"([A-Z]+)\",\"rank\":(\\d+)");

    @Test
    public void publicStateKeepsOpponentHandsHiddenAndOffersOnlyHumanActions() {
        GradleInputs.fixture("Public state projection", "player count", 4,
                "variant", "ROMANIAN_WHIST", "difficulty", "NORMAL", "seed", 20261107L);
        JSONObject state = new JSONObject(GradleInputs.created("serialized game state", startGame()));
        JSONArray players = state.getJSONArray("players");

        assertEquals("BIDDING", state.getString("phase"));
        assertEquals(0, state.getInt("biddingSeat"));
        assertTrue(state.getJSONArray("allowedBids").length() > 0);
        assertEquals(state.getInt("handSize"), state.getJSONArray("hand").length());
        assertEquals(4, players.length());
        for (int index = 0; index < players.length(); index++) {
            assertFalse(players.getJSONObject(index).has("hand"));
        }
        assertFalse(state.has("cardBack"));
    }

    @Test
    public void checkpointRoundTripPreservesAnInProgressHumanTurn() {
        GradleInputs.fixture("Checkpoint round trip", "player count", 4,
                "variant", "ROMANIAN_WHIST", "seed", 20261107L);
        String initial = GradleInputs.created("initial bidding state", startGame());
        String bidding = WhistGameBridge.submitBid(0);
        GradleInputs.created("state after bidding", bidding);
        Matcher card = LEGAL_CARD.matcher(bidding);
        assertTrue(bidding, card.find());

        String afterPlay = WhistGameBridge.playCard(card.group(1), Integer.parseInt(card.group(2)));
        String checkpoint = WhistGameBridge.saveCheckpoint();
        GradleInputs.created("persisted checkpoint", checkpoint);
        String restored = WhistGameBridge.restoreGame(checkpoint);

        assertFalse(initial.isEmpty());
        assertFalse(checkpoint.isEmpty());
        assertEquals(afterPlay, restored);
    }

    @Test
    public void invalidCheckpointReturnsBridgeErrorInsteadOfThrowingAcrossObjectiveC() {
        GradleInputs.fixture("Invalid checkpoint", "checkpoint", "not-base64");
        JSONObject response = new JSONObject(WhistGameBridge.restoreGame("not-base64"));

        assertTrue(response.has("error"));
        assertTrue(WhistGameBridge.currentState().isEmpty());
    }

    private static String startGame() {
        return WhistGameBridge.startGame(4, "ROMANIAN_WHIST", "NORMAL", "Cristi",
                true, 10, true, 20261107L);
    }
}

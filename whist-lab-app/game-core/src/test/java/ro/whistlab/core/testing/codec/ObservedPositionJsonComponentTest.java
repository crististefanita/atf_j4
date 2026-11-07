package ro.whistlab.core.testing.codec;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

public class ObservedPositionJsonComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test public void absentLegacyPenaltyKeepsTheHistoricalFiveWithoutRewritingTheCapture() throws Exception {
        ObjectNode captured = capture();
        captured.remove("streakPenaltyPoints");
        String before = captured.toString();
        var view = ObservedPositionJson.view(json, captured);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "captured", captured, "view", view);
        assertEquals("view.streakBonusPoints() must equal 10", 10, view.streakBonusPoints());
        assertEquals("view.streakPenaltyPoints() must equal 5", 5, view.streakPenaltyPoints());
        assertEquals("captured.toString() must equal before", before, captured.toString());
    }

    @Test public void anExplicitCurrentPenaltyRemainsSymmetric() throws Exception {
        var view = ObservedPositionJson.view(json, capture());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "view", view);
        assertEquals("view.streakBonusPoints() must equal 10", 10, view.streakBonusPoints());
        assertEquals("view.streakPenaltyPoints() must equal 10", 10, view.streakPenaltyPoints());
    }

    @Test public void explicitInvalidValuesAreRefusedRatherThanGivenTheLegacyDefault() {
        for (String value : new String[]{"null", "0", "7", "10.0", "\"5\"", "{}", "[]"}) {
            ObjectNode captured = capture();
            try { captured.set("streakPenaltyPoints", json.readTree(value)); }
            catch (Exception unexpected) { throw new AssertionError(unexpected); }

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "captured", captured, "value", value);
            assertThrows(value, IllegalArgumentException.class, () -> ObservedPositionJson.view(json, captured));
        }
    }

    private ObjectNode capture() {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 317, true, 10);
        return json.valueToTree(MonteCarloAi.observe(GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).snapshot(), 0));
    }
}

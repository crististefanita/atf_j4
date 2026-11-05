package ro.whistlab.core.testing.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ro.whistlab.core.ai.engine.MonteCarloAi;

/** Decodes captured public views without rewriting the original archive or changing its score profile. */
public final class ObservedPositionJson {
    private ObservedPositionJson() { }

    public static MonteCarloAi.View view(ObjectMapper mapper, JsonNode captured) throws JsonProcessingException {
        if (!captured.isObject()) throw new IllegalArgumentException("Captured observation must be an object");
        ObjectNode normalized = captured.deepCopy();
        if (!normalized.has("streakPenaltyPoints")) {
            normalized.put("streakPenaltyPoints", 5);
        } else {
            JsonNode penalty = normalized.get("streakPenaltyPoints");
            if (!penalty.isIntegralNumber() || !penalty.canConvertToInt()
                    || penalty.intValue() != 5 && penalty.intValue() != 10) {
                throw new IllegalArgumentException("Captured series penalty must be 5 or 10");
            }
        }
        return mapper.treeToValue(normalized, MonteCarloAi.View.class);
    }
}

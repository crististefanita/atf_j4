import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ro.cardtable.ui.score.HistoryAppearance;

/** Emits the portable policy from the actual runtime artifact or its producer source. */
public final class MenuHistoryProbe {
    public static void main(String[] args) {
        System.out.println(json(Map.of("regular", HistoryAppearance.REGULAR_GROUPS.cssTokens(),
                "legacy", HistoryAppearance.LEGACY.cssTokens())));
    }
    private static String json(Object value) {
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .map(e -> json(e.getKey()) + ":" + json(e.getValue())).collect(Collectors.joining(",", "{", "}"));
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

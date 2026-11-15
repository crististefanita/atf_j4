package atf.tests.testchange;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public record TestChangeProofChecks(List<String> positive, List<String> negative) {
    private static final Pattern SELECTOR = Pattern.compile(
            "(?:[A-Za-z_$][\\w$]*\\.)+[A-Za-z_$][\\w$]*#[A-Za-z_$][\\w$]*");

    static TestChangeProofChecks parse(JsonNode node) {
        if (node.isMissingNode()) return null;
        if (!node.isObject() || node.size() != 2) throw new IllegalArgumentException("checks requires positive and negative");
        var positive = selectors(node.path("positive"), "positive");
        var negative = selectors(node.path("negative"), "negative");
        if (negative.stream().anyMatch(positive::contains))
            throw new IllegalArgumentException("positive and negative checks must be distinct");
        return new TestChangeProofChecks(positive, negative);
    }

    private static List<String> selectors(JsonNode values, String label) {
        if (!values.isArray() || values.isEmpty()) throw new IllegalArgumentException("checks." + label + " must be nonempty");
        var unique = new LinkedHashSet<String>();
        for (JsonNode item : values) {
            if (!item.isTextual() || !SELECTOR.matcher(item.textValue()).matches() || !unique.add(item.textValue()))
                throw new IllegalArgumentException("invalid or duplicate checks." + label + " selector " + item);
        }
        return List.copyOf(unique);
    }
}

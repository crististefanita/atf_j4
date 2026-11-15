package atf.tests.testchange;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** The same schema governs editorial masking and Git review eligibility. */
public final class TestChangeControls {
    public static final String FEEDBACK = "docs/feedback/";
    public static final String BASELINE = "docs/governance/ATF_TestChangesBaseline.md";

    private static final Pattern FENCE = Pattern.compile("(?ms)^```yaml[ \\t]*\\R(.*?)^```[ \\t]*(?=\\R|\\z)");
    private static final Pattern CONTROL_KEY = Pattern.compile("(?m)^\\s*testChanges\\s*:");
    private static final Pattern PROOF = Pattern.compile("(?i)^(?:mvn(?:w(?:\\.cmd)?)?|(?:\\./)?gradlew(?:\\.bat)?)\\s+.*\\b\\w*(?:test|verify)\\w*\\b");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private TestChangeControls() { }

    public static String editorialProse(String markdown) {
        return FENCE.matcher(markdown).replaceAll(match -> {
            try {
                JsonNode block = block(match.group(1));
                for (JsonNode row : block.path("testChanges"))
                    ((ObjectNode) row).put("id", "<identificator-tehnic>");
                return java.util.regex.Matcher.quoteReplacement("```yaml\n" + block + "\n```");
            } catch (IOException invalid) {
                return java.util.regex.Matcher.quoteReplacement(match.group());
            }
        });
    }

    static List<Control> parse(String markdown, String source) throws IOException {
        var result = new ArrayList<Control>();
        var blocks = FENCE.matcher(markdown);
        while (blocks.find()) {
            String yaml = blocks.group(1);
            if (!CONTROL_KEY.matcher(yaml).find()) continue;
            try {
                for (JsonNode row : block(yaml).path("testChanges")) {
                    var entry = control(row);
                    if (entry.status().equals("ACTIVE") && !source.startsWith(FEEDBACK)
                            || entry.status().equals("VERIFIED") && !source.equals(BASELINE))
                        throw new IllegalArgumentException(entry.id() + " belongs in the document for " + entry.status());
                    result.add(entry);
                }
            } catch (IOException | IllegalArgumentException invalid) {
                throw new IOException("TEST-CHANGE-SCHEMA: " + source + ": " + invalid.getMessage(), invalid);
            }
        }
        if (CONTROL_KEY.matcher(FENCE.matcher(markdown).replaceAll("")).find())
            throw new IOException("TEST-CHANGE-SCHEMA: " + source + ": use a closed yaml fence");
        return List.copyOf(result);
    }

    private static JsonNode block(String yaml) throws IOException {
        try {
            JsonNode root = YAML.readTree(yaml);
            if (root == null || !root.isObject() || root.size() != 1
                    || !root.path("testChanges").isArray() || root.path("testChanges").isEmpty())
                throw new IllegalArgumentException("testChanges must be the only key with a nonempty list");
            for (JsonNode row : root.path("testChanges")) control(row);
            return root;
        } catch (IllegalArgumentException | DateTimeException invalid) {
            throw new IOException(invalid.getMessage(), invalid);
        }
    }

    private static Control control(JsonNode row) {
        if (!row.isObject() || row.size() != (row.has("allowDelete") ? 8 : 7))
            throw new IllegalArgumentException("invalid control fields");
        String id = text(row, "id");
        String status = text(row, "status");
        String base = text(row, "base");
        String reason = text(row, "reason");
        String proof = text(row, "proof");
        if (!id.matches("[A-Z][A-Z0-9-]{3,79}") || !Set.of("ACTIVE", "VERIFIED", "CLOSED", "DRAFT").contains(status)
                || !commitId(base) || reason.length() < 20 || !PROOF.matcher(proof).find())
            throw new IllegalArgumentException("invalid identity, status, base, reason or proof for " + id);
        var paths = new LinkedHashSet<String>();
        JsonNode listed = row.path("paths");
        if (!listed.isArray() || listed.isEmpty() || row.has("allowDelete") && !row.path("allowDelete").isBoolean())
            throw new IllegalArgumentException("invalid paths or allowDelete for " + id);
        for (JsonNode value : listed) {
            String path = value.asText("");
            if (!value.isTextual() || !exactPath(path) || !paths.add(path))
                throw new IllegalArgumentException("invalid or duplicate path for " + id + ": " + path);
        }
        return new Control(id, status, base, LocalDate.parse(text(row, "expires")), Set.copyOf(paths),
                row.path("allowDelete").booleanValue());
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isTextual() || value.textValue().isBlank() || !value.textValue().equals(value.textValue().strip()))
            throw new IllegalArgumentException("expected nonempty text for " + field);
        return value.textValue();
    }

    static boolean commitId(String text) {
        return text.matches("[0-9a-f]{40}|[0-9a-f]{64}");
    }

    static boolean exactPath(String path) {
        if (path.isBlank() || path.matches("(?s).*[\\p{Cntrl}\\\\:*?\\[\\]{}].*")) return false;
        for (String part : path.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..") || !part.equals(part.strip())) return false;
        }
        return true;
    }

    record Control(String id, String status, String base, LocalDate expires, Set<String> paths, boolean allowDelete) {
        boolean authorizesReview() { return status.equals("ACTIVE") || status.equals("VERIFIED"); }
    }
}

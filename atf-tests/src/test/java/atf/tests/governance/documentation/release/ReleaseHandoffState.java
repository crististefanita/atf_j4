package atf.tests.governance.documentation.release;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;

/** A local candidate cannot borrow the identity of a published release. */
final class ReleaseHandoffState {
    private ReleaseHandoffState() { }

    static boolean prepared(JsonNode validation, boolean hasPublishedRecord) {
        String state = validation.path("publication").path("state").asText();
        if (state.isEmpty() && hasPublishedRecord) return false;
        if (state.equals("PUBLISHED")) {
            require(hasPublishedRecord, "STATE");
            require(validation.path("source").path("sourceCommit").asText().matches("[0-9a-f]{40}"), "COMMIT");
            return false;
        }
        require(state.equals("PREPARED") && !hasPublishedRecord, "STATE");
        var provenance = validation.path("source");
        require(provenance.has("sourceCommit") && provenance.path("sourceCommit").isNull(), "COMMIT");
        require(provenance.path("workingTreeFingerprint").asText().matches("[0-9a-f]{64}"), "FINGERPRINT");
        return true;
    }

    static String evidence(JsonNode presentation, String repository, String version, boolean prepared) {
        require(version.equals(presentation.path("version").asText()), "VERSION");
        String base = repository + "/blob/";
        String suffix = "/release/" + version + "/reports/README.md";
        if (prepared) {
            require(!presentation.has("evidenceUrl"), "PUBLIC-CLAIM");
            String template = presentation.path("evidenceUrlTemplate").asText();
            require(template.equals(base + "{evidenceCommit}" + suffix), "TEMPLATE");
            return template;
        }
        require(!presentation.has("evidenceUrlTemplate"), "UNBOUND");
        String url = presentation.path("evidenceUrl").asText();
        require(url.matches(Pattern.quote(base) + "[0-9a-f]{40}/release/"
                + Pattern.quote(version) + "/reports/README\\.md"), "EVIDENCE");
        return url;
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalArgumentException("RELEASE-HANDOFF-" + code);
    }
}

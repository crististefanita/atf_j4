package atf.tests.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/** Selects the published artifact for acceptance or the explicitly announced candidate for README governance. */
public final class ReleaseDownloadSelection {
    public static ApkDownloadTarget select(String scope, String repository, JsonNode index, JsonNode manifest) throws IOException {
        require(scope.equals("current") || scope.equals("advertised"), "SCOPE");
        JsonNode release = manifest.path("release");String version = text(release, "versionName");
        String expected = scope.equals("current") ? text(index, "currentVersion")
                : index.path("candidateVersion").isTextual() && !index.path("candidateVersion").asText().isBlank()
                        ? index.path("candidateVersion").asText() : text(index, "currentVersion");
        require(version.equals(expected), "CURRENT-IDENTITY");
        return new ApkDownloadTarget(repository, version, number(release, "apkBytes"), text(release, "apkSha256"));
    }
    private static long number(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);require(value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0, "NUMBER-" + key);
        return value.longValue();
    }
    private static String text(JsonNode node, String key) throws IOException {
        JsonNode value = node.path(key);require(value.isTextual() && !value.textValue().isBlank(), "FIELD-" + key);return value.textValue();
    }
    private static void require(boolean valid, String code) throws IOException { if (!valid) throw new IOException("RELEASE-DOWNLOAD-" + code); }
    private ReleaseDownloadSelection() { }
}

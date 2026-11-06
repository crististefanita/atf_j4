package atf.tests.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/** A published older version never supplies the expected identity of a current local candidate. */
public final class ReleaseDownloadSelection {
    public static ApkDownloadTarget select(String scope, String repository, JsonNode index, JsonNode manifest) throws IOException {
        require(scope.equals("current"), "SCOPE");
        JsonNode release = manifest.path("release");String version = text(release, "versionName");
        require(version.equals(text(index, "currentVersion")), "CURRENT-IDENTITY");
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

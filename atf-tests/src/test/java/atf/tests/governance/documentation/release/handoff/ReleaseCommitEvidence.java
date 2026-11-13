package atf.tests.governance.documentation.release.handoff;

import atf.android.process.CommandProcess;
import atf.tests.distribution.ReleasePaths;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Reads immutable Git blobs, with no checkout, filters or network access. */
public final class ReleaseCommitEvidence {
    public static final String GIT_ROOT = "atf.governance.gitRoot";
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Pattern LINK = Pattern.compile("(?<!!)\\[[^]\\r\\n]+]\\(([^)\\s]+)\\)");

    private ReleaseCommitEvidence() { }

    public static String verify(Path root, String version, String commit, JsonNode published) throws Exception {
        require(commit.matches("[0-9a-f]{40}") && version.matches("[0-9]+\\.[0-9]+\\.[0-9]+"), "IDENTITY", commit);
        require(text(git(root, "cat-file", "-t", commit)).strip().equals("commit"), "COMMIT", commit);
        String prefix = "release/" + version + "/";
        String index = text(blob(root, commit, prefix + "reports/README.md"));
        JsonNode pinned = JSON.readTree(blob(root, commit, prefix + "release-validation.json"));

        require(sameField(pinned, published, "/release/versionName")
                && version.equals(pinned.at("/release/versionName").asText()), "VERSION", commit);
        require(sameField(pinned, published, "/release/apkSha256")
                && sameField(pinned, published, "/release/apkBytes"), "APK", commit);
        String source = pinned.at("/source/sourceCommit").asText();
        require(source.matches("[0-9a-f]{40}") && sameField(pinned, published, "/source/sourceCommit"), "SOURCE", commit);
        git(root, "merge-base", "--is-ancestor", source, commit);

        String report = ReleasePaths.relative(pinned.at("/evidenceRetention/canonicalReport").asText());
        require(report.startsWith("reports/") && report.endsWith(".html"), "REPORT-PATH", report);
        require(sameField(pinned, published, "/evidenceRetention/canonicalReport"), "REPORT-PATH", commit);
        String link = report.substring("reports/".length());
        require(LINK.matcher(index).results().anyMatch(entry -> entry.group(1).equals(link)), "INDEX-LINK", link);
        String expected = pinned.at("/evidenceRetention/canonicalReportSha256").asText();
        require(expected.matches("[0-9a-f]{64}"), "HASH-FORMAT", commit);
        byte[] bytes = blob(root, commit, prefix + report);
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        require(actual.equals(expected), "BLOB-HASH", commit + " expected=" + expected + " actual=" + actual);
        String retained = published.at("/evidenceRetention/canonicalReportSha256").asText();
        require(actual.equals(retained), "RETAINED-HASH", commit + " expected=" + retained + " actual=" + actual);
        return actual;
    }

    private static boolean sameField(JsonNode pinned, JsonNode published, String pointer) {
        JsonNode value = pinned.at(pointer);
        return !value.isMissingNode() && !value.isNull() && value.equals(published.at(pointer));
    }

    private static byte[] blob(Path root, String commit, String path) throws Exception {
        String entry = text(git(root, "ls-tree", "-z", commit, "--", path));
        require(entry.matches("100(?:644|755) blob [0-9a-f]{40}\\t" + Pattern.quote(path) + "\\x00"), "BLOB", commit + ":" + path);
        return git(root, "cat-file", "blob", commit + ":" + path);
    }

    private static byte[] git(Path root, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git", "--no-replace-objects", "--no-lazy-fetch", "-C", root.toString()));
        command.addAll(List.of(arguments));
        var output = CommandProcess.execute(command, Map.of("GIT_TERMINAL_PROMPT", "0"), Duration.ofSeconds(30), false);
        require(output.exitCode() == 0, "GIT", List.of(arguments) + " " + output.stderr().strip());
        return output.stdout();
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void require(boolean valid, String code, String context) throws IOException {
        if (!valid) throw new IOException("RELEASE-COMMIT-" + code + ": " + context);
    }
}

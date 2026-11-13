package atf.tests.governance.documentation.release.handoff;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@AtfComponentTest
class ReleaseCommitEvidenceComponentTest {
    @Test void gitLinkBindsTheIndexManifestAndExactReportBytes(@TempDir Path root) throws Exception {
        git(root, "init", "-q");
        String source = commit(root, "source");
        Path dossier = Files.createDirectories(root.resolve("release/1.2.3/reports")).getParent();
        Path report = dossier.resolve("reports/extent.html");
        Path index = dossier.resolve("reports/README.md");
        Path manifest = dossier.resolve("release-validation.json");
        Files.writeString(index, "[Extent](extent.html)\n");
        var published = new ObjectMapper().createObjectNode();
        published.putObject("release").put("versionName", "1.2.3").put("apkSha256", "a".repeat(64)).put("apkBytes", 123);
        published.putObject("source").put("sourceCommit", source);
        var retained = published.putObject("evidenceRetention").put("canonicalReport", "reports/extent.html");
        Files.writeString(report, "<html>Final report</html>\r\n");
        String expected = hash(report);
        retained.put("canonicalReportSha256", expected);
        Files.writeString(manifest, published.toString());
        String correct = commit(root, "correct dossier");

        Files.writeString(report, "<html>Earlier report</html>\n");
        String inconsistent = commit(root, "changed report with old manifest");
        var earlier = published.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) earlier.path("evidenceRetention"))
                .put("canonicalReportSha256", hash(report));
        Files.writeString(manifest, earlier.toString());
        String otherReport = commit(root, "changed report with matching manifest");
        Files.writeString(index, "[Extent](other.html)\n");
        String wrongLink = commit(root, "wrong index link");

        AuditedAssertions.assertEquals(expected, ReleaseCommitEvidence.verify(root, "1.2.3", correct, published));
        for (var rejection : Map.of(source, "BLOB", inconsistent, "BLOB-HASH",
                otherReport, "RETAINED-HASH", wrongLink, "INDEX-LINK", "0".repeat(40), "GIT").entrySet()) {
            var failure = AuditedAssertions.assertThrows(IOException.class,
                    () -> ReleaseCommitEvidence.verify(root, "1.2.3", rejection.getKey(), published));
            AuditedAssertions.assertTrue(failure.getMessage().startsWith("RELEASE-COMMIT-" + rejection.getValue() + ":"),
                    failure.getMessage());
        }
    }

    private static String commit(Path root, String message) throws Exception {
        git(root, "add", "--all");
        git(root, "-c", "user.name=ATF test", "-c", "user.email=atf@example.invalid",
                "-c", "commit.gpgSign=false", "-c", "core.autocrlf=false",
                "commit", "--allow-empty", "--no-verify", "-qm", message);
        return git(root, "rev-parse", "HEAD").strip();
    }

    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static String git(Path root, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", root.toString(), "-c", "core.autocrlf=false"));
        command.addAll(List.of(arguments));
        var output = CommandProcess.execute(command, Map.of(), Duration.ofSeconds(30), false);
        if (output.exitCode() != 0) throw new IOException(output.stderr());
        return new String(output.stdout(), StandardCharsets.UTF_8);
    }
}

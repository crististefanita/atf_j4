package atf.tests.governance.documentation.release;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Release-store retention only; inline presence does not certify visibility or complete HTML autonomy. */
@AtfComponentTest
class ReleaseEvidenceRetentionGovernanceTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final atf.tests.reporting.GameReportAudit AUDIT = new atf.tests.reporting.GameReportAudit();
    @TempDir Path temporary;
    private Path attributeRepository;

    @Test void activeReleasesDoNotRetainExactCopiesOfImagesAlreadyEmbeddedInTheirHtml() throws Exception {
        for (Path folder : ReleaseArtifactCatalog.folders(GovernanceProject.locateProjectRoot()))
            AuditedAssertions.assertEquals(List.of(), duplicates(folder), "RELEASE-DUPLICATE-INLINE-IMAGE: " + folder);
    }

    @Test void everyDuplicatePathIsReportedIncludingNestedAndUppercaseImages() throws Exception {
        byte[] image = {1, 3, 5, 7};
        Files.writeString(temporary.resolve("report.html"), "<img src=\"" + inline(image) + "\">");
        Files.createDirectory(temporary.resolve("captures"));
        Files.write(temporary.resolve("captures/first.png"), image);
        Files.write(temporary.resolve("second.PNG"), image);
        AuditedAssertions.assertEquals(List.of(Path.of("captures/first.png"), Path.of("second.PNG")), duplicates(temporary));
        AuditedAssertions.assertTrue(Files.isRegularFile(temporary.resolve("captures/first.png")), "Inspection never deletes evidence");
    }

    @Test void filenamesHashesAndExternalLinksDoNotProveThatTheBytesWereEmbedded() throws Exception {
        Files.write(temporary.resolve("original.png"), new byte[]{1, 3, 5, 7});
        Files.writeString(temporary.resolve("report.html"),
                "<img src=\"original.png\"><p>original.png SHA-256=example</p>");
        AuditedAssertions.assertEquals(List.of(), duplicates(temporary));
    }

    @Test void differentBytesAndIndependentDiagnosticImagesRemainSeparate() throws Exception {
        Files.writeString(temporary.resolve("report.html"), "<img src=\"" + inline(new byte[]{1, 3, 5, 7}) + "\">");
        Files.write(temporary.resolve("browser-review.png"), new byte[]{1, 3, 5, 8});
        Files.write(temporary.resolve("prefix.png"), new byte[]{1, 3, 5});
        Files.writeString(temporary.resolve("diagnostic.log"), "Independent process diagnostic");
        AuditedAssertions.assertEquals(List.of(), duplicates(temporary));
    }

    @Test void releasePayloadDisablesGitTextConversionForEveryRegisteredFile() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        for (Path folder : ReleaseArtifactCatalog.folders(root)) {
            var manifest = new com.fasterxml.jackson.databind.ObjectMapper().readTree(folder.resolve("release-validation.json").toFile());
            var paths = new ArrayList<String>();
            paths.add(root.relativize(folder.resolve("release-validation.json")).toString().replace('\\', '/'));
            for (var file : manifest.required("integrity").required("files"))
                paths.add(root.relativize(folder.resolve(file.required("path").asText())).toString().replace('\\', '/'));
            AuditedAssertions.assertEquals(List.of(), textConversions(root, paths),
                    "RELEASE-GIT-BYTES: effective attributes must disable checkout and index text conversion");
        }
    }

    @Test void missingAutomaticForcedAndNestedTextRulesAreRejected() throws Exception {
        Path fixture = Files.createDirectory(temporary.resolve("repository with spaces"));
        AuditedAssertions.assertFalse(Files.exists(fixture.resolve(".git")), "TMP source copies do not require repository metadata");
        String evidence = "release/1.2.0/reports/report.html";
        for (String policy : List.of("", "release/** text=auto\n", "release/** text eol=crlf\n",
                "release/** -text\nrelease/1.2.0/reports/*.html text\n")) {
            Files.writeString(fixture.resolve(".gitattributes"), policy);
            AuditedAssertions.assertFalse(textConversions(fixture, List.of(evidence)).isEmpty(),
                    "Missing or overriding attributes must not qualify immutable evidence");
        }
        Files.writeString(fixture.resolve(".gitattributes"), "release/** -text\n");
        AuditedAssertions.assertEquals(List.of(), textConversions(fixture, List.of(evidence)),
                "The healthy neighboring policy preserves evidence bytes");
    }

    private List<String> textConversions(Path root, List<String> paths) throws Exception {
        if (attributeRepository == null) {
            attributeRepository = Files.createDirectory(temporary.resolve("attribute inspection"));
            git(attributeRepository, List.of("init", "--quiet"));
        }
        var arguments = new ArrayList<>(List.of("--git-dir=" + attributeRepository.resolve(".git"),
                "--work-tree=" + root, "-c", "core.autocrlf=true", "check-attr", "-z", "text", "--"));
        arguments.addAll(paths);
        String[] records = git(attributeRepository, arguments).split("\\x00");
        if (records.length != paths.size() * 3) throw new IllegalStateException("RELEASE-GIT-ATTR: incomplete Git output");
        var errors = new ArrayList<String>();
        for (int index = 0; index < paths.size(); index++) {
            if (!paths.get(index).equals(records[index * 3]) || !"text".equals(records[index * 3 + 1]))
                throw new IllegalStateException("RELEASE-GIT-ATTR: mismatched evidence identity");
            if (!"unset".equals(records[index * 3 + 2])) errors.add(paths.get(index) + ": " + records[index * 3 + 2]);
        }
        return List.copyOf(errors);
    }

    private String git(Path root, List<String> arguments) throws Exception {
        Path output = Files.createTempFile(temporary, "git-attributes-", ".out");
        var command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(arguments);
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("RELEASE-GIT-ATTR: Git did not finish within 30 seconds");
        }
        String result = Files.readString(output);
        if (process.exitValue() != 0) throw new IllegalStateException("RELEASE-GIT-ATTR: " + result);
        return result;
    }

    private static List<Path> duplicates(Path folder) throws Exception {
        List<Path> files;
        try (var stream = Files.walk(folder)) { files = stream.filter(Files::isRegularFile).sorted().toList(); }
        var payloads = new HashSet<String>();
        var pattern = Pattern.compile("data:image/png;base64,([A-Za-z0-9+/]*={0,2})(?![A-Za-z0-9+/=])");
        for (Path file : files)
            if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html")) {
                var matches = pattern.matcher(Files.readString(file));
                while (matches.find()) payloads.add(matches.group(1));
            }
        var result = new ArrayList<Path>();
        for (Path file : files) {
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) continue;
            String bytes = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
            if (payloads.contains(bytes)) result.add(folder.relativize(file));
        }
        return List.copyOf(result);
    }

    private static String inline(byte[] bytes) { return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes); }
}

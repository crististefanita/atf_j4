package atf.tests.governance.documentation.release;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Release-store retention only; inline presence does not certify visibility or complete HTML autonomy. */
@AtfComponentTest
class ReleaseEvidenceRetentionGovernanceTest {
    @TempDir Path temporary;

    @Test void activeReleasesDoNotRetainExactCopiesOfImagesAlreadyEmbeddedInTheirHtml() throws Exception {
        for (Path folder : ReleaseArtifactCatalog.folders(GovernanceProject.locateProjectRoot()))
            assertEquals(List.of(), duplicates(folder), "RELEASE-DUPLICATE-INLINE-IMAGE: " + folder);
    }

    @Test void everyDuplicatePathIsReportedIncludingNestedAndUppercaseImages() throws Exception {
        byte[] image = {1, 3, 5, 7};
        Files.writeString(temporary.resolve("report.html"), "<img src=\"" + inline(image) + "\">");
        Files.createDirectory(temporary.resolve("captures"));
        Files.write(temporary.resolve("captures/first.png"), image);
        Files.write(temporary.resolve("second.PNG"), image);
        assertEquals(List.of(Path.of("captures/first.png"), Path.of("second.PNG")), duplicates(temporary));
        assertTrue(Files.isRegularFile(temporary.resolve("captures/first.png")), "Inspection never deletes evidence");
    }

    @Test void filenamesHashesAndExternalLinksDoNotProveThatTheBytesWereEmbedded() throws Exception {
        Files.write(temporary.resolve("original.png"), new byte[]{1, 3, 5, 7});
        Files.writeString(temporary.resolve("report.html"),
                "<img src=\"original.png\"><p>original.png SHA-256=example</p>");
        assertEquals(List.of(), duplicates(temporary));
    }

    @Test void differentBytesAndIndependentDiagnosticImagesRemainSeparate() throws Exception {
        Files.writeString(temporary.resolve("report.html"), "<img src=\"" + inline(new byte[]{1, 3, 5, 7}) + "\">");
        Files.write(temporary.resolve("browser-review.png"), new byte[]{1, 3, 5, 8});
        Files.write(temporary.resolve("prefix.png"), new byte[]{1, 3, 5});
        Files.writeString(temporary.resolve("diagnostic.log"), "Independent process diagnostic");
        assertEquals(List.of(), duplicates(temporary));
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

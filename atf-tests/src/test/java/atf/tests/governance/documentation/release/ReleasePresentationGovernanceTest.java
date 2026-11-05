package atf.tests.governance.documentation.release;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Public notes are a product view; the linked evidence retains the complete audit. */
@AtfComponentTest
class ReleasePresentationGovernanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern LINKS = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern AUDIT = Pattern.compile("(?iu)\\b(?:E2E|JUnit|PASS|FAIL|skipped)\\b"
            + "|\\b[0-9]+\\s+(?:teste|tests)\\b|nu\\s+s-a\\s+testat|not\\s+tested|\\bnetestat(?:e|ă|i)?\\b");
    private record Edition(String version, String asset, String checksum, String evidence,
                           String mode, String previous, List<String> points) {
        String changesHeading() {
            return mode.equals("changes") ? "Îmbunătățiri față de " + previous : "Pe scurt";
        }
    }

    @Test void everyPublishedHandoffKeepsSchematicNotesBoundToItsActualAsset() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var repository = GovernanceProject.policy(root).required("publicRepository").asText();
        int releases = 0;
        try (var folders = Files.list(root.resolve("artifacts/reports"))) {
            for (var folder : folders.filter(Files::isDirectory).toList()) {
                if (!Files.isRegularFile(folder.resolve("github-release.json"))) continue;
                var published = JSON.readTree(folder.resolve("github-release.json").toFile());
                var validation = JSON.readTree(folder.resolve("release-validation.json").toFile());
                var presentation = JSON.readTree(folder.resolve("release-presentation.json").toFile());
                String version = validation.required("release").required("versionName").asText();
                String tag = published.required("tag").asText();
                assertEquals("v" + version, tag, folder.toString());
                var assets = new ArrayList<JsonNode>();
                published.required("assets").forEach(asset -> { if (asset.required("name").asText().endsWith(".apk")) assets.add(asset); });
                assertEquals(1, assets.size(), "One canonical APK asset per published release");
                String asset = repository + "/releases/download/" + tag + "/" + assets.getFirst().required("name").asText();
                assertEquals(asset, assets.getFirst().required("url").asText());
                assertEquals(validation.required("release").required("apkBytes").asLong(), assets.getFirst().required("size").asLong());
                assertTrue(java.util.stream.StreamSupport.stream(published.required("assets").spliterator(), false)
                        .anyMatch(row -> row.path("url").asText().equals(asset + ".sha256")), "Checksum asset must exist");
                assertEquals(version, presentation.required("version").asText());
                assertTrue(presentation.required("evidenceUrl").asText().matches(Pattern.quote(repository)
                        + "/blob/[0-9a-f]{40}/artifacts/reports/README\\.md"), "Evidence belongs to an identified documentation commit");
                var edition = new Edition(version, asset, asset + ".sha256", presentation.required("evidenceUrl").asText(),
                        presentation.required("mode").asText(), presentation.path("previousVersion").asText(null),
                        java.util.stream.StreamSupport.stream(presentation.required("points").spliterator(), false).map(JsonNode::asText).toList());
                assertEquals(List.of(), violations(edition, Files.readString(folder.resolve("release-notes.md"))), folder.toString());
                releases++;
            }
        }
        assertTrue(releases > 0, "The curated published-release population must not be empty");
    }

    @Test void aSummaryDoesNotPretendToBeAnUnverifiedVersionComparison() {
        var summary = sample("summary", null);
        assertEquals(List.of(), violations(summary, notes(summary)));
        var changes = sample("changes", "1.1.0");
        assertEquals(List.of(), violations(changes, notes(changes)));
        for (String base : new String[]{null, "", "1.2.0"})
            assertTrue(violations(sample("changes", base), notes(sample("changes", base))).contains("NOTE-BASELINE"));
        assertTrue(violations(sample("summary", "1.1.0"), notes(summary)).contains("NOTE-BASELINE"));
    }

    @Test void rawRepositoryLinksAndAnotherVersionsAssetsAreRejected() {
        var edition = sample("summary", null);
        for (String wrong : List.of("https://raw.githubusercontent.com/example/game/main/dist/Game.apk",
                edition.asset().replace("v1.2.0", "v1.1.0"), "https://github.com/example/game/blob/main/dist/Game.apk"))
            assertTrue(violations(edition, notes(edition).replace(edition.asset(), wrong)).contains("NOTE-ASSET"), wrong);
    }

    @Test void testCountsAndUntestedDeviceStatementsStayInTheEvidenceFolder() {
        var edition = sample("summary", null);
        for (String leak : List.of("73 teste trecute", "JUnit PASS", "E2E sanity", "Nu s-a testat pe ROG 7", "Not tested on device"))
            assertTrue(violations(edition, notes(edition).replace("- Masă încadrată.", "- " + leak)).contains("NOTE-AUDIT-DETAILS"), leak);
    }

    @Test void missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected() {
        var edition = sample("summary", null);
        assertTrue(violations(edition, notes(edition).replace(edition.evidence(), "https://example.test")).contains("NOTE-EVIDENCE"));
        assertTrue(violations(edition, notes(edition).replace("- Masă încadrată.", "Masă încadrată.")).contains("NOTE-BULLETS"));
        assertTrue(violations(edition, notes(edition).replace("- Masă încadrată.", "- Altă afirmație.")).contains("NOTE-POINTS"));
        assertTrue(violations(edition, notes(edition).replace("## Descărcare", "## Download")).contains("NOTE-SECTIONS"));
    }

    private static List<String> violations(Edition edition, String markdown) {
        var errors = new ArrayList<String>();
        if (!List.of("summary", "changes").contains(edition.mode())) errors.add("NOTE-MODE");
        if (edition.mode().equals("changes") && (edition.previous() == null || edition.previous().isBlank()
                || edition.previous().equals(edition.version()))) errors.add("NOTE-BASELINE");
        if (edition.mode().equals("summary") && edition.previous() != null) errors.add("NOTE-BASELINE");
        var sections = new LinkedHashMap<String, List<String>>();
        List<String> current = null;
        for (String row : markdown.split("\\R")) {
            String line = row.strip();
            if (line.isBlank()) continue;
            if (line.startsWith("## ")) {
                current = new ArrayList<>();
                if (sections.put(line.substring(3), current) != null) errors.add("NOTE-SECTIONS");
            } else if (current == null || !line.startsWith("- ")) errors.add("NOTE-BULLETS");
            else current.add(line.substring(2));
        }
        if (!new ArrayList<>(sections.keySet()).equals(List.of("Descărcare", edition.changesHeading(), "Documentație")))
            errors.add("NOTE-SECTIONS");
        if (sections.values().stream().anyMatch(List::isEmpty)) errors.add("NOTE-BULLETS");
        if (!sections.getOrDefault(edition.changesHeading(), List.of()).equals(edition.points()) || edition.points().isEmpty())
            errors.add("NOTE-POINTS");
        var destinations = new ArrayList<String>();
        var matcher = LINKS.matcher(markdown);
        var visible = new StringBuffer();
        while (matcher.find()) {
            destinations.add(matcher.group(2));
            matcher.appendReplacement(visible, java.util.regex.Matcher.quoteReplacement(matcher.group(1)));
        }
        matcher.appendTail(visible);
        if (!destinations.contains(edition.asset()) || !destinations.contains(edition.checksum())
                || destinations.stream().anyMatch(uri -> uri.endsWith(".apk") && !uri.equals(edition.asset()))) errors.add("NOTE-ASSET");
        if (!destinations.contains(edition.evidence())) errors.add("NOTE-EVIDENCE");
        if (AUDIT.matcher(visible).find()) errors.add("NOTE-AUDIT-DETAILS");
        return errors;
    }

    private static Edition sample(String mode, String previous) {
        String asset = "https://github.com/example/game/releases/download/v1.2.0/Game-1.2.0.apk";
        return new Edition("1.2.0", asset, asset + ".sha256",
                "https://github.com/example/game/blob/" + "a".repeat(40) + "/artifacts/reports/README.md",
                mode, previous, List.of("Masă încadrată."));
    }

    private static String notes(Edition edition) {
        return "## Descărcare\n\n- [APK](" + edition.asset() + ")\n- [Checksum](" + edition.checksum()
                + ")\n\n## " + edition.changesHeading() + "\n\n- Masă încadrată.\n\n## Documentație\n\n- [Rapoarte](" + edition.evidence() + ")\n";
    }
}

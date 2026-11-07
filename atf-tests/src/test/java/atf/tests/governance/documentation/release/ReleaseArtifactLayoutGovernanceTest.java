package atf.tests.governance.documentation.release;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import static atf.testkit.assertions.AuditedAssertions.*;

/** The generic strategy stays stable while each release owns its captured results. */
@AtfComponentTest
class ReleaseArtifactLayoutGovernanceTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final atf.tests.reporting.GameReportAudit AUDIT = new atf.tests.reporting.GameReportAudit();
    @TempDir Path temporary;

    @Test void registeredVersionsOwnTheirEvidenceAndHaveDiscoverableReadmes() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String projectGuide = Files.readString(root.resolve("README.md"));
        AuditedAssertions.assertTrue(projectGuide.contains("(release/README.md)"), "The project entry points to the release catalog");
        AuditedAssertions.assertFalse(projectGuide.contains("(dist/"), "The project entry must not revive the retired distribution directory");
        String catalog = Files.readString(root.resolve("release/README.md"));
        var releaseIndex = new com.fasterxml.jackson.databind.ObjectMapper().readTree(root.resolve("release/index.json").toFile());
        String current = releaseIndex.required("currentVersion").asText();
        var validation = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(root.resolve("release/" + current + "/release-validation.json").toFile());
        String repository = GovernanceProject.policy(root).required("publicRepository").asText();
        String download = ReleaseCatalogPresentation.downloadTarget(root.resolve("release/" + current), validation, repository);
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseCatalogPresentation.projectLinks(root.resolve("README.md"), projectGuide,
                releaseIndex, repository, download));
        String currentSection = ReleaseCatalogPresentation.currentSection(root.resolve("release/README.md"), catalog);
        AuditedAssertions.assertTrue(currentSection.contains("(" + current + "/README.md)"), "The current entry follows the structured index");
        for (Path folder : ReleaseArtifactCatalog.folders(root)) {
            AuditedAssertions.assertTrue(catalog.contains("(" + folder.getFileName() + "/README.md)"), folder.toString());
            String guide = Files.readString(folder.resolve("reports/README.md"));
            AuditedAssertions.assertTrue(guide.contains("(../../README.md)"), "Each release links back to the catalog");
            AuditedAssertions.assertTrue(guide.contains("(../../../docs/testing/README.md)"), "Results point to the shared strategy");
        }
    }

    @Test void theGameSectionHasOnlyTheCurrentReleaseAndItsDirectApk() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String guide = Files.readString(root.resolve("README.md")).replace("\r\n", "\n");
        int start = guide.indexOf("\n## Jocul\n");
        AuditedAssertions.assertTrue(start >= 0, "The product entry has its own section");
        int end = guide.indexOf("\n## ", start + 1);
        String section = guide.substring(start, end < 0 ? guide.length() : end);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        String version = json.readTree(root.resolve("release/index.json").toFile()).required("currentVersion").asText();
        String repository = GovernanceProject.policy(root).required("publicRepository").asText();
        Path releaseFolder = root.resolve("release/" + version);
        var manifest = json.readTree(releaseFolder.resolve("release-validation.json").toFile());
        boolean published = manifest.required("publication").required("state").asText().equals("PUBLISHED");
        var links = Pattern.compile("\\]\\((https://github\\.com/[^)]+/releases/[^)]+|[^)]+\\.apk|release/[^)]+)\\)")
                .matcher(section).results().map(result -> result.group(1)).toList();
        var expected = List.of(published ? repository + "/releases/tag/v" + version : "release/" + version + "/README.md",
                ReleaseCatalogPresentation.downloadTarget(releaseFolder, manifest, repository));
        AuditedAssertions.assertEquals(expected, links, "Two product actions; catalog and audit details belong to documentation");
    }

    @ParameterizedTest
    @ValueSource(strings = {"README.md", "reports/README.md"})
    void releaseOverviewAndReportIndexAreBothRequired(String missing) throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.delete(root.resolve("release/1.2.0").resolve(missing));
        rejected(root, "REQUIRED");
    }

    @Test void aSecondRegisteredVersionCannotRemainInTheActiveStore() throws Exception {
        Path root = project("1.2.0", List.of("1.1.0", "1.2.0"));
        rejected(root, "RETENTION");
    }

    @Test void aSecondDistributionDirectoryCannotReappearBesideTheReleaseCatalog() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.createDirectory(root.resolve("dist"));
        rejected(root, "LEGACY-DIST");
    }

    @Test void theRetiredArtifactDirectoryCannotReappearEvenWhenEmpty() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.createDirectory(root.resolve("artifacts"));
        rejected(root, "LEGACY-ARTIFACTS");
    }

    @Test void theMainReadmeAlwaysTargetsTheCurrentVersionLocallyThenOnGitHub() throws Exception {
        var index = downloadIndex();
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseCatalogPresentation.projectLinks(Path.of("README.md"),
                downloadGuide(), index, "https://github.com/example/game", "release/1.2.0/WhistLab-1.2.0.apk"));
        String published = downloadGuide().replace("(release/1.2.0/WhistLab-1.2.0.apk)",
                "(https://github.com/example/game/releases/download/v1.2.0/WhistLab-1.2.0.apk)");
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseCatalogPresentation.projectLinks(Path.of("README.md"),
                published, index, "https://github.com/example/game",
                "https://github.com/example/game/releases/download/v1.2.0/WhistLab-1.2.0.apk"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-current", "latest", "wrong-label", "wrong-asset", "duplicate", "old-extra-asset", "published-local"})
    void staleAmbiguousAndUnpublishedDownloadClaimsAreRejected(String defect) throws Exception {
        var index = downloadIndex();String guide = downloadGuide();boolean currentPublished = false;
        switch (defect) {
            case "wrong-current" -> guide = guide.replace("release/1.2.0/", "release/1.1.0/");
            case "latest" -> guide = guide.replace("release/1.2.0/WhistLab-1.2.0.apk", "https://github.com/example/game/releases/latest");
            case "wrong-label" -> guide = guide.replace("Descarcă APK 1.2.0", "Descarcă APK 1.1.0");
            case "wrong-asset" -> guide = guide.replace("WhistLab-1.2.0.apk", "WhistLab-1.1.0.apk");
            case "duplicate" -> guide += "\n" + guide;
            case "old-extra-asset" -> guide += "\n[Model](https://github.com/example/game/releases/download/v1.1.0/WhistLab-1.1.0.apk)";
            case "published-local" -> currentPublished = true;
            default -> throw new IllegalArgumentException(defect);
        }
        String observed = guide;
        String repository = "https://github.com/example/game";
        String target = ReleaseCatalogPresentation.releaseDownload(repository, "1.2.0", currentPublished);
        var failure = AuditedAssertions.assertThrows(IOException.class, () -> ReleaseCatalogPresentation.projectLinks(
                Path.of("README.md"), observed, index, repository, target));
        AuditedAssertions.assertTrue(failure.getMessage().startsWith("RELEASE-PROJECT-LINK-"), "Specific release-link diagnostic");
    }

    @Test void aPublicDownloadMustMatchThePublishedVersionAndHash() throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var manifest = json.createObjectNode();
        manifest.putObject("release").put("versionName", "1.2.0").put("apkBytes", 50).put("apkSha256", "a".repeat(64));
        manifest.putObject("publication").put("state", "PUBLISHED");
        Path receipt = Files.createDirectories(temporary.resolve("reports")).resolve("distribution-verification.json");
        var record = json.createObjectNode();
        var mirror = record.putObject("publicDownload").put("url", "https://example.org/v1.2.0/WhistLab-1.2.0.apk")
                .put("httpStatus", 200).put("bytes", 50).put("sha256", "a".repeat(64));
        json.writeValue(receipt.toFile(), record);
        String repository = "https://github.com/example/game";
        AuditedAssertions.assertEquals(mirror.path("url").asText(),
                ReleaseCatalogPresentation.downloadTarget(temporary, manifest, repository));
        mirror.put("sha256", "b".repeat(64));
        json.writeValue(receipt.toFile(), record);
        AuditedAssertions.assertThrows(IOException.class,
                () -> ReleaseCatalogPresentation.downloadTarget(temporary, manifest, repository));
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode downloadIndex() throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("currentVersion", "1.2.0");
    }

    private static String downloadGuide() {
        return "[Candidat](release/1.2.0/README.md)\n[Descarcă APK 1.2.0](release/1.2.0/WhistLab-1.2.0.apk)";
    }

    @Test void emptyDuplicateEscapingAndUnknownCurrentVersionsAreRejected() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        for (String[] sample : List.of(new String[]{"1.2.0", "[]", "POPULATION"},
                new String[]{"1.2.0", "[\"1.2.0\",\"1.2.0\"]", "DUPLICATE"},
                new String[]{"1.2.0", "[\"../outside\"]", "VERSION"},
                new String[]{"1.1.0", "[\"1.2.0\"]", "CURRENT"})) {
            index(root, sample[0], sample[1]);
            rejected(root, sample[2]);
        }
    }

    @Test void missingMetadataAndDivergentVersionOrTagCannotPass() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Path folder = root.resolve("release/1.2.0");
        Files.delete(folder.resolve("release-validation.json"));
        rejected(root, "REQUIRED");
        manifest(folder, "1.1.0");
        rejected(root, "IDENTITY");
        manifest(folder, "1.2.0");
        Files.writeString(folder.resolve("github-release.json"), "{\"tag\":\"v1.1.0\"}");
        rejected(root, "TAG");
    }

    @Test void aLegacyVersionFolderCannotBeSilentlyExcludedFromThePopulation() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.createDirectories(root.resolve("release/legacy-retired"));
        rejected(root, "UNREGISTERED");
    }

    @Test void anUnregisteredRetiredRunCannotRemainInsideReports() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.createDirectories(root.resolve("release/1.1.0"));
        rejected(root, "UNREGISTERED");
    }

    @Test void renamingAnOldRunDoesNotQualifyANewVersion() throws Exception {
        Path root = project("0.10.0", List.of("0.10.0"));
        manifest(root.resolve("release/0.10.0"), "1.9.2");
        rejected(root, "IDENTITY");
    }

    @Test void theTestingStrategyLinksTheCatalogWithoutFixingAReleaseOrItsResults() throws Exception {
        String strategy = Files.readString(GovernanceProject.locateProjectRoot().resolve("docs/testing/README.md"));
        AuditedAssertions.assertTrue(strategy.contains("(../../release/README.md)"));
        AuditedAssertions.assertTrue(strategy.contains("(../../release/index.json)"));
        AuditedAssertions.assertFalse(Pattern.compile("\\b[0-9]+\\.[0-9]+\\.[0-9]+\\b").matcher(strategy).find(), "The strategy must not fix the current release");
        AuditedAssertions.assertFalse(Pattern.compile("(?iu)\\b[0-9]+\\s+(?:teste|tests)\\s*[:—]|\\b[0-9]+/[0-9]+\\s+(?:trecute|PASS)").matcher(strategy).find(), "Run totals belong to release evidence");
    }

    @Test void currentSectionCannotBorrowAnEntryFromTheVersionsTable() throws Exception {
        String current = "## Versiunea curentă\n\n[1.2.0](1.2.0/README.md)\n\n";
        String available = "## Versiuni disponibile\n\n[1.1.0](1.1.0/README.md)\n";
        AuditedAssertions.assertEquals(current, ReleaseCatalogPresentation.currentSection(
                Path.of("catalog.md"), "# Catalog\n\n" + current + available));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-current", "missing-versions", "reversed"})
    void absentOrReversedSectionsHaveAnExplicitCatalogDiagnostic(String defect) {
        String current = "## Versiunea curentă\n";
        String available = "## Versiuni disponibile\n";
        String catalog = switch (defect) {
            case "missing-current" -> available;
            case "missing-versions" -> current;
            case "reversed" -> available + current;
            default -> throw new IllegalArgumentException(defect);
        };
        Path file = Path.of("catalog.md");
        var failure = AuditedAssertions.assertThrows(IOException.class,
                () -> ReleaseCatalogPresentation.currentSection(file, catalog));
        AuditedAssertions.assertTrue(failure.getMessage().startsWith("RELEASE-CATALOG-SECTIONS: " + file));
    }

    private Path project(String current, List<String> versions) throws Exception {
        Path root = Files.createDirectory(temporary.resolve("project"));
        Files.createDirectories(root.resolve("release"));

        for (String version : versions) {
            Path folder = Files.createDirectories(root.resolve("release").resolve(version));
            Files.writeString(folder.resolve("README.md"), "# Livrare");
            Files.createDirectories(folder.resolve("reports"));
            Files.writeString(folder.resolve("reports/README.md"), "# Dovezi");
            manifest(folder, version);
        }
        Files.writeString(root.resolve("release/README.md"), "# Catalog");
        index(root, current, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(versions));
        return root;
    }

    private static void manifest(Path folder, String version) throws Exception {
        Files.writeString(folder.resolve("release-validation.json"), "{\"release\":{\"versionName\":\"" + version + "\"}}");
    }

    private static void index(Path root, String current, String versions) throws Exception {
        Files.writeString(root.resolve("release/index.json"), "{\"schemaVersion\":2,\"currentVersion\":\"" + current + "\",\"versions\":" + versions + "}");
    }

    private static void rejected(Path root, String code) {
        AuditedAssertions.assertTrue(AuditedAssertions.assertThrows(IOException.class, () -> ReleaseArtifactCatalog.folders(root)).getMessage().contains("RELEASE-" + code));
    }
}

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
import static org.junit.jupiter.api.Assertions.*;

/** The generic strategy stays stable while each release owns its captured results. */
@AtfComponentTest
class ReleaseArtifactLayoutGovernanceTest {
    @TempDir Path temporary;

    @Test void registeredVersionsOwnTheirEvidenceAndHaveDiscoverableReadmes() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String projectGuide = Files.readString(root.resolve("README.md"));
        AuditedAssertions.assertTrue(projectGuide.contains("(release/README.md)"), "The project entry points to the release catalog");
        AuditedAssertions.assertFalse(projectGuide.contains("(dist/"), "The project entry must not revive the retired distribution directory");
        String catalog = Files.readString(root.resolve("release/README.md"));
        String current = new com.fasterxml.jackson.databind.ObjectMapper().readTree(root.resolve("release/index.json").toFile()).required("currentVersion").asText();
        String currentSection = ReleaseCatalogPresentation.currentSection(root.resolve("release/README.md"), catalog);
        AuditedAssertions.assertTrue(currentSection.contains("(" + current + "/README.md)"), "The current entry follows the structured index");
        for (Path folder : ReleaseArtifactCatalog.folders(root)) {
            AuditedAssertions.assertTrue(catalog.contains("(" + folder.getFileName() + "/README.md)"), folder.toString());
            String guide = Files.readString(folder.resolve("reports/README.md"));
            AuditedAssertions.assertTrue(guide.contains("(../../README.md)"), "Each release links back to the catalog");
            AuditedAssertions.assertTrue(guide.contains("(../../../docs/testing/README.md)"), "Results point to the shared strategy");
        }
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

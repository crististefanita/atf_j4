package atf.tests.governance.documentation.release;

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
        String catalog = Files.readString(root.resolve("artifacts/README.md"));
        String current = new com.fasterxml.jackson.databind.ObjectMapper().readTree(root.resolve("artifacts/index.json").toFile()).required("currentVersion").asText();
        String currentSection = ReleaseCatalogPresentation.currentSection(root.resolve("artifacts/README.md"), catalog);
        assertTrue(currentSection.contains("(reports/" + current + "/README.md)"), "The current entry follows the structured index");
        String reports = Files.readString(root.resolve("artifacts/reports/README.md"));
        for (Path folder : ReleaseArtifactCatalog.folders(root)) {
            assertTrue(catalog.contains("(reports/" + folder.getFileName() + "/README.md)"), folder.toString());
            assertTrue(reports.contains("(" + folder.getFileName() + "/README.md)"), folder.toString());
            String guide = Files.readString(folder.resolve("README.md"));
            assertTrue(guide.contains("(../README.md)"), "Each release links back to the catalog");
            assertTrue(guide.contains("(../../../docs/testing/README.md)"), "Results point to the shared strategy");
        }
    }

    @Test void aSecondRegisteredVersionCannotRemainInTheActiveStore() throws Exception {
        Path root = project("1.2.0", List.of("1.1.0", "1.2.0"));
        rejected(root, "RETENTION");
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
        Path folder = root.resolve("artifacts/reports/1.2.0");
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
        Files.createDirectories(root.resolve("artifacts/1.1.0"));
        rejected(root, "UNREGISTERED");
    }

    @Test void anUnregisteredRetiredRunCannotRemainInsideReports() throws Exception {
        Path root = project("1.2.0", List.of("1.2.0"));
        Files.createDirectories(root.resolve("artifacts/reports/1.1.0"));
        rejected(root, "UNREGISTERED");
    }

    @Test void renamingAnOldRunDoesNotQualifyANewVersion() throws Exception {
        Path root = project("0.10.0", List.of("0.10.0"));
        manifest(root.resolve("artifacts/reports/0.10.0"), "1.9.2");
        rejected(root, "IDENTITY");
    }

    @Test void theTestingStrategyLinksTheCatalogWithoutFixingAReleaseOrItsResults() throws Exception {
        String strategy = Files.readString(GovernanceProject.locateProjectRoot().resolve("docs/testing/README.md"));
        assertTrue(strategy.contains("(../../artifacts/README.md)"));
        assertTrue(strategy.contains("(../../artifacts/index.json)"));
        assertFalse(Pattern.compile("\\b[0-9]+\\.[0-9]+\\.[0-9]+\\b").matcher(strategy).find(), "The strategy must not fix the current release");
        assertFalse(Pattern.compile("(?iu)\\b[0-9]+\\s+(?:teste|tests)\\s*[:—]|\\b[0-9]+/[0-9]+\\s+(?:trecute|PASS)").matcher(strategy).find(), "Run totals belong to release evidence");
    }

    @Test void currentSectionCannotBorrowAnEntryFromTheVersionsTable() throws Exception {
        String current = "## Versiunea curentă\n\n[1.2.0](1.2.0/README.md)\n\n";
        String available = "## Versiuni disponibile\n\n[1.1.0](1.1.0/README.md)\n";
        assertEquals(current, ReleaseCatalogPresentation.currentSection(
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
        var failure = assertThrows(IOException.class,
                () -> ReleaseCatalogPresentation.currentSection(file, catalog));
        assertTrue(failure.getMessage().startsWith("RELEASE-CATALOG-SECTIONS: " + file));
    }

    private Path project(String current, List<String> versions) throws Exception {
        Path root = Files.createDirectory(temporary.resolve("project"));
        Files.createDirectories(root.resolve("artifacts/reports"));
        Files.writeString(root.resolve("artifacts/reports/README.md"), "# Ultimele rezultate");
        for (String version : versions) {
            Path folder = Files.createDirectories(root.resolve("artifacts/reports").resolve(version));
            Files.writeString(folder.resolve("README.md"), "# Dovezi");
            manifest(folder, version);
        }
        Files.writeString(root.resolve("artifacts/README.md"), "# Catalog");
        index(root, current, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(versions));
        return root;
    }

    private static void manifest(Path folder, String version) throws Exception {
        Files.writeString(folder.resolve("release-validation.json"), "{\"release\":{\"versionName\":\"" + version + "\"}}");
    }

    private static void index(Path root, String current, String versions) throws Exception {
        Files.writeString(root.resolve("artifacts/index.json"), "{\"schemaVersion\":2,\"currentVersion\":\"" + current + "\",\"versions\":" + versions + "}");
    }

    private static void rejected(Path root, String code) {
        assertTrue(assertThrows(IOException.class, () -> ReleaseArtifactCatalog.folders(root)).getMessage().contains("RELEASE-" + code));
    }
}

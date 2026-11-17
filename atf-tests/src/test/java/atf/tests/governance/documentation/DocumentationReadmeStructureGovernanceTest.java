package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernanceSupport;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class DocumentationReadmeStructureGovernanceTest {
    @Test
    void ownerDocumentsKeepAReadableStableShape() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.STRUCTURE);
        DocumentationAssertions.assertNoViolations("README structure governance", result.violations());
    }

    @Test
    void theOtherEditionHeadingIsRejectedByTheStructuralRule(@TempDir Path temporary) throws Exception {
        for (String language : List.of("en", "ro")) {
            Path root = Files.createDirectory(temporary.resolve(language));
            Files.writeString(Files.createDirectories(root.resolve("config")).resolve("atf-governance-policy.json"),
                    "{\"documentationLanguage\":\"" + language + "\"}");
            String wrong = language.equals("en") ? "Pe scurt" : "Overview";
            Files.writeString(root.resolve("README.md"), "# Contract\n\n## " + wrong + "\n");
            assertEquals(List.of("DOC-README-OVERALL"), DocumentationGovernance
                    .scanProject(root, DocumentationGovernance.Rule.STRUCTURE).violations().stream()
                    .map(DocumentationGovernanceSupport.Violation::code).toList(), language);
        }
    }

    @ParameterizedTest
    @CsvSource({"en,Overview,Pe scurt", "ro,Pe scurt,Overview"})
    void theDeclaredLanguageControlsRootModuleAndGuideHeadings(
            String language, String expected, String wrong, @TempDir Path root) throws Exception {
        Path config = Files.createDirectories(root.resolve("config")).resolve("atf-governance-policy.json");
        Files.writeString(config, "{\"documentationLanguage\":\"" + language
                + "\",\"publicRepository\":\"https://github.com/example/whist\"}");
        var files = List.of("README.md", "module/README.md", "docs/guide.md");
        String healthy = "# Contract\n\n## " + expected + "\n\nContract.\n";
        for (String relative : files) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, healthy);
        }
        DocumentationAssertions.assertNoViolations("Heading policy without release artifacts",
                DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.STRUCTURE).violations());
        for (String relative : files) {
            Path file = root.resolve(relative);
            Files.writeString(file, healthy.replace(expected, wrong));
            var selected = DocumentationGovernance.scanFile(root, relative, DocumentationGovernance.Rule.STRUCTURE).violations();
            assertEquals(List.of("DOC-README-OVERALL"), selected.stream()
                    .map(DocumentationGovernanceSupport.Violation::code).toList());
            assertTrue(selected.getFirst().fix().contains("## " + expected));
            assertEquals(selected, DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.STRUCTURE)
                    .violations().stream().filter(row -> row.file().equals(Path.of(relative))).toList());
            Files.writeString(file, healthy);
        }
    }

    @ParameterizedTest
    @CsvSource({"en,Overviewish", "ro,Pe scurtish", "en,Summary", "ro,Summary"})
    void partialAndUnrelatedHeadingsFailOnTheMainReadme(
            String language, String heading, @TempDir Path root) throws Exception {
        Files.writeString(Files.createDirectories(root.resolve("config")).resolve("atf-governance-policy.json"),
                "{\"documentationLanguage\":\"" + language + "\"}");
        Files.writeString(root.resolve("README.md"), "# Contract\n\n## " + heading + "\n");
        assertEquals(List.of("DOC-README-OVERALL"), DocumentationGovernance
                .scanProject(root, DocumentationGovernance.Rule.STRUCTURE).violations().stream()
                .map(DocumentationGovernanceSupport.Violation::code).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"fr\"", "\"EN\"", "null", "1", "true", "{}"})
    void malformedLanguageCannotDefaultToAnotherEdition(String value, @TempDir Path root) throws Exception {
        Files.writeString(Files.createDirectories(root.resolve("config")).resolve("atf-governance-policy.json"),
                "{\"documentationLanguage\":" + value + "}");
        Files.writeString(root.resolve("README.md"), "# Contract\n\n## Pe scurt\n");
        IOException failure = assertThrows(IOException.class,
                () -> DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.STRUCTURE));
        assertTrue(failure.getMessage().contains("DOC-CONTENT-CONFIG: invalid documentation language"));
    }

    @Test
    void declaredReleaseReportIndexKeepsItsOriginalShape(@org.junit.jupiter.api.io.TempDir Path root) throws Exception {
        Path release = Files.createDirectories(root.resolve("release/1.2.3/reports/component"));
        Path evidence = Files.writeString(release.resolve("README.md"), "# Component report index\n");
        Path manifest = release.getParent().getParent().resolve("release-validation.json");
        Files.writeString(manifest, """
                {"integrity":{"files":[{"path":"reports/component/README.md"}]}}
                """);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/reports/component/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        DocumentationAssertions.assertNoViolations("Release evidence index", violations);

        Files.writeString(release.getParent().getParent().resolve("README.md"), "# Release owner\n");
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        Files.writeString(manifest, """
                {"integrity":{"files":[]}}
                """);
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/reports/component/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        atf.testkit.assertions.AuditedAssertions.assertEquals(
                List.of("DOC-README-OVERALL", "DOC-README-OVERALL"),
                violations.stream().map(DocumentationGovernanceSupport.Violation::code).toList());
    }
}

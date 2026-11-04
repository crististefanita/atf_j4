package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.links.DocumentationLinkGovernanceTest;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class DocumentationPolicyGovernanceTest {
    @Test void theInventoryContainsAllModuleReadmesAndNoPerReleaseVerificationNotes() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var files = GovernanceProject.markdown(root);
        assertTrue(files.contains(root.resolve("README.md")));
        var modules = GovernanceProject.policy(root).required("mavenModules");
        for (var module : modules) assertTrue(files.contains(root.resolve(module.asText()).resolve("README.md")),
                () -> "Missing governed module README: " + module);
        for (Path file : files) {
            assertFalse(file.getFileName().toString().matches("ATF_Reporting_J[0-9]+_Verification\\.md"),
                    () -> "Release chronology belongs in the external archive: " + file);
            if (file.getFileName().toString().equalsIgnoreCase("README.md")) {
                assertFalse(hasEnglishReadmeProse(Files.readString(file)), () -> "README prose must be Romanian: " + file);
            }
            String prose = Files.readString(file);
            if (file.equals(root.resolve("docs/feedback/README.md"))) prose = prose.replace("atf_j0", "responsabil");
            assertFalse(java.util.regex.Pattern.compile("(?i)\\b(?:atf[_-]j[0-9]+|J[0-9]+)\\b").matcher(prose).find(),
                    () -> "Official documentation uses domain concepts; library-owner routing belongs in feedback/README: " + file);
        }
    }

    static boolean hasEnglishReadmeProse(String source) {
        String prose = source.replaceAll("(?s)```.*?```", "").replaceAll("`[^`]*`", "").replaceAll("\\]\\([^)]*\\)", "]");
        if (java.util.regex.Pattern.compile("(?im)^#{1,6} (?:Overview|Quick start|Getting started|Captured UI timing|Usage|Testing|Architecture|Requirements|Shared platform)\\b")
                .matcher(prose).find()) return true;
        var words = java.util.regex.Pattern.compile("(?i)\\b(the|this|these|that|with|without|and|must|should|their|each|after|before|from|where|when|which|remain|remains)\\b");
        for (String paragraph : prose.split("\\n\\s*\\n")) {
            long signals = words.matcher(paragraph).results().map(match -> match.group().toLowerCase(java.util.Locale.ROOT)).distinct().count();
            if (signals >= 4) return true;
        }
        return false;
    }

    @Test void languageGuardRejectsEnglishProseButKeepsQuotedApiAndCommands() {
        assertTrue(hasEnglishReadmeProse("# Overview\n\nThis module must preserve the source and their contracts before publication."));
        assertTrue(hasEnglishReadmeProse("# Mecanism\n\nThis component must preserve the source and their contracts before publication."));
        assertFalse(hasEnglishReadmeProse("# Mecanism\n\nRegulile păstrează contractul. API-ul `with and before after must` își păstrează numele.\n\n```text\nThis and that must remain before the source.\n```"));
    }

    @Test void copiedValidatorsDetectInjectedDefectsAndAcceptAHealthyNeighbour(@TempDir Path root) throws Exception {
        Path broken = root.resolve("README.md");
        Files.writeString(broken, "# Control\n\n## Pe scurt\n\n[Lipsă](missing.md)\n\n```mermaid\nflowchart LR\nA --> B\nB --> C\n```\n");
        var failures = validate(broken, root);
        var codes = failures.stream().map(DocumentationGovernanceSupport.Violation::code).toList();
        assertTrue(codes.contains("DOC-README-LINK"));
        assertTrue(codes.contains("DOC-DIAG-EDGE"));
        assertTrue(codes.contains("DOC-DIAG-LEGEND"));
        Path healthy = root.resolve("healthy/README.md");
        Files.createDirectories(healthy.getParent());
        Files.writeString(healthy, "# Control sănătos\n\n## Pe scurt\n\nUn contract stabil.\n\n[Acest document](README.md)\n");
        assertTrue(validate(healthy, root).isEmpty(), "A healthy neighbouring document must remain valid");
    }

    @Test void generatedDirectoriesAreExcludedButNestedReadmesAndDocsRemainVisible(@TempDir Path root) throws Exception {
        for (String file : List.of("README.md", "nested/README.md", "docs/guide.md", "nested/build/README.md",
                "target/README.md", "node_modules/pkg/README.md", "dist/README.md", "unrelated/example.md")) {
            Path path = root.resolve(file); Files.createDirectories(path.getParent()); Files.writeString(path, "# Control\n");
        }
        assertEquals(List.of("README.md", "docs/guide.md", "nested/README.md"), GovernanceProject.markdown(root).stream()
                .map(file -> GovernanceProject.relative(root, file)).sorted().toList());
    }

    private static List<DocumentationGovernanceSupport.Violation> validate(Path file, Path root) {
        var failures = new ArrayList<DocumentationGovernanceSupport.Violation>();
        DocumentationReadmeStructureGovernanceTest.validate(file, root, failures);
        DocumentationDiagramGovernanceTest.validate(file, root, failures);
        DocumentationStyleGovernanceTest.validate(file, root, failures);
        DocumentationProseGovernanceTest.validate(file, root, failures);
        DynamicPathDocumentationArchitectureTest.validatePaths(file, root, failures);
        DynamicPathDocumentationArchitectureTest.validateGeneratedReportNames(file, root, failures);
        DocumentationLinkGovernanceTest.validate(file, root, failures);
        return failures;
    }
}

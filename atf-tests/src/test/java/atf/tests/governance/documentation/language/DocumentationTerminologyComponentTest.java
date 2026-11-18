package atf.tests.governance.documentation.language;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.policy.DocumentationTerminologyRules;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DocumentationTerminologyComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"SHAA-256", "checksumes", "READMEM", "Caache", "processor charger",
            "classpathin", "Lacquer delimited", "source snack", "Unbug", "Quotts"})
    void rejectsKnownArtifactsInVisibleEnglish(String term) {
        var findings = inspect(List.of("# Guide", "", "Compare " + term + "."), "en");
        assertEquals(1, findings.size());
        assertEquals("DOC-EN-TERMINOLOGY", findings.getFirst().code());
        assertEquals(3, findings.getFirst().line());
    }

    @Test void technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule() {
        var lines = List.of("`SHAA-256`", "[Checksum](https://example.invalid/READMEM)",
                "~~~text", "classpathin", "~~~", "```json", "{\"key\":\"Caache\"}", "```",
                "Directory, SHA-256, checksum, asset, receipt and annotation processor.");
        assertEquals(List.of(), inspect(lines, "en"));
        assertEquals(List.of(), inspect(List.of("SHAA-256"), "ro"));
    }

    @Test void linkLabelsAndTableCellsRemainVisible() {
        assertEquals(List.of(1, 2), inspect(List.of("[READMEM](https://example.invalid)",
                "| Source | source snack |"), "en").stream().map(Violation::line).toList());
    }

    private static List<Violation> inspect(List<String> lines, String language) {
        Path root = Path.of(".").toAbsolutePath();
        var findings = new ArrayList<Violation>();
        DocumentationTerminologyRules.validate(root.resolve("docs/Guide.md"), root, lines, language, findings);
        return findings;
    }
}

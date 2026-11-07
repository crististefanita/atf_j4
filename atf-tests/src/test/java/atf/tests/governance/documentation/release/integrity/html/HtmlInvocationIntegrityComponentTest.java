package atf.tests.governance.documentation.release.integrity.html;

import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Controlled publication fixtures; no historical report is rewritten or granted new comparisons. */
@AtfComponentTest
public final class HtmlInvocationIntegrityComponentTest {
    @TempDir Path temporary;
    @Test void aPassingOwnedReceiptMatchesItsActualRowsAndCannotBeCountedTwice() throws Exception {
        Path file = html("PASS", 1, true, false);
        var identities = new HashSet<String>();
        assertEquals(1L, HtmlInvocationIntegrity.inspect(file, "GRADLE_COMPONENT_V1", identities));
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.inspect(file, "GRADLE_COMPONENT_V1", identities),
                "A repeated invocation cannot become a second primary suite population");
    }
    @Test void missingComparisonsFailureSkipAndMissingFixtureCannotQualify() throws Exception {
        for (String outcome : java.util.List.of("FAIL", "ERROR", "SKIP")) {
            Path file = html(outcome, 1, true, false);
            assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.inspect(file, "GRADLE_COMPONENT_V1", new HashSet<>()),
                    "A non-passing original outcome remains non-passing: " + outcome);
        }
        Path missing = html("PASS", 2, true, false);
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.inspect(missing, "GRADLE_COMPONENT_V1", new HashSet<>()), "Metadata cannot substitute a missing rendered comparison");
        Path noFixture = html("PASS", 1, false, false);
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.inspect(noFixture, "GRADLE_COMPONENT_V1", new HashSet<>()), "A passing component needs captured runtime context");
    }
    @Test void receiptsOutsideTheirRenderedOwnerCannotIncreaseThePopulation() throws Exception {
        Path file = html("PASS", 1, true, true);
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.inspect(file, "GRADLE_COMPONENT_V1", new HashSet<>()), "Orphan receipt is not an executable test owner");
    }
    @Test void modernQualificationRefusesMissingHtmlGroupsAndDuplicateXmlReporting() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var qualification = mapper.readTree("{\"schemaVersion\":2,\"groups\":{},\"uniqueJunitInvocations\":1}");
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.verify(temporary, qualification, java.util.Set.of()), "Modern audit schema requires canonical HTML groups");
        Path directory = Files.createDirectories(temporary.resolve("reports/component"));
        Files.copy(html("PASS", 1, true, false), directory.resolve("app.html"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) qualification).set("auditedGroups", mapper.readTree(
                "{\"app\":{\"report\":\"reports/component/app.html\",\"protocol\":\"GRADLE_COMPONENT_V1\",\"tests\":1,\"failures\":0,\"errors\":0,\"skipped\":0}}"));
        HtmlInvocationIntegrity.verify(temporary, qualification, java.util.Set.of());
        Files.createDirectories(temporary.resolve("reports/test-results/app"));
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.verify(temporary, qualification, java.util.Set.of()), "Audited HTML and raw component JUnit cannot be retained as duplicate reports");
    }
    private Path html(String outcome, int comparisons, boolean fixture, boolean orphan) throws Exception {
        String receipt = "<details class='atf-observation-evidence'><summary>JUnit invocation receipt</summary><pre>{\"schemaVersion\":1,\"invocationId\":\"fixture.Component#case\",\"junitOutcome\":\""
                + outcome + "\",\"comparisons\":" + comparisons + ",\"fixtureCaptured\":" + fixture + "}</pre></details>";
        String source = "<!doctype html><html><body><section id='test-view'><div class='test-list-wrapper'><div class='test-list-item'><div class='test-item'><div class='test-contents'><div class='mt-4'><div class='accordion'><div class='card'>"
                + receipt + "<table class='helm-validation-table'><tbody><tr><td>Invariant</td><td>1</td><td>1</td><td>✅</td></tr></tbody></table>"
                + "</div></div></div></div></div></div></div></section>" + (orphan ? receipt : "") + "</body></html>";
        Path file = Files.createTempFile(temporary, "controlled-component-", ".html"); Files.writeString(file, source); return file;
    }
}

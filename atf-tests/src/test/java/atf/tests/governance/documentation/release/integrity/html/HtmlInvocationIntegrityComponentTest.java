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
    @Test void currentAtfExtentReceiptsQualifyWithoutDuplicateJunitXml() throws Exception {
        Path file = componentHtml();
        assertEquals(1L, HtmlInvocationIntegrity.inspect(file, "ATF_J24_COMPONENT_EXTENT", new HashSet<>()));
        Path gradle = html("PASS", 1, true, false);
        assertEquals(1L, HtmlInvocationIntegrity.inspect(gradle, "ATF_J24_COMPONENT_EXTENT", new HashSet<>()));
        var qualification = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {"schemaVersion":2,"uniqueJunitInvocations":2,"auditedGroups":{"app":{"report":"reports/component/app.html","protocol":"ATF_J24_COMPONENT_EXTENT","tests":1,"failures":0,"errors":0,"skipped":0},"gradle":{"report":"reports/component/gradle.html","protocol":"ATF_J24_COMPONENT_EXTENT","tests":1,"failures":0,"errors":0,"skipped":0}}}
                """);
        Path report = temporary.resolve("reports/component/app.html");
        Files.createDirectories(report.getParent()); Files.copy(file, report);
        Files.copy(gradle, temporary.resolve("reports/component/gradle.html"));
        HtmlInvocationIntegrity.verify(temporary, qualification, java.util.Set.of());
        assertFalse(Files.exists(temporary.resolve("reports/test-results/app")), "Canonical Extent keeps invocation evidence without duplicate XML");
    }
    @Test void selfReportingGovernanceExtentIsBoundByItsExecutionReceiptAfterTheSuite() throws Exception {
        var qualification = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {"schemaVersion":2,"uniqueJunitInvocations":2,"auditedGroups":{"app":{"report":"reports/component/app.html","protocol":"ATF_J24_COMPONENT_EXTENT","tests":1,"failures":0,"errors":0,"skipped":0},"atf-tests":{"report":"reports/component/atf-tests.html","protocol":"ATF_J24_COMPONENT_EXTENT","tests":1,"failures":0,"errors":0,"skipped":0}}}
                """);
        Path app = temporary.resolve("reports/component/app.html");
        Path governance = temporary.resolve("reports/component/atf-tests.html");
        Files.createDirectories(app.getParent()); Files.copy(componentHtml(), app);
        Files.writeString(governance, "<html>Bound after execution by the root receipt and report hash.</html>");
        HtmlInvocationIntegrity.verify(temporary, qualification, java.util.Set.of(), java.util.Set.of("atf-tests"));
        Files.createDirectories(temporary.resolve("reports/test-results/atf-tests"));
        assertThrows(java.io.IOException.class, () -> HtmlInvocationIntegrity.verify(
                temporary, qualification, java.util.Set.of(), java.util.Set.of("atf-tests")),
                "Deferred report binding still rejects duplicate JUnit output");
    }
    private Path html(String outcome, int comparisons, boolean fixture, boolean orphan) throws Exception {
        String receipt = "<details class='atf-observation-evidence'><summary>JUnit invocation receipt</summary><pre>{\"schemaVersion\":1,\"invocationId\":\"fixture.Component#case\",\"junitOutcome\":\""
                + outcome + "\",\"comparisons\":" + comparisons + ",\"fixtureCaptured\":" + fixture + "}</pre></details>";
        String source = "<!doctype html><html><body><section id='test-view'><div class='test-list-wrapper'><div class='test-list-item'><div class='test-item'><div class='test-contents'><div class='mt-4'><div class='accordion'><div class='card'>"
                + receipt + "<table class='helm-validation-table'><tbody><tr><td>Invariant</td><td>1</td><td>1</td><td>✅</td></tr></tbody></table>"
                + "</div></div></div></div></div></div></div></section>" + (orphan ? receipt : "") + "</body></html>";
        Path file = Files.createTempFile(temporary, "controlled-component-", ".html"); Files.writeString(file, source); return file;
    }
    private Path componentHtml() throws Exception {
        String receipt = "<details class='atf-observation-evidence'><summary>Component invocation receipt</summary><details><summary>Full captured input</summary><pre>{\"schemaVersion\":1,\"invocationId\":\"suite.Component#current\",\"outcome\":\"PASS\",\"comparisons\":1}</pre></details></details>";
        String source = "<!doctype html><html><body><section id='test-view'><div class='test-list-wrapper'><div class='test-list-item'><div class='test-item'><div class='test-contents'><div class='mt-4'><div class='accordion'><div class='card'>"
                + receipt + "<table class='helm-validation-table'><tbody><tr><td>Invariant</td><td>1</td><td>1</td><td>✅</td></tr></tbody></table>"
                + "</div></div></div></div></div></div></div></section></body></html>";
        Path file = Files.createTempFile(temporary, "controlled-atf-extent-", ".html"); Files.writeString(file, source); return file;
    }
}

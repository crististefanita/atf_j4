package atf.gradle.audit;

import atf.gradle.audit.verification.GradleReportVerifier;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Controlled runner executions qualify capture, original verdicts and refusal of unadapted cases. */
public final class GradleAuditBridgeComponentTest extends GradleAuditedTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private String reportBefore, catalogBefore;
    private Path report;
    @Before public void prepare() throws Exception {
        reportBefore = System.getProperty("atf.gradle.audit.report"); catalogBefore = System.getProperty("atf.gradle.audit.catalog");
        report = temporary.getRoot().toPath().resolve("report.html");
        System.setProperty("atf.gradle.audit.report", report.toString()); System.clearProperty("atf.gradle.audit.catalog");
        GradleInputs.fixture("Bridge qualification environment", "engine", "JUnit 4", "renderer", "Extent", "controlled report", report);
    }
    @After public void restore() {
        GradleReport.closeCurrent(); set("atf.gradle.audit.report", reportBefore); set("atf.gradle.audit.catalog", catalogBefore);
    }
    private static void set(String name, String value) { if (value == null) System.clearProperty(name); else System.setProperty(name, value); }
    @Test public void evaluatedInputsAndComparisonsAreInTheOnlyHtml() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Passing.class); GradleReport.closeCurrent();
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
        var receipts = GradleReportVerifier.receipts(report);
        assertEquals(1, receipts.size()); assertTrue(receipts.values().stream().allMatch("PASS"::equals));
        String html = java.nio.file.Files.readString(report);
        assertTrue(html.contains("players")); assertTrue(html.contains("Fixture supplied to the component"));
        assertEquals(1, java.nio.file.Files.list(temporary.getRoot().toPath()).count());
    }
    @Test public void noInputAndNoAuditedComparisonCannotProducePassingInvocations() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.MissingInput.class, BridgeCases.MissingComparison.class); GradleReport.closeCurrent();
        assertEquals(2, result.getFailureCount());
        assertEquals(2, GradleReportVerifier.receipts(report).size());
        assertTrue(GradleReportVerifier.receipts(report).values().stream().allMatch("FAIL"::equals));
    }
    @Test public void failureErrorAndAssumptionRetainTheirOriginalOutcome() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Failure.class, BridgeCases.Error.class, BridgeCases.Abort.class); GradleReport.closeCurrent();
        assertEquals(2, result.getFailureCount()); assertEquals(1, result.getAssumptionFailureCount());
        var outcomes = new java.util.HashSet<>(GradleReportVerifier.receipts(report).values());
        assertEquals(java.util.Set.of("FAIL", "ERROR", "SKIP"), outcomes);
    }
    @Test public void expectedNestedAssertionFailureDoesNotPoisonTheOuterContract() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.ExpectedFailure.class); GradleReport.closeCurrent();
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
        assertEquals(java.util.List.of("PASS"), GradleReportVerifier.receipts(report).values().stream().toList());
    }
    @Test public void parametrizedCasesKeepIndependentIdentitiesAndValues() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.ParameterCases.class); GradleReport.closeCurrent();
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
        assertEquals(2, GradleReportVerifier.receipts(report).size());
    }
    @Test public void aMissingOriginalInvocationFailsTheInventoryGate() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Passing.class); GradleReport.closeCurrent(); assertTrue(result.wasSuccessful());
        Path xml = temporary.newFolder("junit").toPath();
        java.nio.file.Files.writeString(xml.resolve("TEST-controlled.xml"), "<testsuite><testcase classname=\"unadapted.Case\" name=\"missing\"/></testsuite>");
        assertThrows(IllegalStateException.class, () -> GradleReportVerifier.verify(report, xml));
    }
    @Test public void duplicateReceiptsAreRejectedAndInputTextCannotForgeAnInvocation() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.LongText.class); GradleReport.closeCurrent(); assertTrue(result.wasSuccessful());
        assertEquals(1, GradleReportVerifier.receipts(report).size());
        var document = org.jsoup.Jsoup.parse(report.toFile(), "UTF-8");
        var completeValues = document.select("details.atf-observation-evidence").stream()
                .filter(e -> e.children().stream().anyMatch(child -> child.tagName().equals("summary") && child.text().startsWith("Captured context: Full comparison value /")))
                .map(e -> e.select("pre").stream().map(org.jsoup.nodes.Element::wholeText).collect(java.util.stream.Collectors.joining())).toList();
        assertEquals("Expected and actual preserve every character beyond the display limit", java.util.List.of(BridgeCases.LongText.value(), BridgeCases.LongText.value()), completeValues);
        var receipt = document.select("details.atf-observation-evidence").stream()
                .filter(e -> e.children().stream().anyMatch(child -> child.tagName().equals("summary") && child.text().equals("JUnit invocation receipt"))).findFirst().orElseThrow();
        document.body().appendChild(receipt.clone());
        Path duplicate = temporary.getRoot().toPath().resolve("duplicate.html"); java.nio.file.Files.writeString(duplicate, document.outerHtml());
        assertThrows(IllegalStateException.class, () -> GradleReportVerifier.receipts(duplicate));
    }
    @Test public void removedComparisonAndMismatchedPublicationIntentAreRejected() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Passing.class); GradleReport.closeCurrent(); assertTrue(result.wasSuccessful());
        var document = org.jsoup.Jsoup.parse(report.toFile(), "UTF-8");
        document.selectFirst("table.helm-validation-table > tbody > tr").remove();
        Path missing = temporary.getRoot().toPath().resolve("missing-comparison.html"); java.nio.file.Files.writeString(missing, document.outerHtml());
        assertThrows(IllegalStateException.class, () -> GradleReportVerifier.receipts(missing));
    }
    @Test public void numericAndArrayBoundariesKeepTheOriginalJUnitContracts() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Numeric.class); GradleReport.closeCurrent();
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
        assertEquals(1, GradleReportVerifier.receipts(report).size());
    }
    @Test public void declaredRasterBytesAreInlineAndTheNeutralProfileQualifiesThem() throws Exception {
        var result = JUnitCore.runClasses(BridgeCases.Image.class); GradleReport.closeCurrent();
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
        assertEquals(1, GradleReportVerifier.receipts(report).size());
        var document = org.jsoup.Jsoup.parse(report.toFile(), "UTF-8");
        var link = document.selectFirst("a.atf-ui-action-screenshot-link");
        assertNotNull("Declared raster has its owned viewer link", link);
        String target = link.attr("href");
        if (target.startsWith("#")) target = document.getElementById(target.substring(1)).attr("href");
        assertTrue("The viewer resolves to the canonical inline PNG", target.startsWith("data:image/png;base64,"));
        var pixels = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(target.substring("data:image/png;base64,".length()))));
        assertEquals("Raster width is preserved", 2, pixels.getWidth());
        assertEquals("Raster height is preserved", 2, pixels.getHeight());
        assertEquals("Raster retains original ink", 0xff97462e, pixels.getRGB(0, 0));
        link.remove();
        Path missing = temporary.getRoot().toPath().resolve("missing-raster.html"); java.nio.file.Files.writeString(missing, document.outerHtml());
        assertThrows("Theme assets cannot replace the deleted owned raster", IllegalStateException.class, () -> GradleReportVerifier.receipts(missing));
    }
}

package atf.android.e2e.support;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertNotEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertNotNull;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jsoup.Jsoup;

@AtfComponentTest
class AndroidUiFlowReportPublisherTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test
    void embedsCompleteSupportingTextSafelyFromTheFingerprintedBytes(@TempDir Path output) throws Exception {
        Path screenshot = output.resolve("captured.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        String input = "<hierarchy>" + "<node text=\"fixture-value\"/>".repeat(2000)
                + "<script>window.ANDROID_RAW_INPUT_CANARY = 1;</script></hierarchy>";
        Path hierarchy = Files.writeString(output.resolve("full-input.xml"), input);
        Path alias = Files.copy(hierarchy, output.resolve("same-input.xml"));
        var trace = attributionTrace(output, screenshot, false, false, List.of(hierarchy));
        var captured = atf.android.evidence.text.CapturedTextEvidence.read(hierarchy, "full-input.xml");
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("complete.html"),
                "package:/fixture.apk", "mock-device", "28", "Controlled publication; no Android commands",
                List.of(screenshot, alias), trace);
        var document = Jsoup.parse(Files.readString(report));
        String published = Files.readString(report);
        var contract = atf.reporting.ui.contract.UiReportArtifactContract.evaluate(published);
        assertTrue(contract.isCompliant(), "Published Android input satisfies the common UI contract: " + contract.violations());
        assertFalse(atf.reporting.ui.contract.UiReportArtifactContract.evaluate(published.replace(
                "data-atf-evidence-type=\"report-setup\"", "data-atf-evidence-type=\"missing-setup\"")).isCompliant(),
                "Removing the typed publication preconditions must fail the common contract");
        var parts = document.select("pre").stream().filter(node -> {
            var owner = node.closest("details");
            return owner != null && owner.selectFirst("summary") != null
                    && owner.selectFirst("summary").text().startsWith("Captured supporting evidence")
                    && owner.selectFirst("summary").text().contains(captured.sha256());
        }).map(org.jsoup.nodes.Element::wholeText).toList();
        assertEquals(input, String.join("", parts), "Complete ordered input survives the body budget and the final sentinel");
        assertTrue(document.text().contains(captured.sha256()));
        assertTrue(document.text().contains("bytes=" + Files.size(hierarchy)));
        assertTrue(document.text().contains("same-input.xml"), "The inventory preserves both supplied identities");
        assertTrue(document.text().contains("full-input.xml"), "Checkpoint-declared input is mandatory even when omitted by the caller");
        assertEquals(atf.android.evidence.text.EvidenceTextParts.split(input,
                atf.reporting.configuration.AtfReportConfigurationLoader.load().reportingSettings().bodyMaxDisplayCharacters()).size(),
                parts.size(), "Identical supporting bytes have one canonical set of body parts");
        assertFalse(document.select("script").stream().anyMatch(script -> script.data().contains("ANDROID_RAW_INPUT_CANARY")));
        assertFalse(document.select("a[href]").stream().anyMatch(link -> link.attr("href").endsWith(".xml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing.xml", "invalid.xml", "unsupported.bin"})
    void refusesUnreadableSupportingEvidenceBeforeOpeningTheReport(String name, @TempDir Path output) throws Exception {
        Path screenshot = output.resolve("captured.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        var trace = attributionTrace(output, screenshot, false, false);
        Path evidence = output.resolve(name);
        if (name.equals("invalid.xml")) Files.write(evidence, new byte[] {(byte) 0xc3, 0x28});
        if (name.equals("unsupported.bin")) Files.write(evidence, new byte[] {1, 2, 3});
        Path report = output.resolve("must-not-be-passed.html");
        assertThrows(java.io.IOException.class, () -> AndroidUiFlowReportPublisher.publish(report,
                "package:/fixture.apk", "mock-device", "28", "Controlled refusal", List.of(evidence), trace));
        assertFalse(Files.exists(report), "Missing input cannot leave an implicitly successful report");
    }

    @Test
    void refusesFalseDriverTitleWithIdenticalRowsComparisonsAndPng(@TempDir Path output) throws Exception {
        Path screenshot = output.resolve("captured.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        var trace = attributionTrace(output, screenshot, false, false);
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("provenance.html"),
                "package:/fixture.apk", "mock-device", "28", "Mocked Android publication", List.of(screenshot), trace);
        String original = Files.readString(report);
        String mutated = original.replace("UI action sequence (2 entries)", "Playwright action sequence (2 entries)");
        assertFalse(original.equals(mutated));
        var before = Jsoup.parse(original);
        var after = Jsoup.parse(mutated);
        assertEquals(before.select("table.atf-ui-action-table").outerHtml(), after.select("table.atf-ui-action-table").outerHtml());
        assertEquals(before.select(".atf-ui-assertions").outerHtml(), after.select(".atf-ui-assertions").outerHtml());
        assertEquals(before.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").eachAttr("href"),
                after.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").eachAttr("href"));
        Files.writeString(report, mutated);
        var refusal = assertThrows(IllegalStateException.class, () -> AndroidUiFlowReportPublisher.auditPublishedReport(
                report, trace, AtfReportConfigurationLoader.load().reportingSettings()));
        assertTrue(refusal.getMessage().contains("false driver attribution"), refusal.getMessage());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"true,false", "false,true"})
    void dispatchAndUiComparisonKeepTheirOwnResults(boolean dispatchFailed, boolean comparisonFailed,
            @TempDir Path output) throws Exception {
        Path screenshot = output.resolve("complete-frame.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        var trace = attributionTrace(output, screenshot, dispatchFailed, comparisonFailed);
        assertEquals(AtfReportStatus.FAIL, trace.status(), "A later PASS cannot replace either captured failure");
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("failure.html"),
                "package:/fixture.apk", "mock-device", "28", "Mocked dispatch and comparison", List.of(screenshot), trace);
        var document = Jsoup.parse(Files.readString(report));
        var rows = document.select("table.atf-ui-action-table:not(.atf-ui-observation-table) > tbody > tr");
        assertEquals(2, rows.size());
        assertEquals(dispatchFailed ? 1 : 2, rows.select(".atf-ui-action-result-pass").size());
        assertEquals(dispatchFailed ? 1 : 0, rows.select(".atf-ui-action-result-fail").size());
        assertTrue(rows.get(1).text().contains("text=Continue"));
        if (dispatchFailed) assertTrue(rows.get(1).text().contains("ADB dispatch rejected at Continue"));
        var comparisons = document.selectFirst(".atf-ui-assertions");
        assertNotNull(comparisons);
        assertEquals(comparisonFailed ? "fail" : "pass", comparisons.attr("data-atf-evidence-status"));
        assertTrue(document.selectFirst("details.atf-ui-action-sequence > summary").text().contains("UI action sequence"));
    }

    private static AndroidUiFlowTrace attributionTrace(Path output, Path screenshot,
            boolean dispatchFailed, boolean comparisonFailed) throws Exception {
        return attributionTrace(output, screenshot, dispatchFailed, comparisonFailed, List.of());
    }

    private static AndroidUiFlowTrace attributionTrace(Path output, Path screenshot,
            boolean dispatchFailed, boolean comparisonFailed, List<Path> supporting) throws Exception {
        var trace = new AndroidUiFlowTrace(output, "dispatch-ui-attribution", "Mocked Android dispatch and state",
                "AndroidUiFlowReportPublisherTest", "atf.android.e2e.support", "ro.whistlab.app",
                List.of("android", "ui", "component", "mock"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=Start", "Dispatch accepted", "ADB accepted Start", 7,
                AtfReportStatus.PASS));
        trace.recordAction(new AtfUiActionRecord(2, "Tap", "text=Continue", "Dispatch accepted",
                dispatchFailed ? "ADB dispatch rejected at Continue" : "ADB accepted Continue", 11,
                dispatchFailed ? AtfReportStatus.FAIL : AtfReportStatus.PASS));
        trace.checkpoint("TEST BODY", "THEN", "The captured state has its independent verdict", "Continue", screenshot,
                List.of(new AtfUiActionRecord(3, "Inspect", "screen", "ready", comparisonFailed ? "menu" : "ready", 0,
                        comparisonFailed ? AtfReportStatus.FAIL : AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("State after dispatch", "ready", comparisonFailed ? "menu" : "ready", !comparisonFailed)),
                dispatchFailed || comparisonFailed ? AtfReportStatus.FAIL : AtfReportStatus.PASS, supporting);
        trace.finish(AtfReportStatus.PASS);
        return trace;
    }

    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void publishesEachAndroidCheckpointWithStandardUiTablesAndInlineScreenshot(
            @TempDir Path output)
            throws Exception {
        Path screenshot = output.resolve("bidding-state.png");
        Path hierarchy = output.resolve("bidding-state.xml");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        Files.writeString(hierarchy, "<hierarchy><node text=\"Place bid\"/></hierarchy>");
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, "android-report-contract",
                "Android UI action, screenshot, and assertion contract", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=New game", "New game is opened",
                "ADB accepted tap at 50,25", 34, AtfReportStatus.PASS));
        trace.recordAction(new AtfUiActionRecord(2, "Select option", "text=6", "Six players are selected",
                "ADB accepted tap at 85,45", 41, AtfReportStatus.PASS));
        String completeObservation = "visible label: Players = 6\r\nlayout: compact\r\n"
                + "measured bounds; ".repeat(30) + "final-boundary-retained";
        trace.checkpoint("SETUP", "GIVEN", "Configure the six-player game", "New game setup", screenshot,
                List.of(new AtfUiActionRecord(3, "Inspect setup state", "New game setup",
                        "Players = 6", completeObservation, 0,
                        AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Selected player count is six", "6", "6", true)),
                AtfReportStatus.PASS, List.of(hierarchy));
        trace.finish(AtfReportStatus.PASS);

        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("report.html"),
                "package:/data/app/ro.whistlab.app/base.apk", "emulator-5554", "28",
                "Six-player setup is visible and ready", List.of(screenshot, hierarchy, trace.manifest()), trace);

        String html = Files.readString(report);
        String spacedMarkup = html
                .replace("<details class=\"atf-ui-action-sequence\"",
                        "<details data-format-fixture=\"true\" class=\"atf-ui-action-sequence\"")
                .replace("<th>EXPECTED</th><th>ACTUAL</th>",
                        "<th data-format-fixture=\"true\">EXPECTED</th>\n<th>ACTUAL</th>")
                .replace("<td>CHECKPOINT COUNT</td><td>1</td>",
                        "<td>CHECKPOINT COUNT</td>\n<td>1</td>");
        assertNotEquals(html, spacedMarkup, "The reader control must exercise different markup");
        for (String markup : List.of(html, spacedMarkup)) {
            var document = Jsoup.parse(markup);
            assertTrue(document.select("span").stream().anyMatch(span -> span.wholeText()
                    .equals("GIVEN Configure the six-player game")));
            var actions = document.selectFirst("details.atf-ui-action-sequence");
            assertNotNull(actions);
            assertNotNull(actions.selectFirst("table.atf-ui-action-table"));
            assertTrue(actions.selectFirst("summary").text().contains("UI action sequence (2 entries)"));
            assertTrue(actions.text().contains("text=New game"));
            var comparisons = document.selectFirst(".atf-ui-assertions");
            assertNotNull(comparisons);
            assertTrue(comparisons.text().contains("Selected player count is six"));
            var headers = comparisons.select("thead th").eachText();
            assertTrue(headers.size() >= 3, "The comparison must retain its three result columns");
            assertEquals(List.of("EXPECTED", "ACTUAL", "RESULT"),
                    headers.subList(headers.size() - 3, headers.size()));
            var observations = document.selectFirst("details.atf-ui-observations");
            assertNotNull(observations);
            assertTrue(observations.text().contains("visible label: Players = 6"));
            var screenshotLink = actions.selectFirst("a[data-atf-screenshot]");
            assertNotNull(screenshotLink, "The matching action disclosure must own the screenshot");
            assertEquals("data:image/png;base64," + ONE_PIXEL_PNG, screenshotLink.attr("href"));
            assertEquals("image", screenshotLink.attr("data-featherlight"));
            assertTrue(document.text().contains("bidding-state.xml"));
            assertEquals("android-report-contract", receiptValue(document, "FLOW ID"));
            assertEquals("1", receiptValue(document, "CHECKPOINT COUNT"));
            assertEquals("1", receiptValue(document, "INLINE CHECKPOINT SCREENSHOTS"));
            assertTrue(document.select("details.atf-ui-action-sequence[open],"
                    + "details.atf-ui-observations[open],details.atf-ui-assertions[open]").isEmpty(),
                    "UI evidence disclosures must start collapsed");
            var fullObservation = observations.selectFirst("details.atf-ui-value pre");
            assertNotNull(fullObservation, "Long observations must retain their complete value");
            assertEquals(completeObservation, fullObservation.wholeText());
        }
        Files.writeString(report, html.replace("final-boundary-retained", "final-boundary-corrupted"));
        assertThrows(IllegalStateException.class, () -> AndroidUiFlowReportPublisher.auditPublishedReport(
                report, trace, AtfReportConfigurationLoader.load().reportingSettings()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publishesObservationOnlyCheckpointWithoutInventingADispatchedUiAction(
            boolean identicalCapture, @TempDir Path output)
            throws Exception {
        Path actionScreenshot = output.resolve("action-state.png");
        Path observationScreenshot = output.resolve("observed-state.png");
        writePixelPng(actionScreenshot, 0xFFFF0000);
        writePixelPng(observationScreenshot, identicalCapture ? 0xFFFF0000 : 0xFF0000FF);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, "android-observation-only",
                "Android read-only checkpoint report", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=New game", "Open the bidding screen",
                "ADB accepted tap", 31, AtfReportStatus.PASS));
        trace.checkpoint("TEST BODY", "WHEN", "Open the bidding screen", "New game", actionScreenshot,
                List.of(new AtfUiActionRecord(trace.nextActionSequence(), "Observe screen", "Bidding screen",
                        "Retain the hierarchy for this screen", "Bidding screen captured", 0,
                        AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Bidding screen opened", "visible", "visible", true)),
                AtfReportStatus.PASS, List.of());
        trace.checkpoint("TEST BODY", "THEN", "Visible bid options match the rule domain", "Bidding screen",
                observationScreenshot,
                List.of(new AtfUiActionRecord(trace.nextActionSequence(), "Capture UI observation", "Bidding screen",
                        "[0, 1, 2]", "[0, 1, 2]",
                        0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Displayed choices match the legal bid domain", "[0, 1, 2]",
                        "[0, 1, 2]", true)),
                AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);

        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("observation-only.html"),
                "package:/installed.apk", "emulator-5554", "28", "Read-only bid-domain checkpoint",
                List.of(actionScreenshot, observationScreenshot), trace);

        String html = Files.readString(report);
        String actionPng = Base64.getEncoder().encodeToString(Files.readAllBytes(actionScreenshot));
        String observationPng = Base64.getEncoder().encodeToString(Files.readAllBytes(observationScreenshot));
        var observationLink = assertObservationCheckpoint(html, actionPng, observationPng, identicalCapture);
        String spacedMarkup = html
                .replace("<span>WHEN Open the bidding screen</span>",
                        "<span data-format-fixture=\"action\">WHEN Open the bidding screen</span>")
                .replace("<span>THEN Visible bid options match the rule domain</span>",
                        "<span data-format-fixture=\"observation\">THEN Visible bid options match the rule domain</span>");
        assertNotEquals(html, spacedMarkup, "The owner control must exercise different markup");
        assertObservationCheckpoint(spacedMarkup, actionPng, observationPng, identicalCapture);

        var opening = observationLink.sourceRange();
        var closing = observationLink.endSourceRange();
        assertTrue(opening.isTracked() && closing.isTracked(), "The mutated anchor needs exact source bounds");
        int linkStart = opening.startPos(), linkEnd = closing.endPos();
        String originalLink = html.substring(linkStart, linkEnd);
        assertTrue(originalLink.startsWith("<a ") && originalLink.endsWith("</a>"));
        String brokenLink = observationLink.clone().attr("href", "#atf-screenshot-missing").outerHtml();
        assertNotEquals(originalLink, brokenLink, "The negative probe must mutate this checkpoint's image link");
        Files.writeString(report, html.substring(0, linkStart) + brokenLink + html.substring(linkEnd));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AndroidUiFlowReportPublisher.auditPublishedReport(report, trace,
                        AtfReportConfigurationLoader.load().reportingSettings()));
        assertTrue(failure.getMessage().contains("does not resolve to its exact captured PNG"));
    }

    @Test
    void refusesAssertionValuesThatWouldBeTruncatedInExtent(@TempDir Path output) throws Exception {
        int previewLimit = AtfReportConfigurationLoader.load().reportingSettings().assertionMaxDisplayCharacters();
        Path screenshot = output.resolve("overlong.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        AndroidUiFlowTrace trace = validPassingTrace(output, "overlong-assertion", screenshot,
                "x".repeat(previewLimit + 1));
        Path report = output.resolve("overlong-assertion.html");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AndroidUiFlowReportPublisher.publish(report, "package:/installed.apk", "emulator-5554",
                        "28", "An assertion would be visually truncated", List.of(), trace));

        assertTrue(failure.getMessage().contains("configured " + previewLimit + " character preview"));
        assertFalse(Files.exists(report), "An unauditable preview must not leave a candidate Extent report");
    }

    @Test
    void refusesPassingCheckpointWithoutItsOwnScreenshot(@TempDir Path output) throws Exception {
        AndroidUiFlowTrace trace = validPassingTrace(output, "missing-step-screenshot", null, "6");
        Path report = output.resolve("missing-step-screenshot.html");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AndroidUiFlowReportPublisher.publish(report, "package:/installed.apk", "emulator-5554",
                        "28", "Checkpoint screenshot is absent", List.of(), trace));

        assertTrue(failure.getMessage().contains("has no captured PNG"));
        assertFalse(Files.exists(report), "A screenshot-less checkpoint must not publish as a complete UI flow");
    }

    @Test
    void keepsFailureVisibleWhenNoCheckpointWasCaptured(@TempDir Path output) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, "android-failed-before-checkpoint",
                "Android UI failure before first checkpoint", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=Start game", "The game starts",
                "ADB rejected the tap: device disconnected", 27, AtfReportStatus.FAIL));
        trace.fail(new IllegalStateException("device returned no hierarchy"));
        trace.finish(AtfReportStatus.FAIL);

        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("failed-report.html"),
                "package:/data/app/ro.whistlab.app/base.apk", "emulator-5554", "28",
                "Journey did not reach the first UI checkpoint", List.of(trace.manifest()), trace);

        String html = Files.readString(report);
        assertTrue(html.contains("EXECUTION DIAGNOSTICS"));
        assertTrue(html.contains("device returned no hierarchy"));
        assertTrue(html.contains("Android flow stopped after an uncaught failure"));
        assertTrue(html.contains("text=Start game"));
        assertTrue(html.contains("device disconnected"));
        assertFalse(html.contains("Total: 0"));
    }

    private AndroidUiFlowTrace validPassingTrace(Path output, String id, Path screenshot, String actual)
            throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, id,
                "Android checkpoint report preflight", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=Start game", "Open the game",
                "ADB accepted tap", 23, AtfReportStatus.PASS));
        trace.checkpoint("TEST BODY", "THEN", "Start game is visible", "Whist Lab", screenshot,
                List.of(new AtfUiActionRecord(2, "Inspect state", "Whist Lab", "Start game",
                        "visible: Start game", 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Start action remains available", "Start game", actual, true)),
                AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);
        return trace;
    }

    private static void writePixelPng(Path path, int argb) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, argb);
        }
        assertTrue(ImageIO.write(image, "png", path.toFile()), "The JDK PNG writer must be available");
    }
    private static org.jsoup.nodes.Element assertObservationCheckpoint(
            String markup, String actionPng, String observationPng, boolean identicalCapture) {
        var document = Jsoup.parse(markup, "", org.jsoup.parser.Parser.htmlParser().setTrackPosition(true));
        assertNotNull(document.selectFirst("details.atf-ui-action-sequence"),
                "A real action checkpoint remains in the same flow");
        assertNotNull(document.selectFirst("details.atf-ui-observations"));
        assertNotNull(document.selectFirst(".atf-ui-assertions"));
        assertTrue(document.text().contains("Capture UI observation"));
        assertEquals(1L, document.select("a[data-atf-screenshot=base64]").stream()
                .filter(anchor -> anchor.attr("href").equals("data:image/png;base64," + actionPng)).count(),
                "The action checkpoint screenshot must be embedded exactly once");
        assertEquals(1L, document.select("a[data-atf-screenshot=base64]").stream()
                .filter(anchor -> anchor.attr("href").equals("data:image/png;base64," + observationPng)).count(),
                "The read-only checkpoint screenshot must be embedded exactly once");
        var actionOwner = checkpointLabel(document, "WHEN Open the bidding screen");
        var observationOwner = checkpointLabel(document, "THEN Visible bid options match the rule domain");
        var ordered = document.getAllElements();
        int actionStart = ordered.indexOf(actionOwner), observationStart = ordered.indexOf(observationOwner);
        assertTrue(actionStart >= 0 && observationStart > actionStart);
        var actionEvidence = ordered.subList(actionStart, observationStart);
        assertTrue(actionEvidence.stream().anyMatch(element -> element.normalName().equals("a")
                && element.attr("href").equals("data:image/png;base64," + actionPng)),
                "The real UI action must own its own captured screen");
        var observationLink = ordered.subList(observationStart, ordered.size()).stream()
                .filter(element -> element.normalName().equals("a") && element.hasAttr("data-atf-screenshot-ref"))
                .findFirst().orElseThrow(() -> new AssertionError("The read-only checkpoint needs its own screenshot control"));
        String reference = observationLink.attr("data-atf-screenshot-ref");
        var canonical = document.getElementById("atf-screenshot-" + reference);
        assertNotNull(canonical, "The screenshot reference must resolve inside this report");
        assertEquals("data:image/png;base64," + observationPng, canonical.attr("href"));
        assertEquals(identicalCapture ? "#atf-screenshot-" + reference : "data:image/png;base64," + observationPng,
                observationLink.attr("href"));
        assertTrue(document.select("details.atf-ui-observations[open],details.atf-ui-assertions[open]").isEmpty());
        return observationLink;
    }

    private static org.jsoup.nodes.Element checkpointLabel(org.jsoup.nodes.Document document, String title) {
        return document.select("span").stream().filter(span -> span.text().equals(title)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing checkpoint label: " + title));
    }

    private static String receiptValue(org.jsoup.nodes.Document document, String label) {
        return document.select("tr").stream()
                .filter(row -> row.childrenSize() == 2 && row.child(0).text().equals(label))
                .map(row -> row.child(1).text())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Android execution receipt field: " + label));
    }
}

package atf.android.e2e.support;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.ui.UiExtentReportSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Publishes ordered Android checkpoints through the same public UI evidence API used by Playwright. */
public final class AndroidUiFlowReportPublisher {
    private AndroidUiFlowReportPublisher() { }

    public static Path publish(Path report, String installedPackage, String device, String androidApi,
            String summary, List<Path> files, AndroidUiFlowTrace trace) throws Exception {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(trace, "trace");
        List<Path> evidenceFiles = files == null ? List.of() : List.copyOf(files);
        Path output = report.toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        ReportingSettings settings = AtfReportConfigurationLoader.load().reportingSettings();
        validateCheckpointContract(trace,
                settings.assertionMaxDisplayCharacters());

        try (var session = new ExtentReportSession(output, settings)) {
            AtfReportPorts.Scenario raw = session.startScenario(
                    trace.testClass(), trace.testPackage(), trace.name(), trace.tags());
            publishRunReceipt(raw, trace, installedPackage, device, androidApi, summary, evidenceFiles);

            var ui = UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    trace.name());
            for (AndroidUiFlowTrace.PhaseSnapshot phaseSnapshot : trace.phaseSnapshots()) {
                try (var phase = ui.startPhase(phaseSnapshot.name())) {
                    for (var checkpoint : phaseSnapshot.checkpoints()) {
                        publishCheckpoint(phase, checkpoint);
                    }
                    phase.complete(phaseStatus(phaseSnapshot.checkpoints()));
                }
            }
            if (!trace.failure().isBlank()) {
                try (var failurePhase = ui.startPhase("EXECUTION DIAGNOSTICS")) {
                    failurePhase.failureEvidence("Android flow stopped after an uncaught failure",
                            trace.name(), trace.pendingActions(), new IllegalStateException(trace.failure()));
                    failurePhase.complete(AtfReportStatus.FAIL);
                }
            }
            raw.complete(trace.status());
        }

        auditPublishedReport(output, trace, settings);
        return output;
    }

    private static void publishCheckpoint(UiExtentReportSession.UiPhase phase,
            AndroidUiFlowTrace.CheckpointSnapshot checkpoint) throws Exception {
        String screenshot = checkpoint.screenshot() == null ? ""
                : AndroidScreenshotEvidence.from(checkpoint.screenshot()).base64Png();
        phase.step(AtfGherkinKeyword.valueOf(checkpoint.keyword()),
                checkpoint.title(), checkpoint.target(), screenshot,
                checkpoint.assertions(), checkpoint.actions(),
                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, checkpoint.status());
    }

    private static void publishRunReceipt(AtfReportPorts.Scenario scenario, AndroidUiFlowTrace trace,
            String installedPackage, String device, String androidApi, String summary, List<Path> files)
            throws Exception {
        int checkpointCount = checkpointCount(trace);
        String fingerprints = files.stream()
                .filter(file -> !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .map(AndroidUiFlowReportPublisher::fingerprintLine)
                .collect(java.util.stream.Collectors.joining("\n"));
        try (var receipt = scenario.startPhase("Device, run receipt, and supporting Android evidence")) {
            receipt.observation(new ReportObservation("Android UI execution receipt",
                    List.of(new ReportField("Flow ID", trace.id()),
                            new ReportField("Device", valueOrUnknown(device)),
                            new ReportField("Android API", valueOrUnknown(androidApi)),
                            new ReportField("Application package", trace.appPackage()),
                            new ReportField("Installed APK", valueOrUnknown(installedPackage).trim()),
                            new ReportField("Checkpoint count", Integer.toString(checkpointCount)),
                            new ReportField("Inline checkpoint screenshots", Integer.toString(screenshotCount(trace))),
                            new ReportField("Screenshot policy", "One full-display PNG at every captured checkpoint"),
                            new ReportField("Observed journey", valueOrUnknown(summary)),
                            new ReportField("Execution failure", trace.failure().isBlank() ? "none" : trace.failure())),
                    fingerprints.isBlank() ? List.of() : List.of(new ReportObservation.Body(
                            "Supporting Android hierarchy/manifest SHA-256 fingerprints", fingerprints,
                            ReportObservation.Format.TEXT)), List.of()));
            receipt.complete(trace.status());
        }
    }

    private static AtfReportStatus phaseStatus(List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints) {
        return checkpoints.stream().map(AndroidUiFlowTrace.CheckpointSnapshot::status)
                .reduce(AtfReportStatus.INFO, AtfReportStatus::worst);
    }

    static void auditPublishedReport(Path report, AndroidUiFlowTrace trace,
            ReportingSettings settings) throws Exception {
        require(Files.isRegularFile(report) && Files.size(report) > 2_000,
                "Android UI Extent report was not published: " + report);
        String html = Files.readString(report);
        Document document = Jsoup.parse(html);
        require(html.contains("Device, run receipt, and supporting Android evidence"),
                "Extent omitted the Android execution receipt");
        require(containsText(html, trace.id())
                        && hasReceiptField(html, "CHECKPOINT COUNT", Integer.toString(checkpointCount(trace)))
                        && hasReceiptField(html, "INLINE CHECKPOINT SCREENSHOTS", Integer.toString(screenshotCount(trace))),
                "Extent omitted the run identity or checkpoint/screenshot audit counts");
        require(!html.toLowerCase(Locale.ROOT).contains("href=\"file:"),
                "Android UI report must not publish workstation-local file links");
        List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints = trace.phaseSnapshots().stream()
                .flatMap(phase -> phase.checkpoints().stream()).toList();
        long actionTableCount = checkpoints.stream().filter(checkpoint -> checkpoint.actions().stream()
                .anyMatch(action -> action.source().name().equals("ACTION"))).count();
        long observationTableCount = checkpoints.stream().filter(checkpoint -> checkpoint.actions().stream()
                .anyMatch(action -> action.source().name().equals("OBSERVE"))).count();
        long assertionTableCount = checkpoints.stream().filter(checkpoint -> !checkpoint.assertions().isEmpty()).count();
        if (!trace.failure().isBlank()) {
            if (trace.pendingActions().stream().anyMatch(action -> action.source().name().equals("ACTION"))) {
                actionTableCount++;
            }
            observationTableCount++;
            assertionTableCount++;
        }
        require(countOccurrences(html, "class=\"atf-ui-action-sequence\"") == actionTableCount,
                "Extent action-table count does not match the checkpoint owners");
        require(countOccurrences(html, "class=\"atf-ui-observations\"") == observationTableCount,
                "Extent observation-table count does not match the checkpoint owners");
        require(countOccurrences(html, "class=\"atf-ui-assertions\"") == assertionTableCount,
                "Extent assertion-table count does not match the checkpoint owners");
        require(!html.contains("<details class=\"atf-ui-action-sequence\" open")
                        && !html.contains("<details class=\"atf-ui-observations\" open")
                        && !html.contains("<details class=\"atf-ui-assertions\" open"),
                "UI evidence disclosures must be collapsed by default");

        java.util.Map<String, Integer> screenshotOccurrences = new java.util.HashMap<>();
        List<AndroidUiFlowTrace.CheckpointSnapshot> orderedCheckpoints = checkpoints;
        int ownerSearchFrom = 0;
        SensitiveDataRedactor redactor = new SensitiveDataRedactor();
        for (int index = 0; index < orderedCheckpoints.size(); index++) {
            AndroidUiFlowTrace.CheckpointSnapshot checkpoint = orderedCheckpoints.get(index);
            String titleMarker = checkpointTitleMarker(checkpoint);
            int ownerStart = html.indexOf(titleMarker, ownerSearchFrom);
            require(ownerStart >= 0, "Extent omitted checkpoint " + checkpoint.title());
            int ownerEnd = html.length();
            if (index + 1 < orderedCheckpoints.size()) {
                int nextOwner = html.indexOf(checkpointTitleMarker(orderedCheckpoints.get(index + 1)),
                        ownerStart + titleMarker.length());
                require(nextOwner >= 0, "Extent omitted the next ordered checkpoint after " + checkpoint.title());
                ownerEnd = nextOwner;
            }
            int diagnosticStart = html.indexOf("EXECUTION DIAGNOSTICS", ownerStart + titleMarker.length());
            if (diagnosticStart >= 0 && diagnosticStart < ownerEnd) ownerEnd = diagnosticStart;
            String ownerHtml = html.substring(ownerStart, ownerEnd);
            long checkpointActionCount = checkpoint.actions().stream()
                    .filter(action -> action.source().name().equals("ACTION")).count();
            require(countOccurrences(ownerHtml, "class=\"atf-ui-action-sequence\"")
                            == (checkpointActionCount == 0 ? 0 : 1)
                            && countOccurrences(ownerHtml, "class=\"atf-ui-observations\"") == 1
                            && countOccurrences(ownerHtml, "class=\"atf-ui-assertions\"") == 1,
                    "Checkpoint evidence tables do not match the captured action, observation, and assertion owners: "
                            + checkpoint.title());
            String actionDisclosure = detailsBlock(ownerHtml, "atf-ui-action-sequence");
            String observationDisclosure = detailsBlock(ownerHtml, "atf-ui-observations");
            require((checkpointActionCount == 0 || actionDisclosure != null) && observationDisclosure != null,
                    "Checkpoint evidence is detached from its Gherkin owner: " + checkpoint.title());
            for (var action : checkpoint.actions()) {
                String actionEvidence = action.source().name().equals("ACTION")
                        ? actionDisclosure : observationDisclosure;
                boolean operationVisible = containsText(actionEvidence, action.operation());
                boolean targetVisible = containsText(actionEvidence, action.target());
                boolean expectedVisible = containsText(actionEvidence, action.expected());
                boolean observedVisible = containsText(actionEvidence, action.observed());
                boolean statusVisible = containsText(actionEvidence, action.status().marker())
                        || containsText(actionEvidence, action.status().displayValue())
                        || containsText(actionEvidence, action.status().displayLabel());
                boolean rowIsVisible = operationVisible && targetVisible && expectedVisible
                        && observedVisible && statusVisible;
                boolean measuredDispatchIsVisible = action.source().name().equals("OBSERVE")
                        || containsText(actionEvidence, action.durationText());
                require(rowIsVisible && measuredDispatchIsVisible,
                        "Extent omitted an action/observation row from its owning checkpoint: "
                                + checkpoint.title() + " / " + action.operation()
                                + " [operation=" + operationVisible + ", target=" + targetVisible
                                + ", expected=" + expectedVisible + ", observed=" + observedVisible
                                + ", status=" + statusVisible + ", duration=" + measuredDispatchIsVisible + "]");
            }
            for (var assertion : checkpoint.assertions()) {
                String row = assertionRow(assertion, settings, redactor);
                require(ownerHtml.contains(row),
                        "Extent omitted or detached the complete evaluated assertion row from "
                                + checkpoint.title() + ": " + assertion.name());
            }
            if (checkpoint.screenshot() != null) {
                String payload = AndroidScreenshotEvidence.from(checkpoint.screenshot()).base64Png();
                boolean actionOwnsScreenshot = actionDisclosure != null
                        && ownsScreenshot(actionDisclosure, payload, document);
                boolean observationCheckpointOwnsScreenshot = checkpointActionCount == 0
                        && ownsScreenshot(ownerHtml, payload, document);
                require(actionOwnsScreenshot || observationCheckpointOwnsScreenshot,
                        "The checkpoint screenshot link does not resolve to its exact captured PNG: "
                                + checkpoint.title());
                screenshotOccurrences.merge(payload, 1, Integer::sum);
            }
            ownerSearchFrom = ownerEnd;
        }
        for (var screenshot : screenshotOccurrences.entrySet()) {
            long links = document.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").stream()
                    .filter(anchor -> screenshot.getKey().equals(screenshotPayload(anchor, document))).count();
            require(countOccurrences(html, "data:image/png;base64," + screenshot.getKey()) == 1
                            && links == screenshot.getValue(),
                    "Extent screenshot payload or checkpoint reference multiplicity differs from the captured evidence");
        }
        if (trace.status() == AtfReportStatus.PASS) {
            require(trace.phaseSnapshots().stream().mapToInt(phase -> phase.checkpoints().size()).sum() > 0,
                    "A PASS Android UI flow must contain at least one captured checkpoint");
        }
        if (!trace.failure().isBlank()) {
            require(html.contains("EXECUTION DIAGNOSTICS") && containsText(html, trace.failure()),
                    "Extent omitted the captured Android failure diagnostic");
            for (var action : trace.pendingActions()) {
                require(containsText(html, action.operation()) && containsText(html, action.target())
                                && containsText(html, action.expected()) && containsText(html, action.observed())
                                && containsText(html, Long.toString(action.durationMillis()))
                                && (containsText(html, action.status().displayValue())
                                        || containsText(html, action.status().displayLabel())),
                        "Extent omitted the uncheckpointed failure action/observation " + action.operation()
                                + " targeting " + action.target());
            }
        }
    }

    private static void validateCheckpointContract(AndroidUiFlowTrace trace, int assertionPreviewLimit) {
        List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints = trace.phaseSnapshots().stream()
                .flatMap(phase -> phase.checkpoints().stream()).toList();
        for (AndroidUiFlowTrace.CheckpointSnapshot checkpoint : checkpoints) {
            require(checkpoint.screenshot() != null && Files.isRegularFile(checkpoint.screenshot()),
                    "Android checkpoint has no captured PNG: " + checkpoint.title());
            require(checkpoint.actions().stream().anyMatch(action -> action.source().name().equals("OBSERVE")),
                    "Android checkpoint has no recorded UI observation: " + checkpoint.title());
            require(!checkpoint.assertions().isEmpty(),
                    "Android checkpoint has no evaluated assertions: " + checkpoint.title());
            for (var assertion : checkpoint.assertions()) {
                require(assertion.expected().length() <= assertionPreviewLimit,
                        "Assertion expected value exceeds Extent's configured " + assertionPreviewLimit
                                + " character preview: " + assertion.name());
                require(assertion.actual().length() <= assertionPreviewLimit,
                        "Assertion actual value exceeds Extent's configured " + assertionPreviewLimit
                                + " character preview: " + assertion.name()
                                + "; put complete diagnostic values in the expandable observation table");
            }
        }
    }

    private static String checkpointTitleMarker(AndroidUiFlowTrace.CheckpointSnapshot checkpoint) {
        return "<span>" + checkpoint.keyword() + " "
                + AtfReportHtmlVocabulary.escapeHtml(checkpoint.title()) + "</span>";
    }

    private static boolean ownsScreenshot(String ownerHtml, String payload, Document report) {
        return Jsoup.parseBodyFragment(ownerHtml).select("a[data-atf-screenshot],a[data-atf-screenshot-ref]")
                .stream().anyMatch(anchor -> payload.equals(screenshotPayload(anchor, report)));
    }

    private static String screenshotPayload(Element anchor, Document report) {
        String href = anchor.attr("href");
        String prefix = "data:image/png;base64,";
        if (href.startsWith(prefix)) return href.substring(prefix.length());
        String reference = anchor.attr("data-atf-screenshot-ref");
        if (reference.isBlank() || !href.equals("#atf-screenshot-" + reference)) return "";
        Element canonical = report.getElementById("atf-screenshot-" + reference);
        if (canonical == null || !reference.equals(canonical.attr("data-atf-screenshot-ref"))) return "";
        String canonicalHref = canonical.attr("href");
        return canonicalHref.startsWith(prefix) ? canonicalHref.substring(prefix.length()) : "";
    }

    private static String detailsBlock(String ownerHtml, String className) {
        String opening = "<details class=\"" + className + "\"";
        int start = ownerHtml.indexOf(opening);
        if (start < 0) return null;
        int end = ownerHtml.indexOf("</details>", start);
        return end < 0 ? null : ownerHtml.substring(start, end + "</details>".length());
    }

    private static String assertionRow(atf.reporting.contract.AtfAssertion assertion,
            ReportingSettings settings, SensitiveDataRedactor redactor) {
        String name = redactor.redactText(assertion.name());
        String expected = ExtentReportNodeSupport.display(
                redactor.redactField(assertion.name(), assertion.expected()), settings);
        String actual = ExtentReportNodeSupport.display(
                redactor.redactField(assertion.name(), assertion.actual()), settings);
        return "<tr><td>" + AtfReportHtmlVocabulary.escapeHtml(name) + "</td><td>"
                + AtfReportHtmlVocabulary.escapeHtml(expected) + "</td><td>"
                + AtfReportHtmlVocabulary.escapeHtml(actual) + "</td><td>"
                + assertion.resultStatus().marker() + "</td></tr>";
    }

    private static String fingerprintLine(Path file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            return file.getFileName() + "  sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not fingerprint Android UI evidence " + file, failure);
        }
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "not reported" : value;
    }

    private static int checkpointCount(AndroidUiFlowTrace trace) {
        return trace.phaseSnapshots().stream().mapToInt(phase -> phase.checkpoints().size()).sum();
    }

    private static int screenshotCount(AndroidUiFlowTrace trace) {
        return trace.phaseSnapshots().stream().flatMap(phase -> phase.checkpoints().stream())
                .mapToInt(checkpoint -> checkpoint.screenshot() == null ? 0 : 1).sum();
    }

    private static boolean hasReceiptField(String html, String label, String value) {
        return html.contains("<td>" + label.toUpperCase(Locale.ROOT) + "</td><td>" + value + "</td>");
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0; index += token.length()) count++;
        return count;
    }

    private static boolean containsText(String html, String value) {
        return html.contains(value) || html.contains(value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;").replace("\r", "&#13;"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

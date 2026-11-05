package atf.android.e2e.support;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.security.SensitiveDataRedactor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Verifică fidelitatea raportului publicat față de datele capturate, fără sesiune de randare. */
final class AndroidUiFlowReportAudit {
    private AndroidUiFlowReportAudit() {}

    private static void auditActionAttribution(Document document) {
        int ordinal = 0;
        for (Element action : document.select("details.atf-ui-action-sequence")) {
            ordinal++;
            Element summary = action.children().stream()
                    .filter(child -> child.normalName().equals("summary")).findFirst().orElse(null);
            String title = summary == null ? "" : summary.text();
            require(title.contains("UI action sequence") && !title.contains("Playwright action sequence"),
                    "Android ACTION table #" + ordinal + " has false driver attribution: " + title);
        }
    }

    static void auditPublishedReport(Path report, AndroidUiFlowTrace trace,
            ReportingSettings settings) throws Exception {
        require(Files.isRegularFile(report) && Files.size(report) > 2_000,
                "Android UI Extent report was not published: " + report);
        String html = Files.readString(report);
        Document document = Jsoup.parse(html);
        auditActionAttribution(document);
        AndroidPhaseTimingAudit.verify(document, trace.phaseSnapshots());
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
            Element actionDisclosure = detailsBlock(ownerHtml, "atf-ui-action-sequence");
            Element observationDisclosure = detailsBlock(ownerHtml, "atf-ui-observations");
            require((checkpointActionCount == 0 || actionDisclosure != null) && observationDisclosure != null,
                    "Checkpoint evidence is detached from its Gherkin owner: " + checkpoint.title());
            for (var action : checkpoint.actions()) {
                Element actionEvidence = action.source().name().equals("ACTION")
                        ? actionDisclosure : observationDisclosure;
                Element row = actionEvidence.select("tbody tr").stream()
                        .filter(candidate -> candidate.select(".atf-ui-action-number").text()
                                .equals("#" + action.sequence())).findFirst().orElse(null);
                require(row != null && row.childrenSize() >= 5,
                        "Extent omitted the numbered UI evidence row: " + checkpoint.title() + " / #" + action.sequence());
                var cells = row.children();
                Element operation = cells.get(0).selectFirst(".atf-ui-action-operation");
                boolean operationVisible = operation != null
                        && operation.wholeText().equals(redactor.redactText(action.operation()));
                boolean targetVisible = cellValueEquals(cells.get(1), redactor.redactText(action.target()));
                boolean expectedVisible = cellValueEquals(cells.get(2), redactor.redactText(action.expected()));
                boolean observedVisible = cellValueEquals(cells.get(3), redactor.redactText(action.observed()));
                String resultCell = cells.last().html();
                boolean statusVisible = containsText(resultCell, action.status().marker())
                        || containsText(resultCell, action.status().displayValue())
                        || containsText(resultCell, action.status().displayLabel());
                boolean rowIsVisible = operationVisible && targetVisible && expectedVisible
                        && observedVisible && statusVisible;
                boolean measuredDispatchIsVisible = action.source().name().equals("OBSERVE")
                        || containsText(cells.get(4).html(), action.durationText());
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
                        && ownsScreenshot(actionDisclosure.outerHtml(), payload, document);
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

    private static String checkpointTitleMarker(AndroidUiFlowTrace.CheckpointSnapshot checkpoint) {
        return "<span>" + checkpoint.keyword() + " "
                + AtfReportHtmlVocabulary.escapeHtml(checkpoint.title()) + "</span>";
    }

    private static boolean ownsScreenshot(String ownerHtml, String payload, Document report) {
        return Jsoup.parseBodyFragment(ownerHtml).select("a[data-atf-screenshot],a[data-atf-screenshot-ref]")
                .stream().anyMatch(anchor -> payload.equals(screenshotPayload(anchor, report)));
    }

    private static boolean cellValueEquals(Element cell, String expected) {
        Element completeValue = cell.selectFirst("details.atf-ui-value pre");
        return (completeValue == null ? cell : completeValue).wholeText().equals(expected);
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

    private static Element detailsBlock(String ownerHtml, String className) {
        return Jsoup.parseBodyFragment(ownerHtml).selectFirst("details." + className);
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

    static int checkpointCount(AndroidUiFlowTrace trace) {
        return trace.phaseSnapshots().stream().mapToInt(phase -> phase.checkpoints().size()).sum();
    }

    static int screenshotCount(AndroidUiFlowTrace trace) {
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

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

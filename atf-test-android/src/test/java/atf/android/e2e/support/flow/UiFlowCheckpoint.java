package atf.android.e2e.support.flow;

import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.nio.file.Path;
import java.util.List;

/** Publică un checkpoint cu sursele vizuale și aserțiunile sale. */
public final class UiFlowCheckpoint {
    private UiFlowCheckpoint() {}

    public static void checkpoint(
            AndroidUiFlowTrace trace,
            String phase,
            AtfGherkinKeyword keyword,
            String title,
            String target,
            List<Path> evidence,
            String operation,
            String expected,
            String observed,
            long durationMillis,
            List<AtfAssertion> assertions,
            AtfReportStatus status)
            throws Exception {
        Path screenshot =
                evidence.stream()
                        .filter(
                                file ->
                                        file.toString()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .endsWith(".png"))
                        .findFirst()
                        .orElse(null);
        List<Path> supporting =
                evidence.stream()
                        .filter(
                                file ->
                                        file.toString()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .endsWith(".xml"))
                        .toList();
        AtfUiActionRecord action =
                new AtfUiActionRecord(
                        trace.nextActionSequence(),
                        operation,
                        target,
                        expected,
                        observed,
                        Math.max(0, durationMillis),
                        status,
                        AtfUiEvidenceSource.OBSERVE);
        trace.checkpoint(
                phase,
                keyword.text(),
                title,
                target,
                screenshot,
                List.of(action),
                assertions,
                status,
                supporting);
    }
}

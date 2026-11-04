package atf.android.e2e.support;

import java.util.List;
import org.jsoup.nodes.Document;

/** Checks captured owners and exact original nanoseconds independently of visible badge formatting. */
final class AndroidPhaseTimingAudit {
    private AndroidPhaseTimingAudit() { }

    static void verify(Document document, List<AndroidUiFlowTrace.PhaseSnapshot> phases) {
        var expected = phases.stream().filter(phase -> phase.timing().isPresent()).toList();
        var actual = document.select("[data-atf-execution-duration-nanos]");
        require(actual.size() == expected.size(), "Original Android timing population differs");
        for (int index = 0; index < expected.size(); index++) {
            var phase = expected.get(index);
            var timing = phase.timing().orElseThrow();
            var marker = actual.get(index);
            var owner = marker.closest(".card");
            var title = owner == null ? null : owner.selectFirst(".card-header");
            require(title != null && title.text().contains(phase.name()), "Original Android timing moved to another phase");
            require(marker.attr("data-atf-execution-duration-nanos").equals(Long.toString(timing.executionNanos())),
                    "Original Android execution interval differs: " + phase.name());
            require(marker.attr("data-atf-execution-intervals").equals(Integer.toString(timing.intervals().size())),
                    "Original Android interval count differs: " + phase.name());
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}

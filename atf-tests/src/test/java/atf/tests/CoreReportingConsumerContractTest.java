package atf.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.core.reporting.duration.AtfDurationParser;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreReportingConsumerContractTest {
    @Test
    void consumesCoreDurationParsingContract() {
        assertEquals(62_003L, AtfDurationParser.parse("01:02:003").orElseThrow());
        assertEquals(301L, AtfDurationParser.parse("0.301 s").orElseThrow());
        assertTrue(AtfDurationParser.parse("unknown").isEmpty());
    }

    @Test
    void reportingLibraryPublishesAConsumerHtmlReport(@TempDir Path tempDir) throws Exception {
        Path report = tempDir.resolve("consumer-contract.html");
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new HtmlReportSession(report, settings);
             var scenario = session.startScenario("J4 Core and Reporting consumer contract", "atf.tests",
                     "local-consumer", List.of("component", "reporting"));
             var phase = scenario.startPhase("Publish an Extent HTML result")) {
            phase.observation(new ReportObservation("Consumer integration",
                    List.of(new ReportField("Core duration parsing", "PASS"),
                            new ReportField("Reporting output path", report.toString())), List.of(), List.of()));
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        assertTrue(Files.isRegularFile(report), "Reporting did not create the consumer HTML report");
        assertTrue(Files.size(report) > 2_000, "Reporting created an unexpectedly small HTML file");
        String html = Files.readString(report);
        assertTrue(html.contains("J4 Core and Reporting consumer contract"));
        assertTrue(html.contains("Extent HTML result"));
    }
}

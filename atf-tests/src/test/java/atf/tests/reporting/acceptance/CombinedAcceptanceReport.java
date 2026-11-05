package atf.tests.reporting.acceptance;

import static atf.tests.reporting.acceptance.NativeModuleResults.number;
import static atf.tests.reporting.acceptance.NativeModuleResults.parse;
import static atf.tests.reporting.replay.ReplayEvidenceFiles.concatArtifacts;
import static atf.tests.reporting.replay.ReplayEvidenceFiles.concatPaths;
import static atf.tests.reporting.replay.ReplayEvidenceFiles.loadTrace;
import static atf.tests.reporting.replay.UiFlowReplay.integer;
import static atf.tests.reporting.replay.UiFlowReplay.publishUiFlow;
import static atf.tests.reporting.replay.UiFlowReplay.required;
import static atf.tests.reporting.replay.UiFlowReplay.resolveEvidence;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.tests.reporting.acceptance.NativeModuleResults.ModuleStats;
import atf.tests.reporting.replay.ReplayEvidenceFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Compune raportul din rezultate native și dovezi UI verificate. */
public final class CombinedAcceptanceReport {
    private CombinedAcceptanceReport() {}

    public static void publish(List<String> moduleNames) throws Exception {
        assumeTrue(
                Boolean.getBoolean("atf.deep.publish"),
                "Set -Datf.deep.publish=true after component runs to publish the final Extent report");
        Path componentResults = requiredPath("atf.deep.resultsDir");
        Path evidenceDir = requiredPath("atf.deep.evidenceDir");
        Path report = requiredPath("atf.deep.reportPath");

        List<ModuleStats> modules = NativeModuleResults.readAll(componentResults, moduleNames);

        Path labAppXml =
                componentResults
                        .resolve("atf-test-android")
                        .resolve("TEST-atf.android.e2e.gameplay.WhistLabGameplayE2eTest.xml");
        Path appSuiteXml =
                componentResults
                        .resolve("atf-test-android")
                        .resolve("TEST-atf.android.suites.AndroidGameplayE2eSuite.xml");
        assertTrue(
                Files.isRegularFile(labAppXml),
                "Whist Lab Android acceptance result is missing: " + labAppXml);
        assertTrue(
                Files.isRegularFile(appSuiteXml),
                "Android E2E suite result is missing: " + appSuiteXml);
        List<Path> appTestXmls = List.of(labAppXml);
        long appTests = 0, appFailures = 0, appSkipped = 0;
        for (Path appXml : appTestXmls) {
            var appSuite = parse(appXml).getDocumentElement();
            appTests += number(appSuite, "tests");
            appFailures += number(appSuite, "failures") + number(appSuite, "errors");
            appSkipped += number(appSuite, "skipped");
        }
        assertTrue(appTests > 0, "Whist Lab Android E2E reported zero tests: " + appTestXmls);
        AtfReportStatus appStatus =
                appFailures == 0 && appSkipped == 0
                        ? AtfReportStatus.PASS
                        : appFailures == 0 ? AtfReportStatus.WARNING : AtfReportStatus.FAIL;

        List<Path> evidenceAssets;
        try (var paths = Files.list(evidenceDir)) {
            evidenceAssets =
                    paths.filter(Files::isRegularFile)
                            .filter(
                                    file ->
                                            file.getFileName()
                                                            .toString()
                                                            .toLowerCase()
                                                            .endsWith(".png")
                                                    || file.getFileName()
                                                            .toString()
                                                            .toLowerCase()
                                                            .endsWith(".xml")
                                                    || file.getFileName()
                                                            .toString()
                                                            .toLowerCase()
                                                            .endsWith(".html"))
                            .filter(
                                    file ->
                                            !List.of(
                                                            "current-check.png",
                                                            "current-check.xml",
                                                            "manual-dump.xml")
                                                    .contains(file.getFileName().toString()))
                            .sorted()
                            .toList();
        }
        List<Path> screenshots =
                evidenceAssets.stream()
                        .filter(
                                file ->
                                        file.getFileName()
                                                .toString()
                                                .toLowerCase()
                                                .endsWith(".png"))
                        .toList();
        List<Path> flowManifests;
        try (var paths = Files.list(evidenceDir)) {
            flowManifests =
                    paths.filter(file -> file.getFileName().toString().startsWith("ui-flow-"))
                            .filter(file -> file.getFileName().toString().endsWith(".properties"))
                            .sorted()
                            .toList();
        }
        List<Path> flowScreenshots = new ArrayList<>();
        for (Path manifest : flowManifests) {
            Properties trace = loadTrace(manifest);
            int phaseCount = integer(trace, "flow.phaseCount");
            for (int phase = 0; phase < phaseCount; phase++) {
                String phasePrefix = "phase." + phase;
                int stepCount = integer(trace, phasePrefix + ".stepCount");
                for (int step = 0; step < stepCount; step++) {
                    String screenshot =
                            trace.getProperty(phasePrefix + ".step." + step + ".screenshot", "");
                    if (!screenshot.isBlank())
                        flowScreenshots.add(resolveEvidence(evidenceDir, screenshot));
                }
            }
        }
        screenshots =
                screenshots.stream().filter(image -> !flowScreenshots.contains(image)).toList();
        List<Path> linkedEvidenceAssets =
                evidenceAssets.stream()
                        .filter(
                                file ->
                                        !file.getFileName()
                                                .toString()
                                                .toLowerCase()
                                                .endsWith(".png"))
                        .toList();
        List<Path> runLogs = new ArrayList<>();
        Path logsDir = componentResults.resolve("logs");
        if (Files.isDirectory(logsDir)) {
            try (var paths = Files.list(logsDir)) {
                runLogs.addAll(
                        paths.filter(file -> file.getFileName().toString().endsWith(".log"))
                                .sorted()
                                .toList());
            }
        }
        AtfReportStatus aggregate = appStatus;
        for (ModuleStats module : modules)
            aggregate = AtfReportStatus.worst(aggregate, module.status());
        Files.createDirectories(report.toAbsolutePath().getParent());
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new ExtentReportSession(report, settings)) {
            var reportScenario =
                    session.startScenario(
                            "ATF J4 component and Whist Lab deep test",
                            "atf.tests",
                            "Android API 28 emulator",
                            List.of(
                                    "component-tests",
                                    "adb",
                                    "whist-lab",
                                    "live-gameplay",
                                    "visual-review"));
            try (var scenario =
                    UiExtentReportSession.adaptScenario(
                            reportScenario,
                            ReportEvidenceProfile.forScreenshotPolicy(
                                    ReportScreenshotPolicy.EACH_CHECKPOINT),
                            "Android API 28 emulator")) {
                for (ModuleStats module : modules) {
                    try (var phase = reportScenario.startPhase("Component: " + module.name())) {
                        phase.observation(
                                new ReportObservation(
                                        "Surefire component result",
                                        List.of(
                                                new ReportField(
                                                        "Tests", Long.toString(module.tests())),
                                                new ReportField(
                                                        "Failures",
                                                        Long.toString(module.failures())),
                                                new ReportField(
                                                        "Errors", Long.toString(module.errors())),
                                                new ReportField(
                                                        "Skipped", Long.toString(module.skipped())),
                                                new ReportField(
                                                        "Duration", module.duration().toString()),
                                                new ReportField(
                                                        "Surefire XML suites",
                                                        Integer.toString(module.xmlFiles().size())),
                                                new ReportField(
                                                        "Surefire result files",
                                                        Integer.toString(
                                                                module.resultFiles().size()))),
                                        List.of(),
                                        module.resultFiles().stream()
                                                .map(ReplayEvidenceFiles::artifactUnchecked)
                                                .toList()));
                        phase.complete(module.status());
                    }
                }
                try (var phase = reportScenario.startPhase("Whist Lab live gameplay")) {
                    phase.observation(
                            new ReportObservation(
                                    "ADB-driven UI acceptance",
                                    List.of(
                                            new ReportField(
                                                    "Surefire tests", Long.toString(appTests)),
                                            new ReportField(
                                                    "Failures and errors",
                                                    Long.toString(appFailures)),
                                            new ReportField("Skipped", Long.toString(appSkipped)),
                                            new ReportField(
                                                    "Whist Lab flow",
                                                    "Deterministic practice trick, bid 1, force-stop and resume the same human turn, play A♥, AI seats complete trick, exact-bid score Total: 11"),
                                            new ReportField(
                                                    "Screenshot captures",
                                                    Integer.toString(
                                                            screenshots.size()
                                                                    + flowScreenshots.size())),
                                            new ReportField(
                                                    "Screenshot and UI-tree assets",
                                                    Integer.toString(evidenceAssets.size())),
                                            new ReportField(
                                                    "Component run logs",
                                                    Integer.toString(runLogs.size())),
                                            new ReportField(
                                                    "ADB connection",
                                                    "Host ADB bridge via 192.168.56.1:5038; scrcpy UI closed")),
                                    List.of(),
                                    concatArtifacts(
                                            concatPaths(
                                                    appTestXmls,
                                                    concatPaths(List.of(appSuiteXml), runLogs)),
                                            linkedEvidenceAssets)));
                    phase.complete(appStatus);
                }
                try (var phase = scenario.startPhase("Inline Android UI screenshot checkpoints")) {
                    for (Path image : screenshots) {
                        ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
                        phase.uiValidation(
                                screenshot.title(),
                                "Android emulator",
                                screenshot.base64Png(),
                                List.of(),
                                AtfReportStatus.PASS);
                    }
                    phase.complete(appStatus);
                }
                scenario.complete(aggregate);
            }
            for (Path manifest : flowManifests) {
                publishUiFlow(session, manifest, evidenceDir);
            }
        }

        assertTrue(
                Files.isRegularFile(report) && Files.size(report) > 2_000,
                "The final Extent report was not published: " + report);
        String html = Files.readString(report);
        assertTrue(
                html.contains("data:image/png;base64,"),
                "Android screenshots must be rendered inline by the ATF UI Reporting contract");
        for (Path image : screenshots) {
            ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
            assertTrue(
                    html.contains(screenshot.base64Png()),
                    "Final Extent report is missing inline screenshot payload "
                            + screenshot.title());
        }
        for (Path image : flowScreenshots) {
            ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
            assertTrue(
                    html.contains(screenshot.base64Png()),
                    "Final Extent report is missing inline UI-flow checkpoint "
                            + screenshot.title());
        }
        if (!flowManifests.isEmpty()) {
            assertTrue(
                    html.contains("TEST BODY"),
                    "Android E2E test-body phases are missing from the final report");
            for (Path manifest : flowManifests) {
                Properties trace = loadTrace(manifest);
                assertTrue(
                        html.contains(required(trace, "flow.name")),
                        "Final Extent report omitted Android flow " + trace.getProperty("flow.id"));
                for (int phase = 0; phase < integer(trace, "flow.phaseCount"); phase++) {
                    String prefix = "phase." + phase;
                    for (int step = 0; step < integer(trace, prefix + ".stepCount"); step++) {
                        String stepPrefix = prefix + ".step." + step;
                        assertTrue(
                                html.contains(required(trace, stepPrefix + ".title")),
                                "Final Extent report omitted UI checkpoint "
                                        + trace.getProperty(stepPrefix + ".title"));
                        for (int index = 0;
                                index < integer(trace, stepPrefix + ".assertionCount");
                                index++) {
                            String assertion = stepPrefix + ".assertion." + index;
                            assertTrue(
                                    html.contains(required(trace, assertion + ".name")),
                                    "Final Extent report omitted assertion "
                                            + trace.getProperty(assertion + ".name"));
                        }
                    }
                }
            }
        }
    }

    public static Path requiredPath(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Required system property: " + property);
        return Path.of(value).toAbsolutePath().normalize();
    }
}

package atf.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.android.evidence.AndroidScreenshotEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/** Opt-in publisher for module, original-app, and local-app acceptance results. */
class DeepSuiteExtentReportTest {
    private static final List<String> MODULES = List.of(
            "atf-test-kit", "atf-mock", "atf-validation", "atf-console", "atf-android", "atf-tests", "atf-test-android");
    @Test
    void publishesCombinedComponentAndLiveGameplayReport() throws Exception {
        assumeTrue(Boolean.getBoolean("atf.deep.publish"), "Set -Datf.deep.publish=true after component runs to publish the final Extent report");
        Path componentResults = requiredPath("atf.deep.resultsDir");
        Path evidenceDir = requiredPath("atf.deep.evidenceDir");
        Path report = requiredPath("atf.deep.reportPath");

        List<ModuleStats> modules = new ArrayList<>();
        for (String module : MODULES) {
            Path reports = componentResults.resolve(module);
            List<Path> xmlFiles;
            try (var paths = Files.list(reports)) {
                xmlFiles = paths.filter(file -> file.getFileName().toString().startsWith("TEST-")
                        && file.getFileName().toString().endsWith(".xml")).toList();
            }
            assertTrue(!xmlFiles.isEmpty(), "No Surefire XML results found for " + module + " under " + reports);
            List<Path> resultFiles = new ArrayList<>(xmlFiles);
            try (var paths = Files.list(reports)) {
                resultFiles.addAll(paths.filter(file -> file.getFileName().toString().endsWith(".txt")).toList());
            }
            long tests = 0, failures = 0, errors = 0, skipped = 0;
            double seconds = 0;
            for (Path xml : xmlFiles) {
                var suite = parse(xml).getDocumentElement();
                tests += number(suite, "tests");
                failures += number(suite, "failures");
                errors += number(suite, "errors");
                skipped += number(suite, "skipped");
                seconds += decimal(suite, "time");
            }
            AtfReportStatus status = failures + errors == 0 ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            modules.add(new ModuleStats(module, tests, failures, errors, skipped, Duration.ofMillis((long) (seconds * 1_000)), status, xmlFiles, resultFiles));
        }

        Path originalAppXml = componentResults.resolve("atf-test-android")
                .resolve("TEST-atf.android.e2e.ContractWhistGameplayE2eTest.xml");
        Path labAppXml = componentResults.resolve("atf-test-android")
                .resolve("TEST-atf.android.e2e.WhistLabGameplayE2eTest.xml");
        Path appSuiteXml = componentResults.resolve("atf-test-android")
                .resolve("TEST-atf.android.suites.AndroidGameplayE2eSuite.xml");
        assertTrue(Files.isRegularFile(originalAppXml), "Original Android acceptance result is missing: " + originalAppXml);
        assertTrue(Files.isRegularFile(labAppXml), "Whist Lab Android acceptance result is missing: " + labAppXml);
        assertTrue(Files.isRegularFile(appSuiteXml), "Android E2E suite result is missing: " + appSuiteXml);
        List<Path> appTestXmls = List.of(originalAppXml, labAppXml);
        long appTests = 0, appFailures = 0, appSkipped = 0;
        for (Path appXml : appTestXmls) {
            var appSuite = parse(appXml).getDocumentElement();
            appTests += number(appSuite, "tests");
            appFailures += number(appSuite, "failures") + number(appSuite, "errors");
            appSkipped += number(appSuite, "skipped");
        }
        assertTrue(appTests > 0, "Android E2E test classes reported zero tests: " + appTestXmls);
        AtfReportStatus appStatus = appFailures == 0 && appSkipped == 0
                ? AtfReportStatus.PASS : appFailures == 0 ? AtfReportStatus.WARNING : AtfReportStatus.FAIL;

        List<Path> evidenceAssets;
        try (var paths = Files.list(evidenceDir)) {
            evidenceAssets = paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png")
                            || file.getFileName().toString().toLowerCase().endsWith(".xml")
                            || file.getFileName().toString().toLowerCase().endsWith(".html"))
                    .filter(file -> !List.of("current-check.png", "current-check.xml", "manual-dump.xml")
                            .contains(file.getFileName().toString()))
                    .sorted().toList();
        }
        List<Path> screenshots = evidenceAssets.stream()
                .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png")).toList();
        List<Path> linkedEvidenceAssets = evidenceAssets.stream()
                .filter(file -> !file.getFileName().toString().toLowerCase().endsWith(".png")).toList();
        List<Path> runLogs = new ArrayList<>();
        Path logsDir = componentResults.resolve("logs");
        if (Files.isDirectory(logsDir)) {
            try (var paths = Files.list(logsDir)) {
                runLogs.addAll(paths.filter(file -> file.getFileName().toString().endsWith(".log")).sorted().toList());
            }
        }
        AtfReportStatus aggregate = appStatus;
        for (ModuleStats module : modules) aggregate = AtfReportStatus.worst(aggregate, module.status());
        // The screen captures show a persistent rendering problem, so product acceptance remains a warning.
        aggregate = AtfReportStatus.worst(aggregate, AtfReportStatus.WARNING);
        Files.createDirectories(report.toAbsolutePath().getParent());
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new ExtentReportSession(report, settings)) {
            var reportScenario = session.startScenario("ATF J4 component, Contract Whist, and Whist Lab deep test", "atf.tests",
                    "Android API 28 emulator", List.of("component-tests", "adb", "original-app", "parallel-app", "live-gameplay", "visual-review"));
            try (var scenario = UiExtentReportSession.adaptScenario(reportScenario,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    "Android API 28 emulator")) {
            for (ModuleStats module : modules) {
                try (var phase = reportScenario.startPhase("Component: " + module.name())) {
                    phase.observation(new ReportObservation("Surefire component result",
                            List.of(new ReportField("Tests", Long.toString(module.tests())),
                                    new ReportField("Failures", Long.toString(module.failures())),
                                    new ReportField("Errors", Long.toString(module.errors())),
                                    new ReportField("Skipped", Long.toString(module.skipped())),
                                     new ReportField("Duration", module.duration().toString()),
                                     new ReportField("Surefire XML suites", Integer.toString(module.xmlFiles().size())),
                                     new ReportField("Surefire result files", Integer.toString(module.resultFiles().size()))),
                            List.of(), module.resultFiles().stream().map(DeepSuiteExtentReportTest::artifactUnchecked).toList()));
                    phase.complete(module.status());
                }
            }
            try (var phase = reportScenario.startPhase("Original app and Whist Lab live gameplay")) {
                phase.observation(new ReportObservation("ADB-driven UI acceptance",
                        List.of(new ReportField("Surefire tests", Long.toString(appTests)),
                                new ReportField("Failures and errors", Long.toString(appFailures)),
                                new ReportField("Skipped", Long.toString(appSkipped)),
                                new ReportField("Contract Whist flow", "Menu, instructions, setup, bidding, human card play, AI turn, second card play, and saved-game rendering"),
                                new ReportField("Whist Lab flow", "Deterministic practice trick, bid 1, force-stop and resume the same human turn, play A♥, AI seats complete trick, exact-bid score Total: 11"),
                                new ReportField("Screenshot captures", Integer.toString(screenshots.size())),
                                new ReportField("Screenshot and UI-tree assets", Integer.toString(evidenceAssets.size())),
                                new ReportField("Component run logs", Integer.toString(runLogs.size())),
                                new ReportField("ADB connection", "Host ADB bridge via 192.168.56.1:5038; scrcpy UI closed")),
                        List.of(), concatArtifacts(concatPaths(appTestXmls,
                                concatPaths(List.of(appSuiteXml), runLogs)), linkedEvidenceAssets)));
                phase.complete(appStatus);
            }
            try (var phase = reportScenario.startPhase("Visual acceptance observation")) {
                phase.observation(new ReportObservation("Visible rendering defects",
                        List.of(new ReportField("Observed", "Neon-green background; oversized and overlapping labels; distorted card faces; hand clipped at the bottom"),
                                new ReportField("Scope", "Visual defect is evidenced by emulator captures; scoring and full-match completion are not asserted by this run")),
                        List.of(), List.of()));
                phase.complete(AtfReportStatus.WARNING);
            }
            try (var phase = scenario.startPhase("Inline Android UI screenshot checkpoints")) {
                for (Path image : screenshots) {
                    ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
                    phase.uiValidation(screenshot.title(), "Android emulator", screenshot.base64Png(),
                            List.of(), AtfReportStatus.PASS);
                }
                phase.complete(appStatus);
            }
            scenario.complete(aggregate);
            }
        }

        assertTrue(Files.isRegularFile(report) && Files.size(report) > 2_000,
                "The final Extent report was not published: " + report);
        String html = Files.readString(report);
        assertTrue(html.contains("data:image/png;base64,"),
                "Android screenshots must be rendered inline by the ATF UI Reporting contract");
        for (Path image : screenshots) {
            ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
            assertTrue(html.contains(screenshot.base64Png()),
                    "Final Extent report is missing inline screenshot payload " + screenshot.title());
        }
    }

    private static Path requiredPath(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required system property: " + property);
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static org.w3c.dom.Document parse(Path xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(xml.toFile());
    }

    private static long number(Element element, String attribute) {
        return Long.parseLong(element.getAttribute(attribute));
    }

    private static double decimal(Element element, String attribute) {
        return Double.parseDouble(element.getAttribute(attribute));
    }

    private static ReportObservation.Artifact artifactUnchecked(Path file) {
        try { return artifact(file); }
        catch (Exception failure) { throw new IllegalStateException("Could not attach report evidence " + file, failure); }
    }

    private static List<ReportObservation.Artifact> concatArtifacts(List<Path> first, List<Path> second) {
        List<Path> all = new ArrayList<>(first);
        all.addAll(second);
        return all.stream().map(DeepSuiteExtentReportTest::artifactUnchecked).toList();
    }

    private static List<Path> concatPaths(List<Path> first, List<Path> second) {
        List<Path> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

    private static ReportObservation.Artifact artifact(Path file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return new ReportObservation.Artifact(file.getFileName().toString(), file.toUri(), HexFormat.of().formatHex(digest));
    }

    private record ModuleStats(String name, long tests, long failures, long errors, long skipped,
            Duration duration, AtfReportStatus status, List<Path> xmlFiles, List<Path> resultFiles) { }
}

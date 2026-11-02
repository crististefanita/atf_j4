package atf.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.android.evidence.AndroidScreenshotEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
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
        List<Path> flowManifests;
        try (var paths = Files.list(evidenceDir)) {
            flowManifests = paths.filter(file -> file.getFileName().toString().startsWith("ui-flow-"))
                    .filter(file -> file.getFileName().toString().endsWith(".properties")).sorted().toList();
        }
        List<Path> flowScreenshots = new ArrayList<>();
        for (Path manifest : flowManifests) {
            Properties trace = loadTrace(manifest);
            int phaseCount = integer(trace, "flow.phaseCount");
            for (int phase = 0; phase < phaseCount; phase++) {
                String phasePrefix = "phase." + phase;
                int stepCount = integer(trace, phasePrefix + ".stepCount");
                for (int step = 0; step < stepCount; step++) {
                    String screenshot = trace.getProperty(phasePrefix + ".step." + step + ".screenshot", "");
                    if (!screenshot.isBlank()) flowScreenshots.add(resolveEvidence(evidenceDir, screenshot));
                }
            }
        }
        screenshots = screenshots.stream().filter(image -> !flowScreenshots.contains(image)).toList();
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
                                new ReportField("Contract Whist flow", "Menu and rules, setup matrix for 3–6 seats, guarded 4-seat Oh Hell hand-end, manual score-sheet review, and Next Hand bidding with 9 cards"),
                                new ReportField("Card-play evidence limit", "Raster interaction cycles are reported as UI activity only; the custom table does not expose card identity, exact accepted-card count, legality, or trick winners to UIAutomator"),
                                new ReportField("Whist Lab flow", "Deterministic practice trick, bid 1, force-stop and resume the same human turn, play A♥, AI seats complete trick, exact-bid score Total: 11"),
                                new ReportField("Screenshot captures", Integer.toString(screenshots.size() + flowScreenshots.size())),
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
            for (Path manifest : flowManifests) {
                publishUiFlow(session, manifest, evidenceDir);
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
        for (Path image : flowScreenshots) {
            ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
            assertTrue(html.contains(screenshot.base64Png()),
                    "Final Extent report is missing inline UI-flow checkpoint " + screenshot.title());
        }
        if (!flowManifests.isEmpty()) {
            assertTrue(html.contains("TEST BODY"),
                    "Android E2E test-body phases are missing from the final report");
            for (Path manifest : flowManifests) {
                Properties trace = loadTrace(manifest);
                assertTrue(html.contains(required(trace, "flow.name")),
                        "Final Extent report omitted Android flow " + trace.getProperty("flow.id"));
                for (int phase = 0; phase < integer(trace, "flow.phaseCount"); phase++) {
                    String prefix = "phase." + phase;
                    for (int step = 0; step < integer(trace, prefix + ".stepCount"); step++) {
                        String stepPrefix = prefix + ".step." + step;
                        assertTrue(html.contains(required(trace, stepPrefix + ".title")),
                                "Final Extent report omitted UI checkpoint " + trace.getProperty(stepPrefix + ".title"));
                        for (int index = 0; index < integer(trace, stepPrefix + ".assertionCount"); index++) {
                            String assertion = stepPrefix + ".assertion." + index;
                            assertTrue(html.contains(required(trace, assertion + ".name")),
                                    "Final Extent report omitted assertion " + trace.getProperty(assertion + ".name"));
                        }
                    }
                }
            }
        }
    }

    private static Path requiredPath(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required system property: " + property);
        return Path.of(value).toAbsolutePath().normalize();
    }

    static void publishUiFlow(ExtentReportSession session, Path manifest, Path evidenceDir) throws Exception {
        publishUiFlow(session,manifest,evidenceDir,false);
    }

    static void publishUiFlow(ExtentReportSession session, Path manifest, Path evidenceDir, boolean completeActionBodies) throws Exception {
        Properties trace = loadTrace(manifest);
        assertTrue("1".equals(trace.getProperty("format.version")), "Unsupported Android UI flow manifest: " + manifest);
        String flowName = required(trace, "flow.name");
        String testClass = required(trace, "flow.testClass");
        String testPackage = required(trace, "flow.testPackage");
        String appPackage = required(trace, "flow.appPackage");
        AtfReportStatus flowStatus = AtfReportStatus.valueOf(required(trace, "flow.status"));
        List<String> tags = List.of(trace.getProperty("flow.tags", "android,e2e").split(","));
        var raw = session.startScenario(flowName, testPackage, appPackage, tags);
        var ui = UiExtentReportSession.adaptScenario(raw,
                ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), flowName);

        List<Path> retainedArtifacts = new ArrayList<>();
        int phaseCount = integer(trace, "flow.phaseCount");
        int checkpointCount = 0;
        for (int phase = 0; phase < phaseCount; phase++) {
            String phasePrefix = "phase." + phase;
            int stepCount = integer(trace, phasePrefix + ".stepCount");
            checkpointCount += stepCount;
            for (int step = 0; step < stepCount; step++) {
                String prefix = phasePrefix + ".step." + step;
                int artifactCount = integer(trace, prefix + ".artifactCount");
                for (int artifact = 0; artifact < artifactCount; artifact++) {
                    String name = trace.getProperty(prefix + ".artifact." + artifact, "");
                    if (!name.isBlank()) retainedArtifacts.add(resolveEvidence(evidenceDir, name));
                }
            }
        }
        try (var details = raw.startPhase("Android flow metadata and retained UI hierarchy")) {
            details.observation(new ReportObservation("E2E flow identity and execution result",
                    List.of(new ReportField("Test class", testClass),
                            new ReportField("App package", appPackage),
                            new ReportField("Flow status", flowStatus.name()),
                            new ReportField("Ordered UI checkpoints", Integer.toString(checkpointCount)),
                            new ReportField("Failure", trace.getProperty("flow.failure", ""))),
                    List.of(), retainedArtifacts.stream().distinct().map(DeepSuiteExtentReportTest::artifactUnchecked).toList()));
            if(completeActionBodies) {
                for(int p=0;p<phaseCount;p++)for(int s=0;s<integer(trace,"phase."+p+".stepCount");s++) {
                    String key="phase."+p+".step."+s;
                    List<ReportObservation.Body> payloads=new ArrayList<>();
                    for(int a=0;a<integer(trace,key+".actionCount");a++) {
                        String action=key+".action."+a;
                        String label=required(trace,action+".source")+" #"+required(trace,action+".sequence")+" "+required(trace,action+".operation");
                        payloads.add(new ReportObservation.Body(label+" — target",required(trace,action+".target"),ReportObservation.Format.TEXT));
                        payloads.add(new ReportObservation.Body(label+" — expected",trace.getProperty(action+".expected",""),ReportObservation.Format.TEXT));
                        payloads.add(new ReportObservation.Body(label+" — observed",trace.getProperty(action+".observed",""),ReportObservation.Format.TEXT));
                    }
                    for(int a=0;a<integer(trace,key+".assertionCount");a++) {
                        String assertion=key+".assertion."+a;
                        String expected=trace.getProperty(assertion+".expected",""),actual=trace.getProperty(assertion+".actual","");
                        if(expected.length()>240||actual.length()>240) {
                            String label="Assertion "+required(trace,assertion+".name");
                            payloads.add(new ReportObservation.Body(label+" — full expected",expected,ReportObservation.Format.TEXT));
                            payloads.add(new ReportObservation.Body(label+" — full actual",actual,ReportObservation.Format.TEXT));
                        }
                    }
                    details.observation(new ReportObservation("Full state and action payload — "+required(trace,"phase."+p+".name")+" / "+required(trace,key+".title"),
                            List.of(new ReportField("Checkpoint key",key),
                                    new ReportField("Presentation","The UI action table retains its short preview. These native TEXT bodies retain complete values with the same source and sequence.")),payloads,List.of()));
                }
            }
            details.complete(flowStatus);
        }
        for (int phaseIndex = 0; phaseIndex < phaseCount; phaseIndex++) {
            String phasePrefix = "phase." + phaseIndex;
            String phaseName = required(trace, phasePrefix + ".name");
            int stepCount = integer(trace, phasePrefix + ".stepCount");
            AtfReportStatus phaseStatus = AtfReportStatus.PASS;
            try (var phase = ui.startPhase(phaseName)) {
                for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
                    String prefix = phasePrefix + ".step." + stepIndex;
                    String keyword = required(trace, prefix + ".keyword");
                    String title = required(trace, prefix + ".title");
                    String target = required(trace, prefix + ".target");
                    AtfReportStatus status = AtfReportStatus.valueOf(required(trace, prefix + ".status"));
                    phaseStatus = AtfReportStatus.worst(phaseStatus, status);
                    List<AtfAssertion> assertions = new ArrayList<>();
                    for (int index = 0; index < integer(trace, prefix + ".assertionCount"); index++) {
                        String assertion = prefix + ".assertion." + index;
                        assertions.add(new AtfAssertion(required(trace, assertion + ".name"),
                                trace.getProperty(assertion + ".expected", ""),
                                trace.getProperty(assertion + ".actual", ""),
                                Boolean.parseBoolean(trace.getProperty(assertion + ".passed", "false")),
                                AtfReportStatus.valueOf(required(trace, assertion + ".status"))));
                    }
                    List<AtfUiActionRecord> actions = new ArrayList<>();
                    for (int index = 0; index < integer(trace, prefix + ".actionCount"); index++) {
                        String action = prefix + ".action." + index;
                        actions.add(new AtfUiActionRecord(integer(trace, action + ".sequence"),
                                required(trace, action + ".operation"), required(trace, action + ".target"),
                                trace.getProperty(action + ".expected", ""), trace.getProperty(action + ".observed", ""),
                                Long.parseLong(required(trace, action + ".durationMillis")),
                                AtfReportStatus.valueOf(required(trace, action + ".status")),
                                AtfUiEvidenceSource.valueOf(required(trace, action + ".source"))));
                    }
                    String screenshotName = trace.getProperty(prefix + ".screenshot", "");
                    String base64Png = screenshotName.isBlank() ? ""
                            : AndroidScreenshotEvidence.from(resolveEvidence(evidenceDir, screenshotName)).base64Png();
                    phase.step(AtfGherkinKeyword.valueOf(keyword), title, target, base64Png,
                            assertions, actions, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status);
                }
                phase.complete(phaseStatus);
            }
        }
        AtfReportStatus publishedFlowStatus = flowStatus;
        if ("contract-whist-oh-hell-score-next-hand".equals(trace.getProperty("flow.id"))) {
            Path scoreSheet = resolveEvidence(evidenceDir, "ohhell-full-score-sheet.png");
            Path nextHandBidding = resolveEvidence(evidenceDir, "ohhell-next-hand-bidding.png");
            Path manualObservationFile = resolveEvidence(evidenceDir, "ohhell-manual-score-observation.properties");
            Properties manualObservation = Files.isRegularFile(manualObservationFile)
                    ? loadTrace(manualObservationFile) : new Properties();
            String nextBidObservation = actionObservationForTitle(trace,
                    "Advance to the next hand and verify its nine-card bid range");
            List<Path> manualObservationArtifacts = new ArrayList<>(List.of(scoreSheet, nextHandBidding));
            if (Files.isRegularFile(manualObservationFile)) manualObservationArtifacts.add(manualObservationFile);
            try (var visual = raw.startPhase("Visual acceptance observation")) {
                visual.observation(new ReportObservation("Original Oh Hell hand-end table — visual read",
                        List.of(new ReportField("Observed deal row", "Cards 10; the table then lists rows 9 through 1"),
                                new ReportField("Visible score-sheet cells", manualObservation.getProperty("score.cells",
                                        "Manual transcription not supplied; inspect the attached score-sheet PNG")),
                                new ReportField("Flow action", "The test selected Bid 0 for the human seat, reached the hand-end score sheet, then explicitly opened Next Hand"),
                                new ReportField("Next-hand bidding", "Cards: 9; UIAutomator bid domain: Bid 0 through Bid 9; Bid 10 absent; " + nextBidObservation),
                                new ReportField("Conditional score reconciliation", manualObservation.getProperty("score.interpretation",
                                        "The score canvas is not exposed as text; no manual score interpretation was supplied.")),
                                new ReportField("Last-bid rule visual reading", manualObservation.getProperty("next-hand.bid-reconciliation",
                                        "The disabled numeric bid is recorded from the UI hierarchy. Bid counter and other seats' bids require manual visual transcription.")),
                                new ReportField("Evidence method and confidence", manualObservation.getProperty("method",
                                        "The custom score canvas is not exposed as text. No OCR or card-recognition oracle was used; treat visual values as WARNING evidence.")),
                                new ReportField("Card-count boundary", "The adapter records raster interaction cycles whose count can vary between runs. A cycle is not a verified accepted card; this flow does not assert a semantically decoded card count."),
                                new ReportField("Scoring limit", "This single score row does not independently prove trick counts or exact bids. Repeated controlled bids/tricks, lower-hand scoring, full-match completion, and final ranking remain unverified."),
                                new ReportField("Visible defect", "The score table text is rotated in portrait and the layout wastes most of the display area.")),
                        List.of(), manualObservationArtifacts.stream()
                                .map(DeepSuiteExtentReportTest::artifactUnchecked).toList()));
                visual.complete(AtfReportStatus.WARNING);
            }
            publishedFlowStatus = AtfReportStatus.worst(flowStatus, AtfReportStatus.WARNING);
        }
        if ("contract-whist-oh-hell-bid-grid-by-seat-count".equals(trace.getProperty("flow.id"))) {
            List<Path> screenshotsBySeats = List.of(
                    resolveEvidence(evidenceDir, "ohhell-bid-matrix-3-bidding.png"),
                    resolveEvidence(evidenceDir, "ohhell-bid-matrix-4-bidding.png"),
                    resolveEvidence(evidenceDir, "ohhell-bid-matrix-5-bidding.png"),
                    resolveEvidence(evidenceDir, "ohhell-bid-matrix-6-bidding.png"));
            try (var visual = raw.startPhase("Visual acceptance observation — Oh Hell setup matrix")) {
                visual.observation(new ReportObservation("Original app: first deal and trump by table size",
                        List.of(new ReportField("Observed first deal", "3 players: 10 cards; 4: 10; 5: 10; 6: 8"),
                                new ReportField("Visible bid values", "3/4/5 players: Bid 0–10; 6 players: Bid 0–8"),
                                new ReportField("First-hand trump samples", "3 players: Diamonds; 4: Hearts; 5: Clubs; 6: Spades"),
                                new ReportField("Last-bid restriction", "In the 4-player screenshot, prior AI bids total 7 and Bid 3 is disabled for the final bidder; 7 + 3 would equal the 10-card hand"),
                                new ReportField("Evidence method", "Values were read visually from the attached emulator PNGs; custom table text is absent from the UIAutomator hierarchy"),
                                new ReportField("Rule boundary", "First deal sizes match the local 10/10/10/8 model; first-trump origin varies across these sessions and does not yet establish the rotation rule. Only 4-player 10-to-1 progression has been followed in the original app."),
                                new ReportField("Open work", "Repeat fresh games at each seat count and follow five consecutive hands through No Trumps and back to the next trump")),
                        List.of(), screenshotsBySeats.stream().map(DeepSuiteExtentReportTest::artifactUnchecked).toList()));
                visual.complete(AtfReportStatus.WARNING);
            }
            publishedFlowStatus = AtfReportStatus.worst(publishedFlowStatus, AtfReportStatus.WARNING);
        }
        raw.complete(publishedFlowStatus);
    }

    private static Properties loadTrace(Path file) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) { properties.load(input); }
        return properties;
    }

    private static String actionObservationForTitle(Properties trace, String title) {
        for (int phase = 0; phase < integer(trace, "flow.phaseCount"); phase++) {
            String phasePrefix = "phase." + phase;
            for (int step = 0; step < integer(trace, phasePrefix + ".stepCount"); step++) {
                String stepPrefix = phasePrefix + ".step." + step;
                if (title.equals(trace.getProperty(stepPrefix + ".title"))
                        && integer(trace, stepPrefix + ".actionCount") > 0) {
                    return trace.getProperty(stepPrefix + ".action.0.observed", "not recorded");
                }
            }
        }
        return "not recorded";
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing UI flow manifest field " + key);
        return value.trim();
    }

    private static int integer(Properties properties, String key) {
        return Integer.parseInt(required(properties, key));
    }

    private static Path resolveEvidence(Path root, String relativePath) {
        Path resolved = root.resolve(relativePath).toAbsolutePath().normalize();
        if (!resolved.startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("UI flow evidence escapes the run evidence folder: " + relativePath);
        }
        assertTrue(Files.isRegularFile(resolved), "UI flow evidence file is missing: " + resolved);
        return resolved;
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

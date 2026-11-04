package atf.tests.publishing;

import atf.testkit.AtfIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.core.reporting.duration.AtfDurationParser;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfIntegrationTest
class CoreReportingConsumerContractTest {
    @Test
    void combinedAcceptanceRequestsResultsOnlyForDeclaredSourceModules() throws Exception {
        Path root = atf.libraries.PlatformLibrarySurface.workspace();
        var modules = new java.util.HashSet<String>();
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var declared = factory.newDocumentBuilder().parse(root.resolve("pom.xml").toFile()).getElementsByTagName("module");
        for (int index = 0; index < declared.getLength(); index++) modules.add(declared.item(index).getTextContent().strip());
        var catalog = DeepSuiteExtentReportTest.class.getDeclaredField("MODULES"); catalog.setAccessible(true);
        var actual = (List<?>) catalog.get(null);
        assertEquals(modules, new java.util.HashSet<>(actual), "External libraries have producer-native acceptance, not local Surefire XML");
        assertEquals(modules.size(), actual.size(), "Combined results must not duplicate a source module");
    }


    @Test
    void androidInfrastructureAndGameEvidenceLoadFromTheSameModule() {
        var owner = atf.android.whist.evidence.WhistExtentFeedback.class
                .getProtectionDomain().getCodeSource().getLocation();
        for (Class<?> type : List.of(atf.android.AdbClient.class,
                atf.android.evidence.AndroidScreenshotEvidence.class,
                atf.android.evidence.timing.AndroidPhaseClock.class,
                atf.android.evidence.timing.AndroidPhaseTiming.class,
                atf.android.evidence.timing.AndroidPhaseTimingCodec.class,
                atf.android.evidence.timing.AndroidPhaseReportTiming.class)) {
            assertEquals(owner, type.getProtectionDomain().getCodeSource().getLocation(),
                    "Android infrastructure must belong to the consumer module: " + type.getName());
        }
    }

    @Test
    void allLibrariesLoadTheDeclaredSingleFlatJar() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("library-provenance.json"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Missing J4 library provenance root");
        String pom = Files.readString(root.resolve("pom.xml"));
        for (String artifact : List.of("core", "reporting", "console", "mock")) {
            var version = java.util.regex.Pattern.compile("<atf\\." + artifact + "\\.version>([^<]+)</atf\\." + artifact + "\\.version>").matcher(pom);
            assertTrue(version.find(), "Declared library coordinate: " + artifact);
            String name = "atf-" + artifact + "-" + version.group(1) + ".jar";
            Class<?> entry = switch (artifact) {
                case "core" -> AtfDurationParser.class;
                case "reporting" -> ExtentReportSession.class;
                case "console" -> atf.console.AtfConsoleReporter.class;
                default -> atf.mock.AtfMockTransport.class;
            };
            Path loaded = Path.of(entry.getProtectionDomain().getCodeSource().getLocation().toURI());
            assertTrue(Files.isRegularFile(loaded), "Consumer must load a physical JAR: " + loaded);
            assertEquals(name, loaded.getFileName().toString());
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            assertEquals(java.util.HexFormat.of().formatHex(digest.digest(Files.readAllBytes(root.resolve("lib").resolve(name)))),
                    java.util.HexFormat.of().formatHex(digest.digest(Files.readAllBytes(loaded))));
            try (var files = Files.list(root.resolve("lib"))) {
                assertEquals(1, files.filter(file -> file.getFileName().toString().matches("atf-" + artifact + "-.*\\.jar")).count());
            }
        }
    }

    @Test
    void consumesCoreDurationParsingContract() {
        assertEquals(62_003L, AtfDurationParser.parse("01:02:003").orElseThrow());
        assertEquals(301L, AtfDurationParser.parse("0.301 s").orElseThrow());
        assertTrue(AtfDurationParser.parse("unknown").isEmpty());
    }

    @Test
    void reportingLibraryPublishesAConsumerExtentReport(@TempDir Path tempDir) throws Exception {
        Path report = tempDir.resolve("consumer-contract.html");
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new ExtentReportSession(report, settings);
             var scenario = session.startScenario("J4 Core and Reporting consumer contract", "atf.tests",
                     "local-consumer", List.of("component", "reporting"));
             var phase = scenario.startPhase("Publish an Extent result")) {
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
        assertTrue(html.contains("Publish an Extent result"));
    }
}

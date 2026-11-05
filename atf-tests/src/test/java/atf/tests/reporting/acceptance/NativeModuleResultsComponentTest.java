package atf.tests.reporting.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@AtfComponentTest
class NativeModuleResultsComponentTest {
    @Test
    void aggregationKeepsFailuresErrorsSkipsDurationsAndSourceFiles(@TempDir Path root)
            throws Exception {
        Path module = Files.createDirectories(root.resolve("fixture-module"));
        Files.writeString(
                module.resolve("TEST-first.xml"),
                "<testsuite tests=\"2\" failures=\"1\" errors=\"0\" skipped=\"1\" time=\"0.500\"/>");
        Files.writeString(
                module.resolve("TEST-second.xml"),
                "<testsuite tests=\"3\" failures=\"0\" errors=\"1\" skipped=\"0\" time=\"1.250\"/>");
        Files.writeString(module.resolve("console.txt"), "retained native diagnostic");
        var summary = NativeModuleResults.readAll(root, List.of("fixture-module")).getFirst();
        assertEquals(5, summary.tests());
        assertEquals(1, summary.failures());
        assertEquals(1, summary.errors());
        assertEquals(1, summary.skipped());
        assertEquals(Duration.ofMillis(1750), summary.duration());
        assertEquals(AtfReportStatus.FAIL, summary.status());
        assertEquals(2, summary.xmlFiles().size());
        assertEquals(3, summary.resultFiles().size());
    }

    @Test
    void absenceOfNativeResultsAndExternalEntityDocumentsAreRefused(@TempDir Path root)
            throws Exception {
        Path module = Files.createDirectories(root.resolve("fixture-module"));
        assertThrows(
                AssertionError.class,
                () -> NativeModuleResults.readAll(root, List.of("fixture-module")));
        Files.writeString(
                module.resolve("TEST-invalid.xml"),
                "<!DOCTYPE testsuite SYSTEM \"file:///absent-atf-canary.dtd\"><testsuite tests=\"1\"/>");
        assertThrows(
                SAXParseException.class,
                () -> NativeModuleResults.readAll(root, List.of("fixture-module")));
    }
}

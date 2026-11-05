package atf.tests.reporting.acceptance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.reporting.contract.AtfReportStatus;

import org.w3c.dom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Citește rezultatele native fără a pierde eșecuri, omisiuni sau fișiere sursă. */
public final class NativeModuleResults {
    private NativeModuleResults() {}

    public static org.w3c.dom.Document parse(Path xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(xml.toFile());
    }

    public static long number(Element element, String attribute) {
        return Long.parseLong(element.getAttribute(attribute));
    }

    public static double decimal(Element element, String attribute) {
        return Double.parseDouble(element.getAttribute(attribute));
    }

    public record ModuleStats(
            String name,
            long tests,
            long failures,
            long errors,
            long skipped,
            Duration duration,
            AtfReportStatus status,
            List<Path> xmlFiles,
            List<Path> resultFiles) {}

    public static List<ModuleStats> readAll(Path componentResults, List<String> moduleNames)
            throws Exception {
        List<ModuleStats> modules = new ArrayList<>();
        for (String module : moduleNames) {
            Path reports = componentResults.resolve(module);
            List<Path> xmlFiles;
            try (var paths = Files.list(reports)) {
                xmlFiles =
                        paths.filter(
                                        file ->
                                                file.getFileName().toString().startsWith("TEST-")
                                                        && file.getFileName()
                                                                .toString()
                                                                .endsWith(".xml"))
                                .toList();
            }
            assertTrue(
                    !xmlFiles.isEmpty(),
                    "No Surefire XML results found for " + module + " under " + reports);
            List<Path> resultFiles = new ArrayList<>(xmlFiles);
            try (var paths = Files.list(reports)) {
                resultFiles.addAll(
                        paths.filter(file -> file.getFileName().toString().endsWith(".txt"))
                                .toList());
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
            AtfReportStatus status =
                    failures + errors == 0 ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            modules.add(
                    new ModuleStats(
                            module,
                            tests,
                            failures,
                            errors,
                            skipped,
                            Duration.ofMillis((long) (seconds * 1_000)),
                            status,
                            xmlFiles,
                            resultFiles));
        }
        return modules;
    }
}

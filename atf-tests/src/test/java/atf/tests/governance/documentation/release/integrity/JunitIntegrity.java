package atf.tests.governance.documentation.release.integrity;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import static atf.tests.governance.documentation.release.integrity.ReleaseFiles.*;

/** Recounts actual cases once, without trusting summary attributes or loading XML resources. */
final class JunitIntegrity {
    private record Counts(long tests, long failures, long errors, long skipped) {
        Counts plus(Counts value) {
            return new Counts(Math.addExact(tests, value.tests), Math.addExact(failures, value.failures),
                    Math.addExact(errors, value.errors), Math.addExact(skipped, value.skipped));
        }
    }
    static Set<String> verify(Path folder, JsonNode qualification) throws IOException {
        require(text(qualification, "state").equals("PASS"), "QUALIFICATION", folder);
        var groups = qualification.path("groups");require(groups.isObject() && !groups.isEmpty(), "JUNIT-GROUPS", folder);
        Path results = folder.resolve("reports/test-results");
        var expectedGroups = new HashSet<String>();var identities = new HashSet<String>();
        var fields = groups.fields();
        while (fields.hasNext()) {
            var field = fields.next();String name = field.getKey();
            require(name.matches("[a-z][a-z0-9-]*"), "JUNIT-GROUP", name);expectedGroups.add(name);
            group(results.resolve(name), field.getValue(), identities);
        }
        require(Files.isDirectory(results), "JUNIT-DIRECTORY", results);
        try (var entries = Files.list(results)) {
            var actual = new HashSet<String>();
            for (Path entry : entries.toList()) { require(Files.isDirectory(entry), "JUNIT-DIRECTORY", entry);actual.add(entry.getFileName().toString()); }
            require(actual.equals(expectedGroups), "JUNIT-GROUPS", results);
        }
        return Set.copyOf(identities);
    }
    private static void group(Path directory, JsonNode declared, Set<String> identities) throws IOException {
        require(Files.isDirectory(directory), "JUNIT-DIRECTORY", directory);
        var files = declared.path("files");require(files.isArray() && !files.isEmpty(), "JUNIT-FILES", directory);
        var names = new HashSet<String>();var actual = new Counts(0, 0, 0, 0);
        for (JsonNode file : files) {
            require(file.isTextual(), "JUNIT-FILE", file);String name = file.textValue();
            require(name.startsWith("TEST-") && name.endsWith(".xml") && !name.contains("/") && names.add(name), "JUNIT-FILE", name);
            actual = actual.plus(xml(inside(directory, name), identities));
        }
        require(inventory(directory).equals(names), "JUNIT-POPULATION", directory);
        var expected = new Counts(integer(declared, "tests"), integer(declared, "failures"),
                integer(declared, "errors"), integer(declared, "skipped"));
        require(actual.equals(expected) && actual.tests > 0, "JUNIT-TOTALS", directory);
        require(actual.failures == 0 && actual.errors == 0 && actual.skipped == 0, "JUNIT-VERDICT", directory);
    }
    private static Counts xml(Path file, Set<String> identities) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException error) throws SAXException { throw error; }
                @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
            });
            return suite(builder.parse(file.toFile()).getDocumentElement(), identities);
        } catch (javax.xml.parsers.ParserConfigurationException | SAXException invalid) {
            throw new IOException("RELEASE-INTEGRITY-JUNIT-XML: " + file, invalid);
        } catch (ArithmeticException overflow) { throw new IOException("RELEASE-INTEGRITY-JUNIT-COUNTS: " + file, overflow); }
    }
    private static Counts suite(Element suite, Set<String> identities) throws IOException {
        require(Set.of("testsuite", "testsuites").contains(suite.getTagName()), "JUNIT-ROOT", suite.getTagName());
        var actual = new Counts(0, 0, 0, 0);
        for (Node node = suite.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element element)) continue;
            switch (element.getTagName()) {
                case "testcase" -> actual = actual.plus(testcase(element, identities));
                case "testsuite", "testsuites" -> actual = actual.plus(suite(element, identities));
                case "properties", "system-out", "system-err" -> { }
                default -> require(false, "JUNIT-ELEMENT", element.getTagName());
            }
        }
        var expected = new Counts(number(suite, "tests"), number(suite, "failures"), number(suite, "errors"), number(suite, "skipped"));
        require(actual.equals(expected), "JUNIT-COUNTS", suite.getTagName());
        if (suite.hasAttribute("disabled")) require(number(suite, "disabled") == 0, "JUNIT-VERDICT", "disabled cases");
        return actual;
    }
    private static Counts testcase(Element testcase, Set<String> identities) throws IOException {
        String name = testcase.getAttribute("name"), owner = testcase.getAttribute("classname");
        require(!name.isBlank() && !owner.isBlank() && identities.add(owner + "\u0000" + name), "JUNIT-IDENTITY", owner + "#" + name);
        int failed = 0, errors = 0, skipped = 0;
        for (Node node = testcase.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element element)) continue;
            switch (element.getTagName()) {
                case "failure" -> failed++;
                case "error" -> errors++;
                case "skipped" -> skipped++;
                case "system-out", "system-err", "properties" -> { }
                default -> require(false, "JUNIT-ELEMENT", element.getTagName());
            }
        }
        require(failed + errors + skipped <= 1, "JUNIT-OUTCOME", name);
        return new Counts(1, failed, errors, skipped);
    }
    private static long number(Element element, String name) throws IOException {
        String value = element.getAttribute(name);
        require(value.matches("0|[1-9][0-9]*"), "JUNIT-NUMBER", name);
        try { return Long.parseLong(value); }
        catch (NumberFormatException overflow) { throw new IOException("RELEASE-INTEGRITY-JUNIT-NUMBER: " + name, overflow); }
    }
    private JunitIntegrity() { }
}

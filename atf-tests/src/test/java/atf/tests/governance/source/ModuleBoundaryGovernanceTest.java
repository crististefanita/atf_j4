package atf.tests.governance.source;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ModuleBoundaryGovernanceTest {
    @Test void theReactorAndBinaryBoundaryMatchTheDeclaredAndroidPerimeter() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var project = factory.newDocumentBuilder().parse(root.resolve("pom.xml").toFile()).getDocumentElement();
        var modules = new ArrayList<String>();
        for (var node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element element) || !element.getTagName().equals("modules")) continue;
            for (var child = element.getFirstChild(); child != null; child = child.getNextSibling())
                if (child instanceof Element module && module.getTagName().equals("module")) modules.add(module.getTextContent().strip());
        }
        var expected = new ArrayList<String>();
        GovernanceProject.policy(root).required("mavenModules").forEach(value -> expected.add(value.asText()));
        assertEquals(expected, modules, "The source reactor must match the reviewed governance perimeter");
        for (String module : modules) assertTrue(Files.isRegularFile(root.resolve(module).resolve("pom.xml")));
        for (String binary : List.of("atf-core", "atf-reporting", "atf-console", "atf-mock", "atf-reporting-tests"))
            assertFalse(Files.exists(root.resolve(binary)), () -> "Binary consumers must not retain producer source/harness: " + binary);
    }
}

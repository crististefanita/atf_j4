package atf.tests.governance.source;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.AtfJavaSourceInventory;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TestLayerGovernanceTest {
    @Test void executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var annotations = TestLayerRules.annotationSources(root);
        var matrix = new TreeMap<String, Set<String>>();
        var violations = new ArrayList<String>();
        for (var file : AtfJavaSourceInventory.testSources(root)) {
            String source = Files.readString(file);
            if (!TestLayerRules.isConcreteExecutable(source)) continue;
            var layers = TestLayerRules.declaredLayers(source, annotations);
            if (layers.size() != 1) { violations.add(file + ": expected one layer, found " + layers); continue; }
            String module = root.relativize(file).getName(0).toString();
            matrix.computeIfAbsent(module, ignored -> new TreeSet<>()).add(layers.getFirst());
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
        assertFalse(matrix.isEmpty());
        var documented = new TreeMap<String, Set<String>>();
        var columns = java.util.List.of("@AtfUnitTest", "@AtfComponentTest", "@AtfIntegrationTest", "@AtfE2eTest");
        for (String row : Files.readAllLines(root.resolve("docs/testing/ATF_Architecture.md"))) {
            if (!row.startsWith("| `atf-")) continue;
            String[] cells = row.split("\\|", -1);
            String module = cells[1].trim().replace("`", "");
            var layers = new TreeSet<String>();
            for (int i = 0; i < columns.size(); i++) if (cells[i + 2].trim().equals("Da")) layers.add(columns.get(i));
            assertNull(documented.put(module, layers), "Duplicate module in the testing matrix");
        }
        assertEquals(matrix, documented, "Documented layers must be derived from live executable declarations");
    }

    @Test void composedAnnotationsResolveCyclesAndIgnoreNestedFixtureLayers() {
        var annotations = Map.of("fixture.Outer", "import fixture.Inner;\n@Inner\npublic @interface Outer {}",
                "fixture.Inner", "@AtfComponentTest\npublic @interface Inner {}",
                "fixture.Loop", "import fixture.Loop;\n@Loop\npublic @interface Loop {}");
        assertEquals(java.util.List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(
                "import fixture.Outer;\n@Outer\nclass Example {\n @AtfUnitTest static class Fixture {} }", annotations));
        assertTrue(TestLayerRules.declaredLayers("import fixture.Loop;\n@Loop\nclass Example {}", annotations).isEmpty());
    }
}

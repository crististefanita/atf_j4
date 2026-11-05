package atf.tests.governance.source;

import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TestLayerSyntaxTest {
    @Test void parserOwnershipSurvivesASyntaxErrorAndEndsAtClose() {
        var parser = JavaTestDeclarations.open();
        try (parser) {
            assertThrows(IllegalArgumentException.class, () -> parser.parse("class Incomplete {"));
            assertEquals("Healthy", parser.parse("class Healthy {}").types().getFirst().name());
        }
        parser.close();
        assertThrows(IllegalStateException.class, () -> parser.parse("class AfterClose {}"));
    }

    @Test void everyTopLevelExecutableKeepsItsOwnLayer() {
        var classes = TestLayerRules.inspect("class Helper {} @AtfUnitTest class Actual { @Test void caseName() {} }", Map.of());
        assertEquals(2, classes.size());
        assertFalse(classes.getFirst().executable());
        assertTrue(classes.get(1).executable());
        assertEquals(List.of("@AtfUnitTest"), classes.get(1).layers());
    }

    @Test void wildcardImportsResolveOnlyAnUnambiguousComposedAnnotation() {
        var definitions = Map.of("alpha.Mark", "@AtfComponentTest @interface Mark {}",
                "beta.Mark", "@AtfUnitTest @interface Mark {}");
        assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers("import alpha.*; @Mark class Example {}", definitions));
        assertThrows(IllegalArgumentException.class,
                () -> TestLayerRules.declaredLayers("import alpha.*; import beta.*; @Mark class Example {}", definitions));
    }

    @Test void qualifiedAndInlineMethodAnnotationsIdentifyAnExecutableClass() {
        for (String annotation : List.of("@org.junit.jupiter.api.Test", "@org.junit.jupiter.params.ParameterizedTest", "@Test")) {
            String source = "@AtfComponentTest class Example { " + annotation + " void caseName() {} }";
            assertTrue(TestLayerRules.isConcreteExecutable(source), annotation);
            assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(source, Map.of()));
        }
    }

    @Test void annotationsBetweenModifiersBelongToTheDeclaredType() {
        String source = "public @AtfComponentTest final class Example { @Test void caseName() {} }";
        assertTrue(TestLayerRules.isConcreteExecutable(source));
        assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(source, Map.of()));
    }

    @Test void fixtureMethodsCannotTurnAnEnclosingHelperIntoAnExecutable() {
        String helper = """
                class Helper {
                    @AtfE2eTest static class Fixture {
                        @Test void controlledCase() {}
                    }
                }
                """;
        assertFalse(TestLayerRules.isConcreteExecutable(helper));
        assertEquals(List.of(), TestLayerRules.declaredLayers(helper, Map.of()));
        String container = """
                @AtfComponentTest class Container {
                    @Nested class Child {
                        @Test void actualCase() {}
                    }
                }
                """;
        assertTrue(TestLayerRules.isConcreteExecutable(container));
        assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(container, Map.of()));
    }

    @Test void anIdentifierContainingInterfaceIsStillAConcreteClass() {
        assertTrue(TestLayerRules.isConcreteExecutable("class interfaceProbe {\n @Test void actualCase() {}\n}"));
        assertFalse(TestLayerRules.isConcreteExecutable("abstract class Example {\n @Test void actualCase() {}\n}"));
    }

    @Test void syntaxErrorsCannotBecomeSuccessfulEmptyDiscovery() {
        assertThrows(IllegalArgumentException.class,
                () -> TestLayerRules.isConcreteExecutable("class Example { @Test void incomplete("));
    }

    @Test void annotationDiscoveryIgnoresExamplesAndKeepsTheRealDeclaration(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path source = root.resolve("src/test/java/fixture/Real.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package fixture;
                @AtfComponentTest public @interface Real {}
                class Documentation {
                    String example = \"\"\"
                        @AtfE2eTest
                        public @interface Phantom {}
                        \"\"\";
                }
                """);
        var annotations = TestLayerRules.annotationSources(root);
        assertEquals(Set.of("fixture.Real"), annotations.keySet());
        assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(
                "import fixture.Real; @Real class Example {}", annotations));
    }
}

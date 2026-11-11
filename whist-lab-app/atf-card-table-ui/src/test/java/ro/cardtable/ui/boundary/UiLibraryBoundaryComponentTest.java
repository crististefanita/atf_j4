package ro.cardtable.ui.boundary;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class UiLibraryBoundaryComponentTest extends GradleAuditedTest {
    private static final List<String> GAME_PACKAGES = List.of("ro." + "whistlab", "ro." + "rentzlab",
            "ro.atf." + "holdem");

    @Test public void sharedTestsCannotImportConsumerRulesOrSessions() throws Exception {
        inspectSources("src/test/java", 4, GAME_PACKAGES);
        inspectSources("src/androidTest/java", 1, GAME_PACKAGES);
    }

    @Test public void productLibraryHasNoGameEngineAtfOrStorageDependency() throws Exception {
        var forbidden = new ArrayList<>(GAME_PACKAGES);
        forbidden.addAll(List.of("import atf.", "SharedPreferences", "extends Activity"));
        inspectSources("src/main/java", 10, forbidden);
        String manifest = readUtf8(module().resolve("src/main/AndroidManifest.xml"));
        assertFalse("The AAR declares no application", manifest.contains("<application"));
        assertFalse("The AAR declares no permissions", manifest.contains("uses-permission"));
    }

    private static void inspectSources(String sourceSet, int minimum, List<String> forbidden) throws Exception {
        var root = module();
        List<Path> javaSources;
        try (var paths = Files.walk(root.resolve(sourceSet))) {
            javaSources = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        var violations = new ArrayList<String>();
        for (var file : javaSources) {
            String source = readUtf8(file);
            for (var dependency : forbidden) {
                if (source.contains(dependency)) violations.add(root.relativize(file) + " uses " + dependency);
            }
        }
        GradleInputs.fixture("Shared source ownership", "source set", sourceSet,
                "Java files inspected", javaSources.size(), "forbidden dependencies", forbidden);
        assertTrue("A missing source inventory cannot pass the boundary", javaSources.size() >= minimum);
        assertEquals("No shared source crosses a consumer boundary", List.of(), violations);
    }

    private static Path module() {
        return Path.of(System.getProperty("cardui.sourceRoot"));
    }

    private static String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

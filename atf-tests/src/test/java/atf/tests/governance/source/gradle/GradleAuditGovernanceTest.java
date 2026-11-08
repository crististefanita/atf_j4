package atf.tests.governance.source.gradle;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Source ownership and coverage gates for the JUnit 4 fixture variant; no reporting-library exception. */
@AtfComponentTest
public final class GradleAuditGovernanceTest {
    private static final Pattern JUNIT4_TEST_IMPORT = Pattern.compile("\\bimport\\s+org\\.junit\\.(?:Test|\\*)\\s*;");

    @Test void everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var found = new TreeMap<String, String>();
        for (var file : GovernanceProject.sources(root)) {
            if (!file.test() || !file.module().startsWith("whist-lab-app/")) continue;
            String name = file.file().getFileName().toString();
            if (!name.endsWith("Test.java")) continue;
            String source = Files.readString(file.file());
            if (!usesJUnit4Test(source)) continue;
            var declaration = java.util.regex.Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(source);
            assertTrue(declaration.find(), "Each test has an explicit package: " + file.file());
            String owner = declaration.group(1) + "." + name.substring(0, name.length() - 5);
            assertTrue(source.matches("(?s).*\\bextends\\s+(?:atf\\.gradle\\.audit\\.)?GradleAuditedTest\\b.*"),
                    "A new Gradle test cannot silently bypass input/assertion capture: " + owner);
            found.put(owner, "declared");
        }
        var catalog = new TreeMap<String, String>();
        for (String line : Files.readAllLines(root.resolve("config/gradle-audit-collections.properties"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] pair = line.split("=", -1);
            assertEquals(2, pair.length, "The test-layer entry is a single owner/value pair");
            assertNull(catalog.put(pair[0], pair[1]), "An invocation owner must not have duplicate layer declarations");
            assertTrue(List.of("unit", "component", "governance", "learning").contains(pair[1]), "Unknown test layer: " + line);
        }
        assertFalse(found.isEmpty(), "The Gradle source perimeter cannot be empty");
        assertEquals(found.keySet(), catalog.keySet(), "The catalog includes every test class exactly once, including opt-in collections");
    }
    @Test void inventoryRecognizesExplicitAndWildcardJUnit4ImportsWithoutTreatingJupiterAsJUnit4() {
        assertTrue(usesJUnit4Test("import org.junit.Test; class Explicit {}"));
        assertTrue(usesJUnit4Test("import org.junit.*; class Wildcard {}"));
        assertFalse(usesJUnit4Test("import org.junit.jupiter.api.Test; class Jupiter {}"));
    }

    private static boolean usesJUnit4Test(String source) {
        return JUNIT4_TEST_IMPORT.matcher(source).find();
    }
    @Test void productTestsDoNotKeepRawJUnitAssertionsBesideTheAuditFacade() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var violations = new java.util.ArrayList<String>();
        for (var file : GovernanceProject.sources(root)) {
            if (!file.test() || !file.module().startsWith("whist-lab-app/") || file.module().endsWith("/test-audit")) continue;
            String source = Files.readString(file.file());
            if (source.contains("org.junit.Assert")) violations.add(root.relativize(file.file()).toString());
        }
        assertEquals(List.of(), violations, "Helpers and parametrized cases use the same evaluated-assertion capture");
    }
    @Test void auditDependenciesRemainTestOnlyAndTheInventoryGateIsMandatory() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String bridge = Files.readString(root.resolve("whist-lab-app/gradle/component-audit.gradle"));
        assertTrue(bridge.contains("dependencies.add('testImplementation'"), "The fixture variant is attached only to test compilation/runtime");
        assertFalse(bridge.contains("dependencies.add('implementation'"), "Android runtime variants cannot consume reporting or JUnit fixtures");
        assertTrue(bridge.contains("GradleReportVerifier"), "Every passing task must reconcile original JUnit with actual HTML");
        assertTrue(bridge.contains("reports.html.required = false"), "Gradle's duplicate presentation is disabled");
        assertTrue(bridge.contains("maxParallelForks = 1"), "The shared report owner does not claim unqualified parallel execution");
    }
    @Test void thePureJvmEntryUsesTheSameAuditWithoutConfiguringAndroid() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String settings = Files.readString(root.resolve("whist-lab-app/algorithm-build/settings.gradle"));
        String build = Files.readString(root.resolve("whist-lab-app/algorithm-build/build.gradle"));
        assertTrue(settings.contains("include(\":test-audit\")"), "The JVM-only reactor resolves the same test fixture variant");
        assertTrue(build.contains("component-audit.gradle"), "There is one audit configuration for both Gradle entrypoints");
        assertFalse(settings.contains("com.android"), "Offline algorithm tests do not configure the Android plugin");
    }
    @Test void everyApplicationPackageHasExactlyOneNonemptyPrimaryCollection() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var mapping = new TreeMap<String, String>();
        for (String line : Files.readAllLines(root.resolve("config/gradle-app-collections.properties"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] pair = line.split("=", -1);
            assertEquals(2, pair.length, "One package has one primary collection");
            assertNull(mapping.put(pair[0], pair[1]), "Duplicate application package ownership");
        }
        var found = new java.util.TreeSet<String>();
        for (var file : GovernanceProject.sources(root)) {
            String path = root.relativize(file.file()).toString().replace('\\', '/');
            if (!path.startsWith("whist-lab-app/app/src/test/java/ro/whistlab/app/") || !path.endsWith("Test.java")) continue;
            found.add(path.substring("whist-lab-app/app/src/test/java/ro/whistlab/app/".length()).split("/")[0]);
        }
        assertEquals(found, mapping.keySet(), "New or obsolete package ownership cannot silently omit or duplicate tests");
        assertEquals(java.util.Set.of("commands", "bidding", "history", "display"), new java.util.HashSet<>(mapping.values()), "Each declared collection owns actual test packages");
        String build = Files.readString(root.resolve("whist-lab-app/gradle/app-component-collections.gradle"));
        assertTrue(build.contains("enabled = false") && build.contains("dependsOn collections"), "The original task delegates and does not execute a duplicate population");
    }
}

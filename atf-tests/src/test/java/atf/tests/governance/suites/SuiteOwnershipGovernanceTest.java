package atf.tests.governance.suites;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.source.TestLayerRules;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class SuiteOwnershipGovernanceTest {
    @Test void everyMavenExecutableHasOnePrimaryOwnerAndEveryFocusedGateIsDeclared() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var sources = TestLayerRules.executableSources(root);
        var known = new HashMap<String, String>();
        sources.forEach(s -> known.put(s.name(), s.source()));
        var actualSuites = new TreeMap<String, TestLayerRules.ExecutableSource>();
        var tests = new ArrayList<SuiteOwnershipRules.TestSource>();
        for (var source : sources) {
            AuditedAssertions.assertEquals(1, source.layers().size(), source.name());
            if (source.suite()) actualSuites.put(source.name(), source);
            else tests.add(new SuiteOwnershipRules.TestSource(source.module(), source.name(),
                    source.layers().getFirst().replace("@Atf", "").replace("Test", "").toLowerCase(Locale.ROOT)));
        }
        var catalog = new ObjectMapper().readTree(Files.readString(root.resolve("config/atf-test-suites.json")));
        AuditedAssertions.assertEquals(1, catalog.path("schemaVersion").asInt());
        var gates = new ArrayList<SuiteOwnershipRules.Gate>();
        for (var entry : catalog.required("suites")) {
            String name = entry.required("suite").asText();
            var source = actualSuites.get(name);AuditedAssertions.assertNotNull(source, "Missing declared suite: " + name);
            AuditedAssertions.assertEquals(source.module(), entry.required("module").asText(), name);
            var parents = new ArrayList<String>();entry.path("parents").forEach(p -> parents.add(p.asText()));
            gates.add(new SuiteOwnershipRules.Gate(source.module(), name, entry.required("role").asText(),
                    entry.path("layer").asText(), parents, TestLayerRules.suiteSelectors(source.source(), known)));
        }
        AuditedAssertions.assertEquals(actualSuites.keySet(), new TreeSet<>(gates.stream().map(SuiteOwnershipRules.Gate::name).toList()), "Every live suite declares its role");
        AuditedAssertions.assertFalse(tests.isEmpty());
        AuditedAssertions.assertEquals(List.of(), SuiteOwnershipRules.violations(tests, gates));
    }
    @Test void newOmittedClassOverlapWrongLayerAndHiddenSelectionAreRejected() {
        var test = new SuiteOwnershipRules.TestSource("module", "example.component.ProbeTest", "component");
        var primary = gate("Primary", "primary", "component", List.of(), "example");
        AuditedAssertions.assertTrue(SuiteOwnershipRules.violations(List.of(test), List.of(primary)).isEmpty());
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(gate("Missing", "primary", "component", List.of(), "other"))).isEmpty());
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(primary, gate("Duplicate", "primary", "component", List.of(), "example"))).isEmpty());
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(gate("Wrong", "primary", "unit", List.of(), "example"))).isEmpty());
        var hidden = new SuiteOwnershipRules.Gate("module", "Hidden", "primary", "component", List.of(), Map.of("SelectPackages", List.of("example"), "IncludeTags", List.of("component"), "ExcludeTags", List.of("slow")));
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(hidden)).isEmpty());
    }
    @Test void focusedSubsetDoesNotCountTwiceButCannotEscapeItsParent() {
        var test = new SuiteOwnershipRules.TestSource("module", "example.component.ProbeTest", "component");
        var primary = gate("Primary", "primary", "component", List.of(), "example");
        var focus = gate("Focused", "focused", "", List.of("Primary"), "example.component");
        AuditedAssertions.assertTrue(SuiteOwnershipRules.violations(List.of(test), List.of(primary, focus)).isEmpty());
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(primary, gate("Orphan", "focused", "", List.of("Missing"), "example"))).isEmpty());
    }
    @Test void packageScanningCannotIndependentlyLaunchNegativeNestedFixtures() {
        var test = new SuiteOwnershipRules.TestSource("module", "example.ProbeTest", "component");
        var unsafe = new SuiteOwnershipRules.Gate("module", "Primary", "primary", "component", List.of(),
                Map.of("SelectPackages", List.of("example"), "IncludeTags", List.of("component"), "IncludeClassNamePatterns", List.of(".*")));
        AuditedAssertions.assertTrue(SuiteOwnershipRules.violations(List.of(test),List.of(unsafe)).stream().anyMatch(s -> s.contains("nested fixtures")));
    }
    @Test void focusedChannelFilteringCannotRemoveOrReplaceAPrimaryOwner() {
        var test = new SuiteOwnershipRules.TestSource("module", "example.ProbeTest", "component");
        var primary = gate("Primary", "primary", "component", List.of(), "example");
        var selectors = new HashMap<>(primary.selectors());selectors.put("ExcludeTags", List.of("network-live"));
        var focus = new SuiteOwnershipRules.Gate("module", "Offline", "focused", "", List.of("Primary"), selectors);
        AuditedAssertions.assertEquals(List.of(), SuiteOwnershipRules.violations(List.of(test), List.of(primary, focus)),
                "Focused filtering refines its inclusive parent population without reducing the primary gate");
        AuditedAssertions.assertFalse(SuiteOwnershipRules.violations(List.of(test), List.of(focus)).isEmpty(),
                "A filtered focus cannot replace the missing primary");
        var hiddenPrimary = new SuiteOwnershipRules.Gate("module", "Primary", "primary", "component", List.of(), selectors);
        AuditedAssertions.assertTrue(SuiteOwnershipRules.violations(List.of(test), List.of(hiddenPrimary)).stream()
                .anyMatch(problem -> problem.contains("Primary exclusion")), "Primary coverage still refuses negative filters");
    }
    @Test void selectorParsingUsesOnlyTheOwnerAndResolvesImportsAndNamedValues() {
        var parsed = TestLayerRules.suiteSelectors("package fixture; import other.Probe; @SelectClasses(value={Probe.class}) @SelectPackages(\"fixture\") class Gate { @ExcludeTags(\"hidden\") class Nested {} }", Map.of("other.Probe", ""));
        AuditedAssertions.assertEquals(List.of("other.Probe"), parsed.get("SelectClasses"));
        AuditedAssertions.assertFalse(parsed.containsKey("ExcludeTags"));
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> TestLayerRules.suiteSelectors("@SelectPackages(PACKAGE) class Gate {}", Map.of()));
    }
    private static SuiteOwnershipRules.Gate gate(String name, String role, String layer, List<String> parents, String pack) {
        var selectors = new HashMap<String, List<String>>();selectors.put("SelectPackages", List.of(pack));
        selectors.put("IncludeClassNamePatterns", List.of("^[^$]+$"));
        if (role.equals("primary")) selectors.put("IncludeTags", List.of(layer));
        return new SuiteOwnershipRules.Gate("module", name, role, layer, parents, selectors);
    }
}

package atf.tests.governance.suites;

import java.util.*;
import java.util.regex.Pattern;

/** Pure ownership rules; focused gates never contribute a second primary owner. */
final class SuiteOwnershipRules {
    record TestSource(String module, String name, String layer) { }
    record Gate(String module, String name, String role, String layer, List<String> parents,
                Map<String, List<String>> selectors) {
        boolean selects(TestSource test) {
            if (!module.equals(test.module())) return false;
            boolean selected = selectors.getOrDefault("SelectClasses", List.of()).contains(test.name())
                    || selectors.getOrDefault("SelectPackages", List.of()).stream().anyMatch(p -> test.name().startsWith(p + "."));
            return selected && (selectors.getOrDefault("IncludeTags", List.of()).isEmpty()
                    || selectors.get("IncludeTags").contains(test.layer()))
                    && selectors.getOrDefault("IncludeClassNamePatterns", List.of(".*")).stream()
                    .anyMatch(p -> Pattern.matches(p, test.name()));
        }
    }
    static List<String> violations(List<TestSource> tests, List<Gate> gates) {
        var errors = new ArrayList<String>();
        var names = new HashMap<String, Gate>();
        for (var gate : gates) {
            if (names.put(gate.name(), gate) != null) errors.add("Duplicate gate: " + gate.name());
            if (!Set.of("primary", "focused").contains(gate.role())) errors.add("Unknown role: " + gate.name());
            if (gate.selectors().keySet().stream().anyMatch(s -> s.startsWith("Exclude"))) errors.add("Exclusion can hide new cases: " + gate.name());
            if (!gate.selectors().containsKey("SelectPackages") && !gate.selectors().containsKey("SelectClasses")) errors.add("Empty selection: " + gate.name());
            if (gate.selectors().containsKey("SelectPackages") && gate.selectors().getOrDefault("IncludeClassNamePatterns", List.of(".*"))
                    .stream().anyMatch(p -> Pattern.matches(p, "example.Parent$FixtureComponentTest")))
                errors.add("Package scanning can independently execute nested fixtures: " + gate.name());
            if (!gate.selectors().getOrDefault("IncludeEngines", List.of("junit-jupiter")).equals(List.of("junit-jupiter"))) errors.add("Expected Jupiter-only leaf selection: " + gate.name());
            if (gate.role().equals("primary") && !gate.selectors().getOrDefault("IncludeTags", List.of()).equals(List.of(gate.layer()))) errors.add("Primary requires its exact layer: " + gate.name());
        }
        for (var test : tests) {
            var owners = gates.stream().filter(g -> g.role().equals("primary") && g.selects(test)).toList();
            if (owners.size() != 1) errors.add(test.name() + " primary owners=" + owners.stream().map(Gate::name).toList());
            else if (!owners.getFirst().layer().equals(test.layer())) errors.add("Wrong layer: " + test.name());
        }
        for (var gate : gates) {
            if (tests.stream().noneMatch(gate::selects)) errors.add("Gate selects no executable class: " + gate.name());
            if (!gate.role().equals("focused")) continue;
            var parents = gate.parents().stream().map(names::get).filter(Objects::nonNull).toList();
            if (parents.size() != gate.parents().size() || parents.isEmpty() || parents.stream().anyMatch(g -> !g.role().equals("primary"))) errors.add("Focused gate needs declared primary parents: " + gate.name());
            for (var test : tests) if (gate.selects(test) && parents.stream().noneMatch(g -> g.selects(test))) errors.add("Focused case outside parent: " + gate.name() + " / " + test.name());
        }
        return List.copyOf(errors);
    }
    private SuiteOwnershipRules() { }
}

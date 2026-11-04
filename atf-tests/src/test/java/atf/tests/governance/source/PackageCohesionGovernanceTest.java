package atf.tests.governance.source;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class PackageCohesionGovernanceTest {
    @Test void oversizedPackagesRequireAnExactReviewedInventoryAndAnAction() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var policy = GovernanceProject.policy(root);
        var inventory = SourceQualityRules.packageInventory(GovernanceProject.sources(root));
        var violations = check(inventory, policy.required("packageLimit").asInt(), policy.required("reviewedPackages"));
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    static List<String> check(Map<String, List<String>> inventory, int limit, JsonNode reviewed) {
        if (limit != 8) throw new IllegalArgumentException("The inherited cohesion limit is eight source files");
        var result = new ArrayList<String>();
        inventory.forEach((key, files) -> {
            if (files.size() <= limit) return;
            JsonNode exception = reviewed.path(key);
            var expected = new TreeSet<String>();
            exception.path("files").forEach(value -> expected.add(value.asText()));
            if (!expected.equals(new TreeSet<>(files)) || exception.path("reason").asText().isBlank()
                    || exception.path("action").asText().isBlank()) result.add("GOV-COHESION: review " + key + " " + files);
        });
        reviewed.fieldNames().forEachRemaining(key -> {
            if (inventory.getOrDefault(key, List.of()).size() <= limit) result.add("GOV-COHESION-STALE: " + key);
        });
        return result;
    }

    @Test void anExceptionCannotHideGrowthReplacementOrAStalePackage() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var original = java.util.stream.IntStream.range(0, 9).mapToObj(i -> "Type" + i + ".java").toList();
        var entry = mapper.createObjectNode().put("reason", "reviewed seam").put("action", "split by responsibility");
        var files = entry.putArray("files"); original.forEach(files::add);
        var reviewed = mapper.createObjectNode().set("module:main:example", entry);
        assertTrue(check(Map.of("module:main:example", original), 8, reviewed).isEmpty());
        var replaced = new ArrayList<>(original); replaced.set(0, "Replacement.java");
        assertFalse(check(Map.of("module:main:example", replaced), 8, reviewed).isEmpty());
        var grown = new ArrayList<>(original); grown.add("NewType.java");
        assertFalse(check(Map.of("module:main:example", grown), 8, reviewed).isEmpty());
        assertFalse(check(Map.of("module:main:example", original.subList(0, 8)), 8, reviewed).isEmpty());
    }
}

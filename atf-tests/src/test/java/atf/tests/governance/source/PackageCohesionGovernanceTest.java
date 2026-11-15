package atf.tests.governance.source;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.jvm.PackageCohesionRules;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AtfComponentTest
class PackageCohesionGovernanceTest {
    @Test void currentProjectUsesNoCohesionExceptions() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var policy = GovernanceProject.policy(root);
        AuditedAssertions.assertEquals(0, policy.required("reviewedPackages").size(),
                "The current package layout must remain within the cohesion limit without waivers");
    }

    @Test void unreviewedPackagePassesAtEightFilesAndRejectsTheNinth() {
        var files = java.util.stream.IntStream.range(0, 9).mapToObj(i -> "Type" + i + ".java").toList();
        var noExceptions = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        String owner = "module:test:example";
        AuditedAssertions.assertTrue(PackageCohesionRules.check(Map.of(owner, files.subList(0, 8)), 8, noExceptions).isEmpty());
        var violations = PackageCohesionRules.check(Map.of(owner, files), 8, noExceptions);
        AuditedAssertions.assertEquals(1, violations.size());
        AuditedAssertions.assertTrue(violations.getFirst().contains(owner));
    }

    @Test void anExceptionCannotHideGrowthReplacementOrAStalePackage() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var original = java.util.stream.IntStream.range(0, 9).mapToObj(i -> "Type" + i + ".java").toList();
        var entry = mapper.createObjectNode().put("reason", "reviewed seam").put("action", "split by responsibility");
        var files = entry.putArray("files"); original.forEach(files::add);
        var reviewed = mapper.createObjectNode().set("module:main:example", entry);
        AuditedAssertions.assertTrue(PackageCohesionRules.check(Map.of("module:main:example", original), 8, reviewed).isEmpty());
        var replaced = new ArrayList<>(original); replaced.set(0, "Replacement.java");
        AuditedAssertions.assertFalse(PackageCohesionRules.check(Map.of("module:main:example", replaced), 8, reviewed).isEmpty());
        var grown = new ArrayList<>(original); grown.add("NewType.java");
        AuditedAssertions.assertFalse(PackageCohesionRules.check(Map.of("module:main:example", grown), 8, reviewed).isEmpty());
        AuditedAssertions.assertFalse(PackageCohesionRules.check(Map.of("module:main:example", original.subList(0, 8)), 8, reviewed).isEmpty());
    }
}

package atf.tests.governance.source;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class AtfSourceQualityGovernanceTest {
    @Test void sourcesRespectPackagePathsImportsAndProductionBoundaries() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        var policy = GovernanceProject.policy(root);
        var sources = GovernanceProject.sources(root);
        AuditedAssertions.assertFalse(sources.isEmpty(), "The governed source population must not be empty");
        var violations = new ArrayList<String>();
        for (var file : sources) {
            var forbidden = new ArrayList<String>();
            if (!file.test()) policy.required("productionForbiddenNamespaces").required(file.module())
                    .forEach(value -> forbidden.add(value.asText()));
            String relative = GovernanceProject.relative(root, file.file());
            var adapter = policy.path("productionBoundaryAdapters").path(relative);
            adapter.path("allows").forEach(value -> forbidden.remove(value.asText()));
            for (String finding : SourceQualityRules.inspect(file, Files.readString(file.file()), forbidden))
                violations.add(relative + ": " + finding);
        }
        policy.path("productionBoundaryAdapters").fields().forEachRemaining(entry -> {
            if (entry.getValue().path("reason").asText().isBlank()) violations.add("Adapter needs a reason: " + entry.getKey());
            if (sources.stream().noneMatch(file -> !file.test() && GovernanceProject.relative(root, file.file()).equals(entry.getKey())))
                violations.add("Stale production adapter: " + entry.getKey());
        });
        AuditedAssertions.assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    @Test void lexicalGuardFindsRealViolationsAndIgnoresExamples() {
        var file = new GovernanceProject.SourceFile(Path.of("src/a/Example.java"), Path.of("src"), "fixture", false);
        String code = """
                package a;
                import java.util.List;
                import java . util . List;
                class Example { String example = "import x.Y; import x.Y; org.junit.Test"; }
                // import org.junit.Test;
                """;
        AuditedAssertions.assertEquals(List.of("java.util.List"), SourceQualityRules.duplicateImports(code));
        AuditedAssertions.assertEquals(1, SourceQualityRules.inspect(file, code, List.of("org.junit")).size());
        var invalid = SourceQualityRules.inspect(file, "package wrong;\nimport org.junit.Test;", List.of("org.junit"));
        AuditedAssertions.assertEquals(2, invalid.size());
        AuditedAssertions.assertTrue(invalid.stream().anyMatch(value -> value.contains("GOV-PACKAGE-PATH")));
        AuditedAssertions.assertTrue(invalid.stream().anyMatch(value -> value.contains("GOV-BOUNDARY")));
    }
}

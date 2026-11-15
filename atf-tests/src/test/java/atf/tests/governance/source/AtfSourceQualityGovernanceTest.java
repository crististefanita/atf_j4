package atf.tests.governance.source;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.jvm.JvmArchitecture;
import atf.tests.jvm.JvmSourceInventory;
import atf.tests.jvm.SourceQualityRules;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;

@AtfComponentTest
class AtfSourceQualityGovernanceTest {
    @Test void sourcesRespectPackagePathsImportsAndProductionBoundaries() throws Exception {
        var result = JvmArchitecture.inspect(GovernanceProject.locateProjectRoot());
        AuditedAssertions.assertTrue(result.files() > 0, "The governed source population must not be empty");
        AuditedAssertions.assertTrue(result.diagnostics().isEmpty(), () -> String.join("\n", result.diagnostics()));
    }

    @Test void lexicalGuardFindsRealViolationsAndIgnoresExamples() {
        var file = new JvmSourceInventory.SourceFile(Path.of("src/a/Example.java"), Path.of("src"), "fixture", false);
        String code = """
                package a;
                import java.util.List;
                import java . util . List;
                class Example { String example = "import x.Y; import x.Y; org.junit.Test"; }
                // import org.junit.Test;
                """;
        AuditedAssertions.assertEquals(List.of("java.util.List"), SourceQualityRules.duplicateImports(code));
        AuditedAssertions.assertEquals(1, SourceQualityRules.inspect(file, code, List.of("org.junit")).findings().size());
        var invalid = SourceQualityRules.inspect(file, "package wrong;\nimport org.junit.Test;", List.of("org.junit"));
        AuditedAssertions.assertEquals(List.of("GOV-PACKAGE-PATH: expected a"), invalid.findings());
        AuditedAssertions.assertEquals(List.of("org.junit"), invalid.forbiddenReferences());
        AuditedAssertions.assertTrue(SourceQualityRules.inspect(file, code, List.of("org.junit")).forbiddenReferences().isEmpty());
    }
}

package atf.tests.governance.documentation.factual;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

@AtfComponentTest
class DocumentedModuleMapGovernanceTest {
    @Test void rootMapMatchesTheBuildDeclarations() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        AuditedAssertions.assertEquals(List.of(), DocumentedModuleMap.inspect(root, Files.readString(root.resolve("README.md"))));
    }

    @Test void aMissingModuleAChangedScopeAndAnInvertedEdgeAreRejected() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String readme = Files.readString(root.resolve("README.md"));
        for (var mutation : List.of(
                List.of("| Maven | atf-validation, atf-test-android, atf-tests |",
                        "| Maven | atf-validation, atf-tests |", "Maven modules"),
                List.of("| Maven | atf-validation | test | atf-test-kit |",
                        "| Maven | atf-validation | compile | atf-test-kit |", "dependency scopes"),
                List.of("| Gradle | game-core | testImplementation | ai-lab |",
                        "| Gradle | game-core | implementation | ai-lab |", "dependency scopes"),
                List.of("app -->|folosește| game_core",
                        "game_core -->|folosește| app", "product diagram edges"))) {
            AuditedAssertions.assertTrue(readme.contains(mutation.get(0)), "The mutation must change a real documented claim");
            var failures = DocumentedModuleMap.inspect(root, readme.replace(mutation.get(0), mutation.get(1)));
            AuditedAssertions.assertTrue(failures.stream().anyMatch(failure -> failure.contains(mutation.get(2))), failures.toString());
        }
    }
}

package atf.tests.governance.documentation.strategy;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.AtfJavaSourceInventory;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TestingStrategyGovernanceTest {
    @Test void oneStrategyOwnerServesAllReleasesAndGenericTestingGuides() throws Exception {
        Path root = GovernanceProject.locateProjectRoot(), owner = root.resolve("docs/testing/README.md");
        var owners = new ArrayList<Path>();
        for (Path file : GovernanceProject.markdown(root)) {
            String text = Files.readString(file);
            if (text.contains(TestingStrategyRules.OWNER)) owners.add(file);
            if (file.startsWith(root.resolve("docs/testing")))
                AuditedAssertions.assertEquals(java.util.List.of(), TestingStrategyRules.violations(text), file.toString());
        }
        AuditedAssertions.assertEquals(java.util.List.of(owner), owners, "The strategy has one declared normative owner");
    }
    @Test void everyDeclaredTestRootHasAnExplanationLinkedToTheSameStrategy() throws Exception {
        Path root = GovernanceProject.locateProjectRoot(), strategy = root.resolve("docs/testing/README.md");
        var entries = new TreeSet<Path>();
        for (var source : AtfJavaSourceInventory.roots(root)) if (source.test()) entries.add(entry(source.directory()));
        for (var source : GovernanceProject.policy(root).path("additionalSourceRoots")) if (source.path("test").asBoolean())
            entries.add(entry(root.resolve(source.path("path").asText())));
        AuditedAssertions.assertFalse(entries.isEmpty());
        for (Path readme : entries) {
            AuditedAssertions.assertTrue(Files.isRegularFile(readme), "Test root needs its catalogue: " + readme);
            String href = readme.getParent().relativize(strategy).toString().replace('\\', '/');
            AuditedAssertions.assertTrue(Files.readString(readme).contains("(" + href + ")"), "Link the common strategy: " + readme);
        }
    }
    private static Path entry(Path source) {
        return (source.getFileName().toString().equals("java") ? source.getParent() : source).resolve("README.md");
    }
    @ParameterizedTest @ValueSource(strings = {"203 teste fără erori", "159/159 trecute", "72 tests PASS", "[Probe](../../release/1.2.0/reports/README.md)"})
    void variableResultsAndConcreteReleaseLinksAreRefused(String source) {
        AuditedAssertions.assertFalse(TestingStrategyRules.violations(source).isEmpty());
    }
    @ParameterizedTest @ValueSource(strings = {"Un rezultat FAIL rămâne vizibil.", "Componenta refuză zero comparații.", "[Catalog](../../release/README.md)", "Datele sunt în release/<version>/reports/."})
    void genericContractsAndStableNavigationRemainAllowed(String source) {
        AuditedAssertions.assertTrue(TestingStrategyRules.violations(source).isEmpty());
    }
}

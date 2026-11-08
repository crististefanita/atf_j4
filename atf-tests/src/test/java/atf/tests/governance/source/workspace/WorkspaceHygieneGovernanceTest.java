package atf.tests.governance.source.workspace;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class WorkspaceHygieneGovernanceTest {
    @TempDir Path temporary;
    @Test void projectRootContainsOnlyRegisteredOwnersAndKnownToolState() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        var findings = WorkspaceHygieneRules.inspect(root, WorkspaceHygieneRules.roots(GovernanceProject.policy(root)));
        AuditedAssertions.assertEquals(java.util.List.of(), findings, "WORKSPACE-HYGIENE: remove generated work after verifying its ownership and destination");
    }
    @ParameterizedTest @ValueSource(strings = {"UsersSomeoneAppDataLocalTempwhist-screen-compile-example", "CUsersSomeoneAppDataLocalTempcompile", "whist-screen-compile-example", "unknown-module", "dist", "artifacts"})
    void evenAnEmptyUnregisteredOrTemporaryDirectoryIsReported(String name) throws Exception {
        Path directory = Files.createDirectory(temporary.resolve(name));
        var findings = WorkspaceHygieneRules.inspect(temporary, Set.of("module"));
        AuditedAssertions.assertEquals(1, findings.size());AuditedAssertions.assertEquals(directory, findings.getFirst().path());
        AuditedAssertions.assertTrue(Files.isDirectory(directory), "Inspection must never delete the finding");
    }
    @ParameterizedTest @ValueSource(strings = {"Leftover.class", "game.apk", "library.jar", "heap.hprof", "compile.tmp", "hs_err_pid123.log", "javac.123.args"})
    void generatedRootFilesAreReportedWithoutDeletingThem(String name) throws Exception {
        Path file = Files.writeString(temporary.resolve(name), "witness");
        AuditedAssertions.assertEquals(1, WorkspaceHygieneRules.inspect(temporary, Set.of()).size());
        AuditedAssertions.assertEquals("witness", Files.readString(file));
    }
    @Test void declaredNestedModulesAndTheirSourcePackagesKeepTheirIdentity() throws Exception {
        var policy = new ObjectMapper().readTree("{\"mavenModules\":[\"modules/domain\"],\"additionalSourceRoots\":[{\"module\":\"app/core\"}]}");
        Files.createDirectories(temporary.resolve("modules/domain/src/main/java/legal/target"));
        Files.createDirectories(temporary.resolve("app/core"));
        Files.writeString(temporary.resolve("README.md"), "# Proiect");
        AuditedAssertions.assertTrue(WorkspaceHygieneRules.inspect(temporary, WorkspaceHygieneRules.roots(policy)).isEmpty());
    }
    @Test void nativePlatformAndWorkflowDirectoriesHaveExplicitRootOwners() throws Exception {
        var mapper = new ObjectMapper();
        var policy = mapper.readTree("{\"projectRootOwners\":[\".github\",\"ios\"]}");
        AuditedAssertions.assertTrue(WorkspaceHygieneRules.roots(policy).containsAll(Set.of(".github", "ios")));
        for (String invalid : new String[]{"../ios", "nested/ios", "C:\\ios"}) {
            var invalidPolicy = mapper.createObjectNode();
            invalidPolicy.putArray("projectRootOwners").add(invalid);
            AuditedAssertions.assertThrows(IllegalArgumentException.class,
                    () -> WorkspaceHygieneRules.roots(invalidPolicy));
        }
    }
    @Test void registeringAFlattenedTemporaryPathDoesNotMakeItASourceOwner() throws Exception {
        String name = "UsersSomeoneAppDataLocalTempcompile";Files.createDirectory(temporary.resolve(name));
        AuditedAssertions.assertEquals(1, WorkspaceHygieneRules.inspect(temporary, Set.of(name)).size());
    }
}

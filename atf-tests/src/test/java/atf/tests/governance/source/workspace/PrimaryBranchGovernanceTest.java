package atf.tests.governance.source.workspace;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class PrimaryBranchGovernanceTest {
    private static final Pattern LEGACY_PRIMARY_REFERENCE = Pattern.compile(
            "(?i)(?:\\borigin/|\\brefs/heads/|\\bbranches\\s*:\\s*\\[\\s*|/(?:blob|tree)/|"
                    + "\\bHEAD:|\\bdefault[- ]branch\\s*(?:[:=]|\\bis\\b)\\s*|"
                    + "\\bgit\\s+push\\s+\\S+\\s+)master\\b");
    private static final Set<String> GOVERNED_EXTENSIONS = Set.of(".md", ".mjs", ".ps1", ".yml", ".yaml");

    @Test void iosWorkflowRunsOnTheOfficialMainBranch() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String workflow = Files.readString(root.resolve(".github/workflows/ios.yml")).replace("\r\n", "\n");
        AuditedAssertions.assertTrue(Pattern.compile("(?m)^\\s*branches:\\s*\\[\\s*main\\s*]\\s*$")
                .matcher(workflow).find(), "The iOS push workflow follows the official branch");
        AuditedAssertions.assertFalse(containsLegacyPrimaryReference(workflow), "No push workflow targets master");
    }

    @Test void releaseGuidanceAndRepositoryDocumentsUseMainWithoutStaleBranchTargets() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String releaseGuide = Files.readString(root.resolve("docs/testing/ATF_ReleaseQualification.md"));
        AuditedAssertions.assertTrue(releaseGuide.contains("Ramura oficială a repository-ului este `main`"));
        AuditedAssertions.assertTrue(releaseGuide.contains("împinge commit-ul pe `main`"));
        var stale = legacyReferences(root);
        AuditedAssertions.assertEquals(List.of(), stale, "Remove obsolete primary-branch references from docs, release notes and automation");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "branches: [master]",
            "origin/master",
            "refs/heads/master",
            "https://github.com/example/repo/blob/master/README.md",
            "git push origin HEAD:master",
            "default branch: master"
    })
    void obsoletePrimaryBranchFormsAreDetected(String content) {
        AuditedAssertions.assertTrue(containsLegacyPrimaryReference(content), content);
    }

    @Test void gameTerminologyAboutMasterTrumpsIsNotARepositoryBranchReference() {
        AuditedAssertions.assertFalse(containsLegacyPrimaryReference("Master trumps remain a public lower bound."));
    }

    private static List<String> legacyReferences(Path root) throws IOException {
        List<Path> scopes = List.of(root.resolve("README.md"), root.resolve(".github"), root.resolve("docs"),
                root.resolve("release"), root.resolve("scripts"));
        List<String> findings = new ArrayList<>();
        for (Path scope : scopes) {
            if (!Files.exists(scope)) continue;
            try (Stream<Path> paths = Files.isDirectory(scope) ? Files.walk(scope) : Stream.of(scope)) {
                paths.filter(Files::isRegularFile)
                        .filter(PrimaryBranchGovernanceTest::isGovernedDocument)
                        .forEach(file -> inspect(root, file, findings));
            }
        }
        return findings.stream().sorted().toList();
    }

    private static void inspect(Path root, Path file, List<String> findings) {
        try {
            String[] lines = Files.readString(file).split("\\R", -1);
            for (int index = 0; index < lines.length; index++) {
                if (containsLegacyPrimaryReference(lines[index])) {
                    findings.add(root.relativize(file) + ":" + (index + 1));
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect primary-branch guidance: " + file, failure);
        }
    }

    private static boolean isGovernedDocument(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int extension = name.lastIndexOf('.');
        return extension >= 0 && GOVERNED_EXTENSIONS.contains(name.substring(extension));
    }

    private static boolean containsLegacyPrimaryReference(String content) {
        return LEGACY_PRIMARY_REFERENCE.matcher(content).find();
    }
}

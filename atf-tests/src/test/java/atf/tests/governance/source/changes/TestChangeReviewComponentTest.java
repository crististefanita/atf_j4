package atf.tests.governance.source.changes;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.testchange.TestChangeReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static atf.tests.governance.source.changes.TestChangeGitFixture.BASELINE;
import static atf.tests.governance.source.changes.TestChangeGitFixture.FEEDBACK;
import static atf.tests.governance.source.changes.TestChangeGitFixture.IGNORED;
import static atf.tests.governance.source.changes.TestChangeGitFixture.RESOURCE;
import static atf.tests.governance.source.changes.TestChangeGitFixture.TEST;

@AtfComponentTest
class TestChangeReviewComponentTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final String VALID = TODAY.toString();
    private static final String OLD = TODAY.minusDays(1).toString();
    private static final String NEW_TEST = "nested/module/src/test/java/demo/Test nou ș.java";
    private static final String GATE = "config/atf-test-suites.json";

    @Test void theSigningExportPolicyRequiresAnExplicitReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        String policy = "scripts/AtfSigningSourcePolicy.ps1";
        repo.put(policy, "function Test-AtfPrivateSigningInput { $true }\n");
        AuditedAssertions.assertEquals(List.of(uncovered("A", policy, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("SIGNING-EXPORT", "ACTIVE", repo.base, VALID, false, policy));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void theComponentJvmProfileCannotChangeWithoutAReviewedControl(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        String launcher = "scripts/AtfComponentJvm.ps1";
        repo.put(launcher, "function Get-AtfComponentJvmArguments { '-Datf.test.run.scope=suite' }\n");
        AuditedAssertions.assertEquals(List.of(uncovered("A", launcher, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("JVM-PROFILE", "ACTIVE", repo.base, VALID, false, launcher));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void fullDiffSeesCommittedStagedUnstagedUntrackedAndIgnoredTestPaths(@TempDir Path folder) throws Exception {
        var repo = new TestChangeGitFixture(Files.createDirectory(folder.resolve("checkout de probă")));
        repo.put("pom.xml", "<project><name>committed change</name></project>\n");
        repo.commit("committed build edit");
        repo.git("mv", TEST, NEW_TEST);
        repo.put(RESOURCE, "{\"changed\":true}\n");
        repo.put(IGNORED, "ignored source\n");
        String fixture = "whist-lab-app/test-audit/src/testFixtures/resources/replay.json";
        repo.put(fixture, "{}\n");
        repo.put("docs/guide.md", "# Editorial change\n");
        repo.put("target/generated/src/test/java/Debug.java", "generated output\n");
        var missing = check(repo);
        AuditedAssertions.assertEquals(6, missing.protectedPaths(), missing.diagnostics().toString());
        AuditedAssertions.assertEquals(List.of(
                uncovered("A", NEW_TEST, repo.base), uncovered("A", IGNORED, repo.base),
                uncovered("A", fixture, repo.base), uncovered("D", TEST, repo.base),
                uncovered("M", RESOURCE, repo.base), uncovered("M", "pom.xml", repo.base)), missing.diagnostics());
        repo.put(FEEDBACK, repo.control("REVIEW-DIFF", "ACTIVE", repo.base, VALID, true,
                "pom.xml", TEST, NEW_TEST, RESOURCE, IGNORED, fixture));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics(), "An exact control covers every Git change");
    }

    @Test void reviewRefMustExistAndBeAnAncestorAndCiNeedsAnEarlierBase(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var missing = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, "e".repeat(40), Map.of(), TODAY));
        AuditedAssertions.assertTrue(missing.getMessage().contains("TEST-CHANGE-BASE: cannot resolve commit"));
        var noCiBase = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, "", Map.of("CI", "true"), TODAY));
        AuditedAssertions.assertTrue(noCiBase.getMessage().contains("CI requires"));
        var sameAsHead = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, repo.base, Map.of("CI", "true"), TODAY));
        AuditedAssertions.assertTrue(sameAsHead.getMessage().contains("earlier than HEAD"));

        repo.put("README.md", "# Reviewed branch\n");
        repo.commit("main");
        repo.git("switch", "-q", "--detach", repo.base);
        repo.put("README.md", "# Side branch\n");
        repo.commit("side");
        String side = repo.git("rev-parse", "HEAD");
        repo.git("switch", "-q", "main");
        var unrelated = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, side, Map.of(), TODAY));
        AuditedAssertions.assertTrue(unrelated.getMessage().contains("not an ancestor"));
        AuditedAssertions.assertEquals(repo.base, TestChangeReview.inspect(root, null,
                Map.of("CI", "true", TestChangeReview.BASE_ENV, repo.base), TODAY).base());
    }

    @Test void currentDayIsInclusiveButExpiredClosedAndWrongBaseCannotCoverAGate(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.put(GATE, "{}\n");
        repo.put(FEEDBACK, repo.control("CLOSED-GATE", "CLOSED", repo.base, VALID, false, GATE)
                + repo.control("WRONG-BASE", "ACTIVE", "e".repeat(40), VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of(uncovered("A", GATE, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("EXPIRED-GATE", "ACTIVE", repo.base, OLD, false, GATE));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-EXPIRED: A " + GATE + " [EXPIRED-GATE]"),
                check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("ACTIVE-GATE", "ACTIVE", repo.base, VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
        repo.put(BASELINE, repo.control("VERIFIED-GATE", "VERIFIED", repo.base, VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-AMBIGUOUS: A " + GATE + " [ACTIVE-GATE, VERIFIED-GATE]"),
                check(repo).diagnostics());
        Files.delete(root.resolve(FEEDBACK));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void stagedDeletionCannotBeHiddenByRestoringTheWorkingFile(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.git("rm", "-q", TEST);
        repo.put(TEST, "package demo;\nclass Old {}\n");
        repo.put(FEEDBACK, repo.control("REVIEW-DELETE", "ACTIVE", repo.base, VALID, false, TEST));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-DELETE: D " + TEST + " needs allowDelete: true"),
                check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("REVIEW-DELETE", "ACTIVE", repo.base, VALID, true, TEST));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void rejectsMalformedMisplacedRepeatedAndIgnoredControlDocuments(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.put(GATE, "{}\n");
        String valid = repo.control("REVIEW-SCHEMA", "ACTIVE", repo.base, VALID, false, GATE);
        repo.put("docs/feedback/ignored-owner.md", valid);
        AuditedAssertions.assertEquals(List.of(uncovered("A", GATE, repo.base)), check(repo).diagnostics());
        for (String invalid : List.of(valid.replace("```\n", ""),
                valid.replace("    allowDelete:", "    state: TEMP\n    allowDelete:"),
                valid.replace("    status: ACTIVE", "    status: VERIFIED"),
                valid.replace(GATE, "config/*.json"), valid.replace(GATE, "config/../pom.xml"))) {
            repo.put(FEEDBACK, invalid);
            var rejected = AuditedAssertions.assertThrows(IOException.class, () -> check(repo));
            AuditedAssertions.assertTrue(rejected.getMessage().contains("TEST-CHANGE-SCHEMA"), rejected.getMessage());
        }
        repo.put(FEEDBACK, valid);
        repo.put(BASELINE, valid.replace("status: ACTIVE", "status: VERIFIED"));
        var duplicate = AuditedAssertions.assertThrows(IOException.class, () -> check(repo));
        AuditedAssertions.assertTrue(duplicate.getMessage().contains("TEST-CHANGE-ID: duplicate REVIEW-SCHEMA"));
    }

    private static TestChangeReview.Result check(TestChangeGitFixture repo) throws Exception {
        return TestChangeReview.inspect(repo.root, repo.base, Map.of(), TODAY);
    }

    private static String uncovered(String kind, String path, String base) {
        return "TEST-CHANGE-UNCOVERED: " + kind + " " + path + " (base " + base + ")";
    }
}

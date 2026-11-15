package atf.tests.governance.source.changes;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.testchange.TestChangeProof;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static atf.tests.governance.source.changes.TestChangeGitFixture.BASELINE;
import static atf.tests.governance.source.changes.TestChangeGitFixture.RESOURCE;

@AtfComponentTest
class TestChangeProofComponentTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final String ID = "FIXTURE-TEST-PROOF";
    private static final String POSITIVE = "example.component.ValidReviewTest#acceptsExactInputs";
    private static final String NEGATIVE = "example.component.ValidReviewTest#rejectsStaleInput";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void proofMatchesExactGitReviewAndDeclaredCases(@TempDir Path root) throws Exception {
        var fixture = fixture(root);
        var plan = fixture.plan();
        AuditedAssertions.assertEquals(fixture.git().base, plan.identity().base());
        AuditedAssertions.assertEquals(fixture.git().git("rev-parse", "HEAD"), plan.identity().commit());
        AuditedAssertions.assertTrue(plan.identity().diffSha256().matches("[a-f0-9]{64}"));
        AuditedAssertions.assertEquals(List.of(), fixture.verify().diagnostics());
    }

    @Test void proofFromAnotherCommitCannotBeReused(@TempDir Path root) throws Exception {
        var fixture = fixture(root);
        fixture.git().put(RESOURCE, "{\"revision\":2}\n");
        fixture.git().commit("second candidate with the same paths");
        refused(fixture, "COMMIT");
        var changed = fixture.receipt();
        ((ObjectNode) changed.get("identity")).put("commit", fixture.git().git("rev-parse", "HEAD"));
        fixture.write(changed);
        refused(fixture, "DIFF");
    }

    @Test void proofWithoutPassingNegativeControlIsRefused(@TempDir Path root) throws Exception {
        var fixture = fixture(root);
        var receipt = fixture.receipt();
        receipt.remove("negative");
        refusedAfterEdit(fixture, receipt, "negative");
        receipt.putArray("negative");
        refusedAfterEdit(fixture, receipt, "TEST-CHANGE-PROOF-NEGATIVE");
        receipt.withArray("negative").addObject().put("selector", NEGATIVE).put("verdict", "ABORTED");
        refusedAfterEdit(fixture, receipt, "TEST-CHANGE-PROOF-NEGATIVE");
        ((ObjectNode) receipt.withArray("negative").get(0)).put("verdict", "PASS");
        fixture.write(receipt);
        AuditedAssertions.assertEquals(List.of(), fixture.verify().diagnostics());
    }

    @Test void proofRequiresTheSameIdentityAndReportBytes(@TempDir Path root) throws Exception {
        var fixture = fixture(root);
        var receipt = fixture.receipt();
        ((ObjectNode) receipt.get("identity")).put("id", "ANOTHER-CONTROL");
        refusedAfterEdit(fixture, receipt, "TEST-CHANGE-PROOF-ID");
        ((ObjectNode) receipt.get("identity")).put("id", ID).put("base", "b".repeat(40));
        refusedAfterEdit(fixture, receipt, "TEST-CHANGE-PROOF-BASE");
        ((ObjectNode) receipt.get("identity")).put("base", fixture.git().base);
        fixture.write(receipt);
        Files.writeString(fixture.directory().resolve(TestChangeProof.REPORT), "<html>changed</html>");
        refused(fixture, "REPORT");
    }

    @Test void verifiedControlNeedsProofAndCleanCommittedInputs(@TempDir Path root) throws Exception {
        var fixture = fixture(root);
        var missing = AuditedAssertions.assertThrows(IOException.class, () -> TestChangeReview.verify(fixture.git().root,
                fixture.git().base, Map.of(), TODAY));
        AuditedAssertions.assertTrue(missing.getMessage().contains("TEST-CHANGE-PROOF-MISSING"));
        fixture.git().put(RESOURCE, "dirty");
        refused(fixture, "DIRTY");
        fixture.git().git("restore", "--", RESOURCE);
        fixture.git().put(TestChangeGitFixture.IGNORED, "uncommitted source");
        // The ignored input must be covered by the diff gate before proof eligibility is considered.
        AuditedAssertions.assertTrue(fixture.verify().diagnostics().stream()
                .anyMatch(message -> message.startsWith("TEST-CHANGE-UNCOVERED")));
        Files.delete(fixture.git().root.resolve(TestChangeGitFixture.IGNORED));
        fixture.git().put(BASELINE, fixture.git().control(ID, "VERIFIED", fixture.git().base, TODAY.toString(), false, RESOURCE));
        fixture.git().commit("declaration without checks");
        refused(fixture, "CHECKS");
    }

    private static Fixture fixture(Path root) throws Exception {
        var git = new TestChangeGitFixture(Files.createDirectory(root.resolve("checkout")));
        git.put(RESOURCE, "{\"revision\":1}\n");
        String control = git.control(ID, "VERIFIED", git.base, TODAY.toString(), false, RESOURCE)
                .replace("    paths:", "    checks:\n      positive: ['" + POSITIVE
                        + "']\n      negative: ['" + NEGATIVE + "']\n    paths:");
        git.put(BASELINE, control);
        git.commit("review candidate");
        Path directory = Files.createDirectories(root.resolve("proofs").resolve(ID)).toRealPath();
        var fixture = new Fixture(git, directory);
        Files.writeString(directory.resolve(TestChangeProof.REPORT), "<html>fixture report</html>");
        TestChangeProof.stage(fixture.plan(), List.of(new TestChangeProof.Outcome(POSITIVE, "PASS")),
                List.of(new TestChangeProof.Outcome(NEGATIVE, "PASS")), directory);
        Files.move(directory.resolve(TestChangeProof.PENDING), directory.resolve(TestChangeProof.RECEIPT));
        return fixture;
    }

    private static void refused(Fixture fixture, String code) {
        var error = AuditedAssertions.assertThrows(IOException.class, fixture::verify);
        AuditedAssertions.assertTrue(error.getMessage().contains("TEST-CHANGE-PROOF-" + code), error.getMessage());
    }

    private static void refusedAfterEdit(Fixture fixture, ObjectNode receipt, String diagnostic) throws IOException {
        fixture.write(receipt);
        var error = AuditedAssertions.assertThrows(IOException.class, fixture::verify);
        AuditedAssertions.assertTrue(error.getMessage().contains(diagnostic), error.getMessage());
    }

    private record Fixture(TestChangeGitFixture git, Path directory) {
        TestChangeProof.Plan plan() throws Exception {
            return TestChangeReview.proofPlan(git.root, git.base, ID, TODAY);
        }

        TestChangeReview.Result verify() throws Exception {
            return TestChangeReview.verify(git.root, git.base,
                    Map.of(TestChangeProof.ROOT_ENV, directory.getParent().toString()), TODAY);
        }

        ObjectNode receipt() throws IOException {
            return (ObjectNode) JSON.readTree(directory.resolve(TestChangeProof.RECEIPT).toFile());
        }

        void write(ObjectNode receipt) throws IOException {
            JSON.writeValue(directory.resolve(TestChangeProof.RECEIPT).toFile(), receipt);
        }
    }
}

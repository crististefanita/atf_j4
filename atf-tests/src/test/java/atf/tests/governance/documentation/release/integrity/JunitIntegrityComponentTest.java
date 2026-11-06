package atf.tests.governance.documentation.release.integrity;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.integrity.fixture.ReleaseFixture;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static atf.tests.governance.documentation.release.integrity.fixture.ReleaseFixture.suite;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class JunitIntegrityComponentTest {
    @TempDir Path temporary;
    @ParameterizedTest @ValueSource(strings = {"failure", "error", "skipped"})
    void actualNonPassingOutcomesAreRejectedEvenWithHonestCountsAndMatchingHashes(String outcome) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        int failures = outcome.equals("failure") ? 1 : 0, errors = outcome.equals("error") ? 1 : 0, skipped = outcome.equals("skipped") ? 1 : 0;
        fixture.xml(suite("<testcase classname=\"example.Game\" name=\"plays\"><" + outcome + "/></testcase>", 1, failures, errors, skipped));
        fixture.group().put("failures", failures).put("errors", errors).put("skipped", skipped);fixture.capture();
        rejected(fixture, "JUNIT-VERDICT");
    }
    @ParameterizedTest @ValueSource(strings = {"failure", "error", "skipped"})
    void zeroSummaryAttributesCannotHideAnActualNonPassingCase(String outcome) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        fixture.xml(suite("<testcase classname=\"example.Game\" name=\"plays\"><" + outcome + "/></testcase>", 1, 0, 0, 0));
        fixture.capture();rejected(fixture, "JUNIT-COUNTS");
    }
    @ParameterizedTest @ValueSource(strings = {"missing-file", "extra-file", "duplicate-file", "duplicate-case", "extra-group", "empty"})
    void theManifestCannotLoseAddOrCountTheSameInvocationTwice(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        switch (defect) {
            case "missing-file" -> Files.delete(fixture.result(ReleaseFixture.CASE_FILE));
            case "extra-file", "duplicate-case" -> {
                Files.copy(fixture.result(ReleaseFixture.CASE_FILE), fixture.result("TEST-extra.xml"));
                if (defect.equals("duplicate-case")) {
                    ((ArrayNode) fixture.group().path("files")).add("TEST-extra.xml");fixture.group().put("tests", 2);
                }
            }
            case "duplicate-file" -> ((ArrayNode) fixture.group().path("files")).add(ReleaseFixture.CASE_FILE);
            case "extra-group" -> Files.createDirectory(fixture.folder().resolve("reports/test-results/undeclared"));
            case "empty" -> { fixture.xml(suite("", 0, 0, 0, 0));fixture.group().put("tests", 0); }
            default -> fail(defect);
        }
        fixture.capture();rejected(fixture, defect.equals("missing-file") ? "FILE" : "JUNIT-");
    }
    @ParameterizedTest @ValueSource(strings = {"bad-total", "missing-count", "fractional-count", "overflow", "unknown-element", "contradictory", "missing-name", "doctype", "malformed", "wrong-root"})
    void malformedOrAmbiguousXmlIsRefusedAfterItsReceiptHasBeenUpdated(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        String testcase = "<testcase classname=\"example.Game\" name=\"plays\"/>";
        String xml = switch (defect) {
            case "bad-total" -> suite(testcase, 2, 0, 0, 0);
            case "missing-count" -> suite(testcase, 1, 0, 0, 0).replace(" tests=\"1\"", "");
            case "fractional-count" -> suite(testcase, 1, 0, 0, 0).replace("tests=\"1\"", "tests=\"1.5\"");
            case "overflow" -> suite(testcase, 1, 0, 0, 0).replace("tests=\"1\"", "tests=\"9223372036854775808\"");
            case "unknown-element" -> suite("<unrecognised/>", 0, 0, 0, 0);
            case "contradictory" -> suite("<testcase classname=\"example.Game\" name=\"plays\"><failure/><skipped/></testcase>", 1, 1, 0, 1);
            case "missing-name" -> suite("<testcase classname=\"example.Game\"/>", 1, 0, 0, 0);
            case "doctype" -> "<!DOCTYPE testsuite [<!ENTITY external SYSTEM '" + temporary.resolve("secret.txt").toUri() + "'>]>" + suite(testcase, 1, 0, 0, 0);
            case "malformed" -> "<testsuite>";
            case "wrong-root" -> "<report tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\"/>";
            default -> throw new IllegalArgumentException(defect);
        };
        Files.writeString(temporary.resolve("secret.txt"), "private-fixture-sentinel");
        fixture.xml(xml);fixture.capture();rejected(fixture, "JUNIT-");
    }
    @Test void nestedSuitesAreRecountedOnceAndEmptyWrappersDoNotInventTests() throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        fixture.xml("<testsuites tests=\"2\" failures=\"0\" errors=\"0\" skipped=\"0\">"
                + suite("<testcase classname=\"example.Game\" name=\"one\"/>", 1, 0, 0, 0)
                + suite("<testcase classname=\"example.Game\" name=\"two\"/>", 1, 0, 0, 0) + "</testsuites>");
        fixture.group().put("tests", 2);fixture.capture();
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseIntegrity.verify(temporary, fixture.folder()));
    }
    @Test void aDeclaredAggregateMustMatchTheCasesActuallyRead() throws Exception {
        var fixture = ReleaseFixture.create(temporary);fixture.group().put("tests", 2);fixture.save();
        rejected(fixture, "JUNIT-TOTALS");
    }
    private static void rejected(ReleaseFixture fixture, String code) {
        var failure = AuditedAssertions.assertThrows(IOException.class, () -> ReleaseIntegrity.verify(fixture.project(), fixture.folder()));
        AuditedAssertions.assertTrue(failure.getMessage().startsWith("RELEASE-INTEGRITY-" + code), failure::toString);
    }
}

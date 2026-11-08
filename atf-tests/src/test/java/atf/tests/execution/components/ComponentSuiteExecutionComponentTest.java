package atf.tests.execution.components;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ComponentSuiteExecutionComponentTest {
    @Test void provenanceReceiptHashesTheExactJsonFileBytes() throws Exception {
        byte[] provenance = "{\n  \"kind\": \"live-source-components\"\n}\n".getBytes(StandardCharsets.UTF_8);
        String exactFileHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(provenance));
        String parsedJsonHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(new ObjectMapper().readTree(provenance).toString().getBytes(StandardCharsets.UTF_8)));

        AuditedAssertions.assertEquals(exactFileHash, ComponentSuiteExecution.provenanceFingerprint(provenance));
        AuditedAssertions.assertNotEquals(parsedJsonHash, exactFileHash,
                "The receipt must bind the persisted file bytes, not a reserialized JSON representation");
    }
}

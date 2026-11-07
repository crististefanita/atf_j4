package ro.whistlab.core.ai.cost;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class CostProfileTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test public void validatedProfileOwnsItsListsDocumentAndExactByteIdentity() throws Exception {
        ObjectNode input = valid();
        byte[] bytes = JSON.writeValueAsBytes(input);
        var profile = CostProfile.parse(bytes);
        input.put("warmups", 0);
        profile.document().withArray("seeds").removeAll();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bytes", bytes, "input", input, "profile", profile);
        assertEquals("profile.seeds() must equal List.of(19, 101)", List.of(19, 101), profile.seeds());
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, () -> profile.seeds().clear());
        assertEquals("profile.warmups must equal 1", 1, profile.warmups);
        assertEquals("CostProfile.parse(bytes).sha256() must equal profile.sha256()", profile.sha256(), CostProfile.parse(bytes).sha256());
        assertNotEquals("CostProfile.parse((new String(bytes, StandardCharsets.UTF_8) + \" \").getBytes(StandardCharsets.UTF_8)).sha256() must differ from profile.sha256()", profile.sha256(), CostProfile.parse((new String(bytes, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8)).sha256());
    }

    @Test public void emptyDuplicateOrIncompleteDomainsCannotProduceAVacuousQualification() throws Exception {
        for (String field : List.of("seeds", "handSizes")) {
            var input = valid(); input.putArray(field);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "field", field, "input", input);
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(input)));
        }
        var duplicate = valid(); duplicate.putArray("seeds").add(19).add(19);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(duplicate)));
        var partial = valid(); partial.withArray("handSizes").remove(6);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(partial)));
    }

    @Test public void numbersCannotBeTruncatedOverflowedOrCoercedIntoValidLimits() throws Exception {
        for (String field : List.of("warmups", "repetitions", "maximumWorkUnits", "coreProofNodes")) {
            for (String value : List.of("0", "-1", "1.5", "\"1\"", "18446744073709551617")) {
                var input = valid(); input.set(field, JSON.readTree(value));

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "field", field, "input", input, "value", value);
                assertThrows(field + "=" + value, IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(input)));
            }
        }
        for (String field : List.of("warmups", "repetitions")) {
            var input = valid(); input.put(field, BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(input)));
        }
        var fractionalVersion = valid(); fractionalVersion.put("schemaVersion", 1.5);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> CostProfile.parse(JSON.writeValueAsBytes(fractionalVersion)));
    }

    private static ObjectNode valid() {
        var value = JSON.createObjectNode();
        value.put("schemaVersion", 1); value.put("players", 4); value.put("scope", "constructed profile component");
        value.putArray("handSizes").add(2).add(3).add(4).add(5).add(6).add(7).add(8);
        value.putArray("seeds").add(19).add(101);
        for (String key : List.of("warmups", "repetitions", "maximumWorkUnits", "maximumThreadCpuNanos", "maximumWallNanos",
                "coreBidSamples", "coreCardSamples", "coreEnumeratedWorlds", "independentBidSamples", "independentCardSamples",
                "coreProofNodes", "independentProofNodes")) value.put(key, 1);
        return value;
    }
}

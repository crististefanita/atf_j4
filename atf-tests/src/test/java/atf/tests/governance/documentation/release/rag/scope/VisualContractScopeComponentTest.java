package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.*;

@AtfComponentTest
class VisualContractScopeComponentTest {
    private static final String DISPLAY = "responsive-ui-sanity", SMOKE = "signed-release-smoke";
    private static final String AUTO = "signed-release-auto-game", BID = "signed-release-bidding";
    private static final String HISTORY = "history-release-sanity", ACHIEVEMENTS = "signed-release-achievements";
    private static final String PROFILE = "signed-release-home-profile";
    private static final List<String> ALL = List.of(HISTORY, DISPLAY, ACHIEVEMENTS, AUTO, BID, PROFILE, SMOKE);

    @Test void provenTypographyKeepsNativeSanityAndItsComponentOwners() throws Exception {
        var header = probe("header");
        assertFlows(header, List.of(DISPLAY, SMOKE), "Only the declared header constants changed");
        var contract = header.path("selection").path("changes").get(0).path("visualContract");
        AuditedAssertions.assertEquals("PROVEN", contract.path("status").asText());
        AuditedAssertions.assertEquals(List.of("whist-lab-app-display"), values(contract.path("componentGroups")));
        AuditedAssertions.assertEquals("TITLE_SP", contract.path("constants").get(0).path("name").asText());
        AuditedAssertions.assertEquals("14f", contract.path("constants").get(0).path("before").asText());
        AuditedAssertions.assertEquals("16f", contract.path("constants").get(0).path("after").asText());
        var cards = probe("cards");
        assertFlows(cards, List.of(DISPLAY, AUTO, SMOKE), "Card ranks retain the collection and AutoGame sanity");
        AuditedAssertions.assertEquals(List.of("whist-lab-app-display", "whist-lab-app-atf-card-table-ui"),
                values(cards.path("selection").path("changes").get(0).path("visualContract").path("componentGroups")));
    }

    @Test void mixedBehaviorAndBinaryChangesCannotLoseMandatoryFlows() throws Exception {
        var expected = Map.of("bidding", List.of(DISPLAY, AUTO, BID, SMOKE),
                "persistence", List.of(HISTORY, DISPLAY, ACHIEVEMENTS, AUTO, BID, SMOKE),
                "scoring", List.of(HISTORY, DISPLAY, SMOKE), "profile", List.of(DISPLAY, PROFILE, SMOKE),
                "achievements", List.of(DISPLAY, ACHIEVEMENTS, SMOKE), "binary", ALL, "unknown", ALL);
        for (var entry : expected.entrySet()) assertFlows(probe(entry.getKey()), entry.getValue(), entry.getKey());
    }

    @Test void unprovenContractsKeepEveryNativeFlow() throws Exception {
        for (String name : List.of("missing", "wrong-hash", "missing-gate", "added", "deleted", "method",
                "expression", "zero", "renamed", "duplicate", "format-only")) {
            var result = probe(name);
            assertFlows(result, ALL, name);
            AuditedAssertions.assertEquals("FALLBACK", result.path("selection").path("changes").get(0)
                    .path("visualContract").path("status").asText(), name);
        }
    }

    @Test void theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative() throws Exception {
        String output = runContracts(Duration.ofMinutes(2), "visual-contracts.test.mjs");
        AuditedAssertions.assertTrue(output.contains("the real planner narrows only a pinned numeric contract and retains native sanity"));
        AuditedAssertions.assertTrue(output.contains("a missing baseline Git object keeps every native flow instead of trusting a visual claim"));
        AuditedAssertions.assertTrue(output.contains("registered visual contracts retain live source and declared component owners"));
    }

    private static JsonNode probe(String name) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = runNode(List.of("node", root.resolve("atf-tests/tools/visual-scope-fixture.mjs").toString(), name),
                root, Duration.ofSeconds(30));
        return new ObjectMapper().readTree(output);
    }

    private static void assertFlows(JsonNode probe, List<String> expected, String context) {
        AuditedAssertions.assertEquals(expected, values(probe.path("selection").path("requiredE2eGroups")), context);
        AuditedAssertions.assertTrue(probe.path("selection").path("userFlowChanged").asBoolean(), context);
        AuditedAssertions.assertEquals("USER_FLOW", probe.path("selection").path("kind").asText(), context);
    }

    private static List<String> values(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }
}

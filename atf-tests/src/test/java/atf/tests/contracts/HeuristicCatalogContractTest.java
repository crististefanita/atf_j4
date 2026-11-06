package atf.tests.contracts;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import static org.junit.jupiter.api.Assertions.*;import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;import java.nio.file.*;import java.util.*;import org.junit.jupiter.api.Test;
@AtfComponentTest
class HeuristicCatalogContractTest {
 private JsonNode load()throws Exception{return new ObjectMapper(new YAMLFactory()).readTree(Files.readString(Path.of("..","config","whist-hand-patterns.yaml")));}
 @Test void generatedYamlSeparatesGuaranteedRulesAndConditionalNonAdditiveHints()throws Exception{
  var doc=load();AuditedAssertions.assertEquals(1,doc.path("schema_version").asInt());AuditedAssertions.assertEquals("EXACT_CONTRACT_PROBABILITY",doc.path("primary_objective").asText());var ids=new HashSet<String>();for(var row:doc.path("rules")){AuditedAssertions.assertTrue(ids.add(row.path("id").asText()));AuditedAssertions.assertFalse(row.path("additive").asBoolean());AuditedAssertions.assertFalse(row.path("description_ro").asText().isBlank());AuditedAssertions.assertFalse(row.path("description_en").asText().isBlank());AuditedAssertions.assertTrue(Set.of("HIGHEST_CARD_CAPTURE","SUIT_TRICKS").contains(row.path("hint_scope").asText()));}
  AuditedAssertions.assertTrue(ids.containsAll(Set.of("MASTER_TRUMP","KING_WITH_LOW","QUEEN_TWO_LOW","ACE_OFF_TRUMP","KING_QUEEN_WITH_LOW","KING_QUEEN_JACK_WITH_LOW")));
 }
 @Test void unvalidatedContextWeightsCannotSilentlyBecomeRuntimeProbabilities()throws Exception{
  var doc=load();AuditedAssertions.assertEquals(0,doc.path("runtime_prior_influence").asInt());AuditedAssertions.assertEquals("development_only",doc.path("calibration_policy").path("partition").asText());AuditedAssertions.assertTrue(doc.path("calibration_policy").path("heldout").asText().contains("never"));AuditedAssertions.assertEquals(20,doc.path("calibration_policy").path("prior_worlds").asInt());AuditedAssertions.assertTrue(doc.path("required_context").size()>=10);
 }
}

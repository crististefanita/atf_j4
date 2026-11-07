package ro.whistlab.core.learning.mining;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.testing.ForcedContract;
import ro.whistlab.core.testing.PublicEndgameOracle;
import java.nio.file.*;import java.util.*;import com.fasterxml.jackson.databind.*;import org.junit.Test;import static atf.gradle.audit.assertions.GradleAssertions.*;
public class ArenaLossMiningTest extends atf.gradle.audit.GradleAuditedTest {
 @Test public void proveOnlyFromSavedOwnCardsAndPublicFacts()throws Exception{
  var json=new ObjectMapper();var data=json.readTree(Files.readAllBytes(Path.of(System.getProperty("whist.loss.positions"))));Path output=Path.of(System.getProperty("whist.loss.output"));Files.createDirectories(output);var results=new ArrayList<Map<String,Object>>();int mistakes=0,complete=0,certificates=0;
  for(var input:data.path("positions")){var view=ro.whistlab.core.testing.codec.ObservedPositionJson.view(json,input.path("before"));var proof=ForcedContract.prove(view);if(proof.complete())complete++;if(!proof.cards().isEmpty())certificates++;var result=new LinkedHashMap<String,Object>();result.put("id",input.path("id").asText());result.put("complete",proof.complete());result.put("worlds",proof.worlds());result.put("nodes",proof.nodes());result.put("safe",proof.cards().stream().map(LossMiningTest::code).toList());
   if(proof.complete()&&!proof.cards().isEmpty()&&!proof.cards().stream().map(LossMiningTest::code).toList().contains(input.path("actualChoice").asText())){mistakes++;var oracle=PublicEndgameOracle.audit(view);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "certificates", certificates, "complete", complete, "data", data, "input", input, "json", json, "mistakes", mistakes, "oracle", oracle, "output", output, "proof", proof, "result", result, "results", results, "view", view);
assertTrue("Independent list-based oracle must certify the proposed alternatives",oracle.safe().containsAll(proof.cards().stream().map(LossMiningTest::code).toList()));result.put("input",input);result.put("independentOracle",oracle);Files.writeString(output.resolve("mistake-"+mistakes+".json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(result));}
   results.add(result);
  }
  assertTrue("results.size() > 0 must hold", results.size()>0);Files.writeString(output.resolve("summary.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("positions",results.size(),"complete",complete,"nonemptyCertificates",certificates,"mistakes",mistakes,"cases",results)));
 }
}

package ro.whistlab.core.ai.policy;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import static atf.gradle.audit.assertions.GradleAssertions.*;import java.util.*;import com.fasterxml.jackson.databind.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
/** Small public counterexamples: a promoted low card can be an unavoidable winner. */
@RunWith(Parameterized.class)public class PromotedMasterLossTest extends atf.gradle.audit.GradleAuditedTest {
 @Parameterized.Parameters(name="case={0}")public static Collection<Object[]> cases(){return List.of(new Object[][]{{0},{1},{2},{3}});}private final int index;public PromotedMasterLossTest(int index){this.index=index;}
 @Test public void publicPromotionExplainsTheUnavoidableMissWithoutInventingAnOptimalBid()throws Exception{
  var json=new ObjectMapper();var f=json.readTree(getClass().getResourceAsStream("/regressions/bid-forecast/promoted-master-losses.json")).get(index);var v=ro.whistlab.core.testing.codec.ObservedPositionJson.view(json,f.path("before"));int expectedWon=f.path("expectedFinalWon").asInt(),expectedPoints=f.path("expectedPoints").asInt();var context=new LinkedHashMap<String,Object>();context.put("purpose","Classify a lost contract: the final two tricks cannot be ducked after promotion");context.put("rule",f.path("rule").asText());context.put("origin",f.path("origin").asText());context.put("sourceHand",f.path("sourceHand").asInt());context.put("sourceSha256",f.path("sourceSha256").asText());context.put("before",f.path("before"));context.put("scope","Own/public cards only. This proves the late miss is unavoidable; the original bid is not declared optimal or wrong by its ex-post outcome.");
  CapturedRegression.run(f.path("id").asText(),getClass().getName(),"publicPromotionExplainsTheUnavoidableMissWithoutInventingAnOptimalBid",context,c->{
   var oracle=index<3?PublicEndgameOracle.audit(v):rankBound(v,expectedWon);context.put("independentOracle",Map.of("method",index<3?"Complete independent list enumeration":"Symbolic rank bound: own lowest trump exceeds every unknown trump; no hidden deal needed","worldsEnumerated",oracle.worlds(),"finalHeroTricksByChoice",oracle.finalWon()));
   if(index<3)c.equal("At least one feasible public world exists",true,oracle.worlds()>0);for(var row:oracle.finalWon().entrySet())c.equal("Every compatible continuation after "+row.getKey()+" forces the same final tally",Set.of(expectedWon),row.getValue());
   c.equal("No alternative guarantees the already missed contract",List.of(),oracle.safe());var expert=MonteCarloAi.chooseCard(v);for(var candidate:expert.candidates())c.equal("Expected points remain points, not an exact-contract probability",(double)expectedPoints,candidate.meanContractPoints());
   c.equal("Expert action has the proved unavoidable tally",Set.of(expectedWon),oracle.finalWon().get(LossMiningTest.code(expert.card())));int independent=IndependentPlayer.card(PlayerObservations.independent(v)).value();c.equal("Independent action has the proved unavoidable tally",Set.of(expectedWon),oracle.finalWon().get(PublicEndgameOracle.code(independent)));
  });
 }
 private static PublicEndgameOracle.Outcome rankBound(MonteCarloAi.View v,int finalWon){assertTrue("v.trick().isEmpty() must hold", v.trick().isEmpty());int lowest=v.hand().stream().mapToInt(Card::rank).min().orElseThrow();for(Card c:v.hand())assertEquals("c.suit().ordinal() must equal v.trump().ordinal()", v.trump().ordinal(),c.suit().ordinal());for(Card c:MonteCarloAi.unknownCards(v))if(c.suit().ordinal()==v.trump().ordinal())assertTrue("No unseen trump may exceed the held low trump",c.rank()<lowest);var outcomes=new LinkedHashMap<String,Set<Integer>>();for(Card c:v.hand())outcomes.put(LossMiningTest.code(c),Set.of(finalWon));return new PublicEndgameOracle.Outcome(List.of(),0,outcomes,Map.of());}
}

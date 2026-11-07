package ro.whistlab.core.ai.policy;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import static atf.gradle.audit.assertions.GradleAssertions.*;import java.util.*;import com.fasterxml.jackson.databind.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
@RunWith(Parameterized.class)
public class LostGameContractRegressionTest extends atf.gradle.audit.GradleAuditedTest {
 @Parameterized.Parameters(name="{0}") public static Collection<Object[]> cases(){var out=new ArrayList<Object[]>();for(int i=1;i<=4;i++)out.add(new Object[]{String.format("mistake-%03d",i)});return out;}
 private final String id;public LostGameContractRegressionTest(String id){this.id=id;}

 private static String frozenChoice(MonteCarloAi.View v,String role){
  if(role.equals("expert")){var old=new ro.whistlab.core.baseline.v4.RoundFourMonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats().stream().map(s->new ro.whistlab.core.baseline.v4.RoundFourMonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),v.trick(),v.history(),v.turnedCard(),v.streakBonuses(),v.streakBonusPoints());return LossMiningTest.code(ro.whistlab.core.baseline.v4.RoundFourMonteCarloAi.chooseCard(old).card());}
  var p=PlayerObservations.independent(v);var old=new ro.whistlab.coach.baseline.RoundFourIndependentPlayer.Position(p.seat(),p.dealer(),p.size(),p.trump(),p.hand(),p.seats().stream().map(s->new ro.whistlab.coach.baseline.RoundFourIndependentPlayer.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),p.trick().stream().map(x->new ro.whistlab.coach.baseline.RoundFourIndependentPlayer.Play(x.seat(),x.card())).toList(),p.history().stream().map(x->new ro.whistlab.coach.baseline.RoundFourIndependentPlayer.Play(x.seat(),x.card())).toList(),p.turnedCard(),p.streakBonuses(),p.streakBonusPoints());return PublicEndgameOracle.code(ro.whistlab.coach.baseline.RoundFourIndependentPlayer.card(old).value());
 }
 @Test public void knownLostGameDecisionRespectsTheIndependentGuaranteedContractCertificate()throws Exception {
  var json=new ObjectMapper();byte[] bytes=getClass().getResourceAsStream("/regressions/forced-contract/"+id+".json").readAllBytes();var fixture=json.readTree(bytes);var view=ro.whistlab.core.testing.codec.ObservedPositionJson.view(json,fixture.path("before"));var literal=new ArrayList<String>();for(var c:fixture.path("guaranteed"))literal.add(c.asText());Collections.sort(literal);
  var context=new LinkedHashMap<String,Object>();context.put("purpose","Replace a risky late discard with a contract-preserving rank choice");context.put("rule","With two same-suit cards and a fixed current-trick result, retain the high card only if another trick is needed. A low retained card weakly reduces the risk of an unwanted final trick.");context.put("profile","Romanian four players; series +10/-5. Component public-position replay, no UI and no real hidden opponent cards supplied to either player.");context.put("origin",fixture.path("gameId"));context.put("sourceLimit",fixture.path("explanation"));context.put("before",fixture.path("before"));context.put("oldChoice",fixture.path("oldChoice"));context.put("fixtureSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
  CapturedRegression.run(id,getClass().getName(),"knownLostGameDecisionRespectsTheIndependentGuaranteedContractCertificate",context,c->{
   c.equal("Frozen V4 original-role choice is reproduced",fixture.path("oldChoice").asText(),frozenChoice(view,fixture.path("role").asText()));
   var oracle=PublicEndgameOracle.audit(view);context.put("independentOracle",Map.of("method","List-based enumeration of every feasible remaining deal and every legal opponent continuation; own last card is forced","worlds",oracle.worlds(),"possibleFinalHeroTricksByChoice",oracle.finalWon(),"oldChoiceFailureWitness",oracle.witness()));
   c.equal("Independent feasible world count",fixture.path("worlds").asInt(),oracle.worlds());c.equal("Literal safe alternatives proved independently",literal,oracle.safe());c.equal("Old choice admits a missed contract in a compatible continuation",true,!oracle.finalWon().get(fixture.path("oldChoice").asText()).equals(Set.of(view.seats().get(view.seat()).bid())));
   String expert=LossMiningTest.code(MonteCarloAi.chooseCard(view).card()),player=PublicEndgameOracle.code(IndependentPlayer.card(PlayerObservations.independent(view)).value());context.put("after",Map.of("expertChoice",expert,"independentChoice",player));c.equal("Expert chooses the independently certified card",literal.get(0),expert);c.equal("Independent player chooses the independently certified card",literal.get(0),player);
  });
 }
}

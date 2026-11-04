package ro.whistlab.core.ai.contract;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import java.util.*;import com.fasterxml.jackson.databind.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
@RunWith(Parameterized.class)
public class ObservedLateMissRegressionTest {
 @Parameterized.Parameters(name="{0}")public static Collection<Object[]> cases(){return List.of(new Object[][]{{"observed-miss-006",1},{"observed-miss-019",3}});}
 private final String id;private final int expectedLastWinner;public ObservedLateMissRegressionTest(String id,int winner){this.id=id;expectedLastWinner=winner;}
 private static List<int[]> plays(JsonNode list){var result=new ArrayList<int[]>();for(var play:list)result.add(new int[]{play.path("seat").asInt(),PublicEndgameOracle.id(LossMiningTest.card(play.path("card").asText()))});return result;}
 @Test public void recordedMissBecomesExactWhenTheDangerousRetainedRankIsDiscarded()throws Exception {
  var json=new ObjectMapper();var fixture=json.readTree(getClass().getResourceAsStream("/regressions/observed-late/"+id+".json"));var view=json.treeToValue(fixture.path("before"),MonteCarloAi.View.class);var context=new LinkedHashMap<String,Object>();context.put("purpose","Observed contract miss reduced to two final tricks");context.put("rule","With the current trick already lost, discard the high same-suit card when the contract is already met. Preserve the lower card for the forced final play.");context.put("scope",fixture.path("scope"));context.put("sourceGame",fixture.path("gameId"));context.put("before",fixture.path("before"));context.put("oracleContinuationNotGivenToPlayers",Map.of("originalPenultimateTrick",fixture.path("originalCurrentTrick"),"originalFinalTrick",fixture.path("lastTrick"),"correctedFinalTrick",fixture.path("swappedLastTrick")));
  CapturedRegression.run(id,getClass().getName(),"recordedMissBecomesExactWhenTheDangerousRetainedRankIsDiscarded",context,c->{
   int oldWinner=PublicEndgameOracle.winner(plays(fixture.path("lastTrick").path("plays")),view.trump().ordinal()),newWinner=PublicEndgameOracle.winner(plays(fixture.path("swappedLastTrick")),view.trump().ordinal());
   var oldCurrent=plays(fixture.path("originalCurrentTrick").path("plays"));var correctedCurrent=new ArrayList<int[]>();for(var play:oldCurrent)correctedCurrent.add(play[0]==view.seat()?new int[]{play[0],PublicEndgameOracle.id(LossMiningTest.card(fixture.path("newChoice").asText()))}:play);
   int currentWinner=PublicEndgameOracle.winner(oldCurrent,view.trump().ordinal()),correctedCurrentWinner=PublicEndgameOracle.winner(correctedCurrent,view.trump().ordinal());int oldWon=view.seats().get(view.seat()).won()+(currentWinner==view.seat()?1:0)+(oldWinner==view.seat()?1:0),newWon=view.seats().get(view.seat()).won()+(correctedCurrentWinner==view.seat()?1:0)+(newWinner==view.seat()?1:0);
   c.equal("Zero contract is already met before the late choice",List.of(0,0),List.of(view.seats().get(view.seat()).bid(),view.seats().get(view.seat()).won()));c.equal("Changing the discard preserves the penultimate trick winner",currentWinner,correctedCurrentWinner);c.equal("Recorded cumulative tricks",1,oldWon);c.equal("Corrected cumulative tricks on this legal continuation",0,newWon);
   c.equal("Recorded final trick was won by this player",view.seat(),oldWinner);c.equal("Low retained card loses to the recorded legal continuation",expectedLastWinner,newWinner);
   c.equal("Expert discards the dangerous high card",fixture.path("newChoice").asText(),LossMiningTest.code(MonteCarloAi.chooseCard(view).card()));c.equal("Independent player makes the same rule-based correction",fixture.path("newChoice").asText(),PublicEndgameOracle.code(IndependentPlayer.card(PlayerObservations.independent(view)).value()));
   c.equal("Original miss base score, before optional series",-1,GameRules.scoreDelta(oldWon,view.seats().get(view.seat()).bid(),GameMode.ROMANIAN_WHIST));c.equal("Corrected exact-zero base score, before optional series",5,GameRules.scoreDelta(newWon,view.seats().get(view.seat()).bid(),GameMode.ROMANIAN_WHIST));
  });
 }
}

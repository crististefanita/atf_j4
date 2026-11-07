package ro.whistlab.core.learning.evaluation;

import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.testing.CapturedRegression;
import java.util.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import com.fasterxml.jackson.databind.ObjectMapper;
@RunWith(Parameterized.class)
public class ArenaProfileReplayTest extends atf.gradle.audit.GradleAuditedTest {
 @Parameterized.Parameters(name="{0}")public static Collection<Object[]> cases(){return List.of(new Object[][]{{"factorial"},{"mixed"}});}
 final String league;public ArenaProfileReplayTest(String league){this.league=league;}
 @Test public void archivedArenaProfilesKeepTheirActualStrategiesAfterReleaseBinding()throws Exception{
  var json=new ObjectMapper();var expected=json.readTree(getClass().getResourceAsStream("/arena-replay/"+league+".json"));
  List<OfflineMatchHarness.Policy> policies=league.equals("factorial")?List.of(OfflineMatchHarness.Policy.PLAYER_V6,OfflineMatchHarness.Policy.PLAYER_V8,OfflineMatchHarness.Policy.SAFETY_GREEDY,OfflineMatchHarness.Policy.SAFETY_COVERS):List.of(OfflineMatchHarness.Policy.EXPERT_V6,OfflineMatchHarness.Policy.EXPERT_V10,OfflineMatchHarness.Policy.PLAYER_V8,OfflineMatchHarness.Policy.SAFETY_COVERS);
  CapturedRegression.run("arena-profile-replay-"+league,getClass().getName(),"archivedArenaProfilesKeepTheirActualStrategiesAfterReleaseBinding",Map.of("collection","verification","purpose","Keep experimental profiles distinct from the accepted production player","scope","Replay of a development snapshot verifies controller binding only, not optimality.","league",league,"seed",7100,"sourceGameId",expected.path("id").asText()),capture->{
   var actual=OfflineMatchHarness.playWithControllers("arena-replay-"+league,7100,0,policies,g->{});
   capture.equal("All24 hands retain deals, bids, cards, winners and scores",expected.path("hands"),json.valueToTree(actual.get("hands")));capture.equal("Final scores reproduce",expected.path("finalScores"),json.valueToTree(actual.get("finalScores")));
  });
 }
}

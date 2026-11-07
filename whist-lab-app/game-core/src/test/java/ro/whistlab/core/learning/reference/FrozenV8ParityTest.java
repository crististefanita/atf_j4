package ro.whistlab.core.learning.reference;

import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.testing.CapturedRegression;
import java.util.*;import org.junit.Test;import com.fasterxml.jackson.databind.ObjectMapper;
public class FrozenV8ParityTest extends atf.gradle.audit.GradleAuditedTest {
 @Test public void acceptedIndependentSnapshotIsReproducedBeforeComparingSafetyChanges()throws Exception{
  var json=new ObjectMapper();var expected=json.readTree(getClass().getResourceAsStream("/baseline-v8/snapshot-seed6700-seat0.json"));
  CapturedRegression.run("frozen-v8-snapshot-parity",getClass().getName(),"acceptedIndependentSnapshotIsReproducedBeforeComparingSafetyChanges",Map.of("collection","verification","purpose","Validate that the comparison baseline is exactly the accepted Independent revision8, not a weakened substitute","rule","Same development seed6700, hero seat0 and frozen ExpertV6 opponents must reproduce the complete24-hand trace and final scores.","scope","Snapshot reproducibility only. Reproducing historical actions does not label them optimal.","sourceGameId",expected.path("id").asText()),capture->{
   var actual=OfflineMatchHarness.play("frozen-v8-parity",6700,0,OfflineMatchHarness.Policy.PLAYER_V8,OfflineMatchHarness.Policy.EXPERT_V6);
   capture.equal("All dealt cards, bids, plays, trick winners and hand scores reproduce",expected.path("hands"),json.valueToTree(actual.get("hands")));capture.equal("Complete final scoreboard reproduces",expected.path("finalScores"),json.valueToTree(actual.get("finalScores")));
  });
 }
}

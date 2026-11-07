package ro.whistlab.core.learning.evaluation;
import static ro.whistlab.core.testing.match.OfflineMatchHarness.Policy.*;

import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.testing.RoundFourGameOracle;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;

public class RoundFourGameOracleTest extends atf.gradle.audit.GradleAuditedTest {
    private static Map<String,Object> valid;
    @BeforeClass public static void completeReferenceGame(){valid=OfflineMatchHarness.play("oracle-reference-v3",811,0,PLAYER_V3,EXPERT_V3);}
    @Test public void everyHandHasEvaluatedComparisonsAndUniqueCheckpointIdentity(){var checks=RoundFourGameOracle.audit(valid);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "checks", checks);
assertEquals("checks.size() must equal 26", 26,checks.size());assertTrue("checks.stream().flatMap((c)->c.comparisons().stream()).allMatch(RoundFourGameOracle.Comparison::passed) must hold", checks.stream().flatMap(c->c.comparisons().stream()).allMatch(RoundFourGameOracle.Comparison::passed));assertEquals("checks.stream().map(RoundFourGameOracle.Checkpoint::owner).distinct().count() must equal 26", 26,checks.stream().map(RoundFourGameOracle.Checkpoint::owner).distinct().count());}
    @SuppressWarnings("unchecked") @Test public void wrongWinnerActorScoreAndFinalRowAreIndependentlyRefused(){
        for(String mutation:List.of("winner","actor","score","last")){
            Map<String,Object> changed=new ObjectMapper().convertValue(valid,Map.class);var hands=(List<Map<String,Object>>)changed.get("hands");var first=hands.get(0);var trick=((List<Map<String,Object>>)first.get("tricks")).get(0);
            switch(mutation){case "winner"->trick.put("winner",(((Number)trick.get("winner")).intValue()+1)%4);case "actor"->{var play=((List<Map<String,Object>>)trick.get("plays")).get(0);play.put("seat",(((Number)play.get("seat")).intValue()+1)%4);}case "score"->((List<Map<String,Object>>)first.get("scores")).get(0).put("total",12345);case "last"->changed.put("finalScores",List.of(-999,-999,-999,-999));}

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "changed", changed, "first", first, "hands", hands, "mutation", mutation, "trick", trick);
            assertTrue(mutation,RoundFourGameOracle.audit(changed).stream().flatMap(c->c.comparisons().stream()).anyMatch(c->!c.passed()));
        }
    }
}

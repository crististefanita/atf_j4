package ro.whistlab.core;

import static org.junit.Assert.*;
import static ro.whistlab.core.ComponentCorpusCollectorTest.Policy.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;

public class RoundFourGameOracleTest {
    private static Map<String,Object> valid;
    @BeforeClass public static void completeReferenceGame(){valid=ComponentCorpusCollectorTest.play("oracle-reference-v3",811,0,PLAYER_V3,EXPERT_V3);}
    @Test public void everyHandHasEvaluatedComparisonsAndUniqueCheckpointIdentity(){var checks=RoundFourGameOracle.audit(valid);assertEquals(26,checks.size());assertTrue(checks.stream().flatMap(c->c.comparisons().stream()).allMatch(RoundFourGameOracle.Comparison::passed));assertEquals(26,checks.stream().map(RoundFourGameOracle.Checkpoint::owner).distinct().count());}
    @SuppressWarnings("unchecked") @Test public void wrongWinnerActorScoreAndFinalRowAreIndependentlyRefused(){
        for(String mutation:List.of("winner","actor","score","last")){
            Map<String,Object> changed=new ObjectMapper().convertValue(valid,Map.class);var hands=(List<Map<String,Object>>)changed.get("hands");var first=hands.get(0);var trick=((List<Map<String,Object>>)first.get("tricks")).get(0);
            switch(mutation){case "winner"->trick.put("winner",(((Number)trick.get("winner")).intValue()+1)%4);case "actor"->{var play=((List<Map<String,Object>>)trick.get("plays")).get(0);play.put("seat",(((Number)play.get("seat")).intValue()+1)%4);}case "score"->((List<Map<String,Object>>)first.get("scores")).get(0).put("total",12345);case "last"->changed.put("finalScores",List.of(-999,-999,-999,-999));}
            assertTrue(mutation,RoundFourGameOracle.audit(changed).stream().flatMap(c->c.comparisons().stream()).anyMatch(c->!c.passed()));
        }
    }
}

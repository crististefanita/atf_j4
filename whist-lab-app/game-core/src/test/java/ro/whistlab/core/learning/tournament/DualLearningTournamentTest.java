package ro.whistlab.core.learning.tournament;
import static ro.whistlab.core.testing.match.OfflineMatchHarness.Policy.*;






import ro.whistlab.core.testing.match.OfflineMatchHarness;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;


/** Same deal and hero seat; frozen opponents prevent apparent progress by weakening the other side. */
public class DualLearningTournamentTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private record Result(int points,int exact,double wins) { }
    @Test public void bothRolesMustImproveAgainstFrozenOpponentsAndEveryGameIsRetained()throws Exception{
        String corpus=property("whist.corpusDir"),out=property("whist.learningOutput"),candidate=property("whist.learningCandidate");
        assertTrue("Candidate ID must be a safe immutable file component",candidate.matches("[a-z0-9-]+"));
        String phase=System.getProperty("whist.learningPhase","");if(phase.isBlank())phase="development";
        assertTrue(phase.equals("development")||phase.equals("heldout"));boolean held=phase.equals("heldout");
        Files.createDirectories(Path.of(out));var hashes=sourceHashes();Path manifest=Path.of(out,"candidate-source.json");
        if(Files.exists(manifest))assertEquals("Changed source requires a new candidate ID",JSON.valueToTree(hashes),JSON.readTree(Files.readString(manifest)));
        else {assertFalse("Heldout candidate must already have a source receipt",held);write(manifest,hashes);}
        if(held){var frozen=JSON.readTree(Files.readString(Path.of(out,"candidate-freeze.json")));assertEquals(candidate,frozen.path("candidate").asText());assertEquals("FROZEN_BEFORE_HELDOUT",frozen.path("status").asText());assertEquals(JSON.valueToTree(hashes),frozen.path("sourceHashes"));}
        int first=held?60000:2000,seeds=held?16:8;Path folder=Path.of(corpus,"games",phase);Files.createDirectories(folder);Files.createDirectories(Path.of(out));
        var receipt=new LinkedHashMap<String,Object>();var families=new ArrayList<Map<String,Object>>();receipt.put("candidate",candidate);receipt.put("phase",phase);receipt.put("firstSeed",first);receipt.put("seeds",seeds);receipt.put("families",families);receipt.put("criterion","Each fixed family: win share >= baseline and mean score drop <=2; each role: strict aggregate win or score gain");
        boolean passed=true;int pairs=0;String selected=System.getProperty("whist.learningRole","");
        for(String role:List.of("player","expert")){
            if(!selected.isBlank()&&!selected.equals(role))continue;
            int roleOld=0,roleNew=0;double roleOldWins=0,roleNewWins=0;
            for(var opponent:role.equals("player")?List.of(EXPERT_V2):List.of(EXPERT_V2,PLAYER_V2)){
                int oldPoints=0,newPoints=0,oldExact=0,newExact=0;double oldWins=0,newWins=0;
                var details=new ArrayList<Map<String,Object>>();var family=OfflineMatchHarness.map("role",role,"opponent",opponent.name(),"pairs",details);families.add(family);
                for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++){
                    var oldPolicy=role.equals("player")?PLAYER_V2:EXPERT_V2;var newPolicy=role.equals("player")?PLAYER_CANDIDATE:EXPERT_CANDIDATE;
                    String key="-"+role+"-vs-"+opponent.name().toLowerCase(Locale.ROOT)+"-seed"+seed+"-hero"+seat;
                    Result before=game(folder,phase,"learning-baseline"+key,seed,seat,oldPolicy,opponent);
                    Result after=game(folder,phase,"learning-"+candidate+key,seed,seat,newPolicy,opponent);
                    oldPoints+=before.points;newPoints+=after.points;oldWins+=before.wins;newWins+=after.wins;oldExact+=before.exact;newExact+=after.exact;pairs++;
                    details.add(OfflineMatchHarness.map("seed",seed,"seat",seat,"beforePoints",before.points,"afterPoints",after.points,"beforeWinShare",before.wins,"afterWinShare",after.wins,"beforeExact",before.exact,"afterExact",after.exact));
                    receipt.put("status","RUNNING");receipt.put("pairsCompleted",pairs);write(Path.of(out,phase+"-results.json"),receipt);
                }
                int count=seeds*4;boolean familyPass=newWins+1e-9>=oldWins&&newPoints>=oldPoints-2*count;passed&=familyPass;
                family.put("beforePoints",oldPoints);family.put("afterPoints",newPoints);family.put("beforeWinShare",oldWins);family.put("afterWinShare",newWins);family.put("beforeExact",oldExact);family.put("afterExact",newExact);family.put("accepted",familyPass);
                roleOld+=oldPoints;roleNew+=newPoints;roleOldWins+=oldWins;roleNewWins+=newWins;
            }
            boolean gain=roleNewWins>roleOldWins+1e-9||roleNew>roleOld;receipt.put(role+"StrictGain",gain);passed&=gain;
        }
        receipt.put("status",passed?"PASS":"REGRESSION");receipt.put("pairsCompleted",pairs);write(Path.of(out,phase+"-results.json"),receipt);
        assertTrue("Candidate failed the prespecified comparison; retained all complete games and results",passed);
    }
    private static Map<String,String> sourceHashes()throws Exception{
        var result=new TreeMap<String,String>();Path project=Path.of(System.getProperty("user.dir")).getParent();
        for(String file:List.of("game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java","game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java","game-core/src/main/java/ro/whistlab/core/ai/engine/ContractEndgame.java","game-core/src/main/java/ro/whistlab/core/ai/forecast/ExactContractRates.java","game-core/src/main/java/ro/whistlab/core/ai/forecast/StreakForecast.java","ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java"))
            result.put(file,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(project.resolve(file)))));
        return result;
    }
    private static Result game(Path folder,String phase,String id,long seed,int hero,OfflineMatchHarness.Policy policy,OfflineMatchHarness.Policy opponent)throws Exception{
        Path target=folder.resolve(id+".json");JsonNode data;
        if(Files.exists(target)){data=JSON.readTree(Files.readString(target));assertEquals(id,data.path("id").asText());assertEquals(phase,data.path("partition").asText());assertEquals(24,data.path("hands").size());}
        else{var record=OfflineMatchHarness.play(id,seed,hero,policy,opponent);record.put("partition",phase);write(target,record);data=JSON.valueToTree(record);}
        int score=data.path("finalScores").get(hero).asInt(),best=Integer.MIN_VALUE,ties=0,exact=0;
        for(var value:data.path("finalScores"))best=Math.max(best,value.asInt());for(var value:data.path("finalScores"))if(value.asInt()==best)ties++;
        for(var hand:data.path("hands")){var s=hand.path("scores").get(hero);if(s.path("bid").asInt()==s.path("won").asInt())exact++;}
        return new Result(score,exact,score==best?1.0/ties:0);
    }
    private static String property(String name){String value=System.getProperty(name,"");assertFalse("Required "+name,value.isBlank());return value;}
    private static void write(Path target,Object value)throws Exception{
        Path tmp=Files.createTempFile(target.getParent(),"learning-",".tmp");Files.writeString(tmp,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value)+"\n");
        try{Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException unavailable){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
    }
}

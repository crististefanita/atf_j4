package ro.whistlab.core.learning.tournament;
import static ro.whistlab.core.testing.match.OfflineMatchHarness.Policy.*;





import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.testing.RoundFourGameOracle;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** One actual JUnit invocation per paired seed/seat/role/opponent; receipts persist before failure escapes. */
@RunWith(Parameterized.class)
public class RoundFourTournamentTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Parameterized.Parameters(name="role={0},opponent={1},seed={2},seat={3}") public static Collection<Object[]> cases(){
        boolean held=System.getProperty("whist.round4.phase","development").equals("heldout");int first=held?71000:4500,seeds=held?16:8;
        var cases=new ArrayList<Object[]>();String roleFilter=System.getProperty("whist.round4.role","");
        for(String role:List.of("expert","player"))if(roleFilter.isBlank()||roleFilter.equals(role))for(String opponent:List.of("EXPERT_V3","PLAYER_V3"))for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++)cases.add(new Object[]{role,opponent,seed,seat});return cases;
    }
    private final String role,opponent;private final int seed,seat;
    public RoundFourTournamentTest(String role,String opponent,int seed,int seat){this.role=role;this.opponent=opponent;this.seed=seed;this.seat=seat;}
    @Test public void completePairedGamesMatchTheIndependentRuleAndScoreOracle()throws Exception{
        String output=required("whist.round4.output"),corpus=required("whist.round4.corpus"),candidate=required("whist.round4.candidate");String phase=System.getProperty("whist.round4.phase","development");
        assertTrue(candidate.matches("[a-z0-9-]+"));assertTrue(phase.equals("development")||phase.equals("heldout"));Path out=Path.of(output);Files.createDirectories(out.resolve("invocations"));Files.createDirectories(out.resolve("partial-games"));var hashes=policyHashes();
        Path manifest=out.resolve("candidate-source.json");if(Files.exists(manifest))assertEquals("Source changed under an existing candidate ID",JSON.valueToTree(hashes),JSON.readTree(Files.readString(manifest)));else write(manifest,hashes);
        if(phase.equals("heldout")){var freeze=JSON.readTree(Files.readString(out.resolve("candidate-freeze.json")));
            assertEquals("FROZEN_BEFORE_HELDOUT",freeze.path("status").asText());assertEquals(candidate,freeze.path("candidate").asText());assertEquals(JSON.valueToTree(hashes),freeze.path("sourceHashes"));}
        String id=phase+"-"+role+"-"+opponent.toLowerCase(Locale.ROOT)+"-seed"+seed+"-seat"+seat;Path receiptFile=out.resolve("invocations/"+id+".json");assertFalse("Fresh invocation output required",Files.exists(receiptFile));
        var checkpoints=new ArrayList<RoundFourGameOracle.Checkpoint>();var results=new ArrayList<Map<String,Object>>();var receipt=new LinkedHashMap<String,Object>();String start=Instant.now().toString();long begin=System.nanoTime();
        receipt.put("schemaVersion",1);receipt.put("runId",candidate+"/"+phase);receipt.put("invocationId",id);receipt.put("className",getClass().getName());receipt.put("methodName","completePairedGamesMatchTheIndependentRuleAndScoreOracle");receipt.put("parameters",Map.of("role",role,"opponent",opponent,"seed",Integer.toString(seed),"heroSeat",seat));receipt.put("sourceHashes",hashes);receipt.put("startedAt",start);receipt.put("status","RUNNING");receipt.put("checkpoints",checkpoints);receipt.put("results",results);write(receiptFile,receipt);
        try{
            var rival=OfflineMatchHarness.Policy.valueOf(opponent);
            for(boolean after:List.of(false,true)){
                var hero=role.equals("expert")?(after?EXPERT_CANDIDATE:EXPERT_V3):(after?PLAYER_CANDIDATE:PLAYER_V3);
                String gameId="round4-"+candidate+"-"+id+(after?"-after":"-before");
                var record=OfflineMatchHarness.play(gameId,seed,seat,hero,rival,g->{try{write(out.resolve("partial-games/"+gameId+".json"),g);}catch(Exception error){throw new IllegalStateException("Cannot retain last safe hand",error);}});
                record.put("partition",phase);var source=(Map<String,Object>)record.get("source");source.put("invocationId",id);source.put("runId",candidate+"/"+phase);source.put("policyRevision",candidate);source.put("sourceHashes",hashes);
                var players=(List<Map<String,Object>>)record.get("players");for(var player:players){String label=(String)player.get("controller");if(label.contains("candidate-v4"))player.put("controller",label.replace("candidate-v4","round4-"+candidate));}
                Path target=Path.of(corpus,"games",phase,gameId+".json");Files.createDirectories(target.getParent());assertFalse("Closed games are immutable",Files.exists(target));write(target,record);
                var audit=RoundFourGameOracle.audit(record);checkpoints.addAll(audit);write(receiptFile,receipt);
                assertTrue("Independent rules comparison failed; receipt retained",audit.stream().flatMap(c->c.comparisons().stream()).allMatch(RoundFourGameOracle.Comparison::passed));
                var totals=(List<Integer>)record.get("finalScores");int best=totals.stream().mapToInt(Integer::intValue).max().orElseThrow();long ties=totals.stream().filter(t->t==best).count();int exact=0;for(var hand:(List<Map<String,Object>>)record.get("hands")){var score=((List<Map<String,Object>>)hand.get("scores")).get(seat);if(score.get("bid").equals(score.get("won")))exact++;}
                results.add(OfflineMatchHarness.map("kind",after?"after":"before","gameId",gameId,"points",totals.get(seat),"winShare",totals.get(seat)==best?1.0/ties:0.0,"exact",exact,"totals",totals));
            }receipt.put("status","PASS");
        }catch(AssertionError error){receipt.put("status","FAIL");receipt.put("failure",error.toString());throw error;}
        catch(Exception error){receipt.put("status","ERROR");receipt.put("failure",error.toString());throw error;}
        finally{receipt.put("endedAt",Instant.now().toString());receipt.put("durationNanos",System.nanoTime()-begin);write(receiptFile,receipt);}
    }
    static Map<String,String> policyHashes()throws Exception{
        var hashes=new TreeMap<String,String>();Path project=Path.of(System.getProperty("user.dir")).getParent();
        for(String file:List.of("game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java","game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java","game-core/src/main/java/ro/whistlab/core/ai/engine/ContractEndgame.java","game-core/src/main/java/ro/whistlab/core/ai/engine/OneCardBidBelief.java","ai-lab/src/main/java/ro/whistlab/coach/IndependentPlayer.java","ai-lab/src/main/java/ro/whistlab/coach/OneCardBidBelief.java"))hashes.put(file,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(project.resolve(file)))));return hashes;
    }
    private static String required(String key){String value=System.getProperty(key,"");assertFalse("Missing "+key,value.isBlank());return value;}
    static void write(Path file,Object data)throws Exception{Path temporary=Files.createTempFile(file.getParent(),"receipt-",".tmp");Files.writeString(temporary,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data)+"\n");try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException unavailable){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}}
}

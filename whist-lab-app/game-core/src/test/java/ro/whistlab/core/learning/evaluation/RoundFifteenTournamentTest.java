package ro.whistlab.core.learning.evaluation;
import static ro.whistlab.core.testing.match.OfflineMatchHarness.Policy.*;

import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.testing.RoundFourGameOracle;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** One actual JUnit invocation per paired seed/seat/role/opponent; receipts persist before failure escapes. */
@RunWith(Parameterized.class)
public class RoundFifteenTournamentTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Parameterized.Parameters(name="role={0},opponent={1},seed={2},seat={3}") public static Collection<Object[]> cases()throws Exception{
        playerBaseline();boolean held=System.getProperty("whist.round15.phase","development").equals("heldout");int first=number("whist.round15.firstSeed",held?103000:6700),seeds=number("whist.round15.seeds",held?12:6);if(first<0||seeds<1||seeds>1000||(long)first+seeds>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid declared seed range");
        var cases=new ArrayList<Object[]>();String roleFilter=System.getProperty("whist.round15.role",""),opponentFilter=System.getProperty("whist.round15.opponent","");int seatFilter=number("whist.round15.seat",-1);if(!roleFilter.isBlank()&&!List.of("expert","player").contains(roleFilter)||!opponentFilter.isBlank()&&!List.of("EXPERT_V14","PLAYER_V11").contains(opponentFilter)||seatFilter< -1||seatFilter>3)throw new IllegalArgumentException("Invalid declared case filter");
        for(String role:List.of("expert","player"))if(roleFilter.isBlank()||roleFilter.equals(role))for(String opponent:List.of("EXPERT_V14","PLAYER_V11"))if(opponentFilter.isBlank()||opponentFilter.equals(opponent))for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++)if(seatFilter<0||seat==seatFilter)cases.add(new Object[]{role,opponent,seed,seat});String caseFile=System.getProperty("whist.round15.casesFile","");if(!caseFile.isBlank()){var selection=JSON.readTree(Files.readString(Path.of(caseFile)));assertEquals("Selection source must match this unchanged policy",JSON.valueToTree(policyHashes()),selection.path("sourceHashes"));var keys=new HashSet<String>();for(var row:selection.path("cases"))assertTrue("Duplicate declared resume case",keys.add(row.path("role").asText()+"/"+row.path("opponent").asText()+"/"+row.path("seed").asInt()+"/"+row.path("seat").asInt()));var possible=new HashSet<String>();for(var row:cases)possible.add(row[0]+"/"+row[1]+"/"+row[2]+"/"+row[3]);assertTrue("Resume selection contains undeclared cases",possible.containsAll(keys));cases.removeIf(row->!keys.contains(row[0]+"/"+row[1]+"/"+row[2]+"/"+row[3]));assertFalse("Empty resume selection",cases.isEmpty());}return cases;
    }
    private final String role,opponent;private final int seed,seat;
    public RoundFifteenTournamentTest(String role,String opponent,int seed,int seat){this.role=role;this.opponent=opponent;this.seed=seed;this.seat=seat;}
    @Test public void completePairedGamesMatchTheIndependentRuleAndScoreOracle()throws Exception{
        String output=required("whist.round15.output"),corpus=required("whist.round15.corpus"),candidate=required("whist.round15.candidate");String phase=System.getProperty("whist.round15.phase","development");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "candidate", candidate, "corpus", corpus, "output", output, "phase", phase);
        assertTrue("candidate.matches(\"[a-z0-9-]+\") must hold", candidate.matches("[a-z0-9-]+"));assertTrue("phase.equals(\"development\") || phase.equals(\"heldout\") must hold", phase.equals("development")||phase.equals("heldout"));Path out=Path.of(output);Files.createDirectories(out.resolve("invocations"));Files.createDirectories(out.resolve("partial-games"));var hashes=policyHashes();
        Path manifest=out.resolve("candidate-source.json");if(Files.exists(manifest))assertEquals("Source changed under an existing candidate ID",JSON.valueToTree(hashes),JSON.readTree(Files.readString(manifest)));else write(manifest,hashes);
        if(phase.equals("heldout")){var freeze=JSON.readTree(Files.readString(out.resolve("candidate-freeze.json")));
            assertEquals("freeze.path(\"status\").asText() must equal \"FROZEN_BEFORE_HELDOUT\"", "FROZEN_BEFORE_HELDOUT",freeze.path("status").asText());assertEquals("freeze.path(\"candidate\").asText() must equal candidate", candidate,freeze.path("candidate").asText());assertEquals("freeze.path(\"sourceHashes\") must equal JSON.valueToTree(hashes)", JSON.valueToTree(hashes),freeze.path("sourceHashes"));Path use=Path.of(corpus,"holdout-use","romanian4-"+number("whist.round15.firstSeed",103000)+"-"+(number("whist.round15.firstSeed",103000)+number("whist.round15.seeds",12)-1)+".json");var reservation=JSON.readTree(Files.readString(use));assertEquals("Heldout range belongs to a different candidate",candidate,reservation.path("candidate").asText());assertEquals("Heldout source changed after reservation",JSON.valueToTree(hashes),reservation.path("sourceHashes"));}
        String id=phase+"-"+candidate+"-"+role+"-"+opponent.toLowerCase(Locale.ROOT)+"-seed"+seed+"-seat"+seat;Path receiptFile=out.resolve("invocations/"+id+".json");assertFalse("Fresh invocation output required",Files.exists(receiptFile));
        var checkpoints=new ArrayList<RoundFourGameOracle.Checkpoint>();var results=new ArrayList<Map<String,Object>>();var receipt=new LinkedHashMap<String,Object>();String start=Instant.now().toString();long begin=System.nanoTime();
        receipt.put("schemaVersion",1);receipt.put("runId",candidate+"/"+phase);receipt.put("invocationId",id);receipt.put("className",getClass().getName());receipt.put("methodName","completePairedGamesMatchTheIndependentRuleAndScoreOracle");receipt.put("parameters",Map.of("role",role,"opponent",opponent,"seed",Integer.toString(seed),"heroSeat",seat,"heroBaseline",role.equals("expert")?"EXPERT_V14":playerBaseline()));receipt.put("sourceHashes",hashes);String caseFile=System.getProperty("whist.round15.casesFile","");if(!caseFile.isBlank())receipt.put("caseSelectionSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(caseFile)))));receipt.put("startedAt",start);receipt.put("status","RUNNING");receipt.put("checkpoints",checkpoints);receipt.put("results",results);write(receiptFile,receipt);
        try{
            var rival=OfflineMatchHarness.Policy.valueOf(opponent);
            for(boolean after:List.of(false,true)){
                var hero=role.equals("expert")?(after?EXPERT_CANDIDATE:EXPERT_V14):(after?PLAYER_CANDIDATE:OfflineMatchHarness.Policy.valueOf(playerBaseline()));
                String gameId="round15-"+candidate+"-"+id+(after?"-after":"-before");
                var record=OfflineMatchHarness.play(gameId,seed,seat,hero,rival,g->{try{write(out.resolve("partial-games/"+gameId+".json"),g);}catch(Exception error){throw new IllegalStateException("Cannot retain last safe hand",error);}});
                record.put("partition",phase);var source=(Map<String,Object>)record.get("source");source.put("invocationId",id);source.put("runId",candidate+"/"+phase);source.put("policyRevision",candidate);source.put("sourceHashes",hashes);
                var players=(List<Map<String,Object>>)record.get("players");for(var player:players){String label=(String)player.get("controller");if(label.contains("candidate-v5"))player.put("controller",label.replace("candidate-v5","round15-"+candidate));}
                Path target=Path.of(corpus,"games",phase,gameId+".json");Files.createDirectories(target.getParent());assertFalse("Closed games are immutable",Files.exists(target));write(target,record);
                var audit=RoundFourGameOracle.audit(record);checkpoints.addAll(audit);write(receiptFile,receipt);
                assertTrue("Independent rules comparison failed; receipt retained",audit.stream().flatMap(c->c.comparisons().stream()).allMatch(RoundFourGameOracle.Comparison::passed));
                var totals=(List<Integer>)record.get("finalScores");int best=totals.stream().mapToInt(Integer::intValue).max().orElseThrow();long ties=totals.stream().filter(t->t==best).count();int exact=0;for(var hand:(List<Map<String,Object>>)record.get("hands")){var score=((List<Map<String,Object>>)hand.get("scores")).get(seat);if(score.get("bid").equals(score.get("won")))exact++;}
                var bySize=new TreeMap<String,Map<String,Integer>>();for(var hand:(List<Map<String,Object>>)record.get("hands")){String size=hand.get("size").toString();var score=((List<Map<String,Object>>)hand.get("scores")).get(seat);var bucket=bySize.computeIfAbsent(size,k->new TreeMap<>(Map.of("hands",0,"points",0,"exact",0,"absoluteBidError",0)));bucket.merge("hands",1,Integer::sum);bucket.merge("points",((Number)score.get("base")).intValue()+((Number)score.get("series")).intValue(),Integer::sum);bucket.merge("exact",score.get("bid").equals(score.get("won"))?1:0,Integer::sum);bucket.merge("absoluteBidError",Math.abs(((Number)score.get("bid")).intValue()-((Number)score.get("won")).intValue()),Integer::sum);}results.add(OfflineMatchHarness.map("kind",after?"after":"before","gameId",gameId,"points",totals.get(seat),"winShare",totals.get(seat)==best?1.0/ties:0.0,"exact",exact,"totals",totals,"byHandSize",bySize));
            }receipt.put("status","PASS");
        }catch(AssertionError error){receipt.put("status","FAIL");receipt.put("failure",error.toString());throw error;}
        catch(Exception error){receipt.put("status","ERROR");receipt.put("failure",error.toString());throw error;}
        finally{receipt.put("endedAt",Instant.now().toString());receipt.put("durationNanos",System.nanoTime()-begin);write(receiptFile,receipt);}
    }
    static Map<String,String> policyHashes()throws Exception{
        var hashes=new TreeMap<String,String>();Path project=Path.of(System.getProperty("user.dir")).getParent();
        for(String tree:List.of("game-core/src/main/java","ai-lab/src/main/java","game-core/src/test/java/ro/whistlab/core/baseline","game-core/src/test/java/ro/whistlab/coach/baseline")){
            try(var files=Files.walk(project.resolve(tree))){for(Path file:files.filter(p->p.toString().endsWith(".java")).sorted().toList())hashes.put(project.relativize(file).toString().replace('\\','/'),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));}
        }
        for(String file:List.of("game-core/src/test/java/ro/whistlab/core/learning/mining/OfflineMatchHarness.java","game-core/src/test/java/ro/whistlab/core/testing/RoundFourGameOracle.java","game-core/src/test/java/ro/whistlab/core/learning/evaluation/RoundFifteenTournamentTest.java"))hashes.put(file,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(project.resolve(file)))));return hashes;
    }
    static String playerBaseline(){return playerBaseline(System.getProperty("whist.round15.playerBaseline",""));}
    static String playerBaseline(String configured){if(configured.isBlank())return "PLAYER_V11";if(!configured.equals("PLAYER_V11"))throw new IllegalArgumentException("Round11 requires the accepted IndependentV11 baseline");return configured;}
    private static int number(String key,int fallback){String raw=System.getProperty(key,"");return raw.isBlank()?fallback:Integer.parseInt(raw);}
    private static String required(String key){String value=System.getProperty(key,"");assertFalse("Missing "+key,value.isBlank());return value;}
    static void write(Path file,Object data)throws Exception{
        Path temporary=Files.createTempFile(file.getParent(),"receipt-",".tmp");
        Files.writeString(temporary,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data)+"\n");
        boolean durable=true;if(data instanceof Map<?,?> map){if("RUNNING".equals(map.get("status")))durable=false;if(map.get("source") instanceof Map<?,?> source&&Boolean.FALSE.equals(source.get("complete")))durable=false;}
        if(durable)try(var channel=java.nio.channels.FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
        for(int attempt=0;;attempt++){
            try{try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}return;}
            catch(AccessDeniedException transientWindowsSharing){
                if(attempt>=7)throw transientWindowsSharing;
                // A live read or antivirus may briefly retain a Windows handle. Never drop a receipt or ignore an exhausted write.
                Thread.sleep(25L*(attempt+1));
            }
        }
    }
}

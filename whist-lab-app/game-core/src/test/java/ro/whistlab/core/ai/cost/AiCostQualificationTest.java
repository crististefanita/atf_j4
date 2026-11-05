package ro.whistlab.core.ai.cost;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.PlannerControl;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.testing.match.PlayerObservations;

/** Domain integration of two public decision APIs; timing excludes construction, warmup and publication. */
@RunWith(Parameterized.class)
public class AiCostQualificationTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Parameterized.Parameters(name="handSize={0}") public static Collection<Object[]> sizes() {
        return Arrays.asList(new Object[][]{{2},{3},{4},{5},{6},{7},{8}});
    }
    private final int size;
    public AiCostQualificationTest(int size) { this.size=size; }

    @Test(timeout=120000) public void generatedContextsStayLegalDeterministicAndWithinTheDeclaredJvmProfile() throws Exception {
        Path profile=Path.of(required("whist.aiCost.profile")),output=Path.of(required("whist.aiCost.output"));
        byte[] profileBytes=Files.readAllBytes(profile);
        JsonNode config=JSON.readTree(profileBytes);
        validateProfile(config);
        ThreadMXBean cpu=ManagementFactory.getThreadMXBean();assertTrue("Thread CPU clock required",cpu.isCurrentThreadCpuTimeSupported());
        if(!cpu.isThreadCpuTimeEnabled())cpu.setThreadCpuTimeEnabled(true);
        List<Map<String,Object>> rows=new ArrayList<>();
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("scope",config.path("scope").asText());report.put("profile",config);report.put("handSize",size);
        report.put("profileSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(profileBytes)));
        report.put("jvmArguments",ManagementFactory.getRuntimeMXBean().getInputArguments());
        report.put("environment",Map.of("java",System.getProperty("java.runtime.version"),"vm",System.getProperty("java.vm.name"),
                "os",System.getProperty("os.name"),"osVersion",System.getProperty("os.version"),"arch",System.getProperty("os.arch"),
                "processors",Runtime.getRuntime().availableProcessors(),"maximumHeapBytes",Runtime.getRuntime().maxMemory()));
        report.put("rows",rows);report.put("status","RUNNING");
        try {
            for(JsonNode seed:config.path("seeds"))for(var stage:CostPositions.Stage.values()) {
                if(size==2&&stage==CostPositions.Stage.CARD_LATE)continue;
                var scenario=CostPositions.create(size,seed.asInt(),stage);
                for(String policy:List.of("NORMAL","HARD","INDEPENDENT")) {
                    Object expected=null;
                    for(int i=0;i<config.path("warmups").asInt();i++)expected=decide(scenario,policy);
                    assertNotNull(expected);
                    for(int repeat=0;repeat<config.path("repetitions").asInt();repeat++) {
                        Map<String,Object> row=new LinkedHashMap<>();rows.add(row);
                        row.put("stage",stage);row.put("policy",policy);row.put("fixtureSeed",seed.asInt());row.put("repeat",repeat);
                        row.put("publicObservation",scenario.observation());row.put("status","RUNNING");
                        long limit=config.path("maximumWorkUnits").asLong(),beginCpu=cpu.getCurrentThreadCpuTime(),begin=System.nanoTime();
                        try {
                            Object decision=measuredDecision(scenario,policy,limit,row);
                            long wall=System.nanoTime()-begin,threadCpu=cpu.getCurrentThreadCpuTime()-beginCpu;
                            row.put("wallNanos",wall);row.put("threadCpuNanos",threadCpu);
                            row.put("decision",decision);
                            long units=(Long)row.get("workUnits");
                            assertEquals(expected,decision);assertEquals("NONE",row.get("controlStopReason"));assertTrue(units>0&&units<=limit);
                            assertTrue("CPU clock must provide a nonnegative duration",threadCpu>=0);
                            assertTrue("CPU cost exceeds declared profile: "+row,threadCpu<=config.path("maximumThreadCpuNanos").asLong());
                            assertTrue("Wall safeguard exceeded",wall<=config.path("maximumWallNanos").asLong());
                            verifyBounds(config,scenario,policy,events(row));verifyLegal(scenario,decision);
                            row.put("status","PASS");
                        } catch(Throwable failure) {row.put("status","FAIL");row.put("failure",failure.toString());throw failure;}
                        finally {row.putIfAbsent("wallNanos",System.nanoTime()-begin);row.putIfAbsent("threadCpuNanos",cpu.getCurrentThreadCpuTime()-beginCpu);}
                    }
                }
            }
            report.put("status","PASS");
        } finally {
            if(report.get("status").equals("RUNNING"))report.put("status","FAIL");
            Files.createDirectories(output);
            try(var file=Files.newOutputStream(output.resolve("cost-hand-"+size+".json"),java.nio.file.StandardOpenOption.CREATE_NEW)) {
                JSON.writerWithDefaultPrettyPrinter().writeValue(file,report);
            }
        }
    }
    @SuppressWarnings("unchecked") private static Map<String,Long> events(Map<String,Object> row) {
        return (Map<String,Long>)row.get("events");
    }
    private static Object measuredDecision(CostPositions.Scenario s,String policy,long limit,Map<String,Object> row) {
        Map<String,Long> events=new LinkedHashMap<>();row.put("events",events);
        if(policy.equals("INDEPENDENT")) {
            try(var control=PlannerControl.measure(limit)) {
                try {return decide(s,policy);}
                finally {var m=control.snapshot();row.put("workUnits",m.workUnits());row.put("controlStopReason",m.stopReason());m.events().forEach((k,v)->events.put(k.name(),v));}
            }
        }
        try(var control=SearchWork.measure(limit)) {
            try {return decide(s,policy);}
            finally {var m=control.snapshot();row.put("workUnits",m.workUnits());row.put("controlStopReason",m.stopReason());m.events().forEach((k,v)->events.put(k.name(),v));}
        }
    }
    private static void validateProfile(JsonNode config) {
        assertEquals(1,config.path("schemaVersion").asInt());assertEquals(4,config.path("players").asInt());
        assertEquals(JSON.valueToTree(List.of(2,3,4,5,6,7,8)),config.path("handSizes"));
        assertFalse(config.path("scope").asText().isBlank());
        var seeds=config.path("seeds");assertTrue(seeds.isArray()&&!seeds.isEmpty());
        var unique=new java.util.HashSet<Long>();
        for(var seed:seeds){assertTrue(seed.isIntegralNumber()&&seed.canConvertToInt()&&seed.asInt()>=0);assertTrue(unique.add(seed.asLong()));}
        for(String key:List.of("warmups","repetitions","maximumWorkUnits","maximumThreadCpuNanos","maximumWallNanos",
                "coreBidSamples","coreCardSamples","coreEnumeratedWorlds","independentBidSamples","independentCardSamples",
                "coreProofNodes","independentProofNodes")) {
            assertTrue("Positive integer profile field required: "+key,config.path(key).isIntegralNumber()&&config.path(key).asLong()>0);
        }
    }
    private static Object decide(CostPositions.Scenario s,String policy) {
        if(policy.equals("INDEPENDENT")) {
            var p=PlayerObservations.independent(s.observation());
            return s.bidding()?IndependentPlayer.bid(p).value():IndependentPlayer.card(p).value();
        }
        var difficulty=AiDifficulty.valueOf(policy);
        if(s.bidding())return AiPolicy.chooseBid(s.snapshot(),s.actor(),difficulty);
        var card=AiPolicy.chooseCard(s.snapshot(),s.actor(),difficulty);
        return card.suit().ordinal()*13+card.rank()-2;
    }
    private static void verifyLegal(CostPositions.Scenario s,Object selected) {
        int value=(Integer)selected;
        if(s.bidding()) {
            assertTrue(value>=0&&value<=s.snapshot().handSize());
            int count=0,sum=0;for(var p:s.snapshot().players())if(p.bid()!=null){count++;sum+=p.bid();}
            if(count==3)assertNotEquals(s.snapshot().handSize(),sum+value);
        } else {
            var v=s.observation();var legal=GameRules.legalCards(v.hand(),v.trick().isEmpty()?null:v.trick().get(0).card().suit(),v.trump(),v.mode());
            assertTrue(legal.stream().anyMatch(c->c.suit().ordinal()*13+c.rank()-2==value));
        }
    }
    private static void verifyBounds(JsonNode config,CostPositions.Scenario s,String policy,Map<String,Long> events) {
        boolean independent=policy.equals("INDEPENDENT");
        long samples=events.getOrDefault("SAMPLE",0L);
        long worlds=events.getOrDefault("WORLD",0L);
        String sampleKey=(independent?"independent":"core")+(s.bidding()?"BidSamples":"CardSamples");
        assertTrue(samples<=config.path(sampleKey).asLong());
        assertTrue(worlds<=(independent||s.bidding()?samples:Math.max(samples,config.path("coreEnumeratedWorlds").asLong())));
        assertTrue(events.getOrDefault("PROOF",0L)<=config.path(independent?"independentProofNodes":"coreProofNodes").asLong());
        var v=s.observation();int cards=v.seats().stream().mapToInt(p->p.remaining()).sum();
        int choices=s.bidding()?v.handSize()+1:v.hand().size();
        assertTrue(events.getOrDefault("ROLLOUT",0L)<=worlds*choices*cards);
        long leaves=1;boolean small=true;
        for(var seat:v.seats()) {small&=seat.remaining()<=3;for(int i=2;i<=seat.remaining();i++)leaves*=i;}
        long endgameBound=independent||!small?0:worlds*choices*(cards+1)*leaves;
        assertTrue(events.getOrDefault("ENDGAME",0L)<=endgameBound);
        if(independent)assertTrue(events.getOrDefault("REJECTION",0L)<=64*samples);
        else {
            long pool=ro.whistlab.core.ai.simulation.PossibleDeals.unknownCards(v).size();
            assertTrue(events.getOrDefault("MATCHING",0L)<=samples*(cards-v.hand().size())*(pool+1));
        }
    }
    private static String required(String key) {String value=System.getProperty(key,"");if(value.isBlank())throw new IllegalArgumentException("Explicit "+key+" required");return value;}
}

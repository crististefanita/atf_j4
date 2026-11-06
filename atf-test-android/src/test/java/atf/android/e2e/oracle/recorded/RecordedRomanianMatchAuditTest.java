package atf.android.e2e.oracle.recorded;

import atf.testkit.assertions.AuditedAssertions;

import atf.android.e2e.oracle.romanian.RomanianMatchOracle;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.AtfIntegrationTest;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Re-audits the retained real Android evidence without ADB or game-core. */
@AtfIntegrationTest
class RecordedRomanianMatchAuditTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void allRetainedNormalExpertCardsAndVisibleScoreRowsRemainReconstructible()throws Exception{
        assumeTrue(Boolean.getBoolean("atf.romanian.audit"));
        Path out=Path.of(System.getProperty("atf.match.evidenceDir"));int[] totals=new int[4],streaks=new int[4];int moves=0,snapshots=0;
        JsonNode first=read(out,"ro-h01-deal");int dealer=first.path("dealerSeat").asInt();long seed=first.path("seed").asLong();
        for(int h=0;h<24;h++){
            String prefix=String.format("ro-h%02d",h+1);JsonNode start=read(out,prefix+"-deal");
            var oracle=new RomanianMatchOracle(start,h,(dealer+h)%4,totals,first.path("difficulty").asText("EXPERT"),streaks,first.path("streakBonuses").asBoolean(),first.path("streakBonusPoints").asInt(5),first.path("streakPenaltyPoints").asInt(5));pass(oracle.start(start));snapshots++;
            AuditedAssertions.assertEquals(seed,start.path("seed").asLong());pass(oracle.verify(read(out,prefix+"-bids")));snapshots++;
            int size=RomanianMatchOracle.schedule(4).get(h);
            for(int t=1;t<=size;t++){
                String label=prefix+String.format("-turn%02d",t);
                pass(oracle.verify(read(out,label+"-legal")));pass(oracle.verify(read(out,label)));snapshots+=2;
            }
            JsonNode score=read(out,prefix+"-score");pass(oracle.verify(score));snapshots++;totals=oracle.totals();streaks=oracle.streaks();moves+=oracle.moves();
            var f=javax.xml.parsers.DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            var nodes=f.newDocumentBuilder().parse(out.resolve(prefix+"-score.xml").toFile()).getElementsByTagName("node");
            Set<String> visible=new HashSet<>();for(int i=0;i<nodes.getLength();i++)visible.add(((org.w3c.dom.Element)nodes.item(i)).getAttribute("text"));
            for(int seat=0;seat<4;seat++){
                JsonNode p=score.path("players").get(seat);int bid=p.path("bid").asInt(),won=p.path("tricksWon").asInt(),extra=oracle.seriesBonus(seat),delta=(won==bid?5+won:-Math.abs(won-bid))+extra;
                AuditedAssertions.assertTrue(visible.contains(p.path("name").asText()+" | Bid "+bid+" | Won "+won+" | "+(delta>=0?"+":"")+delta+(extra==0?"":" (streak "+(extra>0?"+":"")+extra+")")+" | Total: "+totals[seat]));
            }
        }
        AuditedAssertions.assertEquals(376,moves);JsonNode resumed=read(out,"ro-process-resume");int hand=resumed.path("handIndex").asInt();
        boolean sameCheckpoint=false;String prefix=String.format("ro-h%02d",hand+1);
        for(int t=1;t<=RomanianMatchOracle.schedule(4).get(hand);t++)if(read(out,prefix+String.format("-turn%02d",t)).equals(resumed))sameCheckpoint=true;
        AuditedAssertions.assertTrue(sameCheckpoint,"Process-death resume must match an actual retained human boundary exactly");
        JsonNode complete=read(out,"ro-complete");AuditedAssertions.assertEquals("SCORE",complete.path("phase").asText());AuditedAssertions.assertEquals(23,complete.path("handIndex").asInt());
        for(int seat=0;seat<4;seat++)AuditedAssertions.assertEquals(totals[seat],complete.path("players").get(seat).path("totalScore").asInt());
        Properties result=new Properties();result.setProperty("status","PASS");result.setProperty("hands","24");result.setProperty("moves",""+moves);result.setProperty("snapshots",""+snapshots);result.setProperty("finalTotals",Arrays.toString(totals));result.setProperty("processResume","exact retained checkpoint");
        try(var stream=Files.newOutputStream(out.resolve("offline-audit-result.properties"))){result.store(stream,"Reconstruction from retained real Android evidence; no emulator or engine");}
    }
    private static JsonNode read(Path out,String label)throws Exception{return JSON.readTree(Files.readString(out.resolve(label+"-lab-state.json")));}
    private static void pass(List<AtfAssertion> a){AuditedAssertions.assertTrue(a.stream().allMatch(AtfAssertion::passed),()->a.stream().filter(v->!v.passed()).toList().toString());}
}

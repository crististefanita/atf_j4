package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

/** One normal mobile hand validates the Romanian/Expert integration after leaving debug replay. */
@AndroidE2eTest
class WhistLabRomanianExpertE2eTest {
    @Test void leavingReplayStartsANormalRomanianExpertGameWithCorrectScoring()throws Exception {
        Path out=Path.of(System.getProperty("atf.match.evidenceDir")).toAbsolutePath();
        AdbClient adb=AdbClient.fromSystemProperties();AndroidUiDriver ui=new AndroidUiDriver(adb,out);
        AndroidUiFlowTrace trace=new AndroidUiFlowTrace(out,"romanian-expert-smoke",
                "Whist Lab — leave scripted replay and play a normal Romanian hand with Expert AI",
                getClass().getSimpleName(),"atf.android.e2e","ro.whistlab.app",List.of("normal-game","romanian-whist","expert-ai","mobile"));
        try {
            adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();
            adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity","--ez","debug_replay","true").requireSuccess();
            ui.awaitText("Place your bid",Duration.ofSeconds(20));ui.tapText("Main menu");ui.tapText("New game");
            ui.selectSpinnerOption(1,"Romanian (1–8–1)");ui.selectSpinnerOption(2,"Expert (simulation)");
            ui.capture("romanian-expert-setup");
            String setup=ui.dump();
            WhistTestEvidence.checkpoint(trace,out,"SETUP","Leave recorded input mode and select a normal four-seat Romanian Expert game",
                    "romanian-expert-setup","4 players / Romanian Whist / Expert",List.of(
                            new AtfAssertion("Explicit normal game profile","Romanian (1–8–1) / Expert (simulation)",
                                    ui.texts(setup).toString(),ui.hasText(setup,"Romanian (1–8–1)")&&ui.hasText(setup,"Expert (simulation)"))));
            ui.tapText("Start game");ui.awaitText("Place your bid",Duration.ofSeconds(25));
            JsonNode bidding=saved(adb,out,"romanian-expert-bidding");
            List<AtfAssertion> checks=new ArrayList<>();
            check(checks,"Normal mode replaces the recorded Oh Hell fixture","ROMANIAN_WHIST",bidding.path("mode").asText());
            check(checks,"Expert policy is active","EXPERT",bidding.path("difficulty").asText());
            check(checks,"Initial Romanian hand size","1",bidding.path("handSize").asText());
            check(checks,"Table size","4",bidding.path("playerCount").asText());
            for(JsonNode player:bidding.path("players")) {
                assertEquals(1,player.path("hand").size());assertTrue(player.path("hand").get(0).path("rank").asInt()>=7);
                assertEquals(0,player.path("totalScore").asInt());
            }
            ui.capture("romanian-expert-bidding");
            WhistTestEvidence.checkpoint(trace,out,"TEST BODY","Normal Romanian deal and Expert bids use the real game engine",
                    "romanian-expert-bidding",bidding.toString(),checks);
            ui.tapSpinner(0);String options=ui.dump();
            int bid=ui.texts(options).stream().filter(t->t.matches("[01]")).mapToInt(Integer::parseInt).min().orElseThrow();
            ui.back();ui.selectBid(bid);ui.tapText("Place bid");
            ui.awaitText("Your turn",Duration.ofSeconds(25));
            JsonNode playing=saved(adb,out,"romanian-expert-human-turn");
            JsonNode card=playing.path("players").get(0).path("hand").get(0);
            ui.tapText(OriginalWhistState.card(id(card)));ui.awaitText("Hand complete",Duration.ofSeconds(25));
            JsonNode score=saved(adb,out,"romanian-expert-score");String scoreXml=ui.dump();checks=new ArrayList<>();
            check(checks,"Normal hand reached SCORE","SCORE",score.path("phase").asText());
            List<JsonNode> history=new ArrayList<>();score.path("history").forEach(history::add);assertEquals(4,history.size());
            int trump=List.of("SPADES","HEARTS","CLUBS","DIAMONDS","NO_TRUMP").indexOf(score.path("trump").asText());
            int lead=id(history.get(0))/13,best=0;
            for(int i=1;i<4;i++)if(OriginalMatchOracle.strength(id(history.get(i)),lead,trump)>
                    OriginalMatchOracle.strength(id(history.get(best)),lead,trump))best=i;
            int winner=history.get(best).path("seat").asInt();
            check(checks,"Actual trick winner matches the independent card comparator",Integer.toString(winner),score.path("lastTrickWinnerSeat").asText());
            int totalWon=0;
            for(int seat=0;seat<4;seat++) {
                JsonNode player=score.path("players").get(seat);int won=player.path("tricksWon").asInt(),called=player.path("bid").asInt();
                assertEquals(seat==winner?1:0,won);totalWon+=won;
                int delta=won==called?5+won:-Math.abs(won-called);
                check(checks,"Seat "+seat+" Romanian exact/penalty scoring",Integer.toString(delta),player.path("totalScore").asText());
                String row=player.path("name").asText()+" | Bid "+called+" | Won "+won+" | "+(delta>=0?"+":"")+delta+" | Total: "+delta;
                checks.add(new AtfAssertion("Seat "+seat+" displayed Romanian score",row,"exact row visible="+ui.hasText(scoreXml,row),ui.hasText(scoreXml,row)));
            }
            assertEquals(1,totalWon);ui.capture("romanian-expert-score");
            WhistTestEvidence.checkpoint(trace,out,"TEST BODY","Normal Expert opponents finish the hand and Romanian scores match the rules",
                    "romanian-expert-score",score.toString(),checks);
            ui.tapText("Next hand");ui.awaitText("Place your bid",Duration.ofSeconds(25));
            JsonNode next=saved(adb,out,"romanian-expert-ready");checks=new ArrayList<>();
            check(checks,"Next scheduled hand starts","1",next.path("handIndex").asText());
            check(checks,"Dealer rotates",Integer.toString((score.path("dealerSeat").asInt()+1)%4),next.path("dealerSeat").asText());
            for(int seat=0;seat<4;seat++)check(checks,"Seat "+seat+" cumulative score survives the transition",
                    score.path("players").get(seat).path("totalScore").asText(),next.path("players").get(seat).path("totalScore").asText());
            ui.capture("romanian-expert-ready");
            WhistTestEvidence.checkpoint(trace,out,"TEARDOWN","Leave a normal Romanian Expert game ready for local play",
                    "romanian-expert-ready",next.toString(),checks);
            trace.finish(AtfReportStatus.PASS);
        }catch(Throwable failure){trace.fail(failure);throw failure;}
    }
    private static void check(List<AtfAssertion> checks,String name,String expected,String actual) {
        checks.add(new AtfAssertion(name,expected,actual,expected.equals(actual)));
    }
    private static int id(JsonNode card) {
        return List.of("SPADES","HEARTS","CLUBS","DIAMONDS").indexOf(card.path("suit").asText())*13+card.path("rank").asInt()-2;
    }
    private static JsonNode saved(AdbClient adb,Path out,String label)throws Exception {
        String xml=adb.run("exec-out","run-as","ro.whistlab.app","cat","shared_prefs/whist_lab_game.xml").output();
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var values=factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml))).getElementsByTagName("string");
        for(int i=0;i<values.getLength();i++) {
            var value=(org.w3c.dom.Element)values.item(i);
            if(value.getAttribute("name").equals("current_session_v1")) {
                JsonNode state=new ObjectMapper().readTree(value.getTextContent());
                Files.writeString(out.resolve(label+"-lab-state.json"),state.toPrettyString());return state;
            }
        }
        throw new IllegalStateException("Own app's game checkpoint is missing");
    }
}

package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.*;
import atf.reporting.ui.model.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Normal random deal and normal Expert opponents; no replay inputs or expected outcomes injected. */
@AndroidE2eTest
class CompleteRomanianExpertE2eTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void normalAndroidGameMatchesRomanianRulesAgainstTheDeclaredHumanPolicy()throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("atf.android.fullMatch"));
        boolean challenge=Boolean.getBoolean("atf.android.aiChallenge");
        String level=System.getProperty("atf.android.aiDifficulty","EXPERT");
        int seriesReward=Integer.getInteger("atf.android.streakBonus",10);assertTrue(List.of(0,5,10).contains(seriesReward));
        int limit=Integer.getInteger("atf.android.handLimit",24);assertTrue(limit>=1&&limit<=24);
        Path out=Path.of(System.getProperty("atf.match.evidenceDir")).toAbsolutePath();Files.createDirectories(out);
        AdbClient adb=AdbClient.fromSystemProperties();AndroidUiDriver ui=new AndroidUiDriver(adb,out);
        AndroidUiFlowTrace trace=new AndroidUiFlowTrace(out,"romanian-expert-complete",
                "Whist romanesc — "+limit+" normal Android hands with "+level+", every move and score audited",
                getClass().getSimpleName(),"atf.android.e2e","ro.whistlab.app",List.of("android","romanian-whist",level.toLowerCase(),limit==24?"full-match":"pilot","rules","score"));
        Properties result=new Properties(),coverage=new Properties();int[] totals=new int[4],streaks=new int[4];int moves=0,tricks=0,humanActions=0;boolean resumed=false,backReviewed=false;
        try {
            result.setProperty("scope",limit==24?"COMPLETE":"PILOT");result.setProperty("difficulty",level);result.setProperty("humanPolicy",challenge?"Independent public-information planner; no opponents' cards or shuffle seed":"Own high-card bids and varied legal choices");
            result.setProperty("streakBonus",""+seriesReward);
            adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();
            adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity").requireSuccess();
            ui.tapText("New game");ui.selectNamedSpinnerOption("game-type","Whist romanesc (1-8-1)");ui.selectNamedSpinnerOption("ai-level",level.equals("EXPERT")?"Expert (simulation)":level.equals("HARD")?"Hard":"Normal");
            if(seriesReward==0)ui.tapText("Romanian five-hand streaks (missed series: -5)");
            else ui.selectNamedSpinnerOption("romanian-streak-value",seriesReward==5?"5 points (original)":"10 points");
            ui.capture("ro-setup");String setup=ui.dump();var checks=new ArrayList<AtfAssertion>();
            // Semantic profile is also asserted from the fresh checkpoint after the normal menu action.
            RomanianMatchOracle.truth(checks,"Selected series value in setup",seriesReward==0?"series disabled":seriesReward==5?"5 points (original)":"10 points",ui.texts(setup).toString(),seriesReward==0||ui.hasText(setup,seriesReward==5?"5 points (original)":"10 points"));
            checkpoint(trace,out,"SETUP","Select normal four-seat Romanian Whist and "+level,"ro-setup","No debug replay, seed or deal is supplied",checks,List.of());
            ui.revealText("Start game");ui.tapText("Start game");JsonNode start=stable(adb);long seed=start.path("seed").asLong();int initialDealer=start.path("dealerSeat").asInt();
            result.setProperty("seed",Long.toString(seed));result.setProperty("initialDealer",Integer.toString(initialDealer));
            var schedule=RomanianMatchOracle.schedule(4);
            for(int h=0;h<limit;h++){
                String prefix=String.format("ro-h%02d",h+1);if(h>0)start=stable(adb);
                RomanianMatchOracle oracle=new RomanianMatchOracle(start,h,(initialDealer+h)%4,totals,level,streaks,seriesReward!=0,seriesReward==0?5:seriesReward);
                checks=new ArrayList<>(oracle.start(start));RomanianMatchOracle.equal(checks,"Same normal match seed",seed,start.path("seed").asLong());
                save(out,prefix+"-deal",start);ui.capture(prefix+"-deal");
                checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+" / 24: deal, trump, bid order and prior totals",prefix+"-deal",start.toPrettyString(),checks,List.of());
                int size=schedule.get(h);String optionsBefore=ui.dump();ui.tapSpinner(0);String optionXml=ui.dump();
                int sum=start.path("bidTotal").asInt();boolean last=start.path("bidsSubmitted").asInt()==3;
                var expected=new ArrayList<Integer>();for(int b=0;b<=size;b++)if(!last||b+sum!=size)expected.add(b);
                List<Integer> visible=ui.texts(optionXml).stream().filter(t->t.matches("[0-8]")).map(Integer::valueOf).toList();
                checks=new ArrayList<>();RomanianMatchOracle.equal(checks,"RW03 exact human bid options",expected.toString(),visible.toString());
                ui.capture(prefix+"-bid-options");checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+": allowed bid domain and last-bid exclusion",prefix+"-bid-options",optionXml,checks,List.of());
                int preferred=(int)RomanianMatchOracle.cards(start.path("players").get(0).path("hand")).stream().filter(c->c%13>=11).count();
                var bidAdvice=challenge?ro.whistlab.coach.IndependentPlayer.bid(WhistChallenger.position(start,0)):null;
                int bid=challenge?bidAdvice.value():expected.stream().min(Comparator.comparingInt(b->Math.abs(b-preferred))).orElseThrow();
                assertTrue(expected.contains(bid),"Independent planner must choose from the displayed allowed bid domain");
                ui.back();ui.selectBid(bid);ui.tapText("Place bid");JsonNode state=stable(adb);
                checks=new ArrayList<>(oracle.verify(state));RomanianMatchOracle.equal(checks,"Selected human bid accepted",bid,state.path("players").get(0).path("bid").asInt());
                String label=prefix+"-bids";save(out,label,state);ui.capture(label);
                checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+": all AI bids and opening moves",label,state.toPrettyString()+"\n"+oracle.explanation()+(challenge?"\nRedacted human input: "+WhistChallenger.position(start,0)+"\nHuman decision: "+bidAdvice.explanation():""),checks,
                        List.of(action(trace,"Submit human bid","Bid "+bid+" / Place bid","bid="+bid,"actual="+state.path("players").get(0).path("bid"))));
                int turn=0;
                while(!state.path("phase").asText().equals("SCORE")){
                    label=prefix+String.format("-turn%02d",++turn);String xml=ui.dump();checks=new ArrayList<>();
                    var legal=oracle.humanLegal();
                    for(int c:RomanianMatchOracle.cards(state.path("players").get(0).path("hand")))
                        RomanianMatchOracle.equal(checks,"RW04 UI enabled "+RomanianMatchOracle.label(c),legal.contains(c),ui.isEnabled(xml,RomanianMatchOracle.label(c)));
                    var cardAdvice=challenge?ro.whistlab.coach.IndependentPlayer.card(WhistChallenger.position(state,0)):null;
                    int card=challenge?cardAdvice.value():legal.get(Math.floorMod(17*h+turn,legal.size()));int before=oracle.moves();
                    RomanianMatchOracle.truth(checks,"Declared human policy chooses a legal card",legal.toString(),""+card,legal.contains(card));
                    ui.capture(label+"-legal");save(out,label+"-legal",state);
                    checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+" human turn "+turn+": exact follow-suit/trump enabled domain",label+"-legal",state.toPrettyString(),checks,List.of());
                    ui.tapTextIn(xml,RomanianMatchOracle.label(card));JsonNode next=stable(adb);
                    checks=new ArrayList<>(oracle.verify(next));boolean accepted=false;
                    for(int i=before;i<next.path("history").size();i++){JsonNode p=next.path("history").get(i);if(p.path("seat").asInt()==0&&RomanianMatchOracle.card(p)==card)accepted=true;}
                    RomanianMatchOracle.truth(checks,"Exact human card accepted","seat 0 played "+RomanianMatchOracle.label(card),"accepted="+accepted,accepted);humanActions++;
                    save(out,label,next);ui.capture(label);
                    checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+" after human card "+RomanianMatchOracle.label(card)+": audit every newly appended human/Expert move",label,
                            next.toPrettyString()+"\nIndependent reconstruction:\n"+oracle.explanation()+(challenge?"\nRedacted human input: "+WhistChallenger.position(state,0)+"\nHuman decision: "+cardAdvice.explanation():""),checks,
                            List.of(action(trace,"Play human card",RomanianMatchOracle.label(card),"exact card appended once","accepted="+accepted)));
                    state=next;
                    if(challenge&&!backReviewed&&size>=2&&state.path("phase").asText().equals("PLAYING")){
                        JsonNode beforeBack=state.deepCopy();ui.back();ui.awaitText("Resume game",Duration.ofSeconds(15));ui.tapText("Resume game");state=stable(adb);
                        checks=new ArrayList<>(oracle.verify(state));RomanianMatchOracle.equal(checks,"Device Back and Resume preserve the exact paused game",beforeBack.toString(),state.toString());
                        save(out,"ro-device-back-resume",state);ui.capture("ro-device-back-resume");checkpoint(trace,out,"TEST BODY","Device Controls: Back returns to menu and resumes without background AI moves","ro-device-back-resume",state.toPrettyString(),checks,List.of());backReviewed=true;
                    }
                    if(!resumed&&size>=3&&state.path("phase").asText().equals("PLAYING")){
                        JsonNode beforeRestart=state.deepCopy();adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();
                        adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity").requireSuccess();ui.tapText("Resume game");state=stable(adb);
                        checks=new ArrayList<>(oracle.verify(state));RomanianMatchOracle.equal(checks,"RW07 process-death checkpoint exact parity",beforeRestart.toString(),state.toString());
                        save(out,"ro-process-resume",state);ui.capture("ro-process-resume");checkpoint(trace,out,"TEST BODY","RW07 resume the same normal Expert match after process death","ro-process-resume",state.toPrettyString(),checks,List.of());resumed=true;
                    }
                }
                checks=new ArrayList<>(oracle.verify(state));totals=oracle.totals();streaks=oracle.streaks();oracle.scoreCoverage();moves+=oracle.moves();tricks+=size;
                String scoreXml=ui.awaitText(h==23?"Game complete":"Hand complete",Duration.ofSeconds(20));
                for(int seat=0;seat<4;seat++){
                    JsonNode p=state.path("players").get(seat);int won=p.path("tricksWon").asInt(),called=p.path("bid").asInt(),extra=oracle.seriesBonus(seat);int delta=(won==called?5+won:-Math.abs(won-called))+extra;
                    String row=p.path("name").asText()+" | Bid "+called+" | Won "+won+" | "+(delta>=0?"+":"")+delta+(extra==0?"":" (streak "+(extra>0?"+":"")+extra+")")+" | Total: "+totals[seat];
                    RomanianMatchOracle.truth(checks,"RW06 exact visible score row seat "+seat,row,"visible="+ui.hasText(scoreXml,row),ui.hasText(scoreXml,row));
                }
                result.setProperty("hand."+h+".totals",Arrays.toString(totals));result.setProperty("hand."+h+".size",""+size);
                result.setProperty("hand."+h+".trump",state.path("trump").asText());result.setProperty("handsCompleted",""+(h+1));
                for(String key:oracle.coverage.stringPropertyNames())coverage.setProperty(key,""+(Integer.parseInt(coverage.getProperty(key,"0"))+Integer.parseInt(oracle.coverage.getProperty(key))));
                save(out,prefix+"-score",state);ui.capture(prefix+"-score");
                checkpoint(trace,out,h==23?"TEARDOWN":"TEST BODY","Hand "+(h+1)+" / 24: independently reconciled Romanian score and visible totals",prefix+"-score",state.toPrettyString(),checks,List.of());
                store(result,out.resolve("romanian-match-result.properties"));store(coverage,out.resolve("romanian-rule-coverage.properties"));
                if(h+1<limit)ui.tapText("Next hand");else if(h==23) {
                    checks=new ArrayList<>();RomanianMatchOracle.equal(checks,"RW02 complete schedule",24,h+1);RomanianMatchOracle.equal(checks,"Complete card count",376,moves);RomanianMatchOracle.equal(checks,"Complete trick count",94,tricks);RomanianMatchOracle.equal(checks,"Human card count",94,humanActions);
                    RomanianMatchOracle.truth(checks,"No next hand after game completion","Next hand absent",ui.texts(scoreXml).toString(),!ui.hasText(scoreXml,"Next hand"));
                    int best=Arrays.stream(totals).max().orElseThrow();var names=new ArrayList<String>();for(int s=0;s<4;s++)if(totals[s]==best)names.add(state.path("players").get(s).path("name").asText());
                    String winner=(names.size()==1?"Winner: ":"Joint winners: ")+String.join(", ",names)+" ("+best+" points)";
                    RomanianMatchOracle.truth(checks,"Final winner including ties",winner,"visible="+ui.hasText(scoreXml,winner),ui.hasText(scoreXml,winner));
                    save(out,"ro-complete",state);ui.capture("ro-complete");checkpoint(trace,out,"TEARDOWN","Game complete: 24 hands, 94 tricks, 376 cards and final winner","ro-complete",state.toPrettyString(),checks,List.of());
                }
            }
            result.setProperty("status","PASS");result.setProperty("moves",""+moves);result.setProperty("tricks",""+tricks);result.setProperty("humanCards",""+humanActions);result.setProperty("finalTotals",Arrays.toString(totals));result.setProperty("processResume",""+resumed);
            store(result,out.resolve("romanian-match-result.properties"));trace.finish(AtfReportStatus.PASS);
        }catch(Throwable failure){result.setProperty("status","FAIL");result.setProperty("failure",failure.toString());store(result,out.resolve("romanian-match-result.properties"));trace.fail(failure);throw failure;}
    }
    private static void checkpoint(AndroidUiFlowTrace t,Path o,String phase,String title,String label,String actual,List<AtfAssertion>a,List<AtfUiActionRecord> actions)throws Exception{CompleteOriginalMatchE2eTest.checkpoint(t,o,phase,title,label,actual,a,actions);}
    private static AtfUiActionRecord action(AndroidUiFlowTrace t,String operation,String target,String expected,String observed){return new AtfUiActionRecord(t.nextActionSequence(),operation,target,expected,observed,0,AtfReportStatus.PASS,AtfUiEvidenceSource.ACTION);}
    private static void save(Path o,String label,JsonNode s)throws Exception{Files.writeString(o.resolve(label+"-lab-state.json"),s.toPrettyString());}
    static JsonNode read(AdbClient adb)throws Exception{
        String xml=adb.run("exec-out","run-as","ro.whistlab.app","cat","shared_prefs/whist_lab_game.xml").output();
        var f=javax.xml.parsers.DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var strings=f.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml))).getElementsByTagName("string");
        for(int i=0;i<strings.getLength();i++)if(strings.item(i).getAttributes().getNamedItem("name").getNodeValue().equals("current_session_v1"))return JSON.readTree(strings.item(i).getTextContent());
        throw new IllegalStateException("Normal app checkpoint missing");
    }
    static JsonNode stable(AdbClient adb)throws Exception{
        long deadline=System.nanoTime()+Duration.ofSeconds(35).toNanos();JsonNode s;
        do{Thread.sleep(350);s=read(adb);String p=s.path("phase").asText();if(p.equals("SCORE")||p.equals("BIDDING")&&s.path("biddingSeat").asInt()==0||p.equals("PLAYING")&&s.path("currentSeat").asInt()==0){Thread.sleep(200);return s;}}while(System.nanoTime()<deadline);
        throw new IllegalStateException("No stable normal human/score checkpoint: "+s);
    }
    private static void store(Properties p,Path file)throws Exception{try(var out=Files.newOutputStream(file)){p.store(out,"Normal Android Romanian Expert game — independently audited");}}
}

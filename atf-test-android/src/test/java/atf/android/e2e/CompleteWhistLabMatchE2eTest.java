package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.*;
import atf.reporting.ui.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Replays observed deals/bids/card inputs through Whist Lab UI and the normal engine. */
@AndroidE2eTest
public class CompleteWhistLabMatchE2eTest {
    @Test void sameOriginalMatchProducesIdenticalLegalMovesWinnersAndScores()throws Exception {
        assumeTrue(Boolean.getBoolean("atf.android.fullMatch"),"Explicit full-match opt-in required");
        Path out=Path.of(System.getProperty("atf.match.evidenceDir")).toAbsolutePath();
        Properties recording=new Properties();
        try(var in=Files.newInputStream(out.resolve("original-match.properties"))){recording.load(in);}
        assertEquals("true",recording.getProperty("match.complete"),"Original match must be complete");
        AdbClient adb=AdbClient.fromSystemProperties();AndroidUiDriver ui=new AndroidUiDriver(adb,out);
        boolean continuing=Boolean.getBoolean("atf.match.continueLab");
        int retainedHands=continuing?retainedHands(out,recording):0;
        AndroidUiFlowTrace trace=new AndroidUiFlowTrace(out,"match-whist-lab",
                "Whist Lab — replay the same 19 original deals, bids and 436 card inputs",
                getClass().getSimpleName(),"atf.android.e2e","ro.whistlab.app",
                List.of("parallel-app","full-match","recorded-inputs","rules","score"));
        try {
            if(continuing) {
                assertTrue(retainedHands>0&&retainedHands<19,"Continuation needs completed retained hands and an unfinished next hand");
                Properties live=new Properties();live.load(new StringReader(adb.run("exec-out","run-as","ro.whistlab.app","cat","files/replay-observed.properties").output()));
                assertEquals(Integer.toString(retainedHands),live.getProperty("hand"),"Live app must be at the next unfinished hand");
                assertEquals("BIDDING",live.getProperty("phase"));assertEquals("",live.getProperty("moves"));
            } else {
            adb.run("push",out.resolve("original-match.properties").toString(),"/data/local/tmp/atf-original-match.properties").requireSuccess();
            adb.run("shell","run-as","ro.whistlab.app","mkdir","-p","files").requireSuccess();
            adb.run("shell","cat /data/local/tmp/atf-original-match.properties | run-as ro.whistlab.app sh -c 'cat > files/original-match.properties'").requireSuccess();
            adb.run("shell","am","force-stop","ro.whistlab.app").requireSuccess();
            adb.run("shell","am","start","-W","-n","ro.whistlab.app/.MainActivity","--ez","debug_replay","true").requireSuccess();
            }
            int[] totals=new int[4];int moveCount=0;
            for(int h=0;h<19;h++) {
                String k="hand."+h+".",prefix=String.format("lab-h%02d",h+1);
                boolean retained=h<retainedHands;
                OriginalWhistState deal=OriginalWhistState.read(Files.readAllBytes(out.resolve(String.format("original-h%02d-bidding.save2",h+1))));
                OriginalMatchOracle oracle=new OriginalMatchOracle(deal);
                String bidXml=retained?Files.readString(out.resolve(prefix+"-bidding.xml")):ui.awaitText("Place your bid",Duration.ofSeconds(20));
                if(!retained)ui.capture(prefix+"-bidding");
                String biddingRaw=retained?Files.readString(out.resolve(prefix+"-bidding-lab-state.properties")):
                        adb.run("exec-out","run-as","ro.whistlab.app","cat","files/replay-observed.properties").output();
                Properties observedBid=new Properties();observedBid.load(new StringReader(biddingRaw));
                Files.writeString(out.resolve(prefix+"-bidding-lab-state.properties"),biddingRaw);
                List<AtfAssertion> bidChecks=new ArrayList<>();
                OriginalMatchOracle.check(bidChecks,"Actual engine hand size matches original",Integer.toString(deal.handSize),
                        observedBid.getProperty("size"),Integer.toString(deal.handSize).equals(observedBid.getProperty("size")));
                OriginalMatchOracle.check(bidChecks,"Actual dealer matches original",Integer.toString(deal.dealer),
                        observedBid.getProperty("dealer"),Integer.toString(deal.dealer).equals(observedBid.getProperty("dealer")));
                OriginalMatchOracle.check(bidChecks,"Actual trump matches original",Integer.toString(deal.trump),
                        observedBid.getProperty("trump"),Integer.toString(deal.trump).equals(observedBid.getProperty("trump")));
                OriginalMatchOracle.check(bidChecks,"Actual pre-human bidding prefix matches original",Arrays.toString(deal.bids),
                        observedBid.getProperty("bids"),Arrays.equals(deal.bids,numbers(observedBid.getProperty("bids"))));
                OriginalMatchOracle.check(bidChecks,"UI renders the real hand size",handSizeLabel(deal.handSize),
                        ui.texts(bidXml).toString(),ui.hasText(bidXml,handSizeLabel(deal.handSize)));
                for(int seat=0;seat<4;seat++) {
                    Set<Integer> actual=new TreeSet<>(Arrays.stream(numbers(observedBid.getProperty("cards."+seat))).boxed().toList());
                    OriginalMatchOracle.check(bidChecks,"Seat "+seat+" actual distribution matches original",new TreeSet<>(deal.hand(seat)).toString(),
                            actual.toString(),actual.equals(new TreeSet<>(deal.hand(seat))));
                }
                if(h==0)WhistTestEvidence.checkpoint(trace,out,"SETUP","Load the completed original match as debug input fixtures",
                        prefix+"-bidding","Original outcomes are not injected into the game engine",List.of(
                                new AtfAssertion("Complete fixture", "19 hands / 436 card inputs",recording.getProperty("match.handCount")+" / "+recording.getProperty("match.moves"),
                                        "19".equals(recording.getProperty("match.handCount"))&&"436".equals(recording.getProperty("match.moves")))));
                WhistTestEvidence.checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+": load the same real distribution and trump",
                        prefix+"-bidding","Recorded original inputs; actual engine state is read from the debug observation port",bidChecks);
                if(deal.dealer==0) {
                    int prior=Arrays.stream(deal.bids).filter(b->b>=0).sum();
                    int forbidden=deal.handSize-prior;
                    Set<Integer> expectedOptions=new TreeSet<>(java.util.stream.IntStream.rangeClosed(0,deal.handSize)
                            .filter(b->b!=forbidden).boxed().toList());
                    String optionsLabel=prefix+"-last-bid-options";
                    String optionsXml;
                    if(retained)optionsXml=Files.readString(out.resolve(optionsLabel+".xml"));
                    else {ui.tapSpinner(0);optionsXml=ui.dump();}
                    Set<Integer> actualOptions=new TreeSet<>(ui.texts(optionsXml).stream().filter(t->t.matches("\\d+"))
                            .map(Integer::parseInt).toList());
                    if(!retained)ui.capture(optionsLabel);
                    WhistTestEvidence.checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+": final bidder cannot select the forbidden total",
                            optionsLabel,"prior bids="+prior+"; hand size="+deal.handSize+"; forbidden candidate="+forbidden,List.of(
                                    new AtfAssertion("Actual UI bid choices implement the same last-bid restriction",expectedOptions.toString(),
                                            actualOptions.toString(),actualOptions.equals(expectedOptions))));
                    if(!retained){ui.back();ui.awaitText("Place your bid",Duration.ofSeconds(10));}
                }
                int[] originalBids=numbers(recording.getProperty(k+"bids"));
                int humanBid=originalBids[0];
                if(!retained){ui.selectBid(humanBid);ui.tapText("Place bid");}
                OriginalWhistState state=retained?retainedState(out,prefix+"-turn00",deal):awaitStable(adb,out,prefix+"-turn00",deal,0);
                captureAndVerify(trace,ui,out,prefix+"-turn00","Hand "+(h+1)+": submit bid "+humanBid+" and verify initial AI moves",state,oracle,totals,originalBids,-1,retained);
                List<Integer> expectedMoves=Arrays.stream(numbers(recording.getProperty(k+"plays"))).boxed().toList();
                int humanTurns=0;
                while(!state.handOver) {
                    int chosen=expectedMoves.get(state.plays().size());
                    String card=OriginalWhistState.card(chosen);
                    if(!retained)tapVisibleCard(adb,ui,card);
                    String label=prefix+String.format("-turn%02d",++humanTurns);
                    int minimum=state.plays().size()+1;
                    state=retained?retainedState(out,label,deal):awaitStable(adb,out,label,deal,minimum);
                    captureAndVerify(trace,ui,out,label,"Hand "+(h+1)+", human turn "+humanTurns+": "+card,
                            state,oracle,totals,originalBids,chosen,retained);
                    assertTrue(humanTurns<=deal.handSize,"Replay failed to advance");
                }
                int[] originalTotals=numbers(recording.getProperty(k+"totals"));
                assertArrayEquals(originalTotals,oracle.totals,"Cumulative score parity on hand "+(h+1));
                assertEquals(expectedMoves,oracle.plays,"Every actual input must match original sequence");
                totals=oracle.totals.clone();moveCount+=oracle.plays.size();
                String scoreXml=retained?Files.readString(out.resolve(prefix+"-score-parity.xml")):
                        ui.awaitText(h==18?"Game complete":"Hand complete",Duration.ofSeconds(10));
                List<AtfAssertion> scoreChecks=new ArrayList<>();
                for(int p=0;p<4;p++) {
                    int earned=oracle.won[p]+(oracle.won[p]==originalBids[p]?10:0);
                    String expectedRow=List.of("You","Ava","Miles","Grace").get(p)+" | Bid "+originalBids[p]
                            +" | Won "+oracle.won[p]+" | +"+earned+" | Total: "+originalTotals[p];
                    boolean visible=ui.hasText(scoreXml,expectedRow);
                    scoreChecks.add(new AtfAssertion("Seat "+p+" visible score agrees with original and rule oracle",
                            expectedRow,"exact score row visible="+visible,visible));
                }
                String scoreLabel=prefix+"-score-parity";if(!retained)ui.capture(scoreLabel);
                WhistTestEvidence.checkpoint(trace,out,"TEST BODY","Hand "+(h+1)+": all score rows match original",
                        scoreLabel,Arrays.toString(originalTotals),scoreChecks);
                System.out.println("LAB MATCH hand="+(h+1)+"/19; moves="+oracle.plays.size()+"; totals="+Arrays.toString(totals));
                if(h<18&&!retained)ui.tapText("Next hand");
            }
            assertEquals(436,moveCount);
            ui.capture("whist-lab-full-match-teardown");
            WhistTestEvidence.checkpoint(trace,out,"TEARDOWN","Retain Whist Lab's final independently calculated score",
                    "whist-lab-full-match-teardown",Arrays.toString(totals),List.of(
                            new AtfAssertion("Replay completed all original inputs","436",Integer.toString(moveCount),moveCount==436)));
            trace.finish(AtfReportStatus.PASS);
        }catch(Throwable failure){trace.fail(failure);throw failure;}
    }
    private static void tapVisibleCard(AdbClient adb,AndroidUiDriver ui,String card)throws Exception {
        for(int attempt=0;attempt<6;attempt++) {
            String xml=ui.dump();
            if(ui.hasText(xml,card)){assertTrue(ui.isEnabled(xml,card),"Recorded legal card disabled: "+card);ui.tapTextIn(xml,card);return;}
            adb.run("shell","input","swipe","540","1580","540","600","250").requireSuccess();
        }
        throw new IllegalStateException("Recorded human card is not visible: "+card);
    }
    private static void captureAndVerify(AndroidUiFlowTrace trace,AndroidUiDriver ui,Path out,String label,String title,
            OriginalWhistState state,OriginalMatchOracle oracle,int[] totals,int[] originalBids,int requestedCard,boolean retained)throws Exception {
        if(!retained)ui.capture(label);
        int previousPlayCount=oracle.plays.size();
        List<AtfAssertion> checks=new ArrayList<>(oracle.verify(state,totals));
        OriginalMatchOracle.check(checks,"Actual full bid vector matches recorded original inputs",Arrays.toString(originalBids),
                Arrays.toString(state.bids),Arrays.equals(originalBids,state.bids));
        if(!state.handOver) {
            String xml=Files.readString(out.resolve(label+".xml"));
            boolean mustFollow=oracle.leadSuit>=0&&state.hand(0).stream().anyMatch(c->c/13==oracle.leadSuit);
            for(int card:state.hand(0)) {
                String text=OriginalWhistState.card(card);
                if(ui.hasText(xml,text)) {
                    boolean allowed=!mustFollow||card/13==oracle.leadSuit;
                    OriginalMatchOracle.check(checks,"Visible card "+text+" enabled state enforces follow-suit",
                            "enabled="+allowed,"enabled="+ui.isEnabled(xml,text),ui.isEnabled(xml,text)==allowed);
                }
            }
        }
        AtfUiActionRecord input;
        if(requestedCard>=0) {
            int accepted=state.plays().size()>previousPlayCount?state.plays().get(previousPlayCount):-1;
            boolean correct=accepted==requestedCard;
            OriginalMatchOracle.check(checks,"Requested Android UI card was accepted",OriginalWhistState.card(requestedCard),
                    accepted<0?"no new play":OriginalWhistState.card(accepted),correct);
            input=new AtfUiActionRecord(trace.nextActionSequence(),(retained?"Retained ":"")+"Android UI card tap",OriginalWhistState.card(requestedCard),
                    "selected enabled card is played",accepted<0?"no new play":OriginalWhistState.card(accepted),0,
                    correct?AtfReportStatus.PASS:AtfReportStatus.FAIL,AtfUiEvidenceSource.ACTION);
        }else input=new AtfUiActionRecord(trace.nextActionSequence(),(retained?"Retained ":"")+"Android UI bid submission","Bid "+originalBids[0]+"; Place bid",
                "selected bid is submitted","actual human bid="+state.bids[0],0,
                originalBids[0]==state.bids[0]?AtfReportStatus.PASS:AtfReportStatus.FAIL,AtfUiEvidenceSource.ACTION);
        WhistTestEvidence.checkpoint(trace,out,"TEST BODY",title+(retained?" (retained UI execution revalidated)":""),label,
                (retained?"Retained real UI capture; no new tap issued. ":"")+state.summary(),checks,List.of(input));
    }
    private static OriginalWhistState awaitStable(AdbClient adb,Path out,String label,OriginalWhistState deal,int minimum)throws Exception {
        long deadline=System.nanoTime()+Duration.ofSeconds(25).toNanos();
        Properties p=new Properties();
        do {
            Thread.sleep(450);String raw=adb.run("exec-out","run-as","ro.whistlab.app","cat","files/replay-observed.properties").output();
            p.clear();p.load(new StringReader(raw));
            String phase=p.getProperty("phase","");int[] moves=numbers(p.getProperty("moves",""));
            if(moves.length>=minimum&&(phase.equals("SCORE")||phase.equals("PLAYING")&&p.getProperty("turn").equals("0"))) {
                Files.writeString(out.resolve(label+"-lab-state.properties"),raw);
                OriginalWhistState s=fromObservation(p,deal);
                Thread.sleep(200);return s;
            }
        }while(System.nanoTime()<deadline);
        throw new IllegalStateException("Whist Lab did not reach a stable human/score checkpoint: "+p);
    }
    static String handSizeLabel(int size){return size+(size==1?" card":" cards")+" in this hand";}
    static int retainedHands(Path out,Properties recorded)throws Exception {
        int count=0;
        for(int h=0;h<19;h++) {
            String prefix=String.format("lab-h%02d",h+1);int size=Integer.parseInt(recorded.getProperty("hand."+h+".size"));
            Path file=out.resolve(prefix+String.format("-turn%02d-lab-state.properties",size));
            if(!Files.exists(file)||!Files.exists(out.resolve(prefix+"-score-parity.xml")))break;
            Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
            if(!"SCORE".equals(p.getProperty("phase"))||!Integer.toString(h).equals(p.getProperty("hand"))
                    ||numbers(p.getProperty("moves","")).length!=4*size)break;
            count++;
        }
        return count;
    }
    private static OriginalWhistState retainedState(Path out,String label,OriginalWhistState deal)throws Exception {
        Properties p=new Properties();try(var in=Files.newInputStream(out.resolve(label+"-lab-state.properties"))){p.load(in);}
        return fromObservation(p,deal);
    }
    private static OriginalWhistState fromObservation(Properties p,OriginalWhistState deal) {
                String phase=p.getProperty("phase");
                int[] moves=numbers(p.getProperty("moves",""));
                OriginalWhistState s=new OriginalWhistState();s.players=4;s.mode=6;s.deck=1;
                s.handIndex=Integer.parseInt(p.getProperty("hand"));s.handSize=Integer.parseInt(p.getProperty("size"));
                s.serializedHandSize=s.handSize;
                s.dealer=Integer.parseInt(p.getProperty("dealer"));s.trump=Integer.parseInt(p.getProperty("trump"));s.trumpCard=deal.trumpCard;
                s.turn=Integer.parseInt(p.getProperty("turn"));s.handOver=phase.equals("SCORE");
                s.remaining=new int[4];s.cards=new int[4][];
                for(int seat=0;seat<4;seat++){s.cards[seat]=numbers(p.getProperty("cards."+seat));s.remaining[seat]=s.cards[seat].length;}
                s.bids=numbers(p.getProperty("bids"));s.won=numbers(p.getProperty("won"));
                s.scores=new int[s.handIndex+1][4];s.scores[s.handIndex]=numbers(p.getProperty("totals"));
                List<Integer> history=new ArrayList<>();history.add(deal.trumpCard);for(int c:moves)history.add(c);s.history=history;
                return s;
    }
    static int[] numbers(String csv){return csv.isBlank()?new int[0]:Arrays.stream(csv.split(",")).mapToInt(Integer::parseInt).toArray();}
}

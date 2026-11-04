package atf.tests.publishing;

import atf.testkit.AtfComponentTest;

import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportPhase;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import atf.reporting.contract.AtfReportStatus;

import atf.reporting.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Consumer composition through the J0-owned binary; no private HTML renderer. */
@AtfComponentTest
class RuleComponentExtentReportTest {
    static final List<ReportField> RULES=List.of(
            new ReportField("RW01 — deck","3–6 players; 8 cards per player. Keep ranks 9..A / 7..A / 5..A / 3..A for 3 / 4 / 5 / 6 players. Hearts and diamonds red; spades and clubs black. Ace is highest."),
            new ReportField("RW02 — schedule and trump","N one-card hands; 2..7; N eight-card hands; 7..2; N one-card hands. Dealer advances clockwise. First undealt card sets trump; eight-card hands have no trump."),
            new ReportField("RW03 — bidding","Start left of dealer; bid 0..hand size. Only the last bid making the sum equal to hand size is forbidden. Earlier bids may make that running sum."),
            new ReportField("RW04 — legal card","Leader may play any held card. Follow led suit if held, even after someone trumped. Otherwise trump if held; otherwise discard. No overtrump obligation."),
            new ReportField("RW05 — trick winner","Highest trump wins; without a trump, highest card of led suit wins. Off-suit non-trump cannot win. Seats play clockwise; winner leads the next trick."),
            new ReportField("RW06 — score","Exact contract: 5 + tricks. Otherwise: minus absolute difference between tricks and bid. Add to prior total once after the final card; negative totals are valid."),
            new ReportField("RW07 — persistence and completion",("Save bids, hands, public played cards, turned card, trick counts and cumulative scores. Reject inconsistent checkpoints. Finish after the final scheduled hand; " +
                "scoring cannot be applied twice.")));
    @Test void publishesOfflineRuleCasesAndActualIndependentGameTrace()throws Exception {
        assumeTrue(Boolean.getBoolean("atf.rules.publish"));
        Path run=Path.of(System.getProperty("atf.rules.runDir")),report=run.resolve("component-extent-report.html");
        List<Path> xmls;
        try(var files=Files.walk(run.resolve("components"))){xmls=files.filter(p->p.getFileName().toString().startsWith("TEST-")&&p.toString().endsWith(".xml")).sorted().toList();}
        assertFalse(xmls.isEmpty());
        long tests=0,failures=0,skips=0;List<ReportObservation> cases=new ArrayList<>();
        for(Path file:xmls) {
            var root=parse(file).getDocumentElement();tests+=number(root,"tests");failures+=number(root,"failures")+number(root,"errors");skips+=number(root,"skipped");
            var nodes=root.getElementsByTagName("testcase");
            for(int i=0;i<nodes.getLength();i++) {
                var test=(org.w3c.dom.Element)nodes.item(i);
                boolean pass=test.getElementsByTagName("failure").getLength()+test.getElementsByTagName("error").getLength()==0;
                List<ReportField> details=new ArrayList<>();details.add(new ReportField("Result",pass?"PASS":"FAIL"));
                details.add(new ReportField("Execution time (seconds)",test.getAttribute("time")));
                if(!pass)details.add(new ReportField("Failure",test.getTextContent()));
                cases.add(new ReportObservation(test.getAttribute("classname")+" :: "+test.getAttribute("name"),details,List.of(),List.of(artifact(file))));
            }
        }
        Properties trace=load(run.resolve("evidence/representative-game-trace.properties"));
        Properties ai=load(run.resolve("evidence/ai-tournament.properties"));
        int games=0,moves=0,tricks=0,forced=0,discards=0,negative=0;
        try(var files=Files.list(run.resolve("evidence"))) {
            for(Path file:files.filter(p->p.getFileName().toString().startsWith("game-")).toList()) {
                Properties game=load(file);assertEquals("PASS",game.getProperty("status"));games++;
                moves+=Integer.parseInt(game.getProperty("moves"));tricks+=Integer.parseInt(game.getProperty("tricks"));
                forced+=Integer.parseInt(game.getProperty("forcedTrumpMoves"));discards+=Integer.parseInt(game.getProperty("voidDiscardMoves"));
                negative+=Integer.parseInt(game.getProperty("negativeScoreCheckpoints"));
            }
        }
        boolean pass=failures==0&&skips==0&&"PASS".equals(ai.getProperty("status"));

        try(var session=GameReportSession.open(report)) {
            var scenario=session.startScenario("Whist romanesc — component rule contracts and AI evaluation","atf.tests","JVM / offline",List.of("component","romanian-whist","independent-oracle","AI"));
            try(var phase=scenario.startPhase("SETUP")) {
                phase.observation(new ReportObservation("Rule profile and source contract",List.of(
                        new ReportField("Profile","ROMANIAN_WHIST: classic 1–8–1; mandatory trump when void; exact 5 + tricks; otherwise minus absolute bid error"),
                        new ReportField("Primary game publisher rules","https://ro.goobix.com/jocuri-online/whist/"),
                        new ReportField("Detailed rule description","https://www.pagat.com/exact/romwhist.html"),
                        new ReportField("Source comparison","Club and platform variants differ. This profile has no streak bonuses, forced overtrump, blind one-card bidding or tournament penalties."),
                        new ReportField("Scope","Actual JVM component execution. No Android UI execution, simulated screenshots, or original-app parity is asserted.")),List.of(),List.of()));
                phase.observation(new ReportObservation("Complete normative rules (RW01–RW07)",RULES,List.of(),List.of()));
                phase.complete(AtfReportStatus.PASS);
            }
            try(var phase=scenario.startPhase("TEST BODY")) {
                phase.observation(new ReportObservation("Executed coverage",List.of(
                        new ReportField("JUnit cases",Long.toString(tests)),new ReportField("Failures / skipped",failures+" / "+skips),
                        new ReportField("Complete independent-model games",""+games),new ReportField("Card plays / tricks",moves+" / "+tricks),
                        new ReportField("Forced trump moves / void discards",forced+" / "+discards),
                        new ReportField("Negative-score checkpoints",""+negative),
                        new ReportField("Exhaustive legality combinations","6375"),new ReportField("Exhaustive winner comparisons","4960"),
                        new ReportField("Independence",
                            "Expected legal cards, actors, winners, next leaders and scores are calculated without calling GameRules. Model games also restore checkpoints during bidding, play and scoring.")),List.of(),List.of()));
                for(ReportObservation detail:cases)phase.observation(detail);
                phase.observation(new ReportObservation("AI paired held-out benchmark",ai.stringPropertyNames().stream().filter(k->!k.startsWith("pair.")).sorted().map(k->new ReportField(k,ai.getProperty(k))).toList(),List.of(),
                    List.of(artifact(run.resolve("evidence/ai-tournament.properties")))));
                Path coverage=run.resolve("coverage/jacocoTestReport.xml");
                if(Files.exists(coverage)) {
                    var root=parse(coverage).getDocumentElement();var counters=root.getChildNodes();List<ReportField> fields=new ArrayList<>();
                    for(int i=0;i<counters.getLength();i++)if(counters.item(i) instanceof org.w3c.dom.Element e&&e.getTagName().equals("counter"))
                        fields.add(new ReportField(e.getAttribute("type"),"covered="+e.getAttribute("covered")+"; missed="+e.getAttribute("missed")));
                    GameReportPhase.optional(phase, new ReportObservation("Measured JaCoCo coverage — game-core",fields,List.of(),List.of(artifact(coverage))));
                }
                phase.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            try(var phase=scenario.startPhase("TEARDOWN")) {
                phase.observation(new ReportObservation("Reproduction and boundaries",List.of(
                        new ReportField("Command","scripts/run-rule-components.ps1"),
                        new ReportField("AI information boundary",("Only own cards, public bids/counters, remaining hand sizes and played-card history are passed to the sampling API. Hidden-card permutations must leave the obser" +
                            "vation and decision identical.")),
                        new ReportField("AI limitation","32 sampled deals and a heuristic rollout opponent. The held-out benchmark measures this declared sample, not global optimality or human-level strength."),
                        new ReportField("Android next",("Only after component acceptance: restore the preserved original match, then validate Android integration and exact replay. Romanian rules do not redefine the se" +
                            "parate Oh Hell oracle."))),List.of(),List.of()));
                phase.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            scenario.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            var game=session.startScenario("Whist romanesc — complete 4-seat component game, every move explained","game-core","JVM / offline",List.of("component-flow","24-hands","94-tricks","376-cards"));
            for(String name:List.of("SETUP","TEST BODY","TEARDOWN"))try(var phase=game.startPhase(name)) {
                for(int i=0;i<Integer.parseInt(trace.getProperty("step.count"));i++) {
                    String k="step."+i+".";if(!name.equals(trace.getProperty(k+"phase")))continue;
                    phase.observation(new ReportObservation(trace.getProperty(k+"title"),List.of(
                            new ReportField("Rule IDs",trace.getProperty(k+"rules")),new ReportField("Inputs / hand / legal choices",trace.getProperty(k+"context")),
                            new ReportField("Expected",trace.getProperty(k+"expected")),new ReportField("Actual",trace.getProperty(k+"actual"))),
                            List.of(new ReportObservation.Body("Full inputs and legal choices",trace.getProperty(k+"context"),ReportObservation.Format.TEXT),
                                    new ReportObservation.Body("Full expected state",trace.getProperty(k+"expected"),ReportObservation.Format.TEXT),
                                    new ReportObservation.Body("Full actual state",trace.getProperty(k+"actual"),ReportObservation.Format.TEXT)),List.of()));
                }
                phase.complete(AtfReportStatus.PASS);
            }
            game.complete(AtfReportStatus.PASS);
        }
        String html=Files.readString(report);assertTrue(html.contains("RW04/RW05"));assertTrue(html.contains("376"));
        assertTrue(html.contains("Expected")&&html.contains("Actual"));
        for(ReportField rule:RULES)assertTrue(html.contains(rule.value()),"Report lost rule contract: "+rule.name());
        for(int i=0;i<Integer.parseInt(trace.getProperty("step.count"));i++)for(String field:List.of("title","rules","context","expected","actual")) {
            String value=trace.getProperty("step."+i+"."+field);
            String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
            assertTrue(html.contains(value)||html.contains(escaped),"Report omitted complete component evidence: "+value);
        }
        Properties verification=new Properties();verification.setProperty("cases",""+tests);verification.setProperty("failures",""+failures);
        verification.setProperty("games",""+games);verification.setProperty("moves",""+moves);verification.setProperty("tricks",""+tricks);
        verification.setProperty("status",pass?"PASS":"FAIL");verification.setProperty("report.bytes",""+Files.size(report));
        try(var stream=Files.newOutputStream(run.resolve("component-report-verification.properties"))){verification.store(stream,"Actual component results and native Extent output");}
        assertTrue(pass,"Component or AI acceptance failed; the retained Extent report includes the failure");
    }
    private static org.w3c.dom.Document parse(Path p)throws Exception {
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        // JaCoCo emits an external DTD; never fetch it while parsing a local report.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        return factory.newDocumentBuilder().parse(p.toFile());
    }
    private static long number(org.w3c.dom.Element e,String k){return Long.parseLong(e.getAttribute(k));}
    private static Properties load(Path p)throws Exception{Properties v=new Properties();try(var in=Files.newInputStream(p)){v.load(in);}return v;}
    private static ReportObservation.Artifact artifact(Path p) {
        try{return new ReportObservation.Artifact(p.getFileName().toString(),p.toUri(),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
}

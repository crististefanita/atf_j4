package atf.tests;

import atf.testkit.AtfComponentTest;

import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportTextAudit;
import atf.tests.reporting.GameReportPhase;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.reporting.configuration.*;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Native J0 Reporting composition for a complete normal Romanian Expert Android game. */
@AtfComponentTest
class RomanianExpertExtentReportTest {
    @Test void publishesEveryNormalGameCheckpointAndCompleteRuleEvidence()throws Exception{
        assumeTrue(Boolean.getBoolean("atf.romanian.publish"));
        Path run=Path.of(System.getProperty("atf.romanian.runDir"));Path evidence=run.resolve("evidence/android");
        Path report=run.resolve("romanian-expert-extent-report.html"),proof=run.resolve("report-verification.properties");
        Files.writeString(proof,"status=PENDING\n");
        Properties flow=load(evidence.resolve("ui-flow-romanian-expert-complete.properties")),result=load(evidence.resolve("romanian-match-result.properties"));
        Properties coverage=load(evidence.resolve("romanian-rule-coverage.properties")),ai=load(run.resolve("evidence/components/ai-tournament.properties"));
        var images=new ArrayList<Path>();long assertions=0,failed=0,actions=0;
        for(String k:flow.stringPropertyNames()){
            if(k.endsWith(".screenshot")&&!flow.getProperty(k).isBlank())images.add(evidence.resolve(flow.getProperty(k)));
            if(k.endsWith(".passed")){assertions++;if(!Boolean.parseBoolean(flow.getProperty(k)))failed++;}
            if(k.endsWith(".actionCount"))actions+=Long.parseLong(flow.getProperty(k));
        }
        List<Path> xmls;try(var files=Files.walk(run.resolve("components"))){xmls=files.filter(p->p.getFileName().toString().startsWith("TEST-")&&p.toString().endsWith(".xml")).sorted().toList();}
        long cases=0,errors=0,skips=0;List<ReportObservation> details=new ArrayList<>();
        for(Path xml:xmls){var f=javax.xml.parsers.DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);var suite=f.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
            cases+=Long.parseLong(suite.getAttribute("tests"));errors+=Long.parseLong(suite.getAttribute("failures"))+Long.parseLong(suite.getAttribute("errors"));skips+=Long.parseLong(suite.getAttribute("skipped"));
            var fields=new ArrayList<ReportField>();fields.add(new ReportField("Tests / failures / errors / skipped",
                suite.getAttribute("tests")+" / "+suite.getAttribute("failures")+" / "+suite.getAttribute("errors")+" / "+suite.getAttribute("skipped")));
            var tests=suite.getElementsByTagName("testcase");for(int i=0;i<tests.getLength();i++){var t=(org.w3c.dom.Element)tests.item(i);fields.add(new ReportField(t.getAttribute("name"),
                t.getElementsByTagName("failure").getLength()+t.getElementsByTagName("error").getLength()>0?"FAIL":t.getElementsByTagName("skipped").getLength()>0?"SKIP":"PASS"));}
            details.add(new ReportObservation(suite.getAttribute("name"),fields,List.of(),List.of(artifact(xml))));
        }
        boolean pass="PASS".equals(flow.getProperty("flow.status"))&&"PASS".equals(result.getProperty("status"))&&"PASS".equals(ai.getProperty("status"))&&errors==0&&skips==0&&failed==0;

        try(var session=GameReportSession.open(report)){
            var scenario=session.startScenario("Romanian Whist Expert — full normal Android acceptance","atf.tests","Android API 28 / ADB / Core C3 / Reporting J12",List.of("romanian","expert","component-first","24-hands","376-cards"));
            try(var phase=scenario.startPhase("SETUP")){
                phase.observation(new ReportObservation("Normative rules and independent verification",RuleComponentExtentReportTest.RULES,List.of(),List.of()));
                phase.observation(new ReportObservation("Execution contract",List.of(
                        new ReportField("Normal game","New game through Android setup, four seats, Romanian Whist, Expert. No debug replay, fixed seed, injected deals, bids, moves, winners or scores."),
                        new ReportField("Human test policy","Bid estimated from own high cards and allowed UI domain. Select varied legal cards from own hand. Opponents use normal Expert AI."),
                        new ReportField("Observation",("Read-only private checkpoint of our debuggable app. Compare complete append-only public history with a separate integer-card state machine; no game-core calls i" +
                            "n the Android oracle.")),
                        new ReportField("Screenshot timing",("Real stable human/bidding/score screens. An after-action screenshot may cover several automatic Expert moves; each is audited from public history. Images are no" +
                            "t claimed as one frame per AI card.")),
                        new ReportField("Timing","0 ms action/observe records are uninstrumented placeholders, not latency measurements. Correctness run, not a performance benchmark.")),List.of(),List.of()));
                for(String name:List.of("environment.json","apk-provenance.json","library-binaries.json")){Path p=run.resolve(name);if(Files.exists(p))phase.observation(new ReportObservation(name,List.of(),List.of(new
                    ReportObservation.Body("Complete provenance",Files.readString(p),ReportObservation.Format.TEXT)),List.of(artifact(p))));}
                Path source=run.resolve("source-snapshot.zip");if(Files.exists(source))GameReportPhase.optional(phase, new ReportObservation("Consumer source and flat library snapshot",List.of(new ReportField("Scope",
                    "J4 consumer sources, build files and binary dependencies. Generated output and Android credentials are excluded.")),List.of(),List.of(artifact(source))));
                phase.complete(AtfReportStatus.PASS);
            }
            try(var phase=scenario.startPhase("TEST BODY")){
                phase.observation(new ReportObservation("Accepted execution inventory",List.of(new ReportField("JUnit / Gradle cases",""+cases),new ReportField("Failures / errors",""+errors),new ReportField("Skipped",""+skips),new
                    ReportField("UI assertions",""+assertions),new ReportField("Action / observation records",""+actions),new ReportField("Screenshot checkpoints",""+images.size())),List.of(),List.of()));
                for(var detail:details)phase.observation(detail);
                Path mutant=run.resolve("history-mutant-control.json");if(Files.exists(mutant))phase.observation(new ReportObservation("Expected rejection of a deliberately disabled history validator",List.of(new
                    ReportField("Interpretation",("Seven of twelve component tests reject the isolated mutant. This is a negative control; no mutant APK was produced or installed, and its expected failures are n" +
                    "ot functional failures."))),List.of(new ReportObservation.Body("Exact negative-control receipt",Files.readString(mutant),ReportObservation.Format.TEXT)),List.of(artifact(mutant),
                    artifact(run.resolve("history-mutant-rejection.log")))));
                phase.observation(new ReportObservation("All hand scores and final game result",List.of(),List.of(new ReportObservation.Body("Complete result",Files.readString(evidence.resolve("romanian-match-result.properties")),
                    ReportObservation.Format.TEXT)),List.of(artifact(evidence.resolve("romanian-match-result.properties")))));
                Path audit=evidence.resolve("offline-audit-result.properties");if(Files.exists(audit))phase.observation(new ReportObservation("Independent reconstruction from retained evidence, no Android connection",List.of(),
                    List.of(new ReportObservation.Body("Offline audit",Files.readString(audit),ReportObservation.Format.TEXT)),List.of(artifact(audit))));
                phase.observation(new ReportObservation("Actually observed Romanian rule cases",coverage.stringPropertyNames().stream().sorted().map(k->new ReportField(k,coverage.getProperty(k))).toList(),List.of(),List.of()));
                phase.observation(new ReportObservation("Expert paired benchmark after the change",List.of(new ReportField("Status",ai.getProperty("status")),new ReportField("Paired comparisons",ai.getProperty("comparisons")),new
                    ReportField("Expert / Hard points",ai.getProperty("expertTotal")+" / "+ai.getProperty("baselineTotal"))),List.of(new ReportObservation.Body("Every paired deal and result",
                    Files.readString(run.resolve("evidence/components/ai-tournament.properties")),ReportObservation.Format.TEXT)),List.of()));
                Path c=run.resolve("component-acceptance/component-extent-report.html");phase.observation(new ReportObservation("Deep component report",List.of(new ReportField("Scope",
                    "Rules, save-history negative controls, player explanations, forced-move optimization, independent complete games, AI benchmark and JaCoCo.")),List.of(),Files.exists(c)?List.of(artifact(c)):List.of()));
                phase.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            try(var phase=scenario.startPhase("TEARDOWN")){
                phase.observation(new ReportObservation("Scope and audit limits",List.of(new ReportField("Completion","24 hands / 94 tricks / 376 plays / 94 human cards; same match also resumes after process death."),new
                    ReportField("Boundaries","One normal random four-seat Expert Android match. Other table sizes tested in components. Multiplayer and other devices not certified."),new ReportField("HTML visual audit",
                    "MCP previously denied the local file scheme. No bypass. Exact decoded PNG bytes and complete text are verified automatically; direct PNG review is recorded separately."),new ReportField("Ownership",
                    "J0 owns Core/Reporting implementation. J4 uses native sessions, short preview 240 and complete TEXT evidence bodies.")),List.of(),List.of()));phase.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            scenario.complete(pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            DeepSuiteExtentReportTest.publishUiFlow(session,evidence.resolve("ui-flow-romanian-expert-complete.properties"),evidence,true);
        }
        String html=Files.readString(report);var matcher=java.util.regex.Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)").matcher(html);
        Set<String> hashes=new HashSet<>();StringBuilder text=new StringBuilder();int end=0,uris=0;
        while(matcher.find()){hashes.add(hash(Base64.getDecoder().decode(matcher.group(1))));uris++;text.append(html,end,matcher.start()).append("[verified PNG]");end=matcher.end();}text.append(html,end,html.length());String
            rendered=text.toString();var textAudit=new GameReportTextAudit(rendered);
        for(Path image:images)assertTrue(hashes.contains(hash(Files.readAllBytes(image))),"Exact PNG omitted: "+image);
        for(String key:flow.stringPropertyNames())if(key.endsWith(".title")||key.matches(".*\\.assertion\\.\\d+\\.(name|expected|actual)$")||key.matches(".*\\.action\\.\\d+\\.(operation|target|expected|observed)$")){
            String value=flow.getProperty(key);if(!value.isBlank())assertTrue(textAudit.contains(value),()->"Incomplete report field: "+key+" = "+value);
        }
        assertTrue(pass);assertTrue(cases>200);
        assertTrue(xmls.stream().anyMatch(p->p.getFileName().toString().equals("TEST-atf.android.e2e.CompleteRomanianExpertE2eTest.xml")),"Live JUnit execution proof required");
        assertTrue(xmls.stream().anyMatch(p->p.getFileName().toString().equals("TEST-atf.android.e2e.RecordedRomanianMatchAuditTest.xml")),"Offline reconstruction execution proof required");
        assertEquals("PASS",load(evidence.resolve("offline-audit-result.properties")).getProperty("status"));
        assertEquals("24",result.getProperty("handsCompleted"));assertEquals("376",result.getProperty("moves"));assertEquals("94",result.getProperty("tricks"));assertEquals("94",result.getProperty("humanCards"));assertEquals("true",
            result.getProperty("processResume"));assertEquals("128",coverage.getProperty("no-trump-card"));
        for(String key:List.of("mandatory-trump","void-discard","exact-zero","exact-positive","bid-too-high","bid-too-low"))assertTrue(Integer.parseInt(coverage.getProperty(key,"0"))>0,"Missing observed branch: "+key);
        Properties p=new Properties();p.setProperty("status","PASS");p.setProperty("cases",""+cases);p.setProperty("assertions",""+assertions);p.setProperty("failedAssertions",""+failed);p.setProperty("actions",""+actions);
            p.setProperty("screenshotCheckpoints",""+images.size());p.setProperty("inlinePngUris",""+uris);p.setProperty("uniquePngByteHashes",""+hashes.size());p.setProperty("report.bytes",""+Files.size(report));
            p.setProperty("report.sha256",hashFile(report));try(var out=Files.newOutputStream(proof)){p.store(out,"Complete text and exact decoded PNG bytes verified");}
    }
    private static Properties load(Path p)throws Exception{Properties r=new Properties();try(var in=Files.newInputStream(p)){r.load(in);}return r;}
    private static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}
    private static String hashFile(Path p)throws Exception{var d=java.security.MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(p)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}return
        HexFormat.of().formatHex(d.digest());}
    private static ReportObservation.Artifact artifact(Path p){try{return new ReportObservation.Artifact(p.getFileName().toString(),p.toUri(),hashFile(p));}catch(Exception e){throw new IllegalStateException(e);}}
}

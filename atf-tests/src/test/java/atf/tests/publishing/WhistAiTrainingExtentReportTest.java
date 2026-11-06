package atf.tests.publishing;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;

import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportTextAudit;
import atf.tests.reporting.GameReportPhase;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.reporting.configuration.*;
import atf.reporting.contract.AtfReportStatus;

import atf.reporting.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** J4 evidence composition using the J0 binary and native UI-flow observations. */
@AtfComponentTest
class WhistAiTrainingExtentReportTest {
    @Test void publishesRulesEveryAcceptedComponentAndTheDeclaredAndroidDuel()throws Exception{
        assumeTrue(Boolean.getBoolean("atf.ai.publish"));
        Path run=Path.of(System.getProperty("atf.ai.runDir"));
        String expectedSeriesReward=System.getProperty("atf.ai.expectedBonus","10");
        Path report=run.resolve("whist-ai-extent-report.html"),proof=run.resolve("ai-report-verification.properties");
        Files.writeString(proof,"status=PENDING\n");
        for(String name:List.of("source-hashes.json","apk-provenance.json","library-binaries.json","environment.json","coverage/jacocoTestReport.xml"))AuditedAssertions.assertTrue(Files.isRegularFile(run.resolve(name)),
            "Missing provenance or measured coverage: "+name);
        List<Path> xmls=files(run.resolve("accepted-components"),p->p.getFileName().toString().startsWith("TEST-")&&p.toString().endsWith(".xml"));
        AuditedAssertions.assertFalse(xmls.isEmpty());long cases=0,failures=0,skips=0;
        List<ReportObservation> inventory=new ArrayList<>();
        for(Path xml:xmls){var suite=parse(xml).getDocumentElement();cases+=number(suite,"tests");failures+=number(suite,"failures")+number(suite,"errors");skips+=number(suite,"skipped");
            var fields=new ArrayList<ReportField>();fields.add(new ReportField("Tests / failures / errors / skipped",suite.getAttribute("tests")+" / "+suite.getAttribute("failures")+" / "+suite.getAttribute("errors")
                +" / "+suite.getAttribute("skipped")));
            var nodes=suite.getElementsByTagName("testcase");for(int i=0;i<nodes.getLength();i++){var t=(org.w3c.dom.Element)nodes.item(i);fields.add(new ReportField(t.getAttribute("name"),t.getElementsByTagName("failure")
                .getLength()+t.getElementsByTagName("error").getLength()>0?"FAIL":t.getElementsByTagName("skipped").getLength()>0?"SKIP":"PASS"));}
            inventory.add(new ReportObservation(suite.getAttribute("name"),fields,List.of(),List.of(artifact(xml))));
        }
        List<Path> flows=new ArrayList<>();
        flows.add(run.resolve("original-instructions-final/ui-flow-original-instructions-review.properties"));
        Path finalMenu=run.resolve("android-menu-final/ui-flow-romanian-menu-options.properties");
        flows.add(Files.exists(finalMenu)?finalMenu:run.resolve("android-menu/ui-flow-romanian-menu-options.properties"));
        Path pilot=run.resolve("pilot-hard/evidence/android/ui-flow-romanian-expert-complete.properties");if(Files.exists(pilot))flows.add(pilot);
        List<Path> duels=files(run,p->p.getFileName().toString().equals("romanian-match-result.properties")&&p.toString().contains("android-expert-"));AuditedAssertions.assertFalse(duels.isEmpty(),"A complete normal Android duel is required");
        boolean botsWon=false;for(Path result:duels){var p=load(result);AuditedAssertions.assertEquals("PASS",p.getProperty("status"));AuditedAssertions.assertEquals("24",p.getProperty("handsCompleted"));AuditedAssertions.assertEquals("376",p.getProperty("moves"));AuditedAssertions.assertEquals("true",
            p.getProperty("processResume"));AuditedAssertions.assertEquals(expectedSeriesReward,p.getProperty("streakBonus"));
            int[] totals=Arrays.stream(p.getProperty("finalTotals").replace("[","").replace("]","").split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();AuditedAssertions.assertEquals(4,totals.length);botsWon|=Arrays.stream(totals,1,4)
                .max().orElseThrow()>totals[0];
            flows.add(result.resolveSibling("ui-flow-romanian-expert-complete.properties"));AuditedAssertions.assertEquals("PASS",load(result.resolveSibling("offline-audit-result.properties")).getProperty("status"));
        }
        List<Path> images=new ArrayList<>();long assertions=0,failedAssertions=0;
        for(Path manifest:flows){var flow=load(manifest);AuditedAssertions.assertEquals("PASS",flow.getProperty("flow.status"),manifest.toString());for(String k:flow.stringPropertyNames()){if(k.endsWith(".screenshot")&&!flow.getProperty(k).isBlank())
            images.add(manifest.getParent().resolve(flow.getProperty(k)));if(k.endsWith(".passed")){assertions++;if(!Boolean.parseBoolean(flow.getProperty(k)))failedAssertions++;}}}
        Path classic=run.resolve("evidence/components/ai-heldout.properties"),series=run.resolve("series-heldout/evidence/ai-heldout.properties");
        boolean accepted=failures==0&&skips==0&&failedAssertions==0&&"PASS".equals(load(classic).getProperty("status"))&&"PASS".equals(load(series).getProperty("status"));
        Path seriesData=Files.exists(run.resolve("series-component-evidence/series-domain.tsv"))?run.resolve("series-component-evidence"):run.resolve("evidence/components");
        List<String> domain=Files.readAllLines(seriesData.resolve("series-domain.tsv"));AuditedAssertions.assertEquals(5113,domain.size());
        for(String row:domain.subList(1,domain.size())){String[] c=row.split("\t");AuditedAssertions.assertEquals(12,c.length);AuditedAssertions.assertEquals("true",c[11]);AuditedAssertions.assertEquals(c[5],c[6]);AuditedAssertions.assertEquals(c[7],c[8]);AuditedAssertions.assertEquals(c[9],c[10]);}
        Path componentTrace=Files.exists(run.resolve("instructions-components/evidence/representative-game-trace.properties"))?run.resolve("instructions-components/evidence/representative-game-trace.properties"):run.resolve(
            "evidence/components/representative-game-trace.properties");
        Properties detailedGame=load(componentTrace);AuditedAssertions.assertEquals("PASS",detailedGame.getProperty("status"));

        try(var session=GameReportSession.open(report)){
            var scenario=session.startScenario("Whist Lab — reconstructed rules, selected series rewards and adversarial AI acceptance","atf.tests","Android API 28 / offline components / physical binaries recorded below",List.of(
                "romanian-whist","instructions","component-first","AI","base64"));
            try(var phase=scenario.startPhase("SETUP")){
                phase.observation(new ReportObservation("Rule profiles",List.of(new ReportField("Original Instructions",
                    "Read every page, including Romanian/Long Romanian, Oh Hell variants, device controls, ratings and multiplayer."),new ReportField("User-selected rule",(
                    "Five consecutive eligible exact contracts add the selected 5 or 10 points; default 10. Five missed contracts deduct 5. One-card hands do not advance a series; o" + "pposite outcomes break it.")),new ReportField(
                    "Compatibility","Old saves and frozen classic benchmarks have no series. The per-contract exact reward remains 5 + tricks. Original parity uses positive series value 5."),new ReportField("Not implemented",
                    "Network high scores, ratings, lobby/join/chat and multiplayer timeouts are inventoried requirements, not covered implementations.")),List.of(),List.of()));
                for(String name:List.of("library-binaries.json","apk-provenance.json","environment.json","source-hashes.json","baseline-hashes.json","independent-player-frozen-hash.json"))observeFile(value -> {
                    if(Set.of("source-hashes.json","baseline-hashes.json","independent-player-frozen-hash.json").contains(name))GameReportPhase.optional(phase,value);
                    else phase.observation(value);
                },run.resolve(name),"Provenance: "+name);
                observeFile(value -> GameReportPhase.optional(phase, value),run.resolve("pilot-hard/installed-apk-before.txt"),"Historical Hard pilot used a different APK");
                if(Files.exists(run.resolve("source-snapshot.zip")))phase.observation(new ReportObservation("Frozen consumer source and binary snapshot",List.of(),List.of(),List.of(artifact(run.resolve("source-snapshot.zip")))));
                for(Path doc:files(run.resolve("rule-docs"),p->p.toString().endsWith(".md")))observeFile(phase::observation,doc,doc.getFileName().toString());
                phase.complete(AtfReportStatus.PASS);
            }
            try(var phase=scenario.startPhase("TEST BODY")){
                phase.observation(new ReportObservation("Actual accepted execution inventory",List.of(new ReportField("JUnit executions",Long.toString(cases)),new ReportField("Failures / skipped",failures+" / "+skips),new
                    ReportField("UI assertions / failed",assertions+" / "+failedAssertions),new ReportField("Screenshot checkpoints",""+images.size()),new ReportField("Independent complete variant games",
                    "48: all eight local modes, 3–6 seats, Romanian/Long Romanian classic and series 5/10"),new ReportField("Exhaustive series domain",
                    "5,112 legal size/bid/won/prior-counter/reward combinations inside one JUnit method, not 5,112 distinct tests")),List.of(),List.of()));
                for(var observation:inventory)phase.observation(observation);
                observeFile(value -> GameReportPhase.optional(phase, value),run.resolve("coverage/jacocoTestReport.xml"),"Measured game-core coverage");
                for(Path experiment:files(run.resolve("iterations"),p->p.getFileName().toString().startsWith("ai-")&&p.toString().endsWith(".properties")))observeFile(value -> GameReportPhase.optional(phase, value),experiment,
                    "Retained development experiment: "+run.relativize(experiment));
                observeFile(phase::observation,classic,"Frozen classic holdout — every paired game");observeFile(phase::observation,run.resolve("series-development/evidence/ai-development.properties"),
                    "Selected series +"+expectedSeriesReward+" — development");observeFile(phase::observation,series,"Selected series +"+expectedSeriesReward+" — fixed final holdout");
                observeFile(phase::observation,run.resolve("component-summary.json"),"Measured component summary");
                observeFile(phase::observation,seriesData.resolve("series-domain.tsv"),"All 5,112 series combinations — inputs, independent expected and actual values");
                for(int reward:List.of(5,10))for(int size=1;size<=8;size++)try(var group=phase.startAssertionStep("Series component — hand size "+size+", positive reward "+reward)){
                    var comparisons=new ArrayList<atf.reporting.contract.AtfAssertion>();
                    for(String row:domain.subList(1,domain.size())){String[] c=row.split("\t");if(Integer.parseInt(c[0])!=size||Integer.parseInt(c[4])!=reward)continue;
                        String label="bid="+c[1]+", won="+c[2]+", previous="+c[3];
                        comparisons.add(new atf.reporting.contract.AtfAssertion(label+" / base points",c[5],c[6],c[5].equals(c[6])));
                        comparisons.add(new atf.reporting.contract.AtfAssertion(label+" / series points",c[7],c[8],c[7].equals(c[8])));
                        comparisons.add(new atf.reporting.contract.AtfAssertion(label+" / next counter",c[9],c[10],c[9].equals(c[10])));
                    }
                    group.assertions(comparisons);group.complete(AtfReportStatus.PASS);
                }
                for(int value:List.of(5,10))observeFile(phase::observation,seriesData.resolve("series-integration-"+value+".txt"),"Controlled component session with series +"+value+": cards, bids, expected/actual score and resume");
                observeFile(phase::observation,run.resolve("observed-series-payouts.json"),"Series payments actually observed in the normal Android game");
                observeFile(phase::observation,run.resolve("classic-stream-parity.json"),"Classic random-stream compatibility after adding series");
                for(Path result:duels){observeFile(phase::observation,result,"Normal Android game result: "+run.relativize(result));observeFile(phase::observation,result.resolveSibling("offline-audit-result.properties"),
                    "Independent offline reconstruction");observeFile(phase::observation,result.resolveSibling("romanian-rule-coverage.properties"),"Actually observed Android rule branches");}
                var diagnostics=new ArrayList<ReportField>();
                if(Files.isDirectory(run.resolve("original-instructions")))diagnostics.add(new ReportField("Partial instruction attempt",
                    "The first read was incomplete. A later attempt lost foreground to another test process. The final complete read passed; older attempts remain in their evidence folders."));
                if(Files.exists(pilot))diagnostics.add(new ReportField("Hard baseline pilot",
                    "Six hands on the old APK; totals [25,19,11,12]. Human planner led. This is a retained partial match, not a full victory or the accepted new APK."));
                diagnostics.add(new ReportField("Experimental regressions","Retained development experiments preserve their original scores/status. They are not relabeled as passing current functional suites."));
                phase.observation(new ReportObservation("Retained diagnostic boundaries",diagnostics,List.of(),List.of()));
                phase.complete(accepted?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            try(var phase=scenario.startPhase("TEARDOWN")){
                phase.observation(new ReportObservation("Interpretation of the Android duel",List.of(new ReportField("At least one Expert strictly beats the human-seat planner",""+botsWon),new ReportField("Opponent boundary",(
                    "Independent test-only planner receives own cards, public bids/history, remaining hand sizes, cumulative scores and series counters. Opponents' card values and s" + "huffle seed cannot enter its Position.")),new
                    ReportField("Limits",("This is finite adversarial evaluation under hidden information. It proves neither perfect play nor inevitable wins. A coalition's best score is not individual s" +
                    "uperiority. Every completed duel is retained.")),new ReportField("Images",(
                    "Real stable Android frames. A frame may span several automated moves, each reconstructed from the public history. All PNG bytes are embedded inline Base64 and c" + "hecked after publication.")),new ReportField(
                    "Timing","Zero-duration UI action/observation records are uninstrumented placeholders, not measured latency. Correctness acceptance, not a mobile performance benchmark."),new ReportField("Reporting ownership",
                    "Native J0 Extent sessions and UI-flow observations; preview limit remains 240, complete TEXT bodies retain all expected/actual evidence."),new ReportField("Visual audit boundary",
                    "Local HTML browsing was denied earlier by MCP. No workaround. PNGs are reviewed directly; HTML text and exact embedded image bytes are checked automatically.")),List.of(),List.of()));phase.complete(
                    accepted?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            scenario.complete(accepted?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            var component=session.startScenario("Offline classic Romanian — every move through an independent component model","game-core","JVM / no Android execution or fabricated screenshots",List.of("component-flow","24-hands",
                "94-tricks","376-cards","classic-no-series"));
            for(String name:List.of("SETUP","TEST BODY","TEARDOWN"))try(var phase=component.startPhase(name)){
                for(int i=0;i<Integer.parseInt(detailedGame.getProperty("step.count"));i++){String k="step."+i+".";if(!name.equals(detailedGame.getProperty(k+"phase")))continue;
                    phase.observation(new ReportObservation(detailedGame.getProperty(k+"title"),List.of(new ReportField("Rule IDs",detailedGame.getProperty(k+"rules"))),List.of(
                            new ReportObservation.Body("Complete input / legal choices",detailedGame.getProperty(k+"context"),ReportObservation.Format.TEXT),
                            new ReportObservation.Body("Complete independent expected state",detailedGame.getProperty(k+"expected"),ReportObservation.Format.TEXT),
                            new ReportObservation.Body("Complete actual state",detailedGame.getProperty(k+"actual"),ReportObservation.Format.TEXT)),List.of()));
                    String expected=detailedGame.getProperty(k+"expected"),actual=detailedGame.getProperty(k+"actual");AuditedAssertions.assertEquals(expected,actual,"Component trace presentation changed");
                    try(var group=GameReportPhase.withCapturedValues(phase, detailedGame.getProperty(k+"title"))){
                        group.assertions(List.of(new atf.reporting.contract.AtfAssertion(detailedGame.getProperty(k+"rules")+" state",expected,actual,expected.equals(actual))));group.complete(AtfReportStatus.PASS);
                    }
                }phase.complete(AtfReportStatus.PASS);
            }component.complete(AtfReportStatus.PASS);
            for(Path flow:flows)DeepSuiteExtentReportTest.publishUiFlow(session,flow,flow.getParent(),true);
        }
        String html=Files.readString(report);var matches=java.util.regex.Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)").matcher(html);var hashes=new HashSet<String>();var text=new StringBuilder();int end=0,count=0;
        while(matches.find()){hashes.add(hash(Base64.getDecoder().decode(matches.group(1))));count++;text.append(html,end,matches.start()).append("[verified PNG]");end=matches.end();}text.append(html,end,html.length());String
            rendered=text.toString();var textAudit=new GameReportTextAudit(rendered);
        for(Path png:images)AuditedAssertions.assertTrue(hashes.contains(hash(Files.readAllBytes(png))),"Exact screenshot omitted: "+png);
        AuditedAssertions.assertTrue(textAudit.contains(Files.readString(seriesData.resolve("series-domain.tsv"))),"Series component data was truncated");
        for(int i=0;i<Integer.parseInt(detailedGame.getProperty("step.count"));i++)for(String field:List.of("title","rules","context","expected","actual"))AuditedAssertions.assertTrue(textAudit.contains(
            detailedGame.getProperty("step."+i+"."+field)),"Component trace value omitted: "+i+" / "+field);
        for(Path manifest:flows){var flow=load(manifest);for(String key:flow.stringPropertyNames())if(key.endsWith(".title")||key.matches(".*\\.assertion\\.\\d+\\.(name|expected|actual)$")||key.matches(
            ".*\\.action\\.\\d+\\.(operation|target|expected|observed)$")){String value=flow.getProperty(key);if(!value.isBlank())AuditedAssertions.assertTrue(textAudit.contains(value),
            "Truncated report field: "+manifest+" / "+key);}}
        AuditedAssertions.assertTrue(accepted,"Functional or paired AI gates failed; report retained");AuditedAssertions.assertTrue(botsWon,"No normal complete Android duel met the stated measured win target; all results retained");
        var receipt=new Properties();receipt.setProperty("status","PASS");receipt.setProperty("cases",""+cases);receipt.setProperty("assertions",""+assertions);receipt.setProperty("componentComparisons",""+((domain.size()-1)
            *3+Integer.parseInt(detailedGame.getProperty("step.count"))));receipt.setProperty("totalComparisons",""+(assertions+(domain.size()-1)*3+Integer.parseInt(detailedGame.getProperty("step.count"))));receipt.setProperty(
            "screenshotCheckpoints",""+images.size());receipt.setProperty("inlinePngUris",""+count);receipt.setProperty("uniqueDecodedPngHashes",""+hashes.size());receipt.setProperty("botsStrictlyBeatPlanner",""+botsWon);
            receipt.setProperty("report.bytes",""+Files.size(report));receipt.setProperty("report.sha256",hash(Files.readAllBytes(report)));try(var out=Files.newOutputStream(proof)){receipt.store(out,
            "Native J0 Extent; complete text and exact Base64 PNG bytes verified");}
    }
    private static List<Path> files(Path root,java.util.function.Predicate<Path> filter)throws Exception{if(!Files.isDirectory(root))return List.of();try(var walk=Files.walk(root)){return walk.filter(Files::isRegularFile).filter(
        filter).sorted().toList();}}
    private static Properties load(Path p)throws Exception{var r=new Properties();try(var in=Files.newInputStream(p)){r.load(in);}return r;}
    private static org.w3c.dom.Document parse(Path p)throws Exception{var f=javax.xml.parsers.DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);f.setFeature(
        "http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);return f.newDocumentBuilder().parse(p.toFile());}
    private static long number(org.w3c.dom.Element e,String key){return Long.parseLong(e.getAttribute(key));}
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static ReportObservation.Artifact artifact(Path p){try{return new ReportObservation.Artifact(p.getFileName().toString(),p.toUri(),hash(Files.readAllBytes(p)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static void observeFile(java.util.function.Consumer<ReportObservation> observe,Path p,String title)throws Exception{if(Files.exists(p))observe.accept(new ReportObservation(title,List.of(),List.of(new
        ReportObservation.Body("Complete evidence",Files.readString(p),ReportObservation.Format.TEXT)),List.of(artifact(p))));}
}

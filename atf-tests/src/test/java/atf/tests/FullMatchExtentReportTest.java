package atf.tests;

import atf.testkit.AtfComponentTest;

import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportTextAudit;
import atf.tests.reporting.GameReportPhase;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Publishes the same ATF UI contract used by the aggregate report; no separate HTML renderer. */
@AtfComponentTest
class FullMatchExtentReportTest {
    @Test void publishesEveryOriginalAndParallelMatchCheckpointInline()throws Exception {
        assumeTrue(Boolean.getBoolean("atf.match.publish"));
        Path evidence=Path.of(System.getProperty("atf.match.evidenceDir"));
        Path report=Path.of(System.getProperty("atf.match.reportPath"));
        Properties recorded=load(evidence.resolve("original-match.properties"));
        Properties live=load(evidence.resolve("ui-flow-match-original.properties"));
        Path timingFile=evidence.getParent().resolve("environment-timing-note.txt");
        String timingNote=Files.exists(timingFile)?Files.readString(timingFile):"Correctness run; timings are not a performance benchmark.";
        Path packageProof=evidence.getParent().resolve("original-package-proof.json");
        List<Path> manifests=new ArrayList<>(List.of(evidence.resolve("ui-flow-match-original-audit.properties"),
                evidence.resolve("ui-flow-match-whist-lab.properties")));
        Path normalSmoke=evidence.resolve("ui-flow-romanian-expert-smoke.properties");
        if(Files.exists(normalSmoke))manifests.add(normalSmoke);
        List<Path> screenshots=new ArrayList<>();long assertions=0,failed=0,actionRecords=0;boolean allPassed="PASS".equals(live.getProperty("flow.status"));
        List<Path> resultXml;
        try(var files=Files.walk(evidence.getParent().resolve("components"))) {
            resultXml=files.filter(f->f.getFileName().toString().startsWith("TEST-")&&f.toString().endsWith(".xml")).sorted().toList();
        }
        long componentCases=0,componentFailures=0,componentSkipped=0;
        List<ReportObservation> resultDetails=new ArrayList<>();
        for(Path xml:resultXml) {
            var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            var suite=factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
            componentCases+=Long.parseLong(suite.getAttribute("tests"));
            componentFailures+=Long.parseLong(suite.getAttribute("failures"))+Long.parseLong(suite.getAttribute("errors"));
            long skipped=Long.parseLong(suite.getAttribute("skipped"));componentSkipped+=skipped;
            List<ReportField> cases=new ArrayList<>();
            cases.add(new ReportField("Tests / failures / errors / skipped",suite.getAttribute("tests")+" / "+suite.getAttribute("failures")
                    +" / "+suite.getAttribute("errors")+" / "+suite.getAttribute("skipped")));
            var nodes=suite.getElementsByTagName("testcase");
            for(int i=0;i<nodes.getLength();i++) {
                var test=(org.w3c.dom.Element)nodes.item(i);
                String state=test.getElementsByTagName("failure").getLength()+test.getElementsByTagName("error").getLength()>0?"FAIL":
                        test.getElementsByTagName("skipped").getLength()>0?"SKIP":"PASS";
                cases.add(new ReportField(test.getAttribute("name"),state+"; reported elapsed="+test.getAttribute("time")+" s"));
            }
            resultDetails.add(new ReportObservation(suite.getAttribute("name"),cases,List.of(),List.of(artifact(xml))));
        }
        allPassed&=componentFailures==0&&componentSkipped==0;
        Path aiFile=evidence.resolve("romanian-ai-tournament.properties");
        Properties ai=Files.exists(aiFile)?load(aiFile):null;
        if(ai!=null)allPassed&="PASS".equals(ai.getProperty("status"));
        for(Path manifest:manifests) {
            Properties flow=load(manifest);allPassed&=flow.getProperty("flow.status").equals("PASS");
            for(String key:flow.stringPropertyNames()) {
                if(key.endsWith(".screenshot")&&!flow.getProperty(key).isBlank())screenshots.add(evidence.resolve(flow.getProperty(key)));
                if(key.endsWith(".passed")){assertions++;if(!Boolean.parseBoolean(flow.getProperty(key)))failed++;}
                if(key.endsWith(".actionCount"))actionRecords+=Long.parseLong(flow.getProperty(key));
            }
        }
        Files.createDirectories(report.toAbsolutePath().getParent());
        Properties publicationInputs=new Properties();
        publicationInputs.setProperty("checkpoint.screenshots",Integer.toString(screenshots.size()));
        long pngBytes=0;for(Path screenshot:screenshots)pngBytes+=Files.size(screenshot);
        publicationInputs.setProperty("checkpoint.png.bytes",Long.toString(pngBytes));
        publicationInputs.setProperty("rule.assertions",Long.toString(assertions));
        publicationInputs.setProperty("action.records",Long.toString(actionRecords));
        publicationInputs.setProperty("jvm.maxHeap.bytes",Long.toString(Runtime.getRuntime().maxMemory()));
        publicationInputs.setProperty("reporting.codeSource",ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toString());
        try(var stream=Files.newOutputStream(report.resolveSibling("full-match-publication-inputs.properties"))){publicationInputs.store(stream,"Known publication inputs before native renderer is invoked");}
        Path verificationPath=report.resolveSibling("full-match-report-verification.properties");
        if(Files.exists(verificationPath))Files.copy(verificationPath,verificationPath.resolveSibling("full-match-report-verification-previous-"+System.currentTimeMillis()+".properties"));
        Files.writeString(verificationPath,"status=PENDING\n");

        try(var session=GameReportSession.open(report)) {
            var summary=session.startScenario("Full Oh Hell match — original and Whist Lab parity","atf.tests",
                    "Android API 28 / ADB",List.of("full-match","rules","score","original","parallel-app"));
            try(var phase=summary.startPhase("Component contracts, game engine and live execution results")) {
                phase.observation(new ReportObservation("Current result files",List.of(
                        new ReportField("JUnit / Gradle result files",Integer.toString(resultXml.size())),
                        new ReportField("Reported test cases",Long.toString(componentCases)),
                        new ReportField("Failures and errors",Long.toString(componentFailures)),
                        new ReportField("Skipped",Long.toString(componentSkipped)),
                        new ReportField("Original live acquisition",live.getProperty("flow.status")),
                        new ReportField("Continuation provenance",("Retained original and Lab hands are revalidated before live continuation. Retained Lab UI actions are labeled explicitly; no new tap is claimed. Earlier failed " +
                            "logs and traces remain under diagnostics."))),
                        List.of(),resultXml.stream().map(FullMatchExtentReportTest::artifact).toList()));
                for(ReportObservation detail:resultDetails)phase.observation(detail);
                phase.complete(componentFailures>0?AtfReportStatus.FAIL:componentSkipped>0?AtfReportStatus.WARNING:AtfReportStatus.PASS);
            }
            if(ai!=null)try(var phase=summary.startPhase("Romanian Whist — separate component rules and AI acceptance")) {
                phase.observation(new ReportObservation("Separate profile; original and Android replay below use Oh Hell",
                        RuleComponentExtentReportTest.RULES,List.of(),List.of()));
                phase.observation(new ReportObservation("Paired Expert vs Hard evaluation, 3–6 players",List.of(
                        new ReportField("Paired comparisons",ai.getProperty("comparisons")),
                        new ReportField("Expert / Hard total points",ai.getProperty("expertTotal")+" / "+ai.getProperty("baselineTotal")),
                        new ReportField("Expert / Hard exact contracts",ai.getProperty("expertExactContracts")+" / "+ai.getProperty("baselineExactContracts")),
                        new ReportField("Scope","Same seeded deals; hero rotated through all seats; other players Hard. Expert must meet Hard at each table size. This is a bounded sample, not a claim of global optimality.")),
                        List.of(new ReportObservation.Body("Every paired score and seed",Files.readString(aiFile),ReportObservation.Format.TEXT)),List.of(artifact(aiFile))));
                phase.complete("PASS".equals(ai.getProperty("status"))?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            try(var phase=summary.startPhase("Complete match evidence and independent verification")) {
                phase.observation(new ReportObservation("Verification scope",List.of(
                        new ReportField("Original complete",recorded.getProperty("match.complete","false")),
                        new ReportField("Original app's own gameover preference",recorded.getProperty("match.originalGameOver","unverified")),
                        new ReportField("Hands",recorded.getProperty("match.handCount","0")),
                        new ReportField("Progression","10→9→8→7→6→5→4→3→2→1→2→3→4→5→6→7→8→9→10"),
                        new ReportField("Tricks per app",recorded.getProperty("match.tricks","0")),
                        new ReportField("Card plays per app",recorded.getProperty("match.moves","0")),
                        new ReportField("Recorded rule assertions",Long.toString(assertions)),
                        new ReportField("Action and observation records",Long.toString(actionRecords)),
                        new ReportField("Failed assertions",Long.toString(failed)),
                        new ReportField("Inline screenshot checkpoints",Integer.toString(screenshots.size())),
                        new ReportField("Timing interpretation",timingNote),
                        new ReportField("Step timing","OBSERVE records have no per-command timing; 0 ms is an uninstrumented placeholder, not a measured action latency."),
                        new ReportField("Original package proof",Files.exists(packageProof)?Files.readString(packageProof):"See original-package-version.txt in the run folder"),
                        new ReportField("Original observation",
                            "Unmodified Play-installed v4.5/145; Android package backup observes save2.dat. Every observation pauses and resumes the real game; this exercises persistence as well as gameplay."),
                        new ReportField("Independent oracle","Checks card ownership, following led suit, highest trump/led card, next leader, trick counters, dealer rotation, bid restriction and cumulative tricks + exact-bid bonus."),
                        new ReportField("Phase counters",("Original score saves can carry the next index/size beside the completed previous hand. Played size is derived from empty hands, complete history and won counter" +
                            "s. Whist Lab retains the current index; final original gameover is checked separately.")),
                        new ReportField("Parallel app",("Same observed deals, trump, bids and card inputs are replayed in debug mode through the normal Whist Lab engine. Winners and scores are calculated; original out" +
                            "comes are never injected.")),
                        new ReportField("Limits","One complete four-player Normal-AI Oh Hell scenario. Does not establish all modes, every random deal, online multiplayer, or uninterrupted animation timing.")),List.of(),
                        List.of(artifact(evidence.resolve("original-match.properties")),artifact(evidence.resolve("ui-flow-match-original.properties")),artifact(evidence.resolve("rule-coverage.properties")))));
                phase.complete(allPassed&&failed==0?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
            Path labProvenance=evidence.getParent().resolve("lab-apk-provenance.json");
            if(Files.exists(labProvenance))try(var phase=summary.startPhase("Whist Lab APK provenance and subsequent normal-game check")) {
                phase.observation(new ReportObservation("Exact APKs and validation scope",List.of(
                        new ReportField("Scope",("The complete recorded-input replay and the subsequent normal Romanian Expert test have separately recorded APK hashes. The later app change clears debug replay " +
                            "on New game / Play again."))),
                        List.of(new ReportObservation.Body("APK provenance",Files.readString(labProvenance),ReportObservation.Format.JSON)),List.of(artifact(labProvenance))));
                phase.complete(AtfReportStatus.PASS);
            }
            Properties coverage=load(evidence.resolve("rule-coverage.properties"));
            try(var phase=summary.startPhase("Observed rule branch coverage")) {
                phase.observation(new ReportObservation("Real original-match branches, also replayed in Whist Lab",
                        coverage.stringPropertyNames().stream().sorted().map(k->new ReportField(k,coverage.getProperty(k))).toList(),
                        List.of(),List.of()));
                phase.complete(allPassed?AtfReportStatus.PASS:AtfReportStatus.WARNING);
            }
            for(int h=0;h<Integer.parseInt(recorded.getProperty("match.handCount","0"));h++) {
                String k="hand."+h+".";
                try(var phase=summary.startPhase("Hand "+(h+1)+" — "+recorded.getProperty(k+"size")+" cards")) {
                    phase.observation(new ReportObservation("Observed original inputs and cumulative score",List.of(
                            new ReportField("Dealer seat",recorded.getProperty(k+"dealer")),
                            new ReportField("Seat names",recorded.getProperty(k+"names")),
                            new ReportField("Trump suit (0 Spades, 1 Hearts, 2 Clubs, 3 Diamonds)",recorded.getProperty(k+"trump")),
                            new ReportField("Bids by seat",recorded.getProperty(k+"bids")),
                            new ReportField("Tricks won by seat",recorded.getProperty(k+"won")),
                            new ReportField("Prior cumulative totals",recorded.getProperty(k+"priorTotals")),
                            new ReportField("Seat 0 initial hand",recorded.getProperty(k+"cardLabels.0")),
                            new ReportField("Seat 1 initial hand",recorded.getProperty(k+"cardLabels.1")),
                            new ReportField("Seat 2 initial hand",recorded.getProperty(k+"cardLabels.2")),
                            new ReportField("Seat 3 initial hand",recorded.getProperty(k+"cardLabels.3")),
                            new ReportField("Cumulative totals by seat",recorded.getProperty(k+"totals"))),List.of(),List.of()));
                    phase.complete(allPassed?AtfReportStatus.PASS:AtfReportStatus.WARNING);
                }
            }
            try(var phase=summary.startPhase("Visual acceptance observation")) {
                phase.observation(new ReportObservation("Visual evidence and acceptance scope",List.of(
                        new ReportField("Original UI","Legacy raster table and card surfaces; hand cards reach the Android navigation area in the captured 1080×1920 layout."),
                        new ReportField("Method","Screenshots document the actual product. Card ownership, legality, winners and scores are checked from captured game state, not inferred from raster change."),
                        new ReportField("Visual verdict","WARNING: no complete visual specification or automated layout oracle is asserted. Inspect the inline original and Whist Lab checkpoints.")),List.of(),List.of()));
                phase.complete(AtfReportStatus.WARNING);
            }
            summary.complete(allPassed&&failed==0?AtfReportStatus.WARNING:AtfReportStatus.FAIL);
            for(Path manifest:manifests)DeepSuiteExtentReportTest.publishUiFlow(session,manifest,evidence,true);
        }
        String html=Files.readString(report);
        var matcher=java.util.regex.Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)").matcher(html);
        Set<String> inlinePngHashes=new HashSet<>();StringBuilder textOnly=new StringBuilder();int last=0,inlineCount=0;
        while(matcher.find()) {
            inlinePngHashes.add(hash(Base64.getDecoder().decode(matcher.group(1))));inlineCount++;
            textOnly.append(html,last,matcher.start()).append("[verified inline PNG]");last=matcher.end();
        }
        textOnly.append(html,last,html.length());String rendered=textOnly.toString();var textAudit=new GameReportTextAudit(rendered);
        for(Path screenshot:screenshots)assertTrue(inlinePngHashes.contains(hash(Files.readAllBytes(screenshot))),
                "Missing exact inline PNG bytes: "+screenshot);
        for(Path manifest:manifests) {
            Properties flow=load(manifest);assertRendered(textAudit,flow.getProperty("flow.name"));
            for(String key:flow.stringPropertyNames()) {
                if(key.endsWith(".title")||key.matches(".*\\.assertion\\.\\d+\\.(name|expected|actual)$")
                        ||key.matches(".*\\.action\\.\\d+\\.(operation|target|expected|observed)$")) {
                    String value=flow.getProperty(key);if(!value.isBlank())assertRendered(textAudit,value);
                }
            }
        }
        Properties verification=new Properties();verification.setProperty("report.bytes",Long.toString(Files.size(report)));
        verification.setProperty("inline.png.dataUris",Integer.toString(inlineCount));
        verification.setProperty("inline.png.uniqueByteHashes",Integer.toString(inlinePngHashes.size()));
        verification.setProperty("checkpoint.screenshots",Integer.toString(screenshots.size()));
        verification.setProperty("rule.assertions",Long.toString(assertions));
        verification.setProperty("action.records",Long.toString(actionRecords));
        verification.setProperty("rule.failedAssertions",Long.toString(failed));
        assertEquals("true",recorded.getProperty("match.complete"));assertEquals("19",recorded.getProperty("match.handCount"));
        assertEquals("436",recorded.getProperty("match.moves"));assertEquals("109",recorded.getProperty("match.tricks"));
        assertEquals("true",recorded.getProperty("match.originalGameOver"));
        assertTrue(allPassed,"One of the original or parallel live flows did not pass");assertEquals(0,failed);
        assertTrue(componentCases>0,"Current component/live JUnit results are required");
        verification.setProperty("status","PASS");
        verification.setProperty("report.sha256",hashFile(report));
        try(var out=Files.newOutputStream(verificationPath)){verification.store(out,"Exact PNG bytes and complete assertion texts verified");}
    }
    private static Properties load(Path file)throws Exception {Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}return p;}
    private static void assertRendered(GameReportTextAudit audit,String value) {
        assertTrue(audit.contains(value), "Missing complete rendered value: " + value);
    }
    private static String hash(byte[] bytes)throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String hashFile(Path path)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];
        try(var in=Files.newInputStream(path)){int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}
        return HexFormat.of().formatHex(digest.digest());
    }
    private static ReportObservation.Artifact artifact(Path path) {
        try{return new ReportObservation.Artifact(path.getFileName().toString(),path.toUri(),
                HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));}
        catch(Exception e){throw new IllegalStateException("Cannot retain evidence "+path,e);}
    }
}

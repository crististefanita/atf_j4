package atf.tests.publishing;

import atf.testkit.AtfComponentTest;

import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportTextAudit;
import atf.tests.reporting.GameReportPhase;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import atf.reporting.configuration.*;
import atf.reporting.contract.*;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.*;
import atf.reporting.ui.UiExtentReportSession;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Offline learning evidence, composed exclusively through the J0 reporting binary. */
@AtfComponentTest
class WhistCorpusExtentReportTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void publishesEveryCorpusAuditAndDetailedIndependentReplayWithHonestLearningOutcomes()throws Exception{
        assumeTrue(Boolean.getBoolean("atf.corpus.publish"));Path run=Path.of(System.getProperty("atf.corpus.runDir"));
        Path report=run.resolve("corpus-extent-report.html");var index=read(run.resolve("corpus-index.json"));var replay=read(run.resolve("reference-audit.json"));var acceptance=read(run.resolve("acceptance.json"));
        assertEquals("PASS",acceptance.path("functionalStatus").asText());assertEquals("PASS",replay.path("status").asText());

        long checks=0;var images=new ArrayList<byte[]>();
        try(var session=GameReportSession.open(report)){
            var scenario=session.startScenario("Whist — offline corpus integrity and rule replay","atf.tests","JVM + independent Node replay; no new Android execution",List.of("component","corpus","rules","training"));
            try(var phase=scenario.startPhase("SETUP")){
                phase.observation(observation("Information boundary and dataset protocol",List.of(new ReportField("Corpus",acceptance.path("corpusDir").asText()),new ReportField("Complete games / distinct deal sets",index.path(
                    "games").asText()+" / "+index.path("distinctDeals").asText()),new ReportField("Privacy",
                    "Full deals are post-game evidence. Decision observations contain own cards and contemporaneous public facts only; outcomes are separate training labels."),new ReportField("Split",
                    "Development only for tuning and calibration. Heldout excluded from policy summaries and training-example export. A complete deal fingerprint cannot cross partitions."),new ReportField("Dependence",
                    "Same-deal rotations and repeated policy experiments are correlated. Counts are not independent statistical samples."),new ReportField("Android boundary",
                    "The three inline images below are genuine historical V2 Android evidence; they do not certify a new candidate APK."))));
                phase.observation(file(run.resolve("PROTOCOL.json"),"Prespecified selection protocol"));phase.observation(file(run.resolve("acceptance.json"),"Final functional and algorithm dispositions"));phase.complete(
                    AtfReportStatus.PASS);
            }
            try(var phase=scenario.startPhase("TEST BODY")){
                try(var group=phase.startAssertionStep("Every complete retained game — evaluator outcomes")){
                    var rows=new ArrayList<AtfAssertion>();for(var game:index.path("index")){assertEquals("PASS",game.path("status").asText());rows.add(new AtfAssertion(game.path("id").asText()+" / full rules replay","PASS",
                        game.path("status").asText(),true));checks++;}
                    group.assertions(rows);group.complete(AtfReportStatus.PASS);
                }
                var groups=new LinkedHashMap<String,List<AtfAssertion>>();for(var row:replay.path("rows")){assertTrue(row.path("passed").asBoolean());groups.computeIfAbsent(row.path("scope").asText(),k->new ArrayList<>()).add(new
                    AtfAssertion(row.path("name").asText(),text(row.get("expected")),text(row.get("actual")),row.path("passed").asBoolean()));checks++;}
                for(var group:groups.entrySet())try(var step=phase.startAssertionStep("Retained Android game / offline replay / "+group.getKey())){
                    step.assertions(group.getValue());step.complete(AtfReportStatus.PASS);
                }
                phase.observation(file(run.resolve("reference-game.json"),"Complete simple JSON reference game — all bids, deals and plays"));
                GameReportPhase.optional(phase, file(run.resolve("corpus-contract-tests.log"),"Executed Node contracts — redaction and corrupt-data rejection"));
                phase.observation(file(run.resolve("component-summary.json"),"Executed JVM components and measured coverage"));phase.complete(AtfReportStatus.PASS);
            }
            try(var phase=scenario.startPhase("TEARDOWN")){phase.observation(observation("Interpretation",List.of(new ReportField("Functional acceptance",acceptance.path("functionalStatus").asText()),new ReportField(
                "Learning acceptance",acceptance.path("learningStatus").asText()),new ReportField("Promoted candidate",acceptance.path("promotedCandidate").asText()),new ReportField("Limits",
                "Finite paired games under hidden information cannot establish perfect play or inevitable victory. Rejected candidates below retain their original regression status."))));phase.complete(AtfReportStatus.PASS);}
                scenario.complete(AtfReportStatus.PASS);
            for(var trial:acceptance.path("trials")){
                Path result=run.resolve(trial.asText());var data=read(result);boolean pass=data.path("status").asText().equals("PASS");var status=pass?AtfReportStatus.PASS:AtfReportStatus.WARNING;
                var experiment=session.startScenario("Learning experiment / "+data.path("candidate").asText()+" / "+data.path("phase").asText()+" / "+data.path("status").asText(),"AI development",
                    "Same deals, same hero seats, frozen opponents",List.of("experiment",pass?"accepted-comparison":"rejected-regression"));
                try(var phase=experiment.startPhase("TEST BODY")){var fields=new ArrayList<ReportField>();for(var family:data.path("families")){fields.add(new ReportField(family.path("role").asText()+" versus "+family.path(
                    "opponent").asText(),"points "+family.path("beforePoints").asText()+" -> "+family.path("afterPoints").asText()+"; win share "+family.path("beforeWinShare").asText()+" -> "+family.path("afterWinShare").asText()
                    +"; criterion accepted="+family.path("accepted").asText()));}phase.observation(observation("Paired family results; rejected outcomes are diagnostic warnings",fields));phase.observation(file(result,
                    "Every paired score, exact-contract count and win share"));phase.complete(status);}experiment.complete(status);
            }
            var media=session.startScenario("Historical V2 Android visual acceptance observation","ro.whistlab.app","Previously executed V2 APK; replayed as historical evidence",List.of("historical","Android","inline-base64"));
            try(var ui=UiExtentReportSession.adaptScenario(media,ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),"Historical V2 evidence")){
                try(var phase=ui.startPhase("Visual acceptance observation")){for(var frame:acceptance.path("historicalImages")){byte[] bytes=Files.readAllBytes(Path.of(frame.path("file").asText()));images.add(bytes);
                    phase.uiValidationAsMedia(frame.path("title").asText(),"Historical V2 Android frame",Base64.getEncoder().encodeToString(bytes),List.of(),List.of(),AtfReportStatus.PASS);}phase.complete(AtfReportStatus.PASS);}
                    ui.complete(AtfReportStatus.PASS);
            }
        }
        String html=Files.readString(report);var textAudit=new GameReportTextAudit(html);assertTrue(checks>1000);
        for(var row:replay.path("rows"))for(String key:List.of("expected","actual"))assertTrue(textAudit.contains(text(row.get(key))),"Missing full replay value: "+row.path("scope")+" / "+key);
        for(byte[] image:images)assertTrue(html.contains(Base64.getEncoder().encodeToString(image)),"Lost exact historical PNG bytes");
        Properties proof=new Properties();proof.setProperty("status","PASS");proof.setProperty("nativeComparisons",""+checks);proof.setProperty("completeGames",index.path("games").asText());proof.setProperty("inlineHistoricalPngs",
            ""+images.size());proof.setProperty("learningStatus",acceptance.path("learningStatus").asText());proof.setProperty("report.bytes",""+Files.size(report));proof.setProperty("report.sha256",HexFormat.of().formatHex(
            java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(report))));try(var out=Files.newOutputStream(run.resolve("corpus-report-verification.properties"))){proof.store(out,
            "Native J0 rendering; complete replay values and exact inline PNG bytes verified");}
    }
    private static JsonNode read(Path p)throws Exception{return JSON.readTree(Files.readString(p));}
    private static String text(JsonNode node){return node.isTextual()?node.asText():node.toPrettyString();}
    private static ReportObservation observation(String title,List<ReportField> fields){return new ReportObservation(title,fields,List.of(),List.of());}
    private static ReportObservation file(Path p,String title)throws Exception{return new ReportObservation(title,List.of(new ReportField("Evidence",p.getFileName().toString())),List.of(new ReportObservation.Body(
        "Complete evidence",Files.readString(p),ReportObservation.Format.TEXT)),List.of());}
}

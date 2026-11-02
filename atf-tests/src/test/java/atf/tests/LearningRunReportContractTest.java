package atf.tests;

import static org.junit.jupiter.api.Assertions.*;
import atf.tests.learning.*;
import atf.tests.reporting.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

class LearningRunReportContractTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @TempDir Path folder;
    @BeforeEach void retainOwnedArtifacts(TestInfo info)throws Exception{String output=System.getProperty("atf.learning.contractOutput","");
        if(!output.isBlank()){folder=Path.of(output).resolve(info.getTestMethod().orElseThrow().getName());Files.createDirectories(folder);}}
    @Test void standaloneInvocationsPreserveFullTypedContextOutcomeAndTiming()throws Exception{
        var inputs=List.of(input("owner-a","PASS"),input("owner-b","FAIL"));Path html=folder.resolve("standalone.html");var result=LearningRunReport.publish(html,inputs);assertEquals(2,result.invocations());assertEquals(4,
            result.comparisons());assertEquals(26_000_002,result.executionNanos());assertEquals(4,LearningReportHtmlAudit.verify(html,inputs));
    }
    @Test void missingLastRowCrossOwnerFailedVerdictAndTimingMutantsAreRefused()throws Exception{
        var inputs=List.of(input("owner-a","PASS"),input("owner-b","FAIL"));Path html=folder.resolve("mutant-source.html");LearningRunReport.publish(html,inputs);String source=Files.readString(html);
            assertDoesNotThrow(()->LearningReportHtmlAudit.verify(html,inputs));
        for(String mutation:List.of("last-row","owner","verdict","timing","context","actual-cell")){
            var doc=Jsoup.parse(source);var second=doc.select("#test-view .test-list-item > li.test-item").get(1);
            switch(mutation){case "last-row"->second.select("table.helm-validation-table > tbody > tr").last().remove();case "owner"->{var a=doc.select("#test-view .test-list-item > li.test-item").get(0);var
                context=a.select("div[data-atf-node-role=observation]").stream().filter(e->e.select("td").stream().anyMatch(td->td.text().equals("CHECKPOINT OWNER"))).findFirst().orElseThrow();second.appendChild(context);}case
                "verdict"->second.select("table.helm-validation-table > tbody > tr").last().select("> td").last().text("✅");case
                "timing"->second.select("[data-atf-execution-duration-nanos]").attr("data-atf-execution-duration-nanos","0");case "context"->second.select("pre").get(1).text("{\"wrongOwner\":true}");case
                "actual-cell"->second.select("table.helm-validation-table > tbody > tr").last().select("> td").get(2).text("999");}
            assertThrows(IllegalArgumentException.class,()->LearningReportHtmlAudit.verify(doc,inputs),mutation);
        }
    }
    @Test void shortJsonAndLongBoundedCellsRetainTheirFullOwnedComparisonBody()throws Exception{
        var receipt=fixture("structured","PASS");var rows=(ArrayNode)receipt.path("checkpoints").get(0).path("comparisons");var structured=rows.addObject();structured.put("name","Structured sequence");structured.set("expected",
            JSON.valueToTree(List.of(1,2,3)));structured.set("actual",JSON.valueToTree(List.of(1,2,3)));structured.put("passed",true);rows.addObject().put("name","Long text").put("expected","z".repeat(600)).put("actual",
            "z".repeat(600)).put("passed",true);var longJson=rows.addObject();longJson.put("name","Long structured sequence");longJson.set("expected",JSON.valueToTree(Collections.nCopies(12,Map.of("seat",0,"total",-12,"contract",
            3))));longJson.set("actual",longJson.path("expected").deepCopy());longJson.put("passed",true);
        Path file=folder.resolve("structured.json");byte[] bytes=JSON.writeValueAsBytes(receipt);Files.write(file,bytes);var inputs=List.of(new LearningRunReport.Input(file,receipt,LearningRunReport.hash(bytes)));Path
            html=folder.resolve("structured.html");LearningRunReport.publish(html,inputs);assertEquals(5,LearningReportHtmlAudit.verify(html,inputs));
    }
    @Test void skipAndErrorKeepTheirOriginalOutcomeAndEmptySelectionsCannotPass()throws Exception{
        var inputs=List.of(input("active","PASS"),input("skip","SKIP"),input("error","ERROR"));Path html=folder.resolve("outcomes.html");LearningRunReport.publish(html,inputs);assertEquals(2,LearningReportHtmlAudit.verify(html,
            inputs));assertEquals("FAIL",LearningRunReport.capturedSummary(inputs).capturedStatus());assertEquals(2,LearningRunReport.capturedSummary(inputs).comparisons());
        assertThrows(IllegalArgumentException.class,()->LearningRunReport.publish(folder.resolve("empty.html"),List.of()));assertThrows(IllegalArgumentException.class,()->LearningRunReport.publish(folder.resolve("all-skip.html"),
            List.of(input("only-skip","SKIP"))));
    }
    @Test void zeroDetailContradictoryPassAndChangedClosedSourceAreRefused()throws Exception{
        var pass=fixture("nominal","PASS");((ArrayNode)pass.path("checkpoints")).removeAll();assertThrows(IllegalArgumentException.class,()->LearningRunReport.validate(pass));
        var contradiction=fixture("contradiction","FAIL");contradiction.put("status","PASS");assertThrows(IllegalArgumentException.class,()->LearningRunReport.validate(contradiction));
        var missingParameters=fixture("missing-parameters","PASS");missingParameters.remove("parameters");assertThrows(IllegalArgumentException.class,()->LearningRunReport.validate(missingParameters));var
            unknownSchema=fixture("unknown-schema","PASS");unknownSchema.put("schemaVersion",99);assertThrows(IllegalArgumentException.class,()->LearningRunReport.validate(unknownSchema));
        var source=input("immutable","PASS");Files.writeString(source.source(),"{}");assertThrows(IllegalArgumentException.class,()->LearningRunReport.publish(folder.resolve("changed.html"),List.of(source)));
    }
    @Test void localInterningPreservesDifferentLongValuesWithoutUsingTheirVerdict(){
        var cp=JSON.createObjectNode();cp.putObject("context").put("scores","a".repeat(500));var rows=cp.putArray("comparisons");rows.addObject().put("name","Different after preview").put("expected","a".repeat(500)).put("actual",
            "a".repeat(499)+"b").put("passed",false);rows.addObject().put("name","Same payload").put("expected","a".repeat(500)).put("actual","a".repeat(500)).put("passed",true);
        var encoded=InlineComparisonValues.encode(cp);assertEquals(1,encoded.path("values").size());assertEquals(rows,InlineComparisonValues.decode(encoded,cp.path("context")));
            ((ObjectNode)encoded.path("rows").get(0).path("actual")).put("valueRef","missing");assertThrows(IllegalArgumentException.class,()->InlineComparisonValues.decode(encoded,cp.path("context")));
    }
    @Test void missingSelectedReceiptCannotBeTurnedIntoASmallerAcceptedSuite()throws Exception{
        var inputs=List.of(input("selected-a","PASS"),input("selected-b","PASS"));var manifest=JSON.createObjectNode();manifest.put("schemaVersion",1);manifest.put("capturedGameplayInvocations",2);manifest.put(
            "capturedQualityInvocations",0);var
            entries=manifest.putArray("receipts");for(var input:inputs)entries.addObject().put("file",input.source().getFileName().toString()).put("sha256",input.sha256()).put("invocationId",
            input.receipt().path("invocationId").asText());Path file=folder.resolve("selection.json");Files.writeString(file,manifest.toString());assertEquals(2,LearningRunReport.load(folder,file).size());entries.remove(1);
            Files.writeString(file,manifest.toString());assertThrows(IllegalArgumentException.class,()->LearningRunReport.load(folder,file));
    }
    @ParameterizedTest @EnumSource(EvidencePolicy.DetailLevel.class)
    void everyModePreservesChunkedCheckpointAndIndependentLastFailure(EvidencePolicy.DetailLevel mode)throws Exception{
        var receipt=fixture("segmented-"+mode,"FAIL");
        var checkpoint=(ObjectNode)receipt.path("checkpoints").get(0);
        var array=((ObjectNode)checkpoint.path("context")).putArray("orderedScores");for(int i=0;i<1001;i++)array.add(-i);
        var rows=(ArrayNode)checkpoint.path("comparisons");
        for(int i=0;i<19;i++)rows.addObject().put("name","Retained row "+i).put("expected",i).put("actual",i).put("passed",true);
        rows.addObject().put("name","Independent final mismatch").put("expected","x".repeat(600)+"41")
                .put("actual","x".repeat(600)+"42").put("passed",false);
        byte[] bytes=JSON.writeValueAsBytes(receipt);Path source=folder.resolve("segmented-"+mode+".json");Files.write(source,bytes);
        var inputs=List.of(new LearningRunReport.Input(source,receipt,LearningRunReport.hash(bytes)));
        var configuration=new GameReportConfiguration(new GameReportPolicy(mode,5,20,4096,10,10_000));
        Path report=folder.resolve("segmented-"+mode+".html");var result=LearningRunReport.publish(report,inputs,configuration);
        assertEquals(22,result.comparisons());assertEquals("FAIL",result.capturedStatus());
        assertEquals(22,LearningReportHtmlAudit.verify(report,inputs));
        var doc=Jsoup.parse(Files.readString(report));var scenario=doc.select("#test-view .test-list-item > li.test-item").first();
        assertEquals(5,scenario.select("table.helm-validation-table").size());
        assertTrue(scenario.select("table.helm-validation-table").stream().allMatch(t->t.select("tbody > tr").size()<=5));
        var context=scenario.select("div[data-atf-node-role=observation]").stream()
                .filter(e->e.select("td").stream().anyMatch(td->td.text().equals("CHECKPOINT OWNER"))).findFirst().orElseThrow();
        var parts=context.select("pre");assertTrue(parts.size()>2);
        parts.first().remove();assertThrows(IllegalArgumentException.class,()->LearningReportHtmlAudit.verify(doc,inputs));
    }
    private LearningRunReport.Input input(String id,String status)throws Exception{var receipt=fixture(id,status);Path file=folder.resolve(id+".json");byte[] data=JSON.writeValueAsBytes(receipt);Files.write(file,data);return new
        LearningRunReport.Input(file,receipt,LearningRunReport.hash(data));}
    private static ObjectNode fixture(String id,String status){
        var r=JSON.createObjectNode();r.put("schemaVersion",1);r.put("runId","controlled-report-contract");r.put("invocationId",id);r.put("className","controlled.Component");r.put("methodName","sameName");
            r.putObject("parameters").put("caseId",id);r.put("startedAt","2026-11-02T17:00:00Z");r.put("endedAt","2026-11-02T17:00:00.013Z");r.put("durationNanos",13_000_001);r.put("status",status);
            if(!status.equals("PASS"))r.put("failure","CONTROLLED "+status+"; no business/device failure claimed");var cps=r.putArray("checkpoints");
        if(status.equals("PASS")||status.equals("FAIL")){var cp=cps.addObject();cp.put("owner",id+"/checkpoint-1");cp.put("title","Controlled component state");cp.putObject("context").put("seed","9223372036854775807").put("literal",
            "first\r\nlast & &#13; <x>");var assertions=cp.putArray("comparisons");assertions.addObject().put("name","Exact integer text").put("expected","9223372036854775807").put("actual","9223372036854775807").put("passed",true);
            assertions.addObject().put("name","Final comparison").put("expected",41).put("actual",status.equals("PASS")?41:42).put("passed",status.equals("PASS"));}return r;
    }
}

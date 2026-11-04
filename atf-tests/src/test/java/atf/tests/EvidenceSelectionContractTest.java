package atf.tests;

import atf.testkit.AtfComponentTest;

import atf.tests.learning.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class EvidenceSelectionContractTest {
    @TempDir Path folder;private static final ObjectMapper JSON=new ObjectMapper();
    @org.junit.jupiter.api.BeforeEach void retain(org.junit.jupiter.api.TestInfo info)throws Exception {String out=System.getProperty("atf.learning.contractOutput","");if(!out.isBlank()){folder=Path.of(out,info.getTestMethod().orElseThrow().getName());Files.createDirectories(folder);}}
    private EvidencePolicy policy(EvidencePolicy.DetailLevel level,int invocations,int rows,int context,int optional){return new EvidencePolicy(level,invocations,rows,context,1,optional);}
    private LearningRunReport.Input input(String id,String status,String role)throws Exception{
        var r=JSON.createObjectNode();r.put("schemaVersion",1).put("runId","selection-contract").put("invocationId",id).put("className","controlled.Selection").put("methodName","literalComparison").put("startedAt","2026-11-02T19:00:00Z").put("endedAt","2026-11-02T19:00:00.001Z").put("durationNanos",1000000).put("status",status);r.putObject("parameters").put("role",role).put("opponent","frozen");if(!status.equals("PASS"))r.put("failure","CONTROLLED "+status);r.putObject("sourceHashes").put("policy","a".repeat(64));var cp=r.putArray("checkpoints").addObject();cp.put("owner",id+"/row").put("title","Literal independent integer comparison");cp.putObject("context").put("input",41);cp.putArray("comparisons").addObject().put("name","Integer result").put("expected",41).put("actual",status.equals("FAIL")?42:41).put("passed",!status.equals("FAIL"));var bytes=JSON.writeValueAsBytes(r);var file=folder.resolve(id+".json");Files.write(file,bytes);return new LearningRunReport.Input(file,r,LearningRunReport.hash(bytes));
    }
    @Test void autoThresholdsAreInclusiveAtMinusExactAndPlusOne()throws Exception{
        var all=List.of(input("a","PASS","expert"),input("b","PASS","expert"),input("c","PASS","expert"));
        for(int count:List.of(1,2,3)){var selected=EvidenceSelection.choose(all.subList(0,count),policy(EvidencePolicy.DetailLevel.AUTO,2,99,9999,100));assertEquals(count<=2?count:1,selected.inputs().size());}
        for(int limit:List.of(2,3,4)){var selected=EvidenceSelection.choose(all,policy(EvidencePolicy.DetailLevel.AUTO,99,limit,9999,100));assertEquals(limit<3?1:3,selected.inputs().size());}
        int length=all.get(0).receipt().path("checkpoints").get(0).path("context").toString().length()*3;
        for(int limit:List.of(length-1,length,length+1))assertEquals(limit<length?1:3,EvidenceSelection.choose(all,policy(EvidencePolicy.DetailLevel.AUTO,99,99,limit,100)).inputs().size());
    }
    @Test void summaryRetainsAllOutcomesAndGateWithoutCherryPickingPassingScores()throws Exception{
        var gate=input("z-gate","PASS","gate");var r=(ObjectNode)gate.receipt();r.put("executionKind","NODE_COMPONENT_GATE");Files.write(gate.source(),JSON.writeValueAsBytes(r));gate=new LearningRunReport.Input(gate.source(),r,LearningRunReport.hash(Files.readAllBytes(gate.source())));
        var population=List.of(input("b","PASS","expert"),input("a","PASS","expert"),input("failure","FAIL","expert"),input("error","ERROR","expert"),input("skip","SKIP","expert"),gate);
        var selection=EvidenceSelection.choose(population,policy(EvidencePolicy.DetailLevel.SUMMARY,99,99,9999,100));assertEquals(List.of("a","failure","error","skip","z-gate"),selection.inputs().stream().map(i->i.receipt().path("invocationId").asText()).toList());assertEquals(1,selection.scope().get("omittedInvocations"));assertEquals(List.of("b"),selection.scope().get("omittedIds"));assertTrue(selection.scope().get("effectiveScope").toString().contains("NOT individually audited"));
    }
    @Test void fullKeepsEveryCheckpointAndOriginalValuesEvenBeyondAdaptiveThresholds()throws Exception{
        var population=List.of(input("a","PASS","expert"),input("b","FAIL","expert"));var selection=EvidenceSelection.choose(population,policy(EvidencePolicy.DetailLevel.FULL,1,1,1,1));assertEquals(2,selection.inputs().size());assertEquals(population.get(1).receipt(),selection.inputs().get(1).receipt());Path html=folder.resolve("full.html");LearningRunReport.publish(html,selection);assertEquals(2,LearningReportHtmlAudit.verify(html,selection.inputs()));LearningReportHtmlAudit.verifyScope(html,selection);
        var doc=org.jsoup.Jsoup.parse(Files.readString(html));doc.select("pre").stream().filter(e->e.wholeText().contains("\"effectiveScope\"")).findFirst().orElseThrow().text("{}");Path mutant=folder.resolve("scope-mutant.html");Files.writeString(mutant,doc.outerHtml());assertThrows(IllegalArgumentException.class,()->LearningReportHtmlAudit.verifyScope(mutant,selection));
    }
    @Test void optionalBudgetKeepsWholeDeduplicatedRecordsAtBoundary()throws Exception{
        var population=List.of(input("a","PASS","expert"),input("b","PASS","player"));int size=population.get(0).receipt().path("sourceHashes").toString().length();
        for(int limit:List.of(size-1,size,size+1)){var selection=EvidenceSelection.choose(population,policy(EvidencePolicy.DetailLevel.FULL,99,99,9999,limit));assertEquals((long)size,selection.scope().get("optionalDiagnosticsOriginalCharacters"));assertEquals(limit<size?0L:(long)size,selection.scope().get("optionalDiagnosticsRetainedCharacters"));assertEquals(2,selection.inputs().size());}
    }
    @Test void strictYamlRejectsZeroUnknownEnumsWrongTypesOverflowAndUnknownKeys()throws Exception{
        String good="detailLevel: AUTO\nmaxDetailedInvocations: 2\nmaxDetailedComparisons: 3\nmaxDetailedContextCharacters: 40\npassingExamplesPerFamily: 1\noptionalContextBudgetCharacters: 100\n";Path file=folder.resolve("policy.yml");Files.writeString(file,good);assertEquals(EvidencePolicy.DetailLevel.AUTO,EvidencePolicyYaml.load(file).detailLevel());
        for(String wrong:List.of(good.replace("AUTO","auto"),good.replace("Invocations: 2","Invocations: 0"),good.replace("Invocations: 2","Invocations: -1"),good.replace("Invocations: 2","Invocations: 2147483648"),good.replace("Invocations: 2","Invocations: true"),good+"unknown: 1\n",good+"detailLevel: FULL\n",good+"---\ndetailLevel: FULL\n")){Files.writeString(file,wrong);assertThrows(Exception.class,()->EvidencePolicyYaml.load(file));}
    }
    @Test void capturedPolicyAndReceiptCannotBeChangedByLaterYamlOrCallerMutation()throws Exception{
        Path file=folder.resolve("policy.yml");Files.writeString(file,"detailLevel: FULL\nmaxDetailedInvocations: 2\nmaxDetailedComparisons: 3\nmaxDetailedContextCharacters: 40\npassingExamplesPerFamily: 1\noptionalContextBudgetCharacters: 100\n");var captured=EvidencePolicyYaml.load(file);Files.writeString(file,"broken: true");assertEquals(EvidencePolicy.DetailLevel.FULL,captured.detailLevel());var input=input("a","PASS","expert");((ObjectNode)input.receipt()).put("status","FAIL");assertEquals("PASS",input.receipt().path("status").asText());assertThrows(UnsupportedOperationException.class,()->EvidenceSelection.choose(List.of(input),captured).inputs().clear());
    }
    @Test void tooSmallConfiguredBodyRefusesPublicationBeforeCreatingAnApparentlyCompleteReport()throws Exception{
        var selected=EvidenceSelection.choose(List.of(input("a","PASS","expert")),policy(EvidencePolicy.DetailLevel.FULL,99,99,9999,100));Path config=folder.resolve("atf.yml"),html=folder.resolve("incomplete.html");Files.writeString(config,"reporting:\n  assertionMaxDisplayCharacters: 240\n  bodyMaxDisplayCharacters: 10\n");String previous=System.getProperty("atf.report.config");try{System.setProperty("atf.report.config",config.toString());var error=assertThrows(IllegalArgumentException.class,()->LearningRunReport.publish(html,selected));assertTrue(error.getMessage().contains("INCOMPLETE"));assertFalse(Files.exists(html));}finally{if(previous==null)System.clearProperty("atf.report.config");else System.setProperty("atf.report.config",previous);}
    }
    @Test void configuredRegressionsStayCompleteWhenLearningIsSampled()throws Exception{
        var a=input("learn-a","PASS","expert");var b=input("learn-b","PASS","expert");var c=input("aaa-reg","PASS","expert");var d=input("aab-reg","PASS","expert");var population=new ArrayList<LearningRunReport.Input>();for(var original:List.of(a,b,c,d)){var r=(ObjectNode)original.receipt();if(r.path("invocationId").asText().contains("-reg"))((ObjectNode)r.path("parameters")).put("collection","regression");byte[] bytes=JSON.writeValueAsBytes(r);Files.write(original.source(),bytes);population.add(new LearningRunReport.Input(original.source(),r,LearningRunReport.hash(bytes)));}var selected=EvidenceSelection.choose(population,policy(EvidencePolicy.DetailLevel.SUMMARY,1,1,1,100));assertEquals(List.of("learn-a","aaa-reg","aab-reg"),selected.inputs().stream().map(i->i.receipt().path("invocationId").asText()).toList());assertEquals(List.of("learn-b"),selected.scope().get("omittedIds"));assertThrows(UnsupportedOperationException.class,()->selected.policy().alwaysIncludeCollections().clear());
    }

}

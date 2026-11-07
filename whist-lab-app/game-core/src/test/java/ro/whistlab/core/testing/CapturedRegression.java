package ro.whistlab.core.testing;
import java.util.*;import java.nio.file.*;import java.time.Instant;import com.fasterxml.jackson.databind.ObjectMapper;
/** Captures the actual evaluated assertion, before throwing, with a partial receipt on error. */
public final class CapturedRegression {
 public interface Work {void run(CapturedRegression capture)throws Exception;}
 final List<Map<String,Object>> rows=new ArrayList<>();
 public void equal(String name,Object expected,Object actual){
  try{atf.gradle.audit.assertions.GradleAssertions.assertEquals(name,expected,actual);retain(name,expected,actual,true);}
  catch(AssertionError failure){retain(name,expected,actual,false);throw failure;}
 }
 private void retain(String name,Object expected,Object actual,boolean passed){
  if(System.getProperty("whist.componentEvidence","").isBlank())return;
  var row=new LinkedHashMap<String,Object>();row.put("name",name);row.put("expected",expected);row.put("actual",actual);row.put("passed",passed);rows.add(row);
 }
 public static void run(String id,String owner,String method,Map<String,Object> context,Work work)throws Exception{
  long begin=System.nanoTime();String start=Instant.now().toString(),status="PASS",failure="";var capture=new CapturedRegression();
  atf.gradle.audit.GradleInputs.fixture("Regression inputs before execution","case",id,"owner",owner,"method",method,"context",context);
  try{work.run(capture);}catch(AssertionError error){status="FAIL";failure=error.toString();throw error;}catch(Error error){status="ERROR";failure=error.toString();throw error;}catch(Exception error){status="ERROR";failure=error.toString();throw error;}
  finally{atf.gradle.audit.GradleInputs.note("Evaluated regression context",atf.gradle.audit.input.CapturedValue.text(context));String output=System.getProperty("whist.componentEvidence","");if(!output.isBlank()){var r=new LinkedHashMap<String,Object>();r.put("schemaVersion",1);r.put("runId","offline-component-regressions");r.put("invocationId",id);r.put("className",owner);r.put("methodName",method);r.put("parameters",Map.of("caseId",id,"collection",System.getProperty("whist.componentCollection","regression")));r.put("startedAt",start);r.put("endedAt",Instant.now().toString());r.put("durationNanos",System.nanoTime()-begin);r.put("status",status);r.put("failure",failure);r.put("checkpoints",List.of(Map.of("owner",id+"/decision","title",id+" — "+context.getOrDefault("checkpointTitle","independent component comparisons"),"context",context,"comparisons",capture.rows)));r.put("results",List.of());Path folder=Path.of(output,"regression-receipts","invocations");Files.createDirectories(folder);Path file=folder.resolve(id+".json");Files.writeString(file,new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(r)+"\n",StandardOpenOption.CREATE_NEW);}}
 }
}

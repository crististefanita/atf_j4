package atf.tests.learning;

import atf.tests.reporting.GameReportConfiguration;
import atf.tests.reporting.GameReportSession;
import atf.tests.reporting.GameReportPhase;

import atf.reporting.configuration.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.*;
import atf.reporting.model.timing.ReportPhaseTiming;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** J4 composition root: projects captured invocations; never calls a game engine or reevaluates a rule. */
public final class LearningRunReport {
    private static final ObjectMapper JSON=new ObjectMapper();
    public record Input(Path source,JsonNode receipt,String sha256) {
        public Input { source=Objects.requireNonNull(source).toAbsolutePath().normalize();receipt=Objects.requireNonNull(receipt).deepCopy();if(!sha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException(
            "Invalid captured source digest"); }
        @Override public JsonNode receipt(){return receipt.deepCopy();}
    }
    public record Result(int invocations,long comparisons,long executionNanos,String capturedStatus) { }
    private LearningRunReport(){};
    public static List<Input> load(Path folder,Path manifest)throws Exception{
        var data=JSON.readTree(Files.readString(manifest));if(data==null||!data.isObject()||data.path("schemaVersion").asInt()!=1||!data.path("receipts").isArray())throw new IllegalArgumentException("Invalid selection manifest");
            var inputs=new ArrayList<Input>();var ids=new HashSet<String>();Path base=folder.toAbsolutePath().normalize();
        for(var entry:data.path("receipts")){
            Path file=base.resolve(entry.path("file").asText()).normalize();if(!file.startsWith(base))throw new IllegalArgumentException("Receipt path outside run");
            byte[] bytes=Files.readAllBytes(file);String digest=hash(bytes);if(!digest.equals(entry.path("sha256").asText()))throw new IllegalArgumentException("Captured input changed: "+entry.path("file").asText());
            var r=JSON.readTree(bytes);validate(r);if(!entry.path("invocationId").asText().equals(r.path("invocationId").asText())||!ids.add(r.path("invocationId").asText()))throw new IllegalArgumentException(
                "Duplicate or reassigned invocation");inputs.add(new Input(file,r,digest));
        }
        int declared;if(data.has("capturedInvocations")){if(!data.path("capturedInvocations").isIntegralNumber()||!data.path("capturedInvocations").canConvertToInt())throw new IllegalArgumentException("Invalid captured count");
            declared=data.path("capturedInvocations").intValue();}else declared=Math.addExact(data.path("capturedGameplayInvocations").asInt(-1),data.path("capturedQualityInvocations").asInt(-1));
        if(declared!=inputs.size())throw new IllegalArgumentException("Selected invocation inventory changed");
        if(inputs.isEmpty()||inputs.stream().allMatch(i->i.receipt.path("status").asText().equals("SKIP")))throw new IllegalArgumentException("Empty/all-SKIP execution cannot be accepted");return List.copyOf(inputs);
    }
    public static void validate(JsonNode r){
        if(!r.path("schemaVersion").isIntegralNumber()||r.path("schemaVersion").asInt()!=1||!r.path("parameters").isObject())throw new IllegalArgumentException("Unsupported receipt schema or missing invocation parameters");
        for(String key:List.of("runId","invocationId","className","methodName","startedAt","endedAt","status"))if(!r.path(key).isTextual()||r.path(key).asText().isBlank())throw new IllegalArgumentException("Missing receipt field "+key);
        String status=r.path("status").asText();if(!Set.of("PASS","FAIL","ERROR","SKIP").contains(status))throw new IllegalArgumentException("Unfinished or unknown outcome");
        Instant.parse(r.path("startedAt").asText());Instant.parse(r.path("endedAt").asText());if(!r.path("durationNanos").isIntegralNumber()||!r.path("durationNanos").canConvertToLong()||r.path("durationNanos").asLong()<0)throw new
            IllegalArgumentException("Missing captured duration");
        if(!r.path("checkpoints").isArray())throw new IllegalArgumentException("Missing checkpoint inventory");if(status.equals("PASS")&&r.path("checkpoints").isEmpty())throw new IllegalArgumentException("PASS without evaluated detail");
        var owners=new HashSet<String>();long rows=0;boolean failed=false;
        for(var c:r.path("checkpoints")){if(c.path("owner").asText().isBlank()||!owners.add(c.path("owner").asText())||c.path("title").asText().isBlank()||!c.has("context")||!c.path("comparisons").isArray())throw new
            IllegalArgumentException("Incomplete/repeated checkpoint");for(var a:c.path("comparisons")){if(a.path("name").asText().isBlank()||!a.has("expected")||!a.has("actual")||!a.path("passed").isBoolean())throw new
            IllegalArgumentException("Incomplete evaluated comparison");failed|=!a.path("passed").asBoolean();rows++;}}
        if(status.equals("PASS")&&(rows==0||failed))throw new IllegalArgumentException("Contradictory captured PASS");if((status.equals("ERROR")||status.equals("FAIL"))&&(!r.path("failure").isTextual()||r.path("failure").asText()
            .isBlank()))throw new IllegalArgumentException("Failure cause missing");
    }
    public static Result publish(Path report,List<Input> inputs)throws Exception{
        return publish(report,inputs,GameReportConfiguration.capture());
    }
    public static Result publish(Path report,List<Input> inputs,GameReportConfiguration configuration)throws Exception{
        return publish(report,EvidenceSelection.choose(inputs,configuration.selectionPolicy()),configuration);
    }
    public static Result publish(Path report,EvidenceSelection selection)throws Exception{
        return publish(report,selection,GameReportConfiguration.from(selection.policy(),AtfReportConfigurationLoader.load().reportingSettings()));
    }
    private static Result publish(Path report,EvidenceSelection selection,GameReportConfiguration configuration)throws Exception{
        if(!selection.policy().equals(configuration.selectionPolicy()))throw new IllegalArgumentException("Selection and presentation policies differ");
        List<Input> inputs=selection.inputs();
        if(inputs.isEmpty()||inputs.stream().allMatch(i->i.receipt.path("status").asText().equals("SKIP")))throw new IllegalArgumentException("No active selected invocations");if(Files.exists(report))throw new
            IllegalArgumentException("Choose a fresh report path");var identities=new HashSet<String>();for(var i:inputs){validate(i.receipt);if(!identities.add(i.receipt.path("invocationId").asText())||!hash(Files.readAllBytes(
            i.source)).equals(i.sha256))throw new IllegalArgumentException("Changed or repeated source invocation");}var settings=configuration.settings();
        if(settings.assertionMaxDisplayCharacters()!=240||settings.bodyMaxDisplayCharacters()<=0)throw new IllegalArgumentException("This consumer requires preview 240 and a positive configured body budget");
        requireBody(JSON.valueToTree(selection.scope()).toPrettyString(),settings);
        for(var input:inputs)requireBody(text(input.receipt.path("parameters")),settings);
        long rows=0,nanos=0;AtfReportStatus aggregate=AtfReportStatus.PASS;
        Path binary=Path.of(ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toURI());String publicationBinary=binary.getFileName()+" / SHA-256 "+hash(Files.readAllBytes(binary));
        try(var session=GameReportSession.open(report,configuration)){
            session.reserveOptionalContext(new GameReportSession.OptionalContext(
                    ((Number)selection.scope().get("optionalDiagnosticsOriginalRecords")).longValue(),
                    ((Number)selection.scope().get("optionalDiagnosticsRetainedRecords")).longValue(),
                    ((Number)selection.scope().get("optionalDiagnosticsOriginalCharacters")).longValue(),
                    ((Number)selection.scope().get("optionalDiagnosticsRetainedCharacters")).longValue()));
            for(var input:inputs){var r=input.receipt;String outcome=r.path("status").asText();var status=AtfReportStatus.fromDisplayName(outcome);aggregate=AtfReportStatus.worst(aggregate,status);long duration=r.path(
                "durationNanos").asLong();nanos=Math.addExact(nanos,duration);
                String name=readerTitle(r)+" [invocation "+r.path("invocationId").asText()+"]";
                var scenario=session.startScenario(name,r.path("className").asText(),r.path("runId").asText(),List.of("component","offline-learning",outcome.toLowerCase(Locale.ROOT)));
                try(var phase=scenario.startPhase("TEST BODY")){
                    phase.executionTiming(new ReportPhaseTiming(Duration.ofNanos(duration),1));
                    phase.observation(new ReportObservation("Captured invocation identity and execution scope",List.of(new ReportField("Invocation ID",r.path("invocationId").asText()),new ReportField("Original outcome",outcome),new
                        ReportField("Producer",r.path("className").asText()+"#"+r.path("methodName").asText()),new ReportField("Started / ended",r.path("startedAt").asText()+" / "+r.path("endedAt").asText()),new ReportField(
                        "Captured execution nanoseconds",Long.toString(duration)),new ReportField("Scope",r.path("executionKind").asText().equals("NODE_COMPONENT_GATE")
                        ?"Full-population offline quality gate, not a new gameplay execution":"One measured offline component invocation; every included comparison and its full input are inline. No device run is claimed."),new
                        ReportField("Source receipt SHA-256",input.sha256)),List.of(new ReportObservation.Body("Invocation parameters",text(r.path("parameters")),ReportObservation.Format.JSON)),List.of()));
                    if(input==inputs.get(0)) {
                        phase.observation(new ReportObservation("What this report covers",List.of(new ReportField("Reading order",
                            "Read purpose and rule, initial input, native expected/actual/verdict rows, then full context if needed."),new ReportField("Selected / supplied captures / omitted",selection.inputs().size()+" / "+selection.scope()
                            .get("originalInvocations")+" / "+selection.scope().get("omittedInvocations")),new ReportField("Evidence limit",selection.scope().get("effectiveScope").toString())),List.of(new ReportObservation.Body(
                            "Effective policy and complete selection inventory",JSON.valueToTree(selection.scope()).toPrettyString(),ReportObservation.Format.JSON)),List.of()));
                        phase.observation(new ReportObservation("Romanian Whist — reader guide",List.of(new ReportField("Profile",(
                            "Four players; reduced 32-card deck, ranks 7 through Ace. Standard 24-hand schedule; eight-card hands have no trump. Paired tournaments enable five-hand series, " + "+10 exact / -5 missed.")),new
                            ReportField("Terms and indices",("Hand = complete deal; trick = one card per player; bid = declared tricks. Seats 0..3 go clockwise; every score vector is in seat order. SHCD codes: S spades, H " +
                            "hearts, C clubs, D diamonds; NT no trump; A Ace, K King, Q Queen, J Jack.")),new ReportField("Legal play and winner",
                            "Follow led suit; otherwise mandatory trump if available; otherwise discard. No overtrump duty. Highest trump wins; without trump highest led-suit card wins. Winner leads next."),new ReportField(
                            "Bids and score",("Bid 0..hand size; only the last bid may not make total equal size. Exact: 5+won. Miss: -abs(won-bid). Eligible sizes 2..8 advance signed series; fifth exact +10" +
                            ", fifth miss -5, then reset. One-card matching outcomes preserve, opposite outcomes clear.")),new ReportField("Paired comparison design","Same fixed deal is played twice: frozen V4 hero, then candidate hero, against unchanged frozen opponents. Actions may diverge; both games receive independent rule/score audits. Progression is a separate verdict."),new ReportField("Independent expected values",(
                            "Tournament receipts contain integer-oracle comparisons captured during each JUnit invocation, not reconstructed from PASS. Documentation receipts compare explic" +
                            "it literal examples. AI progression gates are statistical results, not a promise of optimal play."))),List.of(),List.of()));
                        phase.observation(new ReportObservation("Technical provenance and presentation settings",List.of(new ReportField("Reporting binary",publicationBinary),new ReportField("Assertion preview / body budget",
                            settings.assertionMaxDisplayCharacters()+" / "+settings.bodyMaxDisplayCharacters()+" UTF-16 characters; mandatory bodies were checked before publication"),new ReportField("Inline references",(
                            "Long values stay in their own checkpoint. contextPointer addresses its full context; valueRef its local values map. Exact payload identity only; PASS never impl" + "ies expected=actual."))),List.of(),
                            List.of()));
                    }
                    for(var checkpoint:r.path("checkpoints")){
                        String owner=checkpoint.path("owner").asText(),title=checkpoint.path("title").asText();
                        var bodies=new ArrayList<ReportObservation.Body>();bodies.add(new ReportObservation.Body("Complete checkpoint input and observed state",text(checkpoint.path("context")),ReportObservation.Format.JSON));var
                            longValues=InlineComparisonValues.encode(checkpoint);if(!longValues.path("rows").isEmpty())bodies.add(new ReportObservation.Body("Complete values for bounded assertion cells — local references",text(
                            longValues),ReportObservation.Format.JSON));
                        phase.observation(new ReportObservation(title+" — captured context",List.of(new ReportField("Checkpoint owner",owner),new ReportField("Purpose and observed inputs",checkpointSummary(checkpoint))),bodies,List.of()));
                        var assertions=new ArrayList<AtfAssertion>();boolean passed=true;for(var a:checkpoint.path("comparisons")){boolean ok=a.path("passed").asBoolean();passed&=ok;assertions.add(new AtfAssertion(a.path("name")
                            .asText(),text(a.path("expected")),text(a.path("actual")),ok));rows++;}
                        try(var step=GameReportPhase.withCapturedValues(phase,owner+" — "+title)){step.assertions(assertions);step.complete(passed?AtfReportStatus.PASS:AtfReportStatus.FAIL);}
                    }
                    if(!outcome.equals("PASS"))phase.observation(new ReportObservation("Captured failure or skipped invocation",List.of(new ReportField("Original outcome",outcome),new ReportField("Cause",r.path("failure").asText(
                        "No further details captured")),new ReportField("Capture completeness",r.path("checkpoints").isEmpty()
                        ?"No comparison details were captured before this outcome":"Only the persisted checkpoints are projected; no missing steps are invented")),List.of(),List.of()));
                    phase.complete(status);
                }scenario.complete(status);
            }
        }return new Result(inputs.size(),rows,nanos,aggregate.name());
    }
    private static void requireBody(String text,ReportingSettings settings){if(text.length()>settings.bodyMaxDisplayCharacters())throw new IllegalArgumentException(
        "INCOMPLETE: required evidence exceeds configured body budget; select semantic cases or raise the explicit positive budget. No body was silently cut.");}
    private static String readerTitle(JsonNode r){var p=r.path("parameters");if(p.path("collection").asText().equals("regression"))return "Contract safety regression / "+p.path("caseId").asText();if(p.has("example"))return "How to play / "+p.path("language").asText()+" / "+p.path("example").asText();if(p.has("role"))return
        "Two complete Romanian games / "+p.path("role").asText()+" vs three "+p.path("opponent").asText()+" / seed "+p.path("seed").asText()+" / seat "+p.path("heroSeat").asText();if(r.path("executionKind").asText().equals(
        "NODE_COMPONENT_GATE"))return "AI progression across all executed paired games — independent quality gate";return r.path("methodName").asText()+" ["+text(p)+"]";}
    private static String checkpointSummary(JsonNode cp){var c=cp.path("context");if(c.has("rule"))return c.path("purpose").asText()+". "+c.path("rule").asText();if(c.has("deal"))return (cp.path("owner").asText().contains("-before/")?"Frozen V4 baseline game. ":cp.path("owner").asText().contains("-after/")?"Candidate game under evaluation. ":"")+"Before: "+c.path("size").asText()
        +" cards per seat; dealer "+c.path("dealer").asText()+"; trump "+c.path("trump").asText()+(
        ". Input: dealt cards and ordered bids/plays below. After: score entries show bid, won, base, series, total and next streak. Comparisons independently verify eve" + "ry card, actor, winner and score.");return cp.path(
        "title").asText()+". Read the stated expected value against the captured actual value; verdicts come from execution, not presentation.";}
    public static String text(JsonNode n){return n.isTextual()?n.asText():n.toPrettyString();}
    public static Result capturedSummary(List<Input> inputs){
        long rows=0,nanos=0;AtfReportStatus status=AtfReportStatus.INFO;for(var input:inputs){var r=input.receipt;validate(r);status=AtfReportStatus.worst(status,AtfReportStatus.fromDisplayName(r.path("status").asText()));
            nanos=Math.addExact(nanos,r.path("durationNanos").asLong());for(var checkpoint:r.path("checkpoints"))rows=Math.addExact(rows,checkpoint.path("comparisons").size());}return new Result(inputs.size(),rows,nanos,status.name());
    }
    public static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
}

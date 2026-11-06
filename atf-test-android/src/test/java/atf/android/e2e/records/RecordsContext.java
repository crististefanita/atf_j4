package atf.android.e2e.records;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.flow.UiFlowCheckpoint;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

/** Device-scoped UI evidence and reversible test data; all destructive paths are fixed app-private directories. */
final class RecordsContext {
    static final String PACKAGE = "ro.whistlab.app";
    final AdbClient adb = AdbClient.fromSystemProperties();
    final Path run = Path.of(System.getProperty("whist.records.runRoot"));
    final Path output;
    final AndroidUiDriver ui;
    final AndroidUiFlowTrace trace;
    final List<Path> evidence = new ArrayList<>();
    final ObjectNode setup;
    final JsonNode originalManifest;
    private final String leaseId;
    private final Path leasePath=Path.of(System.getProperty("whist.records.leasePath","C:/work/whist_back/android-ui-lease.json"));
    private final AndroidUiDriver.Device device;
    private String phase;
    private int checkpoint;
    private int observedData;

    RecordsContext() throws Exception {
        this("profile-score-records","Whist Lab — nume, istoric comun, clasamente locale și serii ±5/±10","records-minimal");
    }
    RecordsContext(String id,String name,String folder) throws Exception {
        output=Path.of(System.getProperty("atf.android.artifacts")).resolve(folder);
        Files.createDirectories(output);
        setup = (ObjectNode)RecordsFixtures.JSON.readTree(run.resolve("setup-install.json").toFile());
        RecordsRunContract.installedApk(setup);
        originalManifest = RecordsFixtures.JSON.readTree(run.resolve("app-data-before.json").toFile());
        RecordsRunContract.manifest(originalManifest);
        leaseId = RecordsFixtures.JSON.readTree(run.resolve("device-lease.json").toFile()).path("leaseId").asText();
        verifyLease();
        String configured=System.getProperty("atf.android.serial",System.getenv("ATF_ANDROID_SERIAL"));
        if(!setup.path("serial").asText().equals(configured))
            throw new IllegalStateException("Configured ADB serial differs from the preserved-data device");
        device=RecordsRunContract.guarded(new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) throws java.io.IOException,InterruptedException {return adb.run(args);}
            public AdbClient.BinaryResult runBinary(String... args) throws java.io.IOException,InterruptedException {return adb.runBinary(args);}
        },leasePath,leaseId,setup.path("serial").asText());
        trace = new AndroidUiFlowTrace(output,id,name,
                "WhistRecordsE2eTest","atf.android.e2e.records",PACKAGE,
                List.of("android","e2e","api-"+setup.path("api").asText(),"records","presentation","controlled-preconditions"));
        ui = new AndroidUiDriver(device,output);
        ui.recordActionsAs(trace::recordAction);
        ui.recordCapturesAs(trace::capture);
        Files.copy(run.resolve("setup-install.json"),output.resolve("installation.json"));
        evidence.add(output.resolve("installation.json"));
    }

    void phase(String name) throws Exception { verifyLease(); phase = name; trace.beginPhase(name); }
    AndroidUiDriver scopedUi(String scope) throws Exception {
        if(!scope.matches("[A-Za-z0-9.-]+")||scope.contains(".."))throw new IllegalArgumentException("Invalid capture scope");
        var scoped=new AndroidUiDriver(device,output.resolve(scope));
        scoped.recordActionsAs(trace::recordAction);scoped.recordCapturesAs(trace::capture);
        return scoped;
    }
    void verifyLease() throws Exception {
        RecordsRunContract.lease(leasePath,leaseId,setup.path("serial").asText());
    }
    String raw(String... args) throws Exception {
        var result = device.run(args); result.requireSuccess(); return result.output().trim();
    }
    void command(String operation, String expected, String... args) throws Exception {
        verifyLease(); long started = System.nanoTime();
        var result = device.run(args);
        ui.recordAction(operation,String.join(" ",args),expected,result.output().trim(),
                Duration.ofNanos(System.nanoTime()-started).toMillis(),result.succeeded()?AtfReportStatus.PASS:AtfReportStatus.FAIL);
        result.requireSuccess();
    }
    String home(String language) throws Exception {
        return home(language,false);
    }
    String home(String language,boolean holdAi) throws Exception {
        return ui.restartToActivity(PACKAGE,".MainActivity","--es","debug_language",language,
                "--ez","debug_hold_ai_turns",Boolean.toString(holdAi),
                "--ez","debug_hold_ai_bids",Boolean.toString(holdAi));
    }
    ObjectNode state() throws Exception {
        return observed("committed-session", readState());
    }
    private ObjectNode readState() throws Exception {
        String xml = raw("shell","run-as",PACKAGE,"cat","shared_prefs/whist_lab_game.xml");
        var document = xml(xml); var strings = document.getElementsByTagName("string");
        for (int index=0; index<strings.getLength(); index++) {
            var item = (org.w3c.dom.Element)strings.item(index);
            if (item.getAttribute("name").equals("current_session_v1"))
                return (ObjectNode)RecordsFixtures.JSON.readTree(item.getTextContent());
        }
        throw new IllegalStateException("No saved session was exposed");
    }
    ObjectNode scores() throws Exception {
        return observed("local-rankings", (ObjectNode)RecordsFixtures.JSON.readTree(raw("shell","run-as",PACKAGE,"cat","files/highestScores.json")));
    }
    private ObjectNode observed(String role, ObjectNode value) throws Exception {
        var context = RecordsFixtures.JSON.createObjectNode().put("role", "Observed Android " + role)
                .put("phase", phase).put("observationOrdinal", ++observedData)
                .put("capturedAtUtc", java.time.Instant.now().toString());
        context.set("value", value);
        observation(String.format("observed-%03d-%s", observedData, role), context);
        return value;
    }
    void observation(String label, JsonNode data) throws Exception {
        Path file=output.resolve(label+".json");Files.writeString(file,data.toPrettyString());evidence.add(file);
    }
    String awaitState(Predicate<ObjectNode> condition, Duration timeout) throws Exception {
        long end = System.nanoTime()+timeout.toNanos(); ObjectNode last=null;
        do { last=readState(); if(condition.test(last))return observed("committed-session", last).toString(); Thread.sleep(200); } while(System.nanoTime()<end);
        observed("last-committed-session-before-timeout",last);
        throw new IllegalStateException("Saved state did not reach expected transition: "+last);
    }
    void sandbox() throws Exception {
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(run.resolve("app-data-before.tar"))));
        if(!hash.equals(setup.path("backupSha256").asText()))throw new IllegalStateException("User-data backup digest changed");
        RecordsRunContract.privateRoot(raw("shell","run-as",PACKAGE,"pwd"));
        command("Stop app before isolated data setup","App stopped","shell","am","force-stop",PACKAGE);
        command("Clear only backed-up app-private test scope","files and shared_prefs reset under verified package root",
                "shell","run-as",PACKAGE,"rm","-rf","files","shared_prefs");
        command("Create isolated app data directories","Directories available","shell","run-as",PACKAGE,"mkdir","-p","files","shared_prefs");
        command("Select compact portrait profile","720x1280 pixels","shell","wm","size","720x1280");
        command("Select compact density","360 dpi","shell","wm","density","360");
    }
    void inject(RecordsFixtures.Fixture fixture, String label) throws Exception {
        command("Stop app before declared fixture setup","No running activity","shell","am","force-stop",PACKAGE);
        Path json=output.resolve(label+"-fixture.json");
        Files.writeString(json,fixture.json().toPrettyString()); evidence.add(json);
        String encoded=fixture.json().toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
        Path prefs=output.resolve(label+"-preferences.xml");
        Files.writeString(prefs,"<?xml version=\"1.0\" encoding=\"utf-8\"?><map><string name=\"current_session_v1\">"+encoded+"</string></map>");
        push(prefs,"shared_prefs/whist_lab_game.xml"); evidence.add(prefs);
    }
    void writeScores(ObjectNode scores, String label) throws Exception {
        command("Stop app before retained ranking precondition","No running activity","shell","am","force-stop",PACKAGE);
        Path source=output.resolve(label+".json"); Files.writeString(source,scores.toPrettyString()); evidence.add(source);
        push(source,"files/highestScores.json");
    }
    private void push(Path source, String target) throws Exception {
        if(!target.matches("(?:files|shared_prefs)/[A-Za-z0-9_.-]+"))throw new IllegalArgumentException("Unscoped fixture path");
        String remote="/data/local/tmp/whist-records-"+leaseId+"-"+source.getFileName();
        command("Transfer declared fixture", "ADB push completed", "push",source.toString(),remote);
        command("Install fixture in isolated app scope",target,"shell","run-as",PACKAGE,"cp",remote,target);
        raw("shell","rm","-f",remote);
    }
    void typeName(String value) throws Exception {
        ui.tapDescription("Your name");
        command("Enter short player name",value,"shell","input","text",value);
    }
    void tap(String label) throws Exception {
        String actual=RecordsRunContract.visibleLabel(() -> ui.texts(ui.dump()),ui::revealText,label);
        ui.tapText(actual);
    }
    String swipe(String className, boolean horizontal, boolean forward) throws Exception {
        int[] bounds=RecordsRunContract.scrollBounds(ui.dump(),className);
        int left=bounds[0],top=bounds[1],right=bounds[2],bottom=bounds[3];
        int x=(left+right)/2,y=(top+bottom)/2;
        int halfStep=Math.max(1,(right-left)/6);
        int fromX=horizontal?(forward?x+halfStep:x-halfStep):x,toX=horizontal?(forward?x-halfStep:x+halfStep):x;
        int fromY=horizontal?y:(forward?bottom-35:top+35),toY=horizontal?y:(forward?top+35:bottom-35);
        command("Swipe visible "+(horizontal?"columns":"rows"),"Reveal the next requested table region","shell","input","swipe",
                ""+fromX,""+fromY,""+toX,""+toY,"450");
        return ui.dump();
    }
    String revealDescription(String description, boolean horizontal) throws Exception {
        return revealDescription(description,horizontal,0);
    }
    String revealDescription(String description, boolean horizontal,int minimumExtent) throws Exception {
        String hierarchy=ui.dump();
        for(int attempt=0;attempt<16&&!RecordsRunContract.visibleDescription(hierarchy,description,horizontal,minimumExtent);attempt++)
            hierarchy=swipe(horizontal?"android.widget.HorizontalScrollView":"android.widget.ScrollView",horizontal,true);
        if(!RecordsRunContract.visibleDescription(hierarchy,description,horizontal,minimumExtent))throw new IllegalStateException("Could not fully reveal "+description);
        return hierarchy;
    }
    void check(String title, AtfAssertion... assertions) throws Exception {
        List<Path> files=ui.capture(String.format("%02d-",++checkpoint)+title.replaceAll("[^A-Za-z0-9]+","-").toLowerCase());
        evidence.addAll(files);
        boolean pass=List.of(assertions).stream().allMatch(AtfAssertion::passed);
        UiFlowCheckpoint.checkpoint(trace,phase,AtfGherkinKeyword.THEN,title,"Observed Whist Lab Android state",files,
                "Observe UI and committed application state","All named independent comparisons match",
                pass?"All recorded comparisons passed":"See failed expected/actual comparison",0,List.of(assertions),pass?AtfReportStatus.PASS:AtfReportStatus.FAIL);
        if(!pass)throw new AssertionError(title+": "+List.of(assertions));
    }
    static AtfAssertion equal(String name, Object expected, Object actual) {
        return new AtfAssertion(name,RecordsRunContract.describe(expected),RecordsRunContract.describe(actual),java.util.Objects.equals(expected,actual));
    }
    static org.w3c.dom.Document xml(String source) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
    }

    void restore() throws Exception {
        phase("TEARDOWN — restore preserved user data and display");
        RecordsRunContract.privateRoot(raw("shell","run-as",PACKAGE,"pwd"));
        RecordsRunContract.manifest(originalManifest);
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(run.resolve("app-data-before.tar"))));
        if(!digest.equals(setup.path("backupSha256").asText()))throw new IllegalStateException("User-data backup digest changed before restoration");
        command("Stop test activity before restoration","App stopped","shell","am","force-stop",PACKAGE);
        String remote="/data/local/tmp/whist-records-"+leaseId+"-restore.tar";
        command("Transfer preserved data archive","Original archive available","push",run.resolve("app-data-before.tar").toString(),remote);
        command("Remove only isolated test data","Only verified app-private scope cleared","shell","run-as",PACKAGE,"rm","-rf","files","shared_prefs");
        command("Restore original files and preferences","Original archive extracted in package private root","shell","run-as",PACKAGE,"tar","-xf",remote,"-C",".");
        raw("shell","rm","-f",remote);
        var checks=new ArrayList<AtfAssertion>();
        for(var item:originalManifest) {
            String file=item.path("path").asText();
            if(!file.matches("(?:files|shared_prefs)/[A-Za-z0-9_./-]+")||file.contains(".."))throw new IllegalStateException("Invalid restoration path");
            String actual=raw("shell","run-as",PACKAGE,"sha256sum",file).split("\\s+")[0];
            checks.add(equal("Original file restored byte for byte: "+file,item.path("sha256").asText(),actual));
        }
        String size=override(setup.path("size").asText(),"size"),density=override(setup.path("density").asText(),"density");
        command("Restore display size",size,"shell","wm","size",size);
        command("Restore display density",density,"shell","wm","density",density);
        String fontScale = setup.path("fontScale").asText();
        command("Restore font scale",fontScale,atf.android.evidence.FontScaleRestoration.command(fontScale));
        var receipt=RecordsFixtures.JSON.createObjectNode().put("restored",checks.stream().allMatch(AtfAssertion::passed));
        receipt.set("checks",RecordsFixtures.JSON.valueToTree(checks));
        Files.writeString(run.resolve("restoration.json"),receipt.toPrettyString());
        home("EN");
        checks.add(equal("Restored app opens with New game",true,ui.hasText(ui.dump(),"New game")));
        check("Original user data restored",checks.toArray(AtfAssertion[]::new));
    }
    private static String override(String text,String property) {
        var match=Pattern.compile("Override "+property+":\\s*(\\S+)").matcher(text);
        return match.find()?match.group(1):"reset";
    }
}

package atf.android.e2e.release;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.flow.UiFlowCheckpoint;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Actual signed-package evidence; match-changing acceptance requires a verified reversible backup. */
public final class ReleaseBidContext {
    public static final String PACKAGE = "ro.whistlab.app";
    final Path output;
    final AndroidUiFlowTrace trace;
    public final AndroidUiDriver ui;
    public final JsonNode setup;
    private final Path leaseFile;
    private final String leaseId;
    private final AdbClient adb = AdbClient.fromSystemProperties();
    private final List<Path> evidence = new ArrayList<>();

    ReleaseBidContext(Path run) throws Exception {
        this(run, "signed-release-bidding", "Signed release — recommended bid, manual priority and saved contract",
                "WhistReleaseBidE2eTest");
    }

    ReleaseBidContext(Path run, String id, String title, String testClass) throws Exception {
        var json = new ObjectMapper();
        setup = json.readTree(run.resolve("setup-release.json").toFile());
        leaseId = json.readTree(run.resolve("device-lease.json").toFile()).required("leaseId").asText();
        leaseFile = Path.of(System.getProperty("whist.records.leasePath", "C:/work/whist_back/android-ui-lease.json"));
        String configured = System.getProperty("atf.android.serial", System.getenv("ATF_ANDROID_SERIAL"));
        if (!setup.required("serial").asText().equals(configured)) throw new IllegalStateException("Different configured Android serial");
        ReleaseBackupContract.verify(run, setup);
        verifyLease();
        output = Path.of(System.getProperty("atf.android.artifacts")).resolve(id);
        Files.createDirectories(output);
        trace = new AndroidUiFlowTrace(output, id, title, testClass, "atf.android.e2e.release", PACKAGE,
                List.of("e2e", "android", "signed-release"));
        var device = new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) throws java.io.IOException, InterruptedException {
                verifyLease(); return adb.run(args);
            }
            public AdbClient.BinaryResult runBinary(String... args) throws java.io.IOException, InterruptedException {
                verifyLease(); return adb.runBinary(args);
            }
        };
        ui = new AndroidUiDriver(device, output);
        ui.recordActionsAs(trace::recordAction); ui.recordCapturesAs(trace::capture);
        Path receipt = output.resolve("signed-package-context.json");
        Files.copy(run.resolve("setup-release.json"), receipt); evidence.add(receipt);
    }

    void verifyLease() throws java.io.IOException {
        var lease = new ObjectMapper().readTree(leaseFile.toFile());
        if (!lease.path("active").asBoolean() || !leaseId.equals(lease.path("leaseId").asText())
                || !setup.path("serial").asText().equals(lease.path("serial").asText()))
            throw new java.io.IOException("Signed release acceptance lost its exclusive device lease");
    }

    public String installedHash() throws Exception {
        verifyLease();
        var result = adb.run("shell", "sha256sum", setup.required("installedPath").asText());
        result.requireSuccess(); return result.output().trim().split("\\s+")[0];
    }

    public BidChoiceObservation.State awaitRecommendation(String label) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        do {
            String xml = ui.dump();
            if (ui.hasText(xml, label)) {
                var state = BidChoiceObservation.read(xml, label);
                if (state.humanTurn() && state.selected().isPresent()) return state;
            }
            Thread.sleep(120);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("The signed app did not expose a selected human recommendation");
    }

    public void checkpoint(String title, Comparisons comparisons) throws Exception {
        var captured = ui.capture(title.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(java.util.Locale.ROOT));
        evidence.addAll(captured);
        Path xml = captured.stream().filter(file -> file.toString().endsWith(".xml")).findFirst().orElseThrow();
        var checks = comparisons.read(Files.readString(xml));
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        UiFlowCheckpoint.checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN, title, PACKAGE, captured,
                "Observe the actual signed application", "All declared UI comparisons match", "Captured hierarchy and PNG", 0,
                checks, passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        if (!passed) throw new AssertionError(title + ": " + checks);
    }

    void publish(Throwable failure) throws Exception {
        trace.finish(failure == null ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        AndroidUiFlowReportPublisher.publish(output.resolve("report.html"),
                "package:" + setup.required("installedPath").asText() + "; sha256=" + setup.required("requestedApkSha256").asText(),
                setup.required("serial").asText(), setup.required("api").asText(),
                "Actual signed APK; " + trace.name() + "; verified backup restoration belongs to runner teardown", evidence, trace);
    }

    void execute(Flow flow) throws Throwable {
        Throwable failure = null;
        try {
            trace.beginPhase("TEST BODY");
            flow.run(this);
        } catch (Throwable original) {
            failure = original;
            try { trace.fail(original); } catch (Throwable diagnostic) {
                if (diagnostic != original) original.addSuppressed(diagnostic);
            }
        } finally {
            try { publish(failure); } catch (Throwable publication) {
                if (failure == null) failure = publication;
                else if (publication != failure) failure.addSuppressed(publication);
            }
        }
        if (failure != null) throw failure;
    }

    void profileText(String description, String value) throws Exception {
        if (!value.matches("[A-Za-z0-9]{1,10}")) throw new IllegalArgumentException("Controlled short profile fixture required");
        ui.tapDescription(description);
        command("Clear profile input", "shell", "input", "keyevent", "123",
                "67", "67", "67", "67", "67", "67", "67", "67", "67", "67", "67");
        command("Type controlled profile input", "shell", "input", "text", value);
        ui.back();
    }

    private void command(String title, String... arguments) throws Exception {
        verifyLease();
        long start = System.nanoTime();
        try {
            adb.run(arguments).requireSuccess();
            ui.recordAction(title, "Profile editor", "ADB accepts the keyboard command", "Command accepted",
                    Duration.ofNanos(System.nanoTime() - start).toMillis(), AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(title, "Profile editor", "ADB accepts the keyboard command", failure.toString(),
                    Duration.ofNanos(System.nanoTime() - start).toMillis(), AtfReportStatus.FAIL);
            throw failure;
        }
    }

    public static AtfAssertion equal(String label, Object expected, Object actual) {
        return new AtfAssertion(label, String.valueOf(expected), String.valueOf(actual), java.util.Objects.equals(expected, actual));
    }
    @FunctionalInterface public interface Comparisons { List<AtfAssertion> read(String xml) throws Exception; }
    @FunctionalInterface interface Flow { void run(ReleaseBidContext context) throws Exception; }
}

package atf.android.e2e.release;

import atf.android.AdbClient;
import atf.android.e2e.display.flow.responsive.ResponsiveDisplayActions;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.release.isolation.ReleaseTestDataBoundary;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.driver.hierarchy.HierarchyDocument;
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
import java.util.Set;
import java.util.stream.Collectors;

/** Actual signed-package evidence; match-changing acceptance requires a verified data-isolation boundary. */
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
        run = atf.android.e2e.support.flow.WhistLabScenario.externalPath(run.toString(), "whist.records.runRoot");
        setup = json.readTree(run.resolve("setup-release.json").toFile());
        leaseId = json.readTree(run.resolve("device-lease.json").toFile()).required("leaseId").asText();
        leaseFile = atf.android.e2e.support.flow.WhistLabScenario.externalPath(
                System.getProperty("whist.records.leasePath", "C:/work/whist_back/android-ui-lease.json"),
                "whist.records.leasePath");
        String configured = System.getProperty("atf.android.serial", System.getenv("ATF_ANDROID_SERIAL"));
        if (!setup.required("serial").asText().equals(configured)) throw new IllegalStateException("Different configured Android serial");
        ReleaseTestDataBoundary.verify(run, setup);
        verifyLease();
        output = atf.android.e2e.support.flow.WhistLabScenario.artifacts(id);
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
        if ("DISPOSABLE_SECONDARY_USER".equals(setup.path("dataMode").asText())) {
            int expectedUser = setup.path("testUserId").asInt(-1);
            if (expectedUser <= 0) throw new IllegalStateException("Disposable Android user ID is missing");
            var current = adb.run("shell", "am", "get-current-user"); current.requireSuccess();
            if (!Integer.toString(expectedUser).equals(current.output().trim()))
                throw new IllegalStateException("The disposable Android user is not the active test user");
            var installedForUser = adb.run("shell", "pm", "list", "packages", "--user",
                    Integer.toString(expectedUser), PACKAGE); installedForUser.requireSuccess();
            if (!installedForUser.output().contains("package:" + PACKAGE))
                throw new IllegalStateException("Whist Lab is not installed for the disposable Android user");
            Path boundaryReceipt = output.resolve("data-isolation-ready.json");
            Files.copy(run.resolve("data-isolation-ready.json"), boundaryReceipt); evidence.add(boundaryReceipt);
        }
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

    void withPortraitProfiles(List<ResponsiveDisplayProfile> profiles, PortraitFlow flow) throws Exception {
        verifyLease();
        var size = adb.run("shell", "wm", "size");
        var density = adb.run("shell", "wm", "density");
        size.requireSuccess();
        density.requireSuccess();
        Throwable failure = null;
        try {
            for (var profile : profiles) {
                verifyLease();
                ResponsiveDisplayActions.applyProfile(adb, ui, profile);
                flow.run(profile, this);
            }
        } catch (Exception | Error original) {
            failure = original;
            throw original;
        } finally {
            try {
                verifyLease();
                ResponsiveDisplayActions.restoreDisplay(adb, ui, size.output(), density.output());
            } catch (Exception | Error cleanup) {
                if (failure == null) throw cleanup;
                failure.addSuppressed(cleanup);
            }
        }
    }

    public BidChoiceObservation.State awaitRecommendation(String label) throws Exception {
        return awaitRecommendation(label, 8);
    }

    BidChoiceObservation.State awaitRecommendation(String label, int maximumBid) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        do {
            String xml = ui.dump();
            if (ui.hasText(xml, label)) {
                var state = BidChoiceObservation.read(xml, label, maximumBid);
                if (state.humanTurn() && state.selected().isPresent()) return state;
            }
            Thread.sleep(120);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("The signed app did not expose a selected human recommendation");
    }

    public String awaitTextAndDismissAchievementNotice(String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        boolean noticeDismissed = false;
        do {
            String xml = ui.dump();
            if (!noticeDismissed && dismissAchievementNoticeIfPresent(xml)) {
                noticeDismissed = true;
                deadline = System.nanoTime() + timeout.toNanos();
                continue;
            }
            if (ui.hasText(xml, expected)) return xml;
            Thread.sleep(120);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("UI did not show text '" + expected + "' within " + timeout);
    }

    public void revealPostHandAction(String expected) throws Exception {
        ui.revealText(expected, this::dismissAchievementNoticeIfPresent);
    }

    public boolean dismissAchievementNoticeIfPresent(String xml) throws Exception {
        Set<String> visibleText = HierarchyDocument.nodes(xml).stream()
                .map(HierarchyDocument.Node::text).collect(Collectors.toSet());
        var notice = ReleaseAchievementNotice.find(visibleText);
        if (notice.isEmpty()) return false;

        var recognized = notice.orElseThrow();
        checkpoint("A newly earned achievement notice is visible and dismissible", current -> List.of(
                equal("Localized achievement notice is visible", true, ui.hasText(current, recognized.title())),
                equal("The notice exposes its matching close action", true, ui.hasText(current, recognized.closeAction()))));
        ui.tapText(recognized.closeAction());
        return true;
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
                "Actual signed APK; " + trace.name() + "; backup restoration or disposable-user removal belongs to runner teardown", evidence, trace);
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

    void swipeHand(AndroidUiDriver.Bounds hand, boolean towardEnd) throws Exception {
        int distance = (hand.right() - hand.left()) * 3 / 4;
        int left = hand.left() + (hand.right() - hand.left() - distance) / 2;
        int right = left + distance;
        int y = (hand.top() + hand.bottom()) / 2;
        verifyLease();
        long started = System.nanoTime();
        adb.run("shell", "input", "swipe", Integer.toString(towardEnd ? right : left), Integer.toString(y),
                Integer.toString(towardEnd ? left : right), Integer.toString(y), "350").requireSuccess();
        ui.recordAction("Swipe", "Player hand", "Expose the " + (towardEnd ? "last" : "first") + " cards",
                "Horizontal gesture accepted", Duration.ofNanos(System.nanoTime() - started).toMillis(),
                AtfReportStatus.PASS);
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
    @FunctionalInterface interface PortraitFlow {
        void run(ResponsiveDisplayProfile profile, ReleaseBidContext context) throws Exception;
    }
}

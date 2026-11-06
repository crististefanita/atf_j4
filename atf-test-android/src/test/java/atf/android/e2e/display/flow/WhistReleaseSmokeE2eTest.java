package atf.android.e2e.display.flow;

import atf.android.AdbClient;
import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.flow.UiFlowCheckpoint;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Actual signed APK smoke: no debug extras or fixture injection, with the shared Android evidence projection. */
@AndroidE2eTest
class WhistReleaseSmokeE2eTest {
    @Test void signedApkOpensItsRealHomeAndConfiguration() throws Throwable {
        var json = new ObjectMapper();
        Path run = Path.of(System.getProperty("whist.records.runRoot"));
        var setup = json.readTree(run.resolve("setup-release.json").toFile());
        String leaseId = json.readTree(run.resolve("device-lease.json").toFile()).path("leaseId").asText();
        Path leaseFile = Path.of(System.getProperty("whist.records.leasePath", "C:/work/whist_back/android-ui-lease.json"));
        var adb = AdbClient.fromSystemProperties();
        var device = new AndroidUiDriver.Device() {
            private void requireLease() throws Exception {
                var lease = json.readTree(leaseFile.toFile());
                if (!lease.path("active").asBoolean() || !leaseId.equals(lease.path("leaseId").asText())
                        || !setup.path("serial").asText().equals(lease.path("serial").asText()))
                    throw new java.io.IOException("Signed APK smoke lost its exclusive device lease");
            }
            public AdbClient.Result run(String... args) throws java.io.IOException, InterruptedException {
                try { requireLease(); } catch (java.io.IOException failure) { throw failure; }
                catch (Exception failure) { throw new java.io.IOException("Lease could not be read", failure); }
                return adb.run(args);
            }
            public AdbClient.BinaryResult runBinary(String... args) throws java.io.IOException, InterruptedException {
                try { requireLease(); } catch (java.io.IOException failure) { throw failure; }
                catch (Exception failure) { throw new java.io.IOException("Lease could not be read", failure); }
                return adb.runBinary(args);
            }
        };
        Path output = Path.of(System.getProperty("atf.android.artifacts")).resolve("signed-release-smoke");
        Files.createDirectories(output);
        var ui = new AndroidUiDriver(device, output);
        var trace = new AndroidUiFlowTrace(output, "signed-release-smoke", "Signed release — home, language and configuration",
                getClass().getSimpleName(), getClass().getPackageName(), "ro.whistlab.app", List.of("e2e", "android", "signed-release"));
        ui.recordActionsAs(trace::recordAction); ui.recordCapturesAs(trace::capture);
        var files = new ArrayList<Path>();
        Path context = output.resolve("signed-release-context.json");
        Files.copy(run.resolve("setup-release.json"), context); files.add(context);
        Throwable failure = null;
        try {
            trace.beginPhase("TEST BODY");
            ui.restartToActivity("ro.whistlab.app", ".MainActivity");
            String xml = ui.dump();
            boolean romanian = ui.hasText(xml, "Joc nou");
            String newGame = romanian ? "Joc nou" : "New game";
            xml = ui.awaitText(newGame, Duration.ofSeconds(15));
            String installed = device.run("shell", "sha256sum", setup.path("installedPath").asText()).output().trim().split("\\s+")[0];
            record(ui, trace, files, "Real signed APK home", List.of(
                    new AtfAssertion("Installed signed APK SHA-256", setup.path("requestedApkSha256").asText(), installed,
                            setup.path("requestedApkSha256").asText().equals(installed)),
                    new AtfAssertion("Real home controls are visible", "true", "" + ui.hasText(xml, newGame), ui.hasText(xml, newGame)),
                    new AtfAssertion("Home content fits the first viewport", "true", "" + ui.allContentFitsViewport(xml), ui.allContentFitsViewport(xml))));
            ui.tapText(newGame);
            xml = ui.awaitText(romanian ? "Începe partida" : "Start game", Duration.ofSeconds(15));
            record(ui, trace, files, "Real configuration without starting another match", List.of(
                    new AtfAssertion("Start and Back are visible together", "true", "" + (ui.hasText(xml, romanian ? "Înapoi" : "Back")
                            && ui.hasText(xml, romanian ? "Începe partida" : "Start game")),
                            ui.hasText(xml, romanian ? "Înapoi" : "Back") && ui.hasText(xml, romanian ? "Începe partida" : "Start game")),
                    new AtfAssertion("Configuration fits the first viewport", "true", "" + ui.allContentFitsViewport(xml), ui.allContentFitsViewport(xml))));
            ui.tapText(romanian ? "Înapoi" : "Back");
            xml = ui.awaitText(newGame, Duration.ofSeconds(15));
            record(ui, trace, files, "Home remains reachable after Back", List.of(
                    new AtfAssertion("Back returns to the existing home", "true", "" + ui.hasText(xml, newGame), ui.hasText(xml, newGame))));
        } catch (Throwable error) {
            failure = error;
            try { trace.fail(error); } catch (Throwable diagnostic) { if (diagnostic != error) error.addSuppressed(diagnostic); }
        }
        finally {
            try {
            trace.finish(failure == null ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            AndroidUiFlowReportPublisher.publish(output.resolve("report.html"), "package:" + setup.path("installedPath").asText()
                            + "; sha256=" + setup.path("requestedApkSha256").asText(), setup.path("serial").asText(),
                    setup.path("api").asText(), "Actual signed APK; no debug extras, fixture injection or match started", files, trace);
            } catch (Throwable publication) {
                if (failure == null) failure = publication;
                else if (publication != failure) failure.addSuppressed(publication);
            }
        }
        if (failure != null) throw failure;
    }
    private static void record(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> files, String title,
                               List<AtfAssertion> checks) throws Exception {
        var captured = ui.capture(title.replace(' ', '-').toLowerCase(java.util.Locale.ROOT));
        files.addAll(captured);
        boolean pass = checks.stream().allMatch(AtfAssertion::passed);
        UiFlowCheckpoint.checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN, title, "Signed Whist Lab Android state", captured,
                "Observe the real signed APK", "All independent UI comparisons match", "See captured comparisons", 0, checks,
                pass ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        if (!pass) throw new AssertionError(title + ": " + checks);
    }
}

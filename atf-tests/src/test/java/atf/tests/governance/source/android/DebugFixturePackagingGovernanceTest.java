package atf.tests.governance.source.android;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prevents deterministic display/replay fixtures and their intent keys from entering Release sources. */
@AtfComponentTest
class DebugFixturePackagingGovernanceTest {
    private static final List<String> FIXTURE_KEYS = List.of(
            "debug_replay", "debug_fixture", "debug_trick_pause_fixture", "debug_played_card_fixture",
            "debug_hard_ai_bid_fixture", "debug_hand_fan_fixture", "debug_nine_bids_fixture",
            "debug_compact_table_fixture", "persist_fixture");

    @Test void productionCoordinatorDelegatesAndVariantSourcesRemainSeparated() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        Path app = root.resolve("whist-lab-app/app/src");
        String main = Files.readString(app.resolve("main/java/ro/whistlab/app/MainActivity.java"));
        String releaseFactory = Files.readString(
                app.resolve("release/java/ro/whistlab/app/fixtures/DebugFixtureFactory.java"));
        String debugFactory = Files.readString(
                app.resolve("debug/java/ro/whistlab/app/fixtures/DebugFixtureFactory.java"));

        var violations = violations(main, releaseFactory);
        for (String key : FIXTURE_KEYS) {
            if (!debugFactory.contains('"' + key + '"')) violations.add("Debug factory lost launch key " + key);
        }
        AuditedAssertions.assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
        AuditedAssertions.assertTrue(Files.isRegularFile(
                app.resolve("debug/java/ro/whistlab/app/fixtures/DisplayFixtureDeals.java")));
        AuditedAssertions.assertTrue(Files.isRegularFile(
                app.resolve("debug/java/ro/whistlab/app/fixtures/OriginalMatchReplay.java")));
        AuditedAssertions.assertFalse(Files.exists(
                app.resolve("main/java/ro/whistlab/app/fixtures/DisplayFixtureDeals.java")));
        AuditedAssertions.assertFalse(Files.exists(
                app.resolve("main/java/ro/whistlab/app/fixtures/OriginalMatchReplay.java")));
    }

    @Test void policyRejectsFixtureKeysInCoordinatorOrReleaseImplementation() {
        String validCoordinator = "if (BuildConfig.DEBUG) { DebugFixtureFactory.fromIntent(intent, files); }";
        String validRelease = "public DebugLaunch fromIntent(Intent i, File f) { return null; }";
        AuditedAssertions.assertTrue(violations(validCoordinator, validRelease).isEmpty());
        AuditedAssertions.assertFalse(violations(validCoordinator + " String key = \"debug_fixture\";", validRelease).isEmpty());
        AuditedAssertions.assertFalse(violations(validCoordinator, validRelease + " DisplayFixtureDeals").isEmpty());
    }

    private static List<String> violations(String main, String releaseFactory) {
        var failures = new ArrayList<String>();
        if (!main.matches("(?s).*if\\s*\\(BuildConfig\\.DEBUG\\)\\s*\\{[^}]*DebugFixtureFactory\\.fromIntent.*")) {
            failures.add("MainActivity must call the factory only inside its debug build guard");
        }
        for (String key : FIXTURE_KEYS) {
            if (main.contains('"' + key + '"')) failures.add("MainActivity must not own fixture intent key " + key);
            if (releaseFactory.contains('"' + key + '"')) failures.add("Release factory must not recognize fixture intent key " + key);
        }
        for (String implementation : List.of("DisplayFixtureDeals", "OriginalMatchReplay")) {
            if (main.contains(implementation)) failures.add("MainActivity must not construct " + implementation);
            if (releaseFactory.contains(implementation)) failures.add("Release factory must not package " + implementation);
        }
        if (!releaseFactory.contains("return null;")) failures.add("Release factory must ignore debug-only launch requests");
        return failures;
    }
}
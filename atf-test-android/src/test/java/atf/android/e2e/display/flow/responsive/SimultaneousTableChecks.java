package atf.android.e2e.display.flow.responsive;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.display.support.TableViewportObservation;
import java.nio.file.Path;
import java.util.List;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.record;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.APP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.WAIT;

/** Verifică o singură stare capturată; nu unește observații din viewport-uri diferite. */
public final class SimultaneousTableChecks {
    private SimultaneousTableChecks() {}

    public static void inspect(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        for (boolean completeTrick : new boolean[]{false, true}) {
            String fixture = completeTrick ? "debug_compact_table_fixture" : "debug_hand_fan_fixture";
            int handSize = completeTrick ? 12 : 13;
            ui.restartToActivity(APP, ".MainActivity", "--ez", fixture, "true",
                    "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
            String xml = ui.awaitDescriptionContaining("Your hand, " + handSize + " cards.", WAIT);
            var checks = auditSnapshot(ui, xml, profile, handSize, completeTrick);
            record(ui, adb, trace, evidence, profile,
                    completeTrick ? "See all four trick cards, twelve hand cards and four seats without scrolling"
                            : "See thirteen hand cards and every seat without scrolling", checks);
        }
    }

    /** Decodes a recorded hierarchy without sending device commands. */
    public static List<atf.reporting.contract.AtfAssertion> auditSnapshot(AndroidUiDriver ui, String xml,
            ResponsiveDisplayProfile profile, int handSize, boolean completeTrick) throws Exception {
        return TableViewportObservation.inspect(ui,xml,profile,handSize,completeTrick);
    }
}

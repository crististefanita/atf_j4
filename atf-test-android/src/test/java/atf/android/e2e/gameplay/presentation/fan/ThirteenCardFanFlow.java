package atf.android.e2e.gameplay.presentation.fan;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import java.nio.file.Path;
import java.util.List;

/** Intrarea comună a fazelor de evantai; identitatea execuției rămâne la coordonator. */
public final class ThirteenCardFanFlow {
    private ThirteenCardFanFlow() {}

    public static void run(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, List<AtfAssertion> assertions) throws Exception {
        var initial = FanSetupPhase.open(ui, trace, evidence, assertions);
        FanPlayPhase.verify(adb, ui, trace, evidence, assertions, initial);
    }
}

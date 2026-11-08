package atf.android.e2e.release;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.release.autogame.AutoGameReleaseScenario;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@AndroidE2eTest
class WhistReleaseBidE2eTest {
    @Test void signedRecommendationManualChoiceAndSavedContractFollowTheRealRomanianFlow() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")));
        context.execute(ReleaseBidScenario::run);
    }

    @Test void autoGamePlaysOnlyTheCurrentHandAndReturnsControlForTheNext() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")),
                "signed-release-auto-game", "Signed release — AutoGame bounded to one hand", getClass().getSimpleName());
        context.execute(AutoGameReleaseScenario::run);
    }
}

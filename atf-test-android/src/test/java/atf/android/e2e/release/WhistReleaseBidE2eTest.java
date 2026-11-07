package atf.android.e2e.release;

import atf.android.e2e.activation.AndroidE2eTest;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@AndroidE2eTest
class WhistReleaseBidE2eTest {
    @Test void signedRecommendationManualChoiceAndSavedContractFollowTheRealRomanianFlow() throws Throwable {
        var context = new ReleaseBidContext(Path.of(System.getProperty("whist.records.runRoot")));
        context.execute(ReleaseBidScenario::run);
    }
}

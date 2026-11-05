package atf.android.e2e.display.flow;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.display.flow.responsive.ResponsiveSanityRun;

import org.junit.jupiter.api.Test;

@AndroidE2eTest
class WhistResponsivePlaySanityE2eTest {
    @Test
    void configuredProfilesPassMenuAndDisplaySanity() throws Exception {
        ResponsiveSanityRun.run(getClass().getSimpleName());
    }
}

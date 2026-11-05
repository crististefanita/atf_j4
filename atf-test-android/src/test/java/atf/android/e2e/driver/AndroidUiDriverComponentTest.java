package atf.android.e2e.driver;

import atf.testkit.AtfComponentTest;

import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class AndroidUiDriverComponentTest {
    @Test
    void symmetricSeriesSelectorRetainsItsStableIdentityAndLegacyAlias() {
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Five-hand reward / penalty"));
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Bonus / penalizare după cinci mâini"));
        assertTrue(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Five-hand exact-contract bonus"));
        assertFalse(UiHierarchy.matchesSpinnerLabel("romanian-streak-value", "Game type"));
    }
    @Test
    void clippedScrollViewDoesNotPassAsACompleteFirstViewport() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node class="android.widget.ScrollView" scrollable="true" bounds="[0,0][360,640]">
                    <node class="android.widget.TextView" text="Clipped content" bounds="[0,620][360,640]" />
                  </node>
                </hierarchy>
                """;

        assertFalse(UiHierarchy.allContentFitsViewport(hierarchy));
    }

    @Test
    void completeNonScrollableContentPassesTheFirstViewportContract() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node class="android.widget.ScrollView" scrollable="false" bounds="[0,0][360,640]">
                    <node class="android.widget.TextView" text="Visible content" bounds="[0,100][360,140]" />
                  </node>
                </hierarchy>
                """;

        assertTrue(UiHierarchy.allContentFitsViewport(hierarchy));
    }

    @Test
    void enabledNumericBidsAreResolvedAsButtonsDespiteRepeatedSeatLabels() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node text="0" class="android.widget.TextView" enabled="true" />
                  <node text="0" class="android.widget.Button" enabled="true" />
                  <node text="1" class="android.widget.Button" enabled="false" />
                  <node text="2" class="android.widget.Button" enabled="true" />
                </hierarchy>
                """;

        assertEquals(List.of(0, 2), AndroidUiDriver.enabledNumericButtonValues(hierarchy));
    }

    @Test
    void screenshotPointScalesLogicalBoundsToPhysicalCapturePixels() {
        BufferedImage physicalCapture = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);

        assertEquals(95, AndroidUiDriver.screenshotPoint(
                physicalCapture, 63, 768.5, 720, 1280)[0]);
        assertEquals(1153, AndroidUiDriver.screenshotPoint(
                physicalCapture, 63, 768.5, 720, 1280)[1]);
    }
}

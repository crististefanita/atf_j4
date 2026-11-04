package atf.android.e2e.driver;

import atf.testkit.AtfComponentTest;

import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest
class AndroidUiDriverComponentTest {
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

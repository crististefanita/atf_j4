package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class AndroidUiDriverActionTraceTest {
    @Test
    void recordsTheResolvedAndroidTapAndMeasuredDispatchWithoutInventingTheResult(@TempDir Path output)
            throws Exception {
        AndroidUiDriver.Device device = new AndroidUiDriver.Device() {
            @Override
            public AdbClient.Result run(String... arguments) {
                return new AdbClient.Result(0, "", List.of(arguments));
            }

            @Override
            public AdbClient.BinaryResult runBinary(String... arguments) {
                throw new UnsupportedOperationException("Binary capture is outside this interaction fixture");
            }
        };
        AndroidUiDriver driver = new AndroidUiDriver(device, output);
        List<atf.reporting.ui.model.AtfUiActionRecord> actions = new ArrayList<>();
        driver.recordActionsAs(actions::add);
        String hierarchy = "<hierarchy><node text=\"New game\" class=\"android.widget.Button\" "
                + "bounds=\"[10,20][110,70]\" clickable=\"true\" enabled=\"true\"/></hierarchy>";

        driver.tapTextIn(hierarchy, "New game");

        assertEquals(1, actions.size());
        var action = actions.getFirst();
        assertEquals("Tap", action.operation());
        assertEquals("New game", action.target());
        assertEquals("Dispatch a tap to the resolved target at its current accessibility bounds", action.expected());
        assertEquals("ADB accepted tap at 60,45", action.observed());
        assertTrue(action.durationMillis() > 0);
        assertTrue(action.durationMillis() < 500,
                "The measured ADB dispatch must exclude the fixed post-tap UI settling delay");
        assertEquals(AtfReportStatus.PASS, action.status());
        assertEquals(AtfUiEvidenceSource.ACTION, action.source());
    }

    @Test
    void resolvesTheWholeHandViewportWhenNestedNodesShareItsAccessibilityDescription(@TempDir Path output)
            throws Exception {
        AndroidUiDriver.Device device = new AndroidUiDriver.Device() {
            @Override
            public AdbClient.Result run(String... arguments) {
                return new AdbClient.Result(0, "", List.of(arguments));
            }

            @Override
            public AdbClient.BinaryResult runBinary(String... arguments) {
                throw new UnsupportedOperationException("Binary capture is outside this bounds fixture");
            }
        };
        AndroidUiDriver driver = new AndroidUiDriver(device, output);
        String hierarchy = "<hierarchy><node class=\"android.widget.HorizontalScrollView\" "
                + "content-desc=\"Your hand, 13 cards.\" bounds=\"[10,20][610,120]\">"
                + "<node class=\"android.widget.FrameLayout\" content-desc=\"Your hand, 13 cards.\" "
                + "bounds=\"[10,20][580,120]\"/></node></hierarchy>";

        assertEquals(new AndroidUiDriver.Bounds(10, 20, 610, 120),
                driver.boundsForDescription(hierarchy, "Your hand, 13 cards.",
                        "android.widget.HorizontalScrollView"));
    }

    @Test
    void readsTheSelectedValueFromItsNamedSpinnerInsteadOfAnotherMatchingLabel(@TempDir Path output)
            throws Exception {
        AndroidUiDriver.Device device = new AndroidUiDriver.Device() {
            @Override
            public AdbClient.Result run(String... arguments) {
                return new AdbClient.Result(0, "", List.of(arguments));
            }

            @Override
            public AdbClient.BinaryResult runBinary(String... arguments) {
                throw new UnsupportedOperationException("Binary capture is outside this spinner fixture");
            }
        };
        AndroidUiDriver driver = new AndroidUiDriver(device, output);
        String hierarchy = "<hierarchy><node text=\"Players\" class=\"android.widget.Spinner\" "
                + "content-desc=\"Players\" bounds=\"[10,20][210,70]\"><node text=\"6\" "
                + "class=\"android.widget.TextView\" bounds=\"[20,30][200,60]\"/></node>"
                + "<node text=\"6\" class=\"android.widget.Button\" bounds=\"[300,20][350,70]\"/>"
                + "</hierarchy>";

        assertEquals("6", driver.spinnerValue(hierarchy, "Players"));
    }
}

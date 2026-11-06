package atf.android.e2e.display;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.android.e2e.driver.AndroidUiDriver;

import atf.android.AdbClient;
import atf.testkit.AtfComponentTest;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class HandGeometryDriverComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void verticalIndexAndFullCardFaceHaveDifferentGeometry(@TempDir Path out) throws Exception {
        var driver = driver(out);
        String xml = """
                <hierarchy><node class="android.widget.FrameLayout" bounds="[20,100][60,198]">
                  <node class="android.widget.TextView" text="10&#10;♠" bounds="[20,100][40,135]"/>
                </node><node text="10 points" bounds="[200,20][300,60]"/></hierarchy>
                """;
        assertEquals("10♠", driver.bottomRowCardLabels(xml).getFirst().text());
        assertEquals(new AndroidUiDriver.Bounds(20, 100, 40, 135), driver.playingCardLabelBounds(xml).getFirst());
        assertEquals(new AndroidUiDriver.Bounds(20, 100, 60, 198), driver.handCardFaceBounds(xml).getFirst());
        assertTrue(driver.hasText(xml, "10 points"), "Only card notation may be normalized");
    }

    @Test void ASeatValueCannotBeMistakenForTheSameNumericBid(@TempDir Path out) throws Exception {
        var driver = driver(out);
        String xml = """
                <hierarchy><node class="android.widget.TextView" text="0" bounds="[5,5][20,25]"/>
                  <node class="android.widget.Button" text="0" selected="true" bounds="[100,100][148,148]"/>
                </hierarchy>
                """;
        assertEquals(new AndroidUiDriver.Bounds(100, 100, 148, 148), driver.buttonBounds(xml, "0"));
        assertTrue(driver.isSelected(xml, "0"));
        assertThrows(IllegalStateException.class, () -> driver.buttonBounds(xml.replace("android.widget.Button", "android.widget.TextView"), "0"));
    }

    private static AndroidUiDriver driver(Path out) throws Exception {
        return new AndroidUiDriver(new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) { throw new AssertionError("No device call in a parsing contract"); }
            public AdbClient.BinaryResult runBinary(String... args) { throw new AssertionError("No capture in a parsing contract"); }
        }, out);
    }
}

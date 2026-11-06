package atf.android.e2e.display.component;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.android.evidence.DisplayRaster;
import atf.android.evidence.DisplayRaster.Mode;
import atf.android.evidence.DisplayRaster.Size;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DisplayRasterComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    private static final Size PHYSICAL = new Size(1080, 2400);
    private static final Size LOGICAL = new Size(720, 1280);

    @Test void olderNativeCaptureCanRetainTheCompletePhysicalRaster() {
        assertEquals(Mode.PHYSICAL, DisplayRaster.classify(PHYSICAL, PHYSICAL, LOGICAL));
    }

    @Test void modernNativeCaptureCanUseTheCompleteActiveLogicalRaster() {
        assertEquals(Mode.LOGICAL, DisplayRaster.classify(LOGICAL, PHYSICAL, LOGICAL));
    }

    @Test void noOverrideHasOneUnambiguousRaster() {
        assertEquals(Mode.PHYSICAL, DisplayRaster.classify(PHYSICAL, PHYSICAL, PHYSICAL));
    }

    @Test void clippingEvenOnePixelCannotBecomeACompleteCapture() {
        for (Size clipped : new Size[]{new Size(719,1280), new Size(720,1279),
                new Size(1079,2400), new Size(1080,2399)}) {
            assertEquals(Mode.MISMATCH, DisplayRaster.classify(clipped,PHYSICAL,LOGICAL),clipped.toString());
        }
    }

    @Test void mixedAxesAndArbitraryScalingAreRejected() {
        for (Size invalid : new Size[]{new Size(720,2400),new Size(1080,1280),new Size(2160,4800)}) {
            assertEquals(Mode.MISMATCH,DisplayRaster.classify(invalid,PHYSICAL,LOGICAL),invalid.toString());
        }
    }

    @Test void emptyOrNegativeDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Size(0,1280));
        assertThrows(IllegalArgumentException.class,()->new Size(720,-1));
    }
}

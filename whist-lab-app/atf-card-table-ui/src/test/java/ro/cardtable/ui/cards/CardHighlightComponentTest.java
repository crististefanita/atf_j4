package ro.cardtable.ui.cards;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import org.junit.Test;
import ro.cardtable.ui.hand.HandLayoutMetrics;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public final class CardHighlightComponentTest extends GradleAuditedTest {
    @Test public void revealedCardKeepsTheReferencePaletteAndLift() {
        var style = CardHighlight.RAISED;
        GradleInputs.fixture("Revealed card faces", "heights in dp", "26, 48, 64, 88", "density", 2.25f);
        assertEquals("Cream face", 0xfffff6d7, style.fill());
        assertEquals("Golden outline", 0xffc99537, style.outline());
        assertEquals("Terminal face stroke at fractional density", 2, style.strokeWidthPx(2.25f));
        assertEquals("Rounded pixel radius", 14f, style.radiusPx(2.25f));
        assertEquals("Elevation stays at eight dp", 8, style.elevationDp());
        assertEquals("Short face uses minimum lift", 4, style.liftDp(26));
        assertEquals("48 dp face uses minimum lift", 4, style.liftDp(48));
        assertEquals("64 dp face uses proportional lift", 5, style.liftDp(64));
        assertEquals("88 dp face uses proportional lift", 7, style.liftDp(88));
        assertThrows("An empty face has no reveal geometry", IllegalArgumentException.class, () -> style.liftDp(0));
    }

    @Test public void overlapHintUsesTheSamePaletteWithoutMovingOrReorderingCards() {
        var style = CardHighlight.IN_PLACE;
        float density = 2.25f;
        int indexWidth = 54;
        var fan = HandLayoutMetrics.fan(8, 630, density, 1.5f, indexWidth);
        GradleInputs.fixture("Eight cards in a compact fan", "viewport px", 630, "density", density,
                "index px", indexWidth, "font scale", 1.5f);
        assertEquals("Same highlight fill", CardHighlight.RAISED.fill(), style.fill());
        assertEquals("Same highlight outline", CardHighlight.RAISED.outline(), style.outline());
        assertEquals("Original fan corner radius", 20.25f, style.radiusPx(density));
        assertEquals("No preselection lift", 0, style.liftDp(88));
        assertEquals("No preselection elevation", 0, style.elevationDp());
        assertEquals("Single pixel stroke survives low density", 1, style.strokeWidthPx(.75f));
        assertEquals("Fan stroke at fractional density", 3, style.strokeWidthPx(density));
        assertTrue("Cards overlap while the full index and both strokes fit",
                style.strokeWidthPx(density) * 2 < indexWidth
                        && indexWidth <= fan.advance() && fan.advance() < fan.cardWidth());
    }
}

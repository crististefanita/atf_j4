package atf.android.e2e.driver.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.AndroidUiDriver.CardLabel;
import atf.testkit.AtfComponentTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

@AtfComponentTest
class HierarchyContractComponentTest {
    @TempDir Path output;

    @Test void legalCardsKeepObservedOrderAndDistinctDescriptions() throws Exception {
        String queen = "Queen of hearts. Legal card";
        String ace = "Ace of spades. Legal card";
        String xml = hierarchy(slot(queen, true) + slot(ace, true) + slot(queen, true)
                + slot("King of clubs. Not legal now", false));
        assertEquals(queen, driver().firstLegalCardDescription(xml));
        assertEquals(List.of(queen, ace), driver().legalCardDescriptions(xml));
    }

    @Test void noClickableLegalCardIsARefusalRatherThanAnInventedChoice() throws Exception {
        String xml = hierarchy(slot("Ace of spades. Not legal now", false));
        assertEquals(List.of(), driver().legalCardDescriptions(xml));
        var failure = assertThrows(IllegalStateException.class,
                () -> driver().firstLegalCardDescription(xml));
        assertEquals("No clickable legal card is exposed", failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void accessibleLegalityMustAgreeWithClickability(boolean legal) throws Exception {
        String description = "Ace of spades. " + (legal ? "Legal card" : "Not legal now");
        assertEquals(legal, driver().legalCardIsClickable(hierarchy(slot(description, legal)), "A♠"));
        assertThrows(IllegalStateException.class,
                () -> driver().legalCardIsClickable(hierarchy(slot(description, !legal)), "A♠"));
    }

    @Test void duplicateAccessibleSlotsCannotResolveAnAmbiguousCard() throws Exception {
        String card = slot("Ace of spades. Legal card", true);
        assertThrows(IllegalStateException.class,
                () -> driver().legalCardIsClickable(hierarchy(card + card), "A♠"));
    }

    @Test void turnedTrumpRequiresOneTileAndItsMatchingVisibleChild() throws Exception {
        String tile = "<node content-desc=\"Trump set by turned card: 10 of spades\" bounds=\"[0,0][40,60]\">";
        String card = "<node text=\"10&#10;♠\" bounds=\"[5,5][35,55]\"/>";
        String xml = hierarchy(tile + card + "</node>");
        assertTrue(driver().turnedTrumpIdentityMatchesVisibleCard(xml));
        assertFalse(driver().turnedTrumpIdentityMatchesVisibleCard(xml.replace("10&#10;♠", "J♠")));
        assertFalse(driver().turnedTrumpIdentityMatchesVisibleCard(hierarchy(tile + "</node>" + card)));
        assertFalse(driver().turnedTrumpIdentityMatchesVisibleCard(hierarchy(tile + card + "</node>" + tile + "</node>")));
    }

    @Test void bottomRowIncludesTheExactToleranceAndKeepsLabelAndFaceGeometryDistinct() throws Exception {
        String xml = hierarchy(face("A♠", 85, 10) + face("K♥", 100, 30) + face("Q♦", 84, 50));
        assertEquals(List.of(new CardLabel("A♠", 85), new CardLabel("K♥", 100)),
                driver().bottomRowCardLabels(xml));
        assertEquals(List.of(new Bounds(10, 80, 25, 160), new Bounds(30, 80, 45, 160)),
                driver().handCardFaceBounds(xml));
        assertEquals(List.of(new Bounds(11, 85, 24, 105), new Bounds(31, 100, 44, 120),
                        new Bounds(51, 84, 64, 104)), driver().playingCardLabelBounds(xml));
    }

    @Test void onlyCardNotationMayLoseItsSpacesAndLineBreaks() throws Exception {
        String xml = hierarchy("<node text=\" A&#10; ♥ \"/><node text=\"Not a card&#10; two rows\"/>");
        assertEquals(List.of("A♥", "Not a card\n two rows"), driver().texts(xml));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[-1,0][2,2]", "[1,2][3]", "not bounds"})
    void malformedBoundsRemainExplicitFailures(String bounds) throws Exception {
        String xml = hierarchy("<node text=\"Target\" bounds=\"" + bounds + "\"/>");
        assertThrows(IllegalArgumentException.class, () -> driver().boundsForText(xml, "Target"));
    }

    @Test void documentTypeDeclarationsAreRejectedBeforeReadingEntities() throws Exception {
        String xml = "<!DOCTYPE hierarchy [<!ENTITY hidden 'value'>]>"
                + hierarchy("<node text=\"&hidden;\"/>");
        assertThrows(SAXParseException.class, () -> driver().texts(xml));
    }

    private AndroidUiDriver driver() throws Exception {
        return new AndroidUiDriver(new AndroidUiDriver.Device() {
            @Override public AdbClient.Result run(String... arguments) {
                throw new AssertionError("A hierarchy contract must not command a device");
            }
            @Override public AdbClient.BinaryResult runBinary(String... arguments) {
                throw new AssertionError("A hierarchy contract must not capture a device");
            }
        }, output);
    }

    private static String hierarchy(String content) { return "<hierarchy>" + content + "</hierarchy>"; }

    private static String slot(String description, boolean clickable) {
        return "<node class=\"android.widget.FrameLayout\" content-desc=\"" + description
                + "\" clickable=\"" + clickable + "\"/>";
    }

    private static String face(String label, int top, int left) {
        return "<node bounds=\"[" + left + ",80][" + (left + 15) + ",160]\">"
                + "<node text=\"" + label + "\" bounds=\"[" + (left + 1) + "," + top + "]["
                + (left + 14) + "," + (top + 20) + "]\"/></node>";
    }
}

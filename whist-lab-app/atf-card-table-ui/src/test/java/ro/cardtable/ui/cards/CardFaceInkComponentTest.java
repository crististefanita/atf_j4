package ro.cardtable.ui.cards;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import org.junit.Test;
import ro.cardtable.ui.table.PlayedCardStyle;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

public final class CardFaceInkComponentTest extends GradleAuditedTest {
    @Test public void handAndTableRequestTheSameTextGlyphForEverySuit() {
        String[] suits = {"♠", "♥", "♣", "♦"};
        boolean[] red = {false, true, false, true};
        GradleInputs.fixture("Suits for the hand, table and collection", "suits", suits, "red", red);

        for (int index = 0; index < suits.length; index++) {
            String suit = suits[index];
            assertEquals("The hand uses a text glyph for " + suit, "10\n" + suit + "\uFE0E",
                    CardFaceInk.textPresentation("10\n" + suit));
            assertEquals("The compact face uses a text glyph for " + suit, "10" + suit + "\uFE0E",
                    CardFaceInk.textPresentation("10" + suit));
            assertEquals("Ink matches the exposed face palette for " + suit,
                    red[index] ? PlayedCardStyle.RED : PlayedCardStyle.DARK, CardFaceInk.color(red[index]));
        }
        assertEquals("Rank-only text is unchanged", "10", CardFaceInk.textPresentation("10"));
    }
}

package ro.cardtable.ui.cards;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import org.junit.Test;
import ro.cardtable.ui.table.PlayedCardContent;
import ro.cardtable.ui.table.PlayedCardStyle;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

public final class CardFaceInkComponentTest extends GradleAuditedTest {
    @Test public void theDepositedCardKeepsEveryRankAndSuitFromTheHand() {
        var ranks = java.util.List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
        var suits = java.util.Map.of("\u2660", false, "\u2665", true, "\u2663", false, "\u2666", true);
        GradleInputs.fixture("Complete deck at the hand and trick boundary", "ranks", ranks, "suits", suits);

        for (String rank : ranks) {
            suits.forEach((suit, red) -> {
                var held = new CardFace(rank, suit, red);
                var played = new PlayedCardContent(0, rank + suit, red, "Ana", "Ana " + rank + suit);
                assertEquals("A deposited " + rank + suit + " retains the hand face", held, played.face());
            });
        }
    }

    @Test public void onlyTheScaleOfTheHandFaceChangesInAPlayedSlot() {
        GradleInputs.fixture("Hand and compact table face bounds", "heightsDp", java.util.List.of(88, 70, 64));
        var hand = CardFaceMetrics.atHeight(88);
        assertEquals("Hand face has its original width", 63, hand.widthDp());
        assertEquals("Hand rank has its original size", 13f, hand.indexSizeSp(), 0.001f);
        for (int height : new int[]{70, 64}) {
            var played = CardFaceMetrics.atHeight(height);
            assertEquals("Whole face uses the same scale at " + height + " dp",
                    height / 88f, played.scale(), 0.001f);
            assertEquals("Corner rank scales with the complete face at " + height + " dp",
                    hand.indexSizeSp() * height / 88f, played.indexSizeSp(), 0.001f);
        }
    }

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

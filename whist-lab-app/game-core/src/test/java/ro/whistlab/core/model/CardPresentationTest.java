package ro.whistlab.core.model;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class CardPresentationTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void heartsAndDiamondsAreRedWhileSpadesAndClubsAreBlack() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion");
        assertTrue("Suit.HEARTS.red() must hold", Suit.HEARTS.red());assertTrue("Suit.DIAMONDS.red() must hold", Suit.DIAMONDS.red());
        assertFalse("Suit.SPADES.red() must remain false", Suit.SPADES.red());assertFalse("Suit.CLUBS.red() must remain false", Suit.CLUBS.red());
    }
    @Test public void allFiftyTwoCardsHaveUniqueReadableAndAccessibleIdentities() {
        Set<String> shortLabels=new HashSet<>(),accessible=new HashSet<>();
        for(Suit suit:Suit.values())for(int rank=2;rank<=14;rank++) {
            Card card=new Card(suit,rank);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "accessible", accessible, "card", card, "rank", rank, "shortLabels", shortLabels, "suit", suit);
assertTrue("shortLabels.add(card.shortLabel()) must hold", shortLabels.add(card.shortLabel()));
            assertTrue("accessible.add(card.accessibleLabel()) must hold", accessible.add(card.accessibleLabel()));
            assertTrue("card.shortLabel().endsWith(suit.symbol()) must hold", card.shortLabel().endsWith(suit.symbol()));
            assertTrue("card.accessibleLabel().endsWith(\" of \" + suit.label()) must hold", card.accessibleLabel().endsWith(" of "+suit.label()));
        }
        assertEquals("new Card(Suit.DIAMONDS, 14).shortLabel() must equal \"A\\u2666\"", "A♦",new Card(Suit.DIAMONDS,14).shortLabel());
        assertEquals("new Card(Suit.SPADES, 11).accessibleLabel() must equal \"Jack of Spades\"", "Jack of Spades",new Card(Suit.SPADES,11).accessibleLabel());
        assertEquals("new Card(Suit.HEARTS, 12).accessibleLabel() must equal \"Queen of Hearts\"", "Queen of Hearts",new Card(Suit.HEARTS,12).accessibleLabel());
        assertEquals("new Card(Suit.CLUBS, 13).accessibleLabel() must equal \"King of Clubs\"", "King of Clubs",new Card(Suit.CLUBS,13).accessibleLabel());
    }
    @Test public void ranksOutsideTheStandardDeckAreRejected() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 1, "declared constant 2", 15);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new Card(Suit.SPADES,1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new Card(Suit.SPADES,15));
    }
}

package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class CardPresentationTest {
    @Test public void heartsAndDiamondsAreRedWhileSpadesAndClubsAreBlack() {
        assertTrue(Suit.HEARTS.red());assertTrue(Suit.DIAMONDS.red());
        assertFalse(Suit.SPADES.red());assertFalse(Suit.CLUBS.red());
    }
    @Test public void allFiftyTwoCardsHaveUniqueReadableAndAccessibleIdentities() {
        Set<String> shortLabels=new HashSet<>(),accessible=new HashSet<>();
        for(Suit suit:Suit.values())for(int rank=2;rank<=14;rank++) {
            Card card=new Card(suit,rank);assertTrue(shortLabels.add(card.shortLabel()));
            assertTrue(accessible.add(card.accessibleLabel()));
            assertTrue(card.shortLabel().endsWith(suit.symbol()));
            assertTrue(card.accessibleLabel().endsWith(" of "+suit.label()));
        }
        assertEquals("A♦",new Card(Suit.DIAMONDS,14).shortLabel());
        assertEquals("Jack of Spades",new Card(Suit.SPADES,11).accessibleLabel());
        assertEquals("Queen of Hearts",new Card(Suit.HEARTS,12).accessibleLabel());
        assertEquals("King of Clubs",new Card(Suit.CLUBS,13).accessibleLabel());
    }
    @Test public void ranksOutsideTheStandardDeckAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Card(Suit.SPADES,1));
        assertThrows(IllegalArgumentException.class,()->new Card(Suit.SPADES,15));
    }
}

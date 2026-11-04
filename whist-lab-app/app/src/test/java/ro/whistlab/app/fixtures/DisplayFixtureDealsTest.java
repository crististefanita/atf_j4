package ro.whistlab.app.fixtures;

import static org.junit.Assert.*;
import java.util.HashSet;
import org.junit.Test;
import ro.whistlab.core.model.Card;

public class DisplayFixtureDealsTest {
    @Test public void denseDisplayContainsEveryTenWithoutDuplicatingCards() {
        for (int size : new int[]{8, 13}) {
            var hands = DisplayFixtureDeals.fourPlayerHands(size);
            assertEquals(4, hands.size());
            var seen = new HashSet<Card>();
            for (var hand : hands) {
                assertEquals(size, hand.size());
                for (var card : hand) assertTrue("duplicate " + card, seen.add(card));
            }
            assertEquals(4, hands.get(0).stream().filter(card -> card.rank() == 10).map(Card::suit).distinct().count());
        }
    }
}

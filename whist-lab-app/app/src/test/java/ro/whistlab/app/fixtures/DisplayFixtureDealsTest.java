package ro.whistlab.app.fixtures;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.HashSet;
import org.junit.Test;
import ro.whistlab.core.model.Card;

public class DisplayFixtureDealsTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void completedTrickComesFromLegalPlayAndKeepsTwelveCardsAtEverySeat() {
        var fixture = DisplayFixtureDeals.completedThirteenCardTrick();
        var state = fixture.game().snapshot();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "fixture", fixture, "state", state);
        assertEquals("fixture.cards().size() must equal 4", 4, fixture.cards().size());
        assertEquals("fixture.cards().stream().map((card)->card.seat()).distinct().count() must equal 4", 4, fixture.cards().stream().map(card -> card.seat()).distinct().count());
        var all = new HashSet<Card>();
        for (var player : state.players()) {
            assertEquals("player.hand().size() must equal 12", 12, player.hand().size());
            assertTrue("all.addAll(player.hand()) must hold", all.addAll(player.hand()));
        }
        for (var play : fixture.cards()) assertTrue("all.add(play.card()) must hold", all.add(play.card()));
        assertEquals("all.size() must equal 52", 52, all.size());
        assertTrue("state.currentTrick().isEmpty() must hold", state.currentTrick().isEmpty());
        assertEquals("state.players().stream().mapToInt((player)->player.tricksWon()).sum() must equal 1", 1, state.players().stream().mapToInt(player -> player.tricksWon()).sum());
    }

    @Test public void denseDisplayContainsEveryTenWithoutDuplicatingCards() {
        for (int size : new int[]{8, 13}) {
            var hands = DisplayFixtureDeals.fourPlayerHands(size);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hands", hands, "size", size);
            assertEquals("hands.size() must equal 4", 4, hands.size());
            var seen = new HashSet<Card>();
            for (var hand : hands) {
                assertEquals("hand.size() must equal size", size, hand.size());
                for (var card : hand) assertTrue("duplicate " + card, seen.add(card));
            }
            assertEquals("hands.get(0).stream().filter((card)->card.rank() == 10).map(Card::suit).distinct().count() must equal 4", 4, hands.get(0).stream().filter(card -> card.rank() == 10).map(Card::suit).distinct().count());
        }
    }
}

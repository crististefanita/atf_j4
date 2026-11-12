package ro.whistlab.core.rules.documentation;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

@RunWith(Parameterized.class)
public class DocumentedDealComponentTest extends GradleAuditedTest {
    @Parameterized.Parameters(name = "{0}, {1} players")
    public static List<Object[]> tables() {
        var tables = new ArrayList<Object[]>();
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            for (int players = 3; players <= 6; players++) {
                tables.add(new Object[] {mode, players});
            }
        }
        return tables;
    }

    private final GameMode mode;
    private final int players;

    public DocumentedDealComponentTest(GameMode mode, int players) {
        this.mode = mode;
        this.players = players;
    }

    @Test public void theDocumentedScheduleAndReducedDeckApplyAtEveryTableSize() {
        var standard = mode == GameMode.ROMANIAN_WHIST;
        int endSize = standard ? 1 : 8;
        int middleSize = standard ? 8 : 1;
        var ascending = List.of(2, 3, 4, 5, 6, 7);
        var descending = List.of(7, 6, 5, 4, 3, 2);
        var sizes = new ArrayList<>(Collections.nCopies(players, endSize));
        sizes.addAll(standard ? ascending : descending);
        sizes.addAll(Collections.nCopies(players, middleSize));
        sizes.addAll(standard ? descending : ascending);
        sizes.addAll(Collections.nCopies(players, endSize));
        int minimumRank = new int[] {9, 7, 5, 3}[players - 3];
        var ranks = IntStream.rangeClosed(minimumRank, 14).boxed().toList();
        GradleInputs.fixture("Ordinea mâinilor / Hand schedule and reduced deck",
                "mode", mode, "players", players, "expected schedule", sizes, "ranks in every suit", ranks);
        assertEquals("Each scheduled hand has the documented size", sizes, GameRules.handSizes(mode, players));
        var deck = GameRules.deck(mode, players);
        assertEquals("The reduced deck contains eight cards per seat", 8 * players, deck.size());
        for (var suit : Suit.values()) {
            assertEquals(suit + " has every rank in the reduced deck exactly once", ranks,
                    deck.stream().filter(card -> card.suit() == suit).map(Card::rank).sorted().toList());
        }
    }

    @Test public void theDealerRotatesAndAnUndealtCardAloneDeterminesTrump() {
        var config = GameConfig.newMatch(players, mode, AiDifficulty.NORMAL, 31L, true, 10);
        var game = GameSession.newGame(config);
        var first = game.snapshot();
        finishHand(game);
        game.startNextHand();
        var second = game.snapshot();
        for (int index = 1; index < players + 6; index++) {
            finishHand(game);
            game.startNextHand();
        }
        var middle = game.snapshot();
        GradleInputs.fixture("Împărțire și atu / Dealer rotation and trump",
                "config", config, "first deal", first, "second deal", second, "first middle deal", middle);
        assertEquals("The next seat deals the next hand", (first.dealerSeat() + 1) % players, second.dealerSeat());
        assertEquals("The seat left of the new dealer starts bidding", (second.dealerSeat() + 1) % players,
                second.biddingSeat());
        var full = mode == GameMode.LONG_ROMANIAN ? first : middle;
        var partial = mode == GameMode.LONG_ROMANIAN ? middle : first;
        assertEquals("A full deal gives eight cards to each player", Collections.nCopies(players, 8),
                full.players().stream().map(player -> player.hand().size()).toList());
        assertEquals("A full deal leaves no turned card", null, full.turnedCard());
        assertEquals("Eight cards per player means no trump", Trump.NO_TRUMP, full.trump());
        assertEquals("The partial deal gives one card to each player", Collections.nCopies(players, 1),
                partial.players().stream().map(player -> player.hand().size()).toList());
        assertEquals("The turned card is separate from the dealt cards", false, partial.players().stream()
                .anyMatch(player -> player.hand().contains(partial.turnedCard())));
        assertEquals("The turned card's suit is trump", partial.turnedCard().suit(),
                partial.trump().suit().orElseThrow());
    }

    private static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            boolean last = state.bidsSubmitted() == state.players().size() - 1;
            int bid = GameRules.allowedBids(state.handSize(), state.bidTotal(), last).get(0);
            game.submitBid(state.biddingSeat(), bid);
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var hand = state.players().get(state.currentSeat()).hand();
            game.playCard(state.currentSeat(), GameRules.legalCards(hand, led, state.trump(), state.mode()).get(0));
        }
    }
}

package ro.whistlab.app.fixtures;



import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Deterministic display states, reached only through MainActivity's debug intent gate. */
public final class DisplayFixtureDeals {
    private DisplayFixtureDeals() { }

    public record CompletedTrick(GameSession game, List<PlayedCard> cards) {
        public CompletedTrick { cards = List.copyOf(cards); }
    }

    public static CompletedTrick completedThirteenCardTrick() {
        var config = new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 20261105L);
        var game = GameSession.fromDeal(config, fourPlayerHands(13), Trump.SPADES, 3);
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 0);
        var cards = new ArrayList<PlayedCard>();
        for (int count = 0; count < 4; count++) {
            var state = game.snapshot();
            int seat = state.currentSeat();
            Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            Card card = GameRules.legalCards(state.players().get(seat).hand(), led, state.trump(), state.mode()).get(0);
            cards.add(new PlayedCard(seat, card));
            game.playCard(seat, card);
        }
        return new CompletedTrick(game, cards);
    }

    public static List<List<Card>> fourPlayerHands(int cards) {
        if (cards < 4 || cards > 13) throw new IllegalArgumentException("Display hand must contain 4–13 cards");
        List<Card> deck = new ArrayList<>(GameRules.deck(GameMode.DESCENDING, 4));
        List<Card> human = new ArrayList<>();
        for (Card card : deck) if (card.rank() == 10) human.add(card);
        deck.removeAll(human);
        while (human.size() < cards) human.add(deck.remove(0));
        List<List<Card>> hands = new ArrayList<>();
        hands.add(List.copyOf(human));
        for (int seat = 1; seat < 4; seat++) {
            hands.add(List.copyOf(deck.subList(0, cards)));
            deck.subList(0, cards).clear();
        }
        return List.copyOf(hands);
    }
}

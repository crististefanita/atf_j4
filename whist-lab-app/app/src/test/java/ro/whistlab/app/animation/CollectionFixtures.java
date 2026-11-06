package ro.whistlab.app.animation;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public final class CollectionFixtures {
    private CollectionFixtures() { }

    public static GameSession completed(int players, int winner, int size, boolean trumpWinner) {
        var hands = new ArrayList<List<Card>>();
        for (int seat = 0; seat < players; seat++) {
            var cards = new ArrayList<Card>();
            cards.add(new Card(seat == winner && trumpWinner ? Suit.SPADES : Suit.HEARTS,
                    seat == winner ? 14 : 13 - seat));
            if (size > 1) cards.add(new Card(Suit.CLUBS, 13 - seat));
            hands.add(cards);
        }
        var game = GameSession.fromDeal(new GameConfig(players, GameMode.ROMANIAN_WHIST,
                AiDifficulty.HARD, 31), hands, Trump.SPADES, winner);
        bid(game);
        for (int play = 0; play < players; play++) {
            int seat = game.snapshot().currentSeat();
            game.playCard(seat, hands.get(seat).get(0));
        }
        return game;
    }

    public static GameSession finishedMatch(int players) {
        var game = GameSession.newGame(GameConfig.newMatch(players, GameMode.ROMANIAN_WHIST,
                AiDifficulty.HARD, 31, true, 10));
        while (true) {
            bid(game);
            while (game.snapshot().phase() == GamePhase.PLAYING) playLegal(game);
            if (game.snapshot().finalHand()) return GameSession.restoreComplete(game.saveState());
            game.startNextHand();
        }
    }

    static void bid(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            game.submitBid(state.biddingSeat(), GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0));
        }
    }

    static void playLegal(GameSession game) {
        var state = game.snapshot();
        var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        game.playCard(state.currentSeat(), GameRules.legalCards(state.players().get(state.currentSeat()).hand(),
                led, state.trump(), state.mode()).get(0));
    }

    static GameSession.Snapshot observation(GameSession.Snapshot s, GamePhase phase, List<PlayedCard> current,
                                             List<PlayedCard> last, List<PlayedCard> history, int winner) {
        return new GameSession.Snapshot(phase, s.handIndex(), s.handNumber(), s.handSize(), s.trump(),
                s.dealerSeat(), s.biddingSeat(), s.currentSeat(), s.bidsSubmitted(), s.bidTotal(), s.players(),
                current, last, winner, s.finalHand(), s.mode(), history, s.turnedCard(), s.streakBonuses(),
                s.streakBonusPoints(), s.streakPenaltyPoints());
    }
}

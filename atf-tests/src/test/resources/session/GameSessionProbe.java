import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

import java.util.List;

public final class GameSessionProbe {
    private static final Card ACE = new Card(Suit.SPADES, 14);
    private static final Card KING = new Card(Suit.SPADES, 13);
    private static final Card QUEEN = new Card(Suit.SPADES, 12);

    public static void main(String[] args) {
        switch (args[0]) {
            case "advance" -> advance();
            case "reject" -> reject();
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    private static GameSession table() {
        return GameSession.fromDeal(new GameConfig(3, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 19),
                List.of(List.of(ACE), List.of(KING), List.of(QUEEN)), Trump.HEARTS, 2);
    }

    private static void advance() {
        GameSession game = table();
        game.submitBid(0, 1);
        game = resumed(game);
        int nextBidder = game.snapshot().biddingSeat();
        game.submitBid(1, 0);
        game.submitBid(2, 1);
        game.playCard(0, ACE);
        game = resumed(game);
        int nextActor = game.snapshot().currentSeat();
        game.playCard(1, KING);
        game.playCard(2, QUEEN);
        game = resumed(game);
        var closed = game.snapshot();
        var history = game.scoreHistory();
        game.startNextHand();
        var next = game.snapshot();
        System.out.println("bidder=" + nextBidder + " actor=" + nextActor + " close=" + closed.phase()
                + " winner=" + closed.lastTrickWinnerSeat() + " cards=" + closed.history().size()
                + " totals=" + history.latestTotals() + " contracts=" + history.recordedBids()
                + " tricks=" + history.recordedTricks() + " next=" + next.phase() + "/"
                + next.handNumber() + "/" + next.dealerSeat() + "/" + next.biddingSeat());
    }

    private static GameSession resumed(GameSession original) {
        var saved = original.saveState();
        var restored = GameSession.restoreComplete(saved);
        if (!saved.equals(restored.saveState()) || !original.snapshot().equals(restored.snapshot()))
            throw new AssertionError("The checkpoint changed the session");
        return restored;
    }

    private static void reject() {
        var game = table();
        var beforeBid = game.saveState();
        expect("cannot bid now", () -> game.submitBid(1, 1));
        unchanged(beforeBid, game);
        game.submitBid(0, 1);
        game.submitBid(1, 0);
        var beforeLastBid = game.saveState();
        expect("total bids equal the hand size", () -> game.submitBid(2, 0));
        unchanged(beforeLastBid, game);
        game.submitBid(2, 1);
        game.playCard(0, ACE);
        var saved = game.saveState();
        expect("cannot play now", () -> game.playCard(2, QUEEN));
        expect("not in the player's hand", () -> game.playCard(1, QUEEN));
        unchanged(saved, game);
        var invalid = new GameSession.SaveState(saved.config(), saved.phase(), saved.handIndex(), saved.handSize(),
                saved.dealerSeat(), saved.biddingSeat(), 2, saved.bidsSubmitted(), saved.bidTotal(), saved.trump(),
                saved.lastTrickWinnerSeat(), saved.players(), saved.currentTrick(), saved.lastTrick(), saved.history(),
                saved.turnedCard(), saved.scoreHistory());
        expect("Saved current player is inconsistent", () -> GameSession.restore(invalid));
        System.out.println("Rejected wrong bidder, forbidden sum, wrong actor, foreign card and corrupt checkpoint");
    }

    private static void unchanged(GameSession.SaveState before, GameSession game) {
        if (!before.equals(game.saveState())) throw new AssertionError("Rejected command changed the session");
    }

    private static void expect(String expected, Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException | IllegalStateException error) {
            if (error.getMessage().contains(expected)) return;
            throw new AssertionError("Expected " + expected, error);
        }
        throw new AssertionError("Expected refusal: " + expected);
    }
}

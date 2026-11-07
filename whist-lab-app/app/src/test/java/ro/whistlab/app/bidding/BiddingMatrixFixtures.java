package ro.whistlab.app.bidding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Reaches each fixture through legal commands, including its real score ledger and turned card. */
final class BiddingMatrixFixtures {
    static Collection<Object[]> tableSizes() {
        var cases = new ArrayList<Object[]>();
        for (int players = 3; players <= 6; players++) {
            for (int size = 1; size <= 8; size++) cases.add(new Object[] {players, size});
        }
        return cases;
    }

    static GameSession humanTurn(int players, int size, int position) {
        int handIndex = GameRules.handSizes(GameMode.ROMANIAN_WHIST, players).indexOf(size);
        int dealer = Math.floorMod(-position - 1, players);
        var difficulty = position % 2 == 0 ? AiDifficulty.NORMAL : AiDifficulty.HARD;
        GameSession game = null;
        for (int attempt = 0; attempt < 128; attempt++) {
            long seed = 3100 + 0x9E3779B97F4A7C15L * attempt;
            var config = GameConfig.newMatch(players, GameMode.ROMANIAN_WHIST, difficulty, seed,
                    true, size % 2 == 0 ? 5 : 10);
            var candidate = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config));
            if ((candidate.snapshot().dealerSeat() + handIndex) % players == dealer) {
                game = candidate;
                break;
            }
        }
        if (game == null) throw new AssertionError("No deterministic fixture seed for dealer " + dealer);
        while (game.snapshot().handIndex() < handIndex) {
            finishHand(game);
            game.startNextHand();
        }
        while (game.snapshot().biddingSeat() != 0) {
            var state = game.snapshot();
            int bid = state.bidsSubmitted() == 0 ? size / 2 : 0;
            game.submitBid(state.biddingSeat(), bid);
        }
        return game;
    }

    static GameSession certainWinners(int players, int size) {
        var deck = GameRules.deck(GameMode.ROMANIAN_WHIST, players);
        var own = new ArrayList<Card>();
        for (var suit : List.of(Suit.SPADES, Suit.HEARTS)) {
            for (int rank = 14; rank >= 2 && own.size() < size; rank--) {
                var card = new Card(suit, rank);
                if (deck.contains(card)) own.add(card);
            }
        }
        return BidSelectionFixtures.deal(own, size == 8 ? Trump.NO_TRUMP : Trump.SPADES, players - 1, players);
    }

    /** Arithmetic oracle, independent of the application's allowed-bid collection. */
    static List<Integer> legalBids(GameSession.Snapshot state) {
        var legal = new ArrayList<Integer>();
        for (int bid = 0; bid <= state.handSize(); bid++) {
            if (state.bidsSubmitted() < state.players().size() - 1 || state.bidTotal() + bid != state.handSize()) {
                legal.add(bid);
            }
        }
        return legal;
    }

    static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            game.submitBid(state.biddingSeat(), legalBids(state).get(0));
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led, state.trump(), state.mode());
            game.playCard(state.currentSeat(), legal.get(0));
        }
    }

    private BiddingMatrixFixtures() { }
}

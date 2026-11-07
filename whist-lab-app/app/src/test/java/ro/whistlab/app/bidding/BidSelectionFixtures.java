package ro.whistlab.app.bidding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;

final class BidSelectionFixtures {
    static GameSession deal(List<Card> own, Trump trump, int dealer) {
        return deal(own, trump, dealer, 4);
    }

    static GameSession deal(List<Card> own, Trump trump, int dealer, int players) {
        var others = new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST, players));
        others.removeAll(own);
        var hands = new ArrayList<List<Card>>(); hands.add(own);
        for (int seat = 1; seat < players; seat++) hands.add(List.copyOf(others.subList((seat - 1) * own.size(), seat * own.size())));
        return GameSession.fromDeal(new GameConfig(players, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 91), hands, trump, dealer);
    }

    static GameSession ace() { return deal(List.of(new Card(Suit.SPADES, 14)), Trump.SPADES, 3); }

    static final class Worker extends AbstractExecutorService {
        private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        private boolean closed;
        void runNext() { queue.remove().run(); }
        @Override public void execute(Runnable task) { queue.add(task); }
        @Override public void shutdown() { closed = true; }
        @Override public List<Runnable> shutdownNow() { closed = true; var rest = List.copyOf(queue); queue.clear(); return rest; }
        @Override public boolean isShutdown() { return closed; }
        @Override public boolean isTerminated() { return closed && queue.isEmpty(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }
    private BidSelectionFixtures() { }
}

package ro.whistlab.app.bidding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** A contextual default is advice only; manual choice and session ownership remain authoritative. */
public final class HumanBidSelection {
    public record Request(MonteCarloAi.View observation) {
        public Request { Objects.requireNonNull(observation); }
        public int evaluate() {
            int bid = MonteCarloAi.chooseBid(observation);
            if (!allowed(observation).contains(bid)) throw new IllegalStateException("AI suggested an illegal bid");
            return bid;
        }
    }

    private GameSession owner;
    private int handIndex;
    private MonteCarloAi.View context;
    private Integer selected;
    private Request pending;
    private boolean failed;

    public OptionalInt selected(GameSession game) {
        if (!synchronize(game)) return OptionalInt.empty();
        return selected == null ? OptionalInt.empty() : OptionalInt.of(selected);
    }

    /** Captures only the human's observation; the expensive evaluation belongs to the worker. */
    public Optional<Request> request(GameSession game) {
        if (!synchronize(game) || selected != null || failed || pending != null) return Optional.empty();
        pending = new Request(context);
        return Optional.of(pending);
    }

    public void select(GameSession game, int bid) {
        if (!synchronize(game)) throw new IllegalStateException("The human does not own this bidding turn");
        if (!allowed(context).contains(bid)) throw new IllegalArgumentException("Illegal human bid");
        selected = bid;
        pending = null;
    }

    public boolean apply(GameSession game, Request request, int bid) {
        if (!current(game, request)) return false;
        if (!allowed(context).contains(bid)) throw new IllegalArgumentException("Illegal recommendation");
        selected = bid;
        pending = null;
        return true;
    }

    public boolean failed(GameSession game, Request request) {
        if (!current(game, request)) return false;
        pending = null;
        failed = true;
        return true;
    }

    public void cancel() { pending = null; }

    public void reset() {
        owner = null; context = null; selected = null; pending = null; failed = false;
    }

    private boolean synchronize(GameSession game) {
        Objects.requireNonNull(game);
        var state = game.snapshot();
        if (!humanTurn(state)) { reset(); return false; }
        var observed = MonteCarloAi.observe(state, 0);
        if (owner != game || handIndex != state.handIndex() || !observed.equals(context)) {
            reset(); owner = game; handIndex = state.handIndex(); context = observed;
        }
        return true;
    }

    private boolean current(GameSession game, Request request) {
        if (request == null || pending != request || owner != game) return false;
        var state = game.snapshot();
        return humanTurn(state) && state.handIndex() == handIndex && context.equals(MonteCarloAi.observe(state, 0));
    }

    private static boolean humanTurn(GameSession.Snapshot state) {
        return state.phase() == GamePhase.BIDDING && state.biddingSeat() == 0;
    }

    private static List<Integer> allowed(MonteCarloAi.View view) {
        int total = view.seats().stream().filter(seat -> seat.bid() != null).mapToInt(seat -> seat.bid()).sum();
        boolean last = view.seats().stream().filter(seat -> seat.bid() != null).count() == view.seats().size() - 1;
        return GameRules.allowedBids(view.handSize(), total, last);
    }
}

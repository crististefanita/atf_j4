package ro.whistlab.app.lifecycle;

import java.util.Objects;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Captures on the session owner; evaluation reads only the immutable snapshot. */
public final class AiTurnRequest {
    public sealed interface Decision permits Bid, Play { }
    public record Bid(int seat, int value) implements Decision { }
    public record Play(int seat, Card card) implements Decision {
        public Play { Objects.requireNonNull(card); }
    }

    private final GameSession session;
    private final GameSession.Snapshot state;
    private final AiDifficulty difficulty;

    public AiTurnRequest(GameSession session) {
        this(session, false);
    }

    public AiTurnRequest(GameSession session, boolean autoGameControlsHuman) {
        this.session = Objects.requireNonNull(session);
        state = session.snapshot();
        difficulty = session.config().aiDifficulty();
        if (state.phase() != GamePhase.BIDDING && state.phase() != GamePhase.PLAYING) {
            throw new IllegalArgumentException("No automated decision in this phase");
        }
        int seat = state.phase() == GamePhase.BIDDING ? state.biddingSeat() : state.currentSeat();
        if (seat == 0 && !autoGameControlsHuman) throw new IllegalArgumentException("The human owns this turn");
    }

    public Decision evaluate() {
        if (state.phase() == GamePhase.BIDDING) {
            return new Bid(state.biddingSeat(), AiPolicy.chooseBid(state, state.biddingSeat(), difficulty));
        }
        return new Play(state.currentSeat(), AiPolicy.chooseCard(state, state.currentSeat(), difficulty));
    }

    /** Called only by the session owner; repeated or obsolete results never mutate a session. */
    public boolean applyTo(GameSession current, Decision decision) {
        if (current != session || !state.equals(current.snapshot())) return false;
        if (decision instanceof Bid bid && state.phase() == GamePhase.BIDDING
                && bid.seat() == state.biddingSeat()) {
            current.submitBid(bid.seat(), bid.value());
        } else if (decision instanceof Play play && state.phase() == GamePhase.PLAYING
                && play.seat() == state.currentSeat()) {
            current.playCard(play.seat(), play.card());
        } else throw new IllegalArgumentException("AI result does not match the captured turn");
        return true;
    }
}

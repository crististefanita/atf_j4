package ro.whistlab.core.ai.policy;

import ro.whistlab.core.model.ImmutableLists;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;

import java.util.List;

/** Simple bid estimator over the acting seat's cards and the public auction state. */
public final class NormalBidPolicy {
    private NormalBidPolicy() { }

    public record Observation(int playerCount, int handSize, Trump trump, List<Card> ownHand,
            int priorBidSum, int bidsSubmitted) {
        public Observation { ownHand = ImmutableLists.copy(ownHand); }
    }

    public static Observation observe(GameSession.Snapshot game, int seat) {
        return new Observation(game.players().size(), game.handSize(), game.trump(),
                game.players().get(seat).hand(), game.bidTotal(), game.bidsSubmitted());
    }

    static int chooseBid(Observation view) {
        double expectation = view.ownHand().stream()
                .mapToDouble(card -> basicStrength(card, view.trump())).sum();
        int estimate = Math.max(0, Math.min(view.handSize(), (int) Math.round(expectation)));
        boolean lastBid = view.bidsSubmitted() == view.playerCount() - 1;
        return GameRules.allowedBids(view.handSize(), view.priorBidSum(), lastBid).stream()
                .min(java.util.Comparator.comparingInt((Integer bid) -> Math.abs(bid - estimate))
                        .thenComparingInt(Integer::intValue))
                .orElseThrow();
    }

    private static double basicStrength(Card card, Trump trump) {
        double value = switch (card.rank()) {
            case 14 -> 0.9;
            case 13 -> 0.45;
            case 12 -> 0.2;
            default -> 0.05;
        };
        return trump.suit().filter(suit -> suit == card.suit()).isPresent()
                ? Math.min(1.0, value + 0.1) : value;
    }
}

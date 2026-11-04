package ro.whistlab.core.baseline;
import ro.whistlab.core.*;

import java.util.Comparator;
import java.util.List;

public final class RoundFourteenAiPolicy {
    private RoundFourteenAiPolicy() { }

    public static int chooseBid(GameSession.Snapshot game, int seat) {
        return chooseBid(game, seat, AiDifficulty.NORMAL);
    }

    public static int chooseBid(GameSession.Snapshot game, int seat, AiDifficulty difficulty) {
        if (difficulty == AiDifficulty.HARD) return RoundFourteenMonteCarloAi.chooseBid(RoundFourteenMonteCarloAi.observe(game, seat));
        GameSession.PlayerSnapshot player = game.players().get(seat);
        double expectation = player.hand().stream()
                .mapToDouble(card -> basicStrength(card, game.trump()))
                .sum();
        int estimate = Math.max(0, Math.min(game.handSize(), (int) Math.round(expectation)));
        List<Integer> allowed = allowedBids(game);
        return allowed.stream().min(Comparator
                .comparingInt((Integer bid) -> Math.abs(bid - estimate))
                .thenComparingInt(Integer::intValue)).orElseThrow();
    }

    public static Card chooseCard(GameSession.Snapshot game, int seat) {
        return chooseCard(game, seat, AiDifficulty.NORMAL);
    }

    public static Card chooseCard(GameSession.Snapshot game, int seat, AiDifficulty difficulty) {
        if (difficulty == AiDifficulty.HARD) return RoundFourteenMonteCarloAi.chooseCard(RoundFourteenMonteCarloAi.observe(game, seat)).card();
        GameSession.PlayerSnapshot player = game.players().get(seat);
        Suit ledSuit = game.currentTrick().isEmpty() ? null : game.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(player.hand(), ledSuit, game.trump(), game.mode());
        if (legal.isEmpty()) {
            throw new IllegalStateException("AI has no legal card");
        }
        return legal.stream().min(Comparator.comparingInt(Card::rank)).orElseThrow();
    }

    private static List<Integer> allowedBids(GameSession.Snapshot game) {
        boolean lastBid = game.bidsSubmitted() == game.players().size() - 1;
        return GameRules.allowedBids(game.handSize(), game.bidTotal(), lastBid);
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

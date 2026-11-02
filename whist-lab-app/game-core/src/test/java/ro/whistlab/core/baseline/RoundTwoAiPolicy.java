package ro.whistlab.core.baseline;
import ro.whistlab.core.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class RoundTwoAiPolicy {
    private RoundTwoAiPolicy() { }

    public static int chooseBid(GameSession.Snapshot game, int seat) {
        return chooseBid(game, seat, AiDifficulty.NORMAL);
    }

    public static int chooseBid(GameSession.Snapshot game, int seat, AiDifficulty difficulty) {
        if (difficulty == AiDifficulty.EXPERT) return RoundTwoMonteCarloAi.chooseBid(RoundTwoMonteCarloAi.observe(game, seat));
        GameSession.PlayerSnapshot player = game.players().get(seat);
        double expectation = player.hand().stream()
                .mapToDouble(card -> difficulty == AiDifficulty.HARD
                        ? strength(card, game.trump()) : basicStrength(card, game.trump()))
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
        if (difficulty == AiDifficulty.EXPERT) return RoundTwoMonteCarloAi.chooseCard(RoundTwoMonteCarloAi.observe(game, seat)).card();
        GameSession.PlayerSnapshot player = game.players().get(seat);
        Suit ledSuit = game.currentTrick().isEmpty() ? null : game.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(player.hand(), ledSuit, game.trump(), game.mode());
        if (legal.isEmpty()) {
            throw new IllegalStateException("AI has no legal card");
        }
        if (difficulty == AiDifficulty.NORMAL) {
            return legal.stream().min(Comparator.comparingInt(Card::rank)).orElseThrow();
        }
        if (game.currentTrick().isEmpty()) {
            Comparator<Card> power=Comparator.comparingDouble((Card card)->strength(card,game.trump())).thenComparingInt(Card::rank);
            boolean wantsTrick=player.bid()!=null&&player.tricksWon()<player.bid()
                    ||!game.mode().isRomanian()&&player.bid()!=null&&player.tricksWon()>player.bid();
            return wantsTrick?legal.stream().max(power).orElseThrow():legal.stream().min(power).orElseThrow();
        }

        int tricksNeeded = player.bid() == null ? 0 : Math.max(0, player.bid() - player.tricksWon());
        boolean wantsTrick=tricksNeeded>0||!game.mode().isRomanian()&&player.bid()!=null&&player.tricksWon()>player.bid();
        List<Card> winningCards = legal.stream()
                .filter(card -> wouldWin(game, seat, card))
                .sorted(Comparator.comparingInt(Card::rank)).collect(Collectors.toList());
        if (wantsTrick && !winningCards.isEmpty()) {
            return winningCards.get(0);
        }
        if (!wantsTrick && winningCards.size() < legal.size()) {
            return legal.stream().filter(card -> !winningCards.contains(card))
                    .max(Comparator.comparingInt(Card::rank)).orElseThrow();
        }
        return legal.stream().min(Comparator.comparingInt(Card::rank)).orElseThrow();
    }

    private static List<Integer> allowedBids(GameSession.Snapshot game) {
        boolean lastBid = game.bidsSubmitted() == game.players().size() - 1;
        return GameRules.allowedBids(game.handSize(), game.bidTotal(), lastBid);
    }

    private static boolean wouldWin(GameSession.Snapshot game, int seat, Card card) {
        List<PlayedCard> candidate = new java.util.ArrayList<>(game.currentTrick());
        candidate.add(new PlayedCard(seat, card));
        return GameRules.winningSeat(candidate, game.trump()) == seat;
    }

    private static double strength(Card card, Trump trump) {
        double value = switch (card.rank()) {
            case 14 -> 1.0;
            case 13 -> 0.7;
            case 12 -> 0.45;
            case 11 -> 0.25;
            default -> 0.05;
        };
        return trump.suit().filter(suit -> suit == card.suit()).isPresent()
                ? Math.min(1.0, value + 0.2) : value;
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

package ro.whistlab.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A transparent quota-first card policy for Normal difficulty. */
final class NormalPlayPolicy {
    private NormalPlayPolicy() { }

    static Card chooseCard(GameSession.Snapshot game, int seat) {
        GameSession.PlayerSnapshot player = game.players().get(seat);
        List<Card> legal = GameRules.legalCards(player.hand(),
                game.currentTrick().isEmpty() ? null : game.currentTrick().get(0).card().suit(),
                game.trump(), game.mode());
        if (legal.isEmpty()) throw new IllegalStateException("AI has no legal card");
        if (legal.size() == 1) return legal.get(0);

        int need = player.bid() == null ? 0 : player.bid() - player.tricksWon();
        int opportunities = (game.players().stream().mapToInt(p -> p.hand().size()).sum()
                + game.currentTrick().size()) / game.players().size();
        List<Card> currentlyWinning = new ArrayList<>();
        List<Card> currentlyLosing = new ArrayList<>();
        for (Card card : legal) {
            List<PlayedCard> trial = new ArrayList<>(game.currentTrick());
            trial.add(new PlayedCard(seat, card));
            if (GameRules.winningSeat(trial, game.trump()) == seat) currentlyWinning.add(card);
            else currentlyLosing.add(card);
        }

        if (need > 0) {
            if (currentlyWinning.isEmpty()) return weakest(legal, game.trump());
            if (game.currentTrick().isEmpty() || need >= opportunities) {
                return strongest(currentlyWinning, game.trump());
            }
            if (game.currentTrick().size() == game.players().size() - 1) {
                return weakest(currentlyWinning, game.trump());
            }
            return strongest(currentlyWinning, game.trump());
        }

        if (!currentlyLosing.isEmpty()) return strongest(currentlyLosing, game.trump());
        return weakest(currentlyWinning, game.trump());
    }

    private static Card weakest(List<Card> cards, Trump trump) {
        return cards.stream().min(Comparator.comparingInt(card -> strength(card, trump))).orElseThrow();
    }

    private static Card strongest(List<Card> cards, Trump trump) {
        return cards.stream().max(Comparator.comparingInt(card -> strength(card, trump))).orElseThrow();
    }

    private static int strength(Card card, Trump trump) {
        return card.rank() + (trump.suit().filter(suit -> suit == card.suit()).isPresent() ? 20 : 0);
    }
}
package ro.whistlab.core.ai.policy;

import ro.whistlab.core.model.ImmutableLists;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A transparent quota-first card policy for Normal difficulty. */
public final class NormalPlayPolicy {
    private NormalPlayPolicy() { }

    record PublicSeat(int remaining, Integer bid, int tricksWon) { }

    /** The policy receives only its own hand and public table state. */
    public record Observation(GameMode mode, Trump trump, int seat, int dealer, int handSize,
            List<Card> ownHand, List<PublicSeat> seats, List<PlayedCard> trick) {
        public Observation {
            ownHand = ImmutableLists.copy(ownHand);
            seats = List.copyOf(seats);
            trick = List.copyOf(trick);
        }
    }

    public static Observation observe(GameSession.Snapshot game, int seat) {
        GameSession.PlayerSnapshot own = game.players().get(seat);
        return new Observation(game.mode(), game.trump(), seat, game.dealerSeat(), game.handSize(),
                own.hand(), game.players().stream()
                        .map(player -> new PublicSeat(player.hand().size(), player.bid(), player.tricksWon()))
                        .collect(java.util.stream.Collectors.toList()), game.currentTrick());
    }

    static Card chooseCard(Observation game) {
        List<Card> legal = GameRules.legalCards(game.ownHand(),
                game.trick().isEmpty() ? null : game.trick().get(0).card().suit(),
                game.trump(), game.mode());
        if (legal.isEmpty()) throw new IllegalStateException("AI has no legal card");
        if (legal.size() == 1) return legal.get(0);

        PublicSeat player = game.seats().get(game.seat());
        int need = player.bid() == null ? 0 : player.bid() - player.tricksWon();
        int opportunities = (game.seats().stream().mapToInt(PublicSeat::remaining).sum()
                + game.trick().size()) / game.seats().size();
        List<Card> currentlyWinning = new ArrayList<>();
        List<Card> currentlyLosing = new ArrayList<>();
        for (Card card : legal) {
            List<PlayedCard> trial = new ArrayList<>(game.trick());
            trial.add(new PlayedCard(game.seat(), card));
            if (GameRules.winningSeat(trial, game.trump()) == game.seat()) currentlyWinning.add(card);
            else currentlyLosing.add(card);
        }

        if (need > 0) {
            if (currentlyWinning.isEmpty()) return weakest(legal, game.trump());
            if (game.trick().isEmpty() || need >= opportunities) {
                return strongest(currentlyWinning, game.trump());
            }
            if (game.trick().size() == game.seats().size() - 1) {
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

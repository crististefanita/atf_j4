package ro.whistlab.core.session;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession.PlayerState;
import ro.whistlab.core.session.GameSession.SaveState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates checkpoint consistency without mutating a running session or normalizing corrupt data. */
final class CheckpointValidation {
    private CheckpointValidation() {}

    static void header(SaveState saved, List<Integer> handSizes) {
        if (saved.handIndex() < 0 || saved.handIndex() >= handSizes.size()) {
            throw new IllegalArgumentException("Saved hand index is outside the configured game");
        }
        if (saved.handSize() != handSizes.get(saved.handIndex())) {
            throw new IllegalArgumentException(
                    "Saved hand size does not match the configured game mode");
        }
        int playerCount = saved.config().playerCount();
        if (saved.players().size() != playerCount) {
            throw new IllegalArgumentException(
                    "Saved player count does not match the game configuration");
        }
        requireSeat(saved.dealerSeat(), playerCount, "dealer");
        requireSeat(saved.biddingSeat(), playerCount, "bidding");
        requireSeat(saved.currentSeat(), playerCount, "current player");
        if (saved.bidsSubmitted() < 0
                || saved.bidsSubmitted() > playerCount
                || saved.lastTrickWinnerSeat() < -1
                || saved.lastTrickWinnerSeat() >= playerCount) {
            throw new IllegalArgumentException("Saved turn counters are invalid");
        }
    }

    static void player(SaveState saved, PlayerState source, int seat) {
        if (source.seat() != seat || source.human() != (seat == 0)) {
            throw new IllegalArgumentException("Saved player seats or human seat are inconsistent");
        }
        if (source.hand().size() > saved.handSize()
                || source.tricksWon() < 0
                || source.tricksWon() > saved.handSize()
                || (source.totalScore() < 0 && !saved.config().mode().isRomanian())
                || source.streak() < -4
                || source.streak() > 4
                || source.streakPoints() != 0
                        && source.streakPoints() != -5
                        && source.streakPoints() != saved.config().streakBonusPoints()
                || !saved.config().streakBonuses()
                        && (source.streak() != 0 || source.streakPoints() != 0)
                || saved.phase() != GamePhase.SCORE && source.streakPoints() != 0
                || saved.phase() == GamePhase.SCORE
                        && saved.config().streakBonuses()
                        && (source.bid() == null
                                || source.streak() > 0 && source.tricksWon() != source.bid()
                                || source.streak() < 0 && source.tricksWon() == source.bid()
                                || source.streakPoints() != 0
                                        && (saved.handSize() == 1
                                                || source.streak() != 0
                                                || (source.streakPoints() > 0)
                                                        != (source.tricksWon() == source.bid())))
                || source.bid() != null && (source.bid() < 0 || source.bid() > saved.handSize())) {
            throw new IllegalArgumentException("Saved player state is outside the legal range");
        }
    }

    static void progress(SaveState saved, int countedBids) {
        int playerCount = saved.config().playerCount();
        if (countedBids != saved.bidsSubmitted()
                || saved.currentTrick().size() >= playerCount
                || saved.currentTrick().stream().anyMatch(play -> play.seat() >= playerCount)) {
            throw new IllegalArgumentException("Saved bid or trick state is inconsistent");
        }
        if (saved.phase() == GamePhase.BIDDING && saved.bidsSubmitted() >= playerCount
                || saved.phase() != GamePhase.BIDDING && saved.bidsSubmitted() != playerCount) {
            throw new IllegalArgumentException("Saved phase does not match bidding progress");
        }
        if (saved.phase() == GamePhase.SCORE
                && (saved.players().stream().anyMatch(player -> !player.hand().isEmpty())
                        || !saved.currentTrick().isEmpty()
                        || saved.lastTrick().size() != playerCount
                        || saved.lastTrickWinnerSeat() < 0)) {
            throw new IllegalArgumentException("Saved score screen is incomplete");
        }
    }

    static void rules(SaveState saved) {
        int n = saved.players().size();
        int submittedSum =
                saved.players().stream().filter(p -> p.bid() != null).mapToInt(p -> p.bid()).sum();
        if (submittedSum != saved.bidTotal())
            throw new IllegalArgumentException("Saved bid total is inconsistent");
        for (int i = 0; i < n; i++) {
            int seat = (saved.dealerSeat() + 1 + i) % n;
            if ((saved.players().get(seat).bid() != null) != (i < saved.bidsSubmitted()))
                throw new IllegalArgumentException("Saved bids do not follow dealer rotation");
        }
        if (saved.phase() == GamePhase.BIDDING
                && saved.biddingSeat() != (saved.dealerSeat() + 1 + saved.bidsSubmitted()) % n)
            throw new IllegalArgumentException("Saved bidding seat is inconsistent");
        int remaining = saved.players().stream().mapToInt(p -> p.hand().size()).sum();
        int won = saved.players().stream().mapToInt(p -> p.tricksWon()).sum();
        if (remaining + saved.currentTrick().size() + won * n != saved.handSize() * n)
            throw new IllegalArgumentException(
                    "Saved cards and trick counters do not conserve the deal");
        if (saved.phase() == GamePhase.BIDDING
                && (won != 0
                        || !saved.currentTrick().isEmpty()
                        || remaining != saved.handSize() * n))
            throw new IllegalArgumentException("Saved bidding state already contains card plays");
        if (saved.phase() != GamePhase.BIDDING && saved.bidTotal() == saved.handSize())
            throw new IllegalArgumentException("Saved final bid violates the contract restriction");
        Set<Card> valid = new HashSet<>(GameRules.deck(saved.config().mode(), n)),
                seen = new HashSet<>();
        if (saved.turnedCard() != null
                && (!valid.contains(saved.turnedCard())
                        || !saved.trump()
                                .suit()
                                .filter(s -> s == saved.turnedCard().suit())
                                .isPresent()))
            throw new IllegalArgumentException(
                    "Saved turned card does not determine the configured trump");
        if (saved.turnedCard() != null) seen.add(saved.turnedCard());
        for (PlayerState p : saved.players())
            for (Card c : p.hand())
                if (!valid.contains(c) || !seen.add(c))
                    throw new IllegalArgumentException(
                            "Saved hands contain an invalid or duplicate card");
        for (PlayedCard play : saved.history())
            if (play.seat() >= n || !valid.contains(play.card()) || !seen.add(play.card()))
                throw new IllegalArgumentException(
                        "Saved public history contains an invalid, duplicate or held card");
        if (saved.history().size() < saved.currentTrick().size()
                || !saved.history()
                        .subList(
                                saved.history().size() - saved.currentTrick().size(),
                                saved.history().size())
                        .equals(saved.currentTrick()))
            throw new IllegalArgumentException(
                    "Saved current trick is not the public history suffix");
        for (int i = 1; i < saved.currentTrick().size(); i++)
            if (saved.currentTrick().get(i).seat()
                    != (saved.currentTrick().get(i - 1).seat() + 1) % n)
                throw new IllegalArgumentException("Saved trick seats are out of order");
        if (saved.phase() == GamePhase.PLAYING) {
            int expected =
                    !saved.currentTrick().isEmpty()
                            ? (saved.currentTrick().get(saved.currentTrick().size() - 1).seat() + 1)
                                    % n
                            : saved.lastTrickWinnerSeat() >= 0
                                    ? saved.lastTrickWinnerSeat()
                                    : (saved.dealerSeat() + 1) % n;
            if (saved.currentSeat() != expected)
                throw new IllegalArgumentException("Saved current player is inconsistent");
        }
        if (saved.config().mode().isRomanian()
                && saved.handSize() == 8
                && (saved.trump() != Trump.NO_TRUMP || saved.turnedCard() != null))
            throw new IllegalArgumentException("Romanian eight-card hands must have no trump");
        validatePublicHistory(saved, remaining);
    }

    private static void validatePublicHistory(SaveState saved, int remaining) {
        int n = saved.players().size(), played = saved.handSize() * n - remaining;
        if (saved.history().size() > played)
            throw new IllegalArgumentException("Saved history exceeds the played-card count");
        if (!saved.lastTrick().isEmpty()) {
            Set<Card> distinct = new HashSet<>();
            if (saved.lastTrick().size() != n
                    || saved.lastTrick().stream()
                            .anyMatch(
                                    p ->
                                            p.seat() >= n
                                                    || !GameRules.deck(saved.config().mode(), n)
                                                            .contains(p.card())
                                                    || !distinct.add(p.card()))
                    || GameRules.winningSeat(saved.lastTrick(), saved.trump())
                            != saved.lastTrickWinnerSeat())
                throw new IllegalArgumentException("Saved last trick or winner is inconsistent");
        } else if (saved.lastTrickWinnerSeat() != -1)
            throw new IllegalArgumentException("Saved winner has no completed trick");
        // Version-1 saves may retain only the last/current trick. Full history allows a stronger
        // audit.
        if (saved.history().size() != played) return;
        List<List<Card>> hands = new ArrayList<>();
        for (PlayerState p : saved.players()) hands.add(new ArrayList<>(p.hand()));
        for (PlayedCard p : saved.history()) hands.get(p.seat()).add(p.card());
        if (hands.stream().anyMatch(h -> h.size() != saved.handSize()))
            throw new IllegalArgumentException(
                    "Saved card ownership does not conserve each original hand");
        int actor = (saved.dealerSeat() + 1) % n;
        int[] winners = new int[n];
        List<PlayedCard> round = new ArrayList<>();
        List<PlayedCard> completed = java.util.Collections.emptyList();
        int winner = -1;
        for (PlayedCard p : saved.history()) {
            Suit led = round.isEmpty() ? null : round.get(0).card().suit();
            if (p.seat() != actor
                    || !GameRules.legalCards(
                                    hands.get(actor), led, saved.trump(), saved.config().mode())
                            .contains(p.card()))
                throw new IllegalArgumentException(
                        "Saved public history violates turn order, suit or mandatory trump");
            hands.get(actor).remove(p.card());
            round.add(p);
            if (round.size() == n) {
                winner = GameRules.winningSeat(round, saved.trump());
                winners[winner]++;
                actor = winner;
                completed = new ArrayList<>(round);
                round.clear();
            } else actor = (actor + 1) % n;
        }
        for (PlayerState p : saved.players())
            if (p.tricksWon() != winners[p.seat()])
                throw new IllegalArgumentException(
                        "Saved trick counters disagree with public winners");
        if (!round.equals(saved.currentTrick())
                || (!round.isEmpty()
                        && (!saved.lastTrick().isEmpty() || saved.lastTrickWinnerSeat() != -1))
                || (round.isEmpty()
                        && (!saved.lastTrick().equals(completed)
                                || saved.lastTrickWinnerSeat() != winner)))
            throw new IllegalArgumentException(
                    "Saved last/current trick disagrees with public history");
    }

    private static void requireSeat(int seat, int playerCount, String name) {
        if (seat < 0 || seat >= playerCount) {
            throw new IllegalArgumentException("Saved " + name + " seat is outside the table");
        }
    }
}

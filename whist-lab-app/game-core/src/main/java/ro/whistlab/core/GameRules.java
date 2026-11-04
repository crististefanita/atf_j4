package ro.whistlab.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GameRules {
    private GameRules() { }

    public static List<Integer> handSizes(GameMode mode, int playerCount) {
        Objects.requireNonNull(mode, "mode");
        if (playerCount < 3 || playerCount > 6) {
            throw new IllegalArgumentException("Player count must be between 3 and 6");
        }
        if (mode.isRomanian()) {
            List<Integer> sizes = new ArrayList<>();
            for (int i = 0; i < playerCount; i++) sizes.add(1);
            for (int size = 2; size <= 7; size++) sizes.add(size);
            for (int i = 0; i < playerCount; i++) sizes.add(8);
            for (int size = 7; size >= 2; size--) sizes.add(size);
            for (int i = 0; i < playerCount; i++) sizes.add(1);
            if (mode == GameMode.LONG_ROMANIAN) {
                sizes.replaceAll(size -> 9 - size);
            }
            return immutableCopy(sizes);
        }
        int maximum = mode == GameMode.OH_HELL || mode == GameMode.REVERSE_OH_HELL
                ? Math.min(10, 52 / playerCount) : 52 / playerCount;
        List<Integer> sizes = new ArrayList<>(maximum);
        for (int size = maximum; size >= 1; size--) {
            sizes.add(size);
        }
        if (mode == GameMode.OH_HELL || mode == GameMode.DOWN_AND_UP || mode == GameMode.REVERSE_OH_HELL) {
            for (int size = 2; size <= maximum; size++) sizes.add(size);
        }
        if (mode == GameMode.REVERSE_OH_HELL) sizes.replaceAll(size -> maximum + 1 - size);
        return immutableCopy(sizes);
    }

    public static List<Card> standardDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (int rank = 2; rank <= 14; rank++) {
                deck.add(new Card(suit, rank));
            }
        }
        return immutableCopy(deck);
    }

    public static List<Card> deck(GameMode mode, int playerCount) {
        // Validates the mode and table size even for modes using the full deck.
        handSizes(mode, playerCount);
        if (!mode.isRomanian()) return standardDeck();
        int minimumRank = 15 - 2 * playerCount;
        return immutableCopy(standardDeck().stream().filter(card -> card.rank() >= minimumRank)
                .collect(Collectors.toList()));
    }

    /** Romanian Whist requires trump only after the led suit has been exhausted. */
    public static List<Card> legalCards(List<Card> hand, Suit ledSuit, Trump trump, GameMode mode) {
        Objects.requireNonNull(trump, "trump");
        Objects.requireNonNull(mode, "mode");
        List<Card> following = legalCards(hand, ledSuit);
        if (!mode.isRomanian() || ledSuit == null
                || hand.stream().anyMatch(card -> card.suit() == ledSuit)) return following;
        List<Card> trumps = hand.stream().filter(card -> trump.suit().filter(s -> s == card.suit()).isPresent())
                .collect(Collectors.toList());
        return trumps.isEmpty() ? following : immutableCopy(trumps);
    }

    public static List<Card> legalCards(List<Card> hand, Suit ledSuit) {
        Objects.requireNonNull(hand, "hand");
        if (ledSuit == null) {
            return immutableCopy(hand);
        }
        List<Card> followingSuit = hand.stream().filter(card -> card.suit() == ledSuit)
                .collect(Collectors.toList());
        return followingSuit.isEmpty() ? immutableCopy(hand) : immutableCopy(followingSuit);
    }

    public static int winningSeat(List<PlayedCard> trick, Trump trump) {
        Objects.requireNonNull(trick, "trick");
        Objects.requireNonNull(trump, "trump");
        if (trick.isEmpty()) {
            throw new IllegalArgumentException("A trick must contain at least one card");
        }
        Suit leadSuit = trick.get(0).card().suit();
        Suit trumpSuit = trump.suit().orElse(null);
        Comparator<PlayedCard> strength = Comparator
                .comparingInt((PlayedCard play) -> category(play.card().suit(), leadSuit, trumpSuit))
                .thenComparingInt(play -> play.card().rank());
        return trick.stream().max(strength).orElseThrow().seat();
    }

    private static int category(Suit suit, Suit leadSuit, Suit trumpSuit) {
        if (trumpSuit != null && suit == trumpSuit) {
            return 2;
        }
        if (suit == leadSuit) {
            return 1;
        }
        return 0;
    }

    public static int scoreDelta(int tricksWon, int bid) {
        if (tricksWon < 0 || bid < 0) {
            throw new IllegalArgumentException("Tricks won and bid cannot be negative");
        }
        return tricksWon + (tricksWon == bid ? 10 : 0);
    }

    public static int scoreDelta(int tricksWon, int bid, GameMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (!mode.isRomanian()) return scoreDelta(tricksWon, bid);
        if (tricksWon < 0 || bid < 0 || tricksWon > 8 || bid > 8)
            throw new IllegalArgumentException("Romanian tricks and bids must be between 0 and 8");
        return tricksWon == bid ? 5 + tricksWon : -Math.abs(tricksWon - bid);
    }

    public static boolean isForbiddenLastBid(int existingBidTotal, int candidateBid, int handSize) {
        if (existingBidTotal < 0 || candidateBid < 0 || handSize < 1) {
            throw new IllegalArgumentException("Bid totals must be non-negative and hand size must be positive");
        }
        return existingBidTotal + candidateBid == handSize;
    }

    public static List<Integer> allowedBids(int handSize, int existingBidTotal, boolean lastBid) {
        if (handSize < 1 || existingBidTotal < 0) {
            throw new IllegalArgumentException("Hand size must be positive and existing bids non-negative");
        }
        return java.util.stream.IntStream.rangeClosed(0, handSize)
                .filter(bid -> !lastBid || !isForbiddenLastBid(existingBidTotal, bid, handSize))
                .boxed().collect(Collectors.toList());
    }

    static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}

package ro.cardtable.ui.animation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Public card positions in play order and the winning seat supplied by the game. */
public record CollectionTarget(int winner, List<Integer> seats) {
    public static Optional<CollectionTarget> complete(int seatCount, int winner, List<Integer> seats) {
        if (seats.size() != seatCount) return Optional.empty();
        try {
            return Optional.of(new CollectionTarget(winner, seats));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public CollectionTarget {
        seats = List.copyOf(seats);
        int count = seats.size();
        if (count < 3 || count > 6 || winner < 0 || winner >= count
                || new HashSet<>(seats).size() != count
                || seats.stream().anyMatch(seat -> seat < 0 || seat >= count))
            throw new IllegalArgumentException("A collection needs every seat and its winning actor");
    }
}

package ro.cardtable.ui.animation;

import java.util.HashSet;
import java.util.List;

/** Public card positions in play order and the winning seat supplied by the game. */
public record CollectionTarget(int winner, List<Integer> seats) {
    public CollectionTarget {
        seats = List.copyOf(seats);
        int count = seats.size();
        if (count < 3 || count > 6 || winner < 0 || winner >= count
                || new HashSet<>(seats).size() != count
                || seats.stream().anyMatch(seat -> seat < 0 || seat >= count))
            throw new IllegalArgumentException("A collection needs every seat and its winning actor");
    }
}

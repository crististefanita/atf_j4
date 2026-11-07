package ro.whistlab.core.player;

import java.util.List;

/** Short display names; their random stream never participates in dealing or AI sampling. */
public final class PlayerNames {
    public static final int MAX_LENGTH = ro.cardtable.names.PlayerNames.MAX_LENGTH;
    public static final String DEFAULT_HUMAN = ro.cardtable.names.PlayerNames.DEFAULT_HUMAN;
    private PlayerNames() { }

    public static String normalize(String value) {
        return ro.cardtable.names.PlayerNames.normalize(value);
    }

    public static List<String> forMatch(int seats, String humanName, long seed) {
        return ro.cardtable.names.PlayerNames.forMatch(seats, humanName, seed);
    }
}

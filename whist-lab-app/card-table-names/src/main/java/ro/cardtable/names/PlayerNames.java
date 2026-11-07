package ro.cardtable.names;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Short display names; their random stream never participates in dealing or AI sampling. */
public final class PlayerNames {
    public static final int MAX_LENGTH = 10;
    public static final String DEFAULT_HUMAN = "You";
    private static final List<String> POOL = List.of("Ana", "Mara", "Radu", "Iris", "Luca", "Nora",
            "Teo", "Ion", "Vlad", "Dora", "Emil", "Dinu", "Alex", "Oana", "Lia", "Eva", "Paul", "Toma");
    private PlayerNames() { }

    public static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("Name is required");
        String name = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        if (name.isEmpty() || name.codePointCount(0, name.length()) > MAX_LENGTH
                || name.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c) && c != ' ' && c != '-' && c != '\'')) {
            throw new IllegalArgumentException("Use 1–10 letters, digits, spaces, apostrophes or hyphens");
        }
        return name;
    }

    public static List<String> forMatch(int seats, String humanName, long seed) {
        if (seats < 3 || seats > 6) throw new IllegalArgumentException("Expected 3–6 players");
        String human = normalize(humanName);
        var candidates = new ArrayList<>(POOL);
        candidates.removeIf(name -> name.toLowerCase(Locale.ROOT).equals(human.toLowerCase(Locale.ROOT)));
        Collections.shuffle(candidates, new Random(seed ^ 0x4e414d45535f4149L));
        var result = new ArrayList<String>();
        result.add(human);
        result.addAll(candidates.subList(0, seats - 1));
        return Collections.unmodifiableList(result);
    }
}

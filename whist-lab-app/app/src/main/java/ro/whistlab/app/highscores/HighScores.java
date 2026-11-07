package ro.whistlab.app.highscores;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.core.rules.GameMode;

/** Local rankings. Demo entries are explicit data, never evidence of a played game. */
public final class HighScores {
    public static final int CAPACITY = 20;
    public static final int INITIAL_SAMPLES = 10;
    static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::score).reversed()
            .thenComparing(Entry::sample).thenComparingLong(Entry::recordedAt)
            .thenComparing(Entry::matchId).thenComparingInt(Entry::seat);

    public record Profile(GameMode mode, int players, AiDifficulty difficulty,
            boolean series, int reward, int penalty, boolean allowZeroGame) {
        public Profile(GameMode mode, int players, AiDifficulty difficulty, boolean series, int reward, int penalty) {
            this(mode, players, difficulty, series, reward, penalty, false);
        }
        public Profile { new GameConfig(players, mode, difficulty, 0, series, reward, penalty, allowZeroGame); }
        public static Profile from(GameConfig config) {
            return new Profile(config.mode(), config.playerCount(), config.aiDifficulty(),
                    config.streakBonuses(), config.streakBonusPoints(), config.streakPenaltyPoints(),
                    config.allowZeroGame());
        }
        public String key() {
            return mode.name() + ":" + players + ":" + difficulty.name() + ":"
                    + (series ? reward + ":" + penalty : "off") + (allowZeroGame ? ":zero" : "");
        }
    }

    public record Entry(String name, int score, boolean ai, boolean sample,
            String matchId, int seat, long recordedAt) {
        public Entry {
            name = PlayerNames.normalize(name);
            if (matchId == null || matchId.isBlank() || seat < 0 || seat > 5 || recordedAt < 0)
                throw new IllegalArgumentException("Invalid ranking identity");
        }
    }

    public record Board(Profile profile, List<Entry> entries, Set<String> processedMatches) {
        public Board {
            entries = List.copyOf(entries);
            processedMatches = Set.copyOf(processedMatches);
            if (entries.size() > CAPACITY || processedMatches.stream().anyMatch(String::isBlank)
                    || entries.stream().anyMatch(entry -> entry.seat() >= profile.players()))
                throw new IllegalArgumentException("Invalid ranking board");
            var identities = new java.util.HashSet<String>();
            for (Entry entry : entries) if (!identities.add(entry.matchId() + ":" + entry.seat()))
                throw new IllegalArgumentException("Duplicate ranking entry");
        }
    }

    final Map<String, Board> boards = new LinkedHashMap<>();

    static HighScores initial(Random random) {
        var scores = new HighScores();
        for (GameMode mode : GameMode.values()) {
            scores.ensure(new Profile(mode, 4, AiDifficulty.NORMAL, mode.isRomanian(), 10, 10), random);
        }
        return scores;
    }

    Board ensure(Profile profile, Random random) {
        Board existing = boards.get(profile.key());
        if (existing != null) return existing;
        var entries = new ArrayList<Entry>();
        for (int index = 0; index < INITIAL_SAMPLES; index++) {
            String name = PlayerNames.forMatch(profile.players(), "You", random.nextLong()).get(1);
            entries.add(new Entry(name, random.nextInt(120), true, true, "sample-" + index, 1, 0));
        }
        entries.sort(ORDER);
        Board created = new Board(profile, entries, Set.of());
        boards.put(profile.key(), created);
        return created;
    }

    boolean record(Profile profile, String matchId, List<Entry> results, Random random) {
        Board board = ensure(profile, random);
        if (board.processedMatches().contains(matchId)) return false;
        var entries = new ArrayList<>(board.entries());
        entries.addAll(results);
        entries.sort(ORDER);
        if (entries.size() > CAPACITY) entries.subList(CAPACITY, entries.size()).clear();
        var processed = new LinkedHashSet<>(board.processedMatches());
        processed.add(matchId);
        boards.put(profile.key(), new Board(profile, entries, processed));
        return true;
    }
}

package ro.whistlab.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import java.nio.file.Path;
import ro.cardtable.browser.BrowserGame;
import ro.cardtable.browser.BrowserExperience;
import ro.cardtable.browser.BrowserServer;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.cardtable.ui.theme.TableColors;
import ro.whistlab.app.achievements.WhistAchievementRepository.Storage;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.table.TableLayoutMetrics;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;

public final class WhistBrowser implements BrowserGame {
    private static final WhistUiText COPY = new WhistUiText(HowToPlay.Language.RO);
    private final LongSupplier seeds;
    private final Path storage;

    public WhistBrowser(LongSupplier seeds) { this(seeds, null); }

    public static BrowserExperience localProfile(Storage name, Storage progress) {
        return new WhistBrowserProfile(name, progress);
    }

    private WhistBrowser(LongSupplier seeds, Path storage) {
        this.seeds = seeds;
        this.storage = storage;
    }

    public static void main(String[] args) throws Exception {
        var storage = args.length == 4 ? Path.of(args[3]) : null;
        BrowserServer.launch(new WhistBrowser(RandomGenerator.getDefault()::nextLong, storage),
                args.length == 4 ? java.util.Arrays.copyOf(args, 3) : args);
    }

    @Override public String title() { return "Whist Lab"; }
    @Override public String surface() { return "whist"; }
    @Override public List<Token> palette() {
        return List.of(color("paper", TableColors.BACKGROUND), color("surface", TableColors.SURFACE),
                color("pine", TableColors.PINE), color("accent", TableColors.ACCENT),
                color("felt", TableColors.TABLE_FELT), color("seat-fill", TableColors.IDLE_SEAT_FILL),
                color("seat-line", TableColors.IDLE_SEAT_STROKE), color("seat-text", TableColors.IDLE_SEAT_TEXT),
                color("turn-fill", TableColors.ACTIVE_SEAT_FILL), color("turn-line", TableColors.ACTIVE_SEAT_STROKE),
                color("turn-text", TableColors.ACTIVE_SEAT_TEXT));
    }

    private static Token color(String name, int argb) {
        return new Token("--whist-" + name, "#" + Integer.toHexString(argb).substring(2));
    }
    @Override public BoardProfile boardProfile(String mode, int players, int width, TableViewportPolicy viewport) {
        var reservation = TableLayoutMetrics.matchCenterReservation(GameMode.valueOf(mode), players, width, 1f, viewport);
        return new BoardProfile(reservation.reservedHeightDp(), TableLayoutMetrics.HUMAN_HAND_CLEARANCE_DP,
                reservation.playedCards());
    }
    @Override public boolean hasProfile() { return true; }
    @Override public ro.cardtable.browser.BrowserExperience openProfile(String client) {
        return new WhistBrowserProfile(storage, client);
    }

    @Override public List<Field> fields() {
        return List.of(new Field("name", "Numele tău", "Tu", List.of()),
                field("players", COPY.players(), "4", "3", "3", "4", "4", "5", "5", "6", "6"),
                new Field("mode", "Varianta de joc", GameMode.ROMANIAN_WHIST.name(),
                        WhistUiText.MENU_MODES.stream().map(mode -> new Option(mode.name(), COPY.modeName(mode))).toList()),
                new Field("difficulty", COPY.aiLevel(), AiDifficulty.NORMAL.name(),
                        java.util.Arrays.stream(AiDifficulty.values())
                                .map(level -> new Option(level.name(), COPY.difficultyName(level))).toList()),
                field("series", "Bonus / penalizare după cinci mâini", "10", "0", "Dezactivat", "5", "5", "10", "10"),
                field("zero", "Permite joc la 0", "yes", "yes", "Da", "no", "Nu"));
    }

    @Override public Match start(Map<String, String> config) {
        var mode = GameMode.valueOf(config.get("mode"));
        int points = Integer.parseInt(config.get("series"));
        var game = GameConfig.newMatch(Integer.parseInt(config.get("players")), mode,
                AiDifficulty.valueOf(config.get("difficulty")), seeds.getAsLong(),
                mode.isRomanian() && points > 0, points == 0 ? 10 : points,
                mode.isRomanian() && config.get("zero").equals("yes"));
        return new WhistMatch(GameSession.newGame(game, config.get("name")));
    }

    private static Field field(String key, String label, String initial, String... pairs) {
        var options = new ArrayList<Option>();
        for (int i = 0; i < pairs.length; i += 2) options.add(new Option(pairs[i], pairs[i + 1]));
        return new Field(key, label, initial, options);
    }
}

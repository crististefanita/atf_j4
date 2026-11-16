package ro.whistlab.browser;

import static java.util.Map.entry;

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
import ro.cardtable.ui.turn.TurnPacing;
import ro.whistlab.app.achievements.WhistAchievementRepository.Storage;
import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.table.TableLayoutMetrics;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;

public final class WhistBrowser implements BrowserGame {
    static WhistUiText copy(String language) {
        return new WhistUiText("en".equals(language) ? HowToPlay.Language.EN : HowToPlay.Language.RO);
    }
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
    @Override public BoardProfile boardProfile(String mode, int players, int width, int bidOptions,
            TableViewportPolicy viewport) {
        var reservation = TableLayoutMetrics.matchCenterReservation(GameMode.valueOf(mode), players, width, 1f, viewport);
        return new BoardProfile(reservation.center(width, 1f, bidOptions > 0, bidOptions), TableLayoutMetrics.HUMAN_HAND_CLEARANCE_DP,
                reservation.playedCards(), bidOptions == 0 ? null : bidControls(width, bidOptions, reservation.useBidStrip()));
    }
    private static BidControls bidControls(int width, int options, boolean strip) {
        var inset = BidGridMetrics.submitInsets(1f, 0, 0);
        return new BidControls(BidGridMetrics.columnCount(options, width), strip,
                BidGridMetrics.CHOICE_TARGET_DP, BidGridMetrics.CHOICE_ROW_DP, BidGridMetrics.CHOICE_FACE_INSET_DP,
                BidGridMetrics.SUBMIT_WIDTH_DP, BidGridMetrics.SUBMIT_TARGET_DP,
                inset.horizontalPx(), inset.verticalPx(), BidGridMetrics.SUBMIT_TEXT_SP);
    }

    @Override public boolean hasProfile() { return true; }
    @Override public ro.cardtable.browser.BrowserExperience openProfile(String client) {
        return new WhistBrowserProfile(storage, client);
    }

    @Override public int autoDelayMillis() { return (int) TurnPacing.ACTION_DELAY_MILLIS; }

    @Override public Map<String, Object> interfaceCopy(String language) {
        var text = copy(language);
        boolean ro = text.romanian();
        return Map.ofEntries(entry("language", text.languageLabel()), entry("home", text.homeTitle()),
                entry("newGame", text.newGame()), entry("resume", text.resumeGame()),
                entry("setup", text.setupTitle()), entry("start", text.startGame()), entry("back", text.back()),
                entry("name", text.playerSettings()), entry("save", text.save()), entry("reset", text.resetName()),
                entry("cancel", ro ? "Anulează" : "Cancel"), entry("nameHint", text.nameHint()),
                entry("nameError", text.nameError()), entry("achievements", text.achievements()),
                entry("progress", ro ? "Progresul realizărilor" : "Achievement progress"),
                entry("close", text.close()), entry("help", text.help()),
                entry("rules", HowToPlay.sections(text.language()).stream()
                        .map(section -> Map.of("title", section.title(), "body", section.explanation())).toList()),
                entry("streaks", ro ? "Serii de cinci mâini" : "Five-hand streaks"),
                entry("bid", text.placeBid()), entry("history", text.scoreHistory()),
                entry("mainMenu", text.mainMenu()), entry("gameMenu", text.gameMenu()),
                entry("trump", text.trumpHeading()), entry("hand", ro ? "Cărțile tale" : "Your cards"),
                entry("busy", ro ? "O comandă este deja în curs." : "A command is already running."));
    }

    @Override public List<Field> fields() { return fields("ro"); }

    @Override public List<Field> fields(String language) {
        var text = copy(language);
        boolean ro = text.romanian();
        return List.of(new Field("name", text.playerSettings(), "Tu", List.of()),
                field("players", text.players(), "4", "3", "3", "4", "4", "5", "5", "6", "6"),
                new Field("mode", text.gameType(), GameMode.ROMANIAN_WHIST.name(),
                        WhistUiText.MENU_MODES.stream().map(mode -> new Option(mode.name(), text.modeName(mode))).toList()),
                new Field("difficulty", text.aiLevel(), AiDifficulty.NORMAL.name(),
                        java.util.Arrays.stream(AiDifficulty.values())
                                .map(level -> new Option(level.name(), text.difficultyName(level))).toList()),
                field("series", text.streakBonusLabel(), "10", "0", ro ? "Dezactivat" : "Off", "5", "5", "10", "10"),
                field("zero", text.zeroGameToggle(), "yes", "yes", ro ? "Da" : "Yes", "no", ro ? "Nu" : "No"));
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

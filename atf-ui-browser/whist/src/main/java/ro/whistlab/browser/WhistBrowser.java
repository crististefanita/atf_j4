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
import ro.cardtable.ui.table.ScoreGridStyle;
import ro.cardtable.ui.table.PlayedCardStyle;
import ro.cardtable.ui.table.TrumpTileMetrics;
import ro.cardtable.ui.cards.CardHighlight;
import ro.cardtable.ui.cards.CardFaceMetrics;
import ro.cardtable.ui.hand.HandLayoutMetrics;
import ro.cardtable.ui.theme.TableColors;
import ro.cardtable.ui.turn.TurnPacing;
import ro.whistlab.app.achievements.WhistAchievementRepository.Storage;
import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.table.TableLayoutMetrics;
import ro.whistlab.app.theme.TrumpTileStyle;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.app.screens.MatchSetup;
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

    public static BrowserExperience localProfile(Storage name, Storage progress,
            ro.whistlab.app.highscores.HighScoreRepository.Storage rankings) {
        return new WhistBrowserProfile(name, progress, rankings);
    }

    public Match restore(String checkpoint) { return WhistMatch.restore(checkpoint, seeds); }

    public String checkpoint(Match match) { return ((WhistMatch) match).checkpoint(); }

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
        var tokens = new ArrayList<>(List.of(color("paper", TableColors.BACKGROUND), color("surface", TableColors.SURFACE),
                color("pine", TableColors.PINE), color("accent", TableColors.ACCENT),
                color("felt", TableColors.TABLE_FELT), color("seat-fill", TableColors.IDLE_SEAT_FILL),
                color("seat-line", TableColors.IDLE_SEAT_STROKE), color("seat-text", TableColors.IDLE_SEAT_TEXT),
                color("turn-fill", TableColors.ACTIVE_SEAT_FILL), color("turn-line", TableColors.ACTIVE_SEAT_STROKE),
                color("turn-text", TableColors.ACTIVE_SEAT_TEXT),
                color("trump-fill", TrumpTileStyle.FILL), color("trump-line", TrumpTileStyle.LINE),
                color("trump-ink", TrumpTileStyle.INK), color("trump-card-fill", TrumpTileStyle.CARD_FILL),
                color("trump-red-ink", TrumpTileStyle.RED_INK),
                color("score-paper", ScoreGridStyle.PAPER), color("score-stripe", ScoreGridStyle.STRIPE),
                color("score-human", ScoreGridStyle.HUMAN_ROW), color("score-header", ScoreGridStyle.HEADER),
                color("score-rule", ScoreGridStyle.RULE), color("score-ink", ScoreGridStyle.INK),
                color("score-muted", ScoreGridStyle.MUTED),
                color("hint-rejected-fill", CardHighlight.REJECTED.fill()),
                color("hint-rejected-line", CardHighlight.REJECTED.outline()),
                color("hint-legal-fill", CardHighlight.IN_PLACE.fill()),
                color("hint-legal-line", CardHighlight.IN_PLACE.outline()),
                color("played-fill", PlayedCardStyle.FILL), color("played-line", PlayedCardStyle.OUTLINE),
                color("played-red", PlayedCardStyle.RED), color("played-dark", PlayedCardStyle.DARK),
                color("played-player", PlayedCardStyle.PLAYER)));
        tokens.add(new Token("--whist-hand-label", HandLayoutMetrics.cardLabelSizeSp() / 16.0 + "rem"));
        tokens.add(new Token("--whist-hand-rank-cap", "26px"));
        tokens.add(new Token("--whist-face-index-top", CardFaceMetrics.INDEX_TOP_DP + "px"));
        tokens.add(new Token("--whist-face-index-side", CardFaceMetrics.INDEX_SIDE_DP + "px"));
        tokens.add(new Token("--whist-face-suit-baseline", Math.round(CardFaceMetrics.CENTER_SUIT_BASELINE * 100) + "%"));
        tokens.add(new Token("--whist-face-suit-size", CardFaceMetrics.CENTER_SUIT_SIZE_DP + "px"));
        tokens.add(new Token("--whist-face-inverted-size", CardFaceMetrics.INVERTED_SIZE_DP + "px"));
        tokens.add(new Token("--whist-menu-touch", "48px"));
        tokens.add(new Token("--whist-title-gap", "8px"));
        tokens.add(new Token("--whist-summary-inset", "1px"));
        tokens.add(new Token("--whist-summary-leading", ".9375rem"));
        tokens.add(new Token("--whist-summary-whitespace", "pre"));
        tokens.add(new Token("--whist-trump-frame-stroke", TrumpTileStyle.FRAME_STROKE_DP + "px"));
        tokens.add(new Token("--whist-trump-frame-radius", TrumpTileStyle.FRAME_RADIUS_DP + "px"));
        tokens.add(new Token("--whist-trump-width", TrumpTileMetrics.WIDTH_DP + "px"));
        tokens.add(new Token("--whist-trump-min-height", TrumpTileMetrics.MIN_HEIGHT_DP + "px"));
        tokens.add(new Token("--whist-trump-padding", TrumpTileMetrics.PADDING_DP + "px"));
        tokens.add(new Token("--whist-trump-gap", TrumpTileMetrics.VALUE_GAP_DP + "px"));
        tokens.add(new Token("--whist-trump-heading-size", TrumpTileMetrics.HEADING_SP / 16.0 + "rem"));
        tokens.add(new Token("--whist-trump-value-size", TrumpTileMetrics.VALUE_SP / 16.0 + "rem"));
        tokens.add(new Token("--whist-trump-card-inset", TrumpTileMetrics.CARD_INSET_DP + "px"));
        tokens.add(new Token("--whist-trump-card-radius", TrumpTileMetrics.CARD_RADIUS_DP + "px"));
        tokens.add(new Token("--whist-played-radius", PlayedCardStyle.RADIUS_DP + "px"));
        tokens.add(new Token("--whist-played-stroke", PlayedCardStyle.STROKE_DP + "px"));
        tokens.add(new Token("--whist-hint-stroke", CardHighlight.IN_PLACE.strokeDp() + "px"));
        double total = ScoreGridStyle.COLUMN_WEIGHTS.stream().mapToDouble(Float::doubleValue).sum();
        for (int index = 0; index < ScoreGridStyle.COLUMN_WEIGHTS.size(); index++) {
            tokens.add(new Token("--whist-score-col-" + index,
                    (ScoreGridStyle.COLUMN_WEIGHTS.get(index) * 100 / total) + "%"));
        }
        tokens.add(new Token("--whist-hand-face-width", HandLayoutMetrics.CARD_WIDTH_DP + "px"));
        tokens.add(new Token("--whist-hand-face-height", HandLayoutMetrics.CARD_HEIGHT_DP + "px"));
        tokens.add(new Token("--whist-hand-slot", (HandLayoutMetrics.CARD_HEIGHT_DP + HandLayoutMetrics.LIFT_CLEARANCE_DP) + "px"));
        tokens.add(new Token("--whist-score-head", ScoreGridStyle.HEADER_DP + "px"));
        tokens.add(new Token("--whist-score-row", ScoreGridStyle.ROW_DP + "px"));
        return List.copyOf(tokens);
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
                entry("instructions", HowToPlay.title(text.language())),
                entry("instructionsSubtitle", ro ? "Whist românesc — reguli și exemple" : "Romanian Whist — rules and examples"),
                entry("rulesAndSchedule", text.rulesAndSchedule()), entry("modeRules", modeRules(text)),
                entry("scoringDetails", text.scoringDetails()), entry("scoreSheet", text.scoreSheet()),
                entry("stopAutoGame", text.stopAutoGameHand()),
                entry("bidNow", text.seatTurnBadge(true)), entry("playNow", text.seatTurnBadge(false)),
                entry("rules", HowToPlay.sections(text.language()).stream()
                        .map(section -> Map.of("title", section.title(), "body", section.explanation())).toList()),
                entry("streaks", ro ? "Serii de cinci mâini" : "Five-hand streaks"),
                entry("bid", text.placeBid()), entry("history", text.scoreHistory()),
                entry("mainMenu", text.mainMenu()), entry("gameMenu", text.gameMenu()),
                entry("trump", text.trumpHeading()), entry("hand", ro ? "Cărțile tale" : "Your cards"),
                entry("savedGameError", text.noSavedGame()), entry("storageError", text.storageError()),
                entry("ranking", text.highestScores()), entry("rankingHint", text.highScoreHint()),
                entry("gameType", text.gameType()), entry("busy", ro ? "O comandă este deja în curs." : "A command is already running."));
    }

    private static Map<String, Object> modeRules(WhistUiText copy) {
        var rules = new java.util.LinkedHashMap<String, Object>();
        for (var mode : WhistUiText.MENU_MODES) {
            var schedule = new java.util.LinkedHashMap<String, String>();
            for (int players = 3; players <= 6; players++) {
                schedule.put(Integer.toString(players), copy.schedulePreview(mode, players));
            }
            rules.put(mode.name(), Map.of("title", copy.modeName(mode), "description", copy.modeDescription(mode),
                    "schedule", schedule, "romanian", mode.isRomanian(),
                    "extras", mode.isRomanian() ? List.of(copy.streakToggle(), copy.zeroGameRule()) : List.of()));
        }
        return rules;
    }

    @Override public List<Field> fields() { return fields("ro"); }

    @Override public List<Field> fields(String language) {
        var text = copy(language);
        boolean ro = text.romanian();
        var defaults = new MatchSetup();
        return List.of(new Field("name", text.playerSettings(), "Tu", List.of()),
                field("players", text.players(), Integer.toString(defaults.playerCount()), "3", "3", "4", "4", "5", "5", "6", "6"),
                new Field("mode", text.gameType(), defaults.mode().name(),
                        WhistUiText.MENU_MODES.stream().map(mode -> new Option(mode.name(), text.modeName(mode))).toList()),
                new Field("difficulty", text.aiLevel(), defaults.difficulty().name(),
                        java.util.Arrays.stream(AiDifficulty.values())
                                .map(level -> new Option(level.name(), text.difficultyName(level))).toList()),
                field("series", text.streakBonusLabel(), Integer.toString(defaults.streakBonus()),
                        "0", ro ? "Dezactivat" : "Off", "5", text.fivePointsOriginal(), "10", text.tenPoints()),
                field("streaks", text.streakToggle(), defaults.streaks() ? "yes" : "no",
                        "yes", ro ? "Da" : "Yes", "no", ro ? "Nu" : "No"),
                field("zero", text.zeroGameToggle(), defaults.allowZeroGame() ? "yes" : "no", "yes", ro ? "Da" : "Yes", "no", ro ? "Nu" : "No"));
    }

    @Override public Match start(Map<String, String> config) {
        var options = new MatchSetup();
        options.playerCount(Integer.parseInt(config.get("players")));
        options.mode(GameMode.valueOf(config.get("mode")));
        options.difficulty(AiDifficulty.valueOf(config.get("difficulty")));
        int points = Integer.parseInt(config.get("series"));
        options.streaks(points > 0 && !"no".equals(config.get("streaks")));
        if (points > 0) options.streakBonus(points);
        options.allowZeroGame(config.get("zero").equals("yes"));
        return new WhistMatch(GameSession.newGame(options.config(seeds.getAsLong()), config.get("name")), seeds);
    }

    private static Field field(String key, String label, String initial, String... pairs) {
        var options = new ArrayList<Option>();
        for (int i = 0; i < pairs.length; i += 2) options.add(new Option(pairs[i], pairs[i + 1]));
        return new Field(key, label, initial, options);
    }
}

package ro.whistlab.app.achievements;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catalogul stabil al realizărilor Whist; nivelul descrie dificultatea obiectivului, nu AI-ul. */
public final class WhistAchievementCatalog {
    public enum Level { EASY, MEDIUM, HARD }
    public enum Aggregation { SUM, BEST_MATCH }
    public enum Metric {
        COMPLETED_MATCHES, EXACT_CONTRACTS, MATCH_WINS,
        EXACT_ONE_CARD, EXACT_ZERO_LARGE, EXACT_SMALL_RATE, EXACT_MIDDLE_RATE, EXACT_LARGE_RATE,
        EXACT_STREAK, EXACT_MATCH_RATE, EXACT_HAND_SIZE_COUNT, EXACT_FINAL_HAND,
        EXACT_FINAL_THREE, EXACT_FINAL_FIVE,
        SERIES_ENABLED_MATCHES, POSITIVE_SERIES_AWARDS, POSITIVE_SERIES_MATCHES,
        WIN_WITH_SERIES_AWARD,
        SHORT_ROMANIAN_MATCHES, LONG_ROMANIAN_MATCHES,
        SHORT_ROMANIAN_WINS, LONG_ROMANIAN_WINS, ZERO_GAME_MATCHES,
        PLAYER_COUNT_THREE, PLAYER_COUNT_SIX, TOP_TWO_AT_FIVE, TOP_TWO_AT_SIX, WINS_AT_SIX,
        HOLD_OR_IMPROVE_RANK, TOP_THREE_AT_FOUR, TOP_TWO_AT_FOUR, CLIMB_ONE_RANK, LAST_TO_TOP_TWO
    }

    public record Goal(String id, int group, Level level, int target, String titleRo, String titleEn,
            String descriptionRo, String descriptionEn, Metric metric, Aggregation aggregation) {
        public Goal {
            if (id == null || !id.matches("[a-z][a-z0-9_]*") || group < 1 || group > 8 || target < 1)
                throw new IllegalArgumentException("Invalid achievement definition");
            java.util.Objects.requireNonNull(level);
            java.util.Objects.requireNonNull(metric);
            java.util.Objects.requireNonNull(aggregation);
            if (titleRo == null || titleRo.isBlank() || titleEn == null || titleEn.isBlank()
                    || descriptionRo == null || descriptionRo.isBlank()
                    || descriptionEn == null || descriptionEn.isBlank())
                throw new IllegalArgumentException("Achievements require Romanian and English copy");
        }
        public String title(boolean romanian) { return romanian ? titleRo : titleEn; }
        public String description(boolean romanian) { return romanian ? descriptionRo : descriptionEn; }
    }

    public record Group(int number, String titleRo, String titleEn, List<Goal> goals) {
        public Group { goals = List.copyOf(goals); }
        public String title(boolean romanian) { return romanian ? titleRo : titleEn; }
    }

    private static final List<Goal> GOALS = List.of(
            goal("first_match", 1, Level.EASY, 1, "Prima partidă", "First match", "Încheie o partidă validă.", "Finish one valid match.", Metric.COMPLETED_MATCHES, Aggregation.SUM),
            goal("first_exact", 1, Level.EASY, 1, "Primul contract exact", "First exact contract", "Fă cel puțin un contract exact.", "Make at least one exact contract.", Metric.EXACT_CONTRACTS, Aggregation.SUM),
            goal("five_matches", 1, Level.MEDIUM, 5, "Jucător constant", "Regular player", "Încheie cinci partide valide.", "Finish five valid matches.", Metric.COMPLETED_MATCHES, Aggregation.SUM),
            goal("twenty_exact", 1, Level.MEDIUM, 20, "Licitații confirmate", "Contracts confirmed", "Adună douăzeci de contracte exacte.", "Accumulate twenty exact contracts.", Metric.EXACT_CONTRACTS, Aggregation.SUM),
            goal("three_wins", 1, Level.HARD, 3, "Trei victorii", "Three victories", "Câștigă trei partide; egalitatea pe primul loc contează ca victorie comună.", "Win three matches; a tie for first counts as a shared win.", Metric.MATCH_WINS, Aggregation.SUM),

            goal("exact_one_card", 2, Level.EASY, 1, "Contract la o carte", "One-card contract", "Fă exact un contract într-o mână de o carte.", "Make an exact contract in a one-card hand.", Metric.EXACT_ONE_CARD, Aggregation.SUM),
            goal("exact_zero_large", 2, Level.EASY, 1, "Zero la mână mare", "Zero on a large hand", "Licitează zero și termină fără levate o mână de șase până la opt cărți.", "Bid zero and take no tricks in a six- to eight-card hand.", Metric.EXACT_ZERO_LARGE, Aggregation.SUM),
            goal("exact_small_rate", 2, Level.MEDIUM, 50, "Precizie la mâini scurte", "Short-hand accuracy", "Într-o partidă, fă exacte cel puțin 50% dintre mâinile de două sau trei cărți.", "In one match, make at least 50% of two- or three-card hands exact.", Metric.EXACT_SMALL_RATE, Aggregation.BEST_MATCH),
            goal("exact_middle_rate", 2, Level.MEDIUM, 50, "Precizie la mâini medii", "Middle-hand accuracy", "Într-o partidă, fă exacte cel puțin 50% dintre mâinile de patru sau cinci cărți.", "In one match, make at least 50% of four- or five-card hands exact.", Metric.EXACT_MIDDLE_RATE, Aggregation.BEST_MATCH),
            goal("exact_large_rate", 2, Level.HARD, 75, "Stăpânești mâinile mari", "Master large hands", "Într-o partidă, fă exacte cel puțin 75% dintre mâinile de șase până la opt cărți.", "In one match, make at least 75% of six- to eight-card hands exact.", Metric.EXACT_LARGE_RATE, Aggregation.BEST_MATCH),

            goal("three_exact_in_row", 3, Level.EASY, 3, "Trei la rând", "Three in a row", "Fă contracte exacte în trei mâini consecutive ale aceleiași partide.", "Make exact contracts in three consecutive hands of one match.", Metric.EXACT_STREAK, Aggregation.BEST_MATCH),
            goal("fifty_percent_exact", 3, Level.EASY, 50, "Jumătate exacte", "Half exact", "Fă exacte cel puțin 50% dintre mâinile unei partide complete.", "Make at least 50% of the hands in one complete match exact.", Metric.EXACT_MATCH_RATE, Aggregation.BEST_MATCH),
            goal("sixty_percent_exact", 3, Level.MEDIUM, 60, "Precizie de 60%", "60% accuracy", "Fă exacte cel puțin 60% dintre mâinile unei partide complete.", "Make at least 60% of the hands in one complete match exact.", Metric.EXACT_MATCH_RATE, Aggregation.BEST_MATCH),
            goal("sixty_five_percent_exact", 3, Level.MEDIUM, 65, "Precizie de 65%", "65% accuracy", "Fă exacte cel puțin 65% dintre mâinile unei partide complete.", "Make at least 65% of the hands in one complete match exact.", Metric.EXACT_MATCH_RATE, Aggregation.BEST_MATCH),
            goal("seventy_five_percent_exact", 3, Level.HARD, 75, "Control de expert", "Expert control", "Fă exacte cel puțin 75% dintre mâinile unei partide complete.", "Make at least 75% of the hands in one complete match exact.", Metric.EXACT_MATCH_RATE, Aggregation.BEST_MATCH),

            goal("series_enabled_match", 4, Level.EASY, 1, "Încerci seriile", "Try a streak match", "Încheie o partidă românească având seriile de cinci mâini activate.", "Finish a Romanian match with five-hand streaks enabled.", Metric.SERIES_ENABLED_MATCHES, Aggregation.SUM),
            goal("first_positive_series", 4, Level.EASY, 1, "Bonus de serie", "Streak reward", "Obține natural un bonus pozitiv de serie; ratarea contractelor nu este recompensată.", "Earn a positive streak reward through play; missed contracts are never rewarded.", Metric.POSITIVE_SERIES_AWARDS, Aggregation.SUM),
            goal("three_positive_series", 4, Level.MEDIUM, 3, "Serii reușite", "Successful streaks", "Obține trei bonusuri pozitive de serie.", "Earn three positive streak rewards.", Metric.POSITIVE_SERIES_AWARDS, Aggregation.SUM),
            goal("win_with_series", 4, Level.MEDIUM, 1, "Seria câștigătoare", "Winning streak", "Câștigă o partidă în care ai primit cel puțin un bonus pozitiv de serie.", "Win a match in which you earned at least one positive streak reward.", Metric.WIN_WITH_SERIES_AWARD, Aggregation.SUM),
            goal("five_series_matches", 4, Level.HARD, 5, "Sezon de serii", "Streak season", "Obține bonusuri pozitive în cinci partide românești diferite.", "Earn positive streak rewards in five different Romanian matches.", Metric.POSITIVE_SERIES_MATCHES, Aggregation.SUM),

            goal("short_romanian_match", 5, Level.EASY, 1, "Românesc 1–8–1", "Romanian 1–8–1", "Încheie o partidă în varianta românească 1–8–1.", "Finish a Romanian 1–8–1 match.", Metric.SHORT_ROMANIAN_MATCHES, Aggregation.SUM),
            goal("long_romanian_match", 5, Level.EASY, 1, "Românesc lung", "Long Romanian", "Încheie o partidă în varianta românească 8–1–8.", "Finish a Long Romanian 8–1–8 match.", Metric.LONG_ROMANIAN_MATCHES, Aggregation.SUM),
            goal("short_romanian_win", 5, Level.MEDIUM, 1, "Câștigător 1–8–1", "1–8–1 winner", "Câștigă o partidă românească 1–8–1.", "Win a Romanian 1–8–1 match.", Metric.SHORT_ROMANIAN_WINS, Aggregation.SUM),
            goal("long_romanian_win", 5, Level.MEDIUM, 1, "Câștigător 8–1–8", "8–1–8 winner", "Câștigă o partidă românească 8–1–8.", "Win a Long Romanian 8–1–8 match.", Metric.LONG_ROMANIAN_WINS, Aggregation.SUM),
            goal("zero_game_500", 5, Level.HARD, 1, "Joc la 0", "Play to zero", "Cu regula activată, termină o partidă românească la zero și primește 500 de puncte.", "With the rule enabled, finish a Romanian match at zero and receive 500 points.", Metric.ZERO_GAME_MATCHES, Aggregation.SUM),

            goal("three_player_match", 6, Level.EASY, 1, "Masă de trei", "Three-seat table", "Încheie o partidă validă la masa de trei jucători.", "Finish a valid match at a three-player table.", Metric.PLAYER_COUNT_THREE, Aggregation.SUM),
            goal("six_player_match", 6, Level.EASY, 1, "Masă plină", "Full table", "Încheie o partidă validă la masa de șase jucători.", "Finish a valid match at a six-player table.", Metric.PLAYER_COUNT_SIX, Aggregation.SUM),
            goal("top_two_five_players", 6, Level.MEDIUM, 1, "Podium la cinci", "Five-player podium", "Termină în primii doi la o masă de cinci jucători.", "Finish in the top two at a five-player table.", Metric.TOP_TWO_AT_FIVE, Aggregation.SUM),
            goal("top_two_six_players", 6, Level.MEDIUM, 1, "Podium la șase", "Six-player podium", "Termină în primii doi la o masă de șase jucători.", "Finish in the top two at a six-player table.", Metric.TOP_TWO_AT_SIX, Aggregation.SUM),
            goal("three_six_player_wins", 6, Level.HARD, 3, "Stăpânești masa plină", "Master the full table", "Câștigă trei partide la masa de șase jucători.", "Win three matches at a six-player table.", Metric.WINS_AT_SIX, Aggregation.SUM),

            goal("exact_final_hand", 7, Level.EASY, 1, "Închidere exactă", "Exact finish", "Fă contractul exact în ultima mână a unei partide complete.", "Make an exact contract in the final hand of a complete match.", Metric.EXACT_FINAL_HAND, Aggregation.SUM),
            goal("exact_seven_hand_sizes", 7, Level.EASY, 7, "Joc versatil", "Versatile play", "Fă cel puțin un contract exact la șapte mărimi diferite, de la una la opt cărți, într-o singură partidă românească.", "Make at least one exact contract at seven hand sizes from one to eight cards in one Romanian match.", Metric.EXACT_HAND_SIZE_COUNT, Aggregation.BEST_MATCH),
            goal("exact_final_three", 7, Level.MEDIUM, 1, "Final în ritm", "Strong finish", "Fă contractele exacte în ultimele trei mâini ale unei partide complete.", "Make exact contracts in the final three hands of a complete match.", Metric.EXACT_FINAL_THREE, Aggregation.SUM),
            goal("exact_all_hand_sizes", 7, Level.MEDIUM, 8, "Stăpânești toate mărimile", "All hand sizes", "Fă cel puțin un contract exact la fiecare mărime de la una la opt cărți, într-o singură partidă românească.", "Make at least one exact contract at every hand size from one to eight in one Romanian match.", Metric.EXACT_HAND_SIZE_COUNT, Aggregation.BEST_MATCH),
            goal("exact_final_five", 7, Level.HARD, 1, "Cinci mâini impecabile", "Five-hand close", "Fă contractele exacte în ultimele cinci mâini ale unei partide complete.", "Make exact contracts in the final five hands of a complete match.", Metric.EXACT_FINAL_FIVE, Aggregation.SUM),

            goal("hold_mid_match_rank", 8, Level.EASY, 1, "Ții poziția", "Hold your position", "Păstrează sau îmbunătățește locul ocupat la jumătatea partidei.", "Keep or improve your standing from halfway to the end of the match.", Metric.HOLD_OR_IMPROVE_RANK, Aggregation.SUM),
            goal("top_three_four_players", 8, Level.EASY, 1, "Top trei la patru", "Top three at four", "Termină în primii trei la o masă de patru jucători.", "Finish in the top three at a four-player table.", Metric.TOP_THREE_AT_FOUR, Aggregation.SUM),
            goal("top_two_four_players", 8, Level.MEDIUM, 1, "Podium la patru", "Four-player podium", "Termină în primii doi la o masă de patru jucători.", "Finish in the top two at a four-player table.", Metric.TOP_TWO_AT_FOUR, Aggregation.SUM),
            goal("climb_one_rank", 8, Level.MEDIUM, 1, "Urcare în clasament", "Climb the standings", "Urcă cel puțin un loc între jumătatea și finalul partidei.", "Move up at least one place between halfway and the end of the match.", Metric.CLIMB_ONE_RANK, Aggregation.SUM),
            goal("last_to_top_two", 8, Level.HARD, 1, "Revenire de campion", "Champion comeback", "De pe ultimul loc la jumătatea partidei, termină în primii doi.", "Move from last place at halfway to the top two by the end.", Metric.LAST_TO_TOP_TWO, Aggregation.SUM)
    );

    private static final List<Group> GROUPS = List.of(
            group(1, "Primii pași", "Getting started"), group(2, "Contracte pe mărimea mâinii", "Contracts by hand size"),
            group(3, "Precizie în serie", "Sustained accuracy"), group(4, "Serii pozitive", "Positive streaks"),
            group(5, "Variante românești", "Romanian variants"), group(6, "Mese și clasamente", "Tables and standings"),
            group(7, "Închidere și versatilitate", "Finishes and range"),
            group(8, "Clasament și reveniri", "Standings and comebacks"));
    private static final Map<String, Goal> BY_ID = GOALS.stream().collect(Collectors.toUnmodifiableMap(Goal::id, goal -> goal));

    private WhistAchievementCatalog() { }
    private static Goal goal(String id, int group, Level level, int target, String titleRo, String titleEn,
            String descriptionRo, String descriptionEn, Metric metric, Aggregation aggregation) {
        return new Goal(id, group, level, target, titleRo, titleEn, descriptionRo, descriptionEn, metric, aggregation);
    }
    private static Group group(int number, String ro, String en) {
        return new Group(number, ro, en, GOALS.stream().filter(goal -> goal.group() == number)
                .collect(java.util.stream.Collectors.toList()));
    }
    public static List<Goal> goals() { return GOALS; }
    public static List<Group> groups() { return GROUPS; }
    public static Goal goal(String id) {
        Goal goal = BY_ID.get(id);
        if (goal == null) throw new IllegalArgumentException("Unknown achievement: " + id);
        return goal;
    }
}

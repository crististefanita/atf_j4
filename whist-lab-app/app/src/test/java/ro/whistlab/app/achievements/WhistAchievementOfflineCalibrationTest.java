package ro.whistlab.app.achievements;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Aggregation;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Goal;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Metric;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Opt-in paired self-play calibration. Kept out of the fast component collection. */
public final class WhistAchievementOfflineCalibrationTest extends atf.gradle.audit.GradleAuditedTest {
    private static final List<AiDifficulty> LEVELS = List.of(AiDifficulty.NORMAL, AiDifficulty.HARD);
    private static final List<Goal> AUDITED_CONTRACT_GOALS = WhistAchievementCatalog.goals().stream()
            .filter(goal -> goal.group() >= 9).toList();

    @Test public void policiesCompletePairedRomanianDealsAndProduceAuditableAchievementRates() throws Exception {
        String phase = required("whist.achievementCalibrationPhase");
        long firstSeed = Long.parseLong(required("whist.achievementCalibrationFirstSeed"));
        int matches = Integer.parseInt(required("whist.achievementCalibrationMatches"));
        boolean streaks = Boolean.parseBoolean(System.getProperty("whist.achievementCalibrationStreaks", "true"));
        boolean zeroGame = Boolean.parseBoolean(System.getProperty("whist.achievementCalibrationZero", "false"));
        int seriesBonus = Integer.parseInt(System.getProperty("whist.achievementCalibrationSeriesBonus", "10"));
        int playerCount = Integer.parseInt(System.getProperty("whist.achievementCalibrationPlayers", "4"));
        GameMode mode = GameMode.valueOf(System.getProperty("whist.achievementCalibrationMode", "ROMANIAN_WHIST"));
        Path output = Path.of(required("whist.achievementCalibrationOutput")).toAbsolutePath().normalize();
        Path source = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        long startedAt = System.nanoTime();
        if (output.startsWith(source)) throw new IllegalArgumentException("Calibration evidence must stay outside the repository");
        if (!(phase.equals("calibration") || phase.equals("holdout")) || matches < 1
                || playerCount < 3 || playerCount > 6 || !mode.isRomanian()
                || (seriesBonus != 5 && seriesBonus != 10))
            throw new IllegalArgumentException("Invalid calibration phase or match count");

        Files.createDirectories(output);
        Map<AiDifficulty, Cohort> cohorts = new EnumMap<>(AiDifficulty.class);
        for (AiDifficulty level : LEVELS) cohorts.put(level, new Cohort());
        Set<String> distinctDeals = new HashSet<>();
        List<String> gameRows = new ArrayList<>();
        gameRows.add("phase,difficulty,playerCount,mode,seed,dealSha256,seat0Score,seat0Rank,exactContracts,exactRate,shortRate,middleRate,largeRate,longestExactRun,positiveSeriesAwards,zeroAward,"
                + String.join(",", AUDITED_CONTRACT_GOALS.stream().map(Goal::id).toList()));
        writeCheckpoint(output, phase, firstSeed, matches, 0, distinctDeals, gameRows, startedAt);
        var expectedSchedule = List.of(1, 1, 1, 1, 2, 3, 4, 5, 6, 7,
                8, 8, 8, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1);
        var schedule = GameRules.handSizes(mode, playerCount);
        if (mode == GameMode.ROMANIAN_WHIST && playerCount == 4)
            require(schedule.equals(expectedSchedule), "the four-seat Romanian match must retain the complete 24-hand 1–8–1 schedule");

        for (int index = 0; index < matches; index++) {
            long seed = Math.addExact(firstSeed, index);
            var normal = play(seed, AiDifficulty.NORMAL, streaks, seriesBonus, zeroGame, mode, playerCount);
            var hard = play(seed, AiDifficulty.HARD, streaks, seriesBonus, zeroGame, mode, playerCount);
            require(normal.dealHash().equals(hard.dealHash()), "paired policies must receive the same deal");
            require(distinctDeals.add(normal.dealHash()), "every seed must produce a distinct deal");
            observe(cohorts.get(AiDifficulty.NORMAL), normal, phase, seed, index, gameRows,
                    streaks, zeroGame, mode, playerCount);
            observe(cohorts.get(AiDifficulty.HARD), hard, phase, seed, index, gameRows,
                    streaks, zeroGame, mode, playerCount);
            if ((index + 1) % 4 == 0 || index + 1 == matches)
                writeCheckpoint(output, phase, firstSeed, matches, index + 1, distinctDeals, gameRows, startedAt);
        }

        List<String> goalRows = new ArrayList<>();
        goalRows.add("phase,playerCount,mode,difficulty,goalId,stage,level,metric,aggregation,target,eligibleMatches,contributingMatches,bestMatchHits,bestMatchRatePercent,firstCareerUnlockMatch,finalProgress");
        for (AiDifficulty level : LEVELS) {
            Cohort cohort = cohorts.get(level);
            require(matches == cohort.completedMatches, "each policy must complete every requested match");
            for (Goal goal : WhistAchievementCatalog.goals()) {
                GoalStats stats = cohort.goals.get(goal.id());
                String bestMatchHits = goal.aggregation() == Aggregation.BEST_MATCH
                        ? Integer.toString(stats.bestMatchHits) : "NA";
                String bestMatchRate = goal.aggregation() == Aggregation.BEST_MATCH && stats.eligibleMatches > 0
                        ? Integer.toString(Math.round(100f * stats.bestMatchHits / stats.eligibleMatches)) : "NA";
                goalRows.add(String.join(",", phase, Integer.toString(playerCount), mode.name(), level.name(),
                        goal.id(), Integer.toString(goal.group()),
                        goal.level().name(), goal.metric().name(), goal.aggregation().name(),
                        Integer.toString(goal.target()), Integer.toString(stats.eligibleMatches),
                        Integer.toString(stats.contributingMatches), bestMatchHits, bestMatchRate,
                        Integer.toString(stats.firstCareerUnlockMatch), Integer.toString(cohort.repository.state().progress(goal.id()))));
            }
        }
        Files.write(output.resolve(phase + "-games.csv"), gameRows, StandardCharsets.UTF_8);
        Files.write(output.resolve(phase + "-goals.csv"), goalRows, StandardCharsets.UTF_8);
        Files.write(output.resolve(phase + "-run.properties"),
                ("phase=" + phase + "\nfirstSeed=" + firstSeed + "\nlastSeed=" + Math.addExact(firstSeed, matches - 1)
                        + "\npairedSeeds=" + matches + "\ndistinctDealCount=" + distinctDeals.size()
                        + "\npolicyGames=" + (matches * LEVELS.size()) + "\nplayerCount=" + playerCount
                        + "\nmode=" + mode + "\n"
                        + "seriesEnabled=" + streaks + "\nseriesBonus=" + seriesBonus + "\nzeroGameEnabled=" + zeroGame
                        + "\nelapsedSeconds=" + elapsedSeconds(startedAt) + "\n")
                        .getBytes(StandardCharsets.UTF_8));

        atf.gradle.audit.GradleInputs.fixture("Paired Normal/Hard self-play on distinct Romanian deals",
                "phase", phase, "mode", mode, "players", playerCount, "first seed", firstSeed,
                "paired distinct deals", distinctDeals.size(),
                "policy games", matches * LEVELS.size(), "series enabled", streaks,
                "series bonus/penalty", seriesBonus, "zero game enabled", zeroGame,
                "per-game outcomes", output.resolve(phase + "-games.csv"),
                "per-goal attainment and career-unlock table", output.resolve(phase + "-goals.csv"),
                "progress checkpoint", output.resolve("phase-progress.properties"),
                "external evidence directory", output);
        assertEquals("all paired deals must remain distinct", matches, distinctDeals.size());
        assertFalse("the cohort cannot be empty", gameRows.size() <= 1);
    }

    private static void observe(Cohort cohort, MatchRun match, String phase, long seed, int index,
            List<String> gameRows, boolean streaks, boolean zeroGame, GameMode mode, int playerCount) throws IOException {
        var handFacts = WhistHandFacts.from(match.game());
        var matchFacts = WhistMatchFacts.from(match.game(), handFacts).orElseThrow();
        var schedule = GameRules.handSizes(mode, playerCount);
        require(schedule.size() == handFacts.size(), "a complete deal must record every scheduled hand");
        String matchId = phase + ":" + mode + ":" + playerCount + ":" + match.difficulty() + ":" + seed;
        require(cohort.repository.record(matchId, match.game()), "a new match identity must be processed");
        var after = cohort.repository.state();
        var duplicate = new WhistAchievementRepository(cohort.storage);
        require(!duplicate.record(matchId, match.game()), "replay of a completed match must not award progress twice");
        require(after.equals(duplicate.state()), "replay must leave durable progress unchanged");
        cohort.completedMatches++;

        for (Goal goal : WhistAchievementCatalog.goals()) {
            GoalStats stats = cohort.goals.get(goal.id());
            if (!eligible(goal.metric(), match.game(), matchFacts, streaks, zeroGame)) continue;
            stats.eligibleMatches++;
            int matchEvidence = evidence(goal, handFacts, matchFacts);
            if (matchEvidence > 0) stats.contributingMatches++;
            if (goal.aggregation() == Aggregation.BEST_MATCH && matchEvidence >= goal.target()) stats.bestMatchHits++;
            if (stats.firstCareerUnlockMatch == 0 && after.earned(goal)) stats.firstCareerUnlockMatch = index + 1;
        }
        var completedState = match.game().snapshot();
        int seatZeroScore = completedState.matchScore(0);
        int rank = 1 + (int) completedState.players().stream()
                .filter(player -> completedState.matchScore(player.seat()) > seatZeroScore).count();
        gameRows.add(String.join(",", phase, match.difficulty().name(), Integer.toString(playerCount), mode.name(),
                Long.toString(seed), match.dealHash(),
                Integer.toString(seatZeroScore),
                Integer.toString(rank), Integer.toString(matchFacts.exactContracts()),
                Integer.toString(matchFacts.exactMatchRate()), Integer.toString(matchFacts.exactSmallHandRate()),
                Integer.toString(matchFacts.exactMiddleHandRate()), Integer.toString(matchFacts.exactLargeHandRate()),
                Integer.toString(matchFacts.longestExactStreak()), Integer.toString(matchFacts.positiveSeriesAwards()),
                Boolean.toString(matchFacts.zeroGameAward()),
                String.join(",", AUDITED_CONTRACT_GOALS.stream().map(goal ->
                        eligible(goal.metric(), match.game(), matchFacts, streaks, zeroGame)
                                ? Integer.toString(evidence(goal, handFacts, matchFacts)) : "NA").toList())));
    }

    private static int evidence(Goal goal, List<WhistHandFacts> hands, WhistMatchFacts match) {
        if (!goal.metric().isHandMetric()) return match.measure(goal.metric());
        int value = 0;
        for (WhistHandFacts hand : hands) {
            int measured = hand.measure(goal.metric());
            value = goal.aggregation() == Aggregation.BEST_MATCH ? Math.max(value, measured) : Math.addExact(value, measured);
        }
        return value;
    }

    private static boolean eligible(Metric metric, GameSession game, WhistMatchFacts facts,
            boolean streaks, boolean zeroGame) {
        return switch (metric) {
            case LONG_ROMANIAN_MATCHES, LONG_ROMANIAN_WINS -> game.config().mode() == GameMode.LONG_ROMANIAN;
            case SERIES_ENABLED_MATCHES, POSITIVE_SERIES_AWARDS, POSITIVE_SERIES_MATCHES, WIN_WITH_SERIES_AWARD -> streaks;
            case ZERO_GAME_MATCHES -> zeroGame && game.config().mode().isRomanian();
            case PLAYER_COUNT_THREE -> game.config().playerCount() == 3;
            case PLAYER_COUNT_SIX, TOP_TWO_AT_SIX, WINS_AT_SIX -> game.config().playerCount() == 6;
            case TOP_TWO_AT_FIVE -> game.config().playerCount() == 5;
            case TOP_TWO_AT_FOUR, TOP_THREE_AT_FOUR -> game.config().playerCount() == 4;
            case EXACT_POSITIVE_RATE -> facts.contracts().positiveOpportunities() >= 8;
            case SHORT_ROMANIAN_MATCHES, SHORT_ROMANIAN_WINS -> game.config().mode() == GameMode.ROMANIAN_WHIST;
            default -> true;
        };
    }

    private static void writeCheckpoint(Path output, String phase, long firstSeed, int requested,
            int completed, Set<String> deals, List<String> gameRows, long startedAt) throws IOException {
        Files.write(output.resolve(phase + "-games.csv"), gameRows, StandardCharsets.UTF_8);
        Files.write(output.resolve("phase-progress.properties"),
                ("phase=" + phase + "\nfirstSeed=" + firstSeed + "\nrequestedPairs=" + requested
                        + "\ncompletedPairs=" + completed + "\npolicyGames=" + (completed * LEVELS.size())
                        + "\ndistinctDealCount=" + deals.size() + "\nelapsedSeconds=" + elapsedSeconds(startedAt) + "\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static long elapsedSeconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000_000L;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static MatchRun play(long seed, AiDifficulty difficulty, boolean streaks, int seriesBonus,
            boolean zeroGame, GameMode mode, int playerCount) {
        var config = GameConfig.newMatch(playerCount, mode, difficulty, seed, streaks, seriesBonus, zeroGame);
        GameSession game = GameSession.newGame(config);
        var deal = new StringBuilder();
        appendDeal(deal, game.snapshot());
        int actions = 0;
        while (actions++ < 2_000) {
            var state = game.snapshot();
            switch (state.phase()) {
                case BIDDING -> game.submitBid(state.biddingSeat(), AiPolicy.chooseBid(state, state.biddingSeat(), difficulty));
                case PLAYING -> game.playCard(state.currentSeat(), AiPolicy.chooseCard(state, state.currentSeat(), difficulty));
                case SCORE -> {
                    if (state.finalHand()) return new MatchRun(game, difficulty, digest(deal.toString()));
                    game.startNextHand();
                    appendDeal(deal, game.snapshot());
                }
            }
        }
        throw new AssertionError("AI self-play exceeded 2,000 legal transitions for seed " + seed + "/" + difficulty);
    }

    private static void appendDeal(StringBuilder result, GameSession.Snapshot state) {
        result.append(state.mode()).append('|').append(state.players().size()).append('|')
                .append(state.handNumber()).append('|').append(state.dealerSeat()).append('|')
                .append(state.trump()).append('|').append(state.turnedCard()).append('|');
        for (var player : state.players()) {
            result.append(player.seat()).append(':');
            player.hand().stream().map(card -> card.suit().ordinal() * 13 + card.rank())
                    .sorted().forEach(card -> result.append(card).append('.'));
            result.append('|');
        }

        result.append('\n');
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing calibration property: " + key);
        return value;
    }

    private record MatchRun(GameSession game, AiDifficulty difficulty, String dealHash) { }

    private static final class Cohort {
        private final MemoryStorage storage = new MemoryStorage();
        private final WhistAchievementRepository repository = new WhistAchievementRepository(storage);
        private final Map<String, GoalStats> goals = new HashMap<>();
        private int completedMatches;
        private Cohort() {
            WhistAchievementCatalog.goals().forEach(goal -> goals.put(goal.id(), new GoalStats()));
        }
    }

    private static final class GoalStats {
        private int eligibleMatches;
        private int contributingMatches;
        private int bestMatchHits;
        private int firstCareerUnlockMatch;
    }

    private static final class MemoryStorage implements WhistAchievementRepository.Storage {
        private String json;
        @Override public String read() { return json; }
        @Override public void write(String value) { json = value; }
    }
}

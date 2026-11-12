package ro.whistlab.app.achievements;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Level;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public final class WhistAchievementComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private static final Set<String> LEGACY_CATALOG_IDS = Set.of(
            "first_match", "first_exact", "five_matches", "twenty_exact", "three_wins",
            "exact_one_card", "exact_zero_large", "exact_small_rate", "exact_middle_rate", "exact_large_rate",
            "three_exact_in_row", "fifty_percent_exact", "sixty_percent_exact", "sixty_five_percent_exact",
            "seventy_five_percent_exact", "series_enabled_match", "first_positive_series", "three_positive_series",
            "win_with_series", "five_series_matches", "short_romanian_match", "long_romanian_match",
            "short_romanian_win", "long_romanian_win", "zero_game_500", "three_player_match", "six_player_match",
            "top_two_five_players", "top_two_six_players", "three_six_player_wins", "exact_final_hand",
            "exact_seven_hand_sizes", "exact_final_three", "exact_all_hand_sizes", "exact_final_five",
            "hold_mid_match_rank", "top_three_four_players", "top_two_four_players", "climb_one_rank", "last_to_top_two");

    @Test public void catalogGroupsKeepTheirContractAndEveryLegacyIdRemainsAvailable() {
        var groups = WhistAchievementCatalog.groups();
        var goalIds = WhistAchievementCatalog.goals().stream().map(WhistAchievementCatalog.Goal::id)
                .collect(java.util.stream.Collectors.toSet());
        var levelCounts = groups.stream().map(group -> group.goals().stream()
                .collect(java.util.stream.Collectors.groupingBy(WhistAchievementCatalog.Goal::level,
                        java.util.stream.Collectors.counting()))).toList();
        var accuracy = groups.get(2).goals().stream()
                .filter(goal -> goal.metric() == WhistAchievementCatalog.Metric.EXACT_MATCH_RATE)
                .map(WhistAchievementCatalog.Goal::target).toList();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "groups", groups,
                "legacy IDs", LEGACY_CATALOG_IDS, "catalog IDs", goalIds, "levelCounts", levelCounts);
        assertFalse("the catalog must define at least one learning group", groups.isEmpty());
        assertEquals("each group has exactly five independent alternatives", groups.size() * 5,
                WhistAchievementCatalog.goals().size());
        assertEquals("catalog IDs must be unique", WhistAchievementCatalog.goals().size(), goalIds.size());
        assertTrue("the catalog must retain every previously persisted ID", goalIds.containsAll(LEGACY_CATALOG_IDS));
        assertEquals("accuracy goals must use increasing match-relative thresholds", List.of(50, 60, 65, 75), accuracy);
        for (int index = 0; index < groups.size(); index++) {
            var group = groups.get(index);
            assertEquals("chapter numbers must stay consecutive", index + 1, group.number());
            assertEquals("each chapter must contain five freely available goals", 5, group.goals().size());
            assertEquals("each chapter must offer two Easy goals", 2L,
                    levelCounts.get(index).getOrDefault(Level.EASY, 0L).longValue());
            assertEquals("each chapter must offer two Medium goals", 2L,
                    levelCounts.get(index).getOrDefault(Level.MEDIUM, 0L).longValue());
            assertEquals("each chapter must offer one Hard goal", 1L,
                    levelCounts.get(index).getOrDefault(Level.HARD, 0L).longValue());
            for (var goal : group.goals()) {
                assertFalse("Romanian goal title must be present", goal.titleRo().isBlank());
                assertFalse("English goal title must be present", goal.titleEn().isBlank());
                assertFalse("Romanian rule must be present", goal.descriptionRo().isBlank());
                assertFalse("English rule must be present", goal.descriptionEn().isBlank());
            }
        }
        assertEquals("seven card sizes is the versatility milestone", 7,
                WhistAchievementCatalog.goal("exact_seven_hand_sizes").target());
        assertEquals("complete hand-size coverage is a separate match milestone", 8,
                WhistAchievementCatalog.goal("exact_all_hand_sizes").target());
        assertEquals("comeback goal is reserved for a last-place to top-two finish", 1,
                WhistAchievementCatalog.goal("last_to_top_two").target());
        assertThrows("group recommendation rejects an invalid group number", IllegalArgumentException.class,
                () -> WhistAchievementState.empty().earnedInGroup(0));
    }

    @Test public void stableIdsAndTheirProgressRoundTripThroughVersionedStorage() throws Exception {
        var progress = LEGACY_CATALOG_IDS.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> 1));
        var saved = new WhistAchievementState(progress, Set.of("legacy-career"),
                Set.of("legacy-career#hand:1"), Set.of("first_match"));
        String encoded = WhistAchievementCodec.encode(saved);
        var restored = WhistAchievementCodec.decode(encoded);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "encoded versioned state", encoded,
                "legacy catalog IDs", LEGACY_CATALOG_IDS, "restored progress", restored.progress());
        assertEquals("all previous progress values survive a save/load cycle", saved.progress(), restored.progress());
        assertEquals("processed match IDs still deduplicate after restore", saved.processedMatches(), restored.processedMatches());
        assertEquals("processed hand IDs still deduplicate after restore", saved.processedHands(), restored.processedHands());
        assertEquals("pending notices survive restore without changing their achievement identity",
                saved.pendingNotifications(), restored.pendingNotifications());
    }

    @Test public void unlockNoticeNamesTheMomentAndTotalProgressInRomanianAndEnglish() {
        var english = new WhistUiText(HowToPlay.Language.EN);
        var romanian = new WhistUiText(HowToPlay.Language.RO);
        String englishNotice = english.achievementNoticeSummary(2, 7, 40, 5, false, 3);
        String romanianNotice = romanian.achievementNoticeSummary(2, 7, 40, 5, false, 3);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "englishNotice", englishNotice,
                "romanianNotice", romanianNotice);
        assertEquals("singular notice title uses the saved UI language", "Realizare nouă",
                romanian.achievementsUnlocked(1));
        assertEquals("plural notice title uses the saved UI language", "Achievements unlocked",
                english.achievementsUnlocked(2));
        assertTrue("English notice identifies the completed hand and total progress",
                englishNotice.contains("Hand 5 complete") && englishNotice.contains("7 / 40"));
        assertTrue("Romanian notice identifies the completed hand and total progress",
                romanianNotice.contains("Mâna 5 s-a încheiat") && romanianNotice.contains("7 / 40"));
    }

    @Test public void everyAchievementIsAccruedWithoutLocksAndBestMatchGoalsDoNotSumAcrossGames() {
        var state = WhistAchievementState.empty();
        var laterChapterFacts = new WhistMatchFacts(6, GameMode.LONG_ROMANIAN, 0,
                0, 0, 0, 0, 0, 0, 0, true, true, false, 0, false);
        var firstChapterFacts = new WhistMatchFacts(3, GameMode.ROMANIAN_WHIST, 2,
                1, 0, 45, 50, 40, 20, 2, false, false, false, 1, false);
        state = state.recordMatch("later-chapter-first", laterChapterFacts);
        var afterEarlyLaterGoal = state;
        state = state.recordMatch("match-one", firstChapterFacts);
        var afterFirstMatch = state;
        state = state.recordMatch("match-two", firstChapterFacts);
        var streakState = WhistAchievementState.empty()
                .recordHand("streak-one#hand:1", new WhistHandFacts(1, 1, 1, 1, false, 1))
                .recordHand("streak-one#hand:2", new WhistHandFacts(2, 2, 1, 1, false, 2))
                .recordHand("streak-two#hand:1", new WhistHandFacts(1, 1, 1, 1, false, 1))
                .recordHand("streak-two#hand:2", new WhistHandFacts(2, 2, 1, 1, false, 2));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "afterEarlyLaterGoal", afterEarlyLaterGoal,
                "afterFirstMatch", afterFirstMatch, "finalState", state, "streakState", streakState);
        assertEquals("a later chapter goal is recorded by the same match without a lock", 1,
                afterEarlyLaterGoal.progress("six_player_match"));
        assertEquals("one eligible long Romanian match can advance multiple goals together", 1,
                afterEarlyLaterGoal.progress("long_romanian_match"));
        assertTrue("all newly completed goals are queued from the same match event",
                afterEarlyLaterGoal.pendingNotifications().containsAll(Set.of("six_player_match", "long_romanian_match")));
        assertEquals("the first completed goal advances the recommendation", 2,
                afterEarlyLaterGoal.recommendedGroup());
        assertEquals("any earned goal advances the recommended chapter, even when several goals arrive together",
                3, afterFirstMatch.recommendedGroup());
        assertEquals("chapter 6 progress remains available while chapter 2 is recommended", 1,
                state.progress("six_player_match"));
        assertEquals("best consecutive run is the best single match, not a cross-match sum", 2,
                streakState.progress("three_exact_in_row"));
        assertEquals("best match accuracy is not summed across matches", 45,
                state.progress("fifty_percent_exact"));
    }

    @Test public void percentageGoalsUseTheExactBoundaryAndKeepPreviouslySavedProgress() {
        int belowBoundary = WhistMatchFacts.percentage(11, 17);
        int aboveBoundary = WhistMatchFacts.percentage(12, 17);
        var goal = WhistAchievementCatalog.goal("sixty_five_percent_exact");
        var belowFacts = new WhistMatchFacts(3, GameMode.ROMANIAN_WHIST, 11, 0, 0,
                belowBoundary, 0, 0, 0, 0, false, false, false, 0, false);
        var aboveFacts = new WhistMatchFacts(3, GameMode.ROMANIAN_WHIST, 12, 0, 0,
                aboveBoundary, 0, 0, 0, 0, false, false, false, 0, false);
        var below = WhistAchievementState.empty().recordMatch("below-exact-boundary", belowFacts);
        var above = WhistAchievementState.empty().recordMatch("meets-exact-boundary", aboveFacts);
        var historicalProgress = new WhistAchievementState(java.util.Map.of(goal.id(), goal.target()), Set.of());
        var afterHistoricalMatch = historicalProgress.recordMatch("preserve-earned-boundary", belowFacts);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "below ratio", "11/17",
                "below integer percentage", belowBoundary, "above ratio", "12/17",
                "above integer percentage", aboveBoundary, "threshold", goal.target(),
                "previously saved progress", historicalProgress, "after a below-boundary match", afterHistoricalMatch);
        assertEquals("an under-threshold fraction must not round upward", 64, belowBoundary);
        assertEquals("a fraction above the threshold is represented without upward rounding", 70, aboveBoundary);
        assertEquals("an under-threshold match keeps its measured progress", 64, below.progress(goal.id()));
        assertFalse("an under-threshold match cannot earn a 65-percent goal", below.earned(goal));
        assertTrue("an above-threshold match earns the existing goal", above.earned(goal));
        assertEquals("historical saved progress remains intact", goal.target(), afterHistoricalMatch.progress(goal.id()));
        assertTrue("a historically earned goal stays earned after the formula correction", afterHistoricalMatch.earned(goal));
        assertFalse("formula correction does not queue a duplicate notice for the historical goal",
                afterHistoricalMatch.pendingNotifications().contains(goal.id()));
    }

    @Test public void completeRomanianAndLongRomanianMatchesProduceFactsFromTheFullScoreLedger() {
        var shortMatch = finishedMatch(3, GameMode.ROMANIAN_WHIST, false, false, 4101L);
        var longMatch = finishedMatch(6, GameMode.LONG_ROMANIAN, false, false, 4102L);
        var ohHellMatch = finishedMatch(4, GameMode.OH_HELL, false, false, 4107L);
        var shortFacts = WhistMatchFacts.from(shortMatch).orElseThrow();
        var longFacts = WhistMatchFacts.from(longMatch).orElseThrow();
        var ohHellFacts = WhistMatchFacts.from(ohHellMatch).orElseThrow();
        var shortHistory = shortMatch.scoreHistory();
        var schedule = GameRules.handSizes(shortMatch.config().mode(), shortMatch.config().playerCount());
        int independentlyCountedExact = 0, smallExact = 0, smallHands = 0;
        for (int hand = 1; hand <= schedule.size(); hand++) {
            boolean exact = shortHistory.outcome(hand, 0) == ro.whistlab.core.session.ScoreHistory.Outcome.EXACT;
            if (exact)
                independentlyCountedExact++;
            if (schedule.get(hand - 1) >= 2 && schedule.get(hand - 1) <= 3) {
                smallHands++;
                if (exact) smallExact++;
            }
        }
        int independentMatchRate = 100 * independentlyCountedExact / schedule.size();
        int independentSmallRate = 100 * smallExact / smallHands;

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "shortFacts", shortFacts,
                "longFacts", longFacts, "ohHellFacts", ohHellFacts, "shortMatch", shortMatch,
                "longMatch", longMatch, "ohHellMatch", ohHellMatch,
                "schedule", schedule, "independentlyCountedExact", independentlyCountedExact,
                "independentMatchRate", independentMatchRate, "independentSmallRate", independentSmallRate);
        assertTrue("both matches must be fully completed", shortMatch.snapshot().finalHand() && longMatch.snapshot().finalHand());
        assertEquals("short Romanian mode must be identified", 1, shortFacts.measure(WhistAchievementCatalog.Metric.SHORT_ROMANIAN_MATCHES));
        assertEquals("long Romanian mode must be identified", 1, longFacts.measure(WhistAchievementCatalog.Metric.LONG_ROMANIAN_MATCHES));
        assertEquals("exact count must come from recorded bid/trick rows", independentlyCountedExact, shortFacts.exactContracts());
        assertEquals("overall accuracy must normalize against the actual configured schedule", independentMatchRate,
                shortFacts.exactMatchRate());
        assertEquals("band accuracy must normalize against the hands in that band", independentSmallRate,
                shortFacts.exactSmallHandRate());
        assertEquals("all scheduled hands must have recorded bids", schedule.size(), shortHistory.recordedBids().size());
        assertEquals("all scheduled hands must have recorded tricks", schedule.size(), shortHistory.recordedTricks().size());
        assertTrue("only the human seat is included in facts", shortMatch.snapshot().players().get(0).human());
        assertEquals("a long six-seat match contributes the correct table size", 1,
                longFacts.measure(WhistAchievementCatalog.Metric.PLAYER_COUNT_SIX));
        assertEquals("Romanian hand-size mastery cannot be earned in a different Whist variant", 0,
                ohHellFacts.measure(WhistAchievementCatalog.Metric.EXACT_HAND_SIZE_COUNT));
    }

    @Test public void eligibilityFollowsRomanianVariantSeatCountAndEnabledRules() {
        var shortWithoutSeries = finishedMatch(3, GameMode.ROMANIAN_WHIST, false, false, 4110L);
        var longWithSeries = finishedMatch(6, GameMode.LONG_ROMANIAN, true, false, 4111L);
        var ohHell = finishedMatch(4, GameMode.OH_HELL, false, false, 4112L);
        var shortFacts = WhistMatchFacts.from(shortWithoutSeries).orElseThrow();
        var longFacts = WhistMatchFacts.from(longWithSeries).orElseThrow();
        var ohHellFacts = WhistMatchFacts.from(ohHell).orElseThrow();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "short facts", shortFacts,
                "long facts", longFacts, "Oh Hell facts", ohHellFacts);
        assertEquals("1–8–1 is available only to its matching Romanian mode", 1,
                shortFacts.measure(WhistAchievementCatalog.Metric.SHORT_ROMANIAN_MATCHES));
        assertEquals("1–8–1 cannot credit the 8–1–8 objective", 0,
                shortFacts.measure(WhistAchievementCatalog.Metric.LONG_ROMANIAN_MATCHES));
        assertEquals("8–1–8 is available only to its matching Romanian mode", 1,
                longFacts.measure(WhistAchievementCatalog.Metric.LONG_ROMANIAN_MATCHES));
        assertEquals("6-seat goal eligibility follows the configured table", 1,
                longFacts.measure(WhistAchievementCatalog.Metric.PLAYER_COUNT_SIX));
        assertEquals("3-seat eligibility cannot leak to a 6-seat table", 0,
                longFacts.measure(WhistAchievementCatalog.Metric.PLAYER_COUNT_THREE));
        assertEquals("series goals require enabled Romanian series", 0,
                shortFacts.measure(WhistAchievementCatalog.Metric.SERIES_ENABLED_MATCHES));
        assertEquals("series eligibility is available when enabled in Romanian play", 1,
                longFacts.measure(WhistAchievementCatalog.Metric.SERIES_ENABLED_MATCHES));
        assertEquals("non-Romanian play cannot credit a Romanian series configuration", 0,
                ohHellFacts.measure(WhistAchievementCatalog.Metric.SERIES_ENABLED_MATCHES));
        assertEquals("non-Romanian play cannot credit either Romanian schedule", 0,
                ohHellFacts.measure(WhistAchievementCatalog.Metric.SHORT_ROMANIAN_MATCHES)
                        + ohHellFacts.measure(WhistAchievementCatalog.Metric.LONG_ROMANIAN_MATCHES));
        assertEquals("enabling Joc la 0 does not itself credit the award without a 500-point outcome", 0,
                ohHellFacts.measure(WhistAchievementCatalog.Metric.ZERO_GAME_MATCHES));
    }

    @Test public void unfinishedAndLegacyPartialLedgersCannotMintAchievements() {
        var unfinished = GameSession.newGame(new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 4103L));
        var facts = WhistMatchFacts.from(unfinished);
        var current = WhistAchievementState.empty();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "unfinished", unfinished,
                "facts", facts, "current", current);
        assertEquals("unfinished match must not produce achievement facts", java.util.Optional.empty(), facts);
        assertEquals("an unfinished game cannot increment match count", 0, current.progress("first_match"));
    }

    @Test public void repositoryIsIdempotentAcrossRetriesRestartsAndProfileRename() throws Exception {
        var game = finishedMatch(3, GameMode.ROMANIAN_WHIST, true, false, 4104L);
        var storage = new MemoryStorage();
        var repository = new WhistAchievementRepository(storage);
        boolean first = repository.record("stable-match", game);
        int writesAfterFirst = storage.writes;
        var restarted = new WhistAchievementRepository(storage);
        boolean duplicate = restarted.record("stable-match", game);
        var saved = restarted.state();
        game.renameHuman("Mara");
        var afterRename = new WhistAchievementRepository(storage).state();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "afterRename", afterRename,
                "duplicate", duplicate, "first", first, "game", game, "repository", repository,
                "restarted", restarted, "saved", saved, "storage", storage, "writesAfterFirst", writesAfterFirst);
        assertTrue("first finalized match must persist achievements", first);
        assertFalse("replay of the same match identity must be ignored", duplicate);
        assertEquals("duplicate replay must not write storage", writesAfterFirst, storage.writes);
        assertEquals("restored progress must have one completed match", 1, saved.progress("first_match"));
        assertTrue("stable match identity must survive storage reload", saved.processedMatches().contains("stable-match"));
        assertEquals("renaming the local display name must preserve the career", saved, afterRename);
    }

    @Test public void completedHandUnlocksImmediatelyAndNotificationStaysPendingUntilAcknowledged() throws Exception {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 4105L, true, 10));
        finishHand(game);
        var history = game.scoreHistory();
        boolean exact = history.bid(1, 0).equals(history.tricks(1, 0));
        var storage = new MemoryStorage();
        var repository = new WhistAchievementRepository(storage);
        boolean first = repository.record("hand-notice", game);
        int writesAfterFirst = storage.writes;
        var restarted = new WhistAchievementRepository(storage);
        boolean duplicate = restarted.record("hand-notice", game);
        var state = restarted.state();
        var pending = restarted.pendingNotifications();
        restarted.acknowledgeNotifications(pending.stream().map(WhistAchievementCatalog.Goal::id).toList());
        var afterDismiss = new WhistAchievementRepository(storage).state();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "afterDismiss", afterDismiss,
                "duplicate", duplicate, "exact", exact, "first", first, "game", game,
                "pending", pending, "restarted", restarted, "state", state, "storage", storage,
                "writesAfterFirst", writesAfterFirst);
        assertFalse("a completed hand is not yet a completed match", game.snapshot().finalHand());
        assertTrue("the hand event is stored", first);
        assertEquals("the completed hand stores one exact-contract increment when it was exact",
                exact ? 1 : 0, state.progress("first_exact"));
        assertEquals("an incomplete match cannot earn the match-completion goal", 0, state.progress("first_match"));
        assertEquals("the hand event is deduplicated across restart", writesAfterFirst, storage.writes - 1);
        assertFalse("repeating the same completed hand does not write again", duplicate);
        assertEquals("earned hand achievement appears in the durable notice queue", exact,
                pending.stream().anyMatch(goal -> goal.id().equals("first_exact")));
        assertTrue("a dismissed popup clears its durable queue", afterDismiss.pendingNotifications().isEmpty());
        assertEquals("dismissing the notice leaves achievement progress intact", state.progress(), afterDismiss.progress());
    }

    @Test public void newFinishAndComebackFactsMatchIndependentScoreLedgerCalculations() {
        var game = finishedMatch(4, GameMode.ROMANIAN_WHIST, false, false, 4106L);
        var facts = WhistMatchFacts.from(game).orElseThrow();
        var history = game.scoreHistory();
        var schedule = GameRules.handSizes(game.config().mode(), game.config().playerCount());
        var exactSizes = new java.util.HashSet<Integer>();
        boolean exactFinalThree = true, exactFinalFive = true;
        for (int hand = 1; hand <= schedule.size(); hand++) {
            boolean exact = history.outcome(hand, 0) == ro.whistlab.core.session.ScoreHistory.Outcome.EXACT;
            if (exact) exactSizes.add(schedule.get(hand - 1));
            if (hand > schedule.size() - 3) exactFinalThree &= exact;
            if (hand > schedule.size() - 5) exactFinalFive &= exact;
        }
        int midpoint = schedule.size() / 2;
        int midpointScore = history.total(midpoint, 0);
        long midHigher = game.snapshot().players().stream()
                .filter(player -> history.total(midpoint, player.seat()) > midpointScore).count();
        int finalScore = game.snapshot().matchScore(0);
        long finalHigher = game.snapshot().players().stream()
                .filter(player -> game.snapshot().matchScore(player.seat()) > finalScore).count();
        int midRank = Math.toIntExact(midHigher) + 1;
        int finalRank = Math.toIntExact(finalHigher) + 1;
        var recorded = WhistAchievementState.empty().recordMatch("ledger-check", facts);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "exactFinalThree", exactFinalThree,
                "exactFinalFive", exactFinalFive, "exactSizes", exactSizes, "facts", facts,
                "finalRank", finalRank, "game", game, "midRank", midRank, "recorded", recorded,
                "schedule", schedule);
        assertEquals("coverage counts distinct hand sizes with at least one exact result",
                exactSizes.size(), facts.exactHandSizeCount());
        assertEquals("last-hand fact uses the final ledger row",
                history.outcome(schedule.size(), 0) == ro.whistlab.core.session.ScoreHistory.Outcome.EXACT,
                facts.exactFinalHand());
        assertEquals("three-hand finish requires all three final rows to be exact", exactFinalThree,
                facts.exactFinalThree());
        assertEquals("five-hand finish requires all five final rows to be exact", exactFinalFive,
                facts.exactFinalFive());
        assertEquals("rank hold compares final place to halfway place", finalRank <= midRank,
                facts.holdOrImproveRank());
        assertEquals("four-seat top three uses final rank with ties", finalRank <= 3,
                facts.topThreeAtFour());
        assertEquals("four-seat top two uses final rank with ties", finalRank <= 2,
                facts.topTwoAtFour());
        assertEquals("rank climb measures at least one place", midRank - finalRank >= 1,
                facts.climbOneRank());
        assertEquals("comeback requires last at midpoint and top two at finish",
                midRank == game.config().playerCount() && finalRank <= 2, facts.lastToTopTwo());
        assertEquals("recorded exact-finish progress follows its ledger fact", facts.exactFinalHand() ? 1 : 0,
                recorded.progress("exact_final_hand"));
        assertEquals("recorded size coverage is best-match progress", facts.exactHandSizeCount(),
                recorded.progress("exact_seven_hand_sizes"));
        assertEquals("recorded comeback progress follows tie-aware ranks", facts.lastToTopTwo() ? 1 : 0,
                recorded.progress("last_to_top_two"));
    }

    @Test public void versionOneProgressMigratesWithoutInventingPendingHandNotices() throws Exception {
        var restored = WhistAchievementCodec.decode(
                "{\"version\":1,\"progress\":{\"first_match\":1},\"processedMatches\":[\"legacy\"]}");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "restored", restored);
        assertEquals("legacy career progress is retained", 1, restored.progress("first_match"));
        assertTrue("legacy match id still deduplicates", restored.processedMatches().contains("legacy"));
        assertTrue("legacy records contain no fabricated hand ids", restored.processedHands().isEmpty());
        assertTrue("legacy records contain no fabricated popup notices", restored.pendingNotifications().isEmpty());
    }

    @Test public void failedNoticeAcknowledgementLeavesTheDurablePopupQueued() throws Exception {
        var saved = new WhistAchievementState(java.util.Map.of("first_exact", 1), java.util.Set.of(),
                java.util.Set.of("match#hand:1"), java.util.Set.of("first_exact"));
        var storage = new MemoryStorage();
        storage.value = WhistAchievementCodec.encode(saved);
        storage.failure = new IOException("Notice state disk full");
        var repository = new WhistAchievementRepository(storage);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "repository", repository,
                "saved", saved, "storage", storage);
        assertThrows("failed acknowledgement must be surfaced", IOException.class,
                () -> repository.acknowledgeNotifications(List.of("first_exact")));
        assertEquals("failed acknowledgement does not clear the notice from durable storage",
                List.of("first_exact"), repository.pendingNotifications().stream()
                        .map(WhistAchievementCatalog.Goal::id).toList());
    }

    @Test public void actualZeroGameAwardAtHumanSeatCreditsTheFiveHundredPointAchievement() throws Exception {
        var game = finishedZeroGame();
        var facts = WhistMatchFacts.from(game).orElseThrow();
        var storage = new MemoryStorage();
        var repository = new WhistAchievementRepository(storage);
        repository.record("zero-game-match", game);
        var state = repository.state();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "facts", facts,
                "game", game, "repository", repository, "state", state);
        assertTrue("the deterministic finished match reaches zero with the rule enabled",
                game.snapshot().earnedZeroGame(0));
        assertEquals("the achievement must use the credited zero-game outcome", 1,
                facts.measure(WhistAchievementCatalog.Metric.ZERO_GAME_MATCHES));
        assertEquals("the hard goal must be persisted exactly once", 1, state.progress("zero_game_500"));
        assertEquals("ranked result must include the actual 500-point award", 500, game.snapshot().matchScore(0));
    }

    @Test public void malformedOrFutureProgressIsReportedInsteadOfSilentlyReset() {
        var malformed = new MemoryStorage();
        malformed.value = "{not-json";
        var repository = new WhistAchievementRepository(malformed);
        var failure = assertThrows("The operation rejects corrupt progress with IOException",
                IOException.class, repository::state);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure,
                "malformed", malformed, "repository", repository);
        assertTrue("corrupt progress must remain a visible storage problem", failure.getMessage().contains("invalid"));
    }

    private static GameSession finishedMatch(int players, GameMode mode, boolean streaks,
            boolean allowZero, long seed) {
        var config = GameConfig.newMatch(players, mode, AiDifficulty.NORMAL, seed, streaks, 10, allowZero);
        var game = GameSession.newGame(config);
        while (true) {
            finishHand(game);
            if (game.snapshot().finalHand()) return game;
            game.startNextHand();
        }
    }

    private static GameSession finishedZeroGame() {
        long seed = 2727L;
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, seed, true, 10));
        var bids = new java.util.Random(seed ^ 0xC0FFEE);
        while (true) {
            var state = game.snapshot();
            if (state.phase() == GamePhase.SCORE) {
                if (state.finalHand()) return game;
                game.startNextHand();
            } else if (state.phase() == GamePhase.BIDDING) {
                var legal = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1);
                game.submitBid(state.biddingSeat(), legal.get(bids.nextInt(legal.size())));
            } else {
                var ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), ledSuit,
                        state.trump(), state.mode());
                game.playCard(state.currentSeat(), legal.get(0));
            }
        }
    }

    private static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            var legal = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1);
            game.submitBid(state.biddingSeat(), legal.get(0));
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            var ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), ledSuit,
                    state.trump(), state.mode());
            game.playCard(state.currentSeat(), legal.get(0));
        }
    }

    private static final class MemoryStorage implements WhistAchievementRepository.Storage {
        private String value;
        private int writes;
        private IOException failure;
        @Override public String read() { return value; }
        @Override public void write(String json) throws IOException {
            if (failure != null) throw failure;
            value = json;
            writes++;
        }
    }

}

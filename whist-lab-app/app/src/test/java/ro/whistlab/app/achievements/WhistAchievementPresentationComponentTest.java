package ro.whistlab.app.achievements;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Goal;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

public final class WhistAchievementPresentationComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void catalogueProjectionKeepsEveryGoalVisibleAndCapsProgressAtItsTarget() {
        var state = new WhistAchievementState(Map.of("first_exact", 3, "exact_small_rate", 74),
                Set.of(), Set.of(), Set.of());
        var catalogue = WhistAchievementPresentation.catalogue(state, new WhistUiText(HowToPlay.Language.RO));
        var firstGroup = catalogue.groups().get(0);
        var firstExact = goal(catalogue, "first_exact");
        var firstMatch = goal(catalogue, "first_match");
        var smallRate = goal(catalogue, "exact_small_rate");

        atf.gradle.audit.GradleInputs.fixture("Projection inputs before first assertion", "state", state,
                "catalogue", catalogue, "firstExact", firstExact, "firstMatch", firstMatch,
                "smallRate", smallRate);
        assertEquals("the catalogue shows all eleven chapters", 11, catalogue.groups().size());
        assertEquals("all fifty-five goals remain visible", 55,
                catalogue.groups().stream().mapToInt(group -> group.goals().size()).sum());
        assertEquals("the first chapter keeps its five catalog entries", 5, firstGroup.goals().size());
        assertEquals("progress above a threshold is displayed at the completed target", "Progres 1/1",
                firstExact.progress());
        assertEquals("unfinished goals remain visible at zero", "Progres 0/1", firstMatch.progress());
        assertEquals("match-relative metrics retain percentage formatting", "Progres 50%/50%",
                smallRate.progress());
        assertTrue("earned state and completion marker are projected together", firstExact.complete()
                && firstExact.marker().equals("✓"));
        assertTrue("the earliest untouched chapter is marked as recommended",
                catalogue.groups().get(2).recommended() && catalogue.groups().get(2).heading().contains("Recomandat"));
        assertTrue("the summary reports earned progress without hiding other chapters",
                catalogue.summary().contains("2 din 55 de realizări"));
        assertThrows("projected chapter lists cannot be mutated", UnsupportedOperationException.class,
                () -> catalogue.groups().clear());
        assertThrows("projected goal rows cannot be mutated", UnsupportedOperationException.class,
                () -> firstGroup.goals().clear());
    }

    @Test public void catalogueProjectionCoversEveryLocalizedCatalogEntryInStableOrder() {
        for (HowToPlay.Language language : List.of(HowToPlay.Language.RO, HowToPlay.Language.EN)) {
            var copy = new WhistUiText(language);
            var catalogue = WhistAchievementPresentation.catalogue(WhistAchievementState.empty(), copy);
            boolean romanian = language == HowToPlay.Language.RO;

            atf.gradle.audit.GradleInputs.fixture("Localized catalogue inputs before first assertion",
                    "language", language, "catalogue", catalogue);
            assertEquals("summary follows the current language", copy.achievementsSummary(0, 55, 1),
                    catalogue.summary());
            for (int groupIndex = 0; groupIndex < WhistAchievementCatalog.groups().size(); groupIndex++) {
                var expectedGroup = WhistAchievementCatalog.groups().get(groupIndex);
                var actualGroup = catalogue.groups().get(groupIndex);
                assertEquals("group number and order match the stable catalog", expectedGroup.number(),
                        actualGroup.number());
                assertTrue("group heading uses its localized catalog title",
                        actualGroup.heading().contains(expectedGroup.title(romanian)));
                assertEquals("only the next untouched group is recommended", groupIndex == 0,
                        actualGroup.recommended());
                assertEquals("group rows preserve catalog order", expectedGroup.goals().stream()
                        .map(WhistAchievementCatalog.Goal::id).toList(), actualGroup.goals().stream()
                        .map(WhistAchievementPresentation.GoalRow::id).toList());

                for (int goalIndex = 0; goalIndex < expectedGroup.goals().size(); goalIndex++) {
                    var expected = expectedGroup.goals().get(goalIndex);
                    var actual = actualGroup.goals().get(goalIndex);
                    String progress = switch (expected.metric()) {
                        case EXACT_SMALL_RATE, EXACT_MIDDLE_RATE, EXACT_LARGE_RATE, EXACT_MATCH_RATE ->
                                copy.achievementPercent(0, expected.target());
                        default -> copy.achievementProgress(0, expected.target());
                    };
                    assertEquals("goal title follows its catalog locale", expected.title(romanian), actual.title());
                    assertEquals("goal level follows its localized tier", copy.achievementLevel(expected.level()),
                            actual.level());
                    assertEquals("goal description follows its catalog locale", expected.description(romanian),
                            actual.description());
                    assertEquals("unearned goal progress starts at zero in its metric format", progress,
                            actual.progress());
                    assertEquals("unearned goals use the pending marker", "○", actual.marker());
                    assertFalse("unearned goals are not marked complete", actual.complete());
                }
            }
        }
    }

    @Test public void unlockNoticeUsesCatalogOrderAndNamesTheCompletedHandInRomanian() {
        var state = stateWithEarned("first_exact", "exact_one_card");
        Goal firstExact = WhistAchievementCatalog.goal("first_exact");
        Goal oneCard = WhistAchievementCatalog.goal("exact_one_card");
        var notice = WhistAchievementPresentation.notice(List.of(oneCard, firstExact), state, 5, false,
                new WhistUiText(HowToPlay.Language.RO));

        atf.gradle.audit.GradleInputs.fixture("Unlock notice inputs before first assertion", "state", state,
                "requestedOrder", List.of(oneCard, firstExact), "notice", notice);
        assertEquals("multiple unlocks use the localized plural title", "Realizări noi", notice.title());
        assertTrue("summary names this hand, the total progress, and the recommendation",
                notice.summary().contains("Ai obținut 2 · 2 / 55 realizări · Mâna 5 s-a încheiat")
                        && notice.summary().contains("Capitol recomandat 3"));
        assertEquals("notice rows follow stable catalog order instead of set insertion order",
                List.of("first_exact", "exact_one_card"), notice.goals().stream()
                        .map(WhistAchievementPresentation.GoalRow::id).toList());
        assertTrue("every newly earned row carries its celebration marker",
                notice.goals().stream().allMatch(row -> row.marker().equals("★") && row.complete()));
        assertThrows("notice rows cannot be mutated", UnsupportedOperationException.class,
                () -> notice.goals().clear());
    }

    @Test public void unlockNoticeRejectsEmptyDuplicateAndInvalidHandInputs() {
        var state = stateWithEarned("first_exact");
        Goal goal = WhistAchievementCatalog.goal("first_exact");
        var copy = new WhistUiText(HowToPlay.Language.RO);

        atf.gradle.audit.GradleInputs.fixture("Invalid notice inputs before first assertion", "state", state,
                "goal", goal, "language", HowToPlay.Language.RO);
        assertThrows("empty notices cannot produce a popup", IllegalArgumentException.class,
                () -> WhistAchievementPresentation.notice(List.of(), state, 1, false, copy));
        assertThrows("duplicate IDs cannot produce duplicate popup rows", IllegalArgumentException.class,
                () -> WhistAchievementPresentation.notice(List.of(goal, goal), state, 1, false, copy));
        assertThrows("a notice must identify a completed hand", IllegalArgumentException.class,
                () -> WhistAchievementPresentation.notice(List.of(goal), state, 0, false, copy));
    }

    @Test public void unlockNoticeUsesFinalMatchCopyInEnglish() {
        var state = stateWithEarned("first_match");
        var notice = WhistAchievementPresentation.notice(List.of(WhistAchievementCatalog.goal("first_match")),
                state, 12, true, new WhistUiText(HowToPlay.Language.EN));

        atf.gradle.audit.GradleInputs.fixture("Final match notice inputs before first assertion", "state", state,
                "notice", notice);
        assertEquals("one unlock uses the singular English title", "Achievement unlocked", notice.title());
        assertTrue("final-match notice reports the match rather than the last hand",
                notice.summary().contains("Match complete") && !notice.summary().contains("Hand 12 complete"));
    }

    private static WhistAchievementPresentation.GoalRow goal(WhistAchievementPresentation.Catalogue catalogue,
            String id) {
        return catalogue.groups().stream().flatMap(group -> group.goals().stream())
                .filter(row -> row.id().equals(id)).findFirst().orElseThrow();
    }

    private static WhistAchievementState stateWithEarned(String... ids) {
        var progress = new java.util.LinkedHashMap<String, Integer>();
        var pending = new LinkedHashSet<String>();
        for (String id : ids) {
            progress.put(id, WhistAchievementCatalog.goal(id).target());
            pending.add(id);
        }
        return new WhistAchievementState(progress, Set.of(), Set.of(), pending);
    }
}

package ro.whistlab.app.achievements;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.achievements.WhistMatchFacts.ContractPatterns;
import ro.whistlab.app.achievements.WhistMatchFacts.Recovery;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public final class WhistContractAchievementsComponentTest extends GradleAuditedTest {
    @Test public void zeroRequiresTheBidAndTheResultAndCountsEachBandOnce() {
        var hands = List.of(hand(1, 1, 0, 0), hand(2, 2, 0, 0), hand(3, 3, 0, 0),
                hand(4, 4, 0, 1), hand(5, 5, 0, 0), hand(6, 8, 1, 0), hand(7, 9, 0, 0));
        GradleInputs.fixture("Exact zeros, a bid mismatch and two outside sizes", "ledger rows", hands);
        assertEquals("Only two distinct bands qualify", 2, ContractPatterns.from(hands).zeroBands());
        var withEight = new ArrayList<>(hands);
        withEight.add(hand(8, 8, 0, 0));
        assertEquals("An exact eight-card zero completes the third band", 3, ContractPatterns.from(withEight).zeroBands());
        var before = WhistAchievementState.empty();
        for (var hand : withEight) before = before.recordHand(hand.eventId("zero-witness"), hand);
        assertEquals("Only exact zeros in hands of two through eight count", 4, before.progress("zero_multicard"));
        assertEquals("The short band accumulates both different hands", 2, before.progress("zero_short_career"));
        assertEquals("A missed middle hand contributes nothing", 1, before.progress("zero_middle_career"));
        assertEquals("A positive bid with zero tricks cannot earn the eight-card zero", 1, before.progress("zero_eight_career"));
        assertEquals("A match-wide achievement awaits completion", 0, before.progress("zero_all_bands"));
    }

    @Test public void positiveAccuracyUsesAnExplicitMinimumAndTheExactDenominator() {
        var seven = positiveBids(7, 7);
        var below = positiveBids(20, 12);
        var at = positiveBids(20, 13);
        GradleInputs.fixture("Positive contracts around the sample-size and percentage boundaries",
                "too few", seven, "below", below, "at threshold", at);
        assertEquals("Seven positive bids are ineligible even at perfect accuracy", 0,
                ContractPatterns.from(seven).positiveRate());
        assertEquals("Other hands never dilute the denominator", 20, ContractPatterns.from(below).positiveOpportunities());
        assertEquals("Twelve out of twenty positive contracts is below target", 60, ContractPatterns.from(below).positiveRate());
        assertEquals("Thirteen out of twenty positive contracts hits the threshold", 65, ContractPatterns.from(at).positiveRate());
        assertEquals("A result just below 65% does not round up", 64,
                ContractPatterns.from(positiveBids(17, 11)).positiveRate());
    }

    @Test public void recoveringAfterAMissIsCountedOnceAndKeepsTheSignOfTheImmediateMiss() {
        var hands = results(0, 0, 0, 1, -1, 0, 0, 0, 0, 1, 0, 0, -1, 0, 1);
        GradleInputs.fixture("Exact runs, signed misses and an unredeemed final miss", "ledger rows", hands);
        assertEquals("An initial exact run, a fourth exact and the last unresolved miss add no recovery",
                new Recovery(3, 1, 2, 1, 1), Recovery.from(hands));
        assertEquals("Only the immediately preceding miss contributes its sign",
                new Recovery(1, 1, 0, 0, 0), Recovery.from(results(1, -1, 0)));
        assertEquals("Alternating the two directions confirms both kinds of recovery",
                new Recovery(2, 0, 0, 1, 0), Recovery.from(results(-1, 0, 1, 0)));
        assertEquals("A fourth exact cannot count twice, but a new miss starts a new recovery",
                new Recovery(3, 0, 3, 1, 3), Recovery.from(results(-1, 0, 0, 0, 0, 1, 0, 0, 0, -1, 0, 0, 0)));
        assertEquals("Two independently recovered overshoots and shortfalls reach the balanced target",
                2, Recovery.from(results(1, 0, -1, 0, -1, 0, 1, 0)).balancedRecoveries());
    }

    @Test public void oneHandCanAdvanceNewAndOldGoalsAndReloadDoesNotDoubleCredit() throws Exception {
        var hand = hand(8, 6, 3, 3);
        var state = WhistAchievementState.empty().recordHand(hand.eventId("original"), hand);
        var loaded = WhistAchievementCodec.decode(WhistAchievementCodec.encode(state));
        GradleInputs.fixture("A six-card exact positive result, then save and reload",
                "ledger row", hand, "pending", loaded.pendingNotifications());
        assertEquals("The first exact contract and two-plus contract unlock together",
                List.of("first_exact", "two_plus_multicard"),
                loaded.pendingGoals().stream().map(WhistAchievementCatalog.Goal::id).toList());
        assertEquals("The same hand also contributes to the larger career goals", List.of(1, 1),
                List.of(loaded.progress("three_plus_career"), loaded.progress("positive_large_career")));
        assertEquals("The same hand after reload leaves all progress and notifications intact",
                loaded, loaded.recordHand(hand.eventId("original"), hand));
        assertFalse("One three-trick success does not finish a three-success career goal",
                loaded.earned(WhistAchievementCatalog.goal("three_plus_career")));
        var third = loaded.recordHand(hand.eventId("second"), hand).recordHand(hand.eventId("third"), hand);
        assertTrue("Three independently scored successes reach the threshold",
                third.earned(WhistAchievementCatalog.goal("three_plus_career")));
    }

    private static List<WhistHandFacts> positiveBids(int count, int exact) {
        var hands = new ArrayList<WhistHandFacts>();
        hands.add(hand(1, 1, 1, 1));
        hands.add(hand(2, 4, 0, 0));
        for (int index = 0; index < count; index++)
            hands.add(hand(index + 3, 4, 1, index < exact ? 1 : 2));
        return hands;
    }

    private static List<WhistHandFacts> results(int... margins) {
        var hands = new ArrayList<WhistHandFacts>();
        for (int index = 0; index < margins.length; index++)
            hands.add(hand(index + 1, 4, 1, 1 + margins[index]));
        return hands;
    }

    private static WhistHandFacts hand(int number, int size, int bid, int tricks) {
        return new WhistHandFacts(number, size, bid, tricks, false, bid == tricks ? 1 : 0);
    }
}

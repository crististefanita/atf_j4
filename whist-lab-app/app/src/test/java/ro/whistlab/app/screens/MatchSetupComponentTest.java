package ro.whistlab.app.screens;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;

public class MatchSetupComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void zeroGameDefaultsOnForBothRomanianVariantsAndCanBeUnchecked() {
        var setup = new MatchSetup();
        var standard = GameMode.ROMANIAN_WHIST;
        var longMode = GameMode.LONG_ROMANIAN;
        atf.gradle.audit.GradleInputs.fixture("Start menu defaults and mode changes",
                "Romanian variants", java.util.List.of(standard, longMode), "seed", 31);
        assertTrue("The option starts checked", setup.allowZeroGame());
        assertFalse("The non-Romanian default does not use the rule", setup.config(31).allowZeroGame());
        setup.mode(standard);
        var firstMatch = setup.config(31);
        assertTrue("The standard Romanian variant starts enabled", firstMatch.allowZeroGame());
        setup.mode(longMode);
        assertTrue("The long Romanian variant starts enabled", setup.config(31).allowZeroGame());
        setup.allowZeroGame(false);
        setup.mode(GameMode.OH_HELL);
        setup.mode(standard);
        assertFalse("Returning to Romanian mode preserves an explicit opt-out", setup.config(31).allowZeroGame());
        assertTrue("Changing setup cannot change an already started match", firstMatch.allowZeroGame());
    }

    @Test public void initialChoicesProduceTheExistingDefaultConfiguration() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 4, "declared constant 2", 31, "declared constant 3", false, "declared constant 4", 10);
        assertEquals("new MatchSetup().config(31) must equal GameConfig.newMatch(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31, false, 10)", GameConfig.newMatch(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31, false, 10),
                new MatchSetup().config(31));
    }

    @Test public void changingModeRetainsRomanianChoicesWithoutEnablingStreaksElsewhere() {
        var setup = new MatchSetup();
        setup.playerCount(6);
        setup.difficulty(AiDifficulty.HARD);
        setup.mode(GameMode.ROMANIAN_WHIST);
        setup.streakBonus(5);
        var romanian = setup.config(31);
        setup.mode(GameMode.OH_HELL);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "romanian", romanian, "setup", setup);
        assertFalse("setup.config(31).streakBonuses() must remain false", setup.config(31).streakBonuses());
        setup.mode(GameMode.ROMANIAN_WHIST);
        assertEquals("setup.config(31) must equal romanian", romanian, setup.config(31));
        assertEquals("setup.playerCount() must equal 6", 6, setup.playerCount());
        assertEquals("setup.difficulty() must equal AiDifficulty.HARD", AiDifficulty.HARD, setup.difficulty());
    }

    @Test public void bothBonusesAndBothDifficultiesProduceIndependentMatchSnapshots() {
        for (int count = 3; count <= 6; count++) for (int bonus : new int[]{5, 10})
            for (var difficulty : AiDifficulty.values()) {
                var setup = new MatchSetup();
                setup.playerCount(count);
                setup.mode(GameMode.ROMANIAN_WHIST);
                setup.difficulty(difficulty);
                setup.streakBonus(bonus);
                var started = setup.config(31);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bonus", bonus, "count", count, "difficulty", difficulty, "setup", setup, "started", started);
                assertEquals("started must equal GameConfig.newMatch(count, GameMode.ROMANIAN_WHIST, difficulty, 31, true, bonus)", GameConfig.newMatch(count, GameMode.ROMANIAN_WHIST, difficulty, 31, true, bonus), started);
                setup.streaks(false);
                setup.streakBonus(bonus == 5 ? 10 : 5);
                assertTrue("started.streakBonuses() must hold", started.streakBonuses());
                assertEquals("started.streakBonusPoints() must equal bonus", bonus, started.streakBonusPoints());
                assertFalse("setup.config(32).streakBonuses() must remain false", setup.config(32).streakBonuses());
                assertEquals("setup.config(32).seed() must equal 32", 32, setup.config(32).seed());
            }
    }

    @Test public void invalidSelectionsNeverOverwriteRetainedChoices() {
        var setup = new MatchSetup();
        var before = setup.config(31);
        for (int count : new int[]{-1, 0, 2, 7, Integer.MAX_VALUE})
            {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "setup", setup);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> setup.playerCount(count)); }
        for (int bonus : new int[]{0, 7, 15}) assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> setup.streakBonus(bonus));
        assertThrows("The operation rejects its input with NullPointerException.class", NullPointerException.class, () -> setup.mode(null));
        assertThrows("The operation rejects its input with NullPointerException.class", NullPointerException.class, () -> setup.difficulty(null));
        assertEquals("setup.config(31) must equal before", before, setup.config(31));
    }
}

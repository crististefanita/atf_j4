package ro.whistlab.app.screens;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;

public class MatchSetupComponentTest {
    @Test public void initialChoicesProduceTheExistingDefaultConfiguration() {
        assertEquals(GameConfig.newMatch(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31, false, 10),
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
        assertFalse(setup.config(31).streakBonuses());
        setup.mode(GameMode.ROMANIAN_WHIST);
        assertEquals(romanian, setup.config(31));
        assertEquals(6, setup.playerCount());
        assertEquals(AiDifficulty.HARD, setup.difficulty());
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
                assertEquals(GameConfig.newMatch(count, GameMode.ROMANIAN_WHIST, difficulty, 31, true, bonus), started);
                setup.streaks(false);
                setup.streakBonus(bonus == 5 ? 10 : 5);
                assertTrue(started.streakBonuses());
                assertEquals(bonus, started.streakBonusPoints());
                assertFalse(setup.config(32).streakBonuses());
                assertEquals(32, setup.config(32).seed());
            }
    }

    @Test public void invalidSelectionsNeverOverwriteRetainedChoices() {
        var setup = new MatchSetup();
        var before = setup.config(31);
        for (int count : new int[]{-1, 0, 2, 7, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> setup.playerCount(count));
        for (int bonus : new int[]{0, 7, 15}) assertThrows(IllegalArgumentException.class, () -> setup.streakBonus(bonus));
        assertThrows(NullPointerException.class, () -> setup.mode(null));
        assertThrows(NullPointerException.class, () -> setup.difficulty(null));
        assertEquals(before, setup.config(31));
    }
}

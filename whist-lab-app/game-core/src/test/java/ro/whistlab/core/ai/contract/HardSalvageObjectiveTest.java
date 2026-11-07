package ro.whistlab.core.ai.contract;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;
import java.util.List;
import org.junit.Test;

public final class HardSalvageObjectiveTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void publicIrrecoverabilityRanksOwnPointsBeforeRivalMisses() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", true, "declared constant 2", 0, "declared constant 3", -2, "declared constant 4", 3, "declared constant 5", 100, "declared constant 6", 2, "declared constant 7", 20, "declared constant 8", -4);
        assertTrue("MonteCarloAi.prefer(true, true, 0, -2, 3, 100, 0, -2, 2, 20) must hold", MonteCarloAi.prefer(true, true, 0, -2, 3, 100,
                0, -2, 2, 20));
        assertFalse("MonteCarloAi.prefer(true, true, 0, -4, 3, 0, 0, -2, 2, 20) must remain false", MonteCarloAi.prefer(true, true, 0, -4, 3, 0,
                0, -2, 2, 20));
    }

    @Test public void noisyBurnForecastCannotOutrankOwnPointProtection() {
        // Captured at seed 20261108, hand 15, seat 1 after the shared first trick.
        // Q♣ forecasts more rival misses, but the completed continuation lost one
        // extra own point and let one more rival hit contract.

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", true, "declared constant 2", 0, "declared constant 3", 1.3125, "declared constant 4", 2.0, "declared constant 5", 40.0, "declared constant 6", 1.21875, "declared constant 7", 1.78125);
        assertFalse("MonteCarloAi.prefer(true, true, 0, -1.3125, 2.0, 40.0, 0, -1.21875, 1.78125, 40.0, HardStrategyProfile.CURRENT) must remain false", MonteCarloAi.prefer(true, true, 0, -1.3125, 2.0, 40.0,
                0, -1.21875, 1.78125, 40.0, HardStrategyProfile.CURRENT));
        assertTrue("MonteCarloAi.prefer(true, true, 0, -1.3125, 2.0, 40.0, 0, -1.21875, 1.78125, 40.0, HardStrategyProfile.BURN_IF_DOOMED) must hold", MonteCarloAi.prefer(true, true, 0, -1.3125, 2.0, 40.0,
                0, -1.21875, 1.78125, 40.0, HardStrategyProfile.BURN_IF_DOOMED));
    }

    @Test public void aRecoverableContractKeepsExactChanceAheadOfPointsOrRivalDamage() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", true, "declared constant 2", false, "declared constant 3", 0.6, "declared constant 4", -2, "declared constant 5", 3, "declared constant 6", 0, "declared constant 7", 0.5, "declared constant 8", 7, "declared constant 9", -100);
        assertTrue("MonteCarloAi.prefer(true, false, 0.6, -2, 3, 0, 0.5, 7, 0, -100) must hold", MonteCarloAi.prefer(true, false, 0.6, -2, 3, 0,
                0.5, 7, 0, -100));
    }

    @Test public void rolloutUtilityCountsHowManyOpponentsMiss() {
        MonteCarloAi.View view = new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,
                Trump.NO_TRUMP, 0, 3, 4, List.of(new Card(Suit.SPADES, 14)),
                List.of(new MonteCarloAi.Seat(1, 0, 0), new MonteCarloAi.Seat(1, 1, 0),
                        new MonteCarloAi.Seat(1, 2, 0), new MonteCarloAi.Seat(1, 3, 0)),
                List.of(), List.of(), null);
        double[] result = MonteCarloAi.utility(view, new int[]{0, 2, 1, 3}, new int[]{0, 1, 2, 3});

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "result", result, "view", view);
        assertEquals(2.0, result[3], 0.0);
    }
}

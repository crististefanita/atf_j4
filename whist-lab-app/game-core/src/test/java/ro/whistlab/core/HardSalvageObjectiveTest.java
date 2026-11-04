package ro.whistlab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public final class HardSalvageObjectiveTest {
    @Test public void publicIrrecoverabilityRanksOwnPointsBeforeRivalMisses() {
        assertTrue(MonteCarloAi.prefer(true, true, 0, -2, 3, 100,
                0, -2, 2, 20));
        assertFalse(MonteCarloAi.prefer(true, true, 0, -4, 3, 0,
                0, -2, 2, 20));
    }

    @Test public void aRecoverableContractKeepsExactChanceAheadOfPointsOrRivalDamage() {
        assertTrue(MonteCarloAi.prefer(true, false, 0.6, -2, 3, 0,
                0.5, 7, 0, -100));
    }

    @Test public void rolloutUtilityCountsHowManyOpponentsMiss() {
        MonteCarloAi.View view = new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,
                Trump.NO_TRUMP, 0, 3, 4, List.of(new Card(Suit.SPADES, 14)),
                List.of(new MonteCarloAi.Seat(1, 0, 0), new MonteCarloAi.Seat(1, 1, 0),
                        new MonteCarloAi.Seat(1, 2, 0), new MonteCarloAi.Seat(1, 3, 0)),
                List.of(), List.of(), null);
        double[] result = MonteCarloAi.utility(view, new int[]{0, 2, 1, 3}, new int[]{0, 1, 2, 3});
        assertEquals(2.0, result[3], 0.0);
    }
}
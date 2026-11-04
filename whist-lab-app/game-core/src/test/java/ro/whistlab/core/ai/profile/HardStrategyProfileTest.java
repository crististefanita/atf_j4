package ro.whistlab.core.ai.profile;

import ro.whistlab.core.ai.engine.MonteCarloAi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** Small, visible checks for the offline-only strategy variants. */
public final class HardStrategyProfileTest {
    @Test public void prudentBidChoosesLowerBidOnlyInsideItsDeclaredProbabilityBand() {
        List<MonteCarloAi.BidOption> options=List.of(
                new MonteCarloAi.BidOption(0,.66,4,128),
                new MonteCarloAi.BidOption(1,.70,6,128),
                new MonteCarloAi.BidOption(2,.74,6,128),
                new MonteCarloAi.BidOption(3,.20,8,128));

        assertEquals(2,MonteCarloAi.selectBid(options,HardStrategyProfile.CURRENT,true));
        assertEquals(3,MonteCarloAi.selectBid(options,HardStrategyProfile.SCORE_FIRST,true));
        assertEquals(1,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID_04,true));
        assertEquals(0,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID,true));
        assertEquals(0,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID_12,true));
        assertEquals(3,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID,false));
    }

    @Test public void scoreFirstOrdersExpectedPointsBeforeExactProbability() {
        assertTrue(MonteCarloAi.prefer(true,false,.60,8,0,20,
                .75,5,0,30,HardStrategyProfile.SCORE_FIRST));
        assertTrue(!MonteCarloAi.prefer(true,false,.60,8,0,20,
                .75,5,0,30,HardStrategyProfile.CURRENT));
    }

    @Test public void burnProfilePrioritizesRivalMissesOnlyAfterOwnQuotaIsImpossible() {
        assertTrue(MonteCarloAi.prefer(true,true,.1,1,3,90,
                .1,5,1,20,HardStrategyProfile.BURN_IF_DOOMED));
        assertTrue(!MonteCarloAi.prefer(true,false,.1,1,3,90,
                .9,5,1,20,HardStrategyProfile.BURN_IF_DOOMED));
    }
}

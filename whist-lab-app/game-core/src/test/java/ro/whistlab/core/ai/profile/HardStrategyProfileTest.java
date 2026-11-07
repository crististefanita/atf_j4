package ro.whistlab.core.ai.profile;

import ro.whistlab.core.ai.engine.MonteCarloAi;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.util.List;
import org.junit.Test;

/** Small, visible checks for the offline-only strategy variants. */
public final class HardStrategyProfileTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void prudentBidChoosesLowerBidOnlyInsideItsDeclaredProbabilityBand() {
        List<MonteCarloAi.BidOption> options=List.of(
                new MonteCarloAi.BidOption(0,.66,4,128),
                new MonteCarloAi.BidOption(1,.70,6,128),
                new MonteCarloAi.BidOption(2,.74,6,128),
                new MonteCarloAi.BidOption(3,.20,8,128));



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "options", options);
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.CURRENT, true) must equal 2", 2,MonteCarloAi.selectBid(options,HardStrategyProfile.CURRENT,true));
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.SCORE_FIRST, true) must equal 3", 3,MonteCarloAi.selectBid(options,HardStrategyProfile.SCORE_FIRST,true));
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.PRUDENT_BID_04, true) must equal 1", 1,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID_04,true));
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.PRUDENT_BID, true) must equal 0", 0,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID,true));
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.PRUDENT_BID_12, true) must equal 0", 0,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID_12,true));
        assertEquals("MonteCarloAi.selectBid(options, HardStrategyProfile.PRUDENT_BID, false) must equal 3", 3,MonteCarloAi.selectBid(options,HardStrategyProfile.PRUDENT_BID,false));
    }

    @Test public void scoreFirstOrdersExpectedPointsBeforeExactProbability() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", true, "declared constant 2", false, "declared constant 3", .60, "declared constant 4", 8, "declared constant 5", 0, "declared constant 6", 20, "declared constant 7", .75, "declared constant 8", 5, "declared constant 9", 30);
        assertTrue("MonteCarloAi.prefer(true, false, 0.6, 8, 0, 20, 0.75, 5, 0, 30, HardStrategyProfile.SCORE_FIRST) must hold", MonteCarloAi.prefer(true,false,.60,8,0,20,
                .75,5,0,30,HardStrategyProfile.SCORE_FIRST));
        assertTrue("!MonteCarloAi.prefer(true, false, 0.6, 8, 0, 20, 0.75, 5, 0, 30, HardStrategyProfile.CURRENT) must hold", !MonteCarloAi.prefer(true,false,.60,8,0,20,
                .75,5,0,30,HardStrategyProfile.CURRENT));
    }

    @Test public void burnProfilePrioritizesRivalMissesOnlyAfterOwnQuotaIsImpossible() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", true, "declared constant 2", .1, "declared constant 3", 1, "declared constant 4", 3, "declared constant 5", 90, "declared constant 6", 5, "declared constant 7", 20, "declared constant 8", false, "declared constant 9", .9);
        assertTrue("MonteCarloAi.prefer(true, true, 0.1, 1, 3, 90, 0.1, 5, 1, 20, HardStrategyProfile.BURN_IF_DOOMED) must hold", MonteCarloAi.prefer(true,true,.1,1,3,90,
                .1,5,1,20,HardStrategyProfile.BURN_IF_DOOMED));
        assertTrue("!MonteCarloAi.prefer(true, false, 0.1, 1, 3, 90, 0.9, 5, 1, 20, HardStrategyProfile.BURN_IF_DOOMED) must hold", !MonteCarloAi.prefer(true,false,.1,1,3,90,
                .9,5,1,20,HardStrategyProfile.BURN_IF_DOOMED));
    }
}

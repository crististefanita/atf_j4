package ro.whistlab.core.rules;

import ro.whistlab.core.model.Trump;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import org.junit.Test;

public class GameRulesBoundaryTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void noWinnerExistsBeforeAnyCardWasPlayed() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion");
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.winningSeat(List.of(),Trump.NO_TRUMP));
    }
    @Test public void bidsAndTricksCannotBeNegativeInEitherScoringProfile() {
        for(GameMode mode:List.of(GameMode.ROMANIAN_WHIST,GameMode.OH_HELL)) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "mode", mode);
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.scoreDelta(0,-1,mode));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.scoreDelta(-1,0,mode));
        }
    }
    @Test public void finalBidPredicateRejectsImpossibleInputRanges() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", -1, "declared constant 2", 0, "declared constant 3", 1);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(-1,0,1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(0,-1,1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(0,0,0));
    }
    @Test public void bidOptionsRejectInvalidHandSizeAndNegativePriorTotal() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", true, "declared constant 3", 1, "declared constant 4", -1);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.allowedBids(0,0,true));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameRules.allowedBids(1,-1,true));
    }
}

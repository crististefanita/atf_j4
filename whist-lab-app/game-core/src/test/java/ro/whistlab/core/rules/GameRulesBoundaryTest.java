package ro.whistlab.core.rules;

import ro.whistlab.core.model.Trump;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

public class GameRulesBoundaryTest {
    @Test public void noWinnerExistsBeforeAnyCardWasPlayed() {
        assertThrows(IllegalArgumentException.class,()->GameRules.winningSeat(List.of(),Trump.NO_TRUMP));
    }
    @Test public void bidsAndTricksCannotBeNegativeInEitherScoringProfile() {
        for(GameMode mode:List.of(GameMode.ROMANIAN_WHIST,GameMode.OH_HELL)) {
            assertThrows(IllegalArgumentException.class,()->GameRules.scoreDelta(0,-1,mode));
            assertThrows(IllegalArgumentException.class,()->GameRules.scoreDelta(-1,0,mode));
        }
    }
    @Test public void finalBidPredicateRejectsImpossibleInputRanges() {
        assertThrows(IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(-1,0,1));
        assertThrows(IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(0,-1,1));
        assertThrows(IllegalArgumentException.class,()->GameRules.isForbiddenLastBid(0,0,0));
    }
    @Test public void bidOptionsRejectInvalidHandSizeAndNegativePriorTotal() {
        assertThrows(IllegalArgumentException.class,()->GameRules.allowedBids(0,0,true));
        assertThrows(IllegalArgumentException.class,()->GameRules.allowedBids(1,-1,true));
    }
}

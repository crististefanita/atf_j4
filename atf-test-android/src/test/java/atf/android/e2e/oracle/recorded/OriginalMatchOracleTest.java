package atf.android.e2e.oracle.recorded;

import atf.testkit.assertions.AuditedAssertions;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import atf.testkit.AtfComponentTest;

@AtfComponentTest
class OriginalMatchOracleTest {
    @Test void detectsChangedBidsAndPrematureMidHandScoring() {
        var s=deal();s.remaining=new int[]{1,1,1};s.cards=new int[][]{{26},{27},{2}};
        s.bids=new int[]{1,0,0};s.won=new int[]{1,0,0};s.turn=0;s.handOver=false;
        s.history=List.of(0,25,24,23);s.scores=new int[][]{{0,0,0}};
        var oracle=new OriginalMatchOracle(deal());
        AuditedAssertions.assertTrue(oracle.verify(s,new int[3]).stream().allMatch(a->a.passed()));
        s.bids=new int[]{0,1,0};s.scores[0][0]=1;
        var checks=oracle.verify(s,new int[3]);
        AuditedAssertions.assertTrue(checks.stream().anyMatch(a->!a.passed()&&a.name().startsWith("Bids remain")));
        AuditedAssertions.assertTrue(checks.stream().anyMatch(a->!a.passed()&&a.name().contains("stays unchanged")));
    }
    @Test void rejectsABiddingPrefixThatDoesNotStartAfterTheDealer() {
        var deal=deal();deal.bids=new int[]{-1,0,-1};
        var checks=new OriginalMatchOracle(deal).verify(completed(),new int[3]);
        AuditedAssertions.assertTrue(checks.stream().anyMatch(a->!a.passed()&&a.name().contains("Bidders before")));
    }
    @Test void independentlyChecksLedSuitTrumpWinnerAndCumulativeBonus() {
        OriginalMatchOracle oracle=new OriginalMatchOracle(deal());
        var checks=oracle.verify(completed(),new int[3]);
        AuditedAssertions.assertTrue(checks.stream().allMatch(a->a.passed()),checks.toString());
        AuditedAssertions.assertArrayEquals(new int[]{1,0,1},oracle.won);
        AuditedAssertions.assertArrayEquals(new int[]{11,10,1},oracle.totals);
    }
    @Test void detectsWrongScoreAndWrongWinnerCounter() {
        var bad=completed();bad.scores[0][0]=12;bad.won[2]=0;
        var checks=new OriginalMatchOracle(deal()).verify(bad,new int[3]);
        AuditedAssertions.assertTrue(checks.stream().anyMatch(a->!a.passed()&&a.name().contains("cumulative score")));
        AuditedAssertions.assertTrue(checks.stream().anyMatch(a->!a.passed()&&a.name().contains("trick counter")));
    }
    @Test void detectsOffSuitPlayWhileHoldingTheLedSuitAndAlteredHistory() {
        var oracle=new OriginalMatchOracle(deal());var bad=completed();
        bad.history=List.of(0,25,27,23,26,24,2);
        AuditedAssertions.assertTrue(oracle.verify(bad,new int[3]).stream().anyMatch(a->!a.passed()&&a.name().startsWith("Move")));
        bad.history=List.of(0,25,24,23,26,27,2);
        AuditedAssertions.assertTrue(oracle.verify(bad,new int[3]).stream().anyMatch(a->!a.passed()&&a.name().contains("earlier move")));
    }
    private static OriginalWhistState deal() {
        var d=new OriginalWhistState();d.players=3;d.mode=6;d.deck=1;d.handSize=2;d.dealer=2;d.trump=0;d.trumpCard=0;
        d.cards=new int[][]{{25,26},{24,27},{23,2}};d.remaining=new int[]{2,2,2};
        return d;
    }
    private static OriginalWhistState completed() {
        var s=deal();s.remaining=new int[]{0,0,0};s.cards=new int[][]{{},{},{}};
        s.bids=new int[]{1,0,0};s.won=new int[]{1,0,1};s.turn=2;s.handOver=true;
        s.history=List.of(0,25,24,23,26,27,2);s.scores=new int[][]{{11,10,1}};return s;
    }
}

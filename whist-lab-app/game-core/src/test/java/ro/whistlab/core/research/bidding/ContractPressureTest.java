package ro.whistlab.core.research.bidding;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.testing.CapturedRegression;

import java.util.*;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public class ContractPressureTest extends atf.gradle.audit.GradleAuditedTest {
    private static Card c(String code){return LossMiningTest.card(code);}
    private static List<Card> hand(String... codes){return Arrays.stream(codes).map(ContractPressureTest::c).toList();}
    private static MonteCarloAi.View view(int actor,List<Card> hand,int[] bids,int[] won,int[] remaining,
            List<PlayedCard> table,List<PlayedCard> history,Card turned) {
        var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(remaining[s],bids[s],won[s]));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,actor,3,8,hand,seats,table,history,turned);
    }
    @Test public void quotaCountsTheUnfinishedTrickOnce()throws Exception {
        var q=ContractPressure.state(0,new int[]{3,0,0,0},new int[]{2,0,0,0},new int[]{3,2,2,2},3);
        CapturedRegression.run("pressure-quota-partial",getClass().getName(),"quotaCountsTheUnfinishedTrickOnce",Map.of("rule","Three own cards and three table cards mean three remaining tricks, not two or four. Need is bid minus completed tricks."),r->{
            r.equal("Still needed",1,q.needed());r.equal("Opportunities including current trick",3,q.opportunities());r.equal("Required fraction",1.0/3,q.density());r.equal("Still recoverable",false,q.impossible());
        });
    }
    @Test public void signedTableDifferenceSeparatesOverAndUnderbidding()throws Exception {
        int[] remaining={2,2,2,2},zero={0,0,0,0};
        CapturedRegression.run("pressure-table-balance",getClass().getName(),"signedTableDifferenceSeparatesOverAndUnderbidding",Map.of("rule","Table difference is sum of remaining signed contract needs minus available tricks. It is not the own contract deficit."),r->{
            r.equal("One contested excess trick",1,ContractPressure.state(0,new int[]{1,1,1,0},zero,remaining,0).tableDifference());
            r.equal("One unwanted trick at table",-1,ContractPressure.state(0,new int[]{1,0,0,0},zero,remaining,0).tableDifference());
        });
    }
    @Test public void overAndUnreachableContractsAreBothIrrecoverable()throws Exception {
        int[] remaining={2,2,2,2},bids={3,0,0,0};
        CapturedRegression.run("pressure-impossible",getClass().getName(),"overAndUnreachableContractsAreBothIrrecoverable",Map.of("rule","Need greater than remaining opportunities or negative need cannot recover an exact contract; zero need can still preserve it."),r->{
            r.equal("Need three with two tricks left",true,ContractPressure.state(0,bids,new int[4],remaining,0).impossible());
            r.equal("Already won more than bid",true,ContractPressure.state(0,new int[]{1,0,0,0},new int[]{2,0,0,0},remaining,0).impossible());
            r.equal("Exactly at contract",false,ContractPressure.state(0,new int[]{1,0,0,0},new int[]{1,0,0,0},remaining,0).impossible());
        });
    }
    @Test public void exposedAndPlayedHigherTrumpsPromoteTheHeldQueen()throws Exception {
        var v=view(0,hand("QS","7D"),new int[4],new int[4],new int[]{2,2,2,2},List.of(),List.of(new PlayedCard(1,c("KS"))),c("AS"));
        var seen=ContractPressure.seen(v);for(Card card:v.hand())seen[ContractPressure.id(card)]=true;
        CapturedRegression.run("pressure-public-promotion",getClass().getName(),"exposedAndPlayedHigherTrumpsPromoteTheHeldQueen",Map.of("rule","Turned AS and played KS are unavailable; QS is now an unbeatable trump. Removing either public fact removes that certificate."),r->{
            r.equal("Turned ace excluded",true,seen[ContractPressure.id(c("AS"))]);r.equal("Played king excluded",true,seen[ContractPressure.id(c("KS"))]);
            r.equal("Queen is promoted",1,ContractPressure.masterTrumps(v,v.hand(),seen));seen[ContractPressure.id(c("AS"))]=false;
            r.equal("Unseen ace prevents promotion",0,ContractPressure.masterTrumps(v,v.hand(),seen));
        });
    }
    @Test public void quotaChangesTheChoiceWhenOneMasterIsReserved()throws Exception {
        var table=List.of(new PlayedCard(1,c("8H")),new PlayedCard(2,c("10D")),new PlayedCard(3,c("JC")));
        var own=hand("AS","9H","7H");int[] rem={3,2,2,2},won=new int[4];
        var v=view(0,own,new int[]{1,0,0,0},won,rem,table,table,null);
        CapturedRegression.run("pressure-reserve-versus-all",getClass().getName(),"quotaChangesTheChoiceWhenOneMasterIsReserved",Map.of("rule","Last seat can win with9H or duck with7H. With one required trick, retain AS and duck. With all three required, win now. Legal-card obligations always precede strategy.","ownHand",own,"table",table),r->{
            r.equal("Need one of three: reserve ace","7H",LossMiningTest.code(ContractPressure.choose(v,own,table,0,new int[]{1,0,0,0},won,rem,ContractPressure.seen(v),ContractPressure.voids(v))));
            r.equal("Need three of three: take current trick","9H",LossMiningTest.code(ContractPressure.choose(v,own,table,0,new int[]{3,0,0,0},won,rem,ContractPressure.seen(v),ContractPressure.voids(v))));
        });
    }
    @Test public void opponentContractChangesVoluntaryButNotForcedThreat()throws Exception {
        var table=List.of(new PlayedCard(2,c("QH")),new PlayedCard(3,c("JH")));
        int[] rem={2,2,1,1},won=new int[4];var v=view(0,hand("KH","8C"),new int[4],won,rem,table,table,null);
        boolean[] unavailable=new boolean[52];Arrays.fill(unavailable,true);for(Card card:hand("AH","7H","7D","8D"))unavailable[ContractPressure.id(card)]=false;
        var voids=new boolean[4][4];
        CapturedRegression.run("pressure-opponent-bid-demand",getClass().getName(),"opponentContractChangesVoluntaryButNotForcedThreat",Map.of("rule","Only the final opponent can act. Its two-card marginal pool is AH,7H,7D,8D. Needing both tricks gives survival3/6 for KH; needing zero ducks with7H where possible, leaving survival4/6. Conditional marginal calculation, not a joint-world win guarantee.","pool",hand("AH","7H","7D","8D")),r->{
            r.equal("Opponent needs every trick",0.5,ContractPressure.survival(v,c("KH"),Suit.HEARTS,0,new int[]{1,2,0,0},won,rem,unavailable,voids,table));
            double actual=ContractPressure.survival(v,c("KH"),Suit.HEARTS,0,new int[]{1,0,0,0},won,rem,unavailable,voids,table);
            r.equal("Four of six combinations survive within1e-12",true,Math.abs(actual-4.0/6)<1e-12);
        });
    }
    @Test public void zeroQuotaCannotOverrideMandatoryTrump()throws Exception {
        var table=List.of(new PlayedCard(1,c("AH")));int[] rem={2,1,2,2};var v=view(2,hand("AS","AC"),new int[4],new int[4],rem,table,table,null);
        CapturedRegression.run("pressure-mandatory-trump",getClass().getName(),"zeroQuotaCannotOverrideMandatoryTrump",Map.of("rule","No hearts and a held trump require trumping even after achieving zero. Strategic ducking cannot legalize AC."),r->{
            r.equal("Trump remains mandatory","AS",LossMiningTest.code(ContractPressure.choose(v,v.hand(),table,2,new int[4],new int[4],rem,ContractPressure.seen(v),ContractPressure.voids(v))));
        });
    }
    @Test public void salvageCannotSpendOwnPointsToBurnAnotherPlayer()throws Exception {
        CapturedRegression.run("pressure-salvage-priority",getClass().getName(),"salvageCannotSpendOwnPointsToBurnAnotherPlayer",Map.of("rule","Only for an already irrecoverable contract: own penalty first, then number of opponents missing, then strongest rival score. Do not deliberately sacrifice a recoverable own contract."),r->{
            r.equal("Equal own loss: prefer three opponents missing",true,ContractPressure.preferSalvage(-1,3,50,-1,1,40));
            r.equal("Do not worsen own penalty",false,ContractPressure.preferSalvage(-2,3,20,-1,1,40));
            r.equal("Same miss count: constrain strongest rival",true,ContractPressure.preferSalvage(-1,2,30,-1,2,40));
        });
    }
    @Test public void forcedPublicMastersCanMakeANumericallyReachableContractImpossible()throws Exception {
        int[] rem={2,2,2,2};var v=view(0,hand("AS","KS"),new int[]{1,0,0,0},new int[4],rem,List.of(),List.of(),null);
        CapturedRegression.run("pressure-forced-win-bound",getClass().getName(),"forcedPublicMastersCanMakeANumericallyReachableContractImpossible",Map.of("rule","Need one from two opportunities is numerically reachable, but held AS+KS forces two wins in every legal continuation. Recognize the unavoidable overshoot before a trick is won. No actual opponent cards are read."),r->{
            r.equal("Numeric quota alone appears reachable",false,ContractPressure.state(0,new int[]{1,0,0,0},new int[4],rem,0).impossible());
            r.equal("Public guaranteed lower bound",2,ContractPressure.guaranteedOwnTricks(v));
            r.equal("Contract one is already physically impossible",true,ContractPressure.irrecoverable(v));
            var reachable=view(0,hand("AS","KS"),new int[]{2,0,0,0},new int[4],rem,List.of(),List.of(),null);
            r.equal("Contract two remains reachable",false,ContractPressure.irrecoverable(reachable));
        });
    }
    @Test public void publicDiscardProvesBothVoidsOnlyUnderMandatoryTrumpRules()throws Exception {
        var history=List.of(new PlayedCard(0,c("AH")),new PlayedCard(1,c("AC")));
        var romanian=view(2,hand("7D","8D"),new int[4],new int[4],new int[]{1,1,2,2},history,history,null);
        var optional=new MonteCarloAi.View(GameMode.OH_HELL,Trump.SPADES,2,3,8,romanian.hand(),romanian.seats(),history,history,null);
        CapturedRegression.run("pressure-void-inference",getClass().getName(),"publicDiscardProvesBothVoidsOnlyUnderMandatoryTrumpRules",Map.of("rule","AC discarded on AH proves no hearts. Romanian mandatory trump additionally proves no spades; optional-trump Oh Hell does not."),r->{
            var a=ContractPressure.voids(romanian);var b=ContractPressure.voids(optional);
            r.equal("Romanian: hearts absent",true,a[1][Suit.HEARTS.ordinal()]);r.equal("Romanian: spades absent",true,a[1][Suit.SPADES.ordinal()]);
            r.equal("Oh Hell: hearts absent",true,b[1][Suit.HEARTS.ordinal()]);r.equal("Oh Hell: spades not inferred absent",false,b[1][Suit.SPADES.ordinal()]);
        });
    }
    @Test public void alreadyActedPlayersCannotThreatenTheCurrentTrickAgain()throws Exception {
        var table=List.of(new PlayedCard(1,c("7H")),new PlayedCard(2,c("8H")),new PlayedCard(3,c("9H")));
        int[] rem={2,1,1,1};var v=view(0,hand("QH","7D"),new int[4],new int[4],rem,table,table,null);
        var unavailable=ContractPressure.seen(v);for(Card card:v.hand())unavailable[ContractPressure.id(card)]=true;
        CapturedRegression.run("pressure-acted-players",getClass().getName(),"alreadyActedPlayersCannotThreatenTheCurrentTrickAgain",Map.of("rule","As last seat, QH beats7H/8H/9H with certainty for this trick; unseen AH andKH in earlier seats cannot be played a second time."),r->{
            r.equal("Current trick survival is exact one",1.0,ContractPressure.survival(v,c("QH"),Suit.HEARTS,0,new int[]{1,1,1,1},new int[4],rem,unavailable,ContractPressure.voids(v),table));
        });
    }
    @Test public void compulsoryRuffRemainsAThreatAfterOpponentReachesItsContract()throws Exception {
        var table=List.of(new PlayedCard(2,c("KH")),new PlayedCard(3,c("QH")));int[] rem={2,2,1,1};
        var v=view(0,hand("AH","8C"),new int[4],new int[4],rem,table,table,null);boolean[] unavailable=new boolean[52];Arrays.fill(unavailable,true);
        for(Card card:hand("7S","8D"))unavailable[ContractPressure.id(card)]=false;var voids=new boolean[4][4];voids[1][Suit.HEARTS.ordinal()]=true;
        CapturedRegression.run("pressure-forced-ruff",getClass().getName(),"compulsoryRuffRemainsAThreatAfterOpponentReachesItsContract",Map.of("rule","Conditional opponent holding contains7S/8D and is known void in hearts. On AH it must trump with7S, even with zero remaining need. Conditional marginal control, not an inferred real hand."),r->{
            r.equal("Cannot duck a mandatory winning trump",0.0,ContractPressure.survival(v,c("AH"),Suit.HEARTS,0,new int[4],new int[4],rem,unavailable,voids,table));
        });
    }
    @Test public void unseenHigherTrumpPreventsAFalseMasterCertificate()throws Exception {
        var v=view(0,hand("KS","7D"),new int[4],new int[4],new int[]{2,2,2,2},List.of(),List.of(),c("QS"));
        CapturedRegression.run("pressure-unseen-higher-trump",getClass().getName(),"unseenHigherTrumpPreventsAFalseMasterCertificate",Map.of("rule","Turned QS does not remove AS. A held KS is not a publicly guaranteed master until AS is own or public."),r->{
            r.equal("King is not certified while ace is unseen",0,ContractPressure.guaranteedOwnTricks(v));
        });
    }
    @Test public void chooseDoesNotMutatePublicMemory() {
        int[] bids={1,0,0,0},won=new int[4],rem={2,2,2,2};var v=view(0,hand("AS","7H"),bids,won,rem,List.of(),List.of(),c("KS"));
        var seen=ContractPressure.seen(v);var original=seen.clone();var voids=ContractPressure.voids(v);
        ContractPressure.choose(v,v.hand(),List.of(),0,bids,won,rem,seen,voids);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bids", bids, "original", original, "rem", rem, "seen", seen, "v", v, "voids", voids, "won", won);
        assertArrayEquals(original,seen);assertArrayEquals(new int[]{2,2,2,2},rem);assertEquals("v.hand() must equal hand(\"7H\", \"AS\").stream().sorted(Comparator.comparing(Card::suit).thenComparingInt(Card::rank)).toList()", hand("7H","AS").stream().sorted(Comparator.comparing(Card::suit).thenComparingInt(Card::rank)).toList(),v.hand());
    }
}

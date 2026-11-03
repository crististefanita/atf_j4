package ro.whistlab.core;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PublicSalvageTest {
    private static Card c(String s){return LossMiningTest.card(s);}
    private static List<Card> hand(String... cards){return Arrays.stream(cards).map(PublicSalvageTest::c).toList();}
    static MonteCarloAi.View fixture(int ownBid) {
        var history=new ArrayList<PlayedCard>();
        int[][] actors={{3,0,1,2},{3,0,1,2},{3,0,1,2},{0,1,2,3},{0,1,2,3},{0,1,2,3}};
        String[][] plays={{"AD","7D","8D","9D"},{"KD","10D","JD","QD"},{"7C","JC","JS","JH"},
                {"KS","QS","9H","9C"},{"KH","9S","QH","10C"},{"KC","10S","10H","QC"}};
        for(int t=0;t<6;t++)for(int i=0;i<4;i++)history.add(new PlayedCard(actors[t][i],c(plays[t][i])));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.NO_TRUMP,0,2,8,hand("7S","7H"),
                List.of(new MonteCarloAi.Seat(2,ownBid,4,20),new MonteCarloAi.Seat(2,2,0,10),
                        new MonteCarloAi.Seat(2,1,0,10),new MonteCarloAi.Seat(2,0,2,100)),List.of(),history,null);
    }
    private static ro.whistlab.core.baseline.RoundElevenMonteCarloAi.View frozen(MonteCarloAi.View v) {
        return new ro.whistlab.core.baseline.RoundElevenMonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats().stream().map(s->new ro.whistlab.core.baseline.RoundElevenMonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),v.trick(),v.history(),v.turnedCard(),v.streakBonuses(),v.streakBonusPoints());
    }
    @Test public void certifiedLeadBurnsThreeRivalsInsteadOfTwoWithoutWorseningOwnPenalty()throws Exception {
        var v=fixture(0);var proof=PublicSalvage.choose(v,c("7S"));
        CapturedRegression.run("salvage-certified-three-rivals",getClass().getName(),"certifiedLeadBurnsThreeRivalsInsteadOfTwoWithoutWorseningOwnPenalty",Map.of(
                "rule","The public history uniquely leaves seat1 with AS/8S, seat2 with AH/8H and seat3 with AC/8C. Hero already missed0 with4 won. Lead7S gives seat1 both tricks and its exact2; lead7H gives seat2 both, overshooting its1. Both hero penalties stay-4; rival misses rise2to3 in every legal continuation.",
                "before",v,"publicCertificate",proof),r->{
            r.equal("Exactly one complete feasible world",1,proof.worlds());r.equal("Full proof is certified",true,proof.certified());
            r.equal("Baseline rival miss ceiling",2,proof.baselineRivalMissCeiling());r.equal("New rival miss floor",3,proof.rivalMissFloor());r.equal("Own penalty floor is unchanged",-4,proof.ownFloor());
            r.equal("Frozen accepted engine leads spade","7S",LossMiningTest.code(ro.whistlab.core.baseline.RoundElevenMonteCarloAi.chooseCard(frozen(v)).card()));
            r.equal("New Hard engine leads heart","7H",LossMiningTest.code(MonteCarloAi.chooseCard(v).card()));
            var oracle=PublicEndgameOracle.audit(v);r.equal("Independent complete oracle confirms own four wins for both choices",Map.of("7S",Set.of(4),"7H",Set.of(4)),oracle.finalWon());
        });
    }
    @Test public void independentlyTracedFinalTricksReproduceBothRivalMissCounts()throws Exception {
        var v=fixture(0);var cases=List.of(List.of(List.of("7S","8S","8H","8C"),List.of("AS","AH","AC","7H")),
                List.of(List.of("7H","8S","8H","8C"),List.of("AH","AC","7S","AS")));
        CapturedRegression.run("salvage-independent-two-trick-trace",getClass().getName(),"independentlyTracedFinalTricksReproduceBothRivalMissCounts",Map.of("rule","A separate literal two-trick trace checks legal card ownership, follow suit, winner and final opponent contracts. The production proof also checks every other legal response.","traces",cases),r->{
            for(int which=0;which<2;which++){
                var hands=new ArrayList<List<Card>>();for(var h:List.of(hand("7S","7H"),hand("AS","8S"),hand("AH","8H"),hand("AC","8C")))hands.add(new ArrayList<>(h));
                int actor=0;int[] won={4,0,0,2};
                for(var row:cases.get(which)){
                    Suit led=c(row.get(0)).suit();int winner=actor,best=-1;
                    for(String code:row){Card card=c(code);r.equal("Independent legal response",true,RolloutBoundsOracleTest.legal(hands.get(actor),led,Trump.NO_TRUMP).contains(card));r.equal("Played card was held",true,hands.get(actor).remove(card));int power=RolloutBoundsOracleTest.power(card,led,Trump.NO_TRUMP);if(power>best){best=power;winner=actor;}actor=(actor+1)%4;}
                    won[winner]++;actor=winner;
                }
                r.equal("Final won counts for trace"+which,which==0?List.of(4,2,0,2):List.of(4,0,2,2),Arrays.stream(won).boxed().toList());int misses=0;for(int s=1;s<4;s++)if(won[s]!=v.seats().get(s).bid())misses++;
                r.equal("Rival misses for trace"+which,which==0?2:3,misses);
            }
        });
    }
    @Test public void recoverableContractCannotBeSacrificedForRivalDamage()throws Exception {
        var v=fixture(4);var proof=PublicSalvage.choose(v,c("7S"));
        CapturedRegression.run("salvage-recoverable-no-override",getClass().getName(),"recoverableContractCannotBeSacrificedForRivalDamage",Map.of("rule","At bid4 and won4 the contract remains attainable; salvage must not override, even if another line could hurt rivals."),r->{r.equal("Contract remains possible",false,PublicSalvage.quota(v).impossible());r.equal("No salvage certificate",false,proof.certified());r.equal("Original proposal preserved","7S",LossMiningTest.code(proof.card()));});
    }
    @Test public void aPartialSearchCannotClaimDominance()throws Exception {
        var proof=PublicSalvage.choose(fixture(0),c("7S"),1);
        CapturedRegression.run("salvage-budget-fallback",getClass().getName(),"aPartialSearchCannotClaimDominance",Map.of("rule","A one-node budget cannot compare both full trees. Keep the original proposal and never publish a partial search as certain."),r->{r.equal("Incomplete search not certified",false,proof.certified());r.equal("Original card retained","7S",LossMiningTest.code(proof.card()));});
    }
    @Test public void equalExpectedPenaltyIsWeakerThanPointwiseDominance()throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var v=json.treeToValue(json.readTree(getClass().getResourceAsStream("/regressions/contract-certificate/cases.json")).path("impossible").path("input").path("before"),MonteCarloAi.View.class);
        var proof=PublicSalvage.choose(v,c("AS"));
        CapturedRegression.run("salvage-no-expected-value-certificate",getClass().getName(),"equalExpectedPenaltyIsWeakerThanPointwiseDominance",Map.of("rule","AS and AD both average-2.6 under a uniform prior but allow final own won1or2. Overlapping payoff intervals cannot certify pointwise no-worse points; retain baseline."),r->{r.equal("No universal dominance from equal means",false,proof.certified());r.equal("Baseline retained","AS",LossMiningTest.code(proof.card()));});
    }
    @Test public void turnedAceAndHeldTopTrumpsGiveAPublicLowerBound()throws Exception {
        var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,3,2,hand("KS","QS"),List.of(new MonteCarloAi.Seat(2,1,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0)),List.of(),List.of(),c("AS"));
        CapturedRegression.run("salvage-turned-master-quota",getClass().getName(),"turnedAceAndHeldTopTrumpsGiveAPublicLowerBound",Map.of("rule","AS is turned and cannot be held by an opponent. Held KS/QS force two tricks, so bid1 is already impossible even though two numeric opportunities remain.","before",v),r->{var q=PublicSalvage.quota(v);r.equal("Need one",1,q.needed());r.equal("Two opportunities",2,q.opportunities());r.equal("Two publicly certain future wins",2,q.forcedMasterTrumps());r.equal("Physical overshoot already unavoidable",true,q.impossible());});
    }
}

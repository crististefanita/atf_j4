package ro.whistlab.core.ai.engine;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.information.RolloutBoundsOracleTest;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;

/** Reachable public prefixes and universal outcomes, not fixed honor-to-trick values. */
public class ContextualHonorDiscardTest extends atf.gradle.audit.GradleAuditedTest {
    private static Card card(String text){return LossMiningTest.card(text);}
    private static List<Card> hand(String... cards){return Arrays.stream(cards).map(ContextualHonorDiscardTest::card).toList();}
    private static List<PlayedCard> row(int leader,String... cards){
        var plays=new ArrayList<PlayedCard>();
        for(int i=0;i<cards.length;i++)plays.add(new PlayedCard((leader+i)%4,card(cards[i])));
        return plays;
    }
    private static List<List<PlayedCard>> past(String honor,boolean mandatoryTrump){
        var rows=new ArrayList<List<PlayedCard>>();
        rows.add(row(0,"7S","8S","9S",honor.equals("KS")?"AS":"10S"));
        if(!mandatoryTrump)rows.add(row(3,"AD","7D","8D","9D"));
        rows.add(row(3,"KD","10D","JD","QD"));
        rows.add(row(3,"7C","8C","9C","10C"));
        rows.add(row(2,"AH","KH","8H","9H"));
        rows.add(row(2,"JH","JC","10H","QH"));
        return rows;
    }
    private static MonteCarloAi.View view(String honor,int bid,boolean mandatoryTrump){
        var table=row(1,"AC","KC","QC");
        var history=new ArrayList<PlayedCard>();past(honor,mandatoryTrump).forEach(history::addAll);history.addAll(table);
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,mandatoryTrump?Trump.DIAMONDS:Trump.NO_TRUMP,0,3,
                mandatoryTrump?7:8,List.of(card(honor),card(mandatoryTrump?"7D":"7H")),
                List.of(new MonteCarloAi.Seat(2,bid,0),new MonteCarloAi.Seat(1,1,1),
                        new MonteCarloAi.Seat(1,2,2),new MonteCarloAi.Seat(1,mandatoryTrump?2:3,mandatoryTrump?2:3)),
                table,history,mandatoryTrump?card("8D"):null);
    }

    /** A legal witness proves reachability; expectations below still cover EVERY feasible hidden assignment. */
    private static void auditPrefix(MonteCarloAi.View v,String honor,CapturedRegression capture,Map<String,Object> context){
        var hands=new ArrayList<List<Card>>();for(int s=0;s<4;s++)hands.add(new ArrayList<>());
        hands.get(0).addAll(v.hand());hands.get(1).add(card(honor.equals("KS")?"10S":"KS"));
        hands.get(2).add(card("QS"));hands.get(3).add(card("JS"));
        for(var p:v.history())hands.get(p.seat()).add(p.card());
        auditWitness(v,hands,true,capture,context,"reachability");
    }

    /** Independent list replay: the witness qualifies the fixture, never supplies either planner's hidden deal. */
    private static void auditWitness(MonteCarloAi.View v,List<List<Card>> initial,boolean requireObserverTurn,
            CapturedRegression r,Map<String,Object> context,String key){
        var proof=new LinkedHashMap<String,Object>();context.put(key,proof);
        proof.put("initialHands",initial.stream().map(ContextualHonorDiscardTest::codes).toList());
        proof.put("dealer",v.dealer());proof.put("firstActor",(v.dealer()+1)%4);
        proof.put("turnedCard",v.turnedCard()==null?null:LossMiningTest.code(v.turnedCard()));
        proof.put("publicHistory",v.history());proof.put("currentTable",v.trick());
        proof.put("observerMustBeCurrentActor",requireObserverTurn);
        proof.put("scope","A full legal reachability witness audited independently. Neither decision API receives these opponent holdings. Quota-only observations can be off-turn; decision fixtures must match the current actor.");
        String label=key+" / ";var hands=new ArrayList<List<Card>>();for(var h:initial)hands.add(new ArrayList<>(h));
        var all=initial.stream().flatMap(List::stream).toList();
        r.equal(label+"four initial hands",4,initial.size());
        r.equal(label+"dealt card total",4*v.handSize(),all.size());
        r.equal(label+"distinct dealt cards",all.size(),new HashSet<>(all).size());
        r.equal(label+"reduced deck ranks7..A",true,all.stream().allMatch(c->c.rank()>=7&&c.rank()<=14));
        for(int s=0;s<4;s++)r.equal(label+"initial hand size seat"+s,v.handSize(),initial.get(s).size());
        if(v.handSize()==8){r.equal(label+"eight-card no-trump rule",Trump.NO_TRUMP,v.trump());r.equal(label+"no turned card when all32 are dealt",null,v.turnedCard());}
        else{
            r.equal(label+"turned card is present",true,v.turnedCard()!=null);
            r.equal(label+"turned card belongs to reduced deck",true,v.turnedCard().rank()>=7&&v.turnedCard().rank()<=14);
            r.equal(label+"turned card matches declared trump",v.trump().ordinal(),v.turnedCard().suit().ordinal());
            r.equal(label+"turned card was not dealt",false,all.contains(v.turnedCard()));
        }
        proof.put("undealtCardsExcludingTurned",32-all.size()-(v.turnedCard()==null?0:1));
        r.equal(label+"legal full auction range",true,v.seats().stream().allMatch(s->s.bid()!=null&&s.bid()>=0&&s.bid()<=v.handSize()));
        r.equal(label+"last bid keeps total different from hand size",false,v.seats().stream().mapToInt(MonteCarloAi.Seat::bid).sum()==v.handSize());
        int next=(v.dealer()+1)%4;int[] won=new int[4];var table=new ArrayList<PlayedCard>();var winners=new ArrayList<Integer>();
        for(int i=0;i<v.history().size();i++){
            var p=v.history().get(i);Suit led=table.isEmpty()?null:table.get(0).card().suit();
            r.equal(label+"move"+(i+1)+" actor",next,p.seat());
            r.equal(label+"move"+(i+1)+" card ownership",true,hands.get(next).contains(p.card()));
            r.equal(label+"move"+(i+1)+" follow suit or mandatory trump",true,RolloutBoundsOracleTest.legal(hands.get(next),led,v.trump()).contains(p.card()));
            hands.get(next).remove(p.card());table.add(p);
            if(table.size()==4){
                PlayedCard best=table.get(0);Suit suit=best.card().suit();
                for(var candidate:table)if(RolloutBoundsOracleTest.power(candidate.card(),suit,v.trump())>RolloutBoundsOracleTest.power(best.card(),suit,v.trump()))best=candidate;
                next=best.seat();won[next]++;winners.add(next);table.clear();
            }else next=(next+1)%4;
        }
        proof.put("independentlyComputedTrickWinners",winners);proof.put("computedNextActor",next);
        proof.put("remainingWitnessHands",hands.stream().map(ContextualHonorDiscardTest::codes).toList());
        r.equal(label+"public table is the unfinished history suffix",v.trick(),table);
        if(requireObserverTurn)r.equal(label+"observer is current actor",v.seat(),next);
        for(int s=0;s<4;s++){
            r.equal(label+"remaining cards seat"+s,v.seats().get(s).remaining(),hands.get(s).size());
            r.equal(label+"won counter seat"+s,v.seats().get(s).won(),won[s]);
        }
        r.equal(label+"observer remaining card identity",codes(v.hand()),codes(hands.get(v.seat())));
        r.equal(label+"completed trick conservation",v.history().size()/4,Arrays.stream(won).sum());
        proof.put("publicPrefixLegal",true);
    }
    private static List<String> codes(List<Card> cards){return cards.stream().map(LossMiningTest::code).sorted().toList();}

    private static void checkHonor(String honor,int need)throws Exception{
        var v=view(honor,need,false);String expected=need==0?honor:"7H";
        var context=new LinkedHashMap<String,Object>();
        context.put("rule","Same side honor and same public cards: with need0 discard the honor; with need1 discard7H and retain the honor. Current clubs are already won by seat1. Every unseen remaining card is a spade, so the last trick is forced after the current leader wins.");
        context.put("before",v);context.put("remainingNeed",need);context.put("remainingOpportunities",2);
        context.put("tableRemainingDemandMinusOpportunities",need-2);
        context.put("scope","Reachable8-card no-trump Romanian hand, six complete tricks plus three public plays. No optimal-bidding label or unknown future discard opportunity is assumed.");
        CapturedRegression.run("context-discard-"+honor.toLowerCase(Locale.ROOT)+"-need"+need,
                ContextualHonorDiscardTest.class.getName(),need==0?"sideHonorsCanBeDiscardedAfterTheContractIsMade":"theSameHonorMustBeRetainedWhenOneTrickIsStillNeeded",context,r->{
            auditPrefix(v,honor,r,context);
            var oracle=PublicEndgameOracle.audit(v);
            context.put("universalOutcome",Map.of("worlds",oracle.worlds(),"finalWon",oracle.finalWon(),"safe",oracle.safe()));
            r.equal("All feasible remaining assignments",6,oracle.worlds());
            r.equal("Discarding the honor always leaves zero tricks",Set.of(0),oracle.finalWon().get(honor));
            r.equal("Retaining the honor always forces one trick",Set.of(1),oracle.finalWon().get("7H"));
            r.equal("Unique exact-contract choice",List.of(expected),oracle.safe());
            r.equal("Hard uses the current quota",expected,LossMiningTest.code(MonteCarloAi.chooseCard(v).card()));
            r.equal("Independent uses the same public quota",expected,PublicEndgameOracle.code(IndependentPlayer.card(PlayerObservations.independent(v)).value()));
        });
    }
    @Test public void sideHonorsCanBeDiscardedAfterTheContractIsMade()throws Exception{for(String honor:List.of("AS","KS"))checkHonor(honor,0);}
    @Test public void theSameHonorMustBeRetainedWhenOneTrickIsStillNeeded()throws Exception{for(String honor:List.of("AS","KS"))checkHonor(honor,1);}
    @Test public void mandatoryTrumpPreventsTheSideHonorDiscardEvenAtZero()throws Exception{
        var v=view("AS",0,true);var context=new LinkedHashMap<String,Object>();context.put("before",v);
        context.put("rule","In a reachable7-card Romanian trump hand, holding7D makes7D the only legal response to clubs. The targetzero cannot authorize discardingAS. The public turned8D and three undealt cards are separate from the remaining opponent hands.");
        CapturedRegression.run("context-discard-mandatory-trump",getClass().getName(),"mandatoryTrumpPreventsTheSideHonorDiscardEvenAtZero",context,r->{
            auditPrefix(v,"AS",r,context);
            r.equal("Independent legal set",List.of(card("7D")),RolloutBoundsOracleTest.legal(v.hand(),Suit.CLUBS,v.trump()));
            r.equal("Engine legal set",List.of(card("7D")),GameRules.legalCards(v.hand(),Suit.CLUBS,v.trump(),v.mode()));
            r.equal("Hard obeys mandatory trump","7D",LossMiningTest.code(MonteCarloAi.chooseCard(v).card()));
            r.equal("Independent obeys mandatory trump","7D",PublicEndgameOracle.code(IndependentPlayer.card(PlayerObservations.independent(v)).value()));
        });
    }
    @Test public void aHigherTrumpStillOnTheTableCanConsumeOneHeldMaster()throws Exception{
        for(String lead:List.of("AS","KS")){
            var own=lead.equals("AS")?List.of(card("KS"),card("QS")):List.of(card("AS"),card("QS"));
            var table=List.of(new PlayedCard(0,card(lead)));
            var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,1,3,2,own,
                    List.of(new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(2,1,0),
                            new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0)),table,table,card("7S"));
            var context=new LinkedHashMap<String,Object>();context.put("before",v);
            context.put("rule","An exposed higher trump is still active in the unfinished trick. QS can lose under the shown AS/KS; the retained highest trump then forces the last trick. Own contract1 is reachable, so two retained future wins must not be claimed.");
            context.put("scope","Public rank and legality counterexample; the witness qualifies reachability without supplying either policy's hidden hands.");
            CapturedRegression.run("context-current-trump-"+lead.toLowerCase(Locale.ROOT),getClass().getName(),"aHigherTrumpStillOnTheTableCanConsumeOneHeldMaster",context,r->{
                auditWitness(v,List.of(hand(lead,"7H"),own,hand("8H","9H"),hand("10H","JH")),true,r,context,"reachability");
                var q=PublicSalvage.quota(v);
                r.equal("Two opportunities include the unfinished trick once",2,q.opportunities());
                r.equal("Only one retained trump win is unavoidable",1,q.forcedMasterTrumps());
                r.equal("One is still needed",1,q.needed());
                r.equal("Do not falsely classify a recoverable contract",false,q.impossible());
                r.equal("The queen can legally follow underneath the exposed higher trump",true,
                        RolloutBoundsOracleTest.legal(v.hand(),Suit.SPADES,v.trump()).contains(card("QS")));
                r.equal("Salvage does not start on this recoverable contract",0,PublicSalvage.choose(v,card("QS")).nodes());
            });
        }
    }
    @Test public void completedOrTurnedAcesDoNotConsumeARetainedMaster()throws Exception{
        var own=List.of(card("KS"),card("QS"));
        var seats=List.of(new MonteCarloAi.Seat(2,1,1),new MonteCarloAi.Seat(2,1,0),
                new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0));
        var completed=row(0,"AS","7S","8S","9S");
        var after=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,1,3,3,own,seats,List.of(),completed,card("10S"));
        var turned=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,1,3,2,own,
                List.of(new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,1,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0)),List.of(),List.of(),card("AS"));
        var lower=row(0,"JS");
        var lowerView=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,1,3,2,own,
                List.of(new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(2,1,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0)),lower,lower,card("AS"));
        var hearts=row(0,"AH","AS");
        var mustFollow=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,2,3,2,List.of(card("KS"),card("7H")),
                List.of(new MonteCarloAi.Seat(1,1,0),new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0)),hearts,hearts,card("QS"));
        var context=new LinkedHashMap<String,Object>();
        context.put("rule","A turned or previously completed AS cannot win the current trick. A current JS is below both held KS/QS. Do not discount every incomplete trick. Following7H under AH/AS preserves the heldKS because playingKS now is illegal.");
        context.put("afterCompletedAce",after);context.put("turnedAce",turned);context.put("currentLowerTrump",lowerView);context.put("mustFollowHeart",mustFollow);
        CapturedRegression.run("context-inactive-and-lower-trumps",getClass().getName(),"completedOrTurnedAcesDoNotConsumeARetainedMaster",context,r->{
            auditWitness(after,List.of(hand("AS","AH","KH"),hand("7S","KS","QS"),hand("8S","7H","8H"),hand("9S","9H","10H")),false,r,context,"completedAceReachability");
            auditWitness(turned,List.of(hand("AH","KH"),own,hand("7H","8H"),hand("9H","10H")),false,r,context,"turnedAceReachability");
            auditWitness(lowerView,List.of(hand("JS","7H"),own,hand("8H","9H"),hand("10H","JH")),true,r,context,"lowerTrumpReachability");
            auditWitness(mustFollow,List.of(hand("AH","7C"),hand("AS","8C"),hand("KS","7H"),hand("9C","10C")),true,r,context,"mustFollowReachability");
            for(var v:List.of(after,turned,lowerView)){
                r.equal("Two retained wins remain forced",2,PublicSalvage.quota(v).forcedMasterTrumps());
                r.equal("A one-trick contract is already unreachable by overshoot",true,PublicSalvage.quota(v).impossible());
            }
            r.equal("Following heart prevents spending the king",List.of(card("7H")),RolloutBoundsOracleTest.legal(mustFollow.hand(),Suit.HEARTS,mustFollow.trump()));
            r.equal("Illegal trump discard cannot reduce the lower bound",1,PublicSalvage.quota(mustFollow).forcedMasterTrumps());
        });
    }
    @Test public void anUnderbookedTableDoesNotOverrideNeedingEveryRemainingTrick()throws Exception{
        var history=new ArrayList<PlayedCard>();history.addAll(row(1,"9H","AH","10H","7H"));history.addAll(row(2,"KH","JH","8H","QH"));
        var table=row(2,"JS","10S");history.addAll(table);
        var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,0,4,List.of(card("AS"),card("KS")),
                List.of(new MonteCarloAi.Seat(2,2,0),new MonteCarloAi.Seat(2,1,0),new MonteCarloAi.Seat(1,0,2),new MonteCarloAi.Seat(1,0,0)),table,history,card("7S"));
        var context=new LinkedHashMap<String,Object>();context.put("before",v);
        context.put("rule","Bids[2,1,0,0] underbook four tricks. Seat2 already won two: needs[2,1,-2,0], two opportunities, positive demand3. Hero must take both remaining tricks with AS/KS; the negative table imbalance is not a duck instruction.");
        context.put("scope","The complete initial witness and chronological independent comparisons establish reachability. Neither decision API receives opponent holdings; AS/KS force both wins for every feasible world.");
        CapturedRegression.run("context-underbooked-but-need-all",getClass().getName(),"anUnderbookedTableDoesNotOverrideNeedingEveryRemainingTrick",context,r->{
            auditWitness(v,List.of(hand("7H","8H","AS","KS"),hand("9H","QH","QS","7C"),hand("AH","KH","JS","8C"),hand("10H","JH","10S","9C")),true,r,context,"reachability");
            int signed=v.seats().stream().mapToInt(s->s.bid()-s.won()).sum();
            int opportunities=PublicSalvage.quota(v).opportunities();
            r.equal("Original auction difference",-1,v.seats().stream().mapToInt(s->s.bid()).sum()-v.handSize());
            r.equal("Signed imbalance is conserved during the hand",-1,signed-opportunities);
            r.equal("Positive remaining demand exceeds available tricks",3,v.seats().stream().mapToInt(s->Math.max(0,s.bid()-s.won())).sum());
            r.equal("Own need equals both opportunities",2,PublicSalvage.quota(v).needed());
            r.equal("Both retained masters are inevitable wins",2,PublicSalvage.quota(v).forcedMasterTrumps());
            r.equal("Own exact contract is recoverable",false,PublicSalvage.quota(v).impossible());
            var decision=MonteCarloAi.chooseCard(v);
            r.equal("Both physical choices are evaluated",2,decision.candidates().size());
            for(var choice:decision.candidates())r.equal("Each top trump preserves exactly two tricks and seven points",7.0,choice.meanContractPoints());
        });
    }
}

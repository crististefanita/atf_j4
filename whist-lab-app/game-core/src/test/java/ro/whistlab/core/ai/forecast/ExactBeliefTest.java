package ro.whistlab.core.ai.forecast;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class ExactBeliefTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void oneCardProbabilityMatchesAll24360OrderedOpposingDeals(){
        for(int[] example:new int[][]{{0,7,0},{0,7,2},{1,14,0},{1,14,2},{1,7,2},{0,14,2}}){
            int hero=example[2];Card own=new Card(Suit.values()[example[0]],example[1]);var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(1,null,0));
            var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,hero,3,1,List.of(own),seats,List.of(),List.of(),new Card(Suit.SPADES,8));
            var pool=MonteCarloAi.unknownCards(view);long won=0,total=0;int ownId=id(own);
            for(int a=0;a<pool.size();a++)for(int b=0;b<pool.size();b++)if(b!=a)for(int c=0;c<pool.size();c++)if(c!=a&&c!=b){
                int[] other={id(pool.get(a)),id(pool.get(b)),id(pool.get(c))},cards=new int[4];int at=0;for(int s=0;s<4;s++)cards[s]=s==hero?ownId:other[at++];
                int best=0,led=cards[0]/13;for(int s=1;s<4;s++)if(power(cards[s],led)>power(cards[best],led))best=s;if(best==hero)won++;total++;
            }
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "example", example, "hero", hero, "own", own, "ownId", ownId, "pool", pool, "seats", seats, "total", total, "view", view, "won", won);
assertEquals("total must equal 24360", 24360,total);assertEquals(won/(double)total,MonteCarloAi.oneCardWinProbability(view),1e-12);
        }
    }
    @Test public void exactOneCardUtilityStillHonorsTheForbiddenLastBid(){
        var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,3,3,1,List.of(new Card(Suit.SPADES,14)),List.of(new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(1,null,0)),List.of(),List.of(),new Card(Suit.SPADES,8));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "v", v);
        assertEquals(1,MonteCarloAi.oneCardWinProbability(v),0);assertEquals("MonteCarloAi.chooseBid(v) must equal 0", 0,MonteCarloAi.chooseBid(v));
    }
    @Test public void lastTwoCardsEnumerateAllNinetyDistinctConsistentDeals(){
        var history=new ArrayList<PlayedCard>();for(int suit=0;suit<3;suit++)for(int high:new int[]{10,14}){
            history.add(new PlayedCard(0,new Card(Suit.values()[suit],high)));for(int seat=1;seat<4;seat++)history.add(new PlayedCard(seat,new Card(Suit.values()[suit],high-4+seat)));
        }
        var seats=List.of(new MonteCarloAi.Seat(2,6,6),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0));
        var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.NO_TRUMP,0,3,8,List.of(new Card(Suit.DIAMONDS,7),new Card(Suit.DIAMONDS,8)),seats,List.of(),history);
        var worlds=MonteCarloAi.enumerateSmallDeals(v);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "seats", seats, "v", v, "worlds", worlds);
assertEquals("worlds.size() must equal 90", 90,worlds.size());assertEquals("new HashSet<>(worlds).size() must equal 90", 90,new HashSet<>(worlds).size());
        for(var world:worlds){Set<Card> seen=new HashSet<>();for(var hand:world){assertEquals("hand.size() must equal 2", 2,hand.size());for(Card c:hand){assertEquals("c.suit() must equal Suit.DIAMONDS", Suit.DIAMONDS,c.suit());assertTrue("seen.add(c) must hold", seen.add(c));}}assertEquals("seen.size() must equal 8", 8,seen.size());}
        var decision=MonteCarloAi.chooseCard(v);assertTrue("decision.candidates().stream().allMatch((c)->c.samples() == 90) must hold", decision.candidates().stream().allMatch(c->c.samples()==90));assertTrue("decision.method().contains(\"exhaustively enumerated\") must hold", decision.method().contains("exhaustively enumerated"));
    }
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static int power(int c,int led){return (c/13==0?200:c/13==led?100:0)+c%13;}
}

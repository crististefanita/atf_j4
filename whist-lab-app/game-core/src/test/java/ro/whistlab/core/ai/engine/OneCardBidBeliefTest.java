package ro.whistlab.core.ai.engine;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;

public class OneCardBidBeliefTest {
    @Test public void noPriorPublicBidsPreserveUniformProbability(){
        for(int seat=0;seat<4;seat++)for(int suit=0;suit<4;suit++)for(int rank:List.of(7,10,13,14)){
            Card own=new Card(Suit.values()[suit],rank);if(own.equals(new Card(Suit.SPADES,8)))continue;
            var v=view(seat,own,null,null);assertEquals(MonteCarloAi.oneCardWinProbability(v),OneCardBidBelief.probability(v),1e-12);
        }
    }
    @Test public void twoIndependentImplementationsAgreeOnConditionedOrderedDeals()throws Exception{
        var method=Class.forName("ro.whistlab.coach.OneCardBidBelief").getDeclaredMethod("probability",IndependentPlayer.Position.class);method.setAccessible(true);
        for(int seat=1;seat<4;seat++)for(int rank:List.of(7,11,13,14))for(int bid:List.of(0,1)){
            var v=view(seat,new Card(Suit.HEARTS,rank),bid,seat==3?0:null);var p=position(v);
            double actual=OneCardBidBelief.probability(v);assertTrue(actual>=0&&actual<=1);assertEquals((double)method.invoke(null,p),actual,1e-11);
        }
    }
    @Test public void strongEarlierBidLowersTheChanceOfAHighNonTrumpFollower(){
        var zero=view(1,new Card(Suit.HEARTS,14),0,null);var one=view(1,new Card(Suit.HEARTS,14),1,null);
        assertTrue(OneCardBidBelief.probability(zero)>OneCardBidBelief.probability(one));
    }
    @Test public void forcedLastBidIsNotMistakenForHandStrength(){
        var v=view(0,new Card(Suit.HEARTS,14),null,null);var seats=new ArrayList<>(v.seats());seats.set(3,new MonteCarloAi.Seat(1,0,0));
        var forced=new MonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),1,v.hand(),seats,List.of(),List.of(),v.turnedCard());
        assertEquals(MonteCarloAi.oneCardWinProbability(v),OneCardBidBelief.probability(forced),1e-12);
    }
    @Test public void forbiddenLastBidStillWinsOverAWinningAceAndTheModelIsVersioned(){
        var v=view(3,new Card(Suit.SPADES,14),0,0);var seats=new ArrayList<>(v.seats());seats.set(2,new MonteCarloAi.Seat(1,0,0));
        var last=new MonteCarloAi.View(v.mode(),v.trump(),3,3,1,v.hand(),seats,List.of(),List.of(),v.turnedCard());
        assertEquals(0,MonteCarloAi.chooseBid(last));assertEquals(0,IndependentPlayer.bid(position(last)).value());assertEquals(64,OneCardBidBelief.TRAINING_DIGEST.length());
    }
    private static MonteCarloAi.View view(int seat,Card own,Integer leadBid,Integer nextBid){var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(1,s==0?leadBid:s==1?nextBid:null,0));seats.set(seat,new MonteCarloAi.Seat(1,null,0));return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,seat,3,1,List.of(own),seats,List.of(),List.of(),new Card(Suit.SPADES,8));}
    private static IndependentPlayer.Position position(MonteCarloAi.View v){return new IndependentPlayer.Position(v.seat(),v.dealer(),1,v.trump().ordinal(),v.hand().stream().map(c->c.suit().ordinal()*13+c.rank()-2).toList(),v.seats().stream().map(s->new IndependentPlayer.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),List.of(),List.of(),6,false,5);}
}

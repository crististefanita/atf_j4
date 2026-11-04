package ro.whistlab.core.research.bidding;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import java.util.*;
/** Soft likelihood of ALREADY OBSERVED opponent bids. Development-only fitted strength prior; never substitutes a future bid or excludes a feasible deal. */
final class BidEvidence {
    static final String TRAINING_DIGEST="7449c56af437361c78323333adfccb18c56c72443efe6c28e2db94b520dee484";
    private static final double[] COEFFICIENTS={-1.1626522830945545,0.32076073451405246,1.6440294714512247,6.1934081467467434,4.5559029030823668,3.1114380968168511,1.5480000672648795,0.65723341932640167,0.28254540793945859,-4.6309694340086969,0.93940998947564402,1.5942978045527603,0.54940020176091886,-0.51305034257791493};
    private BidEvidence(){}
    static int predict(List<Card> hand,Trump trump,int position,int prior){
        int[] suits=new int[4],high=new int[4];int trumps=0;double trumpPower=0,rankPower=0;for(Card c:hand){suits[c.suit().ordinal()]++;rankPower+=(c.rank()-7)/7.0;if(c.suit().ordinal()==trump.ordinal()){trumps++;trumpPower+=(c.rank()-7)/7.0;}else if(c.rank()>=11)high[14-c.rank()]++;}
        double squares=0,voids=0;for(int count:suits){squares+=count*count;if(count==0)voids++;}double[] x={1,hand.size()/8.0,trump==Trump.NO_TRUMP?1:0,trumps/8.0,trumpPower/8,high[0]/4.0,high[1]/4.0,high[2]/4.0,high[3]/4.0,squares/64,voids/4,rankPower/8,position/3.0,prior/8.0};double value=0;for(int i=0;i<x.length;i++)value+=x[i]*COEFFICIENTS[i];return Math.max(0,Math.min(hand.size(),(int)Math.round(value)));
    }
    static double weight(MonteCarloAi.View view,List<List<Card>> world){
        if(!view.mode().isRomanian()||view.seats().size()!=4||view.handSize()<2||view.handSize()>8)return 1;
        double weight=1;int prior=0;
        for(int i=1;i<=4;i++){int seat=(view.dealer()+i)%4;Integer bid=view.seats().get(seat).bid();
            if(seat!=view.seat()&&bid!=null){var original=new LinkedHashSet<>(world.get(seat));for(var play:view.history())if(play.seat()==seat)original.add(play.card());for(var play:view.trick())if(play.seat()==seat)original.add(play.card());
                if(original.size()==view.handSize()){int error=bid-predict(new ArrayList<>(original),view.trump(),i-1,prior);weight*=.25+.75/(1.0+error*error);}}
            if(bid!=null)prior+=bid;
        }return weight;
    }

}

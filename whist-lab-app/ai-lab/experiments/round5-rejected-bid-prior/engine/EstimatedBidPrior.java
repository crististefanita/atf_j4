package ro.whistlab.core;
import java.util.*;
/** Expected opponent bid, learned only from prior development games. Used in hypothetical rollout worlds. */
final class EstimatedBidPrior {
    static final String TRAINING_DIGEST="7449c56af437361c78323333adfccb18c56c72443efe6c28e2db94b520dee484";
    private static final double[] COEFFICIENTS={-1.1626522830945545,0.32076073451405246,1.6440294714512247,6.1934081467467434,4.5559029030823668,3.1114380968168511,1.5480000672648795,0.65723341932640167,0.28254540793945859,-4.6309694340086969,0.93940998947564402,1.5942978045527603,0.54940020176091886,-0.51305034257791493};
    private EstimatedBidPrior(){}
    static int predict(List<Card> hand,Trump trump,int position,int prior){
        int[] suits=new int[4],high=new int[4];int trumps=0;double trumpPower=0,rankPower=0;for(Card c:hand){suits[c.suit().ordinal()]++;rankPower+=(c.rank()-7)/7.0;if(c.suit().ordinal()==trump.ordinal()){trumps++;trumpPower+=(c.rank()-7)/7.0;}else if(c.rank()>=11)high[14-c.rank()]++;}
        double squares=0,voids=0;for(int count:suits){squares+=count*count;if(count==0)voids++;}double[] x={1,hand.size()/8.0,trump==Trump.NO_TRUMP?1:0,trumps/8.0,trumpPower/8,high[0]/4.0,high[1]/4.0,high[2]/4.0,high[3]/4.0,squares/64,voids/4,rankPower/8,position/3.0,prior/8.0};double value=0;for(int i=0;i<x.length;i++)value+=x[i]*COEFFICIENTS[i];return Math.max(0,Math.min(hand.size(),(int)Math.round(value)));
    }
}

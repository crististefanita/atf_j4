package ro.whistlab.coach;
import java.util.*;
/** Independent feature extraction for the calibrated opponent-bid prior; no engine imports. */
final class EstimatedBidPrior {
    static final String TRAINING_DIGEST="7449c56af437361c78323333adfccb18c56c72443efe6c28e2db94b520dee484";
    private static final double[] BETA={-1.1626522830945545,0.32076073451405246,1.6440294714512247,6.1934081467467434,4.5559029030823668,3.1114380968168511,1.5480000672648795,0.65723341932640167,0.28254540793945859,-4.6309694340086969,0.93940998947564402,1.5942978045527603,0.54940020176091886,-0.51305034257791493};
    private EstimatedBidPrior(){}
    static int predict(List<Integer> cards,int trump,int position,int totalBid){
        var counts=new int[4];var honors=new int[4];double cut=0,cutRank=0,strength=0;for(int c:cards){int suit=c/13,rank=c%13+2;counts[suit]++;strength+=(rank-7)/7.0;if(suit==trump){cut++;cutRank+=(rank-7)/7.0;}else if(rank>10)honors[14-rank]++;}double concentration=0,missing=0;for(int count:counts){concentration+=count*count;if(count==0)missing++;}double[] features={1,cards.size()/8.0,trump==4?1:0,cut/8,cutRank/8,honors[0]/4.0,honors[1]/4.0,honors[2]/4.0,honors[3]/4.0,concentration/64,missing/4,strength/8,position/3.0,totalBid/8.0};double mean=0;for(int f=0;f<features.length;f++)mean+=features[f]*BETA[f];return Math.max(0,Math.min(cards.size(),(int)Math.round(mean)));
    }
}

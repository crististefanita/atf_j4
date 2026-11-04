// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.coach.research.bidding;
import java.util.*;
public final class OpeningBidEvidenceProbe {
    public static Map<String,Object> plan(List<Integer> allowed,int bids,int leads,boolean safety,
                                          double[][][] points,double[][][] exact) {
        var evidence=new OpeningBidEvidence(allowed,bids,leads,safety);
        for(int world=0;world<points.length;world++)evidence.add(points[world],exact[world]);
        var p=evidence.best();
        return Map.of("bid",p.bid(),"lead",p.lead(),"points",p.points(),"exact",p.exact(),"worlds",p.worlds());
    }
}

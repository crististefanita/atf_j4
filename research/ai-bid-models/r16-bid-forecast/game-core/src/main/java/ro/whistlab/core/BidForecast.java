package ro.whistlab.core;

import java.util.HashSet;
import java.util.List;

/** Predicts an unknown future auction action in a hypothetical bid-time holding. */
final class BidForecast {
    private BidForecast() { }

    static boolean supports(List<Card> hand,int size,Card turned,int position,int prefix) {
        return size>=2&&size<=8&&hand.size()==size&&new HashSet<>(hand).size()==size
                &&hand.stream().allMatch(c->c!=null&&c.rank()>=7)
                &&(turned==null||!hand.contains(turned))&&position>=0&&position<4
                &&prefix>=0&&prefix<=position*size;
    }

    static int forecast(List<Card> hand,int size,Trump trump,Card turned,int position,int prefix) {
        if(!supports(hand,size,turned,position,prefix))throw new IllegalArgumentException("Invalid original auction holding or prefix");
        return mode(LearnedBidEvidence.distribution(hand,size,trump,turned,position,prefix));
    }

    /** Exact probability ties keep the lower bid, independently of traversal order elsewhere. */
    static int mode(double[] probabilities) {
        int best=0;
        for(int bid=1;bid<probabilities.length;bid++)if(probabilities[bid]>probabilities[best])best=bid;
        return best;
    }
}

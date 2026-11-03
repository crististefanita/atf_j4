// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.coach;
import java.util.*;
/** Test bridge: its signature cannot represent opponents' private hands. */
public final class PublicTailPlannerProbe {
    public static int choose(IndependentPlayer.Position p,List<Integer> hand,List<IndependentPlayer.Play> trick,
                             int needed,boolean[] seen,int[] remaining) {
        return PublicTailPlanner.choose(p,hand,trick,p.seat(),needed,seen,remaining);
    }
    public static double threat(int card,int trump,boolean[] seen,int[] remaining,int seat) {
        return PublicTailPlanner.unopposedLeadScore(card,trump,seen,remaining,seat);
    }
}

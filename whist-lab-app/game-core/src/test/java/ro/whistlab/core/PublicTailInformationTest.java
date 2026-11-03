// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.core;

import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.*;
import static org.junit.Assert.*;

/** Two legal hidden worlds share one public decision; future choices must not peek at either. */
public class PublicTailInformationTest {
    private static final List<Integer> OWN = List.of(7,51); // 9S, AD
    private static final List<IndependentPlayer.Play> HISTORY = List.of(
            new IndependentPlayer.Play(0,25),new IndependentPlayer.Play(1,18),
            new IndependentPlayer.Play(2,19),new IndependentPlayer.Play(3,20)); // AH,7H,8H,9H
    private static IndependentPlayer.Position position() {
        return new IndependentPlayer.Position(0,3,3,1,OWN,
                List.of(new IndependentPlayer.Seat(2,1,1,0),new IndependentPlayer.Seat(2,0,0,0),
                        new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0)),
                List.of(),HISTORY,21,true,10);
    }
    private static boolean[] seen() {
        boolean[] seen=new boolean[52];for(int c:OWN)seen[c]=true;
        for(var p:HISTORY)seen[p.card()]=true;seen[21]=true;return seen;
    }
    private static int oldChoice(List<List<Integer>> world) throws Exception {
        var method=Class.forName("ro.whistlab.coach.RolloutPlanner").getDeclaredMethod("choose",
                IndependentPlayer.Position.class,List.class,List.class,int.class,int.class,Random.class);
        method.setAccessible(true);return (int)method.invoke(null,position(),world,List.of(),0,0,new Random(0));
    }
    @Test public void indistinguishableWorldsCannotSelectOppositeFutureActions() throws Exception {
        var a=List.of(OWN,List.of(5,6),List.of(8,10),List.of(31,35));
        var b=List.of(OWN,List.of(48,24),List.of(38,22),List.of(32,5));
        int publicChoice=PublicTailPlannerProbe.choose(position(),OWN,List.of(),0,seen(),new int[]{2,2,2,2});
        CapturedRegression.run("public-tail-no-hidden-world",getClass().getName(),
                "onePublicStateHasOneContinuationAction",Map.of("purpose","Remove hidden-world-dependent future actions",
                "rule","The same own hand, public plays, contracts and remaining counts cannot choose9S in one hidden world andAD in another. Hidden cards are model data only; the public continuation API cannot receive them.",
                "publicPosition",position(),"hypotheticalWorldA",a,"hypotheticalWorldB",b,
                "literalTerminalValues",Map.of("worldA",Map.of("9S",6,"AD",-1),"worldB",Map.of("9S",-1,"AD",6)),
                "scope","The two worlds are equally weighted teaching examples. This is information consistency, not proof that one card wins in every real world."),c->{
            c.equal("Old world-aware guide changes future choice",List.of(7,51),List.of(oldChoice(a),oldChoice(b)));
            c.equal("Public continuation retains its action after any hidden-world change",publicChoice,
                    PublicTailPlannerProbe.choose(position(),OWN,List.of(),0,seen(),new int[]{2,2,2,2}));
            c.equal("A decision made after seeing each world has an unattainable mean of6",6.0,(6.0+6)/2);
            c.equal("Either single public action has the attainable mean2.5",2.5,(6.0-1)/2);
        });
    }
    @Test public void lowTrumpCanBeSaferThanLeadingAnOffSuitAce() throws Exception {
        var hand=List.of(5,25); // 7S, AH; spades trump
        boolean[] seen=new boolean[52];for(int c:hand)seen[c]=true;seen[6]=true;
        var p=new IndependentPlayer.Position(0,3,2,0,hand,position().seats(),List.of(),List.of(),6,false,5);
        double low=PublicTailPlannerProbe.threat(5,0,seen,new int[]{2,2,2,2},0);
        double ace=PublicTailPlannerProbe.threat(25,0,seen,new int[]{2,2,2,2},0);
        CapturedRegression.run("public-tail-low-trump-exit",getClass().getName(),"leadToAvoidAnExtraTrick",
                Map.of("purpose","Avoid the fixed trump-cost trap when leading to lose",
                "rule","At a zero remaining contract, a low trump with many unseen higher trumps can be easier to lose than an off-suit ace. Marginal threat scores rank candidates; they are not calibrated win probabilities.",
                "position",p,"lowTrumpNoThreatScore",low,"aceNoThreatScore",ace),c->{
            c.equal("Low trump has the lower unopposed lead score",true,low<ace);
            c.equal("Public continuation leads7S",5,PublicTailPlannerProbe.choose(p,hand,List.of(),0,seen,new int[]{2,2,2,2}));
        });
    }
    @Test public void followSuitAndMandatoryTrumpRemainHardConstraints() {
        var hand=List.of(7,19,51);
        var p=new IndependentPlayer.Position(0,3,3,1,hand,
                List.of(new IndependentPlayer.Seat(3,1,0,0),new IndependentPlayer.Seat(3,0,0,0),
                        new IndependentPlayer.Seat(3,0,0,0),new IndependentPlayer.Seat(2,0,0,0)),
                List.of(new IndependentPlayer.Play(3,18)),List.of(new IndependentPlayer.Play(3,18)),21,false,5);
        assertEquals(19,PublicTailPlannerProbe.choose(p,hand,List.of(new IndependentPlayer.Play(3,18)),0,seen(),new int[]{3,3,3,2}));
        assertEquals(19,PublicTailPlannerProbe.choose(p,hand,List.of(new IndependentPlayer.Play(3,31)),0,seen(),new int[]{3,3,3,2}));
    }
    @Test public void rankingDoesNotMutateObservedInformation() {
        boolean[] known=seen(),original=known.clone();int[] remaining={2,2,2,2},originalCounts=remaining.clone();
        PublicTailPlannerProbe.choose(position(),OWN,List.of(),0,known,remaining);
        assertArrayEquals(original,known);assertArrayEquals(originalCounts,remaining);
    }
}

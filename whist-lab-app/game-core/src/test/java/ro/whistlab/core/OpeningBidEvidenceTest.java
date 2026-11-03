// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.core;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import ro.whistlab.coach.OpeningBidEvidenceProbe;

public class OpeningBidEvidenceTest {
    @Test public void hiddenWorldCannotChooseItsOwnOpeningBeforeAveraging() throws Exception {
        // Two manually traced feasible two-trick worlds: 9S succeeds only in A; AD only in B.
        // Bid zero, no series reward: +5 for zero tricks, -1 for one extra trick.
        double[][][] scores={{{5,-1}},{{-1,5}}},exact={{{1,0}},{{0,1}}};
        for(boolean safety:List.of(false,true)) {
            var core=new OpeningBidEvidence(List.of(0),1,2,safety);
            for(int world=0;world<2;world++)core.add(scores[world],exact[world]);
            var p=core.best();var independent=OpeningBidEvidenceProbe.plan(List.of(0),1,2,safety,scores,exact);
            CapturedRegression.run("opening-common-action-"+(safety?"safety":"points"),getClass().getName(),
                    "aggregateTheSameOpeningAcrossAllWorlds",Map.of("purpose","Prevent hidden-world root-action fusion in bidding",
                    "rule","One legal opening must be retained across both equally weighted teaching worlds. Selecting a different winner inside each world invents100% exactness and5 points. A common opening has50% exactness and2 points.",
                    "worldPoints",scores,"worldExact",exact,"leadNames",List.of("9S","AD"),
                    "scope","Finite teaching distribution with literal trick outcomes; not a calibrated real-world probability or an optimal-play claim."),c->{
                c.equal("Core exact rate after a common decision",.5,p.exact());
                c.equal("Independent exact rate after a common decision",.5,independent.get("exact"));
                c.equal("Core attainable expected contract points",2.0,p.points());
                c.equal("Independent attainable expected contract points",2.0,independent.get("points"));
                c.equal("Stable common-lead tie break",0,p.lead());
                c.equal("Both complete worlds are counted",2,p.worlds());
            });
        }
    }
    @Test public void objectiveIsDeclaredSeparatelyForEachAcceptedRole() throws Exception {
        var scores=new double[4][9][1];var exact=new double[4][9][1];
        for(int w=0;w<4;w++) {scores[w][0][0]=w<3?5:-1;exact[w][0][0]=w<3?1:0;
            scores[w][7][0]=w<2?12:-1;exact[w][7][0]=w<2?1:0;}
        var safety=new OpeningBidEvidence(List.of(0,7),9,1,true);
        for(int w=0;w<4;w++)safety.add(scores[w],exact[w]);
        var points=OpeningBidEvidenceProbe.plan(List.of(0,7),9,1,false,scores,exact);
        CapturedRegression.run("opening-declared-objective",getClass().getName(),"retainRoleSpecificObjective",
                Map.of("purpose","Do not silently change the independent player's accepted points objective",
                "rule","A75% zero contract averages3.5 points; a50% seven contract averages5.5. Expert selects exactness; independent retains expected points.",
                "worldPoints",scores,"worldExact",exact,"allowedBids",List.of(0,7)),c->{
            c.equal("Expert retains the higher exact rate",0,safety.best().bid());
            c.equal("Independent retains the higher expected score",7,points.get("bid"));
            c.equal("Score is reported in points, not probability",5.5,points.get("points"));
        });
    }
    @Test public void noSampleCannotBecomeAnApparentlyCertainOpening() {
        assertThrows(IllegalStateException.class,()->new OpeningBidEvidence(List.of(0),1,2,true).best());
        assertThrows(IllegalStateException.class,()->OpeningBidEvidenceProbe.plan(List.of(0),1,2,false,
                new double[0][][],new double[0][][]));
    }
}

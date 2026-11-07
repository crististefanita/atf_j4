package ro.whistlab.core.ai.engine;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import java.util.*;import java.lang.reflect.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
@RunWith(Parameterized.class)
public class LateDiscardBoundaryTest extends atf.gradle.audit.GradleAuditedTest {
 @Parameterized.Parameters(name="{0}")public static Collection<Object[]> cases(){return List.of(new Object[][]{{"avoid",0,0},{"need-one",1,0},{"already-over",0,1},{"fixed-win-stop",1,0},{"fixed-win-need",2,0},{"early-win",1,0},{"split-winner",1,0},{"different-suits",0,0},{"lead",0,0},{"oh-hell",0,0},{"missing-bid",0,0},{"one-card",0,0},{"undertrump-avoid",0,0},{"undertrump-need",1,0}});}
 private final String id;private final int bid,won;public LateDiscardBoundaryTest(String id,int bid,int won){this.id=id;this.bid=bid;this.won=won;}
 private static Card c(Suit s,int rank){return new Card(s,rank);}
 @Test public void onlyAppliesWhenTheCurrentTrickResultAndRetainedSuitAreFixed()throws Exception{
  int hero=3;GameMode mode=GameMode.ROMANIAN_WHIST;Trump trump=Trump.NO_TRUMP;List<Card> hand=List.of(c(Suit.CLUBS,7),c(Suit.CLUBS,11));List<PlayedCard> trick=List.of(new PlayedCard(0,c(Suit.HEARTS,14)),new PlayedCard(1,c(Suit.HEARTS,12)),new PlayedCard(2,c(Suit.HEARTS,11)));String expected=switch(id){case "avoid","already-over"->"JC";case "need-one"->"7C";case "fixed-win-stop"->"AH";case "fixed-win-need"->"JH";case "undertrump-avoid"->"JS";case "undertrump-need"->"7S";default->null;};
  switch(id){case "fixed-win-stop","fixed-win-need"->{hand=List.of(c(Suit.HEARTS,11),c(Suit.HEARTS,14));trick=List.of(new PlayedCard(0,c(Suit.HEARTS,7)),new PlayedCard(1,c(Suit.HEARTS,8)),new PlayedCard(2,c(Suit.HEARTS,9)));}case "early-win"->{hero=1;hand=List.of(c(Suit.HEARTS,11),c(Suit.HEARTS,14));trick=List.of(new PlayedCard(0,c(Suit.HEARTS,7)));}case "split-winner"->{hand=List.of(c(Suit.HEARTS,7),c(Suit.HEARTS,11));trick=List.of(new PlayedCard(0,c(Suit.HEARTS,10)),new PlayedCard(1,c(Suit.CLUBS,14)),new PlayedCard(2,c(Suit.DIAMONDS,14)));}case "different-suits"->hand=List.of(c(Suit.CLUBS,7),c(Suit.DIAMONDS,7));case "lead"->trick=List.of();case "oh-hell"->mode=GameMode.OH_HELL;case "one-card"->hand=List.of(c(Suit.CLUBS,7));case "undertrump-avoid","undertrump-need"->{trump=Trump.SPADES;hand=List.of(c(Suit.SPADES,7),c(Suit.SPADES,11));trick=List.of(new PlayedCard(0,c(Suit.HEARTS,14)),new PlayedCard(1,c(Suit.SPADES,12)),new PlayedCard(2,c(Suit.HEARTS,7)));}}
  var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(s==hero?hand.size():1,s==hero?(id.equals("missing-bid")?(Integer)null:Integer.valueOf(bid)):Integer.valueOf(0),s==hero?won:0,0,0));var view=new MonteCarloAi.View(mode,trump,hero,2,6,hand,seats,trick,List.of(),null,true,10);var context=new LinkedHashMap<String,Object>();context.put("purpose","Guard the late-discard rule and preserve fallback outside its proven domain");context.put("rule","Both cards must share a suit. The trick must already be lost by both, or both must win at the final seat. Retain high only if another future trick is needed; otherwise retain low.");context.put("scope","Constructed public micro-position isolates the decision rule; it is not a complete deal replay.");context.put("before",view);
  CapturedRegression.run("late-"+id,getClass().getName(),"onlyAppliesWhenTheCurrentTrickResultAndRetainedSuitAreFixed",context,capture->{Card actual=LateDiscard.choose(view);capture.equal("Expert structural choice, or null to retain ordinary planning",expected,actual==null?null:LossMiningTest.code(actual));if(view.mode().isRomanian()){Method method=IndependentPlayer.class.getDeclaredMethod("lateDiscard",IndependentPlayer.Position.class);method.setAccessible(true);Integer chosen=(Integer)method.invoke(null,PlayerObservations.independent(view));capture.equal("Independent implementation agrees with literal boundary expectation",expected,chosen==null?null:PublicEndgameOracle.code(chosen));}});
 }
}

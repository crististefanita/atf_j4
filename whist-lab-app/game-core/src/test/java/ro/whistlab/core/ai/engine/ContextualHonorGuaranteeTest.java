package ro.whistlab.core.ai.engine;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.regression.RolloutPolicyRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.testing.CapturedRegression;
import static org.junit.Assert.*;import java.util.*;import org.junit.Test;import ro.whistlab.coach.IndependentPlayer;
public class ContextualHonorGuaranteeTest {
 static MonteCarloAi.View view(List<Card> hand,Trump trump,Card turned,int size){var seats=new ArrayList<MonteCarloAi.Seat>();for(int i=0;i<4;i++)seats.add(new MonteCarloAi.Seat(size,null,0,0,0));return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,trump,0,3,size,hand,seats,List.of(),List.of(),turned,true,10);}
 @Test public void exposedTrumpAcePromotesTheKingToAnUnavoidableWinner()throws Exception{
  var v=view(RolloutPolicyRegressionTest.hand("KS","7D"),Trump.SPADES,RolloutPolicyRegressionTest.card("AS"),2);
  CapturedRegression.run("honor-exposed-ace-promotes-king",getClass().getName(),"exposedTrumpAcePromotesTheKingToAnUnavoidableWinner",Map.of("purpose","Use public exposed cards, not just nominal ranks","rule","AS is out of play as the turned card. KS cannot be beaten when played and every held card must eventually be played; zero is impossible.","before",v),c->{c.equal("Expert cannot choose an impossible zero",true,MonteCarloAi.chooseBid(v)>0);c.equal("Independent cannot choose an impossible zero",true,IndependentPlayer.bid(PlayerObservations.independent(v)).value()>0);});
 }
 @Test public void sideAceIsNotAForcedTrickInAShortHand()throws Exception{
  var v=view(RolloutPolicyRegressionTest.hand("AH"),Trump.SPADES,RolloutPolicyRegressionTest.card("7S"),1);double expected=23.0*22*21/(30.0*29*28);
  CapturedRegression.run("honor-side-ace-one-card-ruff-risk",getClass().getName(),"sideAceIsNotAForcedTrickInAShortHand",Map.of("purpose","A side-suit ace does not mean one guaranteed trick","rule","At lead with AH and S trumps, the30 unknown cards contain7 trumps. All3 opponents must avoid trumps:23/30*22/29*21/28. Its winning chance is below one half.","before",v),c->{c.equal("Independent combinatorial chance matches the card-count formula",expected,MonteCarloAi.oneCardWinProbability(v));c.equal("Expert safety-first bid is zero",0,MonteCarloAi.chooseBid(v));c.equal("Independent safety-first bid is zero",0,IndependentPlayer.bid(PlayerObservations.independent(v)).value());});
 }
}

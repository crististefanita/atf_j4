package ro.whistlab.core;
import static org.junit.Assert.*;import java.util.*;import org.junit.Test;import ro.whistlab.core.baseline.RoundFourAiPolicy;import ro.whistlab.core.baseline.RoundSixMonteCarloAi;import ro.whistlab.coach.IndependentPlayer;
/** Strategy contracts follow the declared objective; frozen point-only policies remain explicit references. */
public class LargeHandStrategyTest {
 @Test public void oneCardBidsMaximizeExactChanceUnderTheSamePublicBidPrefix(){
  int changed=0;for(int seed=91;seed<111;seed++){var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,true,10));
   while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();var view=MonteCarloAi.observe(s,seat);double chance=OneCardBidBelief.probability(view);var legal=GameRules.allowedBids(1,s.bidTotal(),s.bidsSubmitted()==3);
    int expected=legal.size()==1?legal.get(0):Math.abs(2*chance-1)<1e-12?1:chance>.5?1:0;
    int old=RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT),actual=AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);
    assertEquals("One-card contract follows posterior exact chance, not the old point threshold",expected,actual);assertEquals("Independent keeps its separately accepted V8 policy",ro.whistlab.coach.baseline.RoundEightIndependentPlayer.bid(ComponentCorpusCollectorTest.roundEight(s,seat)).value(),IndependentPlayer.bid(AiTrainingTournamentTest.publicPosition(s,seat)).value());if(old!=actual)changed++;game.submitBid(seat,old);
   }
  }assertTrue("The intended objective change must be exercised by this retained prefix set",changed>0);
 }
 private static Card c(String code){return RolloutPolicyRegressionTest.card(code);}
 private static GameSession frame(int which){
  List<List<Card>> deal=switch(which){
   case 0 ->List.of(RolloutPolicyRegressionTest.hand("KH","AS"),RolloutPolicyRegressionTest.hand("AH","7S"),RolloutPolicyRegressionTest.hand("7H","AC"),RolloutPolicyRegressionTest.hand("7D","7C"));
   case 1 ->List.of(RolloutPolicyRegressionTest.hand("AH","7H"),RolloutPolicyRegressionTest.hand("KH","8H"),RolloutPolicyRegressionTest.hand("QH","9H"),RolloutPolicyRegressionTest.hand("JH","10H"));
   default->List.of(RolloutPolicyRegressionTest.hand("7H","AH"),RolloutPolicyRegressionTest.hand("10H","7C"),RolloutPolicyRegressionTest.hand("JH","7D"),RolloutPolicyRegressionTest.hand("QH","7S"));};
  var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,813+which,true,10),deal,which==0?Trump.SPADES:Trump.NO_TRUMP,which==2?0:3);
  while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();game.submitBid(s.biddingSeat(),which==1&&s.biddingSeat()==0?1:0);}if(which==2){game.playCard(1,c("10H"));game.playCard(2,c("JH"));game.playCard(3,c("QH"));}return game;
 }
 @Test public void safetyRankingDoesNotRelabelPointEvidenceAsProbability(){
  int compared=0;for(int which=0;which<3;which++){var game=frame(which);var view=MonteCarloAi.observe(game.snapshot(),0);var current=MonteCarloAi.chooseCard(view);var frozen=(RoundSixMonteCarloAi.View)RolloutPolicyRegressionTest.frozenView(view);var reference=RoundSixMonteCarloAi.chooseCard(frozen);
   var frozenEvidence=ro.whistlab.core.baseline.RoundFourteenMonteCarloAi.chooseCard(AcceptedBaselineViews.expert14(view));assertEquals(reference.candidates().size(),frozenEvidence.candidates().size());for(var candidate:frozenEvidence.candidates()){double expected=reference.candidates().stream().filter(c->c.card().equals(candidate.card())).findFirst().orElseThrow().meanContractPoints();assertEquals("Public meanContractPoints retains its original numeric meaning",expected,candidate.meanContractPoints(),1e-10);compared++;}
   for(var candidate:current.candidates()){assertTrue(Double.isFinite(candidate.meanContractPoints()));assertTrue(candidate.meanContractPoints()>=-view.handSize()-10&&candidate.meanContractPoints()<=view.handSize()+15);}
   assertTrue(GameRules.legalCards(view.hand(),view.trick().isEmpty()?null:view.trick().get(0).card().suit(),view.trump(),view.mode()).contains(current.card()));
  }assertTrue(compared>=6);
 }
}

package ro.whistlab.core.learning.regression;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.simulation.RolloutPolicy;
import ro.whistlab.core.ai.information.RolloutBoundsOracleTest;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import java.util.*;import org.junit.Test;
/** Cover and public-auction cases; expected legality/winners do not call the engine. */
public class SafetyPlanningCasesTest {
 private static Card c(String s){return RolloutPolicyRegressionTest.card(s);}
 @Test public void smallCoversKeepTheKingUntilItCanDisappearUnderTheAce()throws Exception{
  var source=RolloutPolicyRegressionTest.fixture("low-card-covers");var world=RolloutPolicyRegressionTest.copy(RolloutPolicyRegressionTest.fixtureWorld(source.path("world")));int[] targets={3,0,0,0};
  CapturedRegression.run("covers-preserve-exit-for-king",getClass().getName(),"smallCoversKeepTheKingUntilItCanDisappearUnderTheAce",Map.of("purpose","Test a whole same-suit holding instead of assigning a fixed trick to each honor","rule","Duck JH/QH with small covers; when AH appears discard KH; retain a harmless low card. Opponent continuations are explicitly fixed and legal.","scope",source.path("scope").asText(),"input",source),capture->{
   int heroWon=0;for(var row:source.path("rounds")){
    var table=new ArrayList<PlayedCard>();Card led=c(row.path("lead").asText());world.get(0).remove(led);table.add(new PlayedCard(0,led));
    var view=RolloutPolicyRegressionTest.view(world,1,3,Trump.NO_TRUMP,table,targets);String choice=RolloutPolicyRegressionTest.player(view,world,table,1,0);
    capture.equal("Cover choice under "+row.path("lead").asText(),row.path("hero").asText(),choice);Card chosen=c(choice);capture.equal("Own card follows mandatory suit",true,RolloutBoundsOracleTest.legal(world.get(1),led.suit(),Trump.NO_TRUMP).contains(chosen));world.get(1).remove(chosen);table.add(new PlayedCard(1,chosen));
    for(int seat=2;seat<4;seat++){Card reply=c(row.path("seat"+seat).asText());capture.equal("Scripted opponent follows the independent legality rule",true,RolloutBoundsOracleTest.legal(world.get(seat),led.suit(),Trump.NO_TRUMP).contains(reply));world.get(seat).remove(reply);table.add(new PlayedCard(seat,reply));}
    var best=table.get(0);for(var play:table)if(RolloutBoundsOracleTest.power(play.card(),led.suit(),Trump.NO_TRUMP)>RolloutBoundsOracleTest.power(best.card(),led.suit(),Trump.NO_TRUMP))best=play;
    capture.equal("Independent trick winner",row.path("winner").asInt(),best.seat());if(best.seat()==1)heroWon++;
   }capture.equal("All four cards disposed without an unwanted trick",source.path("heroWon").asInt(),heroWon);capture.equal("Whole holding consumed",true,world.stream().allMatch(List::isEmpty));
  });
 }
 @Test public void aSmallCoverCreatesALosingOptionThatASingletonKingDoesNotHave()throws Exception{
  CapturedRegression.run("covers-singleton-vs-protected-king",getClass().getName(),"aSmallCoverCreatesALosingOptionThatASingletonKingDoesNotHave",Map.of("purpose","Do not value K and K+x as identical fixed winners","rule","Under QH, singleton KH must be played and wins in this specified world. With KH/7H, 7H provides a legal losing response.","scope","Constructed current-trick certificate, not a general guarantee of an entire hand."),capture->{
   var world=new ArrayList<List<Card>>();world.add(new ArrayList<>(RolloutPolicyRegressionTest.hand("7C")));world.add(new ArrayList<>(RolloutPolicyRegressionTest.hand("KH","7H")));world.add(new ArrayList<>(RolloutPolicyRegressionTest.hand("10H","8C")));world.add(new ArrayList<>(RolloutPolicyRegressionTest.hand("9H","9C")));var trick=List.of(new PlayedCard(0,c("QH")));int[] goals={0,0,0,0};var view=RolloutPolicyRegressionTest.view(world,1,3,Trump.NO_TRUMP,trick,goals);capture.equal("Protected holding can duck", "7H",RolloutPolicyRegressionTest.player(view,world,trick,1,0));
   world.set(1,new ArrayList<>(RolloutPolicyRegressionTest.hand("KH")));view=RolloutPolicyRegressionTest.view(world,1,3,Trump.NO_TRUMP,trick,goals);capture.equal("Singleton has no low cover", "KH",RolloutPolicyRegressionTest.player(view,world,trick,1,0));
  });
 }
 @Test public void identicalCardsReactToTheOpponentsPublicContract()throws Exception{
  var world=List.of(RolloutPolicyRegressionTest.hand("10H","9D"),RolloutPolicyRegressionTest.hand("9H","JH"),RolloutPolicyRegressionTest.hand("7H","7D"),RolloutPolicyRegressionTest.hand("8H","8D"));
  CapturedRegression.run("auction-same-cards-different-contracts",getClass().getName(),"identicalCardsReactToTheOpponentsPublicContract",Map.of("purpose","Public bidding must influence predicted behavior, not just a raw honor sum","rule","After hero leads10H, an opponent holding9H/JH ducks with9H when needing zero, and takes withJH when needing one. The hero consequently takes2 or0 in this fixed world.","scope","Two legal hypothetical continuations with identical cards; only the public opponent bid changes.","world",world),capture->{
   for(int bid:List.of(0,1)){int[] goals={0,bid,0,0};var view=RolloutPolicyRegressionTest.view(world,0,3,Trump.NO_TRUMP,List.of(),goals);capture.equal("Known public opponent contract is authoritative",bid,MonteCarloAi.biddingTargets(view,world,0)[1]);
    int[] actual=RolloutPolicy.rollout(view,RolloutPolicyRegressionTest.copy(world),c("10H"),goals,RolloutPolicyRegressionTest.stable());capture.equal("Expert continuation reflects remaining opponent need",bid==0?2:0,actual[0]);
    var p=PlayerObservations.independent(view);var hands=new ArrayList<List<Integer>>();for(var h:world)hands.add(new ArrayList<>(h.stream().map(PublicEndgameOracle::id).toList()));int[] independentWon=ro.whistlab.coach.WorldEnumerationProbe.continueAfter(p,hands,PublicEndgameOracle.id(c("10H")),goals,RolloutPolicyRegressionTest.stable());capture.equal("Independent continuation reflects the same public need",bid==0?2:0,independentWon[0]);
   }
  });
 }
}

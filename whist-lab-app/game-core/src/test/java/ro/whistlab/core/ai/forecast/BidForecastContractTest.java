package ro.whistlab.core.ai.forecast;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.testing.CapturedRegression;
import static org.junit.Assert.*;import java.util.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
@RunWith(Parameterized.class) public class BidForecastContractTest {
 @Parameterized.Parameters(name="size={0}")public static Collection<Object[]> cases(){var out=new ArrayList<Object[]>();for(int n=2;n<=8;n++)out.add(new Object[]{n});return out;}private final int size;public BidForecastContractTest(int size){this.size=size;}
 @Test public void knownAuctionRemainsAuthoritativeAndForbiddenLastBidStaysIllegal()throws Exception{
  var cards=new ArrayList<Card>(GameRules.deck(GameMode.ROMANIAN_WHIST,4).subList(0,size));var seats=List.of(new MonteCarloAi.Seat(size,1,0),new MonteCarloAi.Seat(size,0,0),new MonteCarloAi.Seat(size,0,0),new MonteCarloAi.Seat(size,null,0));
  var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,size==8?Trump.NO_TRUMP:Trump.HEARTS,3,3,size,cards,seats,List.of(),List.of(),null,true,10);var world=new ArrayList<List<Card>>();for(int s=0;s<4;s++)world.add(new ArrayList<>(cards));
  CapturedRegression.run("forecast-known-auction-"+size,getClass().getName(),"knownAuctionRemainsAuthoritativeAndForbiddenLastBidStaysIllegal",Map.of("purpose","Prevent a fitted forecast from replacing actual opponent bids","rule","Known bids are hard public inputs; only future unknown bids may be predicted. Last bidder cannot make the total equal the hand size.","before",v,"scope","Synthetic contract control; no optimal-bid label inferred from a lost game."),c->{c.equal("Known0/1/2 bids and counterfactual own goal remain literal",List.of(1,0,0,2),Arrays.stream(MonteCarloAi.biddingTargets(v,world,2)).boxed().toList());int expert=MonteCarloAi.chooseBid(v);int player=IndependentPlayer.bid(PlayerObservations.independent(v)).value();c.equal("Expert avoids forbidden last bid",true,expert>=0&&expert<=size&&expert!=size-1);c.equal("Independent avoids forbidden last bid",true,player>=0&&player<=size&&player!=size-1);});
 }
}

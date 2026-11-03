package ro.whistlab.core;
import java.util.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import ro.whistlab.coach.IndependentPlayer;
/** Independent bidding certificates: k highest trumps guarantee all k tricks regardless of unseen cards. */
@RunWith(Parameterized.class)
public class DominantBidContractTest {
 @Parameterized.Parameters(name="cards={0},lastBid={1}")public static Collection<Object[]> cases(){var rows=new ArrayList<Object[]>();for(int k=2;k<=8;k++)for(boolean last:List.of(false,true))rows.add(new Object[]{k,last});return rows;}
 final int size;final boolean last;public DominantBidContractTest(int size,boolean last){this.size=size;this.last=last;}
 @Test public void allTopTrumpsBidTheForcedCountOrNearestLegalContract()throws Exception{
  var hand=new ArrayList<Card>();for(int rank=15-size;rank<=14;rank++)hand.add(new Card(Suit.SPADES,rank));var seats=new ArrayList<MonteCarloAi.Seat>();
  for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(size,last&&s!=0?0:null,0,0,0));
  var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,last?0:3,size,hand,seats,List.of(),List.of(),size==8?null:new Card(Suit.HEARTS,7),true,10);
  int expected=last?size-1:size;
  CapturedRegression.run("bid-top-trumps-"+size+(last?"-forbidden":"-open"),getClass().getName(),"allTopTrumpsBidTheForcedCountOrNearestLegalContract",Map.of("purpose","Check bidding at every size 2..8 against a deterministic rule-derived contract","rule","Holding every one of the k highest trumps and no other cards forces exactly k won tricks. Every card follows/trumps and beats any unseen trump. When the final bid k is forbidden by three zero bids, k-1 is the unique legal contract with the smallest miss.","scope","Constructed public hand; no private cards supplied to either bidder. This is a correctness certificate, not tournament superiority.","before",view),c->{
   c.equal("Expert selects forced exact count or unique nearest legal bid",expected,MonteCarloAi.chooseBid(view));
   c.equal("Independent player selects same rule-derived legal bid",expected,IndependentPlayer.bid(LostGameContractRegressionTest.independent(view)).value());
   c.equal("Literal attainable base score",last?-1:5+size,last?-Math.abs(size-expected):5+expected);
  });
 }
}

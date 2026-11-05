package ro.whistlab.core.learning.reference;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.learning.regression.RolloutPolicyRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.testing.CapturedRegression;
import java.util.*;import java.lang.reflect.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;
@RunWith(Parameterized.class)
public class FrozenContinuationContractTest {
 @Parameterized.Parameters(name="cards={0},trump={1}")public static Collection<Object[]> cases(){var out=new ArrayList<Object[]>();for(int size=2;size<=8;size++)for(Trump trump:List.of(Trump.NO_TRUMP,Trump.SPADES))out.add(new Object[]{size,trump});return out;}
 final int size;final Trump trump;public FrozenContinuationContractTest(int size,Trump trump){this.size=size;this.trump=trump;}
 @Test public void expertCardContinuationKeepsTheFrozenV6Policy()throws Exception{
  var deck=new ArrayList<Card>();for(Suit suit:Suit.values())for(int rank=7;rank<=14;rank++)deck.add(new Card(suit,rank));Collections.shuffle(deck,new Random(9800+size+trump.ordinal()*91L));var world=new ArrayList<List<Card>>();for(int seat=0;seat<4;seat++)world.add(new ArrayList<>(deck.subList(seat*size,(seat+1)*size)));
  int[] goals={1,0,1,0};var view=RolloutPolicyRegressionTest.view(world,0,3,trump,List.of(),goals);var old=RolloutPolicyRegressionTest.frozenView(view);Card first=world.get(0).get(0);
  CapturedRegression.run("bid-guide-card-isolation-"+size+"-"+trump,getClass().getName(),"expertCardContinuationKeepsTheFrozenV6Policy",Map.of("purpose","Preserve the frozen continuation simulator while changing the objective and bidding sample budget","rule","Given the same forced first card, hypothetical deal, contracts and random stream, the continuation simulator must remain identical to V6. The separate safety criterion may rank card candidates differently.","scope","Constructed full worlds compare actual frozen and current continuation methods; no claim of better play.","before",view,"hypotheticalWorld",world,"forcedFirstCard",first),capture->{
   var baseline=old.getClass().getEnclosingClass().getDeclaredMethod("rollout",old.getClass(),List.class,Card.class,int[].class,Random.class);baseline.setAccessible(true);
   int[] expected=(int[])baseline.invoke(null,old,RolloutPolicyRegressionTest.copy(world),first,goals,RolloutPolicyRegressionTest.stable());int[] actual=ro.whistlab.core.ai.simulation.RolloutPolicy.rollout(view,RolloutPolicyRegressionTest.copy(world),first,goals,RolloutPolicyRegressionTest.stable());
   capture.equal("Every seat's final trick count remains byte-for-byte equivalent to frozen V6",Arrays.stream(expected).boxed().toList(),Arrays.stream(actual).boxed().toList());
  });
 }
}

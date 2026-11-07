package ro.whistlab.core.ai.information;

import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.learning.regression.RolloutPolicyRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import static atf.gradle.audit.assertions.GradleAssertions.*;import java.util.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;
/** Independent exhaustive current-trick continuations; does not call engine legality/winner for its expected sets. */
@RunWith(Parameterized.class)
public class RolloutBoundsOracleTest extends atf.gradle.audit.GradleAuditedTest {
 @Parameterized.Parameters(name="cards={0},players={1},trump={2}")public static Collection<Object[]> cases(){var out=new ArrayList<Object[]>();for(int size=2;size<=8;size++)for(int n=3;n<=6;n++)for(Trump trump:List.of(Trump.NO_TRUMP,Trump.SPADES))out.add(new Object[]{size,n,trump});return out;}
 final int size,n;final Trump trump;public RolloutBoundsOracleTest(int size,int n,Trump trump){this.size=size;this.n=n;this.trump=trump;}
 public static List<Card> legal(List<Card> hand,Suit led,Trump trump){
  if(led==null)return hand;var follows=hand.stream().filter(c->c.suit()==led).toList();if(!follows.isEmpty())return follows;var trumps=hand.stream().filter(c->c.suit().ordinal()==trump.ordinal()).toList();return trumps.isEmpty()?hand:trumps;
 }
 public static int power(Card c,Suit led,Trump trump){return(c.suit().ordinal()==trump.ordinal()?200:c.suit()==led?100:0)+c.rank();}
 static void outcomes(List<List<Card>> world,int next,int left,Suit led,Trump trump,int ownPower,int bestPower,int owner,int actor,Set<Boolean> result,Set<String> visited){
  if(result.size()==2||!visited.add(next+"/"+left+"/"+bestPower+"/"+owner))return;if(left==0){result.add(owner==actor);return;}
  for(Card c:legal(world.get(next),led,trump)){int value=power(c,led,trump);outcomes(world,(next+1)%world.size(),left-1,led,trump,ownPower,Math.max(bestPower,value),value>bestPower?next:owner,actor,result,visited);}
 }
 @Test public void certaintyPreferenceSurvivesEveryLegalContinuationAtSizesTwoThroughEight()throws Exception{
  for(int sample=0;sample<12;sample++){
   var deck=new ArrayList<Card>();for(Suit suit:Suit.values())for(int rank=15-2*n;rank<=14;rank++)deck.add(new Card(suit,rank));Collections.shuffle(deck,new Random(710L+sample+size*71L+n*997L+trump.ordinal()));
   var world=new ArrayList<List<Card>>();for(int s=0;s<n;s++)world.add(new ArrayList<>(deck.subList(s*size,(s+1)*size)));
   int actor=sample%n,played=sample%n;var trick=new ArrayList<PlayedCard>();for(int s=0;s<played;s++){int seat=(actor-played+s+n)%n;var options=legal(world.get(seat),trick.isEmpty()?null:trick.get(0).card().suit(),trump);Card c=options.get(0);world.get(seat).remove(c);trick.add(new PlayedCard(seat,c));}
   int[] goals=new int[n];goals[actor]=sample%2;var view=RolloutPolicyRegressionTest.view(world,actor,(actor+n-1)%n,trump,trick,goals);
   Suit led=trick.isEmpty()?null:trick.get(0).card().suit();var options=legal(world.get(actor),led,trump);var secure=new HashSet<String>();
   for(Card candidate:options){Suit suit=led==null?candidate.suit():led;int strength=power(candidate,suit,trump),best=strength,owner=actor;
    for(var p:trick)if(power(p.card(),suit,trump)>best){best=power(p.card(),suit,trump);owner=p.seat();}
    var result=new HashSet<Boolean>();outcomes(world,(actor+1)%n,n-trick.size()-1,suit,trump,strength,best,owner,actor,result,new HashSet<>());
    if(result.equals(Set.of(goals[actor]>0)))secure.add(LossMiningTest.code(candidate));
   }
   String expert=RolloutPolicyRegressionTest.reference(view,world,trick,actor,goals[actor]),player=RolloutPolicyRegressionTest.player(view,world,trick,actor,goals[actor]);
   var allowed=options.stream().map(LossMiningTest::code).toList();
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actor", actor, "allowed", allowed, "deck", deck, "expert", expert, "goals", goals, "led", led, "options", options, "played", played, "player", player, "sample", sample, "secure", secure, "trick", trick, "view", view, "world", world);
assertTrue("allowed.contains(expert) must hold", allowed.contains(expert));assertTrue("allowed.contains(player) must hold", allowed.contains(player));
   if(!secure.isEmpty()){assertTrue("Reference selected an uncertain result despite a certain desired result: "+secure+" vs "+expert,secure.contains(expert));assertTrue("Independent selected an uncertain result despite a certain desired result",secure.contains(player));}
  }
 }
}

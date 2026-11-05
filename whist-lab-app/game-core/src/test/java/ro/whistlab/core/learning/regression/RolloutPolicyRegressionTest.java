package ro.whistlab.core.learning.regression;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.information.RolloutBoundsOracleTest;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.learning.reference.RolloutPolicy;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import static org.junit.Assert.*;
import java.util.*;import java.lang.reflect.*;import org.junit.Test;import ro.whistlab.coach.IndependentPlayer;
/** Literal trick expectations, independently scored full hand, and both separate policy implementations. */
public class RolloutPolicyRegressionTest {
 public static Card card(String code){int rank=switch(code.substring(0,code.length()-1)){case "A"->14;case "K"->13;case "Q"->12;case "J"->11;default->Integer.parseInt(code.substring(0,code.length()-1));};return new Card(Suit.values()["SHCD".indexOf(code.charAt(code.length()-1))],rank);}
 public static List<Card> hand(String...codes){return Arrays.stream(codes).map(RolloutPolicyRegressionTest::card).toList();}
 public static MonteCarloAi.View view(List<List<Card>> world,int hero,int dealer,Trump trump,List<PlayedCard> trick,int[] goals){
  var seats=new ArrayList<MonteCarloAi.Seat>();for(int i=0;i<world.size();i++)seats.add(new MonteCarloAi.Seat(world.get(i).size(),goals[i],0,0,0));
  return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,trump,hero,dealer,world.stream().mapToInt(List::size).max().orElseThrow(),world.get(hero),seats,trick,List.of(),null,true,10);
 }
 static com.fasterxml.jackson.databind.JsonNode fixture(String id)throws Exception{try(var stream=RolloutPolicyRegressionTest.class.getResourceAsStream("/regressions/rollout-guide/"+id+".json")){if(stream==null)throw new IllegalStateException("Missing named regression fixture "+id);return new com.fasterxml.jackson.databind.ObjectMapper().readTree(stream);}}
 static List<List<Card>> fixtureWorld(com.fasterxml.jackson.databind.JsonNode rows){var world=new ArrayList<List<Card>>();for(var row:rows){var cards=new ArrayList<Card>();for(var c:row)cards.add(card(c.asText()));world.add(cards);}return world;}
 public static Random stable(){return new Random(0){@Override public int nextInt(int bound){return bound-1;}};}
 public static String reference(MonteCarloAi.View v,List<List<Card>> w,List<PlayedCard> t,int hero,int need){return LossMiningTest.code(RolloutPolicy.choose(v,w,t,hero,need,stable()));}
 public static String player(MonteCarloAi.View v,List<List<Card>> w,List<PlayedCard> t,int hero,int need)throws Exception{
  var p=PlayerObservations.independent(v);var hands=w.stream().map(h->h.stream().map(PublicEndgameOracle::id).toList()).toList();var trick=t.stream().map(x->new IndependentPlayer.Play(x.seat(),PublicEndgameOracle.id(x.card()))).toList();
  var method=Class.forName("ro.whistlab.coach.RolloutPlanner").getDeclaredMethod("choose",IndependentPlayer.Position.class,List.class,List.class,int.class,int.class,Random.class);method.setAccessible(true);return PublicEndgameOracle.code((int)method.invoke(null,p,hands,trick,hero,need,stable()));
 }
 private void literal(String id,List<List<Card>> world,int actor,Trump trump,List<PlayedCard> trick,int need,String expected)throws Exception{
  int[] goals=new int[world.size()];goals[actor]=Math.max(0,need);var v=view(world,actor,(actor+world.size()-1)%world.size(),trump,trick,goals);
  CapturedRegression.run("rollout-"+id,getClass().getName(),"mandatoryResponsesControlTheHypotheticalTrick",Map.of("purpose","Correct card evaluation and bid estimates inside a hypothetical sampled deal","scope","Constructed component world; no real hidden opponents' cards are an input to the public decision API","rule","Follow suit; otherwise trump if available. No obligation to overtrump. Only actors who have not played this trick may affect its remaining outcome.","before",v,"hypotheticalWorld",world,"literalExpectedCard",expected),c->{c.equal("Test-only rule reference obeys independently derived literal expectation",expected,reference(v,world,trick,actor,need));c.equal("Independent rollout obeys independently derived literal expectation",expected,player(v,world,trick,actor,need));});
 }
 @Test public void burnDangerousTrumpWhenEveryOpponentMustBeatIt()throws Exception{literal("burn-trump",List.of(hand("AH","7S","7C"),hand("7H","8S","7D"),hand("8H","9S","9D"),hand("9H","10S","8D")),0,Trump.SPADES,List.of(),0,"7S");}
 @Test public void conserveTopTrumpWhenAPlainSuitAlreadyWins()throws Exception{literal("cheap-secure-win",List.of(hand("QH","AS"),hand("7H","KS"),hand("8H","QS"),hand("9H","JS")),0,Trump.SPADES,List.of(),1,"QH");}
 @Test public void mandatoryTrumpDoesNotMeanMandatoryOvertrump()throws Exception{literal("undertrump",List.of(hand("7D"),hand("7S","JS"),hand("QS"),hand("KS")),1,Trump.SPADES,List.of(new PlayedCard(0,card("AH"))),0,"JS");}
 @Test public void forcedLossKeepsTheHighTrumpForTheRemainingTrick()throws Exception{var source=fixture("forced-loss-retained-trump");literal("preserve-high-when-forced-to-lose",fixtureWorld(source.path("remainingWorld")),source.path("hero").asInt(),Trump.SPADES,List.of(new PlayedCard(0,card("AS"))),1,source.path("expectedCard").asText());}
 @Test public void alreadyLostCardCannotRegainTheTrick()throws Exception{literal("already-lost",List.of(hand("7D"),hand("8D"),hand("8H","9H"),hand("10H")),2,Trump.NO_TRUMP,List.of(new PlayedCard(0,card("7H")),new PlayedCard(1,card("AH"))),0,"9H");}
 @Test public void finalSeatSpendsCheapestGuaranteedWinner()throws Exception{literal("last-seat-win",List.of(hand("7D"),hand("8D"),hand("9D"),hand("JH","QH")),3,Trump.NO_TRUMP,List.of(new PlayedCard(0,card("7H")),new PlayedCard(1,card("8H")),new PlayedCard(2,card("9H"))),1,"JH");}
 @Test public void unavoidableWinnerDisposesTheLargerCard()throws Exception{literal("unavoidable-win",List.of(hand("7D"),hand("8D"),hand("9D"),hand("JH","QH")),3,Trump.NO_TRUMP,List.of(new PlayedCard(0,card("7H")),new PlayedCard(1,card("8H")),new PlayedCard(2,card("9H"))),0,"QH");}
 @Test public void noTrumpDiscardsTheDangerousOffSuitAce()throws Exception{literal("nt-discard",List.of(hand("7D"),hand("8D"),hand("7S","AC"),hand("8H")),2,Trump.NO_TRUMP,List.of(new PlayedCard(0,card("AH")),new PlayedCard(1,card("7H"))),0,"AC");}
 @Test public void sixSeatWrapIncludesOnlyTheTwoRemainingActors()throws Exception{literal("six-seat-wrap",List.of(hand("QH"),hand("7D"),hand("8D"),hand("9D"),hand("10H","JH"),hand("AH")),4,Trump.NO_TRUMP,List.of(new PlayedCard(1,card("7H")),new PlayedCard(2,card("8H")),new PlayedCard(3,card("9H"))),0,"JH");}
 public static Object frozenView(MonteCarloAi.View v){
  return new ro.whistlab.core.baseline.v6.RoundSixMonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats().stream().map(s->new ro.whistlab.core.baseline.v6.RoundSixMonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),v.trick(),v.history(),v.turnedCard(),v.streakBonuses(),v.streakBonusPoints());
 }
 static Object frozenPosition(MonteCarloAi.View v){
  var p=PlayerObservations.independent(v);return new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Position(p.seat(),p.dealer(),p.size(),p.trump(),p.hand(),p.seats().stream().map(s->new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),List.of(),List.of(),-1,p.streakBonuses(),p.streakBonusPoints());
 }
 public static List<List<Card>> copy(List<List<Card>> w){var result=new ArrayList<List<Card>>();for(var h:w)result.add(new ArrayList<>(h));return result;}
 static int[] rollExpert(MonteCarloAi.View v,List<List<Card>> w,int[] goals,boolean old)throws Exception{
  if(!old)return guideReference(v,w,goals);
  Object observed=frozenView(v);var method=ro.whistlab.core.baseline.v6.RoundSixMonteCarloAi.class.getDeclaredMethod("rollout",observed.getClass(),List.class,Card.class,int[].class,Random.class);method.setAccessible(true);return (int[])method.invoke(null,observed,copy(w),null,goals,stable());
 }
 static int[] guideReference(MonteCarloAi.View v,List<List<Card>> world,int[] goals)throws Exception{
  var hands=copy(world);var trick=new ArrayList<>(v.trick());int n=hands.size(),actor=trick.isEmpty()?(v.dealer()+1)%n:v.seat();int[] won=v.seats().stream().mapToInt(MonteCarloAi.Seat::won).toArray();var old=frozenView(v);
  var method=old.getClass().getEnclosingClass().getDeclaredMethod("greedy",List.class,List.class,old.getClass(),int.class,int.class,Random.class);method.setAccessible(true);
  int remaining=hands.stream().mapToInt(List::size).sum();while(remaining-->0){
   Card chosen=actor==v.seat()?RolloutPolicy.choose(v,hands,trick,actor,goals[actor]-won[actor],stable()):(Card)method.invoke(null,hands.get(actor),trick,old,actor,goals[actor]-won[actor],stable());
   if(!RolloutBoundsOracleTest.legal(hands.get(actor),trick.isEmpty()?null:trick.get(0).card().suit(),v.trump()).contains(chosen))throw new AssertionError("Illegal reference move");
   hands.get(actor).remove(chosen);trick.add(new PlayedCard(actor,chosen));
   if(trick.size()==n){var best=trick.get(0);Suit led=best.card().suit();for(var play:trick)if(RolloutBoundsOracleTest.power(play.card(),led,v.trump())>RolloutBoundsOracleTest.power(best.card(),led,v.trump()))best=play;actor=best.seat();won[actor]++;trick.clear();}else actor=(actor+1)%n;
  }return won;
 }
 static int[] rollPlayer(MonteCarloAi.View v,List<List<Card>> w,int[] goals,boolean old)throws Exception{
  Class<?> type=old?ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.class:IndependentPlayer.class;Object observed=old?frozenPosition(v):PlayerObservations.independent(v);
  var hands=new ArrayList<List<Integer>>();for(var h:w)hands.add(new ArrayList<>(h.stream().map(PublicEndgameOracle::id).toList()));
  var method=type.getDeclaredMethod("roll",observed.getClass(),List.class,int.class,int[].class,Random.class);method.setAccessible(true);return (int[])method.invoke(null,observed,hands,-1,goals,stable());
 }
 @Test public void twoTrickTrapIsCorrectedAtAllFourRotations()throws Exception{
  var source=fixture("two-trick-zero-contract");var base=fixtureWorld(source.path("world"));
  for(int hero=0;hero<4;hero++){int rotation=hero;var world=new ArrayList<List<Card>>(Collections.nCopies(4,null));for(int s=0;s<4;s++)world.set((s+hero)%4,base.get(s));int[] goals=new int[4];goals[(hero+3)%4]=1;var v=view(world,hero,(hero+3)%4,Trump.SPADES,List.of(),goals);
   CapturedRegression.run("rollout-two-trick-rotation"+hero,getClass().getName(),"twoTrickTrapIsCorrectedAtAllFourRotations",Map.of("purpose","Repair a full hypothetical hand used to estimate both bids and card values","rule","Lead 7S and lose to 10S; when the only remaining diamond is led, discard AH and stay at zero. Leading AH first draws the opponents’ trumps, strands our 7S, and forces one unwanted trick: -1 instead of +5.","before",v,"hypotheticalWorld",world,"scope","Counterexample to rollout policy, not evidence of an actual played loss or a guaranteed public-information strategy"),c->{
    c.equal("Frozen Expert takes one unwanted trick",1,rollExpert(v,world,goals,true)[rotation]);c.equal("Frozen independent takes one unwanted trick",1,rollPlayer(v,world,goals,true)[rotation]);
    c.equal("Test-only guide reference completes zero contract",0,rollExpert(v,world,goals,false)[rotation]);c.equal("New independent completes zero contract",0,rollPlayer(v,world,goals,false)[rotation]);
    c.equal("Literal score gain for zero versus one trick at bid zero",source.path("guidedHeroBase").asInt()-source.path("oldHeroBase").asInt(),MonteCarloAi.points(v,rotation,rollExpert(v,world,goals,false)[rotation],0)-MonteCarloAi.points(v,rotation,rollExpert(v,world,goals,true)[rotation],0));
   });
  }
 }
}

package ro.whistlab.core.session;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;
import static org.junit.Assert.*;import java.util.*;import org.junit.Test;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import com.fasterxml.jackson.databind.*;import ro.whistlab.coach.IndependentPlayer;
@RunWith(Parameterized.class)public class ScoreTranslationRegressionTest {
 @Parameterized.Parameters(name="case={0}")public static Collection<Object[]> cases(){var out=new ArrayList<Object[]>();for(int i=0;i<28;i++)out.add(new Object[]{i});return out;}private final int index;public ScoreTranslationRegressionTest(int index){this.index=index;}
 @Test public void aCommonScoreOffsetCannotChangeTheDecision()throws Exception{var json=new ObjectMapper();var f=json.readTree(getClass().getResourceAsStream("/regressions/score-invariance/cases.json")).get(index);var before=ro.whistlab.core.testing.codec.ObservedPositionJson.view(json,f.path("before"));var after=new MonteCarloAi.View(before.mode(),before.trump(),before.seat(),before.dealer(),before.handSize(),before.hand(),before.seats().stream().map(s->new MonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total()+100,s.streak())).toList(),before.trick(),before.history(),before.turnedCard(),before.streakBonuses(),before.streakBonusPoints());String role=f.path("role").asText(),kind=f.path("kind").asText();
  String a=decision(before,role,kind),b=decision(after,role,kind);CapturedRegression.run(f.path("id").asText(),getClass().getName(),"aCommonScoreOffsetCannotChangeTheDecision",Map.of("purpose","Remove score-offset noise from possible-deal sampling","rule","Adding100 to every total preserves relative scores, bids, cards, series and every future utility comparison. It must not change either the bid or the card.","origin",f.path("origin").asText(),"sourceSha256",f.path("sourceSha256").asText(),"before",before,"commonOffset",100,"originalDecision",a,"translatedDecision",b),c->c.equal("Semantically identical game state has the same decision",a,b));
 }
 private static String decision(MonteCarloAi.View v,String role,String kind){if(role.equals("expert"))return kind.equals("bid")?Integer.toString(MonteCarloAi.chooseBid(v)):LossMiningTest.code(MonteCarloAi.chooseCard(v).card());var p=PlayerObservations.independent(v);return kind.equals("bid")?Integer.toString(IndependentPlayer.bid(p).value()):PublicEndgameOracle.code(IndependentPlayer.card(p).value());}
}

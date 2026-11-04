package ro.whistlab.core.learning.reference;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.LostGameContractRegressionTest;
import ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer;import ro.whistlab.core.baseline.v10.RoundTenMonteCarloAi;
public final class AcceptedBaselineViews {private AcceptedBaselineViews(){}
 public static RoundTenMonteCarloAi.View expert(MonteCarloAi.View v){return new RoundTenMonteCarloAi.View(v.mode(),v.trump(),v.seat(),v.dealer(),v.handSize(),v.hand(),v.seats().stream().map(s->new RoundTenMonteCarloAi.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),v.trick(),v.history(),v.turnedCard(),v.streakBonuses(),v.streakBonusPoints());}
 public static RoundEightIndependentPlayer.Position player(MonteCarloAi.View v){var p=PlayerObservations.independent(v);return new RoundEightIndependentPlayer.Position(p.seat(),p.dealer(),p.size(),p.trump(),p.hand(),p.seats().stream().map(s->new RoundEightIndependentPlayer.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),p.trick().stream().map(x->new RoundEightIndependentPlayer.Play(x.seat(),x.card())).toList(),p.history().stream().map(x->new RoundEightIndependentPlayer.Play(x.seat(),x.card())).toList(),p.turnedCard(),p.streakBonuses(),p.streakBonusPoints());}
}

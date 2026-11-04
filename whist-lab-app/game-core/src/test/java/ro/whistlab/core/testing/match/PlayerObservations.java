package ro.whistlab.core.testing.match;

import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GameSession;

/** Projects only the acting hand and public facts before invoking the independent player. */
public final class PlayerObservations {
    private PlayerObservations() { }
    public static IndependentPlayer.Position independent(GameSession.Snapshot s,int seat) {
        return new IndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),
                s.players().get(seat).hand().stream().map(PlayerObservations::id).toList(),
                s.players().stream().map(p->new IndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),
                s.currentTrick().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),
                s.history().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),
                s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());
    }
    public static IndependentPlayer.Position independent(MonteCarloAi.View v) {
        return new IndependentPlayer.Position(v.seat(),v.dealer(),v.handSize(),v.trump().ordinal(),
                v.hand().stream().map(PlayerObservations::id).toList(),
                v.seats().stream().map(s->new IndependentPlayer.Seat(s.remaining(),s.bid(),s.won(),s.total(),s.streak())).toList(),
                v.trick().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),
                v.history().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),
                v.turnedCard()==null?-1:id(v.turnedCard()),v.streakBonuses(),v.streakBonusPoints());
    }
    private static int id(Card card) { return card.suit().ordinal()*13+card.rank()-2; }
}

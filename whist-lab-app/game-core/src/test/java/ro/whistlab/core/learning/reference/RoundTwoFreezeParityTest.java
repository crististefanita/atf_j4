package ro.whistlab.core.learning.reference;
import ro.whistlab.core.testing.match.PlayerObservations;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.learning.tournament.AiTrainingTournamentTest;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.baseline.PreviousIndependentPlayer;
import ro.whistlab.core.baseline.v2.RoundTwoAiPolicy;

/** Run once before V3 changes; excluded from ordinary regression and retained as a freeze receipt. */
public class RoundTwoFreezeParityTest {
    @Test public void frozenCopiesMakeTheSameDecisionsBeforeTrainingChanges(){
        int compared=0;for(int seed=800;seed<802;seed++){
            var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,true,10));
            for(int hand=0;hand<6;hand++){
                while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();int current=AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);assertEquals(current,RoundTwoAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));assertEquals(IndependentPlayer.bid(PlayerObservations.independent(s,seat)).value(),PreviousIndependentPlayer.bid(OfflineMatchHarness.previous(s,seat)).value());game.submitBid(seat,current);compared+=2;}
                while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();var current=AiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);assertEquals(current,RoundTwoAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT));assertEquals(IndependentPlayer.card(PlayerObservations.independent(s,seat)).value(),PreviousIndependentPlayer.card(OfflineMatchHarness.previous(s,seat)).value());game.playCard(seat,current);compared+=2;}
                if(hand<5)game.startNextHand();
            }
        }assertTrue(compared>100);
    }
}

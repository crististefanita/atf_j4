package ro.whistlab.core.learning.reference;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.testing.match.OfflineMatchHarness;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import ro.whistlab.core.baseline.v2.RoundTwoAiPolicy;
import ro.whistlab.core.baseline.v3.RoundThreeAiPolicy;
import ro.whistlab.core.baseline.v4.RoundFourAiPolicy;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

import ro.whistlab.coach.baseline.RoundFourIndependentPlayer;
import ro.whistlab.coach.baseline.RoundThreeIndependentPlayer;

/** The new policy changes only one-card bids; verify the frozen reference at every other decision. */
public class RoundThreeBaselineParityTest {
    @Test public void frozenPoliciesMatchUnchangedDecisionsAndTheOlderAnalyticOneCardReference(){
        var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,811,true,10));int cards=0,bids=0;
        while(true){
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();int old=RoundThreeAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);
                if(s.handSize()==1)assertEquals(old,RoundTwoAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));else {assertEquals(old,RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));assertEquals(RoundThreeIndependentPlayer.bid(OfflineMatchHarness.roundThree(s,seat)).value(),RoundFourIndependentPlayer.bid(OfflineMatchHarness.roundFour(s,seat)).value());}game.submitBid(seat,old);bids++;
            }
            while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();Card old=RoundThreeAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);assertEquals(old,RoundFourAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT));assertEquals(RoundThreeIndependentPlayer.card(OfflineMatchHarness.roundThree(s,seat)).value(),RoundFourIndependentPlayer.card(OfflineMatchHarness.roundFour(s,seat)).value());game.playCard(seat,old);cards++;}
            if(game.snapshot().finalHand())break;game.startNextHand();
        }assertEquals(376,cards);assertEquals(96,bids);
    }
}

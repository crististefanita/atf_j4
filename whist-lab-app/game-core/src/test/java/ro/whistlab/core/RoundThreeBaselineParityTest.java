package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.core.baseline.*;
import ro.whistlab.coach.baseline.RoundFourIndependentPlayer;
import ro.whistlab.coach.baseline.RoundThreeIndependentPlayer;

/** The new policy changes only one-card bids; verify the frozen reference at every other decision. */
public class RoundThreeBaselineParityTest {
    @Test public void frozenPoliciesMatchUnchangedDecisionsAndTheOlderAnalyticOneCardReference(){
        var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,811,true,10));int cards=0,bids=0;
        while(true){
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();int old=RoundThreeAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);
                if(s.handSize()==1)assertEquals(old,RoundTwoAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));else {assertEquals(old,RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));assertEquals(RoundThreeIndependentPlayer.bid(ComponentCorpusCollectorTest.roundThree(s,seat)).value(),RoundFourIndependentPlayer.bid(ComponentCorpusCollectorTest.roundFour(s,seat)).value());}game.submitBid(seat,old);bids++;
            }
            while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();Card old=RoundThreeAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);assertEquals(old,RoundFourAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT));assertEquals(RoundThreeIndependentPlayer.card(ComponentCorpusCollectorTest.roundThree(s,seat)).value(),RoundFourIndependentPlayer.card(ComponentCorpusCollectorTest.roundFour(s,seat)).value());game.playCard(seat,old);cards++;}
            if(game.snapshot().finalHand())break;game.startNextHand();
        }assertEquals(376,cards);assertEquals(96,bids);
    }
}

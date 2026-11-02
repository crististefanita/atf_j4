package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.core.baseline.RoundFourAiPolicy;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.baseline.RoundFourIndependentPlayer;

public class LargeHandStrategyTest {
    @Test public void independentFeatureImplementationsMatchAcrossSizesTrumpsPositionsAndPriorTotals()throws Exception{
        var predict=Class.forName("ro.whistlab.coach.EstimatedBidPrior").getDeclaredMethod("predict",List.class,int.class,int.class,int.class);predict.setAccessible(true);var random=new Random(88449);int compared=0;
        for(int size=2;size<=8;size++)for(int t=0;t<5;t++)for(int i=0;i<12;i++){
            var deck=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4));Collections.shuffle(deck,random);var hand=deck.subList(0,size);int pos=i%4,prior=random.nextInt(size*3+1);int actual=EstimatedBidPrior.predict(hand,Trump.values()[t],pos,prior);
            var numeric=hand.stream().map(c->c.suit().ordinal()*13+c.rank()-2).toList();assertEquals((int)predict.invoke(null,numeric,t,pos,prior),actual);assertTrue(actual>=0&&actual<=size);compared++;
        }assertEquals(420,compared);assertEquals(64,EstimatedBidPrior.TRAINING_DIGEST.length());
    }
    @Test public void oneCardBidsKeepTheFrozenV4PolicyForBothPlayers(){
        for(int seed=91;seed<111;seed++){var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,true,10));
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();int old=RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);assertEquals(old,AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT));assertEquals(RoundFourIndependentPlayer.bid(ComponentCorpusCollectorTest.roundFour(s,seat)).value(),IndependentPlayer.bid(AiTrainingTournamentTest.publicPosition(s,seat)).value());game.submitBid(seat,old);}
        }
    }
    @Test public void sampledCardDenialNeverSpendsMoreThanTheDeclaredExpectedPointBudget(){
        var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,813,true,10));int checked=0;
        while(!game.snapshot().finalHand()){
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();game.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.HARD));}
            while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();if(s.handSize()>=2){var decision=MonteCarloAi.chooseCard(MonteCarloAi.observe(s,seat));if(!decision.candidates().isEmpty()){double best=decision.candidates().stream().mapToDouble(MonteCarloAi.Candidate::meanContractPoints).max().orElseThrow();double chosen=decision.candidates().stream().filter(c->c.card().equals(decision.card())).findFirst().orElseThrow().meanContractPoints();assertTrue(chosen+.125+1e-8>=best);checked++;}game.playCard(seat,decision.card());}else game.playCard(seat,s.players().get(seat).hand().get(0));}
            game.startNextHand();
        }assertTrue(checked>30);
    }
}

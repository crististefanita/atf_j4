package ro.whistlab.core;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

/** Opt-in, predeclared held-out paired evaluation; does not establish global optimality. */
public class AiTournamentTest {
    @Test public void ai07ExpertMeetsTheBaselineAcrossPairedDealsAndRotatedSeats()throws Exception {
        Properties results=new Properties();int expert=0,baseline=0,expertExact=0,baselineExact=0,comparisons=0;
        long started=System.nanoTime();
        int seeds=Integer.getInteger("whist.aiBenchmarkSeeds",8);
        boolean everyTablePassed=true;
        for(int players=3;players<=6;players++) {
        int tableExpert=0,tableBaseline=0;
        for(int seed=1000;seed<1000+seeds;seed++)for(int hero=0;hero<players;hero++) {
            int[] old=play(players,seed,hero,AiDifficulty.HARD),updated=play(players,seed,hero,AiDifficulty.EXPERT);
            expert+=updated[0];baseline+=old[0];expertExact+=updated[1];baselineExact+=old[1];
            tableExpert+=updated[0];tableBaseline+=old[0];
            String key="pair."+(comparisons++)+".";
            results.setProperty(key+"seed",""+seed);results.setProperty(key+"seat",""+hero);
            results.setProperty(key+"players",""+players);
            results.setProperty(key+"baselineScore",""+old[0]);results.setProperty(key+"expertScore",""+updated[0]);
            results.setProperty(key+"baselineExact",""+old[1]);results.setProperty(key+"expertExact",""+updated[1]);
        }
        results.setProperty("table."+players+".expertTotal",""+tableExpert);
        results.setProperty("table."+players+".baselineTotal",""+tableBaseline);
        everyTablePassed&=tableExpert>=tableBaseline;
        }
        results.setProperty("comparisons",""+comparisons);results.setProperty("baseline","HARD");
        results.setProperty("baselineTotal",""+baseline);results.setProperty("expertTotal",""+expert);
        results.setProperty("baselineExactContracts",""+baselineExact);results.setProperty("expertExactContracts",""+expertExact);
        results.setProperty("elapsedSeconds",""+((System.nanoTime()-started)/1e9));
        results.setProperty("status",everyTablePassed?"PASS":"FAIL");
        String output=System.getProperty("whist.componentEvidence","");
        if(!output.isBlank()) {
            Files.createDirectories(Path.of(output));
            try(var stream=Files.newOutputStream(Path.of(output,"ai-tournament.properties"))){results.store(stream,"Actual paired AI evaluation; same seeded deals and rotated hero seat");}
        }
        assertTrue("Expert must meet Hard at every table size; total "+expert+" vs "+baseline+" in "+comparisons+" held-out paired games",everyTablePassed);
    }

    private static int[] play(int players,long seed,int hero,AiDifficulty policy) {
        GameSession game=GameSession.newGame(new GameConfig(players,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,seed));int exact=0;
        while(true) {
            while(game.snapshot().phase()==GamePhase.BIDDING) {
                var s=game.snapshot();int seat=s.biddingSeat();game.submitBid(seat,AiPolicy.chooseBid(s,seat,seat==hero?policy:AiDifficulty.HARD));
            }
            while(game.snapshot().phase()==GamePhase.PLAYING) {
                var s=game.snapshot();int seat=s.currentSeat();game.playCard(seat,AiPolicy.chooseCard(s,seat,seat==hero?policy:AiDifficulty.HARD));
            }
            var score=game.snapshot();var player=score.players().get(hero);if(player.bid()==player.tricksWon())exact++;
            if(score.finalHand())return new int[]{player.totalScore(),exact};
            game.startNextHand();
        }
    }
}

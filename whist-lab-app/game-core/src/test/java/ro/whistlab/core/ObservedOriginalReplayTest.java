package ro.whistlab.core;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;

/** Optional task with real recorded inputs; no expected outcomes are supplied to the engine. */
public class ObservedOriginalReplayTest {
    @Test public void allRecordedOriginalInputsProduceTheSameCumulativeScores()throws Exception {
        String fixture=System.getProperty("whist.originalReplay","");
        assertFalse("Provide -Pwhist.originalReplay=<absolute original-match.properties>",fixture.isBlank());
        Properties p=new Properties();try(var in=Files.newInputStream(Path.of(fixture))){p.load(in);}
        int count=Integer.parseInt(p.getProperty("match.handCount","0"));assertTrue(count>0);
        if(!Boolean.getBoolean("whist.allowPartialReplay")) {
            assertEquals("true",p.getProperty("match.complete"));assertEquals(19,count);
        }
        List<Integer> totals=Arrays.asList(0,0,0,0);int moves=0;
        for(int h=0;h<count;h++) {
            String k="hand."+h+".";int size=Integer.parseInt(p.getProperty(k+"size"));
            List<List<Card>> hands=new ArrayList<>();
            for(int seat=0;seat<4;seat++)hands.add(numbers(p.getProperty(k+"cards."+seat)).stream()
                    .map(ObservedOriginalReplayTest::card).collect(Collectors.toList()));
            GameConfig config=GameConfig.defaults(20261101L);
            var base=GameSession.fromDeal(config,hands,Trump.values()[Integer.parseInt(p.getProperty(k+"trump"))],
                    Integer.parseInt(p.getProperty(k+"dealer"))).saveState();
            List<GameSession.PlayerState> players=new ArrayList<>();
            for(var player:base.players())players.add(new GameSession.PlayerState(player.seat(),player.name(),player.human(),
                    player.hand(),null,0,totals.get(player.seat())));
            GameSession game=GameSession.restore(new GameSession.SaveState(config,GamePhase.BIDDING,h,size,base.dealerSeat(),
                    base.biddingSeat(),base.currentSeat(),0,0,base.trump(),-1,players,List.of(),List.of()));
            List<Integer> bids=numbers(p.getProperty(k+"bids"));
            for(int b=0;b<4;b++){int seat=game.snapshot().biddingSeat();game.submitBid(seat,bids.get(seat));}
            for(int id:numbers(p.getProperty(k+"plays"))) {
                int seat=game.snapshot().currentSeat();game.playCard(seat,card(id));moves++;
            }
            assertEquals(GamePhase.SCORE,game.snapshot().phase());
            totals=game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
            assertEquals("Original score parity at hand "+(h+1),numbers(p.getProperty(k+"totals")),totals);
            assertEquals(size,game.snapshot().players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
            assertEquals(h==18,game.snapshot().finalHand());
        }
        if(!Boolean.getBoolean("whist.allowPartialReplay"))assertEquals(436,moves);
    }
    private static Card card(int id){return new Card(Suit.values()[id/13],id%13+2);}
    private static List<Integer> numbers(String csv){return Arrays.stream(csv.split(",")).map(Integer::parseInt).toList();}
}

package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class MatchSummaryTest {
    @Test public void eachProfileExplainsItsOwnRules(){
        assertTrue(MatchSummary.modeDescription(GameMode.ROMANIAN_WHIST).contains("trump if held"));
        assertTrue(MatchSummary.modeDescription(GameMode.OH_HELL).contains("discard or trump"));
        assertTrue(MatchSummary.modeDescription(GameMode.DESCENDING).contains("cycles"));
    }
    @Test public void romanianProgressUsesTwentyFourHands(){assertEquals("Hand 1 / 24",MatchSummary.progress(game().snapshot()));}
    @Test public void leadExplanationDoesNotDemandTrump(){assertTrue(MatchSummary.playHint(game().snapshot()).startsWith("Lead any"));}
    @Test public void heldLedSuitHasPriorityOverTrumpHint(){assertTrue(MatchSummary.playHint(led(GameMode.ROMANIAN_WHIST,Trump.SPADES,List.of(c(Suit.HEARTS,7),c(Suit.SPADES,7)))).startsWith("Follow HEARTS"));}
    @Test public void voidWithTrumpExplainsBothObligationAndOptionalOvertrump(){assertTrue(MatchSummary.playHint(led(GameMode.ROMANIAN_WHIST,Trump.SPADES,List.of(c(Suit.CLUBS,7),c(Suit.SPADES,7)))).contains("Overtrumping is optional"));}
    @Test public void voidWithoutTrumpExplainsDiscard(){assertTrue(MatchSummary.playHint(led(GameMode.ROMANIAN_WHIST,Trump.SPADES,List.of(c(Suit.CLUBS,7),c(Suit.DIAMONDS,7)))).contains("discard any"));}
    @Test public void noTrumpHandCannotRequireTrump(){assertTrue(MatchSummary.playHint(led(GameMode.ROMANIAN_WHIST,Trump.NO_TRUMP,List.of(c(Suit.CLUBS,7),c(Suit.SPADES,7)))).contains("discard any"));}
    @Test public void ohHellExplainsOptionalTrump(){assertEquals("No led suit: discard or play trump.",MatchSummary.playHint(led(GameMode.OH_HELL,Trump.SPADES,List.of(c(Suit.CLUBS,7),c(Suit.SPADES,7)))));}
    @Test public void uniqueWinnerHasSingularLabel(){assertEquals("Winner: You (11 points)",MatchSummary.winners(newGameScore()));}
    @Test public void negativeTotalsAndTiesHaveCorrectWinners(){
        var g=game();var s=g.saveState();var p=new ArrayList<>(s.players());
        for(int seat=0;seat<4;seat++){var old=p.get(seat);p.set(seat,new GameSession.PlayerState(seat,old.name(),old.human(),old.hand(),null,0,seat<2?-1:-7));}
        var tied=GameSession.restore(new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),s.biddingSeat(),s.currentSeat(),0,0,s.trump(),-1,p,List.of(),List.of(),List.of(),s.turnedCard()));
        assertEquals("Joint winners: You, Ava (-1 points)",MatchSummary.winners(tied.snapshot()));
    }
    @Test public void forcedExpertMoveAvoidsUnnecessarySampling(){
        var g=game();var d=MonteCarloAi.chooseCard(MonteCarloAi.observe(g.snapshot(),0));
        assertEquals(g.snapshot().players().get(0).hand().get(0),d.card());assertTrue(d.candidates().isEmpty());assertTrue(d.method().contains("Only legal card"));
    }
    private static GameSession game(){return GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,111));}
    private static Card c(Suit s,int r){return new Card(s,r);}
    private static GameSession.Snapshot led(GameMode mode,Trump trump,List<Card> human){
        var g=GameSession.fromDeal(new GameConfig(4,mode,AiDifficulty.NORMAL,1),List.of(human,List.of(c(Suit.HEARTS,12),c(Suit.DIAMONDS,12)),List.of(c(Suit.SPADES,13),c(Suit.CLUBS,13)),List.of(c(Suit.HEARTS,10),c(Suit.DIAMONDS,10))),trump,2);
        while(g.snapshot().phase()==GamePhase.BIDDING)g.submitBid(g.snapshot().biddingSeat(),0);
        g.playCard(3,c(Suit.HEARTS,10));return g.snapshot();
    }
    private static GameSession.Snapshot newGameScore(){
        var g=GameSession.fromDeal(new GameConfig(4,GameMode.REVERSE_OH_HELL,AiDifficulty.NORMAL,1),List.of(List.of(c(Suit.HEARTS,14)),List.of(c(Suit.HEARTS,13)),List.of(c(Suit.HEARTS,12)),List.of(c(Suit.HEARTS,11))),Trump.SPADES,3);
        g.submitBid(0,1);g.submitBid(1,0);g.submitBid(2,0);g.submitBid(3,1);
        for(int seat=0;seat<4;seat++)g.playCard(seat,c(Suit.HEARTS,14-seat));return g.snapshot();
    }
}

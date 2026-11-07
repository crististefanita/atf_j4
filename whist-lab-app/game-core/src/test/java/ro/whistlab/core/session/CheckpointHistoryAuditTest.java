package ro.whistlab.core.session;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class CheckpointHistoryAuditTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void wrongWinnerWithConservedTrickTotalIsRejected() {
        var s=at(4);var players=new ArrayList<>(s.players());int winner=s.lastTrickWinnerSeat(),other=(winner+1)%4;
        for(int seat:List.of(winner,other)){var p=players.get(seat);players.set(seat,new GameSession.PlayerState(seat,p.name(),p.human(),p.hand(),p.bid(),seat==other?1:0,p.totalScore()));}
        rejects(s,players,s.history(),s.lastTrick(),s.lastTrickWinnerSeat(),s.trump());
    }
    @Test public void inventedLastWinnerIsRejected() {var s=at(4);rejects(s,s.players(),s.history(),s.lastTrick(),(s.lastTrickWinnerSeat()+1)%4,s.trump());}
    @Test public void incompleteLastTrickIsRejected() {var s=at(4);rejects(s,s.players(),s.history(),s.lastTrick().subList(0,3),s.lastTrickWinnerSeat(),s.trump());}
    @Test public void duplicateLastTrickIsRejected() {var s=at(4);var last=new ArrayList<>(s.lastTrick());last.set(1,last.get(0));rejects(s,s.players(),s.history(),last,s.lastTrickWinnerSeat(),s.trump());}
    @Test public void reorderedCompletedHistoryIsRejected() {var s=at(4);var h=new ArrayList<>(s.history());Collections.swap(h,0,1);rejects(s,s.players(),h,s.lastTrick(),s.lastTrickWinnerSeat(),s.trump());}
    @Test public void extraHistoricalWinnerDuringPartialTrickIsRejected() {var s=at(5);rejects(s,s.players(),s.history(),List.of(),0,s.trump());}
    @Test public void validFullHistoryRestoresAtEveryBoundary() {for(int moves=0;moves<=8;moves++){var s=at(moves);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "moves", moves, "s", s);
assertEquals("GameSession.restoreComplete(s).saveState() must equal s", s,GameSession.restoreComplete(s).saveState());}}
    @Test public void eightCardRomanianCheckpointCannotInventTrump() {
        GameSession g=normal();while(g.snapshot().handSize()!=8){finish(g);g.startNextHand();}
        var s=g.saveState();rejects(s,s.players(),s.history(),s.lastTrick(),s.lastTrickWinnerSeat(),Trump.HEARTS);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g, "s", s);
        assertEquals("GameSession.restore(s).saveState() must equal s", s,GameSession.restore(s).saveState());
    }
    @Test public void legacyLastTrickOnlyCheckpointRemainsReadable() {
        GameSession g=normal();while(g.snapshot().handSize()!=3){finish(g);g.startNextHand();}bid(g);
        for(int i=0;i<8;i++)play(g);var s=g.saveState();
        var legacy=new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),s.biddingSeat(),s.currentSeat(),s.bidsSubmitted(),s.bidTotal(),s.trump(),s.lastTrickWinnerSeat(),s.players(),s.currentTrick(),s.lastTrick());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g, "legacy", legacy, "s", s);
        assertEquals("GameSession.restore(legacy).saveState().players() must equal s.players()", s.players(),GameSession.restore(legacy).saveState().players());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restoreComplete(legacy));
    }
    @Test public void versionTwoCannotSilentlyLoseEarlierCompletedTricks() {
        GameSession g=normal();while(g.snapshot().handSize()!=3){finish(g);g.startNextHand();}bid(g);
        for(int i=0;i<8;i++)play(g);var s=g.saveState();
        var truncated=new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),s.biddingSeat(),s.currentSeat(),s.bidsSubmitted(),s.bidTotal(),s.trump(),s.lastTrickWinnerSeat(),s.players(),s.currentTrick(),s.lastTrick(),s.lastTrick(),s.turnedCard());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "g", g, "s", s, "truncated", truncated);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restoreComplete(truncated));
    }
    @Test public void restoreCannotLegalizeFailureToFollowHeldLedSuit(){illegalDiscard(new Card(Suit.HEARTS,8));}
    @Test public void restoreCannotLegalizeAvoidingMandatoryTrump(){illegalDiscard(new Card(Suit.SPADES,9));}
    private static void illegalDiscard(Card stillHeld){
        Card lead=new Card(Suit.HEARTS,7),discard=new Card(Suit.CLUBS,7);
        List<List<Card>> remaining=List.of(List.of(new Card(Suit.CLUBS,8)),List.of(stillHeld),List.of(new Card(Suit.DIAMONDS,7),new Card(Suit.DIAMONDS,8)),List.of(new Card(Suit.SPADES,7),new Card(Suit.SPADES,8)));
        var players=new ArrayList<GameSession.PlayerState>();for(int seat=0;seat<4;seat++)players.add(new GameSession.PlayerState(seat,"P"+seat,seat==0,remaining.get(seat),0,0,0));
        var history=List.of(new PlayedCard(0,lead),new PlayedCard(1,discard));
        var corrupt=new GameSession.SaveState(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,1),GamePhase.PLAYING,4,2,3,3,2,4,0,Trump.SPADES,-1,players,history,List.of(),history);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restoreComplete(corrupt));
    }
    private static void rejects(GameSession.SaveState s,List<GameSession.PlayerState> p,List<PlayedCard> h,List<PlayedCard> last,int winner,Trump trump){
        var bad=new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),s.biddingSeat(),s.currentSeat(),s.bidsSubmitted(),s.bidTotal(),trump,winner,p,s.currentTrick(),last,h,s.turnedCard());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(bad));
    }
    private static GameSession.SaveState at(int moves){GameSession g=normal();while(g.snapshot().handSize()!=2){finish(g);g.startNextHand();}bid(g);for(int i=0;i<moves;i++)play(g);return g.saveState();}
    private static GameSession normal(){return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,7301)));}
    private static void bid(GameSession g){while(g.snapshot().phase()==GamePhase.BIDDING){var s=g.snapshot();g.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.HARD));}}
    private static void play(GameSession g){var s=g.snapshot();g.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.HARD));}
    private static void finish(GameSession g){bid(g);while(g.snapshot().phase()==GamePhase.PLAYING)play(g);}
}

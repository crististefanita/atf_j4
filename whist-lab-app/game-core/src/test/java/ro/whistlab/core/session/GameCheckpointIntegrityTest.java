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

public class GameCheckpointIntegrityTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void rw07CorruptBidTotalOrBiddingSeatCannotBeRestored() {
        var save=fresh().saveState();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "save", save);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(copy(save,save.players(),1,save.biddingSeat())));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(copy(save,save.players(),0,(save.biddingSeat()+1)%4)));
    }

    @Test public void rw07DuplicateHeldCardAndInventedWonTrickAreRejected() {
        var save=fresh().saveState();List<GameSession.PlayerState> players=new ArrayList<>(save.players());
        var p=players.get(1);
        players.set(1,new GameSession.PlayerState(1,p.name(),false,players.get(0).hand(),null,0,0));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "p", p, "players", players, "save", save);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(copy(save,players,0,save.biddingSeat())));
        players.set(1,new GameSession.PlayerState(1,p.name(),false,p.hand(),null,1,0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(copy(save,players,0,save.biddingSeat())));
    }

    @Test public void rw07PlayedCardCannotReappearInPublicHistoryOrAnotherHand() {
        GameSession game=fresh();
        while(game.snapshot().phase()==GamePhase.BIDDING) {
            var s=game.snapshot();game.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.NORMAL));
        }
        var s=game.snapshot();game.playCard(s.currentSeat(),s.players().get(s.currentSeat()).hand().get(0));
        var save=game.saveState();List<PlayedCard> history=new ArrayList<>(save.history());history.add(history.get(0));
        var corrupt=new GameSession.SaveState(save.config(),save.phase(),save.handIndex(),save.handSize(),save.dealerSeat(),
                save.biddingSeat(),save.currentSeat(),save.bidsSubmitted(),save.bidTotal(),save.trump(),save.lastTrickWinnerSeat(),
                save.players(),save.currentTrick(),save.lastTrick(),history);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "corrupt", corrupt, "game", game, "history", history, "s", s, "save", save);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(corrupt));
    }

    @Test public void rw01ReducedDeckRejectsOutOfPackCardsAndDuplicateDeals() {
        var config=new GameConfig(3,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "config", config);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.fromDeal(config,
                List.of(List.of(new Card(Suit.SPADES,2)),List.of(new Card(Suit.HEARTS,14)),List.of(new Card(Suit.CLUBS,14))),Trump.SPADES,2));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.fromDeal(config,
                List.of(List.of(new Card(Suit.SPADES,14)),List.of(new Card(Suit.SPADES,14)),List.of(new Card(Suit.CLUBS,14))),Trump.SPADES,2));
    }

    private static GameSession fresh(){return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,17)));}
    private static GameSession.SaveState copy(GameSession.SaveState s,List<GameSession.PlayerState> players,int total,int bidder) {
        return new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),bidder,s.currentSeat(),
                s.bidsSubmitted(),total,s.trump(),s.lastTrickWinnerSeat(),players,s.currentTrick(),s.lastTrick(),s.history());
    }
}

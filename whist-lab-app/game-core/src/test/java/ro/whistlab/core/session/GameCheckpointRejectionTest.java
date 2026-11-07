package ro.whistlab.core.session;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameMode;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Persisted rule violations must fail closed rather than change an in-progress contract. */
@RunWith(Parameterized.class)
public class GameCheckpointRejectionTest extends atf.gradle.audit.GradleAuditedTest {
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> corruptions() {
        List<Object[]> cases=new ArrayList<>();
        add(cases,"negative hand index",b->b.index=-1);add(cases,"hand past match end",b->b.index=24);
        add(cases,"missing player",b->b.players.remove(3));
        add(cases,"negative dealer",b->b.dealer=-1);add(cases,"dealer outside table",b->b.dealer=4);
        add(cases,"negative bidder",b->b.bidder=-1);add(cases,"bidder outside table",b->b.bidder=4);
        add(cases,"negative current seat",b->b.current=-1);add(cases,"current seat outside table",b->b.current=4);
        add(cases,"negative bid count",b->b.count=-1);add(cases,"too many bids",b->b.count=5);
        add(cases,"negative winner sentinel",b->b.winner=-2);add(cases,"winner outside table",b->b.winner=4);
        add(cases,"seat identity changed",b->{var p=b.players.get(0);b.players.set(0,new GameSession.PlayerState(1,p.name(),true,p.hand(),null,0,0));});
        add(cases,"human flag changed",b->{var p=b.players.get(0);b.players.set(0,new GameSession.PlayerState(0,p.name(),false,p.hand(),null,0,0));});
        add(cases,"negative trick counter",b->b.player(0,null,-1,0));add(cases,"more tricks than hand",b->b.player(0,null,2,0));
        add(cases,"negative bid",b->b.player(0,-1,0,0));add(cases,"bid above hand size",b->b.player(0,2,0,0));
        add(cases,"submitted bid count mismatch",b->b.count=1);
        add(cases,"playing before bids",b->b.phase=GamePhase.PLAYING);
        add(cases,"bids out of clockwise order",b->{b.player((b.dealer+2)%4,0,0,0);b.count=1;});
        add(cases,"forbidden completed bid sum",b->{b.allBids();b.player(0,1,0,0);b.total=1;});
        add(cases,"scoring while cards remain",b->{b.allBids();b.phase=GamePhase.SCORE;});
        add(cases,"wrong player after first card",b->{b.firstPlay();b.current=(b.current+1)%4;});
        add(cases,"missing current trick history",b->{b.firstPlay();b.history.clear();});
        add(cases,"trick not equal to history suffix",b->{b.firstPlay();var p=b.trick.get(0);b.trick.set(0,new PlayedCard((p.seat()+1)%4,p.card()));});
        add(cases,"bidding after all offers",b->{b.allBids();b.phase=GamePhase.BIDDING;});
        return cases;
    }
    private final Consumer<Builder> corrupt;
    public GameCheckpointRejectionTest(String name,Consumer<Builder> corrupt){this.corrupt=corrupt;}
    @Test public void rw07CannotResumeAnInconsistentContract() {
        Builder b=new Builder();corrupt.accept(b);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "b", b);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->GameSession.restore(b.build()));
    }
    private static void add(List<Object[]> cases,String name,Consumer<Builder> mutation){cases.add(new Object[]{name,mutation});}
    private static final class Builder {
        GameSession.SaveState base=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,91))).saveState();
        int index=base.handIndex(),dealer=base.dealerSeat(),bidder=base.biddingSeat(),current=base.currentSeat(),count=0,total=0,winner=-1;
        GamePhase phase=GamePhase.BIDDING;
        List<GameSession.PlayerState> players=new ArrayList<>(base.players());
        List<PlayedCard> trick=new ArrayList<>(),history=new ArrayList<>();
        void player(int seat,Integer bid,int won,int score){var p=players.get(seat);players.set(seat,new GameSession.PlayerState(seat,p.name(),p.human(),p.hand(),bid,won,score));}
        void allBids(){for(int seat=0;seat<4;seat++)player(seat,0,0,0);count=4;total=0;phase=GamePhase.PLAYING;}
        void firstPlay(){allBids();int seat=(dealer+1)%4;var p=players.get(seat);Card c=p.hand().get(0);
            players.set(seat,new GameSession.PlayerState(seat,p.name(),p.human(),List.of(),0,0,0));
            trick.add(new PlayedCard(seat,c));history.addAll(trick);current=(seat+1)%4;}
        GameSession.SaveState build(){return new GameSession.SaveState(base.config(),phase,index,base.handSize(),dealer,bidder,current,count,total,
                base.trump(),winner,players,trick,List.of(),history);}
    }
}

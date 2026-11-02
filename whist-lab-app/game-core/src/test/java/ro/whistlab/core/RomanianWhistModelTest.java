package ro.whistlab.core;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Independent state machine: never calls GameRules for expected legal moves, winners or scores. */
@RunWith(Parameterized.class)
public class RomanianWhistModelTest {
    @Parameterized.Parameters(name="players={0}, AI={1}, seed={2}")
    public static Collection<Object[]> games() {
        List<Object[]> cases=new ArrayList<>();
        for(int players=3;players<=6;players++) for(AiDifficulty difficulty:AiDifficulty.values())
            for(long seed=0;seed<(difficulty==AiDifficulty.EXPERT?2:10);seed++) cases.add(new Object[]{players,difficulty,seed});
        return cases;
    }
    private final int players;
    private final AiDifficulty difficulty;
    private final long seed;
    private final Properties trace=new Properties();
    private int steps;

    public RomanianWhistModelTest(int players,AiDifficulty difficulty,long seed) {
        this.players=players;this.difficulty=difficulty;this.seed=seed;
    }

    @Test public void rw01ThroughRw07EveryMoveOfACompleteGameMatchesIndependentRules()throws Exception {
        GameSession game=GameSession.newGame(new GameConfig(players,GameMode.ROMANIAN_WHIST,difficulty,seed));
        int[] totals=new int[players];int moves=0,tricks=0,negativeScores=0,forcedTrumps=0,voidDiscards=0;
        int initialDealer=game.snapshot().dealerSeat();
        List<Integer> sequence=new ArrayList<>();
        for(int i=0;i<players;i++)sequence.add(1);
        for(int size=2;size<=7;size++)sequence.add(size);
        for(int i=0;i<players;i++)sequence.add(8);
        for(int size=7;size>=2;size--)sequence.add(size);
        for(int i=0;i<players;i++)sequence.add(1);
        for(int h=0;h<sequence.size();h++) {
            var start=game.snapshot();int size=sequence.get(h),dealer=(initialDealer+h)%players;
            assertEquals(h,start.handIndex());assertEquals(size,start.handSize());assertEquals(dealer,start.dealerSeat());
            assertEquals(GameMode.ROMANIAN_WHIST,start.mode());
            if(size==8){assertEquals(Trump.NO_TRUMP,start.trump());assertNull(start.turnedCard());}
            else {assertNotEquals(Trump.NO_TRUMP,start.trump());assertNotNull(start.turnedCard());
                assertEquals(start.trump().ordinal(),start.turnedCard().suit().ordinal());}
            Set<Card> distinct=new HashSet<>();List<List<Card>> hands=new ArrayList<>();
            int minimum=new int[]{9,7,5,3}[players-3];
            for(var p:start.players()) {
                assertEquals(size,p.hand().size());assertNull(p.bid());assertEquals(0,p.tricksWon());
                assertEquals(totals[p.seat()],p.totalScore());
                for(Card c:p.hand()){assertTrue(c.rank()>=minimum);assertTrue(distinct.add(c));}
                hands.add(new ArrayList<>(p.hand()));
            }
            if(start.turnedCard()!=null)assertFalse(distinct.contains(start.turnedCard()));
            record(h==0?"SETUP":"TEST BODY","RW01/RW02","Hand "+(h+1)+" distribution",
                    "size="+size+"; dealer="+dealer+"; distinct="+(players*size),
                    "size="+start.handSize()+"; dealer="+start.dealerSeat()+"; distinct="+distinct.size(),
                    "trump="+start.trump()+"; initial hands="+hands);
            int[] bids=new int[players];int sum=0;
            for(int b=0;b<players;b++) {
                int seat=(dealer+1+b)%players;assertEquals(seat,game.snapshot().biddingSeat());
                int bid=AiPolicy.chooseBid(game.snapshot(),seat,difficulty);
                assertTrue(bid>=0&&bid<=size);
                if(b==players-1)assertNotEquals(size,sum+bid);
                game.submitBid(seat,bid);bids[seat]=bid;sum+=bid;
                assertEquals(game.snapshot(),GameSession.restore(game.saveState()).snapshot());
            }
            record("TEST BODY","RW03","Hand "+(h+1)+" bids in seat order",
                    "phase=PLAYING; first="+((dealer+1)%players)+"; total differs from "+size,
                    "phase="+game.snapshot().phase()+"; first="+game.snapshot().currentSeat()+"; total differs from "+size,
                    "bids="+Arrays.toString(bids)+"; total="+sum);
            List<PlayedCard> trick=new ArrayList<>();int[] won=new int[players];int actor=(dealer+1)%players;
            for(int move=0;move<size*players;move++) {
                var before=game.snapshot();assertEquals(GamePhase.PLAYING,before.phase());assertEquals(actor,before.currentSeat());
                Suit led=trick.isEmpty()?null:trick.get(0).card().suit();
                List<Card> legal=independentLegal(hands.get(actor),led,start.trump());
                Card choice=AiPolicy.chooseCard(before,actor,difficulty);
                assertTrue("held suit/trump requirement: hand="+hands.get(actor)+", led="+led+", trump="+start.trump()+", choice="+choice,
                        legal.contains(choice));
                if(led!=null&&hands.get(actor).stream().noneMatch(c->c.suit()==led)) {
                    if(start.trump().suit().filter(s->s==choice.suit()).isPresent())forcedTrumps++;else voidDiscards++;
                }
                String context="hand before="+hands.get(actor)+"; led="+led+"; trump="+start.trump()
                        +"; legal="+legal+"; seat="+actor+"; played="+choice;
                assertTrue(hands.get(actor).remove(choice));trick.add(new PlayedCard(actor,choice));
                game.playCard(actor,choice);moves++;
                int winner=-1;
                if(trick.size()==players) {
                    winner=independentWinner(trick,start.trump());won[winner]++;actor=winner;tricks++;
                    assertEquals(winner,game.snapshot().lastTrickWinnerSeat());
                    assertEquals(trick,game.snapshot().lastTrick());trick.clear();
                }else actor=(actor+1)%players;
                boolean scored=move+1==size*players;
                if(scored)for(int seat=0;seat<players;seat++)totals[seat]+=won[seat]==bids[seat]?5+won[seat]:-Math.abs(won[seat]-bids[seat]);
                var actual=game.snapshot();
                for(int seat=0;seat<players;seat++) {
                    assertEquals(new HashSet<>(hands.get(seat)),new HashSet<>(actual.players().get(seat).hand()));
                    assertEquals(won[seat],actual.players().get(seat).tricksWon());
                    assertEquals(totals[seat],actual.players().get(seat).totalScore());
                    assertEquals(bids[seat],actual.players().get(seat).bid().intValue());
                }
                assertEquals(trick,actual.currentTrick());
                if(!scored)assertEquals(actor,actual.currentSeat());
                String expected="phase="+(scored?"SCORE":"PLAYING")+"; won="+Arrays.toString(won)+"; totals="+Arrays.toString(totals)
                        +(scored?"":"; next="+actor);
                String observed="phase="+actual.phase()+"; won="+actual.players().stream().map(GameSession.PlayerSnapshot::tricksWon).toList()
                        +"; totals="+actual.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList()
                        +(scored?"":"; next="+actual.currentSeat());
                record("TEST BODY",scored?"RW04/RW05/RW06":"RW04/RW05","Hand "+(h+1)+", move "+(move+1),expected,observed,
                        context+"; resolved winner="+winner);
                if(move%7==0||scored) {
                    game=GameSession.restore(game.saveState());assertEquals(actual,game.snapshot());
                }
            }
            var score=game.snapshot();assertEquals(GamePhase.SCORE,score.phase());
            assertEquals(size,Arrays.stream(won).sum());
            for(int total:totals)if(total<0)negativeScores++;
            boolean finalHand=h+1==sequence.size();assertEquals(finalHand,score.finalHand());
            if(finalHand) {
                GameSession finished=game;
                assertThrows(IllegalStateException.class,finished::startNextHand);assertEquals(score,game.snapshot());
            }else game.startNextHand();
        }
        assertEquals(players*(10*players+54),moves);assertEquals(10*players+54,tricks);
        record("TEARDOWN","RW07","Complete game and immutable final score",
                "hands="+(3*players+12)+"; moves="+(players*(10*players+54))+"; tricks="+(10*players+54),
                "hands="+sequence.size()+"; moves="+moves+"; tricks="+tricks,"final totals="+Arrays.toString(totals));
        String output=System.getProperty("whist.componentEvidence","");
        if(!output.isBlank()) {
            Path out=Path.of(output);Files.createDirectories(out);
            Properties summary=new Properties();summary.setProperty("players",""+players);summary.setProperty("difficulty",difficulty.name());
            summary.setProperty("seed",""+seed);summary.setProperty("hands",""+sequence.size());summary.setProperty("moves",""+moves);
            summary.setProperty("tricks",""+tricks);summary.setProperty("negativeScoreCheckpoints",""+negativeScores);
            summary.setProperty("forcedTrumpMoves",""+forcedTrumps);summary.setProperty("voidDiscardMoves",""+voidDiscards);
            summary.setProperty("totals",Arrays.toString(totals));summary.setProperty("status","PASS");
            try(var stream=Files.newOutputStream(out.resolve("game-"+players+"-"+difficulty+"-"+seed+".properties"))){summary.store(stream,"Actual offline model execution");}
            if(players==4&&difficulty==AiDifficulty.NORMAL&&seed==0) {
                trace.setProperty("step.count",""+steps);trace.setProperty("status","PASS");
                try(var stream=Files.newOutputStream(out.resolve("representative-game-trace.properties"))){trace.store(stream,"Actual game-core trace; no Android execution or images");}
            }
        }
    }

    private void record(String phase,String rules,String title,String expected,String actual,String context) {
        String key="step."+(steps++)+".";
        trace.setProperty(key+"phase",phase);trace.setProperty(key+"rules",rules);trace.setProperty(key+"title",title);
        trace.setProperty(key+"expected",expected);trace.setProperty(key+"actual",actual);trace.setProperty(key+"context",context);
        assertEquals(title,expected,actual);
    }
    private static List<Card> independentLegal(List<Card> hand,Suit led,Trump trump) {
        if(led==null)return new ArrayList<>(hand);
        List<Card> following=hand.stream().filter(c->c.suit()==led).toList();if(!following.isEmpty())return following;
        if(trump!=Trump.NO_TRUMP) {
            List<Card> cutting=hand.stream().filter(c->c.suit().ordinal()==trump.ordinal()).toList();
            if(!cutting.isEmpty())return cutting;
        }
        return new ArrayList<>(hand);
    }
    private static int independentWinner(List<PlayedCard> trick,Trump trump) {
        Suit led=trick.get(0).card().suit();PlayedCard best=trick.get(0);
        for(PlayedCard play:trick) {
            boolean candidateTrump=trump!=Trump.NO_TRUMP&&play.card().suit().ordinal()==trump.ordinal();
            boolean bestTrump=trump!=Trump.NO_TRUMP&&best.card().suit().ordinal()==trump.ordinal();
            if(candidateTrump&&!bestTrump || candidateTrump==bestTrump&&play.card().suit()==best.card().suit()
                    &&play.card().rank()>best.card().rank() || !candidateTrump&&!bestTrump&&best.card().suit()!=led&&play.card().suit()==led)
                best=play;
        }
        return best.seat();
    }
}

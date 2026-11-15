package ro.whistlab.core.rules;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Independent state machine: never calls GameRules for expected legal moves, winners or scores. */
@RunWith(Parameterized.class)
public class RomanianWhistModelTest extends atf.gradle.audit.GradleAuditedTest {
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
        GameSession game=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(players,GameMode.ROMANIAN_WHIST,difficulty,seed)));
        var checks = new ExhaustiveMatrixChecks();
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

            checks.at("hand " + (h + 1) + " / distribution");
            checks.equal("start.handIndex() must equal h", h,start.handIndex());checks.equal("start.handSize() must equal size", size,start.handSize());checks.equal("start.dealerSeat() must equal dealer", dealer,start.dealerSeat());
            checks.equal("start.mode() must equal GameMode.ROMANIAN_WHIST", GameMode.ROMANIAN_WHIST,start.mode());
            if(size==8){checks.equal("start.trump() must equal Trump.NO_TRUMP", Trump.NO_TRUMP,start.trump());checks.equal("turned card absent", null, start.turnedCard());}
            else {checks.equal("trump is declared", true, start.trump() != Trump.NO_TRUMP);checks.equal("turned card is present", true, start.turnedCard() != null);
                checks.equal("start.turnedCard().suit().ordinal() must equal start.trump().ordinal()", start.trump().ordinal(),start.turnedCard().suit().ordinal());}
            Set<Card> distinct=new HashSet<>();List<List<Card>> hands=new ArrayList<>();
            int minimum=new int[]{9,7,5,3}[players-3];
            for(var p:start.players()) {
                checks.equal("p.hand().size() must equal size", size,p.hand().size());checks.equal("bid absent before bidding", null, p.bid());checks.equal("p.tricksWon() must equal 0", 0,p.tricksWon());
                checks.equal("p.totalScore() must equal totals[p.seat()]", totals[p.seat()],p.totalScore());
                for(Card c:p.hand()){checks.equal("rank within declared deck", true, c.rank() >= minimum);checks.equal("dealt card is distinct", true, distinct.add(c));}
                hands.add(new ArrayList<>(p.hand()));
            }
            if(start.turnedCard()!=null)checks.equal("turned card is outside hands", false, distinct.contains(start.turnedCard()));
            record(checks, h==0?"SETUP":"TEST BODY","RW01/RW02","Hand "+(h+1)+" distribution",
                    "size="+size+"; dealer="+dealer+"; distinct="+(players*size),
                    "size="+start.handSize()+"; dealer="+start.dealerSeat()+"; distinct="+distinct.size(),
                    "trump="+start.trump()+"; initial hands="+hands);
            int[] bids=new int[players];int sum=0;
            for(int b=0;b<players;b++) {
                checks.at("hand " + (h + 1) + " / bid " + (b + 1));
                int seat=(dealer+1+b)%players;checks.equal("game.snapshot().biddingSeat() must equal seat", seat,game.snapshot().biddingSeat());
                int bid=AiPolicy.chooseBid(game.snapshot(),seat,difficulty);
                checks.equal("bid within hand size", true, bid >= 0 && bid <= size);
                if(b==players-1)checks.equal("last bid preserves the hook", false, sum + bid == size);
                game.submitBid(seat,bid);bids[seat]=bid;sum+=bid;
                checks.equal("GameSession.restore(game.saveState()).snapshot() must equal game.snapshot()", game.snapshot(),GameSession.restore(game.saveState()).snapshot());
            }
            record(checks, "TEST BODY","RW03","Hand "+(h+1)+" bids in seat order",
                    "phase=PLAYING; first="+((dealer+1)%players)+"; total differs from "+size,
                    "phase="+game.snapshot().phase()+"; first="+game.snapshot().currentSeat()+"; total differs from "+size,
                    "bids="+Arrays.toString(bids)+"; total="+sum);
            List<PlayedCard> trick=new ArrayList<>();int[] won=new int[players];int actor=(dealer+1)%players;
            for(int move=0;move<size*players;move++) {
                String moveLocation = "hand " + (h + 1) + " / move " + (move + 1) + " / seat " + actor;
                checks.at(moveLocation);
                var before=game.snapshot();checks.equal("before.phase() must equal GamePhase.PLAYING", GamePhase.PLAYING,before.phase());checks.equal("before.currentSeat() must equal actor", actor,before.currentSeat());
                Suit led=trick.isEmpty()?null:trick.get(0).card().suit();
                List<Card> legal=independentLegal(hands.get(actor),led,start.trump());
                Card choice=AiPolicy.chooseCard(before,actor,difficulty);
                if (!legal.contains(choice)) {
                    atf.gradle.audit.GradleInputs.fixture("Illegal card context", "held", hands.get(actor),
                            "led", led, "trump", start.trump(), "legal", legal, "choice", choice);
                }
                checks.equal("chosen card follows suit and mandatory trump", true, legal.contains(choice));
                if(led!=null&&hands.get(actor).stream().noneMatch(c->c.suit()==led)) {
                    if(start.trump().suit().filter(s->s==choice.suit()).isPresent())forcedTrumps++;else voidDiscards++;
                }
                String context="hand before="+hands.get(actor)+"; led="+led+"; trump="+start.trump()
                        +"; legal="+legal+"; seat="+actor+"; played="+choice;
                checks.equal("chosen card was held", true, hands.get(actor).remove(choice));trick.add(new PlayedCard(actor,choice));
                game.playCard(actor,choice);moves++;
                int winner=-1;
                if(trick.size()==players) {
                    winner=independentWinner(trick,start.trump());won[winner]++;actor=winner;tricks++;
                    checks.equal("game.snapshot().lastTrickWinnerSeat() must equal winner", winner,game.snapshot().lastTrickWinnerSeat());
                    checks.equal("game.snapshot().lastTrick() must equal trick", trick,game.snapshot().lastTrick());trick.clear();
                }else actor=(actor+1)%players;
                boolean scored=move+1==size*players;
                if(scored)for(int seat=0;seat<players;seat++)totals[seat]+=won[seat]==bids[seat]?5+won[seat]:-Math.abs(won[seat]-bids[seat]);
                var actual=game.snapshot();
                for(int seat=0;seat<players;seat++) {
                    checks.equal("new HashSet<>(actual.players().get(seat).hand()) must equal new HashSet<>(hands.get(seat))", new HashSet<>(hands.get(seat)),new HashSet<>(actual.players().get(seat).hand()));
                    checks.equal("actual.players().get(seat).tricksWon() must equal won[seat]", won[seat],actual.players().get(seat).tricksWon());
                    checks.equal("actual.players().get(seat).totalScore() must equal totals[seat]", totals[seat],actual.players().get(seat).totalScore());
                    checks.equal("actual.players().get(seat).bid().intValue() must equal bids[seat]", bids[seat],actual.players().get(seat).bid().intValue());
                }
                checks.equal("actual.currentTrick() must equal trick", trick,actual.currentTrick());
                if(!scored)checks.equal("actual.currentSeat() must equal actor", actor,actual.currentSeat());
                String expected="phase="+(scored?"SCORE":"PLAYING")+"; won="+Arrays.toString(won)+"; totals="+Arrays.toString(totals)
                        +(scored?"":"; next="+actor);
                String observed="phase="+actual.phase()+"; won="+actual.players().stream().map(GameSession.PlayerSnapshot::tricksWon).toList()
                        +"; totals="+actual.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList()
                        +(scored?"":"; next="+actual.currentSeat());
                record(checks, "TEST BODY",scored?"RW04/RW05/RW06":"RW04/RW05","Hand "+(h+1)+", move "+(move+1),expected,observed,
                        context+"; resolved winner="+winner);
                if(move%7==0||scored) {
                    game=GameSession.restore(game.saveState());checks.equal("game.snapshot() must equal actual", actual,game.snapshot());
                }
            }
            checks.at("hand " + (h + 1) + " / settlement");
            var score=game.snapshot();checks.equal("score.phase() must equal GamePhase.SCORE", GamePhase.SCORE,score.phase());
            checks.equal("Arrays.stream(won).sum() must equal size", size,Arrays.stream(won).sum());
            for(int total:totals)if(total<0)negativeScores++;
            boolean finalHand=h+1==sequence.size();checks.equal("score.finalHand() must equal finalHand", finalHand,score.finalHand());
            if(finalHand) {
                GameSession finished=game;
                assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,finished::startNextHand);checks.equal("game.snapshot() must equal score", score,game.snapshot());
            }else game.startNextHand();
        }
        checks.at("complete match");
        checks.equal("total moves", players * (10 * players + 54), moves);
        checks.equal("total tricks", 10 * players + 54, tricks);
        record(checks, "TEARDOWN","RW07","Complete game and immutable final score",
                "hands="+(3*players+12)+"; moves="+(players*(10*players+54))+"; tricks="+(10*players+54),
                "hands="+sequence.size()+"; moves="+moves+"; tricks="+tricks,"final totals="+Arrays.toString(totals));
        checks.publish();
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

    private void record(ExhaustiveMatrixChecks checks, String phase, String rules, String title,
                        String expected, String actual, String context) {
        checks.equal("milestone " + rules, expected, actual);
        if (players != 4 || difficulty != AiDifficulty.NORMAL || seed != 0) return;
        String key="step."+(steps++)+".";
        trace.setProperty(key+"phase",phase);trace.setProperty(key+"rules",rules);trace.setProperty(key+"title",title);
        trace.setProperty(key+"expected",expected);trace.setProperty(key+"actual",actual);trace.setProperty(key+"context",context);
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

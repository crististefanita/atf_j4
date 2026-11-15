package ro.whistlab.core.testing;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Every offline mode/table, audited with an independent list/card model through complete games. */
@RunWith(Parameterized.class)
public class OriginalVariantModelTest extends atf.gradle.audit.GradleAuditedTest {
    @Parameterized.Parameters(name="mode={0}, seats={1}, seriesReward={2}") public static Collection<Object[]> variants(){var rows=new ArrayList<Object[]>();for(GameMode m:GameMode.values())for(int n=3;n<=6;n++)for(int amount:m.isRomanian()?new int[]{0,5,10}:new int[]{0})rows.add(new Object[]{m,n,amount});return rows;}
    private final GameMode mode;private final int n,seriesReward;
    public OriginalVariantModelTest(GameMode mode,int n,int seriesReward){this.mode=mode;this.n=n;this.seriesReward=seriesReward;}
    @Test public void completeScheduleActorsLegalCardsWinnersScoresAndResumeMatchIndependentModel(){
        GameSession game=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(n,mode,AiDifficulty.NORMAL,87131,seriesReward!=0,seriesReward==0?5:seriesReward)));
        var checks = new ExhaustiveMatrixChecks();
        List<Integer> sizes=expectedSizes();int[] totals=new int[n],streaks=new int[n];int initialDealer=game.snapshot().dealerSeat();Random choices=new Random(73519);
        for(int h=0;h<sizes.size();h++){
            var start=game.snapshot();int size=sizes.get(h);
            checks.at("hand " + (h + 1) + " / distribution");
            checks.equal("start.handSize() must equal size", size,start.handSize());checks.equal("start.handIndex() must equal h", h,start.handIndex());checks.equal("start.dealerSeat() must equal (initialDealer + h) % n", (initialDealer+h)%n,start.dealerSeat());
            checks.equal("start.finalHand() must equal h == sizes.size() - 1", h==sizes.size()-1,start.finalHand());
            if(mode==GameMode.DESCENDING||mode==GameMode.DOWN_AND_UP)checks.equal("start.trump() must equal Trump.values()[h % 5]", Trump.values()[h%5],start.trump());
            else if(mode.isRomanian()&&size==8){checks.equal("start.trump() must equal Trump.NO_TRUMP", Trump.NO_TRUMP,start.trump());checks.equal("turned card absent", null, start.turnedCard());}
            else{checks.equal("turned card present", true, start.turnedCard() != null);checks.equal("start.turnedCard().suit().ordinal() must equal start.trump().ordinal()", start.trump().ordinal(),start.turnedCard().suit().ordinal());}
            var hands=new ArrayList<List<Card>>();Set<Card> unique=new HashSet<>();int minimum=mode.isRomanian()?15-2*n:2;
            for(var p:start.players()){checks.equal("p.hand().size() must equal size", size,p.hand().size());checks.equal("p.totalScore() must equal totals[p.seat()]", totals[p.seat()],p.totalScore());checks.equal("p.streak() must equal streaks[p.seat()]", streaks[p.seat()],p.streak());checks.equal("p.streakPoints() must equal 0", 0,p.streakPoints());for(Card c:p.hand()){checks.equal("minimum rank", true, c.rank() >= minimum);checks.equal("distinct cards", true, unique.add(c));}hands.add(new ArrayList<>(p.hand()));}
            int sum=0;int[] bids=new int[n],won=new int[n];
            for(int i=0;i<n;i++){checks.at("hand " + (h + 1) + " / bid " + (i + 1));int seat=(start.dealerSeat()+1+i)%n,bid=choices.nextInt(size+1);if(i==n-1&&sum+bid==size)bid=bid==0?1:bid-1;checks.equal("game.snapshot().biddingSeat() must equal seat", seat,game.snapshot().biddingSeat());game.submitBid(seat,bid);sum+=bid;bids[seat]=bid;}
            game=GameSession.restoreComplete(game.saveState());int actor=(start.dealerSeat()+1)%n;var trick=new ArrayList<PlayedCard>();
            for(int move=0;move<size*n;move++){
                checks.at("hand " + (h + 1) + " / move " + (move + 1) + " / seat " + actor);
                var before=game.snapshot();checks.equal("before.currentSeat() must equal actor", actor,before.currentSeat());checks.equal("before.phase() must equal GamePhase.PLAYING", GamePhase.PLAYING,before.phase());
                List<Card> allowed=legal(hands.get(actor),trick.isEmpty()?null:trick.get(0).card().suit(),start.trump());Card c=allowed.get(choices.nextInt(allowed.size()));
                checks.equal("played card was held", true, hands.get(actor).remove(c));trick.add(new PlayedCard(actor,c));game.playCard(actor,c);
                if(trick.size()==n){int winner=winner(trick,start.trump());won[winner]++;actor=winner;checks.equal("game.snapshot().lastTrickWinnerSeat() must equal winner", winner,game.snapshot().lastTrickWinnerSeat());checks.equal("game.snapshot().lastTrick() must equal trick", trick,game.snapshot().lastTrick());trick.clear();}else actor=(actor+1)%n;
                for(int s=0;s<n;s++){checks.equal("game.snapshot().players().get(s).hand() must equal hands.get(s)", hands.get(s),game.snapshot().players().get(s).hand());checks.equal("game.snapshot().players().get(s).tricksWon() must equal won[s]", won[s],game.snapshot().players().get(s).tricksWon());if(move+1<size*n)checks.equal("game.snapshot().players().get(s).totalScore() must equal totals[s]", totals[s],game.snapshot().players().get(s).totalScore());}
                if(move==size*n/2)game=GameSession.restoreComplete(game.saveState());
            }
            checks.at("hand " + (h + 1) + " / settlement");
            checks.equal("game.snapshot().phase() must equal GamePhase.SCORE", GamePhase.SCORE,game.snapshot().phase());checks.equal("Arrays.stream(won).sum() must equal size", size,Arrays.stream(won).sum());
            for(int s=0;s<n;s++){
                boolean exact=won[s]==bids[s];int delta=mode.isRomanian()?(exact?5+won[s]:-Math.abs(won[s]-bids[s])):won[s]+(exact?10:0),bonus=0;
                if(seriesReward!=0){int wins=Math.max(streaks[s],0),losses=Math.max(-streaks[s],0);if(exact){losses=0;if(size>1)wins++;}else{wins=0;if(size>1)losses++;}if(wins==5){bonus=seriesReward;wins=0;}if(losses==5){bonus=-5;losses=0;}streaks[s]=wins-losses;}
                checks.equal("game.scoreHistory().bid(h + 1, s) must equal Integer.valueOf(bids[s])", Integer.valueOf(bids[s]), game.scoreHistory().bid(h+1,s));
                checks.equal("game.scoreHistory().points(h + 1, s) must equal delta + bonus", delta+bonus, game.scoreHistory().points(h+1,s));
                totals[s]+=delta+bonus;checks.equal("game.snapshot().players().get(s).totalScore() must equal totals[s]", totals[s],game.snapshot().players().get(s).totalScore());checks.equal("game.snapshot().players().get(s).streak() must equal streaks[s]", streaks[s],game.snapshot().players().get(s).streak());checks.equal("game.snapshot().players().get(s).streakPoints() must equal bonus", bonus,game.snapshot().players().get(s).streakPoints());
            }
            var finalScore=game.snapshot();game=GameSession.restoreComplete(game.saveState());checks.equal("game.snapshot() must equal finalScore", finalScore,game.snapshot());
            for(int seat=0;seat<n;seat++){
                checks.equal("game.scoreHistory().bid(h + 1, seat) must equal Integer.valueOf(bids[seat])", Integer.valueOf(bids[seat]),game.scoreHistory().bid(h+1,seat));
                checks.equal("game.scoreHistory().total(h + 1, seat) must equal totals[seat]", totals[seat],game.scoreHistory().total(h+1,seat));
            }
            if(h+1<sizes.size())game.startNextHand();else {GameSession finished=game;assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,finished::startNextHand);}
        }
        checks.publish();
    }
    private List<Integer> expectedSizes(){
        var r=new ArrayList<Integer>();
        if(mode.isRomanian()){
            int first=mode==GameMode.LONG_ROMANIAN?8:1,last=9-first,step=first==1?1:-1;
            for(int i=0;i<n;i++)r.add(first);for(int size=first+step;size!=last;size+=step)r.add(size);for(int i=0;i<n;i++)r.add(last);for(int size=last-step;size!=first;size-=step)r.add(size);for(int i=0;i<n;i++)r.add(first);return r;
        }
        int max=mode==GameMode.OH_HELL||mode==GameMode.REVERSE_OH_HELL?(n==6?8:10):52/n;
        if(mode==GameMode.REVERSE_OH_HELL){for(int i=1;i<=max;i++)r.add(i);for(int i=max-1;i>=1;i--)r.add(i);return r;}
        for(int i=max;i>=1;i--)r.add(i);if(mode==GameMode.OH_HELL||mode==GameMode.DOWN_AND_UP)for(int i=2;i<=max;i++)r.add(i);return r;
    }
    private List<Card> legal(List<Card> hand,Suit led,Trump trump){if(led==null)return new ArrayList<>(hand);var r=new ArrayList<Card>();for(Card c:hand)if(c.suit()==led)r.add(c);if(!r.isEmpty())return r;if(mode.isRomanian())for(Card c:hand)if(c.suit().ordinal()==trump.ordinal())r.add(c);return r.isEmpty()?new ArrayList<>(hand):r;}
    private int winner(List<PlayedCard> trick,Trump trump){PlayedCard best=trick.get(0);Suit led=best.card().suit();for(PlayedCard p:trick)if(power(p.card(),led,trump)>power(best.card(),led,trump))best=p;return best.seat();}
    private int power(Card c,Suit led,Trump trump){return (c.suit().ordinal()==trump.ordinal()?200:c.suit()==led?100:0)+c.rank();}
}

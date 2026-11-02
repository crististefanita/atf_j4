package ro.whistlab.core;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.core.baseline.LegacyAiPolicy;

/** Prespecified paired adversarial evaluation. This is not proof of perfect hidden-information play. */
public class AiTrainingTournamentTest {
    enum Player { NEW, OLD, CHALLENGER }
    private static final String VARIANT=System.getProperty("whist.aiCandidate","FULL");
    private static final int SERIES_REWARD=Integer.getInteger("whist.streakBonus",0);
    record Result(int hero,int exact,double winShare,int[] totals,long maxDecisionNanos) { }
    @Test public void pairedSameDealSameSeatGamesCompareWithFrozenExpertAndIndependentChallenger()throws Exception {
        boolean held="heldout".equals(System.getProperty("whist.aiTrainingPhase","development"));int first=SERIES_REWARD==0?(held?40000:200):(held?50000:500);
        int seeds=Integer.getInteger("whist.aiTrainingSeeds",held?16:8);Properties p=new Properties();int pair=0;long oldPoints=0,newPoints=0;double oldWins=0,newWins=0;boolean passed=true;
        p.setProperty("profile",SERIES_REWARD==0?"Classic Romanian, no series":"Romanian five-hand positive series +"+SERIES_REWARD+", missed series -5");
        for(Player opponent:List.of(Player.OLD,Player.CHALLENGER)){
            long familyOld=0,familyNew=0;double familyOldWins=0,familyNewWins=0;
            for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++){
                Result before=play(seed,seat,Player.OLD,opponent),after=play(seed,seat,Player.NEW,opponent);String k="pair."+(pair++)+".";
                p.setProperty(k+"seed",""+seed);p.setProperty(k+"seat",""+seat);p.setProperty(k+"opponents",opponent.name());p.setProperty(k+"oldScore",""+before.hero);p.setProperty(k+"newScore",""+after.hero);
                p.setProperty(k+"oldTotals",Arrays.toString(before.totals));p.setProperty(k+"newTotals",Arrays.toString(after.totals));p.setProperty(k+"oldExact",""+before.exact);p.setProperty(k+"newExact",""+after.exact);
                p.setProperty(k+"oldWinShare",""+before.winShare);p.setProperty(k+"newWinShare",""+after.winShare);p.setProperty(k+"newMaxAnyActorDecisionMillis",""+after.maxDecisionNanos/1e6);
                familyOld+=before.hero;familyNew+=after.hero;familyOldWins+=before.winShare;familyNewWins+=after.winShare;
                p.setProperty("pairsCompleted",""+pair);p.setProperty("status","RUNNING");save(p,held);
            }
            p.setProperty("family."+opponent+".oldScore",""+familyOld);p.setProperty("family."+opponent+".newScore",""+familyNew);p.setProperty("family."+opponent+".oldWins",""+familyOldWins);p.setProperty("family."+opponent+".newWins",""+familyNewWins);
            passed&=familyNew>=familyOld;oldPoints+=familyOld;newPoints+=familyNew;oldWins+=familyOldWins;newWins+=familyNewWins;
        }
        p.setProperty("variant",VARIANT);p.setProperty("phase",held?"heldout":"development");p.setProperty("seeds",first+".."+(first+seeds-1));p.setProperty("pairs",""+pair);p.setProperty("oldPoints",""+oldPoints);p.setProperty("newPoints",""+newPoints);p.setProperty("oldWins",""+oldWins);p.setProperty("newWins",""+newWins);p.setProperty("status",passed?"PASS":"REGRESSION");save(p,held);
        assertTrue("New Expert regressed against at least one declared opponent family; retained every game",passed);
    }
    private static void save(Properties p,boolean held)throws Exception{String out=System.getProperty("whist.componentEvidence","");if(!out.isBlank()){Files.createDirectories(Path.of(out));try(var s=Files.newOutputStream(Path.of(out,held?"ai-heldout.properties":"ai-development.properties"))){p.store(s,"Paired same-deal same-seat normal games; all results retained");}}}
    static IndependentPlayer.Position publicPosition(GameSession.Snapshot s,int seat){return new IndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(AiTrainingTournamentTest::id).toList(),s.players().stream().map(p->new IndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new IndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static Result play(long seed,int hero,Player policy,Player opponent){
        GameSession g=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,SERIES_REWARD!=0,SERIES_REWARD==0?5:SERIES_REWARD));int exact=0;long max=0;
        while(true){
            while(g.snapshot().phase()==GamePhase.BIDDING){var s=g.snapshot();int seat=s.biddingSeat();Player p=seat==hero?policy:opponent;long at=System.nanoTime();int bid=p==Player.NEW&&!VARIANT.equals("CARD_ONLY")?AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT):p==Player.OLD||p==Player.NEW?LegacyAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT):IndependentPlayer.bid(publicPosition(s,seat)).value();max=Math.max(max,System.nanoTime()-at);g.submitBid(seat,bid);}
            while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();int seat=s.currentSeat();Player p=seat==hero?policy:opponent;long at=System.nanoTime();Card c;
                if(p==Player.NEW&&!VARIANT.equals("BID_ONLY"))c=AiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);else if(p==Player.OLD||p==Player.NEW)c=LegacyAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);else {int id=IndependentPlayer.card(publicPosition(s,seat)).value();c=new Card(Suit.values()[id/13],id%13+2);}
                max=Math.max(max,System.nanoTime()-at);var own=publicPosition(s,seat);assertTrue(IndependentPlayer.legal(own.hand(),own.trick().isEmpty()?-1:own.trick().get(0).card()/13,own.trump()).contains(id(c)));g.playCard(seat,c);
            }
            var s=g.snapshot();var h=s.players().get(hero);if(h.bid()==h.tricksWon())exact++;
            if(s.finalHand()){int[] totals=s.players().stream().mapToInt(GameSession.PlayerSnapshot::totalScore).toArray();int best=Arrays.stream(totals).max().orElseThrow(),ties=0;for(int t:totals)if(t==best)ties++;return new Result(totals[hero],exact,totals[hero]==best?1.0/ties:0,totals,max);}g.startNextHand();
        }
    }
}

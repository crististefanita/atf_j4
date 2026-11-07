package ro.whistlab.core.ai.engine;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class ContractEndgameTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void exactWorldSolverMatchesIndependentMaxNOnFiniteFourSeatEndgames(){
        Random r=new Random(77291);int compared=0;
        for(int t=0;t<60;t++){
            int size=t<8?3:2;var deck=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4));Collections.shuffle(deck,r);var hands=new ArrayList<List<Card>>();for(int s=0;s<4;s++)hands.add(new ArrayList<>(deck.subList(s*size,s*size+size)));
            int[] bids=new int[4];for(int s=0;s<4;s++)bids[s]=r.nextInt(size+1);Trump trump=Trump.values()[t%5];
            var seats=new ArrayList<MonteCarloAi.Seat>();for(int s=0;s<4;s++)seats.add(new MonteCarloAi.Seat(size,bids[s],0,s*2));
            var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,trump,0,3,size,hands.get(0),seats,List.of(),List.of());
            for(Card first:hands.get(0)){
                int[] actual=ContractEndgame.after(view,hands,first,bids);var numeric=new ArrayList<List<Integer>>();for(var h:hands)numeric.add(new ArrayList<>(h.stream().map(c->13*c.suit().ordinal()+c.rank()-2).toList()));
                int id=13*first.suit().ordinal()+first.rank()-2;numeric.get(0).remove(Integer.valueOf(id));
                int[] expected=reference(numeric,new int[4],bids,1,List.of(id),0,trump.ordinal());
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actual", actual, "bids", bids, "compared", compared, "deck", deck, "expected", expected, "first", first, "hands", hands, "id", id, "numeric", numeric, "r", r, "seats", seats, "size", size, "t", t, "trump", trump, "view", view);
assertArrayEquals("world "+t+" first "+first,expected,actual);compared++;
            }
        }assertEquals("compared must equal 128", 128,compared);
    }
    private static int[] reference(List<List<Integer>> h,int[] won,int[] bids,int actor,List<Integer> trick,int leadSeat,int trump){
        if(h.stream().allMatch(List::isEmpty))return won.clone();
        int led=trick.isEmpty()?-1:trick.get(0)/13;var legal=new ArrayList<Integer>();
        for(int c:h.get(actor))if(led<0||c/13==led)legal.add(c);
        if(legal.isEmpty()&&trump<4)for(int c:h.get(actor))if(c/13==trump)legal.add(c);
        if(legal.isEmpty())legal.addAll(h.get(actor));Collections.sort(legal);int[] best=null;
        for(int c:legal){var next=new ArrayList<List<Integer>>();for(var hand:h)next.add(new ArrayList<>(hand));next.get(actor).remove(Integer.valueOf(c));var round=new ArrayList<>(trick);round.add(c);int[] value;
            if(round.size()==4){int winner=0;for(int i=1;i<4;i++)if(power(round.get(i),round.get(0)/13,trump)>power(round.get(winner),round.get(0)/13,trump))winner=i;winner=(leadSeat+winner)%4;var wins=won.clone();wins[winner]++;value=reference(next,wins,bids,winner,List.of(),winner,trump);}
            else value=reference(next,won,bids,(actor+1)%4,round,leadSeat,trump);
            if(best==null||score(value[actor],bids[actor])>score(best[actor],bids[actor])||score(value[actor],bids[actor])==score(best[actor],bids[actor])&&rival(value,bids,actor)<rival(best,bids,actor))best=value;
        }return best;
    }
    private static int score(int w,int b){return w==b?5+w:-Math.abs(w-b);}
    private static int rival(int[] w,int[] b,int actor){int best=-999;for(int s=0;s<4;s++)if(s!=actor)best=Math.max(best,s*2+score(w[s],b[s]));return best;}
    private static int power(int c,int led,int trump){return (c/13==trump?200:c/13==led?100:0)+c%13;}
    @Test public void constrainedDealsAlwaysRespectKnownVoidsAndAllCardCounts(){
        GameSession g=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,31)));
        while(g.snapshot().handSize()!=8){finish(g);g.startNextHand();}while(g.snapshot().phase()==GamePhase.BIDDING){var s=g.snapshot();g.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.HARD));}
        while(g.snapshot().history().size()<24){var s=g.snapshot();g.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.HARD));}
        var v=MonteCarloAi.observe(g.snapshot(),g.snapshot().currentSeat());Set<Card> forbidden=new HashSet<>(v.hand());for(var p:v.history())forbidden.add(p.card());
        for(int sample=0;sample<100;sample++){var hands=MonteCarloAi.sample(v,new Random(sample));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "forbidden", forbidden, "g", g, "hands", hands, "sample", sample, "v", v);
assertNotNull("hands is available", hands);Set<Card> all=new HashSet<>();for(int s=0;s<4;s++){assertEquals("hands.get(s).size() must equal v.seats().get(s).remaining()", v.seats().get(s).remaining(),hands.get(s).size());for(Card c:hands.get(s)){assertTrue("all.add(c) must hold", all.add(c));if(s!=v.seat())assertFalse("forbidden.contains(c) must remain false", forbidden.contains(c));}}
            for(int i=0;i<v.history().size();i++){var p=v.history().get(i);Suit led=v.history().get(i-i%4).card().suit();if(p.card().suit()!=led){assertTrue("hands.get(p.seat()).stream().noneMatch((c)->c.suit() == led) must hold", hands.get(p.seat()).stream().noneMatch(c->c.suit()==led));}}
        }
    }
    private static void finish(GameSession g){while(g.snapshot().phase()==GamePhase.BIDDING){var s=g.snapshot();g.submitBid(s.biddingSeat(),AiPolicy.chooseBid(s,s.biddingSeat(),AiDifficulty.HARD));}while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();g.playCard(s.currentSeat(),AiPolicy.chooseCard(s,s.currentSeat(),AiDifficulty.HARD));}}
    @Test public void aRomanianDiscardProvesBothLedSuitAndTrumpAreMissing(){
        var history=List.of(new PlayedCard(0,new Card(Suit.HEARTS,7)),new PlayedCard(1,new Card(Suit.CLUBS,7)),new PlayedCard(2,new Card(Suit.HEARTS,8)),new PlayedCard(3,new Card(Suit.SPADES,7)),new PlayedCard(3,new Card(Suit.SPADES,9)));
        var seats=List.of(new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(1,0,1));
        var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,3,3,List.of(new Card(Suit.CLUBS,14),new Card(Suit.DIAMONDS,14)),seats,List.of(history.get(4)),history,new Card(Suit.SPADES,8));
        for(int i=0;i<100;i++){var world=MonteCarloAi.sample(view,new Random(i));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "i", i, "seats", seats, "view", view, "world", world);
assertNotNull("world is available", world);assertTrue("world.get(1).stream().noneMatch((c)->c.suit() == Suit.HEARTS || c.suit() == Suit.SPADES) must hold", world.get(1).stream().noneMatch(c->c.suit()==Suit.HEARTS||c.suit()==Suit.SPADES));assertTrue("world.get(3).stream().noneMatch((c)->c.suit() == Suit.HEARTS) must hold", world.get(3).stream().noneMatch(c->c.suit()==Suit.HEARTS));assertEquals("world.get(3).size() must equal 1", 1,world.get(3).size());}
    }
    @Test public void currentTrickDiscardConstrainsHiddenHandsBeforeTheTrickIsScored(){
        List<PlayedCard> history=new ArrayList<>();
        List<List<Integer>> rankGroups=List.of(List.of(7,8,9,10),List.of(7,8,9,10),
                List.of(7,8,9,10),List.of(7,8,9,10),List.of(11,12,13,14),List.of(11,12,13,14));
        List<Suit> leadSuits=List.of(Suit.HEARTS,Suit.CLUBS,Suit.SPADES,Suit.DIAMONDS,Suit.SPADES,Suit.DIAMONDS);
        for(int trick=0;trick<leadSuits.size();trick++)for(int seat=0;seat<4;seat++)
            history.add(new PlayedCard(seat,new Card(leadSuits.get(trick),rankGroups.get(trick).get(seat))));
        List<PlayedCard> current=List.of(new PlayedCard(0,new Card(Suit.HEARTS,11)),
                new PlayedCard(1,new Card(Suit.CLUBS,11)));
        List<MonteCarloAi.Seat> seats=List.of(new MonteCarloAi.Seat(1,0,0),new MonteCarloAi.Seat(1,0,0),
                new MonteCarloAi.Seat(2,0,0),new MonteCarloAi.Seat(2,0,0));
        var view=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.NO_TRUMP,2,3,8,
                List.of(new Card(Suit.HEARTS,12),new Card(Suit.CLUBS,12)),seats,current,history,null);
        for(int seed=0;seed<100;seed++){
            var world=MonteCarloAi.sample(view,new Random(seed));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "current", current, "history", history, "leadSuits", leadSuits, "rankGroups", rankGroups, "seats", seats, "seed", seed, "view", view, "world", world);
assertNotNull("world is available", world);
            assertTrue("seat 1 discarded while Hearts were led; no Heart can remain",
                    world.get(1).stream().noneMatch(card->card.suit()==Suit.HEARTS));
        }
    }
}

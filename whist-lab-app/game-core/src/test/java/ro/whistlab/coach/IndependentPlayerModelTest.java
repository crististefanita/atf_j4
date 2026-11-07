package ro.whistlab.coach;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.*;
import org.junit.Test;

public class IndependentPlayerModelTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void hallFeasibilityMatchesExhaustiveAllocationOnTwoThousandSmallConstrainedWorlds(){
        var random=new Random(72021);
        for(int k=0;k<2000;k++){int n=3+random.nextInt(4);int[] need=new int[n],counts=new int[4];boolean[][] voids=new boolean[n][4];
            for(int s=0;s<n;s++){need[s]=random.nextInt(3);for(int suit=0;suit<4;suit++)voids[s][suit]=random.nextBoolean();}
            for(int suit=0;suit<4;suit++)counts[suit]=random.nextInt(3);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "counts", counts, "k", k, "n", n, "need", need, "random", random, "voids", voids);
            assertEquals("case="+k,brute(need.clone(),counts.clone(),voids,0),IndependentPlayer.feasible(need,counts,voids));
        }
        assertFalse("IndependentPlayer.feasible(new int[]{-1}, new int[4], new boolean[1][4]) must remain false", IndependentPlayer.feasible(new int[]{-1},new int[4],new boolean[1][4]));
        assertFalse("IndependentPlayer.feasible(new int[]{0}, new int[]{-1, 0, 0, 0}, new boolean[1][4]) must remain false", IndependentPlayer.feasible(new int[]{0},new int[]{-1,0,0,0},new boolean[1][4]));
    }
    private static boolean brute(int[] need,int[] counts,boolean[][] voids,int seat){
        if(seat==need.length)return true;if(need[seat]==0)return brute(need,counts,voids,seat+1);
        for(int suit=0;suit<4;suit++)if(counts[suit]>0&&!voids[seat][suit]){need[seat]--;counts[suit]--;boolean ok=brute(need,counts,voids,seat);counts[suit]++;need[seat]++;if(ok)return true;}return false;
    }
    @Test public void oneCardProbabilityMatchesEveryOrderedOpponentDealForLeaderFollowerAndTrump(){
        for(int seat=0;seat<4;seat++)for(int trump:new int[]{0,1,4})for(int own:List.of(5,9,12,18,25,38,51)){
            int turned=trump==4?-1:trump*13+6;if(turned==own)turned=trump*13+7;
            var p=new IndependentPlayer.Position(seat,3,1,trump,List.of(own),seats(1),List.of(),List.of(),turned);
            var pool=new ArrayList<Integer>();for(int s=0;s<4;s++)for(int r=5;r<13;r++){int c=s*13+r;if(c!=own&&c!=turned)pool.add(c);}
            int[] wins={0,0};int[] cards=new int[4];cards[seat]=own;enumerate(pool,cards,seat,0,trump,wins);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "cards", cards, "own", own, "p", p, "pool", pool, "seat", seat, "trump", trump, "turned", turned, "wins", wins);
            assertEquals(wins[0]/(double)wins[1],IndependentPlayer.oneCardWinChance(p),1e-12);
        }
    }
    private static void enumerate(List<Integer> pool,int[] cards,int own,int seat,int trump,int[] count){
        if(seat==4){int best=0,led=cards[0]/13;for(int s=1;s<4;s++)if(power(cards[s],led,trump)>power(cards[best],led,trump))best=s;count[1]++;if(best==own)count[0]++;return;}
        if(seat==own){enumerate(pool,cards,own,seat+1,trump,count);return;}
        for(int i=0;i<pool.size();i++){int c=pool.remove(i);cards[seat]=c;enumerate(pool,cards,own,seat+1,trump,count);pool.add(i,c);}
    }
    private static int power(int card,int led,int trump){return (card/13==trump?200:card/13==led?100:0)+card%13;}
    private static List<IndependentPlayer.Seat> seats(int remaining){return List.of(new IndependentPlayer.Seat(remaining,null,0,0),new IndependentPlayer.Seat(remaining,null,0,0),new IndependentPlayer.Seat(remaining,null,0,0),new IndependentPlayer.Seat(remaining,null,0,0));}
    @Test public void lastBidNeverChoosesForbiddenContractEvenWithCertainOneCardWin(){
        var players=new ArrayList<>(seats(1));for(int s=0;s<3;s++)players.set(s,new IndependentPlayer.Seat(1,0,0,0));
        var p=new IndependentPlayer.Position(3,3,1,0,List.of(12),players,List.of(),List.of(),5);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "p", p, "players", players);
        assertEquals("IndependentPlayer.bid(p).value() must equal 0", 0,IndependentPlayer.bid(p).value());
    }
    @Test public void constrainedSamplerKeepsCardOwnershipCountsAndVoidEvidenceAcrossAllRandomDraws(){
        // Seat1 publicly discarded a diamond on a heart lead: both hearts and trumps are exhausted there.
        var history=List.of(new IndependentPlayer.Play(0,18),new IndependentPlayer.Play(1,44),new IndependentPlayer.Play(2,19),new IndependentPlayer.Play(3,20));
        var p=new IndependentPlayer.Position(0,3,2,0,List.of(25),seats(1),List.of(),history,5);
        for(int seed=0;seed<200;seed++)for(boolean hallOnly:List.of(false,true)){var hands=hallOnly?IndependentPlayer.feasibleDeal(p,new Random(seed)):IndependentPlayer.deal(p,new Random(seed));
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "hallOnly", hallOnly, "hands", hands, "history", history, "p", p, "seed", seed);
assertNotNull("hands is available", hands);var all=new HashSet<Integer>();
            for(int s=0;s<4;s++){assertEquals("hands.get(s).size() must equal 1", 1,hands.get(s).size());for(int c:hands.get(s)){assertTrue("all.add(c) must hold", all.add(c));assertNotEquals("c must differ from 5", 5,c);assertFalse("history.stream().anyMatch((x)->x.card() == c) must remain false", history.stream().anyMatch(x->x.card()==c));if(s==1){assertNotEquals("c / 13 must differ from 0", 0,c/13);assertNotEquals("c / 13 must differ from 1", 1,c/13);}}}
            assertEquals("hands.get(0) must equal p.hand()", p.hand(),hands.get(0));
        }
    }
    @Test public void currentTrickDiscardConstrainsIndependentWorldsEvenWhenHistoryOmitsItsSuffix(){
        var partial=List.of(new IndependentPlayer.Play(0,18),new IndependentPlayer.Play(1,44));
        var offSuit=new IndependentPlayer.Position(2,3,2,0,List.of(31),seats(1),partial,List.of(),5);
        var voids=IndependentPlayer.knownVoids(offSuit);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "offSuit", offSuit, "partial", partial, "voids", voids);
        assertTrue("voids[1][1] must hold", voids[1][1]);assertTrue("voids[1][0] must hold", voids[1][0]);
        for(int seed=0;seed<96;seed++)for(boolean hallOnly:List.of(false,true)){
            var hands=hallOnly?IndependentPlayer.feasibleDeal(offSuit,new Random(seed)):IndependentPlayer.deal(offSuit,new Random(seed));
            assertNotNull("seed="+seed+" hallOnly="+hallOnly,hands);
            assertTrue("hands.get(1).stream().noneMatch((c)->c / 13 == 0 || c / 13 == 1) must hold", hands.get(1).stream().noneMatch(c->c/13==0||c/13==1));
        }
        var forcedTrump=new IndependentPlayer.Position(2,3,2,0,List.of(31),seats(1),
                List.of(new IndependentPlayer.Play(0,18),new IndependentPlayer.Play(1,5)),List.of(),6);
        var trumpVoids=IndependentPlayer.knownVoids(forcedTrump);
        assertTrue("trumpVoids[1][1] must hold", trumpVoids[1][1]);assertFalse("trumpVoids[1][0] must remain false", trumpVoids[1][0]);
        boolean canStillHoldTrump=false;
        for(int seed=0;seed<96;seed++)canStillHoldTrump|=IndependentPlayer.deal(forcedTrump,new Random(seed)).get(1).stream().anyMatch(c->c/13==0);
        assertTrue("A forced trump proves the led-suit void, not a trump void",canStillHoldTrump);
    }
    @Test public void independentCardOrderEnumeratesEverySmallPubliclyFeasibleWorld(){
        // Eight-card no-trump hand: after six completed tricks only six hidden cards remain.
        // Public voids constrain 19 legal assignments across clubs, diamonds and spades.
        var history=List.of(
                new IndependentPlayer.Play(0,5),new IndependentPlayer.Play(1,18),new IndependentPlayer.Play(2,6),new IndependentPlayer.Play(3,7),
                new IndependentPlayer.Play(0,31),new IndependentPlayer.Play(1,32),new IndependentPlayer.Play(2,33),new IndependentPlayer.Play(3,44),
                new IndependentPlayer.Play(0,45),new IndependentPlayer.Play(1,19),new IndependentPlayer.Play(2,8),new IndependentPlayer.Play(3,46),
                new IndependentPlayer.Play(0,47),new IndependentPlayer.Play(1,20),new IndependentPlayer.Play(2,9),new IndependentPlayer.Play(3,48),
                new IndependentPlayer.Play(0,49),new IndependentPlayer.Play(1,21),new IndependentPlayer.Play(2,34),new IndependentPlayer.Play(3,50),
                new IndependentPlayer.Play(0,51),new IndependentPlayer.Play(1,22),new IndependentPlayer.Play(2,23),new IndependentPlayer.Play(3,10));
        var seats=List.of(new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0),
                new IndependentPlayer.Seat(2,0,0,0),new IndependentPlayer.Seat(2,0,0,0));
        var p=new IndependentPlayer.Position(0,3,8,4,List.of(35,36),seats,List.of(),history,-1);
        var worlds=IndependentPlayer.smallDeals(p);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "p", p, "seats", seats, "worlds", worlds);
        assertEquals("worlds.size() must equal 19", 19,worlds.size());
        for(var world:worlds){assertEquals("world.get(1).size() must equal 2", 2,world.get(1).size());assertEquals("world.get(2).size() must equal 2", 2,world.get(2).size());assertEquals("world.get(3).size() must equal 2", 2,world.get(3).size());
            assertTrue("world.get(1).stream().noneMatch((c)->c / 13 == 0 || c / 13 == 3) must hold", world.get(1).stream().noneMatch(c->c/13==0||c/13==3));
            assertTrue("world.get(3).stream().noneMatch((c)->c / 13 == 2 || c / 13 == 3) must hold", world.get(3).stream().noneMatch(c->c/13==2||c/13==3));}
    }
    @Test public void twoCardSolverMatchesAnOracleUsingRealEngineTransitionsAndTerminalScores(){
        var random=new Random(729);for(int k=0;k<24;k++){
            var deck=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4));Collections.shuffle(deck,random);var deal=new ArrayList<List<Card>>();for(int s=0;s<4;s++)deal.add(new ArrayList<>(deck.subList(2*s,2*s+2)));
            var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,k),deal,Trump.values()[k%5],3);
            while(game.snapshot().phase()==GamePhase.BIDDING){var state=game.snapshot();var allowed=GameRules.allowedBids(state.handSize(),state.players().stream().filter(p->p.bid()!=null).mapToInt(p->p.bid()).sum(),state.biddingSeat()==state.dealerSeat());game.submitBid(state.biddingSeat(),allowed.get(random.nextInt(allowed.size())));}
            var s=game.snapshot();var p=new IndependentPlayer.Position(0,3,2,k%5,s.players().get(0).hand().stream().map(IndependentPlayerModelTest::id).toList(),s.players().stream().map(x->new IndependentPlayer.Seat(2,x.bid(),0,0)).toList(),List.of(),List.of(),-1);
            var world=s.players().stream().map(x->x.hand().stream().map(IndependentPlayerModelTest::id).toList()).toList();int[] goals=s.players().stream().mapToInt(GameSession.PlayerSnapshot::bid).toArray();
            for(Card first:s.players().get(0).hand()){var branch=copyGame(game);branch.playCard(0,first);
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "branch", branch, "deal", deal, "deck", deck, "first", first, "game", game, "goals", goals, "k", k, "p", p, "random", random, "s", s, "world", world);
assertArrayEquals("world="+k+" first="+first,oracle(branch),IndependentPlayer.exactAfter(p,world,id(first),goals));}
        }
    }
    @Test public void feasibleLateGameIsNotLostWhenEveryRejectionAttemptTakesTheWrongSuit()throws Exception{
        // Last two cards: our diamonds, unrestricted seat1 clubs, and seats2/3 spades/hearts.
        var actual=List.of(List.of(card(3,7),card(3,8),card(3,14),card(3,13),card(3,12),card(2,9),card(2,10),card(2,11)),List.of(card(2,7),card(2,8),card(2,14),card(2,13),card(2,12),card(3,9),card(3,10),card(3,11)),List.of(card(0,7),card(0,8),card(0,9),card(0,10),card(0,11),card(0,12),card(0,13),card(0,14)),List.of(card(1,7),card(1,8),card(1,9),card(1,10),card(1,11),card(1,12),card(1,13),card(1,14)));
        var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,0),actual,Trump.NO_TRUMP,3);for(int seat=0;seat<4;seat++)game.submitBid(seat,0);
        int[][] rounds={{0,2,9,1,2,14,2,0,9,3,1,9},{1,3,9,2,0,10,3,1,10,0,3,14},{0,2,10,1,2,13,2,0,11,3,1,11},{1,3,10,2,0,12,3,1,12,0,3,13},{0,2,11,1,2,12,2,0,13,3,1,13},{1,3,11,2,0,14,3,1,14,0,3,12}};
        for(int[] round:rounds)for(int i=0;i<12;i+=3)game.playCard(round[i],card(round[i+1],round[i+2]));var s=game.snapshot();
        var p=new IndependentPlayer.Position(0,3,8,4,s.players().get(0).hand().stream().map(IndependentPlayerModelTest::id).toList(),s.players().stream().map(x->new IndependentPlayer.Seat(x.hand().size(),x.bid(),x.tricksWon(),x.totalScore())).toList(),List.of(),s.history().stream().map(x->new IndependentPlayer.Play(x.seat(),id(x.card()))).toList(),-1);
        var old=new ro.whistlab.coach.baseline.PreviousIndependentPlayer.Position(p.seat(),p.dealer(),p.size(),p.trump(),p.hand(),p.seats().stream().map(x->new ro.whistlab.coach.baseline.PreviousIndependentPlayer.Seat(x.remaining(),x.bid(),x.won(),x.total())).toList(),List.of(),p.history().stream().map(x->new ro.whistlab.coach.baseline.PreviousIndependentPlayer.Play(x.seat(),x.card())).toList(),-1,false,5);
        var method=ro.whistlab.coach.baseline.PreviousIndependentPlayer.class.getDeclaredMethod("deal",old.getClass(),Random.class);method.setAccessible(true);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actual", actual, "game", game, "method", method, "old", old, "p", p, "rounds", rounds, "s", s);
        assertNull("method.invoke(null, old, identityShuffle()) remains absent", method.invoke(null,old,identityShuffle()));var rescued=IndependentPlayer.deal(p,identityShuffle());assertNotNull("rescued is available", rescued);assertEquals("rescued.get(1).size() must equal 2", 2,rescued.get(1).size());assertTrue("rescued.get(1).stream().allMatch((c)->c / 13 == 2) must hold", rescued.get(1).stream().allMatch(c->c/13==2));
    }
    private static Random identityShuffle(){return new Random(){@Override public int nextInt(int bound){return bound-1;}};}
    private static Card card(int suit,int rank){return new Card(Suit.values()[suit],rank);}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static int[] oracle(GameSession game){
        var s=game.snapshot();if(s.phase()!=GamePhase.PLAYING)return s.players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).toArray();
        int actor=s.currentSeat();var choices=new ArrayList<>(GameRules.legalCards(s.players().get(actor).hand(),s.currentTrick().isEmpty()?null:s.currentTrick().get(0).card().suit(),s.trump(),s.mode()));choices.sort(Comparator.comparingInt(IndependentPlayerModelTest::id));int[] best=null;
        for(Card c:choices){var branch=copyGame(game);branch.playCard(actor,c);int[] value=oracle(branch);if(best==null||valueScore(value,s,actor)>valueScore(best,s,actor)||valueScore(value,s,actor)==valueScore(best,s,actor)&&rivalScore(value,s,actor)<rivalScore(best,s,actor))best=value;}return best;
    }
    private static GameSession copyGame(GameSession game){
        var s=game.snapshot();var deal=new ArrayList<List<Card>>();for(var p:s.players())deal.add(new ArrayList<>(p.hand()));for(var play:s.history())deal.get(play.seat()).add(play.card());
        var copy=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,0),deal,s.trump(),s.dealerSeat());
        for(int i=1;i<=4;i++){int seat=(s.dealerSeat()+i)%4;copy.submitBid(seat,s.players().get(seat).bid());}for(var play:s.history())copy.playCard(play.seat(),play.card());return copy;
    }
    private static int valueScore(int[] value,GameSession.Snapshot s,int seat){return GameRules.scoreDelta(value[seat],s.players().get(seat).bid(),GameMode.ROMANIAN_WHIST);}
    private static int rivalScore(int[] value,GameSession.Snapshot s,int actor){int best=Integer.MIN_VALUE;for(int seat=0;seat<4;seat++)if(seat!=actor)best=Math.max(best,valueScore(value,s,seat));return best;}
}

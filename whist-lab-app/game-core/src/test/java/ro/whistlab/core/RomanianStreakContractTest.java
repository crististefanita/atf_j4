package ro.whistlab.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;

/** Original Instructions series semantics plus the user-selectable positive reward. */
public class RomanianStreakContractTest {
    @Test public void rs01OriginalFifthExactContractAddsFiveAndResets(){assertEquals(new RomanianScoring.Result(7,5,0),RomanianScoring.score(2,2,3,4,5));}
    @Test public void rs02SelectedTenReplacesOnlyThePositiveSeriesReward(){assertEquals(new RomanianScoring.Result(7,10,0),RomanianScoring.score(2,2,3,4,10));assertEquals(new RomanianScoring.Result(-2,-5,0),RomanianScoring.score(0,2,3,-4,10));}
    @Test public void rs03FirstFourExactContractsNeverReceiveAPrematureBonus(){for(int value:List.of(5,10))for(int before=0;before<4;before++)assertEquals(new RomanianScoring.Result(5,0,before+1),RomanianScoring.score(0,0,2,before,value));}
    @Test public void rs04BothUnderbidAndOverbidCountAsMissedContracts(){for(int value:List.of(5,10))for(int before=-4;before<=0;before++)for(int won:List.of(0,2)){var r=RomanianScoring.score(won,1,2,before,value);assertEquals(-1,r.basePoints());assertEquals(before==-4?-5:0,r.streakPoints());assertEquals(before==-4?0:before-1,r.nextStreak());}}
    @Test public void rs05ExactZeroCountsAsWinningAContractEvenIfAnotherPlayerTakesAllTricks(){assertEquals(new RomanianScoring.Result(5,10,0),RomanianScoring.score(0,0,8,4,10));}
    @Test public void rs06OppositeEligibleOutcomeStartsANewSeriesAtOne(){for(int before=1;before<=4;before++)assertEquals(-1,RomanianScoring.score(0,1,2,before).nextStreak());for(int before=-4;before<0;before++)assertEquals(1,RomanianScoring.score(0,0,2,before).nextStreak());}
    @Test public void rs07OneCardExactDoesNotAdvanceAWinningSeries(){for(int before=0;before<=4;before++)for(int won=0;won<=1;won++){var r=RomanianScoring.score(won,won,1,before,10);assertEquals(before,r.nextStreak());assertEquals(0,r.streakPoints());}}
    @Test public void rs08OneCardMissDoesNotAdvanceALosingSeries(){for(int before=-4;before<=0;before++){var r=RomanianScoring.score(1,0,1,before,10);assertEquals(before,r.nextStreak());assertEquals(0,r.streakPoints());}}
    @Test public void rs09OppositeOneCardOutcomeBreaksEitherSeriesWithoutStartingAnother(){for(int before=1;before<=4;before++)assertEquals(0,RomanianScoring.score(0,1,1,before).nextStreak());for(int before=-4;before<0;before++)assertEquals(0,RomanianScoring.score(0,0,1,before).nextStreak());}
    @Test public void rs10TheTenthMatchingEligibleResultAwardsASecondBonus(){for(int value:List.of(5,10)){int streak=0,total=0;for(int i=1;i<=10;i++){var r=RomanianScoring.score(0,0,2,streak,value);assertEquals(i%5==0?value:0,r.streakPoints());streak=r.nextStreak();total+=r.totalPoints();}assertEquals(50+2*value,total);assertEquals(0,streak);}}
    @Test public void rs11AllPriorCountersAndLegalBidWonPairsMatchAnIndependentTwoCounterOracle()throws Exception{
        var evidence=new StringBuilder("size\tbid\twon\tprevious\tpositiveReward\texpectedBase\tactualBase\texpectedSeries\tactualSeries\texpectedNext\tactualNext\tpassed\n");
        int combinations=0;try{for(int value:List.of(5,10))for(int size=1;size<=8;size++)for(int before=-4;before<=4;before++)for(int bid=0;bid<=size;bid++)for(int won=0;won<=size;won++){
            int wins=Math.max(before,0),losses=Math.max(-before,0),base;
            if(won==bid){base=bid+5;losses=0;if(size>1)wins++;}else{base=-Math.abs(bid-won);wins=0;if(size>1)losses++;}
            int extra=0;if(wins==5){extra=value;wins=0;}if(losses==5){extra=-5;losses=0;}
            var expected=new RomanianScoring.Result(base,extra,wins-losses);var actual=RomanianScoring.score(won,bid,size,before,value);
            evidence.append(size).append('\t').append(bid).append('\t').append(won).append('\t').append(before).append('\t').append(value).append('\t').append(base).append('\t').append(actual.basePoints()).append('\t').append(extra).append('\t').append(actual.streakPoints()).append('\t').append(wins-losses).append('\t').append(actual.nextStreak()).append('\t').append(expected.equals(actual)).append('\n');
            assertEquals(expected,actual);combinations++;
        }assertEquals(5112,combinations);}finally{evidence("series-domain.tsv",evidence.toString());}
    }
    @Test public void rs12ImpossibleHandResultsCountersAndRewardValuesAreRejected(){
        for(int size:List.of(-1,0,9))assertThrows(IllegalArgumentException.class,()->RomanianScoring.score(0,0,size,0));
        for(int bad:List.of(-1,3)){assertThrows(IllegalArgumentException.class,()->RomanianScoring.score(bad,0,2,0));assertThrows(IllegalArgumentException.class,()->RomanianScoring.score(0,bad,2,0));}
        for(int bad:List.of(-5,5))assertThrows(IllegalArgumentException.class,()->RomanianScoring.score(0,0,2,bad));
        for(int bad:List.of(-5,0,6,15))assertThrows(IllegalArgumentException.class,()->RomanianScoring.score(0,0,2,0,bad));
    }
    @Test public void rs13ClassicCompatibilityAndNewMenuDefaultAreExplicit(){var classic=new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,1);assertFalse(classic.streakBonuses());assertEquals(5,classic.streakBonusPoints());var selected=new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,1,true);assertTrue(selected.streakBonuses());assertEquals(10,selected.streakBonusPoints());}
    @Test public void rs14SeriesAreRejectedForUnrelatedModesAndInvalidRewardOptions(){for(GameMode mode:GameMode.values())if(!mode.isRomanian())assertThrows(IllegalArgumentException.class,()->new GameConfig(4,mode,AiDifficulty.NORMAL,1,true,10));for(int bad:List.of(0,6,20))assertThrows(IllegalArgumentException.class,()->new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,1,true,bad));}
    @Test public void rs15EngineAppliesConfiguredBonusesOnlyOnTheFinalCardAndPreservesThemAcrossResume()throws Exception{for(int value:List.of(5,10)){
        var game=fixture(value,true,new int[]{4,4,-4,-4});String input=game.snapshot().toString();while(game.snapshot().phase()==GamePhase.BIDDING){int s=game.snapshot().biddingSeat();game.submitBid(s,new int[]{2,0,0,1}[s]);}
        for(int i=0;i<8;i++){var s=game.snapshot();assertTrue(s.players().stream().allMatch(p->p.totalScore()==0));game.playCard(s.currentSeat(),s.players().get(s.currentSeat()).hand().get(0));if(i==6)game=GameSession.restoreComplete(game.saveState());}
        assertEquals(List.of(7+value,5+value,5,-6),game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());
        assertEquals(List.of(0,0,1,0),game.snapshot().players().stream().map(GameSession.PlayerSnapshot::streak).toList());
        assertEquals(List.of(value,value,0,-5),game.snapshot().players().stream().map(GameSession.PlayerSnapshot::streakPoints).toList());
        var complete=game.snapshot();game=GameSession.restoreComplete(game.saveState());assertEquals(complete,game.snapshot());var scored=game;assertThrows(IllegalStateException.class,()->scored.playCard(0,new Card(Suit.HEARTS,14)));assertEquals(complete,game.snapshot());
        game.startNextHand();assertEquals(complete.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList(),game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());assertTrue(game.snapshot().players().stream().allMatch(p->p.streakPoints()==0));assertEquals(value,game.config().streakBonusPoints());
        evidence("series-integration-"+value+".txt","Controlled component boundary, not a normal UI game.\nINPUT: "+input+"\nBIDS: [2,0,0,1]\nEXPECTED WON: [2,0,0,0]\nEXPECTED TOTALS: "+List.of(7+value,5+value,5,-6)+"\nEXPECTED COUNTERS: [0,0,1,0]\nEXPECTED SERIES POINTS: "+List.of(value,value,0,-5)+"\nACTUAL SCORED STATE (also identical after checkpoint restore): "+complete+"\nACTUAL NEXT HAND (same totals, cleared awarded points): "+game.snapshot()+"\nAll assertions PASS.\n");
    }}
    @Test public void rs16DisablingSeriesPreservesClassicScores(){var g=fixture(10,false,new int[]{0,0,0,0});while(g.snapshot().phase()==GamePhase.BIDDING){int s=g.snapshot().biddingSeat();g.submitBid(s,new int[]{2,0,0,1}[s]);}while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();g.playCard(s.currentSeat(),s.players().get(s.currentSeat()).hand().get(0));}assertEquals(List.of(7,5,5,-1),g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());}
    @Test public void rs17ExpertUtilityIncludesTheSelectedRewardAndFixedPenalty(){for(int value:List.of(5,10)){var g=fixture(value,true,new int[]{4,-4,0,0});var v=MonteCarloAi.observe(g.snapshot(),0);assertEquals(7+value,MonteCarloAi.points(v,0,2,2));assertEquals(-6,MonteCarloAi.points(v,1,0,1));assertEquals(5,MonteCarloAi.points(v,2,0,0));}}
    @Test public void rs18LegacyIndependentPlannerHashesRemainIdenticalForClassicInputs(){
        var seats=List.of(new IndependentPlayer.Seat(2,null,0,10),new IndependentPlayer.Seat(2,1,0,-2),new IndependentPlayer.Seat(2,0,0,0));
        var oldSeats=seats.stream().map(s->new V1Seat(s.remaining(),s.bid(),s.won(),s.total())).toList();assertEquals(oldSeats.hashCode(),seats.hashCode());
        var p=new IndependentPlayer.Position(0,2,2,0,List.of(5,18),seats,List.of(),List.of(),6);
        assertEquals(new V1Position(0,2,2,0,List.of(5,18),oldSeats,List.of(),List.of(),6).hashCode(),p.hashCode());
    }
    private record V1Seat(int remaining,Integer bid,int won,int total) { }
    private record V1Position(int seat,int dealer,int size,int trump,List<Integer> hand,List<V1Seat> seats,List<IndependentPlayer.Play> trick,List<IndependentPlayer.Play> history,int turnedCard) { }
    @Test public void rs19CheckpointsRejectOutOfRangeSeriesCounters(){for(int bad:List.of(-5,5)){var saved=fixture(10,true,new int[]{0,0,0,0}).saveState();assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(saved,0,bad,0)));}}
    @Test public void rs20BonusCannotAppearBeforeScoringOrWhenDisabled(){var enabled=fixture(10,true,new int[]{4,0,0,0}).saveState();assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(enabled,0,0,10)));var classic=fixture(10,false,new int[]{0,0,0,0}).saveState();assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(classic,0,1,0)));}
    @Test public void rs21BonusSignAndCounterMustMatchTheCompletedContract(){var g=fixture(10,true,new int[]{0,0,0,0});while(g.snapshot().phase()==GamePhase.BIDDING){int s=g.snapshot().biddingSeat();g.submitBid(s,new int[]{2,0,0,1}[s]);}while(g.snapshot().phase()==GamePhase.PLAYING){var s=g.snapshot();g.playCard(s.currentSeat(),s.players().get(s.currentSeat()).hand().get(0));}var scored=g.saveState();assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(scored,3,0,10)));assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(scored,0,0,-5)));assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(scored,0,1,10)));assertThrows(IllegalArgumentException.class,()->GameSession.restoreComplete(change(scored,0,-1,0)));}
    @Test public void rs22ActualOneCardHandPreservesMatchingStreaksAndClearsOppositeStreaks(){
        var config=new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.NORMAL,12,true,10);
        var dealt=GameSession.fromDeal(config,List.of(List.of(c(14)),List.of(c(13)),List.of(c(12)),List.of(c(11))),Trump.NO_TRUMP,3).saveState();
        var g=GameSession.restoreComplete(change(change(change(change(dealt,0,4,0),1,-4,0),2,4,0),3,-4,0));
        for(int seat=0;seat<4;seat++)g.submitBid(seat,new int[]{1,0,0,1}[seat]);for(int seat=0;seat<4;seat++)g.playCard(seat,c(14-seat));
        assertEquals(List.of(4,0,4,-4),g.snapshot().players().stream().map(GameSession.PlayerSnapshot::streak).toList());assertEquals(List.of(6,5,5,-1),g.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());assertTrue(g.snapshot().players().stream().allMatch(p->p.streakPoints()==0));assertEquals(g.snapshot(),GameSession.restoreComplete(g.saveState()).snapshot());
    }
    @Test public void rs23ChangingOnlyAnInapplicableSeriesRewardDoesNotChangeBeliefsOrDecisions(){
        var five=fixture(5,true,new int[]{0,0,0,0});var ten=fixture(10,true,new int[]{0,0,0,0});
        for(int seat=0;seat<4;seat++){five.submitBid(seat,new int[]{2,0,0,1}[seat]);ten.submitBid(seat,new int[]{2,0,0,1}[seat]);}
        assertEquals(MonteCarloAi.chooseCard(MonteCarloAi.observe(five.snapshot(),0)),MonteCarloAi.chooseCard(MonteCarloAi.observe(ten.snapshot(),0)));
        var a=AiTrainingTournamentTest.publicPosition(five.snapshot(),0);var b=AiTrainingTournamentTest.publicPosition(ten.snapshot(),0);assertEquals(IndependentPlayer.card(a),IndependentPlayer.card(b));
    }
    @Test public void rs24BothExpertAndIndependentPlannerBidTheForcedTwoTrickContractWithEitherSeriesReward(){
        for(int value:List.of(5,10)){var g=fixture(value,true,new int[]{4,0,0,0});assertEquals(2,MonteCarloAi.chooseBid(MonteCarloAi.observe(g.snapshot(),0)));assertEquals(2,IndependentPlayer.bid(AiTrainingTournamentTest.publicPosition(g.snapshot(),0)).value());}
    }
    private static GameSession.SaveState change(GameSession.SaveState s,int seat,int streak,int bonus){var players=new ArrayList<>(s.players());var p=players.get(seat);players.set(seat,new GameSession.PlayerState(p.seat(),p.name(),p.human(),p.hand(),p.bid(),p.tricksWon(),p.totalScore(),streak,bonus));return new GameSession.SaveState(s.config(),s.phase(),s.handIndex(),s.handSize(),s.dealerSeat(),s.biddingSeat(),s.currentSeat(),s.bidsSubmitted(),s.bidTotal(),s.trump(),s.lastTrickWinnerSeat(),players,s.currentTrick(),s.lastTrick(),s.history(),s.turnedCard());}
    private static GameSession fixture(int value,boolean enabled,int[] streaks){
        var config=new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,11,enabled,value);
        var deal=GameSession.fromDeal(config,List.of(List.of(new Card(Suit.SPADES,14),new Card(Suit.SPADES,13)),List.of(c(12),c(11)),List.of(c(10),c(9)),List.of(c(8),c(7))),Trump.SPADES,3).saveState();
        var players=deal.players().stream().map(p->new GameSession.PlayerState(p.seat(),p.name(),p.human(),p.hand(),p.bid(),p.tricksWon(),p.totalScore(),streaks[p.seat()],0)).toList();
        return GameSession.restoreComplete(new GameSession.SaveState(config,deal.phase(),19,2,deal.dealerSeat(),deal.biddingSeat(),deal.currentSeat(),0,0,deal.trump(),-1,players,List.of(),List.of(),List.of(),new Card(Suit.SPADES,7)));
    }
    private static void evidence(String name,String value)throws Exception{String out=System.getProperty("whist.componentEvidence","");if(!out.isBlank()){var folder=java.nio.file.Path.of(out);java.nio.file.Files.createDirectories(folder);java.nio.file.Files.writeString(folder.resolve(name),value);}}
    private static Card c(int rank){return new Card(Suit.HEARTS,rank);}
}

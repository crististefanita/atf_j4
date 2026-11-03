package ro.whistlab.core;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.baseline.PreviousIndependentPlayer;
import ro.whistlab.core.baseline.RoundTwoAiPolicy;
import ro.whistlab.core.baseline.RoundThreeAiPolicy;
import ro.whistlab.coach.baseline.RoundThreeIndependentPlayer;
import ro.whistlab.core.baseline.RoundFourAiPolicy;
import ro.whistlab.coach.baseline.RoundFourIndependentPlayer;

/** Offline complete games. Full deals go only to the recorder, not to policy inputs. */
public class ComponentCorpusCollectorTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    enum Policy {EXPERT_V11,PLAYER_V11, EXPERT_V10, SAFETY_COVERS, SAFETY_GREEDY, PLAYER_V8, EXPERT_V6, PLAYER_V6, EXPERT_V2, PLAYER_V2, EXPERT_V3, PLAYER_V3, EXPERT_V4, PLAYER_V4, EXPERT_CANDIDATE, PLAYER_CANDIDATE }
    @Test public void collectsEveryDeclaredGameIncludingLossesAsSimpleJson()throws Exception {
        String output=System.getProperty("whist.corpusDir","");assertFalse("Explicit external corpus folder required",output.isBlank());
        Path folder=Path.of(output).toAbsolutePath().resolve("games/development");Files.createDirectories(folder);
        int first=number("whist.corpusFirstSeed",800),seeds=number("whist.corpusSeeds",64);assertTrue(seeds>0);
        String variant=System.getProperty("whist.corpusVariant","");if(variant.isBlank())variant="V2_BASELINE";
        Policy hero=switch(variant){case "V2_BASELINE"->Policy.PLAYER_V2;case "PLAYER_CANDIDATE"->Policy.PLAYER_CANDIDATE;case "EXPERT_CANDIDATE"->Policy.EXPERT_CANDIDATE;default->throw new IllegalArgumentException("Unknown corpus variant: "+variant);};
        Policy opponent=switch(System.getProperty("whist.corpusOpponent","")){case "", "V2_EXPERT", "EXPERT_V2"->Policy.EXPERT_V2;case "V2_PLAYER", "PLAYER_V2"->Policy.PLAYER_V2;case "EXPERT_CANDIDATE"->Policy.EXPERT_CANDIDATE;case "PLAYER_CANDIDATE"->Policy.PLAYER_CANDIDATE;default->throw new IllegalArgumentException("Unknown corpus opponent");};
        int completed=0,created=0;long started=System.nanoTime();
        for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++){
            String id="component-"+variant.toLowerCase(Locale.ROOT)+"-"+opponent.name().toLowerCase(Locale.ROOT)+"-seed"+seed+"-hero"+seat;
            Path target=folder.resolve(id+".json");
            if(Files.exists(target)){var existing=JSON.readTree(Files.readString(target));assertEquals(id,existing.path("id").asText());assertEquals(24,existing.path("hands").size());}
            else {
                var game=play(id,seed,seat,hero,opponent);String encoded=JSON.writerWithDefaultPrettyPrinter().writeValueAsString(game)+"\n";
                Path temporary=Files.createTempFile(folder,id+"-",".tmp");Files.writeString(temporary,encoded);
                try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException notAtomic){Files.move(temporary,target);}created++;
            }
            completed++;Properties progress=new Properties();progress.setProperty("status","RUNNING");progress.setProperty("variant",variant);progress.setProperty("completed",""+completed);progress.setProperty("created",""+created);progress.setProperty("planned",""+(seeds*4));progress.setProperty("elapsedMillis",""+(System.nanoTime()-started)/1_000_000);
            try(var stream=Files.newOutputStream(Path.of(output).resolve("collection-progress.properties"))){progress.store(stream,"Every loss and tie retained; replay validation follows collection");}
        }
        assertEquals(seeds*4,completed);
        Properties done=new Properties();done.setProperty("status","PASS");done.setProperty("variant",variant);done.setProperty("completed",""+completed);done.setProperty("created",""+created);done.setProperty("planned",""+(seeds*4));try(var stream=Files.newOutputStream(Path.of(output).resolve("collection-progress.properties"))){done.store(stream,"Collection complete; independent replay validation is a separate receipt");}
    }
    static Map<String,Object> play(String id,long seed,int heroSeat,Policy hero,Policy opponent){
        return play(id,seed,heroSeat,hero,opponent,game->{});
    }
    static Map<String,Object> play(String id,long seed,int heroSeat,Policy hero,Policy opponent,java.util.function.Consumer<Map<String,Object>> checkpoint){
        var controllers=new ArrayList<Policy>();for(int seat=0;seat<4;seat++)controllers.add(seat==heroSeat?hero:opponent);return playWithControllers(id,seed,heroSeat,controllers,checkpoint);
    }
    static Map<String,Object> playWithControllers(String id,long seed,int heroSeat,List<Policy> declaredControllers,java.util.function.Consumer<Map<String,Object>> checkpoint){
        List<Policy> controllers=List.copyOf(declaredControllers);if(controllers.size()!=4)throw new IllegalArgumentException("Four controllers required");
        var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,true,10));
        var players=new ArrayList<Map<String,Object>>();for(var p:game.snapshot().players())players.add(map("seat",p.seat(),"name",p.name(),"controller",label(controllers.get(p.seat()))));
        var hands=new ArrayList<Map<String,Object>>();int moves=0,tricks=0;
        while(true){
            var start=game.snapshot();var deal=new ArrayList<Map<String,Object>>();for(var p:start.players())deal.add(map("seat",p.seat(),"cards",p.hand().stream().map(ComponentCorpusCollectorTest::code).toList()));
            var bids=new ArrayList<Map<String,Object>>();var rounds=new ArrayList<Map<String,Object>>();
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();Policy policy=controllers.get(seat);
                int bid=switch(policy){case EXPERT_V11->ro.whistlab.core.baseline.RoundElevenAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V11->ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.bid(roundEleven(s,seat)).value();case EXPERT_V10->ro.whistlab.core.baseline.RoundTenAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case SAFETY_COVERS->ro.whistlab.coach.SafetyCoversPlayer.bid(safetyCovers(s,seat)).value();case SAFETY_GREEDY->ro.whistlab.coach.SafetyGreedyPlayer.bid(safetyGreedy(s,seat)).value();case PLAYER_V8->ro.whistlab.coach.baseline.RoundEightIndependentPlayer.bid(roundEight(s,seat)).value();case EXPERT_V6->ro.whistlab.core.baseline.RoundSixAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V6->ro.whistlab.coach.baseline.RoundSixIndependentPlayer.bid(roundSix(s,seat)).value();case EXPERT_V2->RoundTwoAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V2->PreviousIndependentPlayer.bid(previous(s,seat)).value();case EXPERT_V3->RoundThreeAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V3->RoundThreeIndependentPlayer.bid(roundThree(s,seat)).value();case EXPERT_V4->RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V4->RoundFourIndependentPlayer.bid(roundFour(s,seat)).value();case EXPERT_CANDIDATE->AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_CANDIDATE->IndependentPlayer.bid(AiTrainingTournamentTest.publicPosition(s,seat)).value();};
                bids.add(map("seat",seat,"bid",bid));game.submitBid(seat,bid);
            }
            var plays=new ArrayList<Map<String,Object>>();
            while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();Policy policy=controllers.get(seat);Card choice;
                if(policy==Policy.EXPERT_V11)choice=ro.whistlab.core.baseline.RoundElevenAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V10)choice=ro.whistlab.core.baseline.RoundTenAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V6)choice=ro.whistlab.core.baseline.RoundSixAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V2)choice=RoundTwoAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V3)choice=RoundThreeAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V4)choice=RoundFourAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_CANDIDATE)choice=AiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else {int c=policy==Policy.PLAYER_V11?ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.card(roundEleven(s,seat)).value():policy==Policy.SAFETY_COVERS?ro.whistlab.coach.SafetyCoversPlayer.card(safetyCovers(s,seat)).value():policy==Policy.SAFETY_GREEDY?ro.whistlab.coach.SafetyGreedyPlayer.card(safetyGreedy(s,seat)).value():policy==Policy.PLAYER_V8?ro.whistlab.coach.baseline.RoundEightIndependentPlayer.card(roundEight(s,seat)).value():policy==Policy.PLAYER_V6?ro.whistlab.coach.baseline.RoundSixIndependentPlayer.card(roundSix(s,seat)).value():policy==Policy.PLAYER_V2?PreviousIndependentPlayer.card(previous(s,seat)).value():policy==Policy.PLAYER_V3?RoundThreeIndependentPlayer.card(roundThree(s,seat)).value():policy==Policy.PLAYER_V4?RoundFourIndependentPlayer.card(roundFour(s,seat)).value():IndependentPlayer.card(AiTrainingTournamentTest.publicPosition(s,seat)).value();choice=new Card(Suit.values()[c/13],c%13+2);}
                plays.add(map("seat",seat,"card",code(choice)));game.playCard(seat,choice);moves++;
                var after=game.snapshot();if(plays.size()==4){rounds.add(map("plays",new ArrayList<>(plays),"winner",after.lastTrickWinnerSeat()));plays.clear();tricks++;}
            }
            var end=game.snapshot();var scores=new ArrayList<Map<String,Object>>();for(var p:end.players())scores.add(map("seat",p.seat(),"bid",p.bid(),"won",p.tricksWon(),"base",GameRules.scoreDelta(p.tricksWon(),p.bid(),end.mode()),"series",p.streakPoints(),"total",p.totalScore(),"streak",p.streak()));
            hands.add(map("number",start.handNumber(),"size",start.handSize(),"dealer",start.dealerSeat(),"trump",start.trump()==Trump.NO_TRUMP?"NT":"SHCD".substring(start.trump().ordinal(),start.trump().ordinal()+1),"turnedCard",start.turnedCard()==null?null:code(start.turnedCard()),"deal",deal,"bids",bids,"tricks",rounds,"scores",scores));
            hands.get(hands.size()-1).put("remainingCards",end.players().stream().map(p->p.hand().size()).toList());
            var record=map("schemaVersion",1,"id",id,"partition","development","source",map("kind","component","heroSeat",heroSeat,"complete",end.finalHand(),"visibility","post-game full deal; not a decision input"),"rules",map("variant","ROMANIAN_WHIST","players",4,"seriesEnabled",true,"seriesBonus",10),"seed",Long.toString(seed),"players",players,"hands",hands,"finalScores",end.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());checkpoint.accept(record);
            if(end.finalHand()){assertEquals(376,moves);assertEquals(94,tricks);return record;}
            game.startNextHand();
        }
    }
    static PreviousIndependentPlayer.Position previous(GameSession.Snapshot s,int seat){return new PreviousIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new PreviousIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new PreviousIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new PreviousIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static RoundThreeIndependentPlayer.Position roundThree(GameSession.Snapshot s,int seat){return new RoundThreeIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new RoundThreeIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new RoundThreeIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new RoundThreeIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static RoundFourIndependentPlayer.Position roundFour(GameSession.Snapshot s,int seat){return new RoundFourIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new RoundFourIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new RoundFourIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new RoundFourIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.baseline.RoundSixIndependentPlayer.Position roundSix(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.RoundSixIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.RoundSixIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.RoundSixIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.RoundSixIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.Position roundEleven(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.RoundElevenIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.baseline.RoundEightIndependentPlayer.Position roundEight(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.RoundEightIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.RoundEightIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.RoundEightIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.RoundEightIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.SafetyGreedyPlayer.Position safetyGreedy(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.SafetyGreedyPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.SafetyGreedyPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.SafetyGreedyPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.SafetyGreedyPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.SafetyCoversPlayer.Position safetyCovers(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.SafetyCoversPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(ComponentCorpusCollectorTest::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.SafetyCoversPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.SafetyCoversPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.SafetyCoversPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    private static String label(Policy p){return switch(p){case EXPERT_V11->"Expert certified-v11";case PLAYER_V11->"Independent certified-v11";case EXPERT_V10->"Expert safety-v10";case SAFETY_COVERS->"Independent safety / covers (research)";case SAFETY_GREEDY->"Independent safety / greedy";case PLAYER_V8->"IndependentPlayer round-v8";case EXPERT_V6->"Expert round-v6";case PLAYER_V6->"IndependentPlayer round-v6";case EXPERT_V2->"Expert round-v2";case PLAYER_V2->"IndependentPlayer round-v2";case EXPERT_V3->"Expert round-v3";case PLAYER_V3->"IndependentPlayer round-v3";case EXPERT_V4->"Expert round-v4";case PLAYER_V4->"IndependentPlayer round-v4";case EXPERT_CANDIDATE->"Expert candidate-v5";case PLAYER_CANDIDATE->"IndependentPlayer candidate-v5";};}
    private static int number(String key,int fallback){String value=System.getProperty(key,"");return value.isBlank()?fallback:Integer.parseInt(value);}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static String code(Card c){return (c.rank()==14?"A":c.rank()==13?"K":c.rank()==12?"Q":c.rank()==11?"J":""+c.rank())+"SHCD".charAt(c.suit().ordinal());}
    static Map<String,Object> map(Object... entries){var result=new LinkedHashMap<String,Object>();for(int i=0;i<entries.length;i+=2)result.put((String)entries[i],entries[i+1]);return result;}
}

package ro.whistlab.core.testing.match;

import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.learning.tournament.AiTrainingTournamentTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import ro.whistlab.coach.IndependentPlayer;
import ro.whistlab.coach.baseline.PreviousIndependentPlayer;
import ro.whistlab.core.baseline.v2.RoundTwoAiPolicy;
import ro.whistlab.core.baseline.v3.RoundThreeAiPolicy;
import ro.whistlab.coach.baseline.RoundThreeIndependentPlayer;
import ro.whistlab.core.baseline.v4.RoundFourAiPolicy;
import ro.whistlab.coach.baseline.RoundFourIndependentPlayer;

/** Offline complete games. Full deals go only to the recorder, not to policy inputs. */
public class OfflineMatchHarness {
    public enum Policy {EXPERT_V17,EXPERT_V14,EXPERT_V11,PLAYER_V11, EXPERT_V10, SAFETY_COVERS, SAFETY_GREEDY, PLAYER_V8, EXPERT_V6, PLAYER_V6, EXPERT_V2, PLAYER_V2, EXPERT_V3, PLAYER_V3, EXPERT_V4, PLAYER_V4, EXPERT_CANDIDATE, PLAYER_CANDIDATE }
    public static Map<String,Object> play(String id,long seed,int heroSeat,Policy hero,Policy opponent){
        return play(id,seed,heroSeat,hero,opponent,game->{});
    }
    public static Map<String,Object> play(String id,long seed,int heroSeat,Policy hero,Policy opponent,java.util.function.Consumer<Map<String,Object>> checkpoint){
        var controllers=new ArrayList<Policy>();for(int seat=0;seat<4;seat++)controllers.add(seat==heroSeat?hero:opponent);return playWithControllers(id,seed,heroSeat,controllers,checkpoint);
    }
    public static Map<String,Object> playWithControllers(String id,long seed,int heroSeat,List<Policy> declaredControllers,java.util.function.Consumer<Map<String,Object>> checkpoint){
        List<Policy> controllers=List.copyOf(declaredControllers);if(controllers.size()!=4)throw new IllegalArgumentException("Four controllers required");
        var game=GameSession.newGame(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.EXPERT,seed,true,10));
        var players=new ArrayList<Map<String,Object>>();for(var p:game.snapshot().players())players.add(map("seat",p.seat(),"name",p.name(),"controller",label(controllers.get(p.seat()))));
        var hands=new ArrayList<Map<String,Object>>();int moves=0,tricks=0;
        while(true){
            var start=game.snapshot();var deal=new ArrayList<Map<String,Object>>();for(var p:start.players())deal.add(map("seat",p.seat(),"cards",p.hand().stream().map(OfflineMatchHarness::code).toList()));
            var bids=new ArrayList<Map<String,Object>>();var rounds=new ArrayList<Map<String,Object>>();
            while(game.snapshot().phase()==GamePhase.BIDDING){var s=game.snapshot();int seat=s.biddingSeat();Policy policy=controllers.get(seat);
                int bid=switch(policy){case EXPERT_V17->ro.whistlab.core.baseline.v17.RoundSeventeenAiPolicy.chooseBid(s,seat,AiDifficulty.HARD);case EXPERT_V14->ro.whistlab.core.baseline.v14.RoundFourteenAiPolicy.chooseBid(s,seat,AiDifficulty.HARD);case EXPERT_V11->ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V11->ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.bid(roundEleven(s,seat)).value();case EXPERT_V10->ro.whistlab.core.baseline.v10.RoundTenAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case SAFETY_COVERS->ro.whistlab.coach.strategy.SafetyCoversPlayer.bid(safetyCovers(s,seat)).value();case SAFETY_GREEDY->ro.whistlab.coach.strategy.SafetyGreedyPlayer.bid(safetyGreedy(s,seat)).value();case PLAYER_V8->ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.bid(roundEight(s,seat)).value();case EXPERT_V6->ro.whistlab.core.baseline.v6.RoundSixAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V6->ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.bid(roundSix(s,seat)).value();case EXPERT_V2->RoundTwoAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V2->PreviousIndependentPlayer.bid(previous(s,seat)).value();case EXPERT_V3->RoundThreeAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V3->RoundThreeIndependentPlayer.bid(roundThree(s,seat)).value();case EXPERT_V4->RoundFourAiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_V4->RoundFourIndependentPlayer.bid(roundFour(s,seat)).value();case EXPERT_CANDIDATE->AiPolicy.chooseBid(s,seat,AiDifficulty.EXPERT);case PLAYER_CANDIDATE->IndependentPlayer.bid(PlayerObservations.independent(s,seat)).value();};
                bids.add(map("seat",seat,"bid",bid));game.submitBid(seat,bid);
            }
            var plays=new ArrayList<Map<String,Object>>();
            while(game.snapshot().phase()==GamePhase.PLAYING){var s=game.snapshot();int seat=s.currentSeat();Policy policy=controllers.get(seat);Card choice;
                if(policy==Policy.EXPERT_V17)choice=ro.whistlab.core.baseline.v17.RoundSeventeenAiPolicy.chooseCard(s,seat,AiDifficulty.HARD);
                else if(policy==Policy.EXPERT_V14)choice=ro.whistlab.core.baseline.v14.RoundFourteenAiPolicy.chooseCard(s,seat,AiDifficulty.HARD);
                else if(policy==Policy.EXPERT_V11)choice=ro.whistlab.core.baseline.v11.RoundElevenAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V10)choice=ro.whistlab.core.baseline.v10.RoundTenAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V6)choice=ro.whistlab.core.baseline.v6.RoundSixAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V2)choice=RoundTwoAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V3)choice=RoundThreeAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_V4)choice=RoundFourAiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else if(policy==Policy.EXPERT_CANDIDATE)choice=AiPolicy.chooseCard(s,seat,AiDifficulty.EXPERT);
                else {int c=policy==Policy.PLAYER_V11?ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.card(roundEleven(s,seat)).value():policy==Policy.SAFETY_COVERS?ro.whistlab.coach.strategy.SafetyCoversPlayer.card(safetyCovers(s,seat)).value():policy==Policy.SAFETY_GREEDY?ro.whistlab.coach.strategy.SafetyGreedyPlayer.card(safetyGreedy(s,seat)).value():policy==Policy.PLAYER_V8?ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.card(roundEight(s,seat)).value():policy==Policy.PLAYER_V6?ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.card(roundSix(s,seat)).value():policy==Policy.PLAYER_V2?PreviousIndependentPlayer.card(previous(s,seat)).value():policy==Policy.PLAYER_V3?RoundThreeIndependentPlayer.card(roundThree(s,seat)).value():policy==Policy.PLAYER_V4?RoundFourIndependentPlayer.card(roundFour(s,seat)).value():IndependentPlayer.card(PlayerObservations.independent(s,seat)).value();choice=new Card(Suit.values()[c/13],c%13+2);}
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
    public static PreviousIndependentPlayer.Position previous(GameSession.Snapshot s,int seat){return new PreviousIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new PreviousIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new PreviousIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new PreviousIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    public static RoundThreeIndependentPlayer.Position roundThree(GameSession.Snapshot s,int seat){return new RoundThreeIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new RoundThreeIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new RoundThreeIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new RoundThreeIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    public static RoundFourIndependentPlayer.Position roundFour(GameSession.Snapshot s,int seat){return new RoundFourIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new RoundFourIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new RoundFourIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new RoundFourIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Position roundSix(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.v6.RoundSixIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.Position roundEleven(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.v11.RoundElevenIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    public static ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.Position roundEight(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.baseline.v8.RoundEightIndependentPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.strategy.SafetyGreedyPlayer.Position safetyGreedy(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.strategy.SafetyGreedyPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.strategy.SafetyGreedyPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.strategy.SafetyGreedyPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.strategy.SafetyGreedyPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    static ro.whistlab.coach.strategy.SafetyCoversPlayer.Position safetyCovers(GameSession.Snapshot s,int seat){return new ro.whistlab.coach.strategy.SafetyCoversPlayer.Position(seat,s.dealerSeat(),s.handSize(),s.trump().ordinal(),s.players().get(seat).hand().stream().map(OfflineMatchHarness::id).toList(),s.players().stream().map(p->new ro.whistlab.coach.strategy.SafetyCoversPlayer.Seat(p.hand().size(),p.bid(),p.tricksWon(),p.totalScore(),p.streak())).toList(),s.currentTrick().stream().map(p->new ro.whistlab.coach.strategy.SafetyCoversPlayer.Play(p.seat(),id(p.card()))).toList(),s.history().stream().map(p->new ro.whistlab.coach.strategy.SafetyCoversPlayer.Play(p.seat(),id(p.card()))).toList(),s.turnedCard()==null?-1:id(s.turnedCard()),s.streakBonuses(),s.streakBonusPoints());}
    private static String label(Policy p){return switch(p){case EXPERT_V17->"Expert accepted-v17 corrected-quota";case EXPERT_V14->"Expert accepted-v14";case EXPERT_V11->"Expert certified-v11";case PLAYER_V11->"Independent certified-v11";case EXPERT_V10->"Expert safety-v10";case SAFETY_COVERS->"Independent safety / covers (research)";case SAFETY_GREEDY->"Independent safety / greedy";case PLAYER_V8->"IndependentPlayer round-v8";case EXPERT_V6->"Expert round-v6";case PLAYER_V6->"IndependentPlayer round-v6";case EXPERT_V2->"Expert round-v2";case PLAYER_V2->"IndependentPlayer round-v2";case EXPERT_V3->"Expert round-v3";case PLAYER_V3->"IndependentPlayer round-v3";case EXPERT_V4->"Expert round-v4";case PLAYER_V4->"IndependentPlayer round-v4";case EXPERT_CANDIDATE->"Expert candidate-v5";case PLAYER_CANDIDATE->"IndependentPlayer candidate-v5";};}
    private static int id(Card c){return c.suit().ordinal()*13+c.rank()-2;}
    private static String code(Card c){return (c.rank()==14?"A":c.rank()==13?"K":c.rank()==12?"Q":c.rank()==11?"J":""+c.rank())+"SHCD".charAt(c.suit().ordinal());}
    public static Map<String,Object> map(Object... entries){var result=new LinkedHashMap<String,Object>();for(int i=0;i<entries.length;i+=2)result.put((String)entries[i],entries[i+1]);return result;}
}

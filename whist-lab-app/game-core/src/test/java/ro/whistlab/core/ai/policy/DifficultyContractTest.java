package ro.whistlab.core.ai.policy;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.OneCardBidBelief;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.learning.mining.LossMiningTest;
import ro.whistlab.core.learning.regression.RolloutPolicyRegressionTest;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.testing.CapturedRegression;
import ro.whistlab.core.testing.PublicEndgameOracle;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ro.whistlab.core.baseline.v11.RoundElevenAiPolicy;

/** Menu levels, save compatibility and the strongest accepted policy are separate contracts. */
public class DifficultyContractTest {
    private static GameSession.Snapshot snapshot(MonteCarloAi.View v,List<List<Card>> world,GamePhase phase) {
        var players=new ArrayList<GameSession.PlayerSnapshot>();
        for(int seat=0;seat<world.size();seat++) {
            var s=v.seats().get(seat);
            players.add(new GameSession.PlayerSnapshot(seat,"Player "+seat,seat==0,world.get(seat),
                    s.bid(),s.won(),s.total(),s.streak(),0));
        }
        int submitted=(int)v.seats().stream().filter(s->s.bid()!=null).count();
        int bidTotal=v.seats().stream().filter(s->s.bid()!=null).mapToInt(s->s.bid()).sum();
        return new GameSession.Snapshot(phase,0,1,v.handSize(),v.trump(),v.dealer(),v.seat(),v.seat(),
                submitted,bidTotal,players,v.trick(),List.of(),-1,false,v.mode(),v.history(),v.turnedCard(),
                v.streakBonuses(),v.streakBonusPoints());
    }
    @Test public void onlyNormalAndHardAreSelectable() throws Exception {
        CapturedRegression.run("difficulty-two-levels",getClass().getName(),"onlyTwoMenuLevels",Map.of(
                "purpose","Expose exactly Normal and Hard to the enum-driven menu",
                "rule","EXPERT remains a deprecated source alias to HARD; it is not an enum value or a third selectable level."),c->{
            c.equal("Selectable names",List.of("NORMAL","HARD"),Arrays.stream(AiDifficulty.values()).map(Enum::name).toList());
            c.equal("Legacy source alias is the same Hard instance",true,AiDifficulty.EXPERT==AiDifficulty.HARD);
            c.equal("Visible labels",List.of("Normal","Hard"),Arrays.stream(AiDifficulty.values()).map(AiDifficulty::label).toList());
        });
    }
    @Test public void oldExpertSavesMigrateWithoutSilentlyAcceptingAnUnknownLevel() throws Exception {
        CapturedRegression.run("difficulty-legacy-save",getClass().getName(),"legacyExpertBecomesHard",Map.of(
                "purpose","Retain old saved matches after consolidating levels",
                "rule","Persisted EXPERT maps to HARD; NORMAL and HARD keep their meaning. Unknown values must fail rather than silently change the match."),c->{
            c.equal("Old saved name",AiDifficulty.HARD,AiDifficulty.fromStoredName("EXPERT"));
            c.equal("New Hard name",AiDifficulty.HARD,AiDifficulty.fromStoredName("HARD"));
            c.equal("Normal name",AiDifficulty.NORMAL,AiDifficulty.fromStoredName("NORMAL"));
            c.equal("New serialized name", "HARD",AiDifficulty.fromStoredName("EXPERT").name());
            assertThrows(IllegalArgumentException.class,()->AiDifficulty.fromStoredName("IMPOSSIBLE"));
        });
    }
    @Test public void hardBiddingUsesAnExactPosteriorInsteadOfTheOldHonorHint() throws Exception {
        var seats=List.of(new MonteCarloAi.Seat(1,null,0),new MonteCarloAi.Seat(1,null,0),
                new MonteCarloAi.Seat(1,null,0),new MonteCarloAi.Seat(1,null,0));
        var v=new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,Trump.SPADES,0,3,1,
                List.of(new Card(Suit.HEARTS,14)),seats,List.of(),List.of(),new Card(Suit.SPADES,7));
        var world=List.of(v.hand(),List.of(new Card(Suit.HEARTS,7)),List.of(new Card(Suit.CLUBS,7)),List.of(new Card(Suit.DIAMONDS,7)));
        var s=snapshot(v,world,GamePhase.BIDDING);
        CapturedRegression.run("difficulty-hard-exact-bid",getClass().getName(),"uniformOneCardPosteriorHasALiteralOptimalContract",Map.of(
                "purpose","Hard uses the strongest accepted bidding engine",
                "rule","Lead AH at spades trump, face-up7S, no previous bids. Of30 unknown cards,23 are non-trumps. Exactly1771 of4060 unordered opponent triples contain no trump. Winning probability1771/4060 is below one half, so bid0 maximizes exact-contract chance.",
                "publicPosition",v,"scope","Exact under the declared uniform-deal prior, not a claim of globally perfect hidden-information play."),c->{
            c.equal("Independent literal posterior",1771.0/4060,OneCardBidBelief.probability(v));
            c.equal("Normal retains its honor-based bid",1,AiPolicy.chooseBid(s,0,AiDifficulty.NORMAL));
            c.equal("Hard chooses the exact-chance optimum",0,AiPolicy.chooseBid(s,0,AiDifficulty.HARD));
            c.equal("Legacy source alias uses Hard",0,AiPolicy.chooseBid(s,0,AiDifficulty.EXPERT));
        });
    }
    @Test public void hardBidsBothUnbeatableTopTrumps() throws Exception {
        var hands=List.of(RolloutPolicyRegressionTest.hand("AS","KS"),RolloutPolicyRegressionTest.hand("7H","8H"),
                RolloutPolicyRegressionTest.hand("7C","8C"),RolloutPolicyRegressionTest.hand("7D","8D"));
        var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,91),hands,Trump.SPADES,3);
        CapturedRegression.run("difficulty-hard-two-masters",getClass().getName(),"twoTopTrumpsGuaranteeTwoTricks",Map.of(
                "purpose","Hard optimizes the attainable contract rather than summing honor hints",
                "rule","AS and KS cannot be beaten and win both tricks in every legal deal consistent with owning them.",
                "ownHand",hands.get(0)),c->{
            c.equal("Normal retains its original trump-adjusted estimate",2,AiPolicy.chooseBid(game.snapshot(),0,AiDifficulty.NORMAL));
            c.equal("Hard bids the guaranteed two tricks",2,AiPolicy.chooseBid(game.snapshot(),0,AiDifficulty.HARD));
        });
    }
    @Test public void hardKeepsTheCertifiedContractAcrossHiddenWorlds() throws Exception {
        var json=new ObjectMapper();var fixture=json.readTree(getClass().getResourceAsStream("/regressions/contract-certificate/cases.json")).path("positive");
        var v=ro.whistlab.core.testing.codec.ObservedPositionJson.view(json,fixture.path("input").path("before"));
        var oracle=PublicEndgameOracle.audit(v);var worlds=MonteCarloAi.enumerateSmallDeals(v);
        CapturedRegression.run("difficulty-hard-certified-discard",getClass().getName(),"hardUsesTheAcceptedPublicCertificate",Map.of(
                "purpose","Hard inherits the strongest accepted card-play guard",
                "rule","Bid1 is already made. Discard10D, retainQH: exactly one trick across all96 feasible worlds and every legal continuation. The legacy simple Hard heuristic discardedQH and admitted an unwanted extra trick.",
                "before",v,"independentOracle",Map.of("worlds",oracle.worlds(),"safe",oracle.safe(),"finalWon",oracle.finalWon())),c->{
            c.equal("Independent complete-world count",96,oracle.worlds());
            c.equal("Independent uniquely safe discard",List.of("10D"),oracle.safe());
            c.equal("Hard chooses10D in the first hidden assignment","10D",LossMiningTest.code(AiPolicy.chooseCard(snapshot(v,worlds.get(0),GamePhase.PLAYING),v.seat(),AiDifficulty.HARD)));
            c.equal("Hard chooses10D in another hidden assignment","10D",LossMiningTest.code(AiPolicy.chooseCard(snapshot(v,worlds.get(worlds.size()-1),GamePhase.PLAYING),v.seat(),AiDifficulty.HARD)));
        });
    }
    @Test public void hardMatchesTheAcceptedExpertBiddingAtTwoThroughEightCards() throws Exception {
        for(int size=2;size<=8;size++) {
            var deck=new ArrayList<>(GameRules.deck(GameMode.ROMANIAN_WHIST,4));Collections.shuffle(deck,new Random(8200+size));
            var hands=new ArrayList<List<Card>>();for(int seat=0;seat<4;seat++)hands.add(deck.subList(seat*size,(seat+1)*size));
            var game=GameSession.fromDeal(new GameConfig(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,8200+size),hands,
                    size%2==0?Trump.NO_TRUMP:Trump.SPADES,3);
            var s=game.snapshot();int expected=RoundElevenAiPolicy.chooseBid(s,0,AiDifficulty.EXPERT);
            CapturedRegression.run("difficulty-hard-accepted-bid-"+size,getClass().getName(),"hardMatchesTheAcceptedEngine",Map.of(
                    "purpose","Keep the strongest accepted engine when renaming the selectable level",
                    "rule","Hard and frozen accepted R11 Expert must choose the same contract on the same public state.",
                    "handSize",size,"ownHand",hands.get(0),"scope","Version/dispatch parity; it is not an optimal-bid label for an arbitrary holding."),c->{
                c.equal("Hard equals the accepted Expert engine",expected,AiPolicy.chooseBid(s,0,AiDifficulty.HARD));
            });
        }
    }
}

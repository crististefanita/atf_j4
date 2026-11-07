package atf.android.e2e.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Explicit preconditions: JVM-played history or a controlled two-trick boundary, never a claimed UI match. */
final class RecordsFixtures {
    static final ObjectMapper JSON = new ObjectMapper();
    record Fixture(GameSession game, ObjectNode json) { }
    private RecordsFixtures() { }

    static Fixture history() {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 70231, true, 10, false), "HistUser");
        do { finishHand(game); if (game.snapshot().finalHand()) break; game.startNextHand(); } while (true);
        return new Fixture(game, encode(game, false));
    }

    static Fixture series(int magnitude) {
        var game=seriesStart(magnitude);
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat,new int[]{2,0,0,1}[seat]);
        return new Fixture(game,encode(game,false));
    }

    static Fixture activeTurn(int seat,boolean bidding) {
        if(seat<0||seat>3)throw new IllegalArgumentException("Unsupported table seat");
        var game=seriesStart(10);
        game.renameHuman("SeatUser");
        for(int index=0;index<(bidding?seat:4);index++)game.submitBid(index,new int[]{2,0,0,1}[index]);
        if(!bidding)for(int index=0;index<seat;index++)playFirst(game);
        return new Fixture(game,encode(game,false));
    }

    private static GameSession seriesStart(int magnitude) {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 70240 + magnitude,
                true, magnitude, false);
        var hands = List.of(List.of(new Card(Suit.SPADES,14),new Card(Suit.SPADES,13)),
                List.of(new Card(Suit.HEARTS,12),new Card(Suit.HEARTS,11)),
                List.of(new Card(Suit.HEARTS,10),new Card(Suit.HEARTS,9)),
                List.of(new Card(Suit.HEARTS,8),new Card(Suit.HEARTS,7)));
        var deal = GameSession.fromDeal(config,hands,Trump.SPADES,3).saveState();
        var players = deal.players().stream().map(player -> new GameSession.PlayerState(player.seat(),
                player.seat() == 0 ? "SeriesUser" : player.name(), player.human(), player.hand(), null,0,0,
                player.seat() < 2 ? 4 : -4,0)).toList();
        return GameSession.restoreComplete(new GameSession.SaveState(config,GamePhase.BIDDING,19,2,
                3,0,0,0,0,Trump.SPADES,-1,players,List.of(),List.of(),List.of(),new Card(Suit.SPADES,7)));
    }

    static Fixture finalHumanCard() {
        GameSession game = null;
        for (long index = 0; index < 100; index++) {
            long seed = 70300 + index * 100003;
            var candidate = GameSession.newGame(GameConfig.newMatch(4,GameMode.ROMANIAN_WHIST,
                    AiDifficulty.NORMAL,seed,true,10,false),"RankUser");
            if (candidate.snapshot().dealerSeat() == 1) { game = candidate; break; }
        }
        if (game == null) throw new IllegalStateException("No suitable deterministic dealer");
        while (!game.snapshot().finalHand()) { finishHand(game); game.startNextHand(); }
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(),0);
        for (int card = 0; card < 3; card++) playFirst(game);
        if (game.snapshot().currentSeat() != 0 || game.snapshot().players().get(0).hand().size() != 1)
            throw new IllegalStateException("Final fixture must require the real human UI card action");
        return new Fixture(game,encode(game,true));
    }

    static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(),0);
        while (game.snapshot().phase() == GamePhase.PLAYING) playFirst(game);
    }
    static void playFirst(GameSession game) {
        var state = game.snapshot(); var player = state.players().get(state.currentSeat());
        Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        game.playCard(player.seat(),GameRules.legalCards(player.hand(),led,state.trump(),state.mode()).get(0));
    }

    static ObjectNode encode(GameSession game, boolean eligible) {
        var state = game.saveState(); var config = state.config();
        if (config.allowZeroGame()) throw new IllegalArgumentException("Version seven fixtures require classic scoring");
        ObjectNode json = JSON.createObjectNode();
        json.put("version",7).put("playerCount",config.playerCount()).put("mode",config.mode().name())
                .put("difficulty",config.aiDifficulty().name()).put("seed",config.seed())
                .put("streakBonuses",config.streakBonuses()).put("streakBonusPoints",config.streakBonusPoints())
                .put("streakPenaltyPoints",config.streakPenaltyPoints()).put("phase",state.phase().name())
                .put("handIndex",state.handIndex()).put("handSize",state.handSize()).put("dealerSeat",state.dealerSeat())
                .put("biddingSeat",state.biddingSeat()).put("currentSeat",state.currentSeat())
                .put("bidsSubmitted",state.bidsSubmitted()).put("bidTotal",state.bidTotal())
                .put("trump",state.trump().name()).put("lastTrickWinnerSeat",state.lastTrickWinnerSeat())
                .put("rankingEligible",eligible).put("matchId","records-e2e-"+UUID.randomUUID());
        json.set("players",JSON.valueToTree(state.players()));
        json.set("currentTrick",plays(state.currentTrick()));
        json.set("lastTrick",plays(state.lastTrick()));
        json.set("history",plays(state.history()));
        json.set("turnedCard",JSON.valueToTree(state.turnedCard()));
        ObjectNode scores = json.putObject("scores");
        scores.put("priorHands",state.scoreHistory().priorHands());
        scores.set("opening",JSON.valueToTree(state.scoreHistory().openingTotals()));
        scores.set("hands",JSON.valueToTree(state.scoreHistory().closingTotals()));
        scores.set("bids",JSON.valueToTree(state.scoreHistory().recordedBids()));
        scores.set("tricks",JSON.valueToTree(state.scoreHistory().recordedTricks()));
        return json;
    }

    private static ArrayNode plays(List<PlayedCard> plays) {
        ArrayNode rows = JSON.createArrayNode();
        for (var play : plays) rows.addObject().put("seat",play.seat())
                .put("suit",play.card().suit().name()).put("rank",play.card().rank());
        return rows;
    }

    static ObjectNode rankingProfile(ObjectNode scores) {
        for (var profile : scores.withArray("profiles")) if (profile.path("mode").asText().equals("ROMANIAN_WHIST")
                && profile.path("players").asInt() == 4 && profile.path("difficulty").asText().equals("NORMAL")
                && profile.path("series").asBoolean() && profile.path("reward").asInt() == 10 && profile.path("penalty").asInt() == 10)
            return (ObjectNode)profile;
        throw new IllegalStateException("Expected baseline Romanian ±10 board");
    }

    static void addRetainedRankingPrecondition(ObjectNode scores) {
        retained(scores,8);
    }
    static void visibleTopTwenty(ObjectNode scores) {
        retained(scores,10);
    }
    private static void retained(ObjectNode scores,int count) {
        ObjectNode profile = rankingProfile(scores); ArrayNode rows = profile.withArray("entries");
        for (int index = 0; index < count; index++) {
            rows.addObject().put("name","Old"+index).put("score",-1000-index).put("ai",index%2==0)
                    .put("sample",false).put("matchId","retained-e2e-"+index).put("seat",0).put("recordedAt",1);
            profile.withArray("processedMatches").add("retained-e2e-"+index);
        }
    }
}

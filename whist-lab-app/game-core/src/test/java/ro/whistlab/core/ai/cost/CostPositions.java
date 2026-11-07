package ro.whistlab.core.ai.cost;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Preparatory legal play is outside the measured decision; no archived data or AI is used. */
public final class CostPositions {
    public enum Stage { BID_FIRST, BID_LAST, CARD_LEAD, CARD_FOLLOW, CARD_LATE }
    public record Scenario(Stage stage, GameSession.Snapshot snapshot, int actor) {
        public boolean bidding() { return stage==Stage.BID_FIRST || stage==Stage.BID_LAST; }
        public MonteCarloAi.View observation() { return MonteCarloAi.observe(snapshot,actor); }
    }
    private CostPositions() { }
    public static Scenario create(int size,int seed,Stage stage) {
        GameSession game=GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.newMatch(4,GameMode.ROMANIAN_WHIST,AiDifficulty.HARD,seed,true,10)));
        while(game.snapshot().handSize()!=size) {
            bidAll(game,seed);
            while(game.snapshot().phase()==GamePhase.PLAYING)playOne(game);
            game.startNextHand();
        }
        if(stage==Stage.BID_LAST)for(int i=0;i<3;i++)bidOne(game,seed);
        else if(stage!=Stage.BID_FIRST) {
            bidAll(game,seed);
            int plays=stage==Stage.CARD_FOLLOW?3:stage==Stage.CARD_LATE?4*(size-2):0;
            for(int i=0;i<plays;i++)playOne(game);
        }
        var snapshot=game.snapshot();
        int actor=stage==Stage.BID_FIRST||stage==Stage.BID_LAST?snapshot.biddingSeat():snapshot.currentSeat();
        return new Scenario(stage,snapshot,actor);
    }
    private static void bidAll(GameSession game,int seed) {
        while(game.snapshot().phase()==GamePhase.BIDDING)bidOne(game,seed);
    }
    private static void bidOne(GameSession game,int seed) {
        var s=game.snapshot();int actor=s.biddingSeat(),sum=0,count=0;
        for(var p:s.players())if(p.bid()!=null){sum+=p.bid();count++;}
        int bid=Math.floorMod(seed+actor,Math.min(3,s.handSize()+1));
        if(count==3&&sum+bid==s.handSize())bid=(bid+1)%(s.handSize()+1);
        game.submitBid(actor,bid);
    }
    private static void playOne(GameSession game) {
        var s=game.snapshot();int actor=s.currentSeat();
        var legal=GameRules.legalCards(s.players().get(actor).hand(),s.currentTrick().isEmpty()?null:s.currentTrick().get(0).card().suit(),s.trump(),s.mode());
        game.playCard(actor,legal.get(0));
    }
}

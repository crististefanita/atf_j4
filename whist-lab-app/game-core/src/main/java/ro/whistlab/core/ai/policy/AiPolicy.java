package ro.whistlab.core.ai.policy;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.simulation.SearchCancellation;
import ro.whistlab.core.ai.zero.ZeroMatchReserve;
import ro.whistlab.core.ai.zero.ZeroMatchSearch;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;

public final class AiPolicy {
    private AiPolicy() { }

    public static int chooseBid(GameSession.Snapshot game, int seat) {
        return chooseBid(game, seat, AiDifficulty.NORMAL);
    }

    public static int chooseBid(GameSession.Snapshot game, int seat, AiDifficulty difficulty) {
        SearchCancellation.check();
        if (game.handSize() == 1 && game.bidsSubmitted() == game.players().size() - 1) {
            var legal = GameRules.allowedBids(1, game.bidTotal(), true);
            if (legal.size() == 1) return legal.get(0);
        }
        boolean zeroEnabled = game.allowZeroGame() && game.mode().isRomanian();
        if (zeroEnabled || difficulty == AiDifficulty.HARD) {
            var view = MonteCarloAi.observe(game, seat);
            if (zeroEnabled) {
                var zero = ZeroMatchSearch.bid(view, game.handIndex());
                if (zero.isPresent()) return zero.get();
            }
            if (difficulty == AiDifficulty.HARD) return MonteCarloAi.chooseBid(view);
        }
        return NormalBidPolicy.chooseBid(NormalBidPolicy.observe(game, seat));
    }

    public static Card chooseCard(GameSession.Snapshot game, int seat) {
        return chooseCard(game, seat, AiDifficulty.NORMAL);
    }

    public static Card chooseCard(GameSession.Snapshot game, int seat, AiDifficulty difficulty) {
        SearchCancellation.check();
        boolean zeroEnabled = game.allowZeroGame() && game.mode().isRomanian();
        if (zeroEnabled || difficulty == AiDifficulty.HARD) {
            var view = MonteCarloAi.observe(game, seat);
            if (zeroEnabled) {
                var zero = ZeroMatchSearch.card(view, game.handIndex(), difficulty == AiDifficulty.HARD);
                if (zero.isPresent()) return zero.get();
            }
            if (difficulty == AiDifficulty.HARD) {
                Card conventional = MonteCarloAi.chooseCard(view).card();
                return zeroEnabled ? ZeroMatchReserve.choose(view, game.handIndex(), conventional) : conventional;
            }
        }
        return NormalPlayPolicy.chooseCard(NormalPlayPolicy.observe(game, seat));
    }
}

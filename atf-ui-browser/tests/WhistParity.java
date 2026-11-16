import java.util.List;
import java.util.Map;
import ro.cardtable.browser.BrowserGame;
import ro.whistlab.app.bidding.HumanBidSelection;
import ro.whistlab.browser.WhistBrowser;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Follows the same human commands independently in the browser adapter and native engine. */
class WhistParity {
    public static void main(String[] args) {
        int normal = compare(802451L, AiDifficulty.NORMAL, 24);
        int hard = compare(177217L, AiDifficulty.HARD, 1);
        System.out.printf("{\"name\":\"Whist · oracol JVM cu distribuiri fixe\",\"status\":\"PASS\","
                + "\"expected\":\"Aceleași decizii legale, recomandări și totaluri în adaptor și motor\","
                + "\"observed\":{\"normalHands\":24,\"normalPublicStates\":%d,\"hardHands\":1,\"hardPublicStates\":%d}}%n",
                normal, hard);
    }

    private static int compare(long seed, AiDifficulty difficulty, int hands) {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, difficulty, seed, true, 10, true);
        var oracle = GameSession.newGame(config, "Ștefan");
        var match = new WhistBrowser(() -> seed).start(Map.of("mode", config.mode().name(), "players", "4",
                "difficulty", difficulty.name(), "name", "Ștefan", "zero", "yes", "series", "10"));
        int observations = 0;
        while (observations < 256) {
            advanceOpponents(oracle, difficulty);
            var state = oracle.snapshot();
            var publicState = match.view();
            var human = state.players().get(0);
            equal(state.players().stream().map(GameSession.PlayerSnapshot::name).toList(),
                    publicState.seats().stream().map(BrowserGame.Seat::name).toList());
            equal(human.hand().stream().map(Card::shortLabel).toList(), publicState.hand().stream()
                    .map(card -> card.rank() + card.suit()).toList());
            equal(state.currentTrick().stream().map(play -> play.seat() + ":" + play.card().shortLabel()).toList(),
                    publicState.played().stream().map(play -> play.seat() + ":" + play.card().rank() + play.card().suit()).toList());
            for (int seat = 0; seat < 4; seat++) {
                equal("Levate: " + state.players().get(seat).tricksWon(), publicState.seats().get(seat).secondFact());
            }
            observations++;
            switch (state.phase()) {
                case BIDDING -> {
                    List<Integer> bids = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                            state.bidsSubmitted() == 3);
                    equal(bids.stream().map(bid -> "bid:" + bid).toList(), publicState.actions().stream()
                            .filter(action -> action.key().startsWith("bid:")).map(BrowserGame.Action::key).toList());
                    int suggested = new HumanBidSelection().request(oracle).orElseThrow().evaluate();
                    equal("bid:" + suggested, publicState.actions().stream().filter(BrowserGame.Action::emphasized)
                            .findFirst().orElseThrow().key());
                    int bid = bids.get(bids.size() - 1);
                    oracle.submitBid(0, bid);
                    match.act("bid:" + bid);
                }
                case PLAYING -> {
                    var lead = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                    var legal = GameRules.legalCards(human.hand(), lead, state.trump(), state.mode());
                    equal(legal.stream().map(WhistParity::key).toList(), publicState.hand().stream()
                            .map(BrowserGame.Card::action).filter(action -> !action.isEmpty()).toList());
                    var card = legal.get(legal.size() - 1);
                    oracle.playCard(0, card);
                    match.act(key(card));
                }
                case SCORE -> {
                    equal(state.handNumber(), publicState.history().rows().size());
                    var last = publicState.history().rows().get(state.handIndex());
                    for (int seat = 0; seat < 4; seat++) {
                        equal(oracle.scoreHistory().bid(state.handNumber(), seat) + " / "
                                + oracle.scoreHistory().total(state.handNumber(), seat), last.get(seat + 1));
                    }
                    if (state.handNumber() == hands) return observations;
                    oracle.startNextHand();
                    match.act("next");
                }
            }
        }
        throw new AssertionError("Whist parity exceeded the match limit");
    }

    private static String key(Card card) { return "card:" + card.suit().name() + ":" + card.rank(); }

    private static void advanceOpponents(GameSession oracle, AiDifficulty difficulty) {
        while (true) {
            var state = oracle.snapshot();
            switch (state.phase()) {
                case BIDDING -> {
                    int actor = state.biddingSeat();
                    if (actor == 0) return;
                    oracle.submitBid(actor, AiPolicy.chooseBid(state, actor, difficulty));
                }
                case PLAYING -> {
                    int actor = state.currentSeat();
                    if (actor == 0) return;
                    oracle.playCard(actor, AiPolicy.chooseCard(state, actor, difficulty));
                }
                case SCORE -> { return; }
            }
        }
    }

    private static void equal(Object expected, Object observed) {
        if (!expected.equals(observed)) throw new AssertionError("Expected " + expected + ", observed " + observed);
    }
}

package ro.whistlab.core.ai.zero;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GameSession.PlayerSnapshot;
import ro.whistlab.core.session.GameSession.Snapshot;

final class ZeroMatchPair {
    record Case(GameMode mode, int players, long seed, int hardSeat, boolean series, Integer pressureSeat) {
        String id() { return mode.name() + "-" + players + "-" + seed; }
        AiDifficulty level(int seat) { return seat == hardSeat ? AiDifficulty.HARD : AiDifficulty.NORMAL; }
        boolean pressurePrefix(int handNumber) {
            return pressureSeat != null && handNumber <= GameRules.handSizes(mode, players).size() - 5;
        }
    }

    record Pivot(int hand, int cards, int seat, GamePhase phase, int cardsPlayed,
            boolean selected, String action, List<Integer> totals, List<Integer> bids, List<Integer> tricks) { }

    record Hand(int number, int size, int dealer, String trump, String turnedCard,
            String dealDigest, List<Integer> bids, List<String> plays, List<Integer> tricks,
            List<Integer> series, List<Integer> totals) { }

    record SeatResult(int seat, AiDifficulty difficulty, int raw, int finalScore, int rank,
            int eligible, int selectedBids, int selectedCards, Integer firstHand, GamePhase firstPhase) { }

    record Arm(boolean enabled, List<Hand> hands, List<Pivot> pivots, List<SeatResult> seats) {
        List<String> deals() { return hands.stream().map(Hand::dealDigest).toList(); }
    }

    record Pair(Case scenario, Arm off, Arm on) { }

    private ZeroMatchPair() { }

    static Pair play(Case scenario) {
        return new Pair(scenario, play(scenario, false), play(scenario, true));
    }

    private static Arm play(Case scenario, boolean enabled) {
        var config = GameConfig.newMatch(scenario.players(), scenario.mode(), AiDifficulty.NORMAL,
                scenario.seed(), scenario.series(), 10, enabled);
        var game = GameSession.newGame(config);
        var hands = new ArrayList<Hand>();
        var pivots = new ArrayList<Pivot>();
        while (true) {
            Snapshot start = game.snapshot();
            while (game.snapshot().phase() != GamePhase.SCORE) move(scenario, game, pivots);
            Snapshot end = game.snapshot();
            hands.add(new Hand(end.handNumber(), end.handSize(), end.dealerSeat(), end.trump().name(),
                    label(end.turnedCard()), dealDigest(start), bids(end),
                    end.history().stream().map(play -> play.seat() + ":" + label(play.card())).toList(),
                    end.players().stream().map(PlayerSnapshot::tricksWon).toList(),
                    end.players().stream().map(PlayerSnapshot::streakPoints).toList(), totals(end)));
            if (end.finalHand()) return new Arm(enabled, List.copyOf(hands), List.copyOf(pivots),
                    end.players().stream().map(player -> result(scenario, end, player, pivots)).toList());
            game.startNextHand();
        }
    }

    private static void move(Case scenario, GameSession game, List<Pivot> pivots) {
        Snapshot state = game.snapshot();
        boolean bidding = state.phase() == GamePhase.BIDDING;
        int seat = bidding ? state.biddingSeat() : state.currentSeat();
        boolean prefix = scenario.pressurePrefix(state.handNumber());
        Snapshot observed = prefix && state.allowZeroGame() ? classicObservation(state) : state;
        String action;
        try (var work = SearchWork.measure(Long.MAX_VALUE)) {
            if (bidding) {
                int bid = AiPolicy.chooseBid(observed, seat, scenario.level(seat));
                if (prefix && scenario.pressureSeat() == seat && state.players().get(seat).totalScore() > 2) {
                    // A legal adverse prefix keeps one real player near zero. Both arms use the same prefix.
                    var legal = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                            state.bidsSubmitted() == scenario.players() - 1);
                    int high = legal.get(legal.size() - 1);
                    int low = legal.get(0);
                    bid = high - bid >= bid - low ? high : low;
                }
                game.submitBid(seat, bid);
                action = Integer.toString(bid);
            } else {
                Card card = AiPolicy.chooseCard(observed, seat, scenario.level(seat));
                game.playCard(seat, card);
                action = label(card);
            }
            var events = work.snapshot().events();
            if (events.get(SearchWork.Event.ZERO_ELIGIBLE) > 0) {
                pivots.add(new Pivot(state.handNumber(), state.handSize(), seat, state.phase(),
                        state.history().size(), events.get(SearchWork.Event.ZERO_SELECTED) > 0,
                        action, totals(state), bids(state), state.players().stream().map(PlayerSnapshot::tricksWon).toList()));
            }
        }
    }

    private static Snapshot classicObservation(Snapshot state) {
        return new Snapshot(state.phase(), state.handIndex(), state.handNumber(), state.handSize(), state.trump(),
                state.dealerSeat(), state.biddingSeat(), state.currentSeat(), state.bidsSubmitted(), state.bidTotal(),
                state.players(), state.currentTrick(), state.lastTrick(), state.lastTrickWinnerSeat(),
                state.finalHand(), state.mode(), state.history(), state.turnedCard(), state.streakBonuses(),
                state.streakBonusPoints(), state.streakPenaltyPoints());
    }

    private static SeatResult result(Case scenario, Snapshot end, PlayerSnapshot player, List<Pivot> pivots) {
        int seat = player.seat();
        var eligible = pivots.stream().filter(pivot -> pivot.seat() == seat).toList();
        var selected = eligible.stream().filter(Pivot::selected).toList();
        int selectedBids = (int) selected.stream().filter(pivot -> pivot.phase() == GamePhase.BIDDING).count();
        int finalScore = end.matchScore(seat);
        int rank = 1 + (int) end.players().stream().filter(other -> end.matchScore(other.seat()) > finalScore).count();
        return new SeatResult(seat, scenario.level(seat), player.totalScore(), finalScore, rank,
                eligible.size(), selectedBids, selected.size() - selectedBids,
                selected.isEmpty() ? null : selected.get(0).hand(), selected.isEmpty() ? null : selected.get(0).phase());
    }

    private static List<Integer> totals(Snapshot state) {
        return state.players().stream().map(PlayerSnapshot::totalScore).toList();
    }

    private static List<Integer> bids(Snapshot state) {
        return state.players().stream().map(PlayerSnapshot::bid).toList();
    }

    private static String label(Card card) {
        return card == null ? null : card.suit().name() + "-" + card.rank();
    }

    private static String dealDigest(Snapshot state) {
        String deal = state.dealerSeat() + "|" + state.trump() + "|" + label(state.turnedCard()) + "|"
                + state.players().stream().map(player -> player.hand().stream().map(ZeroMatchPair::label).toList()).toList();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(deal.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

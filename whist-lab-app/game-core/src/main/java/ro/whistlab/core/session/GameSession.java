package ro.whistlab.core.session;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.ImmutableLists;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;
import ro.whistlab.core.session.scoring.PlayerScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Match lifecycle and the stable public projection of the current hand. */
public final class GameSession {
    private final GameConfig config;
    private final List<Integer> handSizes;
    private final List<String> names;
    private List<PlayerScore> scores;
    private ScoreHistory scoreHistory;
    private int handIndex;
    private HandPlay hand;

    GameSession(GameConfig config, List<Integer> handSizes, List<String> names, int handIndex,
                HandPlay hand, List<PlayerScore> scores, ScoreHistory scoreHistory) {
        this.config = config;
        this.handSizes = handSizes;
        this.names = new ArrayList<>(names);
        this.handIndex = handIndex;
        this.hand = hand;
        this.scores = scores;
        this.scoreHistory = scoreHistory;
    }

    public static GameSession newGame(GameConfig config) {
        return newGame(config, "You");
    }

    public static GameSession newGame(GameConfig config, String humanName) {
        Objects.requireNonNull(config, "config");
        int count = config.playerCount();
        int dealer = config.mode().usesTurnedTrump() ? new Random(config.seed()).nextInt(count) : count - 1;
        var sizes = GameRules.handSizes(config.mode(), count);
        return open(config, sizes, humanName, HandPlay.deal(config, 0, sizes.get(0), dealer));
    }

    public static GameSession fromDeal(GameConfig config, List<List<Card>> hands, Trump trump, int dealerSeat) {
        var hand = HandPlay.fromDeal(config, hands, trump, dealerSeat);
        return open(config, GameRules.handSizes(config.mode(), config.playerCount()), "You", hand);
    }

    private static GameSession open(GameConfig config, List<Integer> sizes, String humanName, HandPlay hand) {
        int count = config.playerCount();
        return new GameSession(config, sizes, PlayerNames.forMatch(count, humanName, config.seed()), 0, hand,
                Collections.nCopies(count, new PlayerScore(0, 0, 0)), ScoreHistory.newMatch(count));
    }

    /** Restores a validated, immutable checkpoint produced by {@link #saveState()}. */
    public static GameSession restoreComplete(SaveState saved) {
        return SessionCheckpoint.restore(saved, true);
    }

    /** Also reads legacy checkpoints that retained only the last/current trick. */
    public static GameSession restore(SaveState saved) {
        return SessionCheckpoint.restore(saved, false);
    }

    public void renameHuman(String name) {
        String normalized = PlayerNames.normalize(name);
        if (names.subList(1, names.size()).stream().anyMatch(normalized::equalsIgnoreCase))
            throw new IllegalArgumentException("That name is already used at this table");
        names.set(0, normalized);
    }

    public void submitBid(int seat, int bid) {
        hand.submitBid(seat, bid);
    }

    public void playCard(int seat, Card card) {
        if (hand.playCard(seat, card)) settleHand();
    }

    public void startNextHand() {
        hand.requirePhase(GamePhase.SCORE);
        if (handIndex + 1 >= handSizes.size()) throw new IllegalStateException("The game is complete");
        int dealer = (hand.dealerSeat() + 1) % config.playerCount();
        handIndex++;
        hand = HandPlay.deal(config, handIndex, handSizes.get(handIndex), dealer);
        scores = scores.stream().map(PlayerScore::beginHand).collect(java.util.stream.Collectors.toList());
    }

    private void settleHand() {
        var totals = new ArrayList<Integer>(names.size());
        var bids = new ArrayList<Integer>(names.size());
        var tricks = new ArrayList<Integer>(names.size());
        var next = new ArrayList<PlayerScore>(names.size());
        for (int seat = 0; seat < names.size(); seat++) {
            Integer bid = hand.bid(seat);
            int won = hand.tricksWon(seat);
            var score = scores.get(seat).settle(config, hand.size(), bid, won);
            next.add(score);
            totals.add(score.total());
            bids.add(bid);
            tricks.add(won);
        }
        scoreHistory = scoreHistory.finish(handIndex + 1, totals, bids, tricks);
        scores = next;
        hand.markScored();
    }

    public Snapshot snapshot() {
        var players = new ArrayList<PlayerSnapshot>(names.size());
        for (int seat = 0; seat < names.size(); seat++) {
            var score = scores.get(seat);
            players.add(new PlayerSnapshot(seat, names.get(seat), seat == 0, hand.cards(seat),
                    hand.bid(seat), hand.tricksWon(seat), score.total(), score.streak(), score.streakPoints()));
        }
        return new Snapshot(hand.phase(), handIndex, handIndex + 1, hand.size(), hand.trump(), hand.dealerSeat(),
                hand.biddingSeat(), hand.currentSeat(), hand.bidsSubmitted(), hand.bidTotal(), players,
                hand.currentTrick(), hand.lastTrick(), hand.lastTrickWinnerSeat(), handIndex + 1 == handSizes.size(),
                config.mode(), hand.history(), hand.turnedCard(), config.streakBonuses(), config.streakBonusPoints(),
                config.streakPenaltyPoints(), config.allowZeroGame());
    }

    public SaveState saveState() {
        return SessionCheckpoint.capture(config, handIndex, names, hand, scores, scoreHistory);
    }

    public ScoreHistory scoreHistory() { return scoreHistory; }

    public GameConfig config() { return config; }

    public record PlayerSnapshot(
            int seat,
            String name,
            boolean human,
            List<Card> hand,
            Integer bid,
            int tricksWon,
            int totalScore,
            int streak,
            int streakPoints) {
        public PlayerSnapshot(
                int seat,
                String name,
                boolean human,
                List<Card> hand,
                Integer bid,
                int tricksWon,
                int totalScore) {
            this(seat, name, human, hand, bid, tricksWon, totalScore, 0, 0);
        }

        public PlayerSnapshot {
            hand = ImmutableLists.copy(hand);
        }
    }

    public record Snapshot(
            GamePhase phase,
            int handIndex,
            int handNumber,
            int handSize,
            Trump trump,
            int dealerSeat,
            int biddingSeat,
            int currentSeat,
            int bidsSubmitted,
            int bidTotal,
            List<PlayerSnapshot> players,
            List<PlayedCard> currentTrick,
            List<PlayedCard> lastTrick,
            int lastTrickWinnerSeat,
            boolean finalHand,
            GameMode mode,
            List<PlayedCard> history,
            Card turnedCard,
            boolean streakBonuses,
            int streakBonusPoints, int streakPenaltyPoints, boolean allowZeroGame) {
        public Snapshot(GamePhase phase, int handIndex, int handNumber, int handSize, Trump trump,
                int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted, int bidTotal,
                List<PlayerSnapshot> players, List<PlayedCard> currentTrick, List<PlayedCard> lastTrick,
                int lastTrickWinnerSeat, boolean finalHand, GameMode mode, List<PlayedCard> history,
                Card turnedCard, boolean streakBonuses, int streakBonusPoints, int streakPenaltyPoints) {
            this(phase, handIndex, handNumber, handSize, trump, dealerSeat, biddingSeat, currentSeat,
                    bidsSubmitted, bidTotal, players, currentTrick, lastTrick, lastTrickWinnerSeat,
                    finalHand, mode, history, turnedCard, streakBonuses, streakBonusPoints,
                    streakPenaltyPoints, false);
        }

        /** Legacy projections retain their historical five-point penalty. */
        public Snapshot(
            GamePhase phase,
            int handIndex,
            int handNumber,
            int handSize,
            Trump trump,
            int dealerSeat,
            int biddingSeat,
            int currentSeat,
            int bidsSubmitted,
            int bidTotal,
            List<PlayerSnapshot> players,
            List<PlayedCard> currentTrick,
            List<PlayedCard> lastTrick,
            int lastTrickWinnerSeat,
            boolean finalHand,
            GameMode mode,
            List<PlayedCard> history,
            Card turnedCard,
            boolean streakBonuses,
            int streakBonusPoints) {
            this(phase, handIndex, handNumber, handSize, trump, dealerSeat, biddingSeat, currentSeat, bidsSubmitted, bidTotal, players, currentTrick, lastTrick, lastTrickWinnerSeat, finalHand, mode, history, turnedCard, streakBonuses, streakBonusPoints, 5);
        }
        public Snapshot {
            players = ImmutableLists.copy(players);
            currentTrick = ImmutableLists.copy(currentTrick);
            lastTrick = ImmutableLists.copy(lastTrick);
            history = ImmutableLists.copy(history);
        }

        public boolean earnedZeroGame(int seat) {
            return allowZeroGame && mode.isRomanian() && finalHand && phase == GamePhase.SCORE
                    && players.get(seat).totalScore() == 0;
        }

        public int matchScore(int seat) {
            return earnedZeroGame(seat) ? RomanianScoring.ZERO_GAME_SCORE : players.get(seat).totalScore();
        }
    }

    public record PlayerState(
            int seat,
            String name,
            boolean human,
            List<Card> hand,
            Integer bid,
            int tricksWon,
            int totalScore,
            int streak,
            int streakPoints) {
        public PlayerState(
                int seat,
                String name,
                boolean human,
                List<Card> hand,
                Integer bid,
                int tricksWon,
                int totalScore) {
            this(seat, name, human, hand, bid, tricksWon, totalScore, 0, 0);
        }

        public PlayerState {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) throw new IllegalArgumentException("Player name cannot be blank");
            hand = immutableCopy(hand);
        }
    }

    public record SaveState(
            GameConfig config,
            GamePhase phase,
            int handIndex,
            int handSize,
            int dealerSeat,
            int biddingSeat,
            int currentSeat,
            int bidsSubmitted,
            int bidTotal,
            Trump trump,
            int lastTrickWinnerSeat,
            List<PlayerState> players,
            List<PlayedCard> currentTrick,
            List<PlayedCard> lastTrick,
            List<PlayedCard> history,
            Card turnedCard,
            ScoreHistory scoreHistory) {
        public SaveState(
                GameConfig config,
                GamePhase phase,
                int handIndex,
                int handSize,
                int dealerSeat,
                int biddingSeat,
                int currentSeat,
                int bidsSubmitted,
                int bidTotal,
                Trump trump,
                int lastTrickWinnerSeat,
                List<PlayerState> players,
                List<PlayedCard> currentTrick,
                List<PlayedCard> lastTrick,
                List<PlayedCard> history,
                Card turnedCard) {
            this(
                    config,
                    phase,
                    handIndex,
                    handSize,
                    dealerSeat,
                    biddingSeat,
                    currentSeat,
                    bidsSubmitted,
                    bidTotal,
                    trump,
                    lastTrickWinnerSeat,
                    players,
                    currentTrick,
                    lastTrick,
                    history,
                    turnedCard,
                    null);
        }

        public SaveState(
                GameConfig config,
                GamePhase phase,
                int handIndex,
                int handSize,
                int dealerSeat,
                int biddingSeat,
                int currentSeat,
                int bidsSubmitted,
                int bidTotal,
                Trump trump,
                int lastTrickWinnerSeat,
                List<PlayerState> players,
                List<PlayedCard> currentTrick,
                List<PlayedCard> lastTrick,
                List<PlayedCard> history) {
            this(
                    config,
                    phase,
                    handIndex,
                    handSize,
                    dealerSeat,
                    biddingSeat,
                    currentSeat,
                    bidsSubmitted,
                    bidTotal,
                    trump,
                    lastTrickWinnerSeat,
                    players,
                    currentTrick,
                    lastTrick,
                    history,
                    null);
        }

        /** Compatibility for callers constructing initial or pre-history checkpoints. */
        public SaveState(
                GameConfig config,
                GamePhase phase,
                int handIndex,
                int handSize,
                int dealerSeat,
                int biddingSeat,
                int currentSeat,
                int bidsSubmitted,
                int bidTotal,
                Trump trump,
                int lastTrickWinnerSeat,
                List<PlayerState> players,
                List<PlayedCard> currentTrick,
                List<PlayedCard> lastTrick) {
            this(
                    config,
                    phase,
                    handIndex,
                    handSize,
                    dealerSeat,
                    biddingSeat,
                    currentSeat,
                    bidsSubmitted,
                    bidTotal,
                    trump,
                    lastTrickWinnerSeat,
                    players,
                    currentTrick,
                    lastTrick,
                    knownHistory(lastTrick, currentTrick),
                    null);
        }

        public SaveState {
            Objects.requireNonNull(config, "config");
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(trump, "trump");
            players = immutableCopy(players);
            currentTrick = immutableCopy(currentTrick);
            lastTrick = immutableCopy(lastTrick);
            history = immutableCopy(history);
        }
    }

    private static List<PlayedCard> knownHistory(List<PlayedCard> last, List<PlayedCard> current) {
        List<PlayedCard> result = new ArrayList<>(last);
        result.addAll(current);
        return result;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return ImmutableLists.copy(Objects.requireNonNull(values, "values"));
    }
}

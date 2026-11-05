package ro.whistlab.core.session;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.ImmutableLists;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class GameSession {
    private final GameConfig config;
    private final List<Integer> handSizes;
    private final List<Player> players;
    private ScoreHistory scoreHistory;
    private int handIndex;
    private int handSize;
    private int dealerSeat;
    private int biddingSeat;
    private int currentSeat;
    private int bidsSubmitted;
    private int bidTotal;
    private int lastTrickWinnerSeat = -1;
    private Trump trump;
    private Card turnedCard;
    private GamePhase phase;
    private final List<PlayedCard> trick = new ArrayList<>();
    private final List<PlayedCard> history = new ArrayList<>();
    private List<PlayedCard> lastTrick = java.util.Collections.emptyList();

    private GameSession(GameConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.handSizes = GameRules.handSizes(config.mode(), config.playerCount());
        this.players = new ArrayList<>(config.playerCount());
        this.scoreHistory = ScoreHistory.newMatch(config.playerCount());
        var names = ro.whistlab.core.player.PlayerNames.forMatch(config.playerCount(), "You", config.seed());
        players.add(new Player(0, names.get(0), true));
        for (int seat = 1; seat < config.playerCount(); seat++) {
            players.add(new Player(seat, names.get(seat), false));
        }
        dealerSeat =
                config.mode().usesTurnedTrump()
                        ? new Random(config.seed()).nextInt(config.playerCount())
                        : config.playerCount() - 1;
    }

    public static GameSession newGame(GameConfig config) {
        GameSession session = new GameSession(config);
        session.beginHand();
        return session;
    }

    public static GameSession newGame(GameConfig config, String humanName) {
        GameSession session = newGame(config);
        var names = ro.whistlab.core.player.PlayerNames.forMatch(config.playerCount(), humanName, config.seed());
        for (int seat = 0; seat < names.size(); seat++) session.players.get(seat).name = names.get(seat);
        return session;
    }

    public void renameHuman(String name) {
        String normalized = ro.whistlab.core.player.PlayerNames.normalize(name);
        if (players.stream().anyMatch(player -> !player.human && player.name.equalsIgnoreCase(normalized)))
            throw new IllegalArgumentException("That name is already used at this table");
        players.get(0).name = normalized;
    }

    public static GameSession fromDeal(
            GameConfig config, List<List<Card>> hands, Trump trump, int dealerSeat) {
        Objects.requireNonNull(hands, "hands");
        Objects.requireNonNull(trump, "trump");
        if (hands.size() != config.playerCount()) {
            throw new IllegalArgumentException("Deal must have one hand for every player");
        }
        if (dealerSeat < 0 || dealerSeat >= config.playerCount()) {
            throw new IllegalArgumentException("Dealer seat is outside the table");
        }
        int handSize = hands.get(0).size();
        if (handSize < 1 || hands.stream().anyMatch(hand -> hand.size() != handSize)) {
            throw new IllegalArgumentException(
                    "All players must receive the same positive hand size");
        }
        Set<Card> validCards = new HashSet<>(GameRules.deck(config.mode(), config.playerCount()));
        if (handSize * config.playerCount() > validCards.size())
            throw new IllegalArgumentException("Deal exceeds the configured deck");
        Set<Card> unique = new HashSet<>();
        for (List<Card> hand : hands) {
            for (Card card : hand) {
                if (!validCards.contains(card))
                    throw new IllegalArgumentException(
                            "Card is outside the configured deck: " + card);
                if (!unique.add(card)) {
                    throw new IllegalArgumentException(
                            "A card appears more than once in the deal: " + card);
                }
            }
        }

        GameSession session = new GameSession(config);
        session.handSize = handSize;
        session.dealerSeat = dealerSeat;
        session.trump = trump;
        for (int seat = 0; seat < hands.size(); seat++) {
            session.players.get(seat).hand.addAll(hands.get(seat));
            sortHand(session.players.get(seat).hand);
        }
        session.prepareBiddingAndPlay();
        return session;
    }

    /** Restores a validated, immutable checkpoint produced by {@link #saveState()}. */
    public static GameSession restoreComplete(SaveState saved) {
        Objects.requireNonNull(saved, "saved");
        int remaining = saved.players().stream().mapToInt(p -> p.hand().size()).sum();
        if (saved.history().size() != saved.handSize() * saved.config().playerCount() - remaining)
            throw new IllegalArgumentException(
                    "Complete checkpoint is missing public card history");
        return restore(saved);
    }

    /** Also reads legacy checkpoints that retained only the last/current trick. */
    public static GameSession restore(SaveState saved) {
        Objects.requireNonNull(saved, "saved");
        GameSession session = new GameSession(saved.config());
        CheckpointValidation.header(saved, session.handSizes);
        int playerCount = saved.config().playerCount();

        session.handIndex = saved.handIndex();
        session.handSize = saved.handSize();
        session.dealerSeat = saved.dealerSeat();
        session.biddingSeat = saved.biddingSeat();
        session.currentSeat = saved.currentSeat();
        session.bidsSubmitted = saved.bidsSubmitted();
        session.bidTotal = saved.bidTotal();
        session.lastTrickWinnerSeat = saved.lastTrickWinnerSeat();
        session.trump = Objects.requireNonNull(saved.trump(), "saved trump");
        session.turnedCard = saved.turnedCard();
        session.phase = Objects.requireNonNull(saved.phase(), "saved phase");
        session.trick.addAll(saved.currentTrick());
        session.lastTrick = immutableCopy(saved.lastTrick());
        session.history.addAll(saved.history());
        session.players.clear();

        int countedBids = 0;
        for (int seat = 0; seat < playerCount; seat++) {
            PlayerState source = saved.players().get(seat);
            CheckpointValidation.player(saved, source, seat);
            Player player = new Player(seat, source.name(), source.human());
            player.hand.addAll(source.hand());
            player.bid = source.bid();
            player.tricksWon = source.tricksWon();
            player.score = source.totalScore();
            player.streak = source.streak();
            player.streakPoints = source.streakPoints();

            if (player.bid != null) countedBids++;
            session.players.add(player);
        }
        CheckpointValidation.progress(saved, countedBids);
        CheckpointValidation.rules(saved);
        session.scoreHistory = CheckpointScores.restore(saved, session.handSizes);
        return session;
    }

    private List<Integer> playerBids() {
        return players.stream()
                .map(player -> player.bid)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<Integer> playerTotals() {
        return players.stream()
                .map(player -> player.score)
                .collect(java.util.stream.Collectors.toList());
    }

    public ScoreHistory scoreHistory() {
        return scoreHistory;
    }

    public void submitBid(int seat, int bid) {
        requirePhase(GamePhase.BIDDING);
        if (seat != biddingSeat) {
            throw new IllegalStateException(
                    "Seat " + seat + " cannot bid now; expected seat " + biddingSeat);
        }
        if (bid < 0 || bid > handSize) {
            throw new IllegalArgumentException("Bid must be between 0 and " + handSize);
        }
        boolean lastBid = bidsSubmitted == players.size() - 1;
        if (lastBid && GameRules.isForbiddenLastBid(bidTotal, bid, handSize)) {
            throw new IllegalArgumentException(
                    "The last bid cannot make total bids equal the hand size");
        }
        players.get(seat).bid = bid;
        bidTotal += bid;
        bidsSubmitted++;
        if (bidsSubmitted == players.size()) {
            phase = GamePhase.PLAYING;
            currentSeat = nextSeat(dealerSeat);
        } else {
            biddingSeat = nextSeat(seat);
        }
    }

    public void playCard(int seat, Card card) {
        requirePhase(GamePhase.PLAYING);
        Objects.requireNonNull(card, "card");
        if (seat != currentSeat) {
            throw new IllegalStateException(
                    "Seat " + seat + " cannot play now; expected seat " + currentSeat);
        }
        Player player = players.get(seat);
        if (!player.hand.contains(card)) {
            throw new IllegalArgumentException("Card is not in the player's hand: " + card);
        }
        Suit ledSuit = trick.isEmpty() ? null : trick.get(0).card().suit();
        if (!GameRules.legalCards(player.hand, ledSuit, trump, config.mode()).contains(card)) {
            throw new IllegalArgumentException(
                    "Follow the led suit; Romanian Whist requires trump when void and trump is held");
        }

        lastTrick = java.util.Collections.emptyList();
        lastTrickWinnerSeat = -1;
        player.hand.remove(card);
        trick.add(new PlayedCard(seat, card));
        history.add(new PlayedCard(seat, card));
        if (trick.size() < players.size()) {
            currentSeat = nextSeat(seat);
            return;
        }

        lastTrick = ImmutableLists.copy(trick);
        lastTrickWinnerSeat = GameRules.winningSeat(lastTrick, trump);
        players.get(lastTrickWinnerSeat).tricksWon++;
        trick.clear();
        if (players.stream().allMatch(p -> p.hand.isEmpty())) {
            scoreHand();
        } else {
            currentSeat = lastTrickWinnerSeat;
        }
    }

    public void startNextHand() {
        requirePhase(GamePhase.SCORE);
        if (handIndex + 1 >= handSizes.size()) {
            throw new IllegalStateException("The game is complete");
        }
        handIndex++;
        dealerSeat = nextSeat(dealerSeat);
        beginHand();
    }

    public Snapshot snapshot() {
        List<PlayerSnapshot> playerSnapshots =
                players.stream()
                        .map(
                                player ->
                                        new PlayerSnapshot(
                                                player.seat,
                                                player.name,
                                                player.human,
                                                player.hand,
                                                player.bid,
                                                player.tricksWon,
                                                player.score,
                                                player.streak,
                                                player.streakPoints))
                        .collect(java.util.stream.Collectors.toList());
        return new Snapshot(
                phase,
                handIndex,
                handIndex + 1,
                handSize,
                trump,
                dealerSeat,
                biddingSeat,
                currentSeat,
                bidsSubmitted,
                bidTotal,
                playerSnapshots,
                trick,
                lastTrick,
                lastTrickWinnerSeat,
                handIndex + 1 == handSizes.size(),
                config.mode(),
                history,
                turnedCard,
                config.streakBonuses(),
                config.streakBonusPoints(), config.streakPenaltyPoints());
    }

    public SaveState saveState() {
        List<PlayerState> savedPlayers =
                players.stream()
                        .map(
                                player ->
                                        new PlayerState(
                                                player.seat,
                                                player.name,
                                                player.human,
                                                player.hand,
                                                player.bid,
                                                player.tricksWon,
                                                player.score,
                                                player.streak,
                                                player.streakPoints))
                        .collect(java.util.stream.Collectors.toList());
        return new SaveState(
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
                savedPlayers,
                trick,
                lastTrick,
                history,
                turnedCard,
                scoreHistory);
    }

    public GameConfig config() {
        return config;
    }

    private void beginHand() {
        handSize = handSizes.get(handIndex);
        turnedCard = null;
        trump = Trump.forHandIndex(handIndex);
        for (Player player : players) {
            player.hand.clear();
            player.bid = null;
            player.tricksWon = 0;
            player.streakPoints = 0;
        }

        List<Card> deck = new ArrayList<>(GameRules.deck(config.mode(), players.size()));
        long handSeed = config.seed() + 0x9E3779B97F4A7C15L * handIndex;
        Collections.shuffle(deck, new Random(handSeed));
        int firstSeat = nextSeat(dealerSeat);
        int cardsToDeal = handSize * players.size();
        for (int index = 0; index < cardsToDeal; index++) {
            int seat = (firstSeat + index) % players.size();
            players.get(seat).hand.add(deck.get(index));
        }
        if (config.mode().isRomanian() && cardsToDeal == deck.size()) {
            trump = Trump.NO_TRUMP;
        } else if (config.mode().usesTurnedTrump()) {
            // The first undealt card determines Oh Hell trump; cycle applies to Descending.
            turnedCard = deck.get(cardsToDeal);
            trump = Trump.values()[turnedCard.suit().ordinal()];
        }
        players.forEach(player -> sortHand(player.hand));
        prepareBiddingAndPlay();
    }

    private void prepareBiddingAndPlay() {
        trick.clear();
        history.clear();
        lastTrick = java.util.Collections.emptyList();
        lastTrickWinnerSeat = -1;
        bidsSubmitted = 0;
        bidTotal = 0;
        phase = GamePhase.BIDDING;
        biddingSeat = nextSeat(dealerSeat);
        currentSeat = nextSeat(dealerSeat);
    }

    private void scoreHand() {
        for (Player player : players) {
            if (player.bid == null) {
                throw new IllegalStateException(
                        "Cannot score a hand before all bids are submitted");
            }
            if (config.streakBonuses()) {
                RomanianScoring.Result result =
                        RomanianScoring.score(
                                player.tricksWon,
                                player.bid,
                                handSize,
                                player.streak,
                                config.streakBonusPoints(), config.streakPenaltyPoints());
                player.score += result.totalPoints();
                player.streak = result.nextStreak();
                player.streakPoints = result.streakPoints();
            } else
                player.score += GameRules.scoreDelta(player.tricksWon, player.bid, config.mode());
        }
        scoreHistory = scoreHistory.finish(handIndex + 1, playerTotals(), playerBids());
        phase = GamePhase.SCORE;
    }

    private int nextSeat(int seat) {
        return (seat + 1) % players.size();
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new IllegalStateException(
                    "Expected phase " + expected + " but game is in " + phase);
        }
    }

    private static void sortHand(List<Card> hand) {
        hand.sort(java.util.Comparator.comparing(Card::suit).thenComparingInt(Card::rank));
    }

    private static final class Player {
        private final int seat;
        private String name;
        private final boolean human;
        private final List<Card> hand = new ArrayList<>();
        private Integer bid;
        private int tricksWon;
        private int score;
        private int streak;
        private int streakPoints;

        private Player(int seat, String name, boolean human) {
            this.seat = seat;
            this.name = name;
            this.human = human;
        }
    }

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
            int streakBonusPoints, int streakPenaltyPoints) {
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

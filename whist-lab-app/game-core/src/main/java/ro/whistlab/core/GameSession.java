package ro.whistlab.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class GameSession {
    private static final List<String> AI_NAMES =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                    "Ava", "Miles", "Grace", "Noah", "Lily"));

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
        players.add(new Player(0, "You", true));
        for (int seat = 1; seat < config.playerCount(); seat++) {
            players.add(new Player(seat, AI_NAMES.get(seat - 1), false));
        }
        dealerSeat = config.mode().usesTurnedTrump()
                ? new Random(config.seed()).nextInt(config.playerCount()) : config.playerCount() - 1;
    }

    public static GameSession newGame(GameConfig config) {
        GameSession session = new GameSession(config);
        session.beginHand();
        return session;
    }

    public static GameSession fromDeal(GameConfig config, List<List<Card>> hands, Trump trump, int dealerSeat) {
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
            throw new IllegalArgumentException("All players must receive the same positive hand size");
        }
        Set<Card> validCards = new HashSet<>(GameRules.deck(config.mode(), config.playerCount()));
        if (handSize * config.playerCount() > validCards.size())
            throw new IllegalArgumentException("Deal exceeds the configured deck");
        Set<Card> unique = new HashSet<>();
        for (List<Card> hand : hands) {
            for (Card card : hand) {
                if (!validCards.contains(card)) throw new IllegalArgumentException("Card is outside the configured deck: " + card);
                if (!unique.add(card)) {
                    throw new IllegalArgumentException("A card appears more than once in the deal: " + card);
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
        Objects.requireNonNull(saved,"saved");
        int remaining=saved.players().stream().mapToInt(p->p.hand().size()).sum();
        if(saved.history().size()!=saved.handSize()*saved.config().playerCount()-remaining)
            throw new IllegalArgumentException("Complete checkpoint is missing public card history");
        return restore(saved);
    }

    /** Also reads legacy checkpoints that retained only the last/current trick. */
    public static GameSession restore(SaveState saved) {
        Objects.requireNonNull(saved, "saved");
        GameSession session = new GameSession(saved.config());
        if (saved.handIndex() < 0 || saved.handIndex() >= session.handSizes.size()) {
            throw new IllegalArgumentException("Saved hand index is outside the configured game");
        }
        if (saved.handSize() != session.handSizes.get(saved.handIndex())) {
            throw new IllegalArgumentException("Saved hand size does not match the configured game mode");
        }
        int playerCount = saved.config().playerCount();
        if (saved.players().size() != playerCount) {
            throw new IllegalArgumentException("Saved player count does not match the game configuration");
        }
        requireSeat(saved.dealerSeat(), playerCount, "dealer");
        requireSeat(saved.biddingSeat(), playerCount, "bidding");
        requireSeat(saved.currentSeat(), playerCount, "current player");
        if (saved.bidsSubmitted() < 0 || saved.bidsSubmitted() > playerCount
                || saved.lastTrickWinnerSeat() < -1 || saved.lastTrickWinnerSeat() >= playerCount) {
            throw new IllegalArgumentException("Saved turn counters are invalid");
        }

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
            if (source.seat() != seat || source.human() != (seat == 0)) {
                throw new IllegalArgumentException("Saved player seats or human seat are inconsistent");
            }
            Player player = new Player(seat, source.name(), source.human());
            player.hand.addAll(source.hand());
            player.bid = source.bid();
            player.tricksWon = source.tricksWon();
            player.score = source.totalScore();
            player.streak = source.streak();
            player.streakPoints = source.streakPoints();
            if (player.hand.size() > session.handSize || player.tricksWon < 0 || player.tricksWon > session.handSize
                    || (player.score < 0 && !saved.config().mode().isRomanian())
                    || player.streak < -4 || player.streak > 4
                    || player.streakPoints != 0 && player.streakPoints != -5 && player.streakPoints != saved.config().streakBonusPoints()
                    || !saved.config().streakBonuses() && (player.streak != 0 || player.streakPoints != 0)
                    || saved.phase() != GamePhase.SCORE && player.streakPoints != 0
                    || saved.phase() == GamePhase.SCORE && saved.config().streakBonuses()
                        && (player.bid == null
                            || player.streak > 0 && player.tricksWon != player.bid
                            || player.streak < 0 && player.tricksWon == player.bid
                            || player.streakPoints != 0 && (session.handSize == 1 || player.streak != 0
                                || (player.streakPoints > 0) != (player.tricksWon == player.bid)))
                    || player.bid != null && (player.bid < 0 || player.bid > session.handSize)) {
                throw new IllegalArgumentException("Saved player state is outside the legal range");
            }
            if (player.bid != null) countedBids++;
            session.players.add(player);
        }
        if (countedBids != session.bidsSubmitted
                || session.trick.size() >= playerCount
                || session.trick.stream().anyMatch(play -> play.seat() >= playerCount)) {
            throw new IllegalArgumentException("Saved bid or trick state is inconsistent");
        }
        if (session.phase == GamePhase.BIDDING && session.bidsSubmitted >= playerCount
                || session.phase != GamePhase.BIDDING && session.bidsSubmitted != playerCount) {
            throw new IllegalArgumentException("Saved phase does not match bidding progress");
        }
        if (session.phase == GamePhase.SCORE
                && (session.players.stream().anyMatch(player -> !player.hand.isEmpty())
                        || !session.trick.isEmpty() || session.lastTrick.size() != playerCount
                        || session.lastTrickWinnerSeat < 0)) {
            throw new IllegalArgumentException("Saved score screen is incomplete");
        }
        session.validateRestoredRules();
        session.restoreScoreHistory(saved.scoreHistory());
        return session;
    }

    private void restoreScoreHistory(ScoreHistory recorded) {
        List<Integer> totals = playerTotals();
        int completed = handIndex + (phase == GamePhase.SCORE ? 1 : 0);
        if (recorded == null) {
            List<Integer> opening = new ArrayList<>(totals);
            if (phase == GamePhase.SCORE) {
                for (Player player : players) opening.set(player.seat, Math.subtractExact(player.score, earned(player)));
            }
            recorded = new ScoreHistory(handIndex, opening, Collections.emptyList());
            if (phase == GamePhase.SCORE) recorded = recorded.finish(completed, totals);
        }
        if (recorded.completedHands() != completed || !recorded.latestTotals().equals(totals)
                || (phase == GamePhase.SCORE && recorded.closingTotals().isEmpty())) {
            throw new IllegalArgumentException("Score history does not match the saved hand and totals");
        }
        for (int hand = recorded.priorHands() + 1; hand <= completed; hand++) {
            int size = handSizes.get(hand - 1);
            boolean series = config.streakBonuses() && size > 1;
            int minimum = config.mode().isRomanian() ? -size - (series ? 5 : 0) : 0;
            int maximum = size + (config.mode().isRomanian() ? 5 : 10) + (series ? config.streakBonusPoints() : 0);
            for (Player player : players) {
                int points = recorded.points(hand, player.seat);
                if (points < minimum || points > maximum)
                    throw new IllegalArgumentException("Score history exceeds the hand's scoring range");
                if (phase == GamePhase.SCORE && hand == completed && points != earned(player))
                    throw new IllegalArgumentException("Score history does not match the completed contract");
            }
        }
        scoreHistory = recorded;
    }

    private int earned(Player player) {
        return GameRules.scoreDelta(player.tricksWon, player.bid, config.mode()) + player.streakPoints;
    }

    private List<Integer> playerTotals() {
        return players.stream().map(player -> player.score).collect(java.util.stream.Collectors.toList());
    }

    public ScoreHistory scoreHistory() { return scoreHistory; }

    private void validateRestoredRules() {
        int n=players.size();
        int submittedSum=players.stream().filter(p->p.bid!=null).mapToInt(p->p.bid).sum();
        if(submittedSum!=bidTotal)throw new IllegalArgumentException("Saved bid total is inconsistent");
        for(int i=0;i<n;i++) {
            int seat=(dealerSeat+1+i)%n;
            if((players.get(seat).bid!=null)!=(i<bidsSubmitted))
                throw new IllegalArgumentException("Saved bids do not follow dealer rotation");
        }
        if(phase==GamePhase.BIDDING&&biddingSeat!=(dealerSeat+1+bidsSubmitted)%n)
            throw new IllegalArgumentException("Saved bidding seat is inconsistent");
        int remaining=players.stream().mapToInt(p->p.hand.size()).sum();
        int won=players.stream().mapToInt(p->p.tricksWon).sum();
        if(remaining+trick.size()+won*n!=handSize*n)
            throw new IllegalArgumentException("Saved cards and trick counters do not conserve the deal");
        if(phase==GamePhase.BIDDING&&(won!=0||!trick.isEmpty()||remaining!=handSize*n))
            throw new IllegalArgumentException("Saved bidding state already contains card plays");
        if(phase!=GamePhase.BIDDING&&bidTotal==handSize)
            throw new IllegalArgumentException("Saved final bid violates the contract restriction");
        Set<Card> valid=new HashSet<>(GameRules.deck(config.mode(),n)),seen=new HashSet<>();
        if(turnedCard!=null&&(!valid.contains(turnedCard)||!trump.suit().filter(s->s==turnedCard.suit()).isPresent()))
            throw new IllegalArgumentException("Saved turned card does not determine the configured trump");
        if(turnedCard!=null)seen.add(turnedCard);
        for(Player p:players)for(Card c:p.hand)if(!valid.contains(c)||!seen.add(c))
            throw new IllegalArgumentException("Saved hands contain an invalid or duplicate card");
        for(PlayedCard play:history)if(play.seat()>=n||!valid.contains(play.card())||!seen.add(play.card()))
            throw new IllegalArgumentException("Saved public history contains an invalid, duplicate or held card");
        if(history.size()<trick.size()||!history.subList(history.size()-trick.size(),history.size()).equals(trick))
            throw new IllegalArgumentException("Saved current trick is not the public history suffix");
        for(int i=1;i<trick.size();i++)if(trick.get(i).seat()!=(trick.get(i-1).seat()+1)%n)
            throw new IllegalArgumentException("Saved trick seats are out of order");
        if(phase==GamePhase.PLAYING) {
            int expected=!trick.isEmpty()?(trick.get(trick.size()-1).seat()+1)%n:
                    lastTrickWinnerSeat>=0?lastTrickWinnerSeat:(dealerSeat+1)%n;
            if(currentSeat!=expected)throw new IllegalArgumentException("Saved current player is inconsistent");
        }
        if(config.mode().isRomanian()&&handSize==8
                &&(trump!=Trump.NO_TRUMP||turnedCard!=null))
            throw new IllegalArgumentException("Romanian eight-card hands must have no trump");
        validatePublicHistory(remaining);
    }

    private void validatePublicHistory(int remaining) {
        int n=players.size(),played=handSize*n-remaining;
        if(history.size()>played)throw new IllegalArgumentException("Saved history exceeds the played-card count");
        if(!lastTrick.isEmpty()) {
            Set<Card> distinct=new HashSet<>();
            if(lastTrick.size()!=n||lastTrick.stream().anyMatch(p->p.seat()>=n
                    ||!GameRules.deck(config.mode(),n).contains(p.card())||!distinct.add(p.card()))
                    ||GameRules.winningSeat(lastTrick,trump)!=lastTrickWinnerSeat)
                throw new IllegalArgumentException("Saved last trick or winner is inconsistent");
        } else if(lastTrickWinnerSeat!=-1)throw new IllegalArgumentException("Saved winner has no completed trick");
        // Version-1 saves may retain only the last/current trick. Full history allows a stronger audit.
        if(history.size()!=played)return;
        List<List<Card>> hands=new ArrayList<>();
        for(Player p:players)hands.add(new ArrayList<>(p.hand));
        for(PlayedCard p:history)hands.get(p.seat()).add(p.card());
        if(hands.stream().anyMatch(h->h.size()!=handSize))
            throw new IllegalArgumentException("Saved card ownership does not conserve each original hand");
        int actor=nextSeat(dealerSeat);int[] winners=new int[n];List<PlayedCard> round=new ArrayList<>();
        List<PlayedCard> completed=java.util.Collections.emptyList();int winner=-1;
        for(PlayedCard p:history) {
            Suit led=round.isEmpty()?null:round.get(0).card().suit();
            if(p.seat()!=actor||!GameRules.legalCards(hands.get(actor),led,trump,config.mode()).contains(p.card()))
                throw new IllegalArgumentException("Saved public history violates turn order, suit or mandatory trump");
            hands.get(actor).remove(p.card());round.add(p);
            if(round.size()==n) {
                winner=GameRules.winningSeat(round,trump);winners[winner]++;
                actor=winner;completed=new ArrayList<>(round);round.clear();
            }else actor=nextSeat(actor);
        }
        for(Player p:players)if(p.tricksWon!=winners[p.seat])
            throw new IllegalArgumentException("Saved trick counters disagree with public winners");
        if(!round.equals(trick)||(!round.isEmpty()&&(!lastTrick.isEmpty()||lastTrickWinnerSeat!=-1))
                ||(round.isEmpty()&&(!lastTrick.equals(completed)||lastTrickWinnerSeat!=winner)))
            throw new IllegalArgumentException("Saved last/current trick disagrees with public history");
    }

    public void submitBid(int seat, int bid) {
        requirePhase(GamePhase.BIDDING);
        if (seat != biddingSeat) {
            throw new IllegalStateException("Seat " + seat + " cannot bid now; expected seat " + biddingSeat);
        }
        if (bid < 0 || bid > handSize) {
            throw new IllegalArgumentException("Bid must be between 0 and " + handSize);
        }
        boolean lastBid = bidsSubmitted == players.size() - 1;
        if (lastBid && GameRules.isForbiddenLastBid(bidTotal, bid, handSize)) {
            throw new IllegalArgumentException("The last bid cannot make total bids equal the hand size");
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
            throw new IllegalStateException("Seat " + seat + " cannot play now; expected seat " + currentSeat);
        }
        Player player = players.get(seat);
        if (!player.hand.contains(card)) {
            throw new IllegalArgumentException("Card is not in the player's hand: " + card);
        }
        Suit ledSuit = trick.isEmpty() ? null : trick.get(0).card().suit();
        if (!GameRules.legalCards(player.hand, ledSuit, trump, config.mode()).contains(card)) {
            throw new IllegalArgumentException("Follow the led suit; Romanian Whist requires trump when void and trump is held");
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

        lastTrick = GameRules.immutableCopy(trick);
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
        List<PlayerSnapshot> playerSnapshots = players.stream()
                .map(player -> new PlayerSnapshot(player.seat, player.name, player.human,
                        player.hand, player.bid, player.tricksWon, player.score, player.streak, player.streakPoints))
                .collect(java.util.stream.Collectors.toList());
        return new Snapshot(phase, handIndex, handIndex + 1, handSize, trump, dealerSeat,
                biddingSeat, currentSeat, bidsSubmitted, bidTotal, playerSnapshots,
                trick, lastTrick, lastTrickWinnerSeat, handIndex + 1 == handSizes.size(), config.mode(), history, turnedCard, config.streakBonuses(),config.streakBonusPoints());
    }

    public SaveState saveState() {
        List<PlayerState> savedPlayers = players.stream()
                .map(player -> new PlayerState(player.seat, player.name, player.human,
                        player.hand, player.bid, player.tricksWon, player.score, player.streak, player.streakPoints))
                .collect(java.util.stream.Collectors.toList());
        return new SaveState(config, phase, handIndex, handSize, dealerSeat, biddingSeat,
                currentSeat, bidsSubmitted, bidTotal, trump, lastTrickWinnerSeat,
                savedPlayers, trick, lastTrick, history, turnedCard, scoreHistory);
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
                throw new IllegalStateException("Cannot score a hand before all bids are submitted");
            }
            if (config.streakBonuses()) {
                RomanianScoring.Result result = RomanianScoring.score(player.tricksWon, player.bid, handSize, player.streak,config.streakBonusPoints());
                player.score += result.totalPoints();
                player.streak = result.nextStreak();
                player.streakPoints = result.streakPoints();
            } else player.score += GameRules.scoreDelta(player.tricksWon, player.bid, config.mode());
        }
        scoreHistory = scoreHistory.finish(handIndex + 1, playerTotals());
        phase = GamePhase.SCORE;
    }

    private int nextSeat(int seat) {
        return (seat + 1) % players.size();
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Expected phase " + expected + " but game is in " + phase);
        }
    }

    private static void sortHand(List<Card> hand) {
        hand.sort(java.util.Comparator.comparing(Card::suit).thenComparingInt(Card::rank));
    }

    private static void requireSeat(int seat, int playerCount, String name) {
        if (seat < 0 || seat >= playerCount) {
            throw new IllegalArgumentException("Saved " + name + " seat is outside the table");
        }
    }

    private static final class Player {
        private final int seat;
        private final String name;
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

    public record PlayerSnapshot(int seat, String name, boolean human, List<Card> hand,
            Integer bid, int tricksWon, int totalScore, int streak, int streakPoints) {
        public PlayerSnapshot(int seat, String name, boolean human, List<Card> hand,
                Integer bid, int tricksWon, int totalScore) {
            this(seat, name, human, hand, bid, tricksWon, totalScore, 0, 0);
        }
        public PlayerSnapshot {
            hand = GameRules.immutableCopy(hand);
        }
    }

    public record Snapshot(GamePhase phase, int handIndex, int handNumber, int handSize,
            Trump trump, int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted,
            int bidTotal, List<PlayerSnapshot> players, List<PlayedCard> currentTrick,
            List<PlayedCard> lastTrick, int lastTrickWinnerSeat, boolean finalHand, GameMode mode, List<PlayedCard> history, Card turnedCard, boolean streakBonuses,int streakBonusPoints) {
        public Snapshot {
            players = GameRules.immutableCopy(players);
            currentTrick = GameRules.immutableCopy(currentTrick);
            lastTrick = GameRules.immutableCopy(lastTrick);
            history = GameRules.immutableCopy(history);
        }
    }

    public record PlayerState(int seat, String name, boolean human, List<Card> hand,
            Integer bid, int tricksWon, int totalScore, int streak, int streakPoints) {
        public PlayerState(int seat, String name, boolean human, List<Card> hand,
                Integer bid, int tricksWon, int totalScore) {
            this(seat, name, human, hand, bid, tricksWon, totalScore, 0, 0);
        }
        public PlayerState {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) throw new IllegalArgumentException("Player name cannot be blank");
            hand = immutableCopy(hand);
        }
    }

    public record SaveState(GameConfig config, GamePhase phase, int handIndex, int handSize,
            int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted, int bidTotal,
            Trump trump, int lastTrickWinnerSeat, List<PlayerState> players,
            List<PlayedCard> currentTrick, List<PlayedCard> lastTrick, List<PlayedCard> history, Card turnedCard,
            ScoreHistory scoreHistory) {
        public SaveState(GameConfig config, GamePhase phase, int handIndex, int handSize,
                int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted, int bidTotal,
                Trump trump, int lastTrickWinnerSeat, List<PlayerState> players,
                List<PlayedCard> currentTrick, List<PlayedCard> lastTrick, List<PlayedCard> history, Card turnedCard) {
            this(config, phase, handIndex, handSize, dealerSeat, biddingSeat, currentSeat, bidsSubmitted,
                    bidTotal, trump, lastTrickWinnerSeat, players, currentTrick, lastTrick, history, turnedCard, null);
        }
        public SaveState(GameConfig config, GamePhase phase, int handIndex, int handSize,
                int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted, int bidTotal,
                Trump trump, int lastTrickWinnerSeat, List<PlayerState> players,
                List<PlayedCard> currentTrick, List<PlayedCard> lastTrick, List<PlayedCard> history) {
            this(config, phase, handIndex, handSize, dealerSeat, biddingSeat, currentSeat, bidsSubmitted,
                    bidTotal, trump, lastTrickWinnerSeat, players, currentTrick, lastTrick, history, null);
        }
        /** Compatibility for callers constructing initial or pre-history checkpoints. */
        public SaveState(GameConfig config, GamePhase phase, int handIndex, int handSize,
                int dealerSeat, int biddingSeat, int currentSeat, int bidsSubmitted, int bidTotal,
                Trump trump, int lastTrickWinnerSeat, List<PlayerState> players,
                List<PlayedCard> currentTrick, List<PlayedCard> lastTrick) {
            this(config, phase, handIndex, handSize, dealerSeat, biddingSeat, currentSeat, bidsSubmitted,
                    bidTotal, trump, lastTrickWinnerSeat, players, currentTrick, lastTrick,
                    knownHistory(lastTrick, currentTrick), null);
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
        List<PlayedCard> result = new ArrayList<>(last); result.addAll(current); return result;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return java.util.Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(values, "values")));
    }
}

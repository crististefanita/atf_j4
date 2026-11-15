package ro.whistlab.core.session;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.ImmutableLists;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Owns the deal, contracts, turns and public card history for one hand. */
final class HandPlay {
    private final GameMode mode;
    private final int dealerSeat;
    private final int handSize;
    private final Trump trump;
    private final Card turnedCard;
    private final List<SeatHand> seats;
    private final List<PlayedCard> trick = new ArrayList<>();
    private final List<PlayedCard> history = new ArrayList<>();
    private List<PlayedCard> lastTrick = Collections.emptyList();
    private int lastTrickWinnerSeat = -1;
    private int biddingSeat;
    private int currentSeat;
    private int bidsSubmitted;
    private int bidTotal;
    private GamePhase phase = GamePhase.BIDDING;

    private HandPlay(GameMode mode, int dealerSeat, int handSize, Trump trump, Card turnedCard, List<SeatHand> seats) {
        this.mode = mode;
        this.dealerSeat = dealerSeat;
        this.handSize = handSize;
        this.trump = trump;
        this.turnedCard = turnedCard;
        this.seats = seats;
        biddingSeat = nextSeat(dealerSeat);
        currentSeat = biddingSeat;
    }

    static HandPlay deal(GameConfig config, int index, int size, int dealer) {
        int count = config.playerCount();
        List<Card> deck = new ArrayList<>(GameRules.deck(config.mode(), count));
        long handSeed = config.seed() + 0x9E3779B97F4A7C15L * index;
        Collections.shuffle(deck, new Random(handSeed));
        var hands = new ArrayList<List<Card>>(count);
        for (int seat = 0; seat < count; seat++) hands.add(new ArrayList<>(size));
        int dealt = size * count;
        for (int position = 0; position < dealt; position++) {
            hands.get((dealer + 1 + position) % count).add(deck.get(position));
        }
        Trump trump = Trump.forHandIndex(index);
        Card turned = null;
        if (config.mode().isRomanian() && dealt == deck.size()) {
            trump = Trump.NO_TRUMP;
        } else if (config.mode().usesTurnedTrump()) {
            turned = deck.get(dealt);
            trump = Trump.values()[turned.suit().ordinal()];
        }
        return opening(config.mode(), dealer, size, trump, turned, hands);
    }

    static HandPlay fromDeal(GameConfig config, List<List<Card>> hands, Trump trump, int dealer) {
        Objects.requireNonNull(hands, "hands");
        Objects.requireNonNull(trump, "trump");
        int count = config.playerCount();
        if (hands.size() != count) throw new IllegalArgumentException("Deal must have one hand for every player");
        if (dealer < 0 || dealer >= count) throw new IllegalArgumentException("Dealer seat is outside the table");
        int size = hands.get(0).size();
        if (size < 1 || hands.stream().anyMatch(hand -> hand.size() != size)) {
            throw new IllegalArgumentException("All players must receive the same positive hand size");
        }
        var validCards = new HashSet<>(GameRules.deck(config.mode(), count));
        if (size * count > validCards.size()) throw new IllegalArgumentException("Deal exceeds the configured deck");
        var seen = new HashSet<Card>();
        for (var hand : hands) for (Card card : hand) {
            if (!validCards.contains(card)) throw new IllegalArgumentException("Card is outside the configured deck: " + card);
            if (!seen.add(card)) throw new IllegalArgumentException("A card appears more than once in the deal: " + card);
        }
        return opening(config.mode(), dealer, size, trump, null, hands);
    }

    private static HandPlay opening(GameMode mode, int dealer, int size, Trump trump, Card turned, List<List<Card>> hands) {
        var seats = hands.stream().map(cards -> {
            var hand = new SeatHand(cards, null, 0);
            hand.cards.sort(Comparator.comparing(Card::suit).thenComparingInt(Card::rank));
            return hand;
        }).collect(java.util.stream.Collectors.toList());
        return new HandPlay(mode, dealer, size, trump, turned, seats);
    }

    static HandPlay restored(GameSession.SaveState saved) {
        var seats = saved.players().stream()
                .map(seat -> new SeatHand(seat.hand(), seat.bid(), seat.tricksWon())).collect(java.util.stream.Collectors.toList());
        var result = new HandPlay(saved.config().mode(), saved.dealerSeat(), saved.handSize(),
                saved.trump(), saved.turnedCard(), seats);
        result.biddingSeat = saved.biddingSeat();
        result.currentSeat = saved.currentSeat();
        result.bidsSubmitted = saved.bidsSubmitted();
        result.bidTotal = saved.bidTotal();
        result.lastTrickWinnerSeat = saved.lastTrickWinnerSeat();
        result.phase = saved.phase();
        result.trick.addAll(saved.currentTrick());
        result.lastTrick = ImmutableLists.copy(saved.lastTrick());
        result.history.addAll(saved.history());
        return result;
    }

    void submitBid(int seat, int bid) {
        requirePhase(GamePhase.BIDDING);
        if (seat != biddingSeat)
            throw new IllegalStateException("Seat " + seat + " cannot bid now; expected seat " + biddingSeat);
        if (bid < 0 || bid > handSize)
            throw new IllegalArgumentException("Bid must be between 0 and " + handSize);
        if (bidsSubmitted == seats.size() - 1 && GameRules.isForbiddenLastBid(bidTotal, bid, handSize))
            throw new IllegalArgumentException("The last bid cannot make total bids equal the hand size");

        seats.get(seat).bid = bid;
        bidTotal += bid;
        bidsSubmitted++;
        if (bidsSubmitted == seats.size()) {
            phase = GamePhase.PLAYING;
            currentSeat = nextSeat(dealerSeat);
        } else {
            biddingSeat = nextSeat(seat);
        }
    }

    /** Returns true when the accepted card closes the hand. */
    boolean playCard(int seat, Card card) {
        requirePhase(GamePhase.PLAYING);
        Objects.requireNonNull(card, "card");
        if (seat != currentSeat)
            throw new IllegalStateException("Seat " + seat + " cannot play now; expected seat " + currentSeat);
        var player = seats.get(seat);
        if (!player.cards.contains(card))
            throw new IllegalArgumentException("Card is not in the player's hand: " + card);
        Suit led = trick.isEmpty() ? null : trick.get(0).card().suit();
        if (!GameRules.legalCards(player.cards, led, trump, mode).contains(card))
            throw new IllegalArgumentException("Follow the led suit; Romanian Whist requires trump when void and trump is held");

        lastTrick = Collections.emptyList();
        lastTrickWinnerSeat = -1;
        player.cards.remove(card);
        var play = new PlayedCard(seat, card);
        trick.add(play);
        history.add(play);
        if (trick.size() < seats.size()) {
            currentSeat = nextSeat(seat);
            return false;
        }

        lastTrick = ImmutableLists.copy(trick);
        lastTrickWinnerSeat = GameRules.winningSeat(lastTrick, trump);
        seats.get(lastTrickWinnerSeat).tricksWon++;
        trick.clear();
        if (seats.stream().allMatch(entry -> entry.cards.isEmpty())) return true;
        currentSeat = lastTrickWinnerSeat;
        return false;
    }

    void markScored() { phase = GamePhase.SCORE; }

    void requirePhase(GamePhase expected) {
        if (phase != expected) throw new IllegalStateException("Expected phase " + expected + " but game is in " + phase);
    }

    private int nextSeat(int seat) { return (seat + 1) % seats.size(); }

    int size() { return handSize; }
    int dealerSeat() { return dealerSeat; }
    int biddingSeat() { return biddingSeat; }
    int currentSeat() { return currentSeat; }
    int bidsSubmitted() { return bidsSubmitted; }
    int bidTotal() { return bidTotal; }
    int lastTrickWinnerSeat() { return lastTrickWinnerSeat; }
    Trump trump() { return trump; }
    Card turnedCard() { return turnedCard; }
    GamePhase phase() { return phase; }
    List<PlayedCard> currentTrick() { return trick; }
    List<PlayedCard> lastTrick() { return lastTrick; }
    List<PlayedCard> history() { return history; }
    List<Card> cards(int seat) { return seats.get(seat).cards; }
    Integer bid(int seat) { return seats.get(seat).bid; }
    int tricksWon(int seat) { return seats.get(seat).tricksWon; }

    private static final class SeatHand {
        private final List<Card> cards;
        private Integer bid;
        private int tricksWon;

        private SeatHand(List<Card> cards, Integer bid, int tricksWon) {
            this.cards = new ArrayList<>(cards);
            this.bid = bid;
            this.tricksWon = tricksWon;
        }
    }
}

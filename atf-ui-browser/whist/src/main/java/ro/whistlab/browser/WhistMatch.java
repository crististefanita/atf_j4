package ro.whistlab.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import ro.cardtable.browser.BrowserGame;
import ro.cardtable.browser.BrowserGame.Action;
import ro.cardtable.browser.BrowserGame.Played;
import ro.cardtable.browser.BrowserGame.Seat;
import ro.cardtable.browser.BrowserGame.View;
import ro.whistlab.app.bidding.HumanBidSelection;
import ro.whistlab.app.animation.TrickCollectionRequest;
import ro.whistlab.app.achievements.WhistAchievementRepository;
import ro.cardtable.ui.animation.TrickCollectionMotion;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GameSession.Snapshot;

final class WhistMatch implements BrowserGame.Match {
    private final GameSession game;
    private final String matchId = UUID.randomUUID().toString();
    private BrowserGame.CollectionCue collection;
    private final HumanBidSelection bidSelection = new HumanBidSelection();

    WhistMatch(GameSession game) {
        this.game = game;
        advance(false);
    }

    @Override public void act(String action) {
        collection = null;
        if (action.equals("auto")) {
            advance(true);
            return;
        }
        if (action.equals("next")) {
            game.startNextHand();
        } else if (action.startsWith("bid:")) {
            game.submitBid(0, Integer.parseInt(action.substring(4)));
        } else {
            var card = game.snapshot().players().get(0).hand().stream()
                    .filter(candidate -> cardKey(candidate).equals(action)).findFirst().orElseThrow();
            play(0, card);
        }
        advance(false);
    }

    private void advance(boolean automaticHuman) {
        for (int step = 0; step < 120; step++) {
            var state = game.snapshot();
            int actor = actor(state);
            if (actor < 0 || (actor == 0 && !automaticHuman)) return;
            var difficulty = game.config().aiDifficulty();
            if (state.phase() == GamePhase.BIDDING) {
                int bid = actor == 0 ? recommend() : AiPolicy.chooseBid(state, actor, difficulty);
                game.submitBid(actor, bid);
            } else {
                play(actor, AiPolicy.chooseCard(state, actor, difficulty));
            }
        }
        throw new IllegalStateException("Whist turn bound exceeded");
    }

    private void play(int actor, Card card) {
        game.playCard(actor, card);
        var state = game.snapshot();
        TrickCollectionRequest.from(state, state.lastTrick()).ifPresent(target -> collection =
                new BrowserGame.CollectionCue(state.lastTrickWinnerSeat(), TrickCollectionMotion.DISPLAY_MILLIS,
                        TrickCollectionMotion.COLLECT_MILLIS, state.lastTrick().stream()
                        .map(played -> new Played(played.seat(), face(played.card(), ""))).toList()));
    }

    @Override public BrowserGame.CollectionCue collection() { return collection; }
    int handNumber() { return game.snapshot().handNumber(); }
    boolean finished() { return game.snapshot().phase() == GamePhase.SCORE && game.snapshot().finalHand(); }
    void validateName(String value) { game.validateHumanName(value); }
    void rename(String value) { game.renameHuman(value); }

    void recordAchievements(WhistAchievementRepository repository) throws IOException {
        if (game.snapshot().phase() == GamePhase.SCORE) repository.record(matchId, game);
    }

    @Override public View view() {
        var state = game.snapshot();
        var legalCards = legalCards(state);
        var seats = state.players().stream().map(player -> Seat.at(player.seat(), state.players().size(), player.name(),
                "Licitat " + (player.bid() == null ? "—" : player.bid()) + " · Luat " + player.tricksWon(),
                "Total " + state.matchScore(player.seat()), actor(state) == player.seat())).toList();
        var hand = state.players().get(0).hand().stream()
                .map(card -> face(card, legalCards.contains(card) ? cardKey(card) : "")).toList();
        var trick = state.currentTrick().isEmpty() ? state.lastTrick() : state.currentTrick();
        List<Played> played = trick.stream().map(play -> new Played(play.seat(), face(play.card(), ""))).toList();
        var center = state.turnedCard() == null ? List.<BrowserGame.Card>of() : List.of(face(state.turnedCard(), ""));
        var subtitle = state.handSize() + " cărți · Atu " + state.trump().suit().map(Suit::symbol).orElse("—")
                + " · Licitații " + state.bidTotal();
        return new View("Mâna " + state.handNumber(), subtitle, prompt(state), seats, hand, played, center,
                actions(state), WhistScores.history(game));
    }

    private List<Action> actions(Snapshot state) {
        var moves = new ArrayList<Action>();
        if (state.phase() == GamePhase.SCORE) {
            if (!state.finalHand()) moves.add(new Action("next", "Mâna următoare", true));
        } else if (actor(state) == 0) {
            if (state.phase() == GamePhase.BIDDING) {
                int recommendation = recommend();
                for (int bid : GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1)) {
                    moves.add(new Action("bid:" + bid, Integer.toString(bid), bid == recommendation));
                }
            }
            moves.add(new Action("auto", "Joacă automat"));
        }
        return moves;
    }

    private int recommend() {
        var pending = bidSelection.request(game);
        if (pending.isPresent()) {
            var request = pending.get();
            bidSelection.apply(game, request, request.evaluate());
        }
        return bidSelection.selected(game).orElseThrow();
    }

    private static int actor(Snapshot state) {
        return switch (state.phase()) {
            case BIDDING -> state.biddingSeat();
            case PLAYING -> state.currentSeat();
            case SCORE -> -1;
        };
    }

    private static String prompt(Snapshot state) {
        return switch (state.phase()) {
            case BIDDING -> "Alege licitația";
            case PLAYING -> "Alege o carte";
            case SCORE -> state.finalHand() ? "Clasament final" : "Mână încheiată";
        };
    }

    private static List<Card> legalCards(Snapshot state) {
        if (state.phase() != GamePhase.PLAYING || actor(state) != 0) return List.of();
        Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        return GameRules.legalCards(state.players().get(0).hand(), led, state.trump(), state.mode());
    }

    private static String cardKey(Card card) { return "card:" + card.suit().name() + ":" + card.rank(); }

    private static BrowserGame.Card face(Card card, String action) {
        return new BrowserGame.Card(card.rankLabel(), card.suit().symbol(), card.accessibleLabel(), action);
    }
}

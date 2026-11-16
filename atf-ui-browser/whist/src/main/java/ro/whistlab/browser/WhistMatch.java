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
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.theme.GameHeaderTypography;
import ro.whistlab.app.animation.TrickCollectionRequest;
import ro.whistlab.app.achievements.WhistAchievementRepository;
import ro.cardtable.ui.animation.TrickCollectionMotion;
import ro.cardtable.ui.turn.TurnPacing;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GameSession.Snapshot;

final class WhistMatch implements BrowserGame.Match {
    private static final WhistUiText COPY = new WhistUiText(HowToPlay.Language.RO);
    private final GameSession game;
    private final String matchId = UUID.randomUUID().toString();
    private final List<BrowserGame.CollectionCue> collections = new ArrayList<>();
    private final List<BrowserGame.PlayStep> playback = new ArrayList<>();
    private final HumanBidSelection bidSelection = new HumanBidSelection();

    WhistMatch(GameSession game) {
        this.game = game;
        advance(false);
    }

    @Override public void act(String action) {
        collections.clear();
        playback.clear();
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
            play(0, card, false);
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
                play(actor, AiPolicy.chooseCard(state, actor, difficulty), true);
            }
        }
        throw new IllegalStateException("Whist turn bound exceeded");
    }

    private void play(int actor, Card card, boolean automated) {
        boolean startsTrick = game.snapshot().currentTrick().isEmpty();
        game.playCard(actor, card);
        var state = game.snapshot();
        var collection = TrickCollectionRequest.from(state, state.lastTrick()).map(target ->
                new BrowserGame.CollectionCue(state.lastTrickWinnerSeat(), TrickCollectionMotion.DISPLAY_MILLIS,
                        TrickCollectionMotion.COLLECT_MILLIS, state.lastTrick().stream()
                        .map(played -> new Played(played.seat(), face(played.card(), ""))).toList())).orElse(null);
        if (collection != null) collections.add(collection);
        playback.add(new BrowserGame.PlayStep(new Played(actor, face(card, "")), startsTrick,
                automated ? TurnPacing.ACTION_DELAY_MILLIS : 0, collection));
    }

    @Override public List<BrowserGame.CollectionCue> collections() { return List.copyOf(collections); }
    @Override public List<BrowserGame.PlayStep> playback() { return List.copyOf(playback); }
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
        var seats = state.players().stream().map(player -> Seat.at(player.seat(), state.players().size(), COPY.seatName(player),
                COPY.seatFacts(player),
                "Total " + state.matchScore(player.seat()), actor(state) == player.seat())).toList();
        var hand = state.players().get(0).hand().stream()
                .map(card -> face(card, legalCards.contains(card) ? cardKey(card) : "")).toList();
        var trick = state.currentTrick().isEmpty() ? state.lastTrick() : state.currentTrick();
        List<Played> played = trick.stream().map(play -> new Played(play.seat(), face(play.card(), ""))).toList();
        var center = state.turnedCard() == null ? List.<BrowserGame.Card>of() : List.of(face(state.turnedCard(), ""));
        var subtitle = state.handSize() + " cărți · Atu " + state.trump().suit().map(Suit::symbol).orElse("—")
                + " · Licitații " + state.bidTotal();
        var header = new BrowserGame.Header(state.mode().name(), COPY.gameTitle(state.mode()), COPY.progress(state),
                COPY.bidSummary(state), COPY.trumpIndicatorValue(state), COPY.trumpAccessibility(state),
                GameHeaderTypography.TITLE_SP, GameHeaderTypography.PROGRESS_SP, GameHeaderTypography.BID_SUMMARY_SP);
        var score = state.phase() == GamePhase.SCORE ? WhistScores.hand(game, COPY) : null;
        return new View("Mâna " + state.handNumber(), subtitle, prompt(state), seats, hand, played, center,
                actions(state), WhistScores.history(game), header, score);
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

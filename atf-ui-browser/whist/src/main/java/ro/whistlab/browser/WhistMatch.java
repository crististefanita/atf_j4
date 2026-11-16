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
import ro.whistlab.app.highscores.HighScoreRepository;
import ro.whistlab.app.persistence.GameCheckpointCodec;
import org.json.JSONObject;
import ro.cardtable.ui.animation.TrickCollectionMotion;
import ro.cardtable.ui.turn.TurnPacing;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GameSession.Snapshot;

final class WhistMatch implements BrowserGame.Match {
    private WhistUiText copy = WhistBrowser.copy("ro");
    private final GameSession game;
    private final String matchId;
    private final List<BrowserGame.CollectionCue> collections = new ArrayList<>();
    private final List<BrowserGame.PlayStep> playback = new ArrayList<>();
    private final HumanBidSelection bidSelection = new HumanBidSelection();

    WhistMatch(GameSession game) {
        this(game, UUID.randomUUID().toString());
        advance(false);
    }

    private WhistMatch(GameSession game, String matchId) {
        this.game = game;
        this.matchId = matchId;
    }

    static WhistMatch restore(String json) {
        String id = new JSONObject(json).getString("matchId");
        if (id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid match identity");
        return new WhistMatch(GameCheckpointCodec.fromJson(json), id);
    }

    String checkpoint() {
        return GameCheckpointCodec.encode(game.saveState()).put("matchId", matchId).toString();
    }

    GameConfig config() { return game.config(); }

    void recordRanking(HighScoreRepository repository) throws IOException {
        if (finished()) repository.record(matchId, game, System.currentTimeMillis());
    }

    @Override public void setLanguage(String language) {
        copy = WhistBrowser.copy(language);
    }

    @Override public void act(String action) {
        collections.clear();
        playback.clear();
        if (action.equals("auto")) {
            advance(true);
            return;
        }
        if (action.equals("auto-step")) {
            automaticDecision(game.snapshot(), 0, false);
        } else if (action.equals("next")) {
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
            automaticDecision(state, actor, true);
        }
        throw new IllegalStateException("Whist turn bound exceeded");
    }

    private void automaticDecision(Snapshot state, int actor, boolean delay) {
        var difficulty = game.config().aiDifficulty();
        if (state.phase() == GamePhase.BIDDING) {
            int bid = actor == 0 ? recommend() : AiPolicy.chooseBid(state, actor, difficulty);
            game.submitBid(actor, bid);
        } else {
            play(actor, AiPolicy.chooseCard(state, actor, difficulty), delay);
        }
    }

    private void play(int actor, Card card, boolean automated) {
        var before = game.snapshot();
        game.playCard(actor, card);
        var state = game.snapshot();
        var collection = TrickCollectionRequest.from(state, state.lastTrick()).map(target ->
                new BrowserGame.CollectionCue(state.lastTrickWinnerSeat(), TrickCollectionMotion.DISPLAY_MILLIS,
                        TrickCollectionMotion.COLLECT_MILLIS, state.lastTrick().stream()
                        .map(played -> new Played(played.seat(), face(played.card(), ""))).toList())).orElse(null);
        if (collection != null) collections.add(collection);
        playback.add(new BrowserGame.PlayStep(new Played(actor, face(card, "")), before.currentTrick().isEmpty(),
                automated ? TurnPacing.ACTION_DELAY_MILLIS : 0, collection, copy.bidSummary(before),
                before.players().stream().map(copy::seatFacts).toList()));
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
        var seats = state.players().stream().map(player -> Seat.at(player.seat(), state.players().size(), copy.seatName(player),
                copy.seatFacts(player),
                "Total " + state.matchScore(player.seat()), actor(state) == player.seat())).toList();
        var hand = state.players().get(0).hand().stream()
                .map(card -> face(card, legalCards.contains(card) ? cardKey(card) : "")).toList();
        var trick = state.currentTrick().isEmpty() ? state.lastTrick() : state.currentTrick();
        List<Played> played = trick.stream().map(play -> new Played(play.seat(), face(play.card(), ""))).toList();
        var center = state.turnedCard() == null ? List.<BrowserGame.Card>of() : List.of(face(state.turnedCard(), ""));
        var subtitle = copy.trumpHeading() + " " + copy.trumpIndicatorValue(state) + " · " + copy.bidSummary(state);
        var header = new BrowserGame.Header(state.mode().name(), copy.gameTitle(state.mode()), copy.progress(state),
                copy.bidSummary(state), copy.trumpIndicatorValue(state), copy.trumpAccessibility(state),
                GameHeaderTypography.TITLE_SP, GameHeaderTypography.PROGRESS_SP, GameHeaderTypography.BID_SUMMARY_SP);
        var score = state.phase() == GamePhase.SCORE ? WhistScores.hand(game, copy) : null;
        return new View(copy.historyHand() + " " + state.handNumber(), subtitle, prompt(state), seats, hand, played, center,
                actions(state), WhistScores.history(game, copy), header, score);
    }

    private List<Action> actions(Snapshot state) {
        var moves = new ArrayList<Action>();
        if (state.phase() == GamePhase.SCORE) {
            if (!state.finalHand()) moves.add(new Action("next", copy.nextHand(), true));
        } else if (actor(state) == 0) {
            if (state.phase() == GamePhase.BIDDING) {
                int recommendation = recommend();
                for (int bid : GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1)) {
                    moves.add(new Action("bid:" + bid, Integer.toString(bid), bid == recommendation));
                }
            }
            moves.add(new Action("auto", copy.autoGameHand()));
            moves.add(new Action("auto-step", copy.autoGameToggle()));
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

    private String prompt(Snapshot state) {
        return switch (state.phase()) {
            case BIDDING -> copy.bidSelector();
            case PLAYING -> copy.playHint(state);
            case SCORE -> state.finalHand() ? copy.gameComplete() : copy.handComplete();
        };
    }

    private static List<Card> legalCards(Snapshot state) {
        if (state.phase() != GamePhase.PLAYING || actor(state) != 0) return List.of();
        Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        return GameRules.legalCards(state.players().get(0).hand(), led, state.trump(), state.mode());
    }

    private static String cardKey(Card card) { return "card:" + card.suit().name() + ":" + card.rank(); }

    private BrowserGame.Card face(Card card, String action) {
        return new BrowserGame.Card(card.rankLabel(), card.suit().symbol(), copy.cardLabel(card), action);
    }
}

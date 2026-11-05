package ro.whistlab.app;
import ro.whistlab.app.screens.ScoreScreens;
import ro.whistlab.app.screens.GameScreen;
import ro.whistlab.app.screens.SetupScreen;
import ro.whistlab.app.screens.MenuScreens;
import ro.whistlab.app.screens.MatchSetup;
import ro.whistlab.app.screens.ScreenViews;
import ro.whistlab.app.navigation.AppNavigation;

import ro.whistlab.app.fixtures.DisplayFixtureDeals;
import ro.whistlab.app.fixtures.OriginalMatchReplay;
import ro.whistlab.app.lifecycle.CompletedTrickPause;
import ro.whistlab.app.lifecycle.AiTurnCoordinator;
import ro.whistlab.app.lifecycle.AiTurnRequest;
import ro.whistlab.app.lifecycle.GameCommandFlow;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.app.persistence.GamePersistence;
import ro.whistlab.app.highscores.AndroidHighScoreStorage;
import ro.whistlab.app.highscores.HighScoreRepository;
import ro.whistlab.app.highscores.HighScores;
import ro.whistlab.app.highscores.HighScoresDialog;
import ro.whistlab.app.profile.PlayerPreferences;
import ro.whistlab.app.profile.PlayerSettingsDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.session.ScoreHistory;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

public final class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler aiResults = new Handler(Looper.getMainLooper());
    private final AiTurnCoordinator aiTurns = new AiTurnCoordinator(command -> aiResults.post(command));
    private final GameCommandFlow commands = new GameCommandFlow(this::persistGame, this::driveAutomatedTurns);
    private boolean automationActive = true;
    private final CompletedTrickPause completedTrickPause = new CompletedTrickPause();
    private GameSession game;
    private GameConfig lastConfig;
    private OriginalMatchReplay recordedReplay;
    private boolean debugHoldAiTurns;
    private boolean debugHoldAiBids;
    private WhistUiText copy;
    private final AppNavigation navigation = new AppNavigation();
    private final MatchSetup setup = new MatchSetup();
    private ScreenViews views;
    private MenuScreens menuScreens;
    private SetupScreen setupScreen;
    private GameScreen gameScreen;
    private ScoreScreens scoreScreens;
    private Integer selectedBid;
    private PlayerPreferences playerPreferences;
    private HighScoreRepository highScores;
    private GamePersistence persistence;
    private boolean rankingEligible;
    private String matchId = java.util.UUID.randomUUID().toString();

    @Override protected void onResume(){super.onResume();automationActive=true;if(navigation.gameVisible()&&game!=null)driveAutomatedTurns();}
    @Override protected void onStop(){automationActive=false;cancelAutomatedTurns();persistGame();super.onStop();}
    @Override protected void onDestroy(){automationActive=false;cancelAutomatedTurns();aiTurns.close();aiResults.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed() { if (navigation.backReturnsHome()) renderHome(); else super.onBackPressed(); }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String selectedLanguage = getPreferences(MODE_PRIVATE).getString("interfaceLanguage",
                getPreferences(MODE_PRIVATE).getString("helpLanguage", "EN"));
        if (BuildConfig.DEBUG && getIntent().hasExtra("debug_language")) {
            selectedLanguage = getIntent().getStringExtra("debug_language");
        }
        copy = new WhistUiText(WhistUiText.parseLanguage(selectedLanguage));
        views = new ScreenViews(this, () -> copy);
        menuScreens = new MenuScreens(views);
        setupScreen = new SetupScreen(views, menuScreens);
        gameScreen = new GameScreen(views);
        scoreScreens = new ScoreScreens(views);
        playerPreferences = new PlayerPreferences(this);
        highScores = new HighScoreRepository(new AndroidHighScoreStorage(this), new java.util.Random());
        persistence = new GamePersistence(
                (session, eligible, id) -> GameSessionStore.save(this, session, eligible, id),
                highScores::record, System::currentTimeMillis,
                failure -> Toast.makeText(this, copy.storageError(), Toast.LENGTH_LONG).show());
        try { highScores.boards(); } catch (java.io.IOException failure) {
            Toast.makeText(this, copy.storageError(), Toast.LENGTH_LONG).show();
        }
        rankingEligible = !BuildConfig.DEBUG || !java.util.stream.Stream.of("debug_replay", "debug_fixture",
                "debug_trick_pause_fixture", "debug_played_card_fixture", "debug_hard_ai_bid_fixture",
                "debug_hand_fan_fixture", "debug_nine_bids_fixture", "debug_compact_table_fixture")
                .anyMatch(key -> getIntent().getBooleanExtra(key, false));
        debugHoldAiTurns = BuildConfig.DEBUG
                && getIntent().getBooleanExtra("debug_hold_ai_turns", false);
        debugHoldAiBids = BuildConfig.DEBUG
                && getIntent().getBooleanExtra("debug_hold_ai_bids", false);
        views.applyStandardWindowChrome();
        if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_replay", false)) {
            try {
                recordedReplay = new OriginalMatchReplay(getFilesDir());
                game = recordedReplay.startHand(0, ScoreHistory.newMatch(4));
                lastConfig = game.config();
                persistGame();
                driveAutomatedTurns();
            } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_fixture", false)) {
            openOneTrickFixture(getIntent().getBooleanExtra("persist_fixture", false));
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_trick_pause_fixture", false)) {
            openTrickPauseFixture();
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_played_card_fixture", false)) {
            openPlayedCardFixture();
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_hard_ai_bid_fixture", false)) {
            openHardAiBidFixture();
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_hand_fan_fixture", false)) {
            openDisplayFixture(13, true);
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_nine_bids_fixture", false)) {
            openDisplayFixture(8, false);
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_compact_table_fixture", false)) {
            var fixture = DisplayFixtureDeals.completedThirteenCardTrick();
            game = fixture.game();
            lastConfig = game.config();
            renderPlay(fixture.cards(), true);
        } else {
            loadSavedGame();
            renderHome();
        }
    }

    private void loadSavedGame() {
        try {
            game = GameSessionStore.load(this);
            if (game != null) {
                lastConfig = game.config();
                rankingEligible = GameSessionStore.rankingEligible(this);
                matchId = GameSessionStore.matchId(this);
            }
        } catch (GameSessionStore.UnsupportedSavedMode unavailable) {
            Toast.makeText(this, copy.unsupportedSavedMode(), Toast.LENGTH_LONG).show();
            return;
        } catch (IllegalStateException invalidSave) {
            GameSessionStore.clear(this);
            game = null;
            lastConfig = null;
            Toast.makeText(this, copy.noSavedGame(), Toast.LENGTH_LONG).show();
            return;
        }
        persistGame();
    }

    private void renderHome() {
        navigation.show(AppNavigation.Screen.HOME);
        cancelAutomatedTurns();
        views.applyStandardWindowChrome();
        menuScreens.home(copy, playerPreferences.name(), game != null,
                new MenuScreens.Actions(this::showPlayerSettings, this::renderSetup, this::renderCurrentState,
                        this::showHighScores, this::renderInstructions, this::setUiLanguage));
    }

    private void setUiLanguage(HowToPlay.Language language) {
        if (copy.language() == language) return;
        getPreferences(MODE_PRIVATE).edit()
                .putString("interfaceLanguage", language.name())
                .putString("helpLanguage", language.name())
                .apply();
        copy = new WhistUiText(language);
        switch (navigation.screen()) {
            case INSTRUCTIONS -> renderInstructions();
            case SETUP -> renderSetup();
            default -> renderHome();
        }
    }

    private void renderSetup() {
        navigation.show(AppNavigation.Screen.SETUP);
        cancelAutomatedTurns();
        views.applyStandardWindowChrome();
        setupScreen.show(copy, setup, this::setUiLanguage, this::startGame, this::renderHome);
    }

    private void startGame(GameConfig config) {
        cancelAutomatedTurns();
        recordedReplay = null;
        selectedBid = null;
        lastConfig = config;
        rankingEligible = true;
        matchId = java.util.UUID.randomUUID().toString();
        game = GameSession.newGame(lastConfig, playerPreferences.sessionName());
        persistGame();
        driveAutomatedTurns();
    }

    private void renderBidding() {
        var state = game.snapshot();
        List<Integer> choices = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                state.bidsSubmitted() == state.players().size() - 1);
        if (selectedBid == null || !choices.contains(selectedBid)) selectedBid = choices.get(0);
        gameScreen.bidding(copy, state, choices, selectedBid, bid -> selectedBid = bid,
                this::submitSelectedBid, gameMenuActions());
    }

    private void submitSelectedBid() {
        if (aiTurns.isPending()) return;
        commands.execute(() -> game.submitBid(0, selectedBid), () -> selectedBid = null,
                refusal -> {
                    Toast.makeText(this, copy.invalidBid(), Toast.LENGTH_SHORT).show();
                    renderBidding();
                });
    }

    private void renderPlay() {
        renderPlay(Collections.emptyList(), false);
    }

    private void renderPlay(List<PlayedCard> completedTrick, boolean pauseActive) {
        gameScreen.playing(copy, game.snapshot(), completedTrick, pauseActive, 0,
                this::playHumanCard, gameMenuActions());
    }

    private void renderCollecting(CompletedTrickPause.DisplayState pause) {
        gameScreen.playing(copy, game.snapshot(), pause.cards(), true, pause.remainingMillis(),
                this::playHumanCard, gameMenuActions());
    }

    private GameScreen.MenuActions gameMenuActions() {
        return new GameScreen.MenuActions(this::showLiveScoreboard, this::showPlayerSettings,
                this::showHighScores, this::renderHome);
    }

    private void showLiveScoreboard() {
        var state = game.snapshot();
        boolean resume = state.phase() == GamePhase.BIDDING && state.biddingSeat() != 0
                || state.phase() == GamePhase.PLAYING && state.currentSeat() != 0;
        var token = openDialog();
        var history = game.scoreHistory();
        scoreScreens.live(copy, state, history, () -> dismissDialog(token, resume), () -> showScoreHistory(state, history));
    }

    private void showScoreHistory(GameSession.Snapshot state, ScoreHistory history) {
        var token = openDialog();
        scoreScreens.history(copy, state, history, () -> dismissDialog(token, true));
    }

    private AppNavigation.Dialog openDialog() {
        cancelAutomatedTurns();
        return navigation.openDialog();
    }

    private void dismissDialog(AppNavigation.Dialog token, boolean resume) {
        if (navigation.dismiss(token) && resume && game != null) driveAutomatedTurns();
    }

    private void playHumanCard(Card card) {
        if (aiTurns.isPending()) return;
        commands.execute(() -> game.playCard(0, card), () -> {
            if (recordedReplay != null) recordedReplay.played(card);
        }, refusal -> {
            Toast.makeText(this, copy.invalidCard(), Toast.LENGTH_SHORT).show();
            renderPlay();
        });
    }

    private void renderScore() {
        var state = game.snapshot();
        scoreScreens.score(copy, state, game.scoreHistory(), new ScoreScreens.Actions(
                () -> nextHand(state), this::playAgain, () -> showScoreHistory(state, game.scoreHistory()), this::renderHome));
    }

    private void nextHand(GameSession.Snapshot state) {
        cancelAutomatedTurns();
        if (recordedReplay != null) game = recordedReplay.startHand(state.handIndex() + 1, game.scoreHistory());
        else game.startNextHand();
        persistGame();
        driveAutomatedTurns();
    }

    private void playAgain() {
        startGame(GameConfig.newMatch(lastConfig.playerCount(), lastConfig.mode(),
                lastConfig.aiDifficulty(), System.nanoTime(), lastConfig.streakBonuses(), lastConfig.streakBonusPoints()));
    }

    private void showPlayerSettings() {
        var token = openDialog();
        AlertDialog dialog = PlayerSettingsDialog.show(this, copy, playerPreferences.name(), name -> {
            if (game != null) game.renameHuman(name.isBlank() ? "You" : name);
            playerPreferences.saveName(name);
            persistGame();
            if (!navigation.gameVisible()) {
                if (navigation.screen() == AppNavigation.Screen.SETUP || navigation.screen() == AppNavigation.Screen.INSTRUCTIONS) renderSetup();
                else renderHome();
            }
        });
        dialog.setOnDismissListener(ignored -> dismissDialog(token, true));
    }

    private void showHighScores() {
        var token = openDialog();
        try {
            GameConfig config = lastConfig != null ? lastConfig : setup.config(0);
            AlertDialog dialog = HighScoresDialog.show(this, copy, highScores, HighScores.Profile.from(config), playerPreferences);
            dialog.setOnDismissListener(ignored -> dismissDialog(token, true));
        } catch (java.io.IOException failure) {
            Toast.makeText(this, copy.storageError(), Toast.LENGTH_LONG).show();
            dismissDialog(token, true);
        }
    }

    private void renderInstructions() {
        navigation.show(AppNavigation.Screen.INSTRUCTIONS);
        cancelAutomatedTurns();
        views.applyStandardWindowChrome();
        menuScreens.instructions(copy, this::setUiLanguage, this::renderHome);
    }

    private void driveAutomatedTurns() {
        if (!automationActive) return;
        handler.removeCallbacksAndMessages(null);
        if (navigation.blocksAutomation()) return;
        if (aiTurns.isPending()) return;
        if (game == null) {
            renderHome();
            return;
        }
        GameSession.Snapshot state = game.snapshot();
        navigation.show(AppNavigation.Screen.GAME);
        CompletedTrickPause.DisplayState trickPause = completedTrickPause.evaluate(
                game, SystemClock.uptimeMillis());
        if (trickPause.active()) {
            renderCollecting(trickPause);
            handler.postDelayed(this::driveAutomatedTurns, trickPause.remainingMillis());
            return;
        }
        if (state.phase() == GamePhase.SCORE) {
            renderScore();
            return;
        }
        if (debugHoldAiTurns && state.phase() == GamePhase.PLAYING && state.currentSeat() != 0) {
            renderPlay();
            return;
        }
        if (state.phase() == GamePhase.BIDDING) {
            renderBidding();
            if (state.biddingSeat() == 0 || debugHoldAiBids) {
                return;
            }
            int seat = state.biddingSeat();
            if (recordedReplay == null) {
                submitAiTurn();
                return;
            }
            game.submitBid(seat, recordedReplay.bid(seat));
            persistGame();
            driveAutomatedTurns();
            return;
        }
        if (state.currentSeat() == 0) {
            renderPlay();
            return;
        }
        renderPlay();
        handler.postDelayed(() -> {
            if (game == null || game.snapshot().phase() != GamePhase.PLAYING) {
                driveAutomatedTurns();
                return;
            }
            GameSession.Snapshot current = game.snapshot();
            if (current.currentSeat() == 0) {
                renderPlay();
                return;
            }
            int seat = current.currentSeat();
            if (recordedReplay == null) {
                submitAiTurn();
                return;
            }
            Card choice = recordedReplay.nextCard();
            game.playCard(seat, choice);
            if (recordedReplay != null) recordedReplay.played(choice);
            persistGame();
            driveAutomatedTurns();
        }, 550L);
    }

    private void submitAiTurn() {
        AiTurnRequest request = new AiTurnRequest(game);
        aiTurns.submit(request::evaluate, decision -> {
            if (!automationActive || !navigation.gameVisible() || navigation.blocksAutomation()) return;
            if (request.applyTo(game, decision)) persistGame();
            driveAutomatedTurns();
        }, failure -> { throw new IllegalStateException("AI computation failed", failure); });
    }

    private void cancelAutomatedTurns() {
        if (gameScreen != null) gameScreen.cancelCollection();
        handler.removeCallbacksAndMessages(null);
        aiTurns.cancel();
        aiResults.removeCallbacksAndMessages(null);
    }

    private void renderCurrentState() {
        if (game == null) {
            renderHome();
        } else {
            driveAutomatedTurns();
        }
    }

    private void persistGame() {
        if (game == null) return;
        persistence.save(game, rankingEligible, matchId, session -> {
            if (recordedReplay != null) recordedReplay.export(session);
        });
    }

    private void openOneTrickFixture(boolean persistFixture) {
        GameConfig config = new GameConfig(4, GameMode.REVERSE_OH_HELL, AiDifficulty.NORMAL, 20261101L);
        List<List<Card>> hands = Arrays.asList(
                java.util.Collections.singletonList(new Card(Suit.HEARTS, 14)),
                java.util.Collections.singletonList(new Card(Suit.HEARTS, 13)),
                java.util.Collections.singletonList(new Card(Suit.HEARTS, 12)),
                java.util.Collections.singletonList(new Card(Suit.HEARTS, 11)));
        game = GameSession.fromDeal(config, hands, Trump.SPADES, 3);
        lastConfig = config;
        if (persistFixture) persistGame();
        driveAutomatedTurns();
    }

    private void openTrickPauseFixture() {
        GameConfig config = new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 20261103L);
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 14)),
                Arrays.asList(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 13)),
                Arrays.asList(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 12)),
                Arrays.asList(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 11)));
        game = GameSession.fromDeal(config, hands, Trump.SPADES, 3);
        lastConfig = config;
        persistGame();
        driveAutomatedTurns();
    }

    private void openPlayedCardFixture() {
        GameConfig config = new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 20261104L);
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 14)),
                Arrays.asList(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 13)),
                Arrays.asList(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 12)),
                Arrays.asList(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 11)));
        game = GameSession.fromDeal(config, hands, Trump.SPADES, 3);
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            game.submitBid(game.snapshot().biddingSeat(), 0);
        }
        GameSession.Snapshot state = game.snapshot();
        if (state.currentSeat() != 0) {
            throw new IllegalStateException("Played-card fixture must begin with the human lead");
        }
        game.playCard(0, new Card(Suit.HEARTS, 14));
        lastConfig = config;
        driveAutomatedTurns();
    }

    private void openHardAiBidFixture() {
        GameConfig config = new GameConfig(4, GameMode.OH_HELL, AiDifficulty.HARD, 20261104L);
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 14)),
                Arrays.asList(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 13)),
                Arrays.asList(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 12)),
                Arrays.asList(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 11)));
        game = GameSession.fromDeal(config, hands, Trump.SPADES, 3);
        selectedBid = 0;
        lastConfig = config;
        driveAutomatedTurns();
    }

    private void openDisplayFixture(int cards, boolean playing) {
        GameConfig config = new GameConfig(4, GameMode.DESCENDING, AiDifficulty.NORMAL, 20261103L);
        game = GameSession.fromDeal(config, DisplayFixtureDeals.fourPlayerHands(cards), Trump.SPADES, 3);
        if (playing) {
            for (int seat = 0; seat < config.playerCount(); seat++) game.submitBid(game.snapshot().biddingSeat(), 0);
        }
        lastConfig = config;
        selectedBid = null;
        driveAutomatedTurns();
    }

}

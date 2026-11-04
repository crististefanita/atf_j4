package ro.whistlab.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import ro.whistlab.core.HowToPlay;
import ro.whistlab.core.AiDifficulty;
import ro.whistlab.core.AiPolicy;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameConfig;
import ro.whistlab.core.GameMode;
import ro.whistlab.core.GamePhase;
import ro.whistlab.core.GameRules;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.PlayedCard;
import ro.whistlab.core.Suit;
import ro.whistlab.core.Trump;

public final class MainActivity extends Activity {
    private static final long COMPLETED_TRICK_DISPLAY_MILLIS = 1_500L;
    private static final int PINE = Color.rgb(29, 54, 43);
    private static final int INK = Color.rgb(41, 54, 46);
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int ACCENT = Color.rgb(48, 111, 78);
    private static final int SURFACE = Color.rgb(255, 254, 248);
    private static final int BACKGROUND = Color.rgb(246, 245, 237);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompletedTrickPause completedTrickPause = new CompletedTrickPause();
    private GameSession game;
    private GameConfig lastConfig;
    private OriginalMatchReplay recordedReplay;
    private boolean tableVisible;
    private boolean menuSubpage;
    private boolean instructionsVisible;
    private boolean scoreDialogShowing;
    private boolean debugHoldAiTurns;
    private WhistUiText copy;
    private int setupPlayerCount = 4;
    private GameMode setupMode = GameMode.OH_HELL;
    private AiDifficulty setupDifficulty = AiDifficulty.NORMAL;
    private boolean setupStreaks = true;
    private int setupStreakBonus = 10;
    private Integer selectedBid;

    @Override protected void onResume(){super.onResume();if(tableVisible&&game!=null)driveAutomatedTurns();}
    @Override protected void onStop(){handler.removeCallbacksAndMessages(null);persistGame();super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){if(tableVisible||menuSubpage)renderHome();else super.onBackPressed();}

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String selectedLanguage = getPreferences(MODE_PRIVATE).getString("interfaceLanguage",
                getPreferences(MODE_PRIVATE).getString("helpLanguage", "EN"));
        if (BuildConfig.DEBUG && getIntent().hasExtra("debug_language")) {
            selectedLanguage = getIntent().getStringExtra("debug_language");
        }
        copy = new WhistUiText(WhistUiText.parseLanguage(selectedLanguage));
        debugHoldAiTurns = BuildConfig.DEBUG
                && getIntent().getBooleanExtra("debug_hold_ai_turns", false);
        applyStandardWindowChrome();
        if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_replay", false)) {
            try {
                recordedReplay = new OriginalMatchReplay(getFilesDir());
                game = recordedReplay.startHand(0, Arrays.asList(0, 0, 0, 0));
                lastConfig = game.config();
                persistGame();
                driveAutomatedTurns();
            } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_fixture", false)) {
            openOneTrickFixture(getIntent().getBooleanExtra("persist_fixture", false));
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_trick_pause_fixture", false)) {
            openTrickPauseFixture();
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_hand_fan_fixture", false)) {
            openDisplayFixture(13, true);
        } else if (BuildConfig.DEBUG && getIntent().getBooleanExtra("debug_nine_bids_fixture", false)) {
            openDisplayFixture(8, false);
        } else {
            try {
                game = GameSessionStore.load(this);
                if (game != null) lastConfig = game.config();
            } catch (IllegalStateException invalidSave) {
                GameSessionStore.clear(this);
                Toast.makeText(this, copy.noSavedGame(), Toast.LENGTH_LONG).show();
            }
            renderHome();
        }
    }

    private void renderHome() {
        applyStandardWindowChrome();
        tableVisible=false;
        menuSubpage=false;
        instructionsVisible=false;
        handler.removeCallbacksAndMessages(null);
        LinearLayout page = page(null, copy.homeTitle());
        addText(page, copy.homeTagline(),
                16, MUTED, false, 0, 20);
        addLanguagePicker(page);
        addButton(page, copy.newGame(), "btn_new_game", true, this::renderSetup, true);
        addButton(page, copy.resumeGame(), "btn_resume_game", game != null,
                this::renderCurrentState, false);
        addButton(page, copy.help(), "btn_instructions", true,
                this::renderInstructions, false);
        addText(page, copy.homeFootnote(),
                13, MUTED, false, 24, 0);
    }

    private void addLanguagePicker(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(copy.languageLabel(), 13, MUTED, true);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Spinner language = spinner(List.of("English", "Română"), "interface-language");
        language.setSelection(copy.language() == HowToPlay.Language.EN ? 0 : 1);
        row.addView(language, new LinearLayout.LayoutParams(dp(150), dp(48)));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(6);
        parent.addView(row, params);
        language.post(() -> language.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                setUiLanguage(position == 0 ? HowToPlay.Language.EN : HowToPlay.Language.RO);
            }
            public void onNothingSelected(AdapterView<?> adapter) { }
        }));
    }

    private void setUiLanguage(HowToPlay.Language language) {
        if (copy.language() == language) return;
        getPreferences(MODE_PRIVATE).edit()
                .putString("interfaceLanguage", language.name())
                .putString("helpLanguage", language.name())
                .apply();
        copy = new WhistUiText(language);
        if (instructionsVisible) renderInstructions();
        else if (menuSubpage) renderSetup();
        else renderHome();
    }

    private void renderSetup() {
        applyStandardWindowChrome();
        tableVisible=false;menuSubpage=true;instructionsVisible=false;handler.removeCallbacksAndMessages(null);
        LinearLayout page = page(null, copy.setupTitle());
        addLanguagePicker(page);

        Spinner players = spinner(Arrays.asList("3", "4", "5", "6"), "player-count");
        players.setSelection(setupPlayerCount - 3);
        addSetupSelector(page, copy.players(), players);

        Spinner modes = spinner(Arrays.stream(GameMode.values()).map(copy::modeName).collect(Collectors.toList()),
                "game-type");
        modes.setSelection(setupMode.ordinal());
        LinearLayout.LayoutParams modeParams = matchWrap();
        modeParams.topMargin = dp(4);
        page.addView(modes, modeParams);
        addQuietButton(page, copy.rulesAndSchedule(), () -> {
            new AlertDialog.Builder(this).setTitle(copy.modeName(setupMode))
                    .setMessage(copy.modeDescription(setupMode) + "\n\n" + copy.schedulePreview(setupMode, setupPlayerCount)
                            + (setupMode.isRomanian() ? "\n\n" + copy.streakToggle() : ""))
                    .setPositiveButton(copy.close(), null).show();
        });
        CheckBox streakBonuses=new CheckBox(this);
        streakBonuses.setText(copy.romanian() ? "Serii de cinci mâini" : "Five-hand streaks");
        streakBonuses.setTextSize(13);
        streakBonuses.setContentDescription(copy.streakToggle());
        streakBonuses.setTag("romanian-streak-bonuses");
        streakBonuses.setChecked(setupStreaks);


        Spinner streakValue=spinner(Arrays.asList(copy.fivePointsOriginal(),copy.tenPoints()),"romanian-streak-value");
        streakValue.setSelection(setupStreakBonus == 5 ? 0 : 1);

        streakValue.setEnabled(setupStreaks);
        streakBonuses.setOnCheckedChangeListener((button,checked)->{
            setupStreaks=checked;
            streakValue.setEnabled(checked);
        });
        LinearLayout streakRow = new LinearLayout(this);
        streakRow.setGravity(Gravity.CENTER_VERTICAL);
        streakRow.addView(streakBonuses, new LinearLayout.LayoutParams(0, dp(48), 1f));
        streakRow.addView(streakValue, new LinearLayout.LayoutParams(dp(138), dp(48)));
        page.addView(streakRow, matchWrap());
        players.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupPlayerCount = position + 3;
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        streakValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupStreakBonus = position == 0 ? 5 : 10;
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        Runnable updateModeDetails = () -> {
            streakRow.setVisibility(setupMode.isRomanian() ? View.VISIBLE : View.GONE);
        };
        updateModeDetails.run();
        modes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupMode = GameMode.values()[position];
                updateModeDetails.run();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Spinner difficulty = spinner(Arrays.stream(AiDifficulty.values()).map(copy::difficultyName).collect(Collectors.toList()),
                "ai-level");
        difficulty.setSelection(setupDifficulty.ordinal());
        addSetupSelector(page, copy.aiLevel(), difficulty);
        difficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupDifficulty = AiDifficulty.values()[position];
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        addButton(page, copy.startGame(), "btn_start_game", true, () -> {
            int playerCount = Integer.parseInt(String.valueOf(players.getSelectedItem()));
            GameMode mode = GameMode.values()[modes.getSelectedItemPosition()];
            AiDifficulty ai = AiDifficulty.values()[difficulty.getSelectedItemPosition()];
            recordedReplay = null;
            selectedBid = null;
            lastConfig = new GameConfig(playerCount, mode, ai, System.nanoTime(),mode.isRomanian()&&streakBonuses.isChecked(),streakValue.getSelectedItemPosition()==0?5:10);
            game = GameSession.newGame(lastConfig);
            persistGame();
            driveAutomatedTurns();
        }, true);
        addQuietButton(page, copy.back(), this::renderHome);
    }

    private void renderBidding() {
        applyTableWindowChrome();
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = gamePage();
        addGameHeader(page, state);
        boolean lastBid = state.bidsSubmitted() == state.players().size() - 1;
        List<Integer> choices = GameRules.allowedBids(state.handSize(), state.bidTotal(), lastBid);
        if (selectedBid == null || !choices.contains(selectedBid)) selectedBid = choices.get(0);
        GameTablePanel table = GameTablePanel.bidding(this, state, choices, selectedBid,
                bid -> selectedBid = bid, this::submitSelectedBid, copy);
        page.addView(table, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void submitSelectedBid() {
        try {
            game.submitBid(0, selectedBid);
            selectedBid = null;
            persistGame();
            driveAutomatedTurns();
        } catch (RuntimeException invalidBid) {
            Toast.makeText(this, copy.invalidBid(), Toast.LENGTH_SHORT).show();
            renderBidding();
        }
    }

    private void renderPlay() {
        renderPlay(Collections.emptyList(), false);
    }

    private void renderPlay(List<PlayedCard> completedTrick, boolean pauseActive) {
        applyTableWindowChrome();
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = gamePage();
        addGameHeader(page, state);
        boolean humanTurn = !pauseActive && state.phase() == GamePhase.PLAYING && state.currentSeat() == 0;
        Suit ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(state.players().get(0).hand(), ledSuit, state.trump(), state.mode());
        String instruction = humanTurn ? copy.playHint(state) : null;
        GameTablePanel table = GameTablePanel.playing(this, state,
                humanTurn ? legal : java.util.Collections.emptyList(),
                this::playHumanCard, instruction, completedTrick, pauseActive, copy);
        page.addView(table, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void addGameHeader(LinearLayout page, GameSession.Snapshot state) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setTag("game-header");

        FrameLayout menuButton = new FrameLayout(this);
        menuButton.setTag("btn_game_menu");
        menuButton.setContentDescription(copy.gameMenu());
        menuButton.setClickable(true);
        menuButton.setFocusable(true);
        menuButton.setBackground(roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 24));
        menuButton.setOnClickListener(ignored -> showGameMenu(menuButton));
        for (int offset : new int[]{-6, 0, 6}) {
            View line = new View(this);
            line.setBackground(roundedBackground(Color.rgb(225, 239, 228),
                    Color.rgb(225, 239, 228), 2));
            line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(dp(20), dp(2), Gravity.CENTER);
            lineParams.topMargin = dp(offset);
            menuButton.addView(line, lineParams);
        }
        header.addView(menuButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(8), 0, 0, 0);
        TextView title = text(copy.gameTitle(state.mode()), 14, Color.rgb(245, 248, 240), true);
        title.setSingleLine(true);
        title.setTag("game-title");
        TextView progress = text(copy.progress(state), 11, Color.rgb(229, 239, 230), true);
        TextView summary = text(copy.bidSummary(state), 9, Color.rgb(194, 216, 199), false);
        summary.setSingleLine(true);
        status.addView(title, matchWrap());
        status.addView(progress, matchWrap());
        status.addView(summary, matchWrap());
        header.addView(status, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(new TrumpIndicatorView(this, state, copy),
                new LinearLayout.LayoutParams(dp(82), dp(56)));

        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(4);
        page.addView(header, params);
    }

    private void showGameMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(copy.scoreSheet()).setOnMenuItemClickListener(item -> {
            showLiveScoreboard();
            return true;
        });
        menu.getMenu().add(copy.mainMenu()).setOnMenuItemClickListener(item -> {
            renderHome();
            return true;
        });
        menu.show();
    }

    private void showLiveScoreboard() {
        GameSession.Snapshot state = game.snapshot();
        boolean resumeAutomatedTurn = (state.phase() == GamePhase.BIDDING && state.biddingSeat() != 0)
                || (state.phase() == GamePhase.PLAYING && state.currentSeat() != 0);
        handler.removeCallbacksAndMessages(null);
        scoreDialogShowing = true;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(2), dp(8), 0);
        TextView summary = text(copy.bidSummary(state), 12, MUTED, false);
        summary.setPadding(dp(2), dp(2), dp(2), dp(8));
        content.addView(summary, matchWrap());
        if (state.players().get(0).bid() == null) {
            TextView pendingBid = text(copy.bidNotPlaced(), 12, MUTED, true);
            pendingBid.setPadding(dp(2), 0, dp(2), dp(8));
            content.addView(pendingBid, matchWrap());
        }
        ScrollView scoreScroll = new ScrollView(this);
        scoreScroll.setFillViewport(false);
        scoreScroll.addView(new ScoreTableView(this, state, copy),
                new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scoreScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(copy.currentScores(state.handNumber()))
                .setView(content)
                .setPositiveButton(copy.close(), null)
                .create();
        dialog.setOnDismissListener(ignored -> {
            scoreDialogShowing = false;
            if (resumeAutomatedTurn && tableVisible && game != null) driveAutomatedTurns();
        });
        dialog.show();
    }

    private void playHumanCard(Card card) {
        try {
            game.playCard(0, card);
            if (recordedReplay != null) recordedReplay.played(card);
            persistGame();
            driveAutomatedTurns();
        } catch (RuntimeException invalidPlay) {
            Toast.makeText(this, copy.invalidCard(), Toast.LENGTH_SHORT).show();
            renderPlay();
        }
    }

    private void renderScore() {
        applyStandardWindowChrome();
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page(copy.romanian() ? "MÂNA " + state.handNumber()
                : "HAND " + state.handNumber(), copy.scoreSheet());
        addText(page,copy.roundStatus(state),13,MUTED,false,0,4);
        addText(page, state.finalHand() ? copy.gameComplete() : copy.handComplete(),
                16, MUTED, false, 0, 12);
        if(state.finalHand())addText(page,copy.winner(state),18,ACCENT,true,0,12);

        LinearLayout.LayoutParams scoreTableParams = matchWrap();
        scoreTableParams.setMargins(0, 0, 0, dp(8));
        page.addView(new ScoreTableView(this, state, copy), scoreTableParams);
        addQuietButton(page, copy.scoringDetails(), () -> new AlertDialog.Builder(this)
                .setTitle(copy.scoringDetails()).setMessage(copy.scoreRule(state)
                        + (state.streakBonuses() ? "\n\n" + copy.streakSummary(state) : ""))
                .setPositiveButton(copy.close(), null).show());

        if (!state.finalHand()) {
            addButton(page, copy.nextHand(), "btn_next_hand", true, () -> {
                if (recordedReplay != null) {
                    game = recordedReplay.startHand(state.handIndex() + 1,
                            state.players().stream().map(GameSession.PlayerSnapshot::totalScore).collect(Collectors.toList()));
                } else game.startNextHand();
                persistGame();
                driveAutomatedTurns();
            }, true);
        } else {
            addButton(page, copy.playAgain(), "btn_play_again", true, () -> {
                recordedReplay = null;
                game = GameSession.newGame(new GameConfig(lastConfig.playerCount(), lastConfig.mode(),
                        lastConfig.aiDifficulty(), System.nanoTime(),lastConfig.streakBonuses(),lastConfig.streakBonusPoints()));
                persistGame();
                driveAutomatedTurns();
            }, true);
        }
        addQuietButton(page, copy.mainMenu(), this::renderHome);
    }

    private void renderInstructions() {
        applyStandardWindowChrome();
        tableVisible=false;menuSubpage=true;instructionsVisible=true;handler.removeCallbacksAndMessages(null);
        HowToPlay.Language language = copy.language();
        LinearLayout page=page(HowToPlay.title(language),language==HowToPlay.Language.RO
                ? "Whist românesc — reguli și exemple" : "Romanian Whist — rules and examples");
        addLanguagePicker(page);
        for(var section:HowToPlay.sections(language))addRule(page,section.title(),section.explanation());
        addButton(page,copy.back(),"btn_instructions_back",true,this::renderHome,true);
    }

    private void driveAutomatedTurns() {
        handler.removeCallbacksAndMessages(null);
        if (scoreDialogShowing) return;
        if (game == null) {
            renderHome();
            return;
        }
        GameSession.Snapshot state = game.snapshot();
        tableVisible=true;
        CompletedTrickPause.DisplayState trickPause = completedTrickPause.evaluate(
                state, SystemClock.uptimeMillis());
        if (trickPause.active()) {
            renderPlay(trickPause.cards(), true);
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
            if (state.biddingSeat() == 0) {
                renderBidding();
                return;
            }
            int seat = state.biddingSeat();
            game.submitBid(seat, recordedReplay != null ? recordedReplay.bid(seat)
                    : AiPolicy.chooseBid(state, seat, lastConfig.aiDifficulty()));
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
            Card choice = recordedReplay != null ? recordedReplay.nextCard()
                    : AiPolicy.chooseCard(current, seat, lastConfig.aiDifficulty());
            game.playCard(seat, choice);
            if (recordedReplay != null) recordedReplay.played(choice);
            persistGame();
            driveAutomatedTurns();
        }, 550L);
    }

    private void renderCurrentState() {
        if (game == null) {
            renderHome();
        } else {
            driveAutomatedTurns();
        }
    }

    private void persistGame() {
        if (game != null) GameSessionStore.save(this, game);
        if (recordedReplay != null) recordedReplay.export(game);
    }

    private void openOneTrickFixture(boolean persistFixture) {
        GameConfig config = new GameConfig(4, GameMode.PRACTICE_HAND, AiDifficulty.NORMAL, 20261101L);
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
    private LinearLayout page(String eyebrow, String title) {
        return page(eyebrow, title, 22);
    }

    private LinearLayout page(String eyebrow, String title, int horizontalPadding) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(horizontalPadding), dp(16), dp(horizontalPadding), dp(14));
        content.setBackgroundColor(BACKGROUND);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        if (eyebrow != null && !eyebrow.isBlank()) addText(content, eyebrow, 12, ACCENT, true, 0, 8);
        if (title != null && !title.isBlank()) addText(content, title, 25, PINE, true, 0, 8);
        return content;
    }

    private LinearLayout gamePage() {
        ScrollView viewport = new ScrollView(this);
        viewport.setFillViewport(true);
        viewport.setBackgroundColor(WhistUiPalette.TABLE_FELT);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(12), dp(6), dp(12), dp(4));
        content.setBackgroundColor(WhistUiPalette.TABLE_FELT);
        viewport.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(viewport);
        return content;
    }

    private void applyStandardWindowChrome() {
        getWindow().setStatusBarColor(PINE);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void applyTableWindowChrome() {
        getWindow().setStatusBarColor(WhistUiPalette.TABLE_FELT);
        getWindow().setNavigationBarColor(WhistUiPalette.TABLE_FELT);
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private void addRule(LinearLayout page, String heading, String body) {
        addText(page, heading, 16, INK, true, 18, 4);
        addText(page, body, 15, MUTED, false, 0, 4);
    }

    private void addText(LinearLayout parent, String value, int size, int color,
            boolean bold, int top, int bottom) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(top), 0, dp(bottom));
        parent.addView(view, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private void addButton(LinearLayout parent, String label, String description, boolean enabled,
            Runnable action, boolean primary) {
        Button button = actionButton(label, description, enabled, action, primary, 16);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, dp(3));
        parent.addView(button, params);
    }

    private Button actionButton(String label, String description, boolean enabled,
            Runnable action, boolean primary, int textSize) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(textSize);
        button.setTextColor(primary ? Color.WHITE : PINE);
        button.setContentDescription(label);
        button.setTag(description);
        button.setEnabled(enabled);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setBackground(buttonBackground(primary));
        button.setOnClickListener(ignored -> action.run());
        return button;
    }

    private Spinner spinner(List<String> values, String description) {
        Spinner spinner = new Spinner(this);
        spinner.setContentDescription(copy.spinnerLabel(description));
        spinner.setTag(description);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(values)) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                TextView selected = (TextView) super.getView(position, recycled, parent);
                selected.setTextSize(14);
                selected.setTextColor(INK);
                selected.setSingleLine(true);
                selected.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return selected;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setGravity(Gravity.CENTER_VERTICAL);
        spinner.setBackground(new DropdownBackground(getResources().getDisplayMetrics().density));
        spinner.setMinimumHeight(dp(48));
        spinner.setPadding(dp(12), dp(4), dp(34), dp(4));
        return spinner;
    }

    private void addSetupSelector(LinearLayout parent, String label, Spinner selector) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 14, INK, true), new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(selector, new LinearLayout.LayoutParams(dp(150), dp(48)));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(8);
        parent.addView(row, params);
    }

    private void addQuietButton(LinearLayout parent, String label, Runnable action) {
        Button button = actionButton(label, label, true, action, false, 14);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setElevation(0);
        button.setStateListAnimator(null);
        parent.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
    }

    private GradientDrawable buttonBackground(boolean primary) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? ACCENT : SURFACE);
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), primary ? ACCENT : Color.rgb(211, 220, 210));
        return shape;
    }

    private GradientDrawable roundedBackground(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}

package ro.whistlab.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import ro.whistlab.core.Suit;
import ro.whistlab.core.Trump;

public final class MainActivity extends Activity {
    private static final int PINE = Color.rgb(29, 54, 43);
    private static final int INK = Color.rgb(41, 54, 46);
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int ACCENT = Color.rgb(48, 111, 78);
    private static final int SURFACE = Color.rgb(255, 254, 248);
    private static final int BACKGROUND = Color.rgb(246, 245, 237);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameSession game;
    private GameConfig lastConfig;
    private OriginalMatchReplay recordedReplay;
    private boolean tableVisible;
    private boolean menuSubpage;
    private boolean instructionsVisible;
    private boolean scoreDialogShowing;
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
        copy = new WhistUiText(WhistUiText.parseLanguage(selectedLanguage));
        getWindow().setStatusBarColor(PINE);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
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
        tableVisible=false;
        menuSubpage=false;
        instructionsVisible=false;
        handler.removeCallbacksAndMessages(null);
        LinearLayout page = page("WHIST LAB", copy.homeSubtitle());
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
        addText(parent, copy.languageLabel(), 13, MUTED, true, 10, 3);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setGravity(Gravity.CENTER_VERTICAL);
        addLanguageOption(choices, HowToPlay.Language.EN, "English");
        addLanguageOption(choices, HowToPlay.Language.RO, "Română");
        parent.addView(choices, matchWrap());
    }

    private void addLanguageOption(LinearLayout parent, HowToPlay.Language language, String label) {
        boolean selected = copy.language() == language;
        Button option = new Button(this);
        option.setAllCaps(false);
        option.setText(label);
        option.setTextSize(14);
        option.setTextColor(selected ? Color.WHITE : PINE);
        option.setContentDescription(copy.languageOptionDescription(language, selected));
        option.setTag("language:" + language.name());
        option.setSelected(selected);
        option.setMinimumWidth(0);
        option.setMinHeight(dp(42));
        option.setBackground(buttonBackground(selected));
        option.setOnClickListener(ignored -> setUiLanguage(language));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(0, 0, dp(8), 0);
        parent.addView(option, params);
    }

    private void setUiLanguage(HowToPlay.Language language) {
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
        tableVisible=false;menuSubpage=true;instructionsVisible=false;handler.removeCallbacksAndMessages(null);
        LinearLayout page = page(copy.setupEyebrow(), copy.setupTitle());
        addText(page, copy.setupHint(), 15, MUTED, false, 0, 4);
        addLanguagePicker(page);

        addText(page, copy.players(), 16, INK, true, 18, 4);
        Spinner players = spinner(Arrays.asList("3", "4", "5", "6"), "player-count");
        players.setSelection(setupPlayerCount - 3);
        page.addView(players, matchWrap());
        addText(page, copy.playerCountHint(), 13, MUTED, false, 3, 8);

        addText(page, copy.gameType(), 16, INK, true, 16, 4);
        Spinner modes = spinner(Arrays.stream(GameMode.values()).map(copy::modeName).collect(Collectors.toList()),
                "game-type");
        modes.setSelection(setupMode.ordinal());
        page.addView(modes, matchWrap());
        TextView modeHelp=text(copy.modeDescription(setupMode),13,MUTED,false);
        page.addView(modeHelp,matchWrap());
        TextView schedulePreview = text(copy.schedulePreview(setupMode, setupPlayerCount), 13, MUTED, false);
        schedulePreview.setPadding(dp(10), dp(9), dp(10), dp(9));
        schedulePreview.setBackground(roundedBackground(Color.rgb(235, 241, 233),
                Color.rgb(205, 219, 204), 10));
        schedulePreview.setContentDescription(copy.schedulePreview(setupMode, setupPlayerCount));
        LinearLayout.LayoutParams scheduleParams = matchWrap();
        scheduleParams.topMargin = dp(8);
        page.addView(schedulePreview, scheduleParams);
        CheckBox streakBonuses=new CheckBox(this);
        streakBonuses.setText(copy.streakToggle());
        streakBonuses.setContentDescription(copy.streakToggle());
        streakBonuses.setTag("romanian-streak-bonuses");
        streakBonuses.setChecked(setupStreaks);
        streakBonuses.setVisibility(View.GONE);
        page.addView(streakBonuses,matchWrap());
        Spinner streakValue=spinner(Arrays.asList(copy.fivePointsOriginal(),copy.tenPoints()),"romanian-streak-value");
        TextView streakValueLabel=text(copy.streakBonusLabel(),13,MUTED,false);
        streakValueLabel.setVisibility(View.GONE);
        streakValue.setSelection(setupStreakBonus == 5 ? 0 : 1);
        streakValue.setVisibility(View.GONE);
        streakValue.setEnabled(setupStreaks);
        streakBonuses.setOnCheckedChangeListener((button,checked)->{
            setupStreaks=checked;
            streakValue.setEnabled(checked);
        });
        page.addView(streakValueLabel,matchWrap());
        page.addView(streakValue,matchWrap());
        players.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupPlayerCount = position + 3;
                schedulePreview.setText(copy.schedulePreview(setupMode, setupPlayerCount));
                schedulePreview.setContentDescription(copy.schedulePreview(setupMode, setupPlayerCount));
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
            modeHelp.setText(copy.modeDescription(setupMode));
            String schedule = copy.schedulePreview(setupMode, setupPlayerCount);
            schedulePreview.setText(schedule);
            schedulePreview.setContentDescription(schedule);
            streakBonuses.setVisibility(setupMode.isRomanian()?View.VISIBLE:View.GONE);
            streakValue.setVisibility(setupMode.isRomanian()?View.VISIBLE:View.GONE);
            streakValueLabel.setVisibility(setupMode.isRomanian()?View.VISIBLE:View.GONE);
        };
        updateModeDetails.run();
        modes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setupMode = GameMode.values()[position];
                updateModeDetails.run();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        addText(page, copy.aiLevel(), 16, INK, true, 16, 4);
        Spinner difficulty = spinner(Arrays.stream(AiDifficulty.values()).map(copy::difficultyName).collect(Collectors.toList()),
                "ai-level");
        difficulty.setSelection(setupDifficulty.ordinal());
        page.addView(difficulty, matchWrap());
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
        addButton(page, copy.back(), "btn_setup_back", true, this::renderHome, false);
    }

    private void renderBidding() {
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page("", "", 12);
        addGameHeader(page, state);
        boolean lastBid = state.bidsSubmitted() == state.players().size() - 1;
        List<Integer> choices = GameRules.allowedBids(state.handSize(), state.bidTotal(), lastBid);
        if (selectedBid == null || !choices.contains(selectedBid)) selectedBid = choices.get(0);
        page.addView(GameTablePanel.bidding(this, state, choices, selectedBid,
                bid -> selectedBid = bid, copy), matchWrap());

        Button submitBid = actionButton(copy.placeBid(), "btn_submit_bid", true, () -> {
            try {
                int bid = selectedBid;
                game.submitBid(0, bid);
                selectedBid = null;
                persistGame();
                driveAutomatedTurns();
            } catch (RuntimeException invalidBid) {
                Toast.makeText(this, copy.invalidBid(), Toast.LENGTH_SHORT).show();
                renderBidding();
            }
        }, true, 14);
        FrameLayout submitRow = new FrameLayout(this);
        submitRow.addView(submitBid, new FrameLayout.LayoutParams(dp(178), dp(46), Gravity.CENTER));
        page.addView(submitRow, matchWrap());
    }

    private void renderPlay() {
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page("", "", 12);
        addGameHeader(page, state);
        boolean humanTurn = state.currentSeat() == 0;
        Suit ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(state.players().get(0).hand(), ledSuit, state.trump(), state.mode());
        String instruction = humanTurn ? copy.playHint(state)
                : copy.waitingFor(copy.playerName(state.players().get(state.currentSeat())));
        page.addView(GameTablePanel.playing(this, state,
                humanTurn ? legal : java.util.Collections.emptyList(),
                this::playHumanCard, instruction, copy), matchWrap());
    }

    private void addGameHeader(LinearLayout page, GameSession.Snapshot state) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout menuButton = new FrameLayout(this);
        menuButton.setTag("btn_game_menu");
        menuButton.setContentDescription(copy.gameMenu());
        menuButton.setClickable(true);
        menuButton.setFocusable(true);
        menuButton.setBackground(roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 24));
        menuButton.setOnClickListener(ignored -> showGameMenu(menuButton));
        for (int offset : new int[]{-6, 0, 6}) {
            View line = new View(this);
            line.setBackground(roundedBackground(ACCENT, ACCENT, 2));
            line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(dp(20), dp(2), Gravity.CENTER);
            lineParams.topMargin = dp(offset);
            menuButton.addView(line, lineParams);
        }
        header.addView(menuButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(8), 0, 0, 0);
        TextView progress = text(copy.progress(state), 18, PINE, true);
        TextView summary = text(copy.bidSummary(state), 11, MUTED, false);
        status.addView(progress, matchWrap());
        status.addView(summary, matchWrap());
        header.addView(status, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

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
        String details = state.players().stream().map(player -> {
            String bid = player.bid() == null
                    ? (copy.romanian() ? "nepusă" : "not placed") : player.bid().toString();
            return copy.playerName(player) + " — " + player.totalScore() + " " + copy.pointsLabel()
                    + " · " + copy.bidLabel() + " " + bid
                    + " · " + copy.tricksWonLabel() + " " + player.tricksWon();
        }).collect(Collectors.joining("\n\n"));
        handler.removeCallbacksAndMessages(null);
        scoreDialogShowing = true;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(copy.currentScores(state.handNumber()))
                .setMessage(details)
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
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page(copy.romanian() ? "MÂNA " + state.handNumber()
                : "HAND " + state.handNumber(), copy.scoreSheet());
        addText(page,copy.roundStatus(state),13,MUTED,false,0,4);
        addText(page, state.finalHand() ? copy.gameComplete() : copy.handComplete(),
                16, MUTED, false, 0, 12);
        if(state.finalHand())addText(page,copy.winner(state),18,ACCENT,true,0,12);
        addText(page, copy.scoreRule(state), 14, MUTED, false, 0, 14);
        if(state.streakBonuses())addText(page,copy.streakSummary(state),13,MUTED,false,0,10);

        for (GameSession.PlayerSnapshot player : state.players()) {
            int bid = player.bid() == null ? 0 : player.bid();
            int earned = GameRules.scoreDelta(player.tricksWon(), bid, state.mode())+player.streakPoints();
            String row = copy.scoreLine(player, earned);
            TextView score = text(row, 15, INK, player.human());
            score.setContentDescription(copy.scoreAccessibility(player, earned));
            score.setTag("score:" + player.seat());
            score.setPadding(dp(12), dp(14), dp(12), dp(14));
            score.setBackground(roundedBackground(SURFACE,
                    Color.rgb(211, 220, 210), 12));
            LinearLayout.LayoutParams params = matchWrap();
            params.setMargins(0, dp(4), 0, dp(4));
            page.addView(score, params);
        }

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
        addButton(page, copy.mainMenu(), "btn_score_home", true, this::renderHome, false);
    }

    private void renderInstructions() {
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
        if (state.phase() == GamePhase.SCORE) {
            renderScore();
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
        addText(content, eyebrow, 12, ACCENT, true, 0, 8);
        if (title != null && !title.isBlank()) addText(content, title, 25, PINE, true, 0, 8);
        return content;
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
        button.setMinHeight(dp(44));
        button.setBackground(buttonBackground(primary));
        button.setOnClickListener(ignored -> action.run());
        return button;
    }

    private Spinner spinner(List<String> values, String description) {
        Spinner spinner = new Spinner(this);
        spinner.setContentDescription(copy.spinnerLabel(description));
        spinner.setTag(description);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(values));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setGravity(Gravity.CENTER_VERTICAL);
        spinner.setBackground(buttonBackground(false));
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        return spinner;
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

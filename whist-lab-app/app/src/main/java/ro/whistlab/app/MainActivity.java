package ro.whistlab.app;

import android.app.Activity;
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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import ro.whistlab.core.AiDifficulty;
import ro.whistlab.core.AiPolicy;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameConfig;
import ro.whistlab.core.GameMode;
import ro.whistlab.core.GamePhase;
import ro.whistlab.core.GameRules;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.PlayedCard;
import ro.whistlab.core.MatchSummary;
import ro.whistlab.core.Suit;
import ro.whistlab.core.Trump;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(21, 34, 73);
    private static final int INK = Color.rgb(28, 38, 59);
    private static final int MUTED = Color.rgb(100, 112, 132);
    private static final int BLUE = Color.rgb(71, 91, 201);
    private static final int SURFACE = Color.WHITE;
    private static final int BACKGROUND = Color.rgb(242, 245, 250);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameSession game;
    private GameConfig lastConfig;
    private Spinner bidSpinner;
    private OriginalMatchReplay recordedReplay;
    private boolean tableVisible;
    private boolean menuSubpage;

    @Override protected void onResume(){super.onResume();if(tableVisible&&game!=null)driveAutomatedTurns();}
    @Override protected void onStop(){handler.removeCallbacksAndMessages(null);persistGame();super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){if(tableVisible||menuSubpage)renderHome();else super.onBackPressed();}

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
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
                Toast.makeText(this, "Saved game could not be resumed and was cleared.", Toast.LENGTH_LONG).show();
            }
            renderHome();
        }
    }

    private void renderHome() {
        tableVisible=false;
        menuSubpage=false;
        handler.removeCallbacksAndMessages(null);
        LinearLayout page = page("WHIST LAB", "A thoughtful trick-taking table");
        addText(page, "Make an exact bid. Follow the suit. Win the tricks you called.",
                16, MUTED, false, 0, 20);
        addButton(page, "New game", "btn_new_game", true, this::renderSetup, true);
        addButton(page, "Resume game", "btn_resume_game", game != null,
                this::renderCurrentState, false);
        addButton(page, "How to play", "btn_instructions", true,
                this::renderInstructions, false);
        addText(page, "Play locally against computer opponents. Your game state stays on this device.",
                13, MUTED, false, 24, 0);
    }

    private void renderSetup() {
        tableVisible=false;menuSubpage=true;handler.removeCallbacksAndMessages(null);
        LinearLayout page = page("NEW GAME", "Set up your table");
        addText(page, "Choose a game size and opponents.", 15, MUTED, false, 0, 12);

        addText(page, "Players", 16, INK, true, 18, 4);
        Spinner players = spinner(Arrays.asList("3", "4", "5", "6"), "player-count");
        players.setSelection(1);
        page.addView(players, matchWrap());
        addText(page, "Including you. The other seats are local AI players.", 13, MUTED, false, 3, 8);

        addText(page, "Game type", 16, INK, true, 16, 4);
        Spinner modes = spinner(Arrays.stream(GameMode.values()).map(GameMode::label).collect(Collectors.toList()),
                "game-type");
        modes.setSelection(0);
        page.addView(modes, matchWrap());
        TextView modeHelp=text(MatchSummary.modeDescription(GameMode.OH_HELL),13,MUTED,false);
        page.addView(modeHelp,matchWrap());
        CheckBox streakBonuses=new CheckBox(this);
        streakBonuses.setText("Romanian five-hand streaks (missed series: -5)");
        streakBonuses.setContentDescription("romanian-streak-bonuses");
        streakBonuses.setChecked(true);
        streakBonuses.setVisibility(View.GONE);
        page.addView(streakBonuses,matchWrap());
        Spinner streakValue=spinner(Arrays.asList("5 points (original)","10 points"),"romanian-streak-value");
        TextView streakValueLabel=text("Five-hand exact-contract bonus",13,MUTED,false);
        streakValueLabel.setVisibility(View.GONE);
        streakValue.setSelection(1);
        streakValue.setVisibility(View.GONE);
        streakBonuses.setOnCheckedChangeListener((button,checked)->streakValue.setEnabled(checked));
        modes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                modeHelp.setText(MatchSummary.modeDescription(GameMode.values()[position]));
                streakBonuses.setVisibility(GameMode.values()[position].isRomanian()?View.VISIBLE:View.GONE);
                streakValue.setVisibility(GameMode.values()[position].isRomanian()?View.VISIBLE:View.GONE);
                streakValueLabel.setVisibility(GameMode.values()[position].isRomanian()?View.VISIBLE:View.GONE);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        addText(page, "AI level", 16, INK, true, 16, 4);
        Spinner difficulty = spinner(Arrays.stream(AiDifficulty.values()).map(AiDifficulty::label).collect(Collectors.toList()),
                "ai-level");
        difficulty.setSelection(0);
        page.addView(difficulty, matchWrap());
        page.addView(streakValueLabel,matchWrap());
        page.addView(streakValue,matchWrap());

        addButton(page, "Start game", "btn_start_game", true, () -> {
            int playerCount = Integer.parseInt(String.valueOf(players.getSelectedItem()));
            GameMode mode = GameMode.values()[modes.getSelectedItemPosition()];
            AiDifficulty ai = AiDifficulty.values()[difficulty.getSelectedItemPosition()];
            recordedReplay = null;
            lastConfig = new GameConfig(playerCount, mode, ai, System.nanoTime(),mode.isRomanian()&&streakBonuses.isChecked(),streakValue.getSelectedItemPosition()==0?5:10);
            game = GameSession.newGame(lastConfig);
            persistGame();
            driveAutomatedTurns();
        }, true);
        addButton(page, "Back", "btn_setup_back", true, this::renderHome, false);
    }

    private void renderBidding() {
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page("HAND " + state.handNumber(), "Place your bid");
        addText(page,MatchSummary.progress(state),13,MUTED,false,0,4);
        addText(page, state.handSize() + (state.handSize() == 1 ? " card in this hand" : " cards in this hand"),
                18, INK, true, 0, 6);
        addText(page, "Trump: " + trumpLabel(state.trump())
                + (state.turnedCard() == null ? "" : " | Turned: " + state.turnedCard().shortLabel()), 15, MUTED, false, 0, 12);
        addText(page, "Bids so far: " + state.bidTotal()+" · Balance: "+MatchSummary.bidBalance(state), 15, MUTED, false, 0, 18);

        boolean lastBid = state.bidsSubmitted() == state.players().size() - 1;
        List<String> choices = GameRules.allowedBids(state.handSize(), state.bidTotal(), lastBid)
                .stream().map(String::valueOf).collect(Collectors.toList());
        bidSpinner = spinner(choices, "bid-options");
        page.addView(bidSpinner, matchWrap());
        if (lastBid) {
            addText(page, "Your bid cannot make total bids equal the number of cards.",
                    13, MUTED, false, 2, 8);
        }
        addButton(page, "Place bid", "btn_submit_bid", true, () -> {
            try {
                int bid = Integer.parseInt(String.valueOf(bidSpinner.getSelectedItem()));
                game.submitBid(0, bid);
                persistGame();
                driveAutomatedTurns();
            } catch (RuntimeException invalidBid) {
                Toast.makeText(this, invalidBid.getMessage(), Toast.LENGTH_SHORT).show();
                renderBidding();
            }
        }, true);
        addButton(page, "Main menu", "btn_bidding_home", true, this::renderHome, false);
    }

    private void renderPlay() {
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page("HAND " + state.handNumber() + " · " + state.trump(),
                "Play a card");
        addText(page,MatchSummary.progress(state),13,MUTED,false,0,4);
        addText(page, "Trump: " + trumpLabel(state.trump()) + "  ·  Cards in hand: " + state.handSize(),
                14, MUTED, false, 0, 12);

        addText(page, "Players", 16, INK, true, 12, 4);
        for (GameSession.PlayerSnapshot player : state.players()) {
            String bid = player.bid() == null ? "—" : Integer.toString(player.bid());
            String line = player.name() + "  ·  Bid " + bid + "  ·  Won " + player.tricksWon()
                    + "  ·  Score " + player.totalScore();
            addText(page, line, player.human() ? 15 : 14, player.human() ? INK : MUTED,
                    player.human(), 2, 3);
        }

        addText(page, "Table", 16, INK, true, 16, 4);
        if (state.currentTrick().isEmpty()) {
            addText(page, "Lead a card to begin the trick.", 14, MUTED, false, 2, 6);
        } else {
            for (PlayedCard play : state.currentTrick()) {
                GameSession.PlayerSnapshot player = state.players().get(play.seat());
                addText(page, player.name() + " played " + play.card().accessibleLabel(),
                        14, INK, false, 2, 3);
            }
        }

        boolean humanTurn = state.currentSeat() == 0;
        if(humanTurn)addText(page,MatchSummary.playHint(state),13,MUTED,false,4,4);
        addText(page, humanTurn ? "Your turn" : "Waiting for " + state.players().get(state.currentSeat()).name(),
                16, humanTurn ? BLUE : MUTED, true, 16, 8);
        Suit ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(state.players().get(0).hand(), ledSuit, state.trump(), state.mode());
        GridLayout cards = new GridLayout(this);
        cards.setColumnCount(4);
        cards.setUseDefaultMargins(true);
        for (Card card : state.players().get(0).hand()) {
            boolean playable = humanTurn && legal.contains(card);
            Button cardButton = new Button(this);
            cardButton.setAllCaps(false);
            cardButton.setText(card.shortLabel());
            cardButton.setTextSize(19);
            cardButton.setTextColor(card.suit().red() ? Color.rgb(190, 51, 67) : INK);
            cardButton.setContentDescription(card.accessibleLabel());
            cardButton.setTag("card:" + card.suit().name() + ":" + card.rank());
            cardButton.setEnabled(playable);
            cardButton.setAlpha(playable ? 1.0f : 0.5f);
            cardButton.setBackground(cardBackground());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(76);
            params.height = dp(94);
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            cards.addView(cardButton, params);
            cardButton.setOnClickListener(ignored -> {
                try {
                    game.playCard(0, card);
                    if (recordedReplay != null) recordedReplay.played(card);
                    persistGame();
                    driveAutomatedTurns();
                } catch (RuntimeException invalidPlay) {
                    Toast.makeText(this, invalidPlay.getMessage(), Toast.LENGTH_SHORT).show();
                    renderPlay();
                }
            });
        }
        page.addView(cards, matchWrap());
        addButton(page, "Main menu", "btn_game_home", true, this::renderHome, false);
    }

    private void renderScore() {
        GameSession.Snapshot state = game.snapshot();
        LinearLayout page = page("HAND " + state.handNumber(), "Score sheet");
        addText(page,MatchSummary.progress(state),13,MUTED,false,0,4);
        addText(page, state.finalHand() ? "Game complete" : "Hand complete",
                16, MUTED, false, 0, 12);
        if(state.finalHand())addText(page,MatchSummary.winners(state),18,BLUE,true,0,12);
        addText(page, state.mode().isRomanian()
                ? "Exact bid: 5 + tricks. Otherwise: minus the bid difference."
                : "Each trick is one point. An exact bid adds ten points.",
                14, MUTED, false, 0, 14);
        if(state.streakBonuses())addText(page,"Five eligible exact contracts: +"+state.streakBonusPoints()+"; five missed: -5, then reset. One-card hands do not advance a streak; opposite outcomes break it.",13,MUTED,false,0,10);

        for (GameSession.PlayerSnapshot player : state.players()) {
            int bid = player.bid() == null ? 0 : player.bid();
            int earned = GameRules.scoreDelta(player.tricksWon(), bid, state.mode())+player.streakPoints();
            String row = player.name() + " | Bid " + bid + " | Won " + player.tricksWon()
                    + " | " + (earned >= 0 ? "+" : "") + earned
                    + (player.streakPoints()==0?"":" (streak "+(player.streakPoints()>0?"+":"")+player.streakPoints()+")")
                    + " | Total: " + player.totalScore();
            TextView score = text(row, 15, INK, player.human());
            score.setContentDescription("score-" + player.seat() + ": " + row);
            score.setTag("score:" + player.seat());
            score.setPadding(dp(12), dp(14), dp(12), dp(14));
            score.setBackground(cardBackground());
            LinearLayout.LayoutParams params = matchWrap();
            params.setMargins(0, dp(4), 0, dp(4));
            page.addView(score, params);
        }

        if (!state.finalHand()) {
            addButton(page, "Next hand", "btn_next_hand", true, () -> {
                if (recordedReplay != null) {
                    game = recordedReplay.startHand(state.handIndex() + 1,
                            state.players().stream().map(GameSession.PlayerSnapshot::totalScore).collect(Collectors.toList()));
                } else game.startNextHand();
                persistGame();
                driveAutomatedTurns();
            }, true);
        } else {
            addButton(page, "Play again", "btn_play_again", true, () -> {
                recordedReplay = null;
                game = GameSession.newGame(new GameConfig(lastConfig.playerCount(), lastConfig.mode(),
                        lastConfig.aiDifficulty(), System.nanoTime(),lastConfig.streakBonuses(),lastConfig.streakBonusPoints()));
                persistGame();
                driveAutomatedTurns();
            }, true);
        }
        addButton(page, "Main menu", "btn_score_home", true, this::renderHome, false);
    }

    private void renderInstructions() {
        tableVisible=false;menuSubpage=true;handler.removeCallbacksAndMessages(null);
        LinearLayout page = page("HOW TO PLAY", "Simple rules. Sharp decisions.");
        addRule(page, "Bidding", "Bid the exact number of tricks you expect to win. The final bid cannot make all bids add up to the cards in the hand.");
        addRule(page, "Play", "Follow the suit led if you can. Romanian Whist: when void you must trump if you have one; otherwise discard. Oh Hell: when void you may discard or trump.");
        addRule(page, "Winning a trick", "The highest card of the led suit wins unless a trump is played. Then the highest trump wins.");
        addRule(page, "Trump", "Romanian Whist and Oh Hell turn an undealt card to determine trump. Romanian eight-card hands have no trump. Descending cycles spades, hearts, clubs, diamonds, then no trump.");
        addRule(page, "Scoring", "Romanian Whist: exact bid scores 5 + tricks, otherwise lose the difference. Oh Hell: tricks + 10 for an exact bid.");
        addRule(page, "Romanian streaks", "Five consecutive eligible exact contracts add the selected 5 or 10 points (default 10). Five missed contracts deduct 5. The series then resets. One-card hands do not advance the streak, but an opposite result breaks it. Disable streaks for classic scoring.");
        addButton(page, "Back", "btn_instructions_back", true, this::renderHome, true);
    }

    private void driveAutomatedTurns() {
        handler.removeCallbacksAndMessages(null);
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(26), dp(22), dp(30));
        content.setBackgroundColor(BACKGROUND);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        addText(content, eyebrow, 12, BLUE, true, 0, 8);
        addText(content, title, 25, NAVY, true, 0, 8);
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
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(primary ? Color.WHITE : NAVY);
        button.setContentDescription(description);
        button.setEnabled(enabled);
        button.setBackground(buttonBackground(primary));
        button.setOnClickListener(ignored -> action.run());
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, dp(3));
        parent.addView(button, params);
    }

    private Spinner spinner(List<String> values, String description) {
        Spinner spinner = new Spinner(this);
        spinner.setContentDescription(description);
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
        shape.setColor(primary ? BLUE : SURFACE);
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), primary ? BLUE : Color.rgb(218, 224, 235));
        return shape;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SURFACE);
        shape.setCornerRadius(dp(12));
        shape.setStroke(dp(1), Color.rgb(223, 228, 238));
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String trumpLabel(Trump trump) {
        return trump.suit().map(Suit::label).orElse("No trump");
    }
}

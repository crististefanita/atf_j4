package ro.whistlab.app.screens;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.table.GameTablePanel;
import ro.whistlab.app.theme.GameHeaderTypography;
import ro.whistlab.app.table.TrumpIndicatorView;
import ro.cardtable.ui.screens.GameTitleBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;

/** Game table and header composition; decisions and navigation stay with their owners. */
public final class GameScreen {
    static final class AutoGameHeaderContent {
        record State(boolean visible, boolean checked, String label, String startDescription, String stopDescription) { }

        static State state(GamePhase phase, boolean active, WhistUiText copy) {
            java.util.Objects.requireNonNull(phase);
            java.util.Objects.requireNonNull(copy);
            boolean visible = phase == GamePhase.BIDDING || phase == GamePhase.PLAYING;
            return new State(visible, visible && active, copy.autoGameToggle(),
                    copy.autoGameHand(), copy.stopAutoGameHand());
        }

        private AutoGameHeaderContent() { }
    }

    private GameTablePanel currentTable;
    private Runnable cancelFirstDraw = () -> { };

    public void cancelCollection() {
        cancelFirstDraw.run();
        cancelFirstDraw = () -> { };
        if (currentTable != null) { currentTable.cancelCollection(); currentTable = null; }
    }
    private final ScreenViews views;
    private final Activity activity;
    public GameScreen(ScreenViews views) { this.views = views; this.activity = views.activity(); }
    public record MenuActions(Runnable scoreboard, Runnable settings, Runnable scores, Runnable home,
                              Runnable autoGame, boolean autoGameActive,
                              java.util.function.Supplier<Runnable> opened) { }
    public void bidding(WhistUiText copy, GameSession.Snapshot state, List<Integer> choices, int selectedBid,
            boolean autoGame, java.util.function.IntConsumer selected, Runnable submitted, MenuActions actions) {
        cancelCollection();
        views.applyTableWindowChrome();
        LinearLayout page = views.gamePage();
        addGameHeader(page, state, !autoGame, copy, actions);
        GameTablePanel table = GameTablePanel.bidding(activity, state, choices, selectedBid, selected, submitted,
                copy);
        currentTable = table;
        page.addView(table, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }
    public void playing(WhistUiText copy, GameSession.Snapshot state, List<PlayedCard> completedTrick,
            boolean pauseActive, boolean autoGame, LongSupplier startCollection, Runnable resized,
            java.util.function.Consumer<Card> selected, MenuActions actions) {
        cancelCollection();
        views.applyTableWindowChrome();
        LinearLayout page = views.gamePage();
        addGameHeader(page, state, !pauseActive && !autoGame, copy, actions);
        boolean humanTurn = !pauseActive && !autoGame && state.phase() == GamePhase.PLAYING && state.currentSeat() == 0;
        Suit ledSuit = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(state.players().get(0).hand(), ledSuit, state.trump(), state.mode());
        String instruction = humanTurn ? copy.playHint(state) : null;
        GameTablePanel table = GameTablePanel.playing(activity, state,
                humanTurn ? legal : java.util.Collections.emptyList(),
                selected, instruction, completedTrick, pauseActive || autoGame, copy);
        currentTable = table;
        page.addView(table, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        if (pauseActive && startCollection != null) attachAtFirstDraw(table, startCollection, resized);
    }

    private void attachAtFirstDraw(GameTablePanel table, LongSupplier startCollection, Runnable resized) {
        var observer = table.getViewTreeObserver();
        ViewTreeObserver.OnPreDrawListener firstDraw = () -> {
            cancelFirstDraw.run();
            cancelFirstDraw = () -> { };
            if (currentTable == table) {
                long deadline = startCollection.getAsLong();
                if (deadline > 0) table.collectCompletedTrick(deadline - SystemClock.uptimeMillis(), resized);
            }
            return true;
        };
        cancelFirstDraw = () -> (observer.isAlive() ? observer : table.getViewTreeObserver())
                .removeOnPreDrawListener(firstDraw);
        observer.addOnPreDrawListener(firstDraw);
    }
    private void addGameHeader(LinearLayout page, GameSession.Snapshot state,
            boolean currentPlayerHighlighted, WhistUiText copy, MenuActions actions) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setTag("game-header");

        FrameLayout menuButton = new FrameLayout(activity);
        menuButton.setTag("btn_game_menu");
        menuButton.setContentDescription(copy.gameMenu());
        menuButton.setClickable(true);
        menuButton.setFocusable(true);
        menuButton.setBackground(views.roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 24));
        menuButton.setOnClickListener(ignored -> showMenu(menuButton, copy, actions));
        for (int offset : new int[]{-6, 0, 6}) {
            View line = new View(activity);
            line.setBackground(views.roundedBackground(Color.rgb(225, 239, 228),
                    Color.rgb(225, 239, 228), 2));
            line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(views.dp(20), views.dp(2), Gravity.CENTER);
            lineParams.topMargin = views.dp(offset);
            menuButton.addView(line, lineParams);
        }
        int titleColor = Color.rgb(245, 248, 240);
        TextView title = views.text(copy.gameTitle(state.mode()), GameHeaderTypography.TITLE_SP, titleColor, true);
        title.setSingleLine(false);
        title.setTag("game-title");
        var autoGame = AutoGameHeaderContent.state(state.phase(), actions.autoGameActive(), copy);
        var control = autoGame.visible() ? new GameTitleBar.AutoPlay(autoGame.label(),
                autoGame.startDescription(), autoGame.stopDescription(), autoGame.checked(), true, titleColor,
                ignored -> { if (actions.autoGame() != null) actions.autoGame().run(); }) : null;
        header.addView(new GameTitleBar(activity, menuButton, title, control), views.matchWrap());

        LinearLayout details = new LinearLayout(activity);
        details.setOrientation(LinearLayout.HORIZONTAL);
        details.setGravity(Gravity.CENTER_VERTICAL);
        details.setTag("game-header-details");
        LinearLayout status = new LinearLayout(activity);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(views.dp(GameTitleBar.TOUCH_TARGET_DP + 8), 0, 0, 0);
        TextView progress = views.text(copy.progress(state), GameHeaderTypography.PROGRESS_SP, Color.rgb(229, 239, 230), true);
        TextView summary = views.text(copy.bidSummary(state), GameHeaderTypography.BID_SUMMARY_SP, Color.rgb(194, 216, 199), false);
        summary.setSingleLine(false);
        status.addView(progress, views.matchWrap());
        status.addView(summary, views.matchWrap());
        details.addView(status, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams trumpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trumpParams.leftMargin = views.dp(8);
        details.addView(new TrumpIndicatorView(activity, state, copy, currentPlayerHighlighted), trumpParams);
        header.addView(details, views.matchWrap());

        LinearLayout.LayoutParams params = views.matchWrap();
        params.bottomMargin = views.dp(4);
        page.addView(header, params);
    }
    private void showMenu(View anchor, WhistUiText copy, MenuActions actions) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        for (GameMenuContent.Item entry : GameMenuContent.items(copy, actions)) {
            menu.getMenu().add(entry.label()).setOnMenuItemClickListener(item -> {
                entry.action().run();
                return true;
            });
        }
        Runnable dismissed = actions.opened().get();
        menu.setOnDismissListener(ignored -> dismissed.run());
        menu.show();
    }
}

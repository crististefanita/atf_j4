package ro.whistlab.app.screens;

import static ro.whistlab.app.screens.ScreenViews.MUTED;
import static ro.whistlab.app.screens.ScreenViews.ACCENT;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.ScoreHistoryView;
import ro.whistlab.app.score.ScoreTableView;
import ro.whistlab.app.score.layout.HistoryDialogWindow;
import ro.whistlab.app.score.layout.HistoryContentScroll;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Score pages and dialogs consume immutable observations and explicit actions. */
public final class ScoreScreens {
    private final ScreenViews views;
    private final Activity activity;
    public ScoreScreens(ScreenViews views) { this.views = views; this.activity = views.activity(); }
    public record Actions(Runnable next, Runnable again, Runnable history, Runnable home) { }
    public void score(WhistUiText copy, GameSession.Snapshot state, ScoreHistory history, Actions actions) {
        views.applyStandardWindowChrome();
        LinearLayout page = views.page(copy.romanian() ? "MÂNA " + state.handNumber()
                : "HAND " + state.handNumber(), copy.scoreSheet());
        views.addText(page,copy.roundStatus(state),13,MUTED,false,0,4);
        views.addText(page, state.finalHand() ? copy.gameComplete() : copy.handComplete(),
                16, MUTED, false, 0, 12);
        if(state.finalHand())views.addText(page,copy.winner(state),18,ACCENT,true,0,12);
        LinearLayout.LayoutParams scoreTableParams = views.matchWrap();
        scoreTableParams.setMargins(0, 0, 0, views.dp(8));
        page.addView(new ScoreTableView(activity, state, copy, history), scoreTableParams);
        views.addQuietButton(page, copy.scoringDetails(), () -> new AlertDialog.Builder(activity)
                .setTitle(copy.scoringDetails()).setMessage(copy.scoreRule(state)
                        + (state.streakBonuses() ? "\n\n" + copy.streakSummary(state) : ""))
                .setPositiveButton(copy.close(), null).show());

        if (!state.finalHand()) {
            views.addButton(page, copy.nextHand(), "btn_next_hand", true, () -> {
                actions.next().run();
            }, true);
        } else {
            views.addButton(page, copy.playAgain(), "btn_play_again", true, () -> {
                actions.again().run();
            }, true);
        }
        views.addQuietButton(page, copy.scoreHistory(), actions.history());
        views.addQuietButton(page, copy.mainMenu(), actions.home());
    }
    public void live(WhistUiText copy, GameSession.Snapshot state, ScoreHistory history, Runnable dismissed, Runnable showHistory) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(views.dp(8), views.dp(2), views.dp(8), 0);
        TextView summary = views.text(copy.bidSummary(state), 12, MUTED, false);
        summary.setPadding(views.dp(2), views.dp(2), views.dp(2), views.dp(8));
        content.addView(summary, views.matchWrap());
        if (state.players().get(0).bid() == null) {
            TextView pendingBid = views.text(copy.bidNotPlaced(), 12, MUTED, true);
            pendingBid.setPadding(views.dp(2), 0, views.dp(2), views.dp(8));
            content.addView(pendingBid, views.matchWrap());
        }
        ScrollView scoreScroll = new ScrollView(activity);
        scoreScroll.setFillViewport(false);
        scoreScroll.addView(new ScoreTableView(activity, state, copy, history),
                new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scoreScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(copy.currentScores(state.handNumber()))
                .setView(content)
                .setPositiveButton(copy.close(), null);
        if (history.completedHands() > 0) builder.setNeutralButton(copy.scoreHistory(), null);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(ignored -> {
            dismissed.run();
        });
        dialog.show();
        if (history.completedHands() > 0) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> showHistory.run());
        }
    }
    public void history(WhistUiText copy, GameSession.Snapshot state, ScoreHistory history, Runnable dismissed) {
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(copy.scoreHistory())
                .setView(new HistoryContentScroll(activity, new ScoreHistoryView(activity, state, history, copy)))
                .setPositiveButton(copy.close(), null).create();
        HistoryDialogWindow.show(activity, dialog, dismissed);
    }
}

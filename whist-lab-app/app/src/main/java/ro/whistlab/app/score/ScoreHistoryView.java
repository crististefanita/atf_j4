package ro.whistlab.app.score;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** All players share a chronological table; the header stays above the vertical viewport. */
public final class ScoreHistoryView extends HorizontalScrollView {
    public ScoreHistoryView(Context context, GameSession.Snapshot state, ScoreHistory history, WhistUiText copy) {
        super(context);
        ScoreHistoryTable model = ScoreHistoryTable.forMatch(state, history, copy);
        ScoreHistoryGridMetrics grid = ScoreHistoryGridMetrics.forDensity(
                model.players().size(), getResources().getDisplayMetrics().density);
        setTag("score-history-table");
        setFillViewport(true);
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(grid.outerPaddingPx(), dp(6), grid.outerPaddingPx(), dp(4));
        addView(table, new LayoutParams(grid.tableWidthPx(), -2));
        LinearLayout names = line(0xffdfeada);
        text(names, copy.historyHand(), grid.handWidthPx(), true, true);
        for (String name : model.players()) {
            separator(names, grid);
            text(names, name, grid.playerWidthPx(), true, true);
        }
        table.addView(names);
        LinearLayout headings = line(0xffeef3ea);
        text(headings, copy.cardsHeading(), grid.handWidthPx(), false, true);
        for (String ignored : model.players()) {
            separator(headings, grid);
            text(headings, copy.scoreColumnBid(), grid.bidWidthPx(), false, true);
            text(headings, copy.historyPoints(), grid.pointsWidthPx(), false, true);
            text(headings, copy.scoreColumnTotal(), grid.totalWidthPx(), false, true);
        }
        table.addView(headings);
        ScrollView scroll = new ScrollView(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (var row : model.rows()) {
            int fill = row.status() == ScoreHistoryTable.Status.CURRENT ? 0xffe5eedc
                    : row.hand() % 2 == 0 ? 0xfff1f5ee : 0xfffffef8;
            LinearLayout line = line(fill);
            text(line, row.cardLabel(), grid.handWidthPx(), false, true);
            for (var cell : row.cells()) {
                separator(line, grid);
                LinearLayout group = new LinearLayout(context);
                group.setGravity(Gravity.CENTER_VERTICAL);
                group.setFocusable(true);
                group.setContentDescription(cell.description());
                text(group, cell.bid(), grid.bidWidthPx(), false, false);
                text(group, cell.points(), grid.pointsWidthPx(), false, false);
                text(group, cell.total(), grid.totalWidthPx(), true, false);
                line.addView(group, new LinearLayout.LayoutParams(grid.playerWidthPx(), -2));
            }
            rows.addView(line);
        }
        scroll.addView(rows);
        int viewport = Math.min(dp(380), Math.max(dp(80), getResources().getDisplayMetrics().heightPixels - dp(260)));
        table.addView(scroll, new LinearLayout.LayoutParams(-1, viewport));
    }

    private void separator(LinearLayout row, ScoreHistoryGridMetrics grid) {
        View divider = new View(getContext());
        divider.setBackgroundColor(0xff71846c);
        divider.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(divider, new LinearLayout.LayoutParams(grid.separatorWidthPx(), -1));
    }

    private LinearLayout line(int fill) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(fill);
        return row;
    }
    private void text(LinearLayout row, String value, int width, boolean bold, boolean accessible) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(12);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(40));
        view.setPadding(dp(3), dp(6), dp(3), dp(6));
        view.setTextColor(value.startsWith("-") || value.startsWith("−") ? 0xff97462e : 0xff29362e);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (!accessible) view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(view, new LinearLayout.LayoutParams(width, -2));
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

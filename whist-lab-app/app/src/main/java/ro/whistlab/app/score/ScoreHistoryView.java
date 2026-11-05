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
        ScoreHistoryGridMetrics grid = measuredGrid(model, copy);
        setTag("score-history-table");
        setFillViewport(false);
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(grid.outerPaddingPx(), dp(6), grid.outerPaddingPx(), dp(4));
        addView(table, new LayoutParams(grid.tableWidthPx(), -2));
        LinearLayout names = line(0xffdfeada);
        text(names, copy.cardsHeading(), grid.handWidthPx(), true, true).setTextSize(11);
        for (String name : model.players()) {
            separator(names, grid);
            text(names, name, grid.playerWidthPx(), true, true);
        }
        table.addView(names);
        LinearLayout headings = line(0xffeef3ea);
        text(headings, "", grid.handWidthPx(), false, false);
        for (String ignored : model.players()) {
            separator(headings, grid);
            text(headings, copy.historyBidHeading(), grid.bidWidthPx(), false, true);
            text(headings, copy.historyPointsHeading(), grid.pointsWidthPx(), false, true);
            text(headings, copy.historyTotalHeading(), grid.totalWidthPx(), false, true);
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
                bid(group, cell, grid.bidWidthPx());
                text(group, cell.points(), grid.pointsWidthPx(), false, false);
                text(group, cell.total(), grid.totalWidthPx(), true, false);
                line.addView(group, new LinearLayout.LayoutParams(grid.playerWidthPx(), -2));
            }
            rows.addView(line);
        }
        scroll.addView(rows);
        int viewport = Math.min(dp(380), Math.max(dp(80), getResources().getDisplayMetrics().heightPixels - dp(260)));
        table.addView(scroll, new LinearLayout.LayoutParams(-1, viewport));
        TextView legend = new TextView(context);
        legend.setText(copy.historyLegend());
        legend.setTextSize(11);
        legend.setTextColor(0xff52634f);
        legend.setPadding(dp(1), dp(8), dp(1), dp(4));
        legend.setSingleLine(false);
        table.addView(legend, new LinearLayout.LayoutParams(-1, -2));
    }

    private ScoreHistoryGridMetrics measuredGrid(ScoreHistoryTable model, WhistUiText copy) {
        TextView probe = new TextView(getContext());
        probe.setTextSize(12);
        probe.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        android.graphics.Paint paint = probe.getPaint();
        int padding = 2 * dp(1);
        TextView headingProbe = new TextView(getContext());
        headingProbe.setTextSize(11);
        headingProbe.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        int hand = width(headingProbe.getPaint(), copy.cardsHeading()) + padding;
        int bid = width(paint, copy.historyBidHeading()) + padding;
        int points = width(paint, copy.historyPointsHeading()) + padding;
        int total = width(paint, copy.historyTotalHeading()) + padding;
        int name = 0;
        for (String value : model.players()) name = Math.max(name, width(paint, value) + padding);
        for (var row : model.rows()) {
            hand = Math.max(hand, width(paint, row.cardLabel()) + padding);
            for (var cell : row.cells()) {
                bid = Math.max(bid, width(paint, cell.bid()) + 4 * dp(1));
                points = Math.max(points, width(paint, cell.points()) + padding);
                total = Math.max(total, width(paint, cell.total()) + padding);
            }
        }
        return ScoreHistoryGridMetrics.forDensity(model.players().size(), getResources().getDisplayMetrics().density)
                .fitContent(hand, bid, points, total, name);
    }

    private static int width(android.graphics.Paint paint, String text) {
        return (int) Math.ceil(paint.measureText(text));
    }

    private void bid(LinearLayout row, ScoreHistoryTable.Cell cell, int width) {
        boolean missed = cell.outcome() == ScoreHistory.Outcome.MISSED;
        TextView view = new HistoryBidView(getContext(), missed);
        style(view, cell.bid(), false, false);
        if (missed) view.setTextColor(0xff97462e);
        row.addView(view, new LinearLayout.LayoutParams(width, -2));
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
    private TextView text(LinearLayout row, String value, int width, boolean bold, boolean accessible) {
        TextView view = new TextView(getContext());
        style(view, value, bold, accessible);
        row.addView(view, new LinearLayout.LayoutParams(width, -2));
        return view;
    }
    private void style(TextView view, String value, boolean bold, boolean accessible) {
        view.setText(value);
        view.setTextSize(12);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(40));
        view.setPadding(dp(1), dp(6), dp(1), dp(6));
        view.setTextColor(value.startsWith("-") || value.startsWith("−") ? 0xff97462e : 0xff29362e);
        view.setSingleLine(true);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (!accessible) view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

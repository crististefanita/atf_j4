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
import ro.whistlab.app.score.layout.HistoryViewport;
import ro.whistlab.app.score.award.SeriesPointsView;

/** All players share a chronological table; the header stays above the vertical viewport. */
public final class ScoreHistoryView extends LinearLayout {
    private final HorizontalScrollView columns;
    private final LinearLayout table;
    private final ScrollView body;

    public ScoreHistoryView(Context context, GameSession.Snapshot state, ScoreHistory history, WhistUiText copy) {
        super(context);
        ScoreHistoryTable model = ScoreHistoryTable.forMatch(state, history, copy);
        ScoreHistoryGridMetrics grid = measuredGrid(model, copy);
        setTag("score-history-table");
        setOrientation(VERTICAL);
        columns = new HorizontalScrollView(context);
        columns.setFillViewport(false);
        addView(columns, new LayoutParams(-1, -2));
        table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(grid.outerPaddingPx(), dp(6), grid.outerPaddingPx(), dp(4));
        columns.addView(table, new HorizontalScrollView.LayoutParams(grid.tableWidthPx(), -2));
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
        }
        table.addView(headings);
        body = new ScrollView(context);
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
                TextView points = new SeriesPointsView(context, cell.seriesPoints());
                style(points, cell.displayPoints(), false, false);
                group.addView(points, new LinearLayout.LayoutParams(grid.pointsWidthPx(), -2));
                line.addView(group, new LinearLayout.LayoutParams(grid.playerWidthPx(), -2));
            }
            rows.addView(line);
        }
        body.addView(rows);
        table.addView(body, new LayoutParams(-1, dp(380)));
        TextView legend = new TextView(context);
        legend.setText(copy.historyLegend());
        legend.setTextSize(11);
        legend.setTextColor(0xff52634f);
        int captionInset = grid.outerPaddingPx() + dp(1);
        legend.setPadding(captionInset, dp(8), captionInset, dp(4));
        legend.setSingleLine(false);
        addView(legend, new LayoutParams(-1, -2));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int preferred = dp(380);
        bodyHeight(preferred);
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED) return;
        int fitted = HistoryViewport.bodyHeight(preferred, getMeasuredHeight(), MeasureSpec.getSize(heightSpec));
        if (fitted != preferred) bodyHeight(fitted);
        super.onMeasure(widthSpec, heightSpec);
    }

    private void bodyHeight(int height) {
        body.getLayoutParams().height = height;
        body.forceLayout();
        table.forceLayout();
        columns.forceLayout();
    }

    private ScoreHistoryGridMetrics measuredGrid(ScoreHistoryTable model, WhistUiText copy) {
        TextView probe = new TextView(getContext());
        probe.setTextSize(12);
        probe.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        android.graphics.Paint paint = probe.getPaint();
        TextView headingProbe = new TextView(getContext());
        headingProbe.setTextSize(11);
        headingProbe.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        var measured = ScoreHistoryMeasure.measure(model,
                new ScoreHistoryMeasure.Labels(copy.cardsHeading(), copy.historyBidHeading(),
                        copy.historyPointsHeading()),
                text -> width(paint, text), text -> width(headingProbe.getPaint(), text), dp(1));
        return measured.fit(ScoreHistoryGridMetrics.forDensity(model.players().size(),
                getResources().getDisplayMetrics().density));
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

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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/** All players share a chronological table; the header stays above the vertical viewport. */
public final class ScoreHistoryView extends LinearLayout {
    private final HorizontalScrollView columns;
    private final LinearLayout table;
    private final ScrollView body;
    private final Map<View, ToIntFunction<ScoreHistoryGridMetrics>> widths = new IdentityHashMap<>();
    private final ScoreHistoryGridMetrics minimumGrid;
    private ScoreHistoryGridMetrics currentGrid;
    private boolean wholeScroll;

    public ScoreHistoryView(Context context, GameSession.Snapshot state, ScoreHistory history, WhistUiText copy) {
        super(context);
        ScoreHistoryTable model = ScoreHistoryTable.forMatch(state, history, copy);
        ScoreHistoryGridMetrics grid = measuredGrid(model, copy);
        minimumGrid = currentGrid = grid;
        setTag("score-history-table");
        setOrientation(VERTICAL);
        columns = new HorizontalScrollView(context);
        columns.setFillViewport(true);
        addView(columns, new LayoutParams(-1, -2));
        table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(grid.outerPaddingPx(), dp(6), grid.outerPaddingPx(), dp(4));
        columns.addView(table, new HorizontalScrollView.LayoutParams(grid.tableWidthPx(), -2));
        LinearLayout names = line(0xffdfeada);
        text(names, copy.cardsHeading(), ScoreHistoryGridMetrics::handWidthPx, true, true).setTextSize(HistoryTypography.CAPTION_SP);
        for (String name : model.players()) {
            separator(names, grid);
            text(names, name, ScoreHistoryGridMetrics::playerWidthPx, true, true);
        }
        table.addView(names);
        LinearLayout headings = line(0xffeef3ea);
        text(headings, "", ScoreHistoryGridMetrics::handWidthPx, false, false);
        for (String ignored : model.players()) {
            separator(headings, grid);
            text(headings, copy.historyBidHeading(), ScoreHistoryGridMetrics::bidWidthPx, false, true);
            text(headings, copy.historyPointsHeading(), ScoreHistoryGridMetrics::pointsWidthPx, false, true);
        }
        table.addView(headings);
        body = new ScrollView(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (var row : model.rows()) {
            int fill = row.status() == ScoreHistoryTable.Status.CURRENT ? 0xffe5eedc
                    : row.hand() % 2 == 0 ? 0xfff1f5ee : 0xfffffef8;
            LinearLayout line = line(fill);
            text(line, row.cardLabel(), ScoreHistoryGridMetrics::handWidthPx, false, true);
            for (var cell : row.cells()) {
                separator(line, grid);
                LinearLayout group = new LinearLayout(context);
                group.setGravity(Gravity.CENTER_VERTICAL);
                group.setFocusable(true);
                group.setContentDescription(cell.description());
                bid(group, cell);
                TextView points = new SeriesPointsView(context, cell.seriesPoints());
                style(points, cell.displayPoints(), false, false);
                group.addView(points, size(points, ScoreHistoryGridMetrics::pointsWidthPx, -2));
                line.addView(group, size(group, ScoreHistoryGridMetrics::playerWidthPx, -2));
            }
            rows.addView(line);
        }
        body.addView(rows);
        table.addView(body, new LayoutParams(-1, dp(380)));
        TextView legend = new TextView(context);
        String awards = copy.zeroGameAwards(state);
        legend.setText(copy.historyLegend() + (awards.isEmpty() ? "" : "\n" + awards));
        legend.setTextSize(HistoryTypography.LEGEND_SP);
        legend.setTextColor(0xff52634f);
        int captionInset = grid.outerPaddingPx() + dp(1);
        legend.setPadding(captionInset, dp(8), captionInset, dp(4));
        legend.setSingleLine(false);
        addView(legend, new LayoutParams(-1, -2));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        fitWidth(widthSpec);
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED) {
            bodyHeight(LayoutParams.WRAP_CONTENT);
            super.onMeasure(widthSpec, heightSpec);
            return;
        }
        int preferred = dp(380);
        bodyHeight(preferred);
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        android.view.ViewGroup rows = (android.view.ViewGroup) body.getChildAt(0);
        int minimumRow = rows.getChildCount() == 0 ? dp(40) : Math.max(1, rows.getChildAt(0).getMeasuredHeight());
        wholeScroll = HistoryViewport.needsWholeScroll(preferred, getMeasuredHeight(), MeasureSpec.getSize(heightSpec), minimumRow);
        int fitted = HistoryViewport.bodyHeight(preferred, getMeasuredHeight(), MeasureSpec.getSize(heightSpec));
        if (fitted != preferred) bodyHeight(fitted);
        super.onMeasure(widthSpec, heightSpec);
    }

    public boolean needsWholeScroll() { return wholeScroll; }

    private void fitWidth(int widthSpec) {
        int available = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED ? 0
                : Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        var fitted = minimumGrid.fitViewport(available);
        if (fitted.equals(currentGrid)) return;
        currentGrid = fitted;
        for (var entry : widths.entrySet()) {
            var params = entry.getKey().getLayoutParams();
            params.width = entry.getValue().applyAsInt(fitted);
            entry.getKey().setLayoutParams(params);
        }
        var params = table.getLayoutParams();
        params.width = fitted.tableWidthPx();
        table.setLayoutParams(params);
    }

    private void bodyHeight(int height) {
        body.getLayoutParams().height = height;
        body.forceLayout();
        table.forceLayout();
        columns.forceLayout();
    }

    private ScoreHistoryGridMetrics measuredGrid(ScoreHistoryTable model, WhistUiText copy) {
        TextView probe = new TextView(getContext());
        probe.setTextSize(HistoryTypography.VALUE_SP);
        probe.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        android.graphics.Paint paint = probe.getPaint();
        TextView headingProbe = new TextView(getContext());
        headingProbe.setTextSize(HistoryTypography.CAPTION_SP);
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

    private void bid(LinearLayout row, ScoreHistoryTable.Cell cell) {
        boolean missed = cell.outcome() == ScoreHistory.Outcome.MISSED;
        TextView view = new HistoryBidView(getContext(), missed);
        style(view, cell.bid(), false, false);
        if (missed) view.setTextColor(0xff97462e);
        row.addView(view, size(view, ScoreHistoryGridMetrics::bidWidthPx, -2));
    }

    private void separator(LinearLayout row, ScoreHistoryGridMetrics grid) {
        View divider = new View(getContext());
        divider.setBackgroundColor(0xff71846c);
        divider.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(divider, size(divider, ScoreHistoryGridMetrics::separatorWidthPx, -1));
    }

    private LinearLayout line(int fill) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(fill);
        return row;
    }
    private TextView text(LinearLayout row, String value, ToIntFunction<ScoreHistoryGridMetrics> width, boolean bold, boolean accessible) {
        TextView view = new TextView(getContext());
        style(view, value, bold, accessible);
        row.addView(view, size(view, width, -2));
        return view;
    }
    private void style(TextView view, String value, boolean bold, boolean accessible) {
        view.setText(value);
        view.setTextSize(HistoryTypography.VALUE_SP);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(40));
        view.setPadding(dp(1), dp(6), dp(1), dp(6));
        view.setTextColor(value.startsWith("-") || value.startsWith("−") ? 0xff97462e : 0xff29362e);
        view.setSingleLine(true);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (!accessible) view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private LinearLayout.LayoutParams size(View view, ToIntFunction<ScoreHistoryGridMetrics> width, int height) {
        widths.put(view, width);
        return new LinearLayout.LayoutParams(width.applyAsInt(currentGrid), height);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

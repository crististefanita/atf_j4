package ro.cardtable.ui.score;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.function.Consumer;
import ro.cardtable.ui.table.ScoreGridStyle;

/** Pinned labels, synchronized horizontal panes and a vertical body with a short-window fallback. */
public class ScoreHistoryView extends ScrollView {
    private final ScrollView rows;
    private final HorizontalScrollView headings, scores;
    private final LinearLayout header, scoreHeadings, scoreRows;
    private final TextView corner, legend;
    private final List<TextView> names = new ArrayList<>();
    private final List<List<TextView>> columns = new ArrayList<>();
    private final List<RowViews> rowViews = new ArrayList<>();
    private final int separator, labelMinimum, nameMinimum;
    private final List<Integer> columnMinima;
    private int fittedWidth = -1, naturalRows, firstRow;

    public ScoreHistoryView(Context context, HistoryContent data, Consumer<String> openDetails) {
        super(context); Objects.requireNonNull(openDetails);
        separator = dp(1);
        for (int i = 0; i < data.columns().size(); i++) columns.add(new ArrayList<>());
        var content = column(); addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        header = new LinearLayout(context); header.setBackgroundColor(ScoreGridStyle.HEADER);
        corner = text(data.corner(), true, Gravity.START); heading(corner);
        header.addView(corner, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
        headings = horizontal(); scoreHeadings = new LinearLayout(context);
        for (var player : data.players()) {
            var name = text(player.label(), true, Gravity.CENTER_HORIZONTAL); heading(name); names.add(name);
            var group = column(); group.addView(name);
            group.addView(cells(data.columns().stream().map(ScoreTableContent.Cell::new).collect(Collectors.toList()), true));
            addGroup(scoreHeadings, group, ScoreGridStyle.HEADER);
        }
        headings.addView(scoreHeadings, new HorizontalScrollView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        header.addView(headings, remainingWidth(LayoutParams.MATCH_PARENT));
        content.addView(header, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0));
        rows = new ScrollView(context);
        var body = new LinearLayout(context);
        rows.addView(body, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        var labels = column(); body.addView(labels, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        scores = horizontal(); scoreRows = column(); int index = 0;
        for (var row : data.rows()) {
            int fill = index++ % 2 == 0 ? ScoreGridStyle.PAPER : ScoreGridStyle.STRIPE;
            var label = text(row.label(), false, Gravity.START); label.setBackgroundColor(fill);
            label.setTag(row.id()); label.setContentDescription(row.description());
            label.setPaintFlags(label.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            label.setOnClickListener(ignored -> openDetails.accept(row.id())); label.setFocusable(true);
            if (Build.VERSION.SDK_INT >= 26) label.setTooltipText(data.detailsLabel());
            labels.addView(label);
            var numbers = new LinearLayout(context); numbers.setBackgroundColor(fill);
            for (var values : row.values()) addGroup(numbers, cells(values, false), fill);
            scoreRows.addView(numbers); rowViews.add(new RowViews(label, numbers));
        }
        scores.addView(scoreRows, new HorizontalScrollView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        body.addView(scores, remainingWidth(LayoutParams.WRAP_CONTENT));
        content.addView(rows, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0));
        legend = text(data.legend(), false, Gravity.START);
        content.addView(legend, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        var sync = new HistoryScrollSync();
        var headingPort = pane(headings); var scorePort = pane(scores);
        headings.setOnScrollChangeListener((view, x, y, oldX, oldY) -> sync.follow(scorePort, x));
        scores.setOnScrollChangeListener((view, x, y, oldX, oldY) -> sync.follow(headingPort, x));
        headings.setScrollbarFadingEnabled(false);
        int labelWidth = desiredWidth(corner);
        for (var row : rowViews) labelWidth = Math.max(labelWidth, desiredWidth(row.label()));
        labelMinimum = labelWidth; nameMinimum = maxWidth(names);
        columnMinima = columns.stream().map(ScoreHistoryView::maxWidth).collect(Collectors.toList());
    }
    public final HorizontalScrollView headingsView() { return headings; }
    public final ScrollView bodyView() { return rows; }
    public final HorizontalScrollView scoresView() { return scores; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = Math.max(2, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        if (width != fittedWidth) fit(width);
        measureNatural(legend, width);
        int available = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE
                : MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom();
        int height = HistoryGridMetrics.bodyHeight(available, header.getLayoutParams().height, legend.getMeasuredHeight(), naturalRows, firstRow);
        if (rows.getLayoutParams().height != height) {
            rows.getLayoutParams().height = height; rows.forceLayout(); ((View) rows.getParent()).forceLayout();
        }
        super.onMeasure(widthSpec, heightSpec);
    }
    private void fit(int width) {
        fittedWidth = width;
        var grid = HistoryGridMetrics.fit(width, labelMinimum, nameMinimum, columnMinima, separator, names.size());
        size(corner, grid.label(), LayoutParams.MATCH_PARENT);
        for (var name : names) size(name, grid.playerWidth() - separator, LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < columns.size(); i++) for (var cell : columns.get(i)) size(cell, grid.columns().get(i), LayoutParams.MATCH_PARENT);
        measureNatural(corner, grid.label()); measureNatural(scoreHeadings, grid.scoresWidth());
        size(header, LayoutParams.MATCH_PARENT, Math.max(corner.getMeasuredHeight(), scoreHeadings.getMeasuredHeight()));
        size(scoreHeadings, grid.scoresWidth(), LayoutParams.MATCH_PARENT);
        naturalRows = 0;
        for (var row : rowViews) {
            measureNatural(row.label(), grid.label()); measureNatural(row.numbers(), grid.scoresWidth());
            int height = Math.max(row.label().getMeasuredHeight(), row.numbers().getMeasuredHeight());
            size(row.label(), grid.label(), height); size(row.numbers(), grid.scoresWidth(), height); naturalRows += height;
        }
        firstRow = rowViews.isEmpty() ? 0 : rowViews.get(0).label().getLayoutParams().height;
        size(scoreRows, grid.scoresWidth(), LayoutParams.WRAP_CONTENT);
    }
    private LinearLayout cells(List<ScoreTableContent.Cell> values, boolean captions) {
        var group = new LinearLayout(getContext());
        for (int i = 0; i < values.size(); i++) {
            var cell = values.get(i); var view = text(cell.text(), cell.emphasized(), Gravity.END);
            view.setSingleLine(true); view.setContentDescription(cell.description());
            if (cell.underlined()) view.setPaintFlags(view.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            if (captions) heading(view);
            columns.get(i).add(view); group.addView(view);
        }
        if (!captions) {
            group.setFocusable(true);
            group.setContentDescription(String.join(" · ", values.stream().map(ScoreTableContent.Cell::description).collect(Collectors.toList())));
            for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return group;
    }
    private void addGroup(LinearLayout line, LinearLayout group, int fill) {
        var border = new View(getContext()); border.setBackgroundColor(ScoreGridStyle.RULE);
        border.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        line.addView(border, new LinearLayout.LayoutParams(separator, LayoutParams.MATCH_PARENT));
        group.setBackgroundColor(fill); line.addView(group, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }
    private TextView text(String value, boolean bold, int gravity) {
        var view = new TextView(getContext()); view.setText(value); view.setTextSize(13); view.setTextColor(ScoreGridStyle.INK);
        view.setGravity(gravity | Gravity.CENTER_VERTICAL); view.setPadding(dp(6), dp(5), dp(6), dp(5));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private LinearLayout column() { var layout = new LinearLayout(getContext()); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private HorizontalScrollView horizontal() { var view = new HorizontalScrollView(getContext()); view.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS); return view; }
    private static HistoryScrollSync.Pane pane(HorizontalScrollView view) {
        return new HistoryScrollSync.Pane() {
            public int position() { return view.getScrollX(); }
            public void scrollTo(int x) { view.scrollTo(x, 0); }
        };
    }
    private static void heading(View view) { if (Build.VERSION.SDK_INT >= 28) view.setAccessibilityHeading(true); }
    private static LinearLayout.LayoutParams remainingWidth(int height) { return new LinearLayout.LayoutParams(0, height, 1f); }
    private static int maxWidth(List<TextView> views) { return views.stream().mapToInt(ScoreHistoryView::desiredWidth).max().orElse(1); }
    private static int desiredWidth(TextView view) { return (int) Math.ceil(Layout.getDesiredWidth(view.getText(), view.getPaint())) + view.getCompoundPaddingLeft() + view.getCompoundPaddingRight(); }
    private static void measureNatural(View view, int width) { view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)); }
    private static void size(View view, int width, int height) { var params = view.getLayoutParams(); params.width = width; params.height = height; view.forceLayout(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private record RowViews(TextView label, LinearLayout numbers) { }
}

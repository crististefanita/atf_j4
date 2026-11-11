package ro.cardtable.ui.achievements;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import ro.cardtable.ui.screens.ScreenViews;
import ro.cardtable.ui.theme.TablePalette;

/** Themed achievement browser. Callers supply localized copy, ordered rows and navigation. */
public final class AchievementScreen {
    public record Action(String label, Runnable run) {
        public Action { Objects.requireNonNull(run); }
    }
    public record Copy(String title, String summary, String empty, String path, String close) { }
    public record Section(String title, String text) { }
    public record Detail(String progress, List<Section> sections) {
        public Detail { sections = List.copyOf(sections); }
    }
    public record Row(long stableId, String key, String title, String subtitle, boolean earned, Supplier<Detail> detail) {
        public static Row heading(long stableId, String title) {
            return new Row(stableId, null, title, "", false, null);
        }
        public boolean heading() { return key == null; }
    }

    public static final class State {
        private int position;
        private String selected;

        public void resetPosition() { position = 0; }

        public void save(Bundle bundle) {
            bundle.putInt("achievementPosition", position);
            bundle.putString("achievementSelected", selected);
        }

        public void restore(Bundle bundle) {
            position = Math.max(0, bundle.getInt("achievementPosition"));
            selected = bundle.getString("achievementSelected");
        }
    }

    private final ScreenViews views;
    private final State state;
    private final String close;
    private final ListView list;
    private final TextView empty;
    private final TextView filterSummary;
    private final Rows rows = new Rows();
    private AlertDialog detailDialog;

    public AchievementScreen(ScreenViews views, State state, Copy copy, int earned, int total,
            List<Action> actions, Action back) {
        this.views = views;
        this.state = state;
        this.close = copy.close();
        views.applyTableWindowChrome();
        var page = views.gamePage();
        var heading = views.text(copy.title(), 22, Color.WHITE, true);
        if (Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
        var headingLayout = views.matchWrap();
        headingLayout.setMargins(0, views.dp(8), 0, views.dp(4));
        page.addView(heading, headingLayout);
        for (var action : actions) tableAction(page, action);
        views.addText(page, copy.summary(), 15, TablePalette.ACTIVE_SEAT_FILL, true, 3, 2);

        var progress = new ProgressBar(views.activity(), null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(total);
        progress.setProgress(earned);
        progress.setProgressTintList(ColorStateList.valueOf(TablePalette.ACTIVE_SEAT_STROKE));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(TablePalette.IDLE_SEAT_STROKE));
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        page.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, views.dp(4)));

        filterSummary = views.text("", 13, TablePalette.IDLE_SEAT_TEXT, true);
        page.addView(filterSummary, views.matchWrap());
        list = new ListView(views.activity());
        list.setDivider(new ColorDrawable(TablePalette.TABLE_FELT));
        list.setDividerHeight(views.dp(5));
        list.setContentDescription(copy.path());
        list.setAdapter(rows);
        list.setOnItemClickListener((parent, view, position, id) -> showDetail(rows.getItem(position)));
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int status) { }
            @Override public void onScroll(AbsListView view, int first, int visible, int total) {
                if (visible > 0) state.position = first;
            }
        });
        empty = views.text(copy.empty(), 15, Color.WHITE, false);
        empty.setGravity(Gravity.CENTER);
        var body = new FrameLayout(views.activity());
        body.addView(list, new FrameLayout.LayoutParams(-1, -1));
        body.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        page.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        tableAction(page, back);
    }

    public void update(List<Row> items, String summary) {
        int position = Math.min(state.position, Math.max(0, items.size() - 1));
        rows.items = checkedRows(items);
        rows.notifyDataSetChanged();
        list.setSelection(position);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        filterSummary.setText(summary);
        filterSummary.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
    }

    static List<Row> checkedRows(List<Row> items) {
        var ids = new HashSet<Long>();
        var keys = new HashSet<String>();
        for (var row : items) {
            if (row.stableId() == AdapterView.INVALID_ROW_ID || !ids.add(row.stableId())) {
                throw new IllegalArgumentException("Duplicate or invalid achievement row ID: " + row.stableId());
            }
            if (!row.heading() && (!keys.add(row.key()) || row.detail() == null)) {
                throw new IllegalArgumentException("Duplicate or incomplete achievement row: " + row.key());
            }
        }
        return List.copyOf(items);
    }

    public void restoreDetail() {
        if (state.selected == null) return;
        for (var row : rows.items) {
            if (state.selected.equals(row.key())) {
                showDetail(row);
                return;
            }
        }
        state.selected = null;
    }

    private void tableAction(LinearLayout page, Action action) {
        var button = new Button(views.activity());
        button.setText(action.label());
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setMinHeight(views.dp(48));
        button.setPadding(views.dp(10), views.dp(5), views.dp(10), views.dp(5));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x28ffffff),
                views.roundedBackground(TablePalette.IDLE_SEAT_FILL, TablePalette.IDLE_SEAT_STROKE, 12), null));
        button.setOnClickListener(view -> action.run().run());
        var layout = views.matchWrap();
        layout.setMargins(0, views.dp(4), 0, views.dp(4));
        page.addView(button, layout);
    }

    private void showDetail(Row row) {
        if (row.heading() || detailDialog != null) return;
        state.selected = row.key();
        var detail = row.detail().get();
        var content = new LinearLayout(views.activity());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(views.dp(20), views.dp(4), views.dp(20), views.dp(12));
        views.addText(content, detail.progress(), 14, ScreenViews.ACCENT, true, 4, 6);
        for (var section : detail.sections()) {
            if (!section.title().isEmpty())
                views.addText(content, section.title(), 14, ScreenViews.PINE, true, 8, 2);
            views.addText(content, section.text(), 14, ScreenViews.INK, false, 0, 4);
        }
        var scroll = new ScrollView(views.activity());
        scroll.addView(content);
        detailDialog = new AlertDialog.Builder(views.activity()).setTitle(row.title()).setView(scroll)
                .setPositiveButton(close, null).create();
        detailDialog.setOnDismissListener(dialog -> {
            detailDialog = null;
            state.selected = null;
        });
        detailDialog.show();
    }

    private final class Rows extends BaseAdapter {
        private List<Row> items = List.of();
        @Override public int getCount() { return items.size(); }
        @Override public Row getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) {
            return valid(position) ? getItem(position).stableId() : AdapterView.INVALID_ROW_ID;
        }
        @Override public boolean hasStableIds() { return true; }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int position) { return valid(position) && !getItem(position).heading(); }
        private boolean valid(int position) { return position >= 0 && position < items.size(); }

        @Override public View getView(int position, View reusable, ViewGroup parent) {
            LinearLayout item;
            if (reusable instanceof LinearLayout existing) {
                item = existing;
            } else {
                item = new LinearLayout(views.activity());
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(views.dp(16), views.dp(8), views.dp(16), views.dp(8));
                item.addView(views.text("", 15, ScreenViews.PINE, true), views.matchWrap());
                item.addView(views.text("", 13, ScreenViews.INK, false), views.matchWrap());
            }
            var row = getItem(position);
            var title = (TextView) item.getChildAt(0);
            var detail = (TextView) item.getChildAt(1);
            item.setBackground(row.heading() ? null : views.roundedBackground(
                    row.earned() ? TablePalette.ACTIVE_SEAT_FILL : TablePalette.IDLE_SEAT_FILL,
                    row.earned() ? TablePalette.ACTIVE_SEAT_STROKE : TablePalette.IDLE_SEAT_STROKE, 12));
            title.setText(row.title());
            title.setTextColor(row.heading() ? TablePalette.ACTIVE_SEAT_FILL : row.earned() ? ScreenViews.PINE : Color.WHITE);
            if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(row.heading());
            detail.setText(row.subtitle());
            detail.setTextColor(row.earned() ? ScreenViews.PINE : TablePalette.IDLE_SEAT_TEXT);
            detail.setVisibility(row.heading() ? View.GONE : View.VISIBLE);
            return item;
        }
    }
}

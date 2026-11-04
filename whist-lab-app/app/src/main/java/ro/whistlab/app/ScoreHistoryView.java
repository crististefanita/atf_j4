package ro.whistlab.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import ro.whistlab.core.ScoreHistory;

/** One player's complete column, newest hand first, without horizontal scrolling. */
final class ScoreHistoryView extends LinearLayout {
    private final ScoreHistory history;
    private final WhistUiText copy;
    private final TextView opening;
    private final LinearLayout rows;
    private final ScrollView scroll;
    private int shownSeat = -1;

    ScoreHistoryView(Context context, ScoreHistory history, WhistUiText copy, Spinner players) {
        super(context);
        this.history = history;
        this.copy = copy;
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(4), dp(16), 0);
        addView(players, new LayoutParams(-1, -2));
        opening = label(false);
        opening.setGravity(Gravity.START);
        addView(opening, new LayoutParams(-1, -2));
        if (!history.closingTotals().isEmpty()) {
            addView(row(copy.historyHand(), copy.historyPoints(), copy.scoreColumnTotal(), true), new LayoutParams(-1, -2));
        }
        scroll = new ScrollView(context);
        rows = new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        scroll.addView(rows);
        addView(scroll, new LayoutParams(-1, -2));
        showPlayer(0);
        players.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { showPlayer(position); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void showPlayer(int seat) {
        if (seat == shownSeat) return;
        shownSeat = seat;
        ScoreHistoryColumn column = ScoreHistoryColumn.forPlayer(history, seat, copy);
        opening.setText(column.opening());
        opening.setVisibility(column.opening().isEmpty() ? GONE : VISIBLE);
        rows.removeAllViews();
        for (ScoreHistoryColumn.Row result : column.rows()) {
            LinearLayout line = row(result.hand(), result.points(), result.total(), false);
            if (rows.getChildCount() % 2 == 0) line.setBackgroundColor(0xfff1f5ef);
            line.setContentDescription(result.description());
            line.setFocusable(true);
            rows.addView(line);
        }
        scroll.scrollTo(0, 0);
    }

    private LinearLayout row(String hand, String points, String total, boolean header) {
        LinearLayout line = new LinearLayout(getContext());
        line.setGravity(Gravity.CENTER_VERTICAL);
        cell(line, hand, header, header, Gravity.START);
        cell(line, points, header, header, Gravity.END);
        cell(line, total, header, true, Gravity.END);
        return line;
    }

    private void cell(LinearLayout line, String value, boolean header, boolean bold, int gravity) {
        TextView text = label(bold);
        text.setGravity(Gravity.CENTER_VERTICAL | gravity);
        text.setText(value);
        if (!header) text.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        line.addView(text, new LayoutParams(0, -2, 1));
    }

    private TextView label(boolean bold) {
        TextView text = new TextView(getContext());
        text.setPadding(dp(8), dp(6), dp(8), dp(6));
        text.setMinHeight(dp(36));
        text.setTextColor(0xff29362e);
        text.setTextSize(14);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

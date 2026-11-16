package ro.whistlab.app.score;

import ro.whistlab.app.localization.WhistUiText;
import ro.cardtable.ui.table.ScoreGridStyle;
import static ro.cardtable.ui.table.ScoreGridStyle.*;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.core.session.ScoreHistory;
import ro.whistlab.core.session.GameSession;

/** Compact current-hand score table shared by the live view and hand-result screen. */
public final class ScoreTableView extends LinearLayout {
    private final ScoreHistory history;

    public ScoreTableView(Context context, GameSession.Snapshot state, WhistUiText copy, ScoreHistory history) {
        super(context);
        this.history = history;
        setOrientation(VERTICAL);
        setBackground(rounded(PAPER, RULE, 12));

        addView(row(copy, null, state, true),
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(HEADER_DP)));
        for (int index = 0; index < state.players().size(); index++) {
            GameSession.PlayerSnapshot player = state.players().get(index);
            addView(row(copy, player, state, false),
                    new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ROW_DP)));
            if (index + 1 < state.players().size()) {
                View divider = new View(context);
                divider.setBackgroundColor(RULE);
                addView(divider, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
    }

    private View row(WhistUiText copy, GameSession.PlayerSnapshot player,
            GameSession.Snapshot state, boolean header) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        var values = header ? ScoreRows.headings(copy) : ScoreRows.player(copy, player, state, history);
        if (header) {
            row.setBackground(rounded(HEADER, Color.TRANSPARENT, 11));
        } else {
            row.setBackgroundColor(player.human() ? HUMAN_ROW
                    : (player.seat() % 2 == 0 ? PAPER : STRIPE));
            row.setTag("score:" + player.seat());
            row.setContentDescription(copy.scoreLine(player, values.get(3), state.matchScore(player.seat())));
            row.setFocusable(true);
            row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        for (int index = 0; index < values.size(); index++) {
            boolean name = index == 0;
            boolean total = index == values.size() - 1;
            int gravity = name ? Gravity.CENTER_VERTICAL | Gravity.START : Gravity.CENTER;
            addCell(row, values.get(index), ScoreGridStyle.COLUMN_WEIGHTS.get(index),
                    gravity, header || (name || total) && player.human(), header);
        }
        return row;
    }

    private void addCell(LinearLayout row, String value, float weight, int gravity,
            boolean bold, boolean header) {
        TextView cell = new TextView(getContext());
        cell.setText(value);
        cell.setTextSize(header ? 11 : 13);
        cell.setTextColor(header ? MUTED : INK);
        cell.setGravity(gravity);
        cell.setSingleLine(true);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (!header) cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(cell, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

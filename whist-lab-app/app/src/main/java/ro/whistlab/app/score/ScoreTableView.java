package ro.whistlab.app.score;

import ro.whistlab.app.localization.WhistUiText;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.ScoreHistory;
import ro.whistlab.core.session.GameSession;

/** Compact current-hand score table shared by the live view and hand-result screen. */
public final class ScoreTableView extends LinearLayout {
    private static final int INK = Color.rgb(41, 54, 46);
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int PAPER = Color.rgb(255, 254, 248);
    private static final int STRIPE = Color.rgb(244, 247, 240);
    private static final int HUMAN_ROW = Color.rgb(235, 243, 232);
    private static final int RULE = Color.rgb(218, 226, 216);

    private final ScoreHistory history;

    public ScoreTableView(Context context, GameSession.Snapshot state, WhistUiText copy, ScoreHistory history) {
        super(context);
        this.history = history;
        setOrientation(VERTICAL);
        setBackground(rounded(PAPER, RULE, 12));

        addView(row(copy, null, state, true),
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        for (int index = 0; index < state.players().size(); index++) {
            GameSession.PlayerSnapshot player = state.players().get(index);
            addView(row(copy, player, state, false),
                    new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(43)));
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
        if (header) {
            row.setBackground(rounded(Color.rgb(235, 241, 233), Color.TRANSPARENT, 11));
        } else {
            row.setBackgroundColor(player.human() ? HUMAN_ROW
                    : (player.seat() % 2 == 0 ? PAPER : STRIPE));
            row.setTag("score:" + player.seat());
            row.setContentDescription(copy.scoreLine(player, handValue(player, state), state.matchScore(player.seat())));
            row.setFocusable(true);
            row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        addCell(row, header ? copy.scoreColumnPlayer() : copy.playerName(player), 2.0f,
                Gravity.CENTER_VERTICAL | Gravity.START, header || player.human(), header);
        addCell(row, header ? copy.scoreColumnBid() : bidValue(player), 0.78f,
                Gravity.CENTER, header, header);
        addCell(row, header ? copy.scoreColumnTricks() : Integer.toString(player.tricksWon()), 0.78f,
                Gravity.CENTER, header, header);
        addCell(row, header ? copy.scoreColumnHand() : handValue(player, state), 0.82f,
                Gravity.CENTER, header, header);
        addCell(row, header ? copy.scoreColumnTotal() : Integer.toString(state.matchScore(player.seat())), 0.9f,
                Gravity.CENTER, header || player.human(), header);
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

    private String bidValue(GameSession.PlayerSnapshot player) {
        return player.bid() == null ? "—" : Integer.toString(player.bid());
    }

    private String handValue(GameSession.PlayerSnapshot player, GameSession.Snapshot state) {
        if (state.phase() != GamePhase.SCORE) return "—";
        int delta = history.points(state.handNumber(), player.seat());
        return (delta > 0 ? "+" : "") + delta;
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

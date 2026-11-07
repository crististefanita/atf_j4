package ro.whistlab.app.highscores;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.IOException;
import java.util.List;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.profile.PlayerPreferences;

/** A compact local ranking selector; recorded and sample rows remain distinguishable. */
public final class HighScoresDialog {
    private HighScoresDialog() { }
    public static AlertDialog show(Context context, WhistUiText copy, HighScoreRepository repository,
            HighScores.Profile preferred, PlayerPreferences preferences) throws IOException {
        repository.board(preferred);
        List<HighScores.Board> boards = repository.boards();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context,16), dp(context,8), dp(context,16), 0);
        TextView hint = label(context, copy.highScoreHint(), 12, false);
        content.addView(hint);
        Spinner profiles = selector(context, boards.stream().map(board -> profileLabel(board.profile(), copy))
                .collect(java.util.stream.Collectors.toList()));
        profiles.setContentDescription(copy.gameType());
        profiles.setTag("high-score-profile");
        int selected = 0;
        for (int index = 0; index < boards.size(); index++) if (boards.get(index).profile().key().equals(preferred.key())) selected = index;
        profiles.setSelection(selected);
        content.addView(profiles, new LinearLayout.LayoutParams(-1, dp(context,48)));
        Spinner limit = selector(context, List.of("Top 10", "Top 20"));
        limit.setContentDescription(copy.highestScores() + " · Top 10 / Top 20");
        limit.setSelection(preferences.rankingLimit() == 10 ? 0 : 1);
        content.addView(limit, new LinearLayout.LayoutParams(-1, dp(context,48)));
        ScrollView scroll = new ScrollView(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        content.addView(scroll, new LinearLayout.LayoutParams(-1,
                Math.max(dp(context,120), context.getResources().getDisplayMetrics().heightPixels / 3)));
        Runnable render = () -> {
            rows.removeAllViews();
            var entries = boards.get(profiles.getSelectedItemPosition()).entries();
            int count = Math.min(limit.getSelectedItemPosition() == 0 ? 10 : 20, entries.size());
            for (int rank = 0; rank < count; rank++) {
                var entry = entries.get(rank);
                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(context,8), dp(context,8), dp(context,8), dp(context,8));
                row.setBackgroundColor(rank % 2 == 0 ? 0xffeef3ea : 0xfffffef8);
                row.addView(label(context, Integer.toString(rank + 1), 13, false), new LinearLayout.LayoutParams(dp(context,30), -2));
                String kind = (entry.ai() ? " · AI" : "") + (entry.sample() ? " · " + copy.sampleScore() : "");
                String displayName = copy.displayName(entry.name(), !entry.ai());
                TextView name = label(context, displayName + kind, 13, false);
                row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
                TextView score = label(context, Integer.toString(entry.score()), 15, true);
                score.setGravity(Gravity.END);
                row.addView(score, new LinearLayout.LayoutParams(dp(context,60), -2));
                row.setContentDescription((rank + 1) + ". " + displayName + kind + ", " + entry.score() + " " + copy.pointsLabel());
                row.setFocusable(true);
                rows.addView(row);
            }
            scroll.scrollTo(0, 0);
        };
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try { preferences.rankingLimit(limit.getSelectedItemPosition() == 0 ? 10 : 20); }
                catch (IllegalStateException failure) {
                    android.widget.Toast.makeText(context, copy.storageError(), android.widget.Toast.LENGTH_LONG).show();
                }
                render.run();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        };
        profiles.setOnItemSelectedListener(listener);
        limit.setOnItemSelectedListener(listener);
        render.run();
        return new AlertDialog.Builder(context).setTitle(copy.highestScores()).setView(content)
                .setPositiveButton(copy.close(), null).show();
    }

    private static String profileLabel(HighScores.Profile profile, WhistUiText copy) {
        return copy.modeName(profile.mode()) + " · " + profile.players() + " · " + copy.difficultyName(profile.difficulty())
                + (profile.series() ? " · +" + profile.reward() + "/−" + profile.penalty() : "")
                + (profile.allowZeroGame() ? " · " + copy.zeroGameToggle() : "");
    }
    private static Spinner selector(Context context, List<String> labels) {
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }
    private static TextView label(Context context, String value, int size, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(size); view.setTextColor(0xff29362e);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}

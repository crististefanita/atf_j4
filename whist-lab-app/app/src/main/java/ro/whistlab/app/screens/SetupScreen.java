package ro.whistlab.app.screens;

import ro.whistlab.app.localization.WhistUiText;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import java.util.Arrays;
import java.util.stream.Collectors;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;

/** Setup widgets edit retained choices; starting a match is delegated to the owner. */
public final class SetupScreen {
    private final ScreenViews views;
    private final Activity activity;
    private final MenuScreens menus;
    public SetupScreen(ScreenViews views, MenuScreens menus) { this.views = views; this.activity = views.activity(); this.menus = menus; }
    public void show(WhistUiText copy, MatchSetup setup, java.util.function.Consumer<HowToPlay.Language> changed, java.util.function.Consumer<GameConfig> start, Runnable back) {
        LinearLayout page = views.page(null, copy.setupTitle());
        menus.languagePicker(page, copy, changed);

        Spinner players = views.spinner(Arrays.asList("3", "4", "5", "6"), "player-count");
        players.setSelection(setup.playerCount() - 3);
        views.addSetupSelector(page, copy.players(), players);

        Spinner modes = views.spinner(WhistUiText.MENU_MODES.stream().map(copy::modeName).collect(Collectors.toList()),
                "game-type");
        modes.setSelection(WhistUiText.MENU_MODES.indexOf(setup.mode()));
        LinearLayout.LayoutParams modeParams = views.matchWrap();
        modeParams.topMargin = views.dp(4);
        page.addView(modes, modeParams);
        views.addQuietButton(page, copy.rulesAndSchedule(), () -> {
            new AlertDialog.Builder(activity).setTitle(copy.modeName(setup.mode()))
                    .setMessage(copy.modeDescription(setup.mode()) + "\n\n" + copy.schedulePreview(setup.mode(), setup.playerCount())
                            + (setup.mode().isRomanian() ? "\n\n" + copy.streakToggle() : ""))
                    .setPositiveButton(copy.close(), null).show();
        });
        CheckBox streakBonuses=new CheckBox(activity);
        streakBonuses.setText(copy.romanian() ? "Serii de cinci mâini" : "Five-hand streaks");
        streakBonuses.setTextSize(13);
        streakBonuses.setContentDescription(copy.streakToggle());
        streakBonuses.setTag("romanian-streak-bonuses");
        streakBonuses.setChecked(setup.streaks());


        Spinner streakValue=views.spinner(Arrays.asList(copy.fivePointsOriginal(),copy.tenPoints()),"romanian-streak-value");
        streakValue.setSelection(setup.streakBonus() == 5 ? 0 : 1);

        streakValue.setEnabled(setup.streaks());
        streakBonuses.setOnCheckedChangeListener((button,checked)->{
            setup.streaks(checked);
            streakValue.setEnabled(checked);
        });
        LinearLayout streakRow = new LinearLayout(activity);
        streakRow.setGravity(Gravity.CENTER_VERTICAL);
        streakRow.addView(streakBonuses, new LinearLayout.LayoutParams(0, views.dp(48), 1f));
        streakRow.addView(streakValue, new LinearLayout.LayoutParams(views.dp(138), views.dp(48)));
        page.addView(streakRow, views.matchWrap());
        players.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setup.playerCount(position + 3);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        streakValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setup.streakBonus(position == 0 ? 5 : 10);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        Runnable updateModeDetails = () -> {
            streakRow.setVisibility(setup.mode().isRomanian() ? View.VISIBLE : View.GONE);
        };
        updateModeDetails.run();
        modes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setup.mode(WhistUiText.MENU_MODES.get(position));
                updateModeDetails.run();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Spinner difficulty = views.spinner(Arrays.stream(AiDifficulty.values()).map(copy::difficultyName).collect(Collectors.toList()),
                "ai-level");
        difficulty.setSelection(setup.difficulty().ordinal());
        views.addSetupSelector(page, copy.aiLevel(), difficulty);
        difficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                setup.difficulty(AiDifficulty.values()[position]);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        views.addButton(page, copy.startGame(), "btn_start_game", true, () -> {
            int playerCount = Integer.parseInt(String.valueOf(players.getSelectedItem()));
            GameMode mode = WhistUiText.MENU_MODES.get(modes.getSelectedItemPosition());
            AiDifficulty ai = AiDifficulty.values()[difficulty.getSelectedItemPosition()];
            start.accept(GameConfig.newMatch(playerCount, mode, ai, System.nanoTime(), mode.isRomanian() && streakBonuses.isChecked(), streakValue.getSelectedItemPosition() == 0 ? 5 : 10));
        }, true);
        views.addQuietButton(page, copy.back(), back);
    }
}

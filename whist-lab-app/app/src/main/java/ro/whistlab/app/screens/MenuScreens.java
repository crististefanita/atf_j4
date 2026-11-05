package ro.whistlab.app.screens;

import static ro.whistlab.app.screens.ScreenViews.MUTED;

import ro.whistlab.app.localization.WhistUiText;
import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;
import ro.whistlab.core.rules.HowToPlay;


/** Home, help and language controls retain the same widgets and callbacks. */
public final class MenuScreens {
    private final ScreenViews views;
    private final Activity activity;
    public MenuScreens(ScreenViews views) { this.views = views; this.activity = views.activity(); }
    public record Actions(Runnable settings, Runnable setup, Runnable resume, Runnable scores,
                          Runnable help, java.util.function.Consumer<HowToPlay.Language> language) { }
    public void home(WhistUiText copy, String preferredName, boolean canResume, Actions actions) {
        LinearLayout page = views.page(null, copy.homeTitle());
        languagePicker(page, copy, actions.language());
        views.addQuietButton(page, copy.playerSettings() + ": " + (preferredName.isBlank()
                ? (copy.romanian() ? "Tu" : "You") : preferredName), actions.settings());
        views.addButton(page, copy.newGame(), "btn_new_game", true, actions.setup(), true);
        views.addButton(page, copy.resumeGame(), "btn_resume_game", canResume,
                actions.resume(), false);
        views.addQuietButton(page, copy.highestScores(), actions.scores());
        views.addButton(page, copy.help(), "btn_instructions", true,
                actions.help(), false);
    }
    public void instructions(WhistUiText copy, java.util.function.Consumer<HowToPlay.Language> changed, Runnable back) {
        HowToPlay.Language language = copy.language();
        LinearLayout page=views.page(HowToPlay.title(language),language==HowToPlay.Language.RO
                ? "Whist românesc — reguli și exemple" : "Romanian Whist — rules and examples");
        languagePicker(page, copy, changed);
        for(var section:HowToPlay.sections(language))views.addRule(page,section.title(),section.explanation());
        views.addButton(page,copy.back(),"btn_instructions_back",true,back,true);
    }
    public void languagePicker(LinearLayout parent, WhistUiText copy, java.util.function.Consumer<HowToPlay.Language> changed) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = views.text(copy.languageLabel(), 13, MUTED, true);
        row.addView(label, new LinearLayout.LayoutParams(0, views.dp(48), 1f));

        Spinner language = views.spinner(List.of("English", "Română"), "interface-language");
        language.setSelection(copy.language() == HowToPlay.Language.EN ? 0 : 1);
        row.addView(language, new LinearLayout.LayoutParams(views.dp(150), views.dp(48)));
        LinearLayout.LayoutParams params = views.matchWrap();
        params.topMargin = views.dp(6);
        parent.addView(row, params);
        language.post(() -> language.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                changed.accept(position == 0 ? HowToPlay.Language.EN : HowToPlay.Language.RO);
            }
            public void onNothingSelected(AdapterView<?> adapter) { }
        }));
    }
}

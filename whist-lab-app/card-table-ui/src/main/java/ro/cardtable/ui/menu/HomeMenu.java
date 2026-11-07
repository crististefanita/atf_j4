package ro.cardtable.ui.menu;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;
import java.util.function.IntConsumer;
import ro.cardtable.ui.screens.ScreenViews;
import static ro.cardtable.ui.screens.ScreenViews.MUTED;

/** Shared home widgets; the product owns language, routes and profile persistence. */
public final class HomeMenu {
    private final ScreenViews views;
    private final Activity activity;
    public HomeMenu(ScreenViews views) { this.views=views; this.activity=views.activity(); }
    public void home(HomeMenuContent copy, String preferredName, boolean canResume,
            HomeMenuContent.Actions actions, IntConsumer changed) {
        LinearLayout page=views.page(null,copy.title());
        languagePicker(page,copy.languageLabel(),copy.selectedLanguage(),changed);
        for(var control:copy.controls(preferredName,canResume,actions)) {
            if(control.kind()==HomeMenuContent.Kind.QUIET) views.addQuietButton(page,control.label(),control.action());
            else views.addButton(page,control.label(),control.tag(),control.enabled(),control.action(),control.primary());
        }
    }
    public void languagePicker(LinearLayout parent, String languageLabel, int selectedLanguage, IntConsumer changed) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = views.text(languageLabel, 13, MUTED, true);
        row.addView(label, new LinearLayout.LayoutParams(0, views.dp(48), 1f));

        Spinner language = views.spinner(List.of("English", "Română"), "interface-language");
        language.setSelection(selectedLanguage);
        row.addView(language, new LinearLayout.LayoutParams(views.dp(150), views.dp(48)));
        LinearLayout.LayoutParams params = views.matchWrap();
        params.topMargin = views.dp(6);
        parent.addView(row, params);
        language.post(() -> language.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                changed.accept(position);
            }
            public void onNothingSelected(AdapterView<?> adapter) { }
        }));
    }
}

package ro.whistlab.app.screens;


import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ro.whistlab.app.R;
import ro.whistlab.app.localization.WhistUiText;
import ro.cardtable.ui.menu.HomeMenu;
import ro.cardtable.ui.menu.HomeMenuContent;
import ro.whistlab.core.rules.HowToPlay;


/** Home, help and language controls retain the same widgets and callbacks. */
public final class MenuScreens {
    private final ScreenViews views;
    private final HomeMenu home;
    public MenuScreens(ScreenViews views) { this.views = views; this.home = new HomeMenu(views); }
    public record Actions(Runnable settings, Runnable setup, Runnable resume, Runnable scores,
                          Runnable achievements, Runnable help, java.util.function.Consumer<HowToPlay.Language> language) { }
    public void home(WhistUiText copy, String preferredName, boolean canResume, Actions actions) {
        var content=WhistMenuContent.from(copy);
        var callbacks=new HomeMenuContent.Actions(actions.settings(),actions.setup(),
                actions.resume(),actions.scores(),actions.achievements(),actions.help());
        var page = home.homePage(content, preferredName, canResume, callbacks,
                index -> actions.language().accept(index == 0 ? HowToPlay.Language.EN : HowToPlay.Language.RO));
        addDedication(page, copy);
    }

    private void addDedication(LinearLayout page, WhistUiText copy) {
        var photo = new ImageView(views.activity());
        photo.setImageResource(R.drawable.teo_dedication);
        photo.setAdjustViewBounds(true);
        photo.setContentDescription(copy.homeDedicationPhoto());
        var photoLayout = new LinearLayout.LayoutParams(views.dp(112), LinearLayout.LayoutParams.WRAP_CONTENT);
        photoLayout.gravity = Gravity.CENTER_HORIZONTAL;
        photoLayout.topMargin = views.dp(12);
        page.addView(photo, photoLayout);

        var caption = views.text(copy.homeDedication(), 15, ScreenViews.MUTED, false);
        caption.setGravity(Gravity.CENTER);
        var captionLayout = views.matchWrap();
        captionLayout.topMargin = views.dp(8);
        page.addView(caption, captionLayout);
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
        home.languagePicker(parent,copy.languageLabel(),
                copy.language()==HowToPlay.Language.EN?0:1,
                index->changed.accept(index==0?HowToPlay.Language.EN:HowToPlay.Language.RO));
    }
}

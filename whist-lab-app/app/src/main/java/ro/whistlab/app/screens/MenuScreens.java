package ro.whistlab.app.screens;


import ro.whistlab.app.localization.WhistUiText;
import android.widget.LinearLayout;
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
        home.home(content,preferredName,canResume,callbacks,
                index->actions.language().accept(index==0?HowToPlay.Language.EN:HowToPlay.Language.RO));
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

package ro.whistlab.app.screens;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.cardtable.ui.menu.HomeMenuContent;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class WhistMenuContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void realWhistCopyKeepsEveryVisibleMenuLabelAndLanguageSelection() {
        for(var language:HowToPlay.Language.values()) {
            var copy=new WhistUiText(language);var projected=WhistMenuContent.from(copy);
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "copy", copy, "language", language, "projected", projected);
            assertEquals("projected.title() must equal copy.homeTitle()", copy.homeTitle(),projected.title());assertEquals("projected.languageLabel() must equal copy.languageLabel()", copy.languageLabel(),projected.languageLabel());
            assertEquals("projected.selectedLanguage() must equal language == HowToPlay.Language.EN ? 0 : 1", language==HowToPlay.Language.EN?0:1,projected.selectedLanguage());
            assertEquals("projected.profile() must equal copy.playerSettings()", copy.playerSettings(),projected.profile());assertEquals("projected.newGame() must equal copy.newGame()", copy.newGame(),projected.newGame());
            assertEquals("projected.resumeGame() must equal copy.resumeGame()", copy.resumeGame(),projected.resumeGame());assertEquals("projected.scores() must equal copy.highestScores()", copy.highestScores(),projected.scores());assertEquals("projected.achievements() must equal copy.achievements()",copy.achievements(),projected.achievements());
            assertEquals("projected.help() must equal copy.help()", copy.help(),projected.help());assertEquals("projected.blankName() must equal language == HowToPlay.Language.EN ? \"You\" : \"Tu\"", language==HowToPlay.Language.EN?"You":"Tu",projected.blankName());
        }
    }

    @Test public void whistCallbackOrderAndDisabledResumeKeepTheirOriginalMeaning() {
        var invoked=new ArrayList<String>();var copy=new WhistUiText(HowToPlay.Language.EN);
        var actions=new HomeMenuContent.Actions(()->invoked.add("profile"),()->invoked.add("setup"),
                ()->invoked.add("resume"),()->invoked.add("scores"),()->invoked.add("achievements"),()->invoked.add("help"));
        var controls=WhistMenuContent.from(copy).controls("Ana",false,actions);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actions", actions, "controls", controls, "copy", copy, "invoked", invoked);
        assertEquals("controls.get(0).label() must equal copy.playerSettings() + \": Ana\"", copy.playerSettings()+": Ana",controls.get(0).label());assertFalse("controls.get(2).enabled() must remain false", controls.get(2).enabled());assertEquals("controls.get(4).label() must equal copy.achievements()",copy.achievements(),controls.get(4).label());
        for(var control:controls) control.action().run();
        assertEquals("invoked must include all home actions exactly once", List.of("profile","setup","resume","scores","achievements","help"),invoked);
    }
}

package ro.cardtable.ui.menu;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class HomeMenuContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private HomeMenuContent content(boolean romanian) {
        return romanian
                ? new HomeMenuContent("Card room", "Limbă", 1, "Jucător", "Tu", "Partidă nouă", "Continuă", "Scoruri", "Realizări", "Reguli")
                : new HomeMenuContent("Card room", "Language", 0, "Player", "You", "New game", "Resume", "Scores", "Achievements", "Rules");
    }

    @Test public void currentHomeOrderTagsAndResumeStateRemainStableInBothLanguages() {
        for(boolean romanian:new boolean[]{false,true}) for(boolean resume:new boolean[]{false,true}) {
            var copy=content(romanian);
            var controls=copy.controls("Ana",resume,actions(new ArrayList<>()));
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "controls", controls, "copy", copy, "resume", resume, "romanian", romanian);
            assertEquals("controls.size() must equal 6", 6,controls.size());
            assertEquals("controls.stream().map(HomeMenuContent.Control::kind).toList() must equal List.of(HomeMenuContent.Kind.QUIET, HomeMenuContent.Kind.BUTTON, HomeMenuContent.Kind.BUTTON, HomeMenuContent.Kind.QUIET, HomeMenuContent.Kind.QUIET, HomeMenuContent.Kind.BUTTON)", List.of(HomeMenuContent.Kind.QUIET,HomeMenuContent.Kind.BUTTON,HomeMenuContent.Kind.BUTTON,
                    HomeMenuContent.Kind.QUIET,HomeMenuContent.Kind.QUIET,HomeMenuContent.Kind.BUTTON),controls.stream().map(HomeMenuContent.Control::kind).toList());
            assertEquals("controls.stream().map(HomeMenuContent.Control::tag).toList() must include the localized achievement control", List.of(copy.profile()+": Ana","btn_new_game","btn_resume_game",copy.scores(),"btn_achievements","btn_instructions"),
                    controls.stream().map(HomeMenuContent.Control::tag).toList());
            assertEquals("controls.get(2).enabled() must equal resume", resume,controls.get(2).enabled());
            assertEquals("controls.stream().map(HomeMenuContent.Control::primary).toList() must keep only new game primary", List.of(false,true,false,false,false,false),controls.stream().map(HomeMenuContent.Control::primary).toList());
            assertEquals("copy.selectedLanguage() must equal romanian ? 1 : 0", romanian?1:0,copy.selectedLanguage());
        }
    }

    @Test public void blankNameUsesLocalizedDefaultWithoutMutatingTheProvidedName() {
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "Player: You", "declared constant 2", false, "declared constant 3", " ", "declared constant 4", 0, "declared constant 5", "Jucător: Tu", "declared constant 6", true, "declared constant 7", "", "declared constant 8", "Jucător: Ștefan", "declared constant 9", "Ștefan");
        assertEquals("content(false).controls(\" \", false, actions(new ArrayList<>())).get(0).label() must equal \"Player: You\"", "Player: You",content(false).controls(" ",false,actions(new ArrayList<>())).get(0).label());
        assertEquals("content(true).controls(\"\", true, actions(new ArrayList<>())).get(0).label() must equal \"Juc\\u0103tor: Tu\"", "Jucător: Tu",content(true).controls("",true,actions(new ArrayList<>())).get(0).label());
        assertEquals("content(true).controls(\"\\u0218tefan\", true, actions(new ArrayList<>())).get(0).label() must equal \"Juc\\u0103tor: \\u0218tefan\"", "Jucător: Ștefan",content(true).controls("Ștefan",true,actions(new ArrayList<>())).get(0).label());
    }

    @Test public void eachControlRetainsOnlyItsOwnCallbackAndOldPlansKeepTheirResumeState() {
        var seen=new ArrayList<String>();var actions=actions(seen);var copy=content(false);
        var original=copy.controls("Ana",false,actions);
        copy.controls("Mara",true,actions);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actions", actions, "copy", copy, "original", original, "seen", seen);
        assertFalse("original.get(2).enabled() must remain false", original.get(2).enabled());assertEquals("original.get(0).label() must equal \"Player: Ana\"", "Player: Ana",original.get(0).label());
        for(var control:original)control.action().run();
        assertEquals("seen must include achievements without changing the other callbacks", List.of("settings","setup","resume","scores","achievements","help"),seen);
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->original.clear());
    }

    private HomeMenuContent.Actions actions(List<String> seen) {
        return new HomeMenuContent.Actions(()->seen.add("settings"),()->seen.add("setup"),()->seen.add("resume"),
                ()->seen.add("scores"),()->seen.add("achievements"),()->seen.add("help"));
    }
}

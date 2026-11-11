package ro.whistlab.app.screens;

import org.junit.Test;
import ro.cardtable.ui.screens.GameTitleBar;
import ro.whistlab.app.screens.GameScreen.AutoGameHeaderContent;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** The opt-in control follows the Rentz header pattern and only exists during an active hand. */
public final class AutoGameHeaderComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void headerControlIsVisibleAndMirrorsTheHandLeaseDuringBiddingAndPlay() {
        for (GamePhase phase : new GamePhase[]{GamePhase.BIDDING, GamePhase.PLAYING})
            for (boolean active : new boolean[]{false, true})
                for (HowToPlay.Language language : HowToPlay.Language.values()) {
                    var copy = new WhistUiText(language);
                    var control = AutoGameHeaderContent.state(phase, active, copy);
                    atf.gradle.audit.GradleInputs.fixture("Header Auto-play state",
                            "phase", phase, "active", active, "language", language, "control", control);
                    assertTrue("A live hand exposes Auto-play in its header", control.visible());
                    assertEquals("The checkbox mirrors the current hand lease", active, control.checked());
                    assertEquals("The compact caption follows the current language",
                            language == HowToPlay.Language.EN ? "Auto play" : "Automat", control.label());
                    assertEquals("Accessible start action", copy.autoGameHand(), control.startDescription());
                    assertEquals("Accessible stop action", copy.stopAutoGameHand(), control.stopDescription());
                }
    }

    @Test public void completedScoreScreensDoNotShowOrCarryAnActiveHeaderControl() {
        for (HowToPlay.Language language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var control = AutoGameHeaderContent.state(GamePhase.SCORE, true, copy);
            atf.gradle.audit.GradleInputs.fixture("Settled-hand Auto-play state",
                    "language", language, "control", control);
            assertFalse("A settled hand does not expose Auto-play", control.visible());
            assertFalse("A settled hand cannot render a checked control", control.checked());
            assertEquals("The header target remains large enough for touch", 48,
                    GameTitleBar.TOUCH_TARGET_DP);
        }
    }

    @Test public void handVisibilityDoesNotDependOnTheHeaderWidth() {
        var copy = new WhistUiText(HowToPlay.Language.EN);
        var bidding = AutoGameHeaderContent.state(GamePhase.BIDDING, false, copy);
        var playing = AutoGameHeaderContent.state(GamePhase.PLAYING, false, copy);
        atf.gradle.audit.GradleInputs.fixture("Same control across an active hand",
                "bidding", bidding, "playing", playing);
        assertEquals("The shared renderer gets the same unchecked control during play", bidding, playing);
    }
}

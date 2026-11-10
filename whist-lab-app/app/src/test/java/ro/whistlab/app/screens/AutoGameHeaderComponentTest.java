package ro.whistlab.app.screens;

import org.junit.Test;
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
                    var control = AutoGameHeaderContent.state(phase, active, copy, 560);
                    atf.gradle.audit.GradleInputs.fixture("Header Auto-play state",
                            "phase", phase, "active", active, "language", language, "control", control);
                    assertTrue("A live hand exposes Auto-play in its header", control.visible());
                    assertEquals("The checkbox mirrors the current hand lease", active, control.checked());
                    assertEquals("The compact caption follows the current language",
                            language == HowToPlay.Language.EN ? "Auto-play" : "Automat", control.label());
                    assertEquals("The accessible action says how to start or stop",
                            active ? copy.stopAutoGameHand() : copy.autoGameHand(), control.description());
                    assertEquals("A wide game header follows the established inline placement",
                            AutoGameHeaderContent.Placement.INLINE, control.placement());
                }
    }

    @Test public void completedScoreScreensDoNotShowOrCarryAnActiveHeaderControl() {
        for (HowToPlay.Language language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var control = AutoGameHeaderContent.state(GamePhase.SCORE, true, copy, 280);
            atf.gradle.audit.GradleInputs.fixture("Settled-hand Auto-play state",
                    "language", language, "control", control);
            assertFalse("A settled hand does not expose Auto-play", control.visible());
            assertFalse("A settled hand cannot render a checked control", control.checked());
            assertEquals("The header target remains large enough for touch", 48,
                    AutoGameHeaderContent.TOUCH_TARGET_DP);
            assertEquals("A narrow viewport moves the control to its own header row",
                    AutoGameHeaderContent.Placement.BELOW_HEADER, control.placement());
        }
    }

    @Test public void compactAndWideViewportsChooseStableResponsivePlacements() {
        var copy = new WhistUiText(HowToPlay.Language.EN);
        var compact = AutoGameHeaderContent.state(GamePhase.BIDDING, false, copy, 360);
        var wide = AutoGameHeaderContent.state(GamePhase.PLAYING, false, copy, 560);
        atf.gradle.audit.GradleInputs.fixture("Header width placement boundary",
                "inline threshold", AutoGameHeaderContent.INLINE_MIN_WIDTH_DP,
                "compact", compact, "wide", wide);
        assertEquals("The game never crowds the compact title and trump regions",
                AutoGameHeaderContent.Placement.BELOW_HEADER, compact.placement());
        assertEquals("The wide header uses the same inline pattern as Rentz",
                AutoGameHeaderContent.Placement.INLINE, wide.placement());
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class,
                () -> AutoGameHeaderContent.state(GamePhase.BIDDING, false, copy, 0));
    }
}

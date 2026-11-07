package ro.whistlab.app.navigation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public class AppNavigationComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void backReturnsHomeFromEverySecondaryScreen() {
        var navigation = new AppNavigation();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "navigation", navigation);
        assertFalse("navigation.backReturnsHome() must remain false", navigation.backReturnsHome());
        for (var screen : AppNavigation.Screen.values()) {
            navigation.show(screen);
            assertEquals("navigation.screen() must equal screen", screen, navigation.screen());
            assertEquals("navigation.gameVisible() must equal screen == AppNavigation.Screen.GAME", screen == AppNavigation.Screen.GAME, navigation.gameVisible());
            assertEquals("navigation.backReturnsHome() must equal screen != AppNavigation.Screen.HOME", screen != AppNavigation.Screen.HOME, navigation.backReturnsHome());
        }
    }

    @Test public void aGameDialogBlocksAutomationAndResumesOnlyOnce() {
        var navigation = game();
        var dialog = navigation.openDialog();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "dialog", dialog, "navigation", navigation);
        assertTrue("navigation.blocksAutomation() must hold", navigation.blocksAutomation());
        assertTrue("navigation.dismiss(dialog) must hold", navigation.dismiss(dialog));
        assertFalse("navigation.blocksAutomation() must remain false", navigation.blocksAutomation());
        assertFalse("navigation.dismiss(dialog) must remain false", navigation.dismiss(dialog));
    }

    @Test public void nestedDialogsResumeOnlyAfterTheLastOneInEitherDismissalOrder() {
        for (boolean parentFirst : new boolean[]{false, true}) {
            var navigation = game();
            var parent = navigation.openDialog();
            var history = navigation.openDialog();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "navigation", navigation, "parent", parent, "parentFirst", parentFirst);
            assertFalse("navigation.dismiss(parentFirst ? parent : history) must remain false", navigation.dismiss(parentFirst ? parent : history));
            assertTrue("navigation.blocksAutomation() must hold", navigation.blocksAutomation());
            assertTrue("navigation.dismiss(parentFirst ? history : parent) must hold", navigation.dismiss(parentFirst ? history : parent));
            assertFalse("navigation.blocksAutomation() must remain false", navigation.blocksAutomation());
        }
    }

    @Test public void oldDismissalCannotResumeANewRouteOrCloseItsDialog() {
        var navigation = game();
        var old = navigation.openDialog();
        navigation.show(AppNavigation.Screen.HOME);
        navigation.show(AppNavigation.Screen.GAME);
        var current = navigation.openDialog();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "current", current, "navigation", navigation, "old", old);
        assertFalse("navigation.dismiss(old) must remain false", navigation.dismiss(old));
        assertTrue("navigation.blocksAutomation() must hold", navigation.blocksAutomation());
        assertTrue("navigation.dismiss(current) must hold", navigation.dismiss(current));
    }

    @Test public void foreignDialogWithTheSameSequenceCannotCloseThisOwnersDialog() {
        var first = game();
        var second = game();
        var foreign = first.openDialog();
        var local = second.openDialog();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "foreign", foreign, "local", local, "second", second);
        assertFalse("second.dismiss(foreign) must remain false", second.dismiss(foreign));
        assertTrue("second.blocksAutomation() must hold", second.blocksAutomation());
        assertTrue("first.blocksAutomation() must hold", first.blocksAutomation());
        assertTrue("second.dismiss(local) must hold", second.dismiss(local));
        assertTrue("first.dismiss(foreign) must hold", first.dismiss(foreign));
    }

    @Test public void closingADialogOnAMenuNeverRequestsGameAutomation() {
        var navigation = new AppNavigation();
        for (var screen : new AppNavigation.Screen[]{AppNavigation.Screen.HOME,
                AppNavigation.Screen.SETUP, AppNavigation.Screen.INSTRUCTIONS}) {
            navigation.show(screen);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "navigation", navigation, "screen", screen);
            assertFalse("navigation.dismiss(navigation.openDialog()) must remain false", navigation.dismiss(navigation.openDialog()));
            assertFalse("navigation.blocksAutomation() must remain false", navigation.blocksAutomation());
            assertEquals("navigation.screen() must equal screen", screen, navigation.screen());
        }
    }

    @Test public void leavingGameInvalidatesAllNestedDialogLeases() {
        var navigation = game();
        var score = navigation.openDialog();
        var history = navigation.openDialog();
        navigation.show(AppNavigation.Screen.SETUP);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "navigation", navigation, "score", score);
        assertFalse("navigation.blocksAutomation() must remain false", navigation.blocksAutomation());
        assertFalse("navigation.dismiss(score) must remain false", navigation.dismiss(score));
        assertFalse("navigation.dismiss(history) must remain false", navigation.dismiss(history));
        assertEquals("navigation.screen() must equal AppNavigation.Screen.SETUP", AppNavigation.Screen.SETUP, navigation.screen());
    }

    private static AppNavigation game() {
        var navigation = new AppNavigation();
        navigation.show(AppNavigation.Screen.GAME);
        return navigation;
    }
}

package ro.whistlab.app.navigation;

import static org.junit.Assert.*;
import org.junit.Test;

public class AppNavigationComponentTest {
    @Test public void backReturnsHomeFromEverySecondaryScreen() {
        var navigation = new AppNavigation();
        assertFalse(navigation.backReturnsHome());
        for (var screen : AppNavigation.Screen.values()) {
            navigation.show(screen);
            assertEquals(screen, navigation.screen());
            assertEquals(screen == AppNavigation.Screen.GAME, navigation.gameVisible());
            assertEquals(screen != AppNavigation.Screen.HOME, navigation.backReturnsHome());
        }
    }

    @Test public void aGameDialogBlocksAutomationAndResumesOnlyOnce() {
        var navigation = game();
        var dialog = navigation.openDialog();
        assertTrue(navigation.blocksAutomation());
        assertTrue(navigation.dismiss(dialog));
        assertFalse(navigation.blocksAutomation());
        assertFalse(navigation.dismiss(dialog));
    }

    @Test public void nestedDialogsResumeOnlyAfterTheLastOneInEitherDismissalOrder() {
        for (boolean parentFirst : new boolean[]{false, true}) {
            var navigation = game();
            var parent = navigation.openDialog();
            var history = navigation.openDialog();
            assertFalse(navigation.dismiss(parentFirst ? parent : history));
            assertTrue(navigation.blocksAutomation());
            assertTrue(navigation.dismiss(parentFirst ? history : parent));
            assertFalse(navigation.blocksAutomation());
        }
    }

    @Test public void oldDismissalCannotResumeANewRouteOrCloseItsDialog() {
        var navigation = game();
        var old = navigation.openDialog();
        navigation.show(AppNavigation.Screen.HOME);
        navigation.show(AppNavigation.Screen.GAME);
        var current = navigation.openDialog();
        assertFalse(navigation.dismiss(old));
        assertTrue(navigation.blocksAutomation());
        assertTrue(navigation.dismiss(current));
    }

    @Test public void foreignDialogWithTheSameSequenceCannotCloseThisOwnersDialog() {
        var first = game();
        var second = game();
        var foreign = first.openDialog();
        var local = second.openDialog();
        assertFalse(second.dismiss(foreign));
        assertTrue(second.blocksAutomation());
        assertTrue(first.blocksAutomation());
        assertTrue(second.dismiss(local));
        assertTrue(first.dismiss(foreign));
    }

    @Test public void closingADialogOnAMenuNeverRequestsGameAutomation() {
        var navigation = new AppNavigation();
        for (var screen : new AppNavigation.Screen[]{AppNavigation.Screen.HOME,
                AppNavigation.Screen.SETUP, AppNavigation.Screen.INSTRUCTIONS}) {
            navigation.show(screen);
            assertFalse(navigation.dismiss(navigation.openDialog()));
            assertFalse(navigation.blocksAutomation());
            assertEquals(screen, navigation.screen());
        }
    }

    @Test public void leavingGameInvalidatesAllNestedDialogLeases() {
        var navigation = game();
        var score = navigation.openDialog();
        var history = navigation.openDialog();
        navigation.show(AppNavigation.Screen.SETUP);
        assertFalse(navigation.blocksAutomation());
        assertFalse(navigation.dismiss(score));
        assertFalse(navigation.dismiss(history));
        assertEquals(AppNavigation.Screen.SETUP, navigation.screen());
    }

    private static AppNavigation game() {
        var navigation = new AppNavigation();
        navigation.show(AppNavigation.Screen.GAME);
        return navigation;
    }
}

package ro.whistlab.app.screens;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

public class GameMenuContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void menuOffersCurrentHandAutoplayAndAStopActionInBothLanguages() {
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var calls = new ArrayList<String>();
            var actions = new GameScreen.MenuActions(() -> calls.add("score"), () -> calls.add("settings"),
                    () -> calls.add("scores"), () -> calls.add("home"), () -> calls.add("toggle"), false, true,
                    () -> () -> calls.add("dismiss"));
            var items = GameMenuContent.items(copy, actions);
            var auto = items.stream().filter(item -> item.label().equals(copy.autoGameHand())).findFirst().orElseThrow();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actions", actions,
                    "copy", copy, "items", items, "language", language);
            assertEquals("auto.label() must equal copy.autoGameHand()", copy.autoGameHand(), auto.label());
            auto.action().run();
            assertEquals("calls must equal List.of(\"toggle\")", List.of("toggle"), calls);

            var active = new GameScreen.MenuActions(actions.scoreboard(), actions.settings(), actions.scores(),
                    actions.home(), actions.autoGame(), true, true, actions.opened());
            var stop = GameMenuContent.items(copy, active).stream()
                    .filter(item -> item.label().equals(copy.stopAutoGameHand())).findFirst().orElseThrow();
            assertEquals("active menu must keep a single AutoGame entry", 1L, GameMenuContent.items(copy, active).stream()
                    .filter(item -> item.label().equals(copy.autoGameHand()) || item.label().equals(copy.stopAutoGameHand()))
                    .count());
            stop.action().run();
            assertEquals("both menu states must route to the same toggle action", List.of("toggle", "toggle"), calls);

            var scoreBoundary = new GameScreen.MenuActions(actions.scoreboard(), actions.settings(), actions.scores(),
                    actions.home(), actions.autoGame(), false, false, actions.opened());
            assertEquals("AutoGame must be absent after the score boundary", 0L,
                    GameMenuContent.items(copy, scoreBoundary).stream()
                            .filter(item -> item.label().equals(copy.autoGameHand())
                                    || item.label().equals(copy.stopAutoGameHand()))
                            .count());
        }
    }
}

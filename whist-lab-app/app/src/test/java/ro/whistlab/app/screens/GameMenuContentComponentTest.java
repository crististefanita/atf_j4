package ro.whistlab.app.screens;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

public class GameMenuContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void menuOnlyOffersStopWhileAutoplayIsActiveInBothLanguages() {
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var calls = new ArrayList<String>();
            var actions = new GameScreen.MenuActions(() -> calls.add("score"), () -> calls.add("settings"),
                    () -> calls.add("scores"), () -> calls.add("home"), () -> calls.add("stop"), false,
                    () -> () -> calls.add("dismiss"));
            var items = GameMenuContent.items(copy, actions);
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actions", actions,
                    "copy", copy, "items", items, "language", language);
            assertEquals("The inactive menu offers only its ordinary navigation",
                    List.of(copy.scoreSheet(), copy.playerSettings(), copy.highestScores(), copy.mainMenu()),
                    items.stream().map(GameMenuContent.Item::label).collect(java.util.stream.Collectors.toList()));

            var active = new GameScreen.MenuActions(actions.scoreboard(), actions.settings(), actions.scores(),
                    actions.home(), actions.autoGame(), true, actions.opened());
            var activeItems = GameMenuContent.items(copy, active);
            var stop = activeItems.stream()
                    .filter(item -> item.label().equals(copy.stopAutoGameHand())).findFirst().orElseThrow();
            assertEquals("active menu must keep a single AutoGame entry", 1L, activeItems.stream()
                    .filter(item -> item.label().equals(copy.autoGameHand()) || item.label().equals(copy.stopAutoGameHand()))
                    .count());
            stop.action().run();
            assertEquals("The stop item routes once to the action", List.of("stop"), calls);
        }
    }
}

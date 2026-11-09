package ro.whistlab.app.screens;

import java.util.List;
import java.util.Objects;
import ro.whistlab.app.localization.WhistUiText;

/** Keeps game-menu labels and actions testable without constructing Android views. */
public final class GameMenuContent {
    public record Item(String label, Runnable action) {
        public Item { Objects.requireNonNull(label); Objects.requireNonNull(action); }
    }

    public static List<Item> items(WhistUiText copy, GameScreen.MenuActions actions) {
        Objects.requireNonNull(copy);
        Objects.requireNonNull(actions);
        var items = new java.util.ArrayList<Item>();
        items.add(new Item(copy.scoreSheet(), actions.scoreboard()));
        if (actions.autoGameActive()) {
            items.add(new Item(copy.stopAutoGameHand(), actions.autoGame()));
        }
        items.add(new Item(copy.playerSettings(), actions.settings()));
        items.add(new Item(copy.highestScores(), actions.scores()));
        items.add(new Item(copy.mainMenu(), actions.home()));
        return List.copyOf(items);
    }

    private GameMenuContent() { }
}

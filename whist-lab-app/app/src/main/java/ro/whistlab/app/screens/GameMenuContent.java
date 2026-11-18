package ro.whistlab.app.screens;

import java.util.List;
import java.util.Objects;
import ro.whistlab.app.localization.WhistUiText;
import ro.cardtable.ui.menu.TableMenuContent;
import ro.cardtable.ui.menu.TableMenuContent.Item;

/** Keeps game-menu labels and actions testable without constructing Android views. */
public final class GameMenuContent {
    public static List<Item> items(WhistUiText copy, GameScreen.MenuActions actions) {
        Objects.requireNonNull(copy);
        Objects.requireNonNull(actions);
        var items = new java.util.ArrayList<Item>();
        items.add(new Item("score-sheet", copy.scoreSheet(), actions.scoreboard()));
        if (actions.autoGameActive()) {
            items.add(new Item("stop-auto", copy.stopAutoGameHand(), actions.autoGame()));
        }
        items.add(new Item("settings", copy.playerSettings(), actions.settings()));
        items.add(new Item("rankings", copy.highestScores(), actions.scores()));
        items.add(new Item("home", copy.mainMenu(), actions.home()));
        return TableMenuContent.historyFirst(copy.scoreHistory(), actions.history(), items);
    }

    private GameMenuContent() { }
}

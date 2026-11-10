package ro.whistlab.app.screens;

import ro.cardtable.ui.menu.HomeMenuContent;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

/** Whist text projects into the neutral menu without moving rules or preferences. */
public final class WhistMenuContent {
    private WhistMenuContent() { }
    public static HomeMenuContent from(WhistUiText copy) {
        return new HomeMenuContent(copy.homeTitle(),copy.languageLabel(),
                copy.language()==HowToPlay.Language.EN?0:1,copy.playerSettings(),copy.romanian()?"Tu":"You",
                copy.newGame(),copy.resumeGame(),copy.highestScores(),copy.achievements(),copy.help());
    }
}

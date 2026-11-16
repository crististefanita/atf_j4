package ro.whistlab.app.highscores;

import ro.whistlab.app.localization.WhistUiText;

/** Labels for the same ranking entries in both presentation surfaces. */
public final class HighScorePresentation {
    private HighScorePresentation() { }

    public static String profile(HighScores.Profile profile, WhistUiText copy) {
        return copy.modeName(profile.mode()) + " · " + profile.players() + " · " + copy.difficultyName(profile.difficulty())
                + (profile.series() ? " · +" + profile.reward() + "/−" + profile.penalty() : "")
                + (profile.allowZeroGame() ? " · " + copy.zeroGameToggle() : "");
    }

    public static String player(HighScores.Entry entry, WhistUiText copy) {
        return copy.displayName(entry.name(), !entry.ai()) + (entry.ai() ? " · AI" : "")
                + (entry.sample() ? " · " + copy.sampleScore() : "");
    }
}

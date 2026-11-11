package ro.cardtable.ui.menu;

import java.util.List;

/** Product text and the established home control order, independent of Android and game rules. */
public record HomeMenuContent(String title, String languageLabel, int selectedLanguage, String profile,
        String blankName, String newGame, String resumeGame, String scores, String achievements, String help) {
    public HomeMenuContent(String title, String languageLabel, int selectedLanguage, String profile,
            String blankName, String newGame, String resumeGame, String scores, String help) {
        this(title, languageLabel, selectedLanguage, profile, blankName, newGame, resumeGame,
                scores, "Achievements", help);
    }
    public enum Kind { QUIET, BUTTON }
    public record Actions(Runnable settings, Runnable setup, Runnable resume, Runnable scores,
            Runnable achievements, Runnable help) {
        public Actions(Runnable settings, Runnable setup, Runnable resume, Runnable scores, Runnable help) {
            this(settings, setup, resume, scores, () -> { }, help);
        }
    }
    public record Control(Kind kind, String label, String tag, boolean enabled, boolean primary, Runnable action) { }

    public List<Control> controls(String preferredName, boolean canResume, Actions actions) {
        String caption = profile + ": " + (preferredName.isBlank() ? blankName : preferredName);
        return List.of(new Control(Kind.QUIET, caption, caption, true, false, actions.settings()),
                new Control(Kind.BUTTON, newGame, "btn_new_game", true, true, actions.setup()),
                new Control(Kind.BUTTON, resumeGame, "btn_resume_game", canResume, false, actions.resume()),
                new Control(Kind.QUIET, scores, scores, true, false, actions.scores()),
                new Control(Kind.QUIET, achievements, "btn_achievements", true, false, actions.achievements()),
                new Control(Kind.BUTTON, help, "btn_instructions", true, false, actions.help()));
    }
}

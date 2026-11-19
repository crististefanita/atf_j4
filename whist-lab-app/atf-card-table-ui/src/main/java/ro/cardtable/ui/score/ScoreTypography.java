package ro.cardtable.ui.score;

/** Logical score text sizes: Android consumes sp, Web consumes rem at a 16 px root. */
public enum ScoreTypography {
    TITLE(25), HAND(12), SUMMARY(13), BODY(16), WINNER(18), AWARD(14),
    PRIMARY(16), QUIET(14), COLUMN(11), VALUE(13), LIVE_SUMMARY(12), DIALOG_TITLE(20),
    ACTION_PRIMARY(24), ACTION_SECONDARY(21);

    private final int sp;

    ScoreTypography(int sp) { this.sp = sp; }

    public int sp() { return sp; }

    public String cssName() { return "--score-text-" + name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'); }

    public String rem() { return Double.toString(sp / 16.0) + "rem"; }
}

package ro.whistlab.core;

public enum AiDifficulty {
    NORMAL("Normal"),
    HARD("Hard");

    /** Source compatibility for older adapters; never a third menu option. */
    @Deprecated
    public static final AiDifficulty EXPERT = HARD;

    private final String label;

    AiDifficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Old persisted names migrate without discarding the saved match. */
    public static AiDifficulty fromStoredName(String name) {
        return "EXPERT".equals(name) ? HARD : valueOf(name);
    }
}

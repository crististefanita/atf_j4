package ro.whistlab.core;

public enum AiDifficulty {
    NORMAL("Normal"),
    HARD("Hard"),
    EXPERT("Expert (simulation)");

    private final String label;

    AiDifficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

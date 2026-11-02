package ro.whistlab.core;

public enum GameMode {
    OH_HELL("Oh Hell"),
    DESCENDING("Descending"),
    PRACTICE_HAND("Practice hand"),
    ROMANIAN_WHIST("Whist romanesc (1-8-1)");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

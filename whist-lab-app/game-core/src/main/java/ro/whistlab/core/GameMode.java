package ro.whistlab.core;

public enum GameMode {
    OH_HELL("Oh Hell"),
    DESCENDING("Descending"),
    PRACTICE_HAND("Practice hand"),
    ROMANIAN_WHIST("Whist romanesc (1-8-1)"),
    DOWN_AND_UP("Down & Up"),
    MAXIMUM("Maximum"),
    LONG_ROMANIAN("Long Romanian (8-1-8)"),
    REVERSE_OH_HELL("Reverse Oh Hell");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isRomanian() {
        return this == ROMANIAN_WHIST || this == LONG_ROMANIAN;
    }

    public boolean usesTurnedTrump() {
        return isRomanian() || this == OH_HELL || this == REVERSE_OH_HELL;
    }
}

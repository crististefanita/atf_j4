package ro.whistlab.core.rules;

public enum GameMode {
    OH_HELL(0, "Oh Hell"),
    DESCENDING(1, "Descending"),
    ROMANIAN_WHIST(3, "Whist romanesc (1-8-1)"),
    DOWN_AND_UP(4, "Down & Up"),
    LONG_ROMANIAN(6, "Long Romanian (8-1-8)"),
    REVERSE_OH_HELL(7, "Reverse Oh Hell");

    private final String label;
    private final int samplingId;

    GameMode(int samplingId, String label) {
        this.samplingId = samplingId;
        this.label = label;
    }

    /** Stable input to seeded AI sampling, independent of menu order. */
    public int samplingId() {
        return samplingId;
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

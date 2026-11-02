package ro.whistlab.core;

public enum Suit {
    SPADES("Spades", "♠", false),
    HEARTS("Hearts", "♥", true),
    CLUBS("Clubs", "♣", false),
    DIAMONDS("Diamonds", "♦", true);

    private final String label;
    private final String symbol;
    private final boolean red;

    Suit(String label, String symbol, boolean red) {
        this.label = label;
        this.symbol = symbol;
        this.red = red;
    }

    public String label() {
        return label;
    }

    public String symbol() {
        return symbol;
    }

    public boolean red() {
        return red;
    }
}

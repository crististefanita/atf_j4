package ro.cardtable.ui.cards;

/** Ink and text presentation of a card face, independent of its position on the table. */
public final class CardFaceInk {
    public static final int RED = 0xffad393a;
    public static final int DARK = 0xff2a352f;

    private CardFaceInk() { }

    public static int color(boolean red) {
        return red ? RED : DARK;
    }

    /** Requests monochrome text glyphs so the renderer's ink applies to all four suits. */
    public static String textPresentation(String label) {
        return label.replace("♠", "♠\uFE0E").replace("♥", "♥\uFE0E")
                .replace("♦", "♦\uFE0E").replace("♣", "♣\uFE0E");
    }
}

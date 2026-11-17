package ro.whistlab.app.theme;

import ro.cardtable.ui.theme.TableColors;
import ro.whistlab.core.model.Suit;

/** State-independent palette shared by the native and browser trump tiles. */
public final class TrumpTileStyle {
    public static final int FRAME_RADIUS_DP = 11;
    public static final int FRAME_STROKE_DP = 1;
    public static final int FILL = TableColors.ACTIVE_SEAT_FILL;
    public static final int LINE = TableColors.ACTIVE_SEAT_STROKE;
    public static final int INK = TableColors.ACTIVE_SEAT_TEXT;
    public static final int CARD_FILL = TableColors.SURFACE;
    public static final int RED_INK = 0xff9d2d36;

    private TrumpTileStyle() { }

    public static int valueInk(Suit suit) {
        return suit != null && suit.red() ? RED_INK : INK;
    }
}

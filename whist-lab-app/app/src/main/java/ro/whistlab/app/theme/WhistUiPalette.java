package ro.whistlab.app.theme;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

/** Existing Whist name; all visual values and drawing belong to the shared library. */
public final class WhistUiPalette {
    private WhistUiPalette() { }
    public static final int TABLE_FELT = ro.cardtable.ui.theme.TablePalette.TABLE_FELT;
    public static final int ACTIVE_SEAT_FILL = ro.cardtable.ui.theme.TablePalette.ACTIVE_SEAT_FILL;
    public static final int ACTIVE_SEAT_STROKE = ro.cardtable.ui.theme.TablePalette.ACTIVE_SEAT_STROKE;
    public static final int ACTIVE_SEAT_TEXT = ro.cardtable.ui.theme.TablePalette.ACTIVE_SEAT_TEXT;
    public static final int IDLE_SEAT_FILL = ro.cardtable.ui.theme.TablePalette.IDLE_SEAT_FILL;
    public static final int IDLE_SEAT_STROKE = ro.cardtable.ui.theme.TablePalette.IDLE_SEAT_STROKE;
    public static final int IDLE_SEAT_TEXT = ro.cardtable.ui.theme.TablePalette.IDLE_SEAT_TEXT;
    public static GradientDrawable seatFrame(Context context, boolean active) {
        return ro.cardtable.ui.theme.TablePalette.seatFrame(context, active);
    }
}

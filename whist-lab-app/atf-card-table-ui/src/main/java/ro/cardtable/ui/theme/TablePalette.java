package ro.cardtable.ui.theme;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

/** Shared presentation colors for the full-bleed game surface. */
public final class TablePalette {
    public static final int TABLE_FELT = TableColors.TABLE_FELT;
    public static final int ACTIVE_SEAT_FILL = TableColors.ACTIVE_SEAT_FILL;
    public static final int ACTIVE_SEAT_STROKE = TableColors.ACTIVE_SEAT_STROKE;
    public static final int ACTIVE_SEAT_TEXT = TableColors.ACTIVE_SEAT_TEXT;
    public static final int IDLE_SEAT_FILL = TableColors.IDLE_SEAT_FILL;
    public static final int IDLE_SEAT_STROKE = TableColors.IDLE_SEAT_STROKE;
    public static final int IDLE_SEAT_TEXT = TableColors.IDLE_SEAT_TEXT;

    private TablePalette() { }

    public static GradientDrawable seatFrame(Context context, boolean active) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(active ? ACTIVE_SEAT_FILL : IDLE_SEAT_FILL);
        shape.setCornerRadius(Math.round(11 * density));
        shape.setStroke(Math.round(density), active ? ACTIVE_SEAT_STROKE : IDLE_SEAT_STROKE);
        return shape;
    }
}

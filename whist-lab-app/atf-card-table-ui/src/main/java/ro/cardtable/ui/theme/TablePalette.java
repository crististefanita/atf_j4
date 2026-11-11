package ro.cardtable.ui.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/** Shared presentation colors for the full-bleed game surface. */
public final class TablePalette {
    public static final int TABLE_FELT = Color.rgb(23, 79, 60);
    public static final int ACTIVE_SEAT_FILL = Color.rgb(241, 230, 200);
    public static final int ACTIVE_SEAT_STROKE = Color.rgb(220, 181, 105);
    public static final int ACTIVE_SEAT_TEXT = Color.rgb(41, 54, 46);
    public static final int IDLE_SEAT_FILL = Color.rgb(31, 87, 67);
    public static final int IDLE_SEAT_STROKE = Color.rgb(76, 118, 96);
    public static final int IDLE_SEAT_TEXT = Color.rgb(211, 239, 226);

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

package ro.whistlab.app.bidding;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;

/** Keeps the standard selector affordance visible over the application's rounded surface. */
public final class DropdownBackground extends GradientDrawable {
    private final Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public DropdownBackground(float density) {
        this.density = density;
        setColor(Color.rgb(255, 254, 248));
        setCornerRadius(10 * density);
        setStroke(Math.round(density), Color.rgb(211, 220, 210));
        arrow.setColor(Color.rgb(48, 111, 78));
        arrow.setStrokeWidth(1.5f * density);
        arrow.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        float x = getBounds().right - 17 * density;
        float y = getBounds().exactCenterY();
        canvas.drawLine(x - 4 * density, y - 2 * density, x, y + 2 * density, arrow);
        canvas.drawLine(x, y + 2 * density, x + 4 * density, y - 2 * density, arrow);
    }
}

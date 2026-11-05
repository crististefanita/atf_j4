package ro.whistlab.app.window;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

/** Native window protection and measurements share the system-bar and cutout boundary. */
public final class WindowContent {
    private WindowContent() {}

    public static void protect(View root) {
        if (Build.VERSION.SDK_INT < 35) return;
        var base = new WindowPadding(root.getPaddingLeft(), root.getPaddingTop(),
                root.getPaddingRight(), root.getPaddingBottom());
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            var obstruction = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            var padding = base.plus(new WindowPadding(obstruction.left, obstruction.top,
                    obstruction.right, obstruction.bottom));
            view.setPadding(padding.left(), padding.top(), padding.right(), padding.bottom());
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
    }

    public static int widthDp(Context context) { return dimensionDp(context, true); }
    public static int heightDp(Context context) { return dimensionDp(context, false); }

    private static int dimensionDp(Context context, boolean horizontal) {
        if (Build.VERSION.SDK_INT < 30) {
            var config = context.getResources().getConfiguration();
            return horizontal ? config.screenWidthDp : config.screenHeightDp;
        }
        var metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
        var bars = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        var bounds = metrics.getBounds();
        return WindowPadding.usableDp(horizontal ? bounds.width() : bounds.height(),
                horizontal ? bars.left : bars.top, horizontal ? bars.right : bars.bottom,
                context.getResources().getDisplayMetrics().density);
    }
}

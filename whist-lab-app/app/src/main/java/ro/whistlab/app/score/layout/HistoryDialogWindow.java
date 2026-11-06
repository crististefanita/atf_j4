package ro.whistlab.app.score.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.Window;
import ro.whistlab.app.window.WindowContent;

/** Tracks the activity window while the history is open and releases the listener on dismissal. */
public final class HistoryDialogWindow {
    private HistoryDialogWindow() { }

    public static void show(Activity activity, AlertDialog dialog, Runnable dismissed) {
        View owner = activity.getWindow().getDecorView();
        Runnable resize = () -> {
            Window window = dialog.getWindow();
            if (window == null || !dialog.isShowing()) return;
            float density = activity.getResources().getDisplayMetrics().density;
            var size = HistoryViewport.dialogSize(Math.max(1, Math.round(WindowContent.widthDp(activity) * density)),
                    Math.max(1, Math.round(WindowContent.heightDp(activity) * density)), density);
            window.setLayout(size.width(), size.height());
        };
        View.OnLayoutChangeListener observer = (view, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) resize.run();
        };
        dialog.setOnDismissListener(ignored -> {
            owner.removeOnLayoutChangeListener(observer);
            dismissed.run();
        });
        dialog.show();
        owner.addOnLayoutChangeListener(observer);
        resize.run();
    }
}

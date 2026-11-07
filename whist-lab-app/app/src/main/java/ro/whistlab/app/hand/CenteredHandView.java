package ro.whistlab.app.hand;

import android.content.Context;
import android.view.View;
import java.util.function.IntFunction;

/** Preserves the existing view identity while delegating measurement and scrolling. */
public final class CenteredHandView extends ro.cardtable.ui.hand.CenteredHandView {
    public CenteredHandView(Context context, int maximumWidth, IntFunction<View> createFan) {
        super(context, maximumWidth, createFan);
    }
}

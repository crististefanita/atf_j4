package ro.whistlab.app.seat;
import android.content.Context;
import android.widget.LinearLayout;
/** Keeps the established Whist view identity over the shared measured seat. */
public final class PlayerSeatView extends ro.cardtable.ui.seat.PlayerSeatView {
    public PlayerSeatView(Context context, LinearLayout frame, String label, boolean active, int seat, boolean compact) {
        super(context, frame, label, active, seat, compact);
    }

    public PlayerSeatView(Context context, LinearLayout frame, String label, boolean active, int seat,
            boolean compact, int reservedBadgeHeightPx) {
        super(context, frame, label, active, seat, compact, reservedBadgeHeightPx);
    }
}

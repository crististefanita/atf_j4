package ro.whistlab.app.table;
import android.content.Context;
/** Whist keeps ownership of the central mode while the board measures neutral occupied regions. */
final class GameBoardLayout extends ro.cardtable.ui.table.BoardLayout {
    GameBoardLayout(Context context, int players, TableViewportPolicy viewport,
            ro.cardtable.ui.table.TableGeometry.CenterSpec center, int handClearanceDp) {
        super(context, players, center.width(), viewport, center.fullWidth(), center.reservedHeight(), true,
                center.alignment(), handClearanceDp);
    }
}

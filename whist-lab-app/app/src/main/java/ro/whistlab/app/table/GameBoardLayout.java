package ro.whistlab.app.table;
import android.content.Context;
/** Whist keeps ownership of the central mode while the board measures neutral occupied regions. */
final class GameBoardLayout extends ro.cardtable.ui.table.BoardLayout {
    GameBoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean fullWidth, int reservedCenterHeightDp, int handClearanceDp,
            TableGeometry.CenterAlignment alignment) {
        super(context, players, centerWidthDp, viewport, fullWidth, reservedCenterHeightDp, true,
                ro.cardtable.ui.table.TableGeometry.CenterAlignment.valueOf(alignment.name()), handClearanceDp);
    }
}

package ro.whistlab.app.table;
import android.content.Context;
/** Whist keeps ownership of the central mode while the board measures neutral occupied regions. */
final class GameBoardLayout extends ro.cardtable.ui.table.BoardLayout {
    GameBoardLayout(Context context, int players, int centerWidthDp, TableViewportPolicy viewport,
            boolean fullWidth, TableGeometry.CenterAlignment alignment) {
        super(context, players, centerWidthDp, viewport, fullWidth,
                ro.cardtable.ui.table.TableGeometry.CenterAlignment.valueOf(alignment.name()));
    }
}

package ro.whistlab.app.table;
/** Compatibility policy; measured values have one implementation in the UI library. */
final class TableViewportPolicy extends ro.cardtable.ui.table.TableViewportPolicy {
    TableViewportPolicy(int heightDp) { super(heightDp); }
    TableViewportPolicy(int heightDp, float fontScale) { super(heightDp, fontScale); }
}

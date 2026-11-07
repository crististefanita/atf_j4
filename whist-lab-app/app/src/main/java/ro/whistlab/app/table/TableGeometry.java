package ro.whistlab.app.table;


import java.util.ArrayList;
import java.util.List;

/** Measured regions, shared by Android layout and independent rectangle contracts. */
final class TableGeometry {
    enum CenterAlignment { BALANCED, TOWARD_HUMAN }

    record Rect(int left, int top, int right, int bottom) {
        boolean overlaps(Rect other) {
            return left < other.right && right > other.left && top < other.bottom && bottom > other.top;
        }
    }

    record Plan(List<Rect> seats, Rect center) { }

    private static final class Shared {
        static Rect box(ro.cardtable.ui.table.TableGeometry.Rect r) { return new Rect(r.left(), r.top(), r.right(), r.bottom()); }
        static Plan plan(ro.cardtable.ui.table.TableGeometry.Plan p) {
            return new Plan(p.seats().stream().map(Shared::box).collect(java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toList(), List::copyOf)), box(p.center()));
        }
    }
    static int minimumHeight(int players, int seatHeight, int centerHeight, int gap) {
        return ro.cardtable.ui.table.TableGeometry.minimumHeight(players, seatHeight, centerHeight, gap);
    }
    static Plan arrangeDisplay(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows, CenterAlignment alignment) {
        return Shared.plan(ro.cardtable.ui.table.TableGeometry.arrangeDisplay(width, height, players, seatWidth,
                seatHeight, centerWidth, centerHeight, gap, fullWidthCardRows,
                ro.cardtable.ui.table.TableGeometry.CenterAlignment.valueOf(alignment.name())));
    }
    static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight, int centerWidth, int centerHeight, int gap) {
        return arrange(width, height, players, seatWidth, seatHeight, centerWidth, centerHeight, gap, false);
    }
    static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight, int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows) {
        return arrange(width, height, players, seatWidth, seatHeight, centerWidth, centerHeight, gap, fullWidthCardRows, CenterAlignment.BALANCED);
    }
    static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight, int centerWidth,
            int centerHeight, int gap, boolean fullWidthCardRows, CenterAlignment alignment) {
        return Shared.plan(ro.cardtable.ui.table.TableGeometry.arrange(width, height, players, seatWidth,
                seatHeight, centerWidth, centerHeight, gap, fullWidthCardRows,
                ro.cardtable.ui.table.TableGeometry.CenterAlignment.valueOf(alignment.name())));
    }
}

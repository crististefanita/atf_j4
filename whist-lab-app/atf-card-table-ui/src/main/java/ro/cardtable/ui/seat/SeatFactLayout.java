package ro.cardtable.ui.seat;

/** Width policy shared by the native row and its JVM components. */
final class SeatFactLayout {
    private SeatFactLayout() { }

    static boolean stacks(int available, int label, int value, int reservedValue, int gap) {
        return (long) label + gap + Math.max(value, reservedValue) > available;
    }
}

package ro.cardtable.ui.table;

import java.util.List;

/** Shared visual dimensions and colors for a five-column bid, tricks and score table. */
public final class ScoreGridStyle {
    public static final List<Float> COLUMN_WEIGHTS = List.of(2f, .78f, .78f, .82f, .9f);
    public static final int HEADER_DP = 38;
    public static final int ROW_DP = 43;
    public static final int INK = 0xff29362e;
    public static final int MUTED = 0xff69756a;
    public static final int PAPER = 0xfffffef8;
    public static final int STRIPE = 0xfff4f7f0;
    public static final int HUMAN_ROW = 0xffebf3e8;
    public static final int HEADER = 0xffebf1e9;
    public static final int RULE = 0xffdae2d8;

    private ScoreGridStyle() { }
}

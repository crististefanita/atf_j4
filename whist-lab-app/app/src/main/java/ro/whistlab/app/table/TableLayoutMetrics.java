package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

/** Seat dimensions; played-card sizing belongs to TrickCardMetrics. */
public final class TableLayoutMetrics {
    public record CenterReservation(int biddingHeightDp, int reservedHeightDp, boolean useBidStrip,
                             TrickCardMetrics.Layout trickLayout) {
        int playingHeightDp() { return trickLayout.centerHeightDp(); }
        public ro.cardtable.ui.table.PlayedCardMetrics.Layout playedCards() { return trickLayout.shared(); }

        public ro.cardtable.ui.table.TableGeometry.CenterSpec center(int tableWidthDp, float fontScale,
                boolean bidding, int bidOptions) {
            int width = bidding ? BidGridMetrics.actionLayout(bidOptions, tableWidthDp, fontScale, useBidStrip)
                    .centerWidthDp() : trickLayout.safeCenterWidthDp();
            var alignment = bidding ? ro.cardtable.ui.table.TableGeometry.CenterAlignment.TOWARD_HUMAN
                    : ro.cardtable.ui.table.TableGeometry.CenterAlignment.BALANCED;
            return new ro.cardtable.ui.table.TableGeometry.CenterSpec(width, reservedHeightDp, !bidding, alignment);
        }
    }
    public static final int HUMAN_HAND_CLEARANCE_DP = 28;

    static int topInsetPx(int boardHeightPx) { return ro.cardtable.ui.table.TableSizing.topInsetPx(boardHeightPx); }

    static CenterReservation centerReservation(int players, int handSize, int tableWidthDp,
            float fontScale, ro.cardtable.ui.table.TableViewportPolicy viewport) {
        if (players < 3 || players > 6 || handSize < 1 || handSize > 17) {
            throw new IllegalArgumentException("A table reservation needs 3–6 players and a 1–17 card hand");
        }
        boolean useBidStrip = players == 3 && viewport.isCompact() && handSize > 13;
        int biddingHeight = BidGridMetrics.actionLayout(handSize + 1, tableWidthDp, fontScale, useBidStrip)
                .centerHeightDp();
        var trick = useBidStrip ? TrickCardMetrics.compactThreePlayer(tableWidthDp, fontScale, viewport)
                : TrickCardMetrics.forTable(players, tableWidthDp, fontScale, viewport);
        return new CenterReservation(biddingHeight, Math.max(biddingHeight, trick.centerHeightDp()), useBidStrip, trick);
    }

    /** Reserves the same center band for every hand in the selected match schedule. */
    public static CenterReservation matchCenterReservation(GameMode mode, int players, int tableWidthDp,
            float fontScale, ro.cardtable.ui.table.TableViewportPolicy viewport) {
        int largestHand = GameRules.handSizes(mode, players).stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("A table schedule must contain a hand"));
        return centerReservation(players, largestHand, tableWidthDp, fontScale, viewport);
    }

    /** The bid and play screens reserve one full face plus its complete lift region. */
    static int privateHandHeightPx(float density) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.slotHeightPx(
                density, ro.whistlab.app.hand.HandLayoutMetrics.LIFT_CLEARANCE_DP);
    }

    private TableLayoutMetrics() { }

    static int humanHandClearanceDp() { return HUMAN_HAND_CLEARANCE_DP; }

    static int centerPlayWidthDp(int tableWidthDp) {
        return centerPlayWidthDp(tableWidthDp, false);
    }

    static int centerPlayWidthDp(int tableWidthDp, boolean compact) {
        return Math.min(320, tableWidthDp - 2 * seatWidthDp(tableWidthDp, compact) - 20);
    }

    static int seatWidthDp(int tableWidthDp) {
        return seatWidthDp(tableWidthDp, false);
    }

    static int seatWidthDp(int tableWidthDp, boolean compact) {
        return ro.cardtable.ui.table.TableSizing.seatWidthDp(tableWidthDp, compact);
    }
}

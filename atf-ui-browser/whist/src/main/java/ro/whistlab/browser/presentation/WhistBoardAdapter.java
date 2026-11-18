package ro.whistlab.browser.presentation;

import ro.cardtable.browser.BrowserGame;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.table.TableLayoutMetrics;
import ro.whistlab.core.rules.GameMode;

/** Projects the native Whist table policy into the browser's logical coordinate system. */
public final class WhistBoardAdapter {
    private WhistBoardAdapter() { }

    public static BrowserGame.BoardProfile profile(String mode, int players, int width, int options,
                                                   TableViewportPolicy viewport, float fontScale) {
        var reservation = TableLayoutMetrics.matchCenterReservation(GameMode.valueOf(mode), players, width, fontScale, viewport);
        return new BrowserGame.BoardProfile(reservation.center(width, fontScale, options > 0, options),
                TableLayoutMetrics.HUMAN_HAND_CLEARANCE_DP, reservation.playedCards(),
                options == 0 ? null : bids(width, options, reservation.useBidStrip(), fontScale));
    }

    private static BrowserGame.BidControls bids(int width, int options, boolean strip, float scale) {
        var inset = BidGridMetrics.submitInsets(scale, 0, 0);
        return new BrowserGame.BidControls(BidGridMetrics.columnCount(options, width), strip,
                BidGridMetrics.CHOICE_TARGET_DP, BidGridMetrics.CHOICE_ROW_DP, BidGridMetrics.CHOICE_FACE_INSET_DP,
                BidGridMetrics.SUBMIT_WIDTH_DP, BidGridMetrics.SUBMIT_TARGET_DP,
                inset.horizontalPx(), inset.verticalPx(), BidGridMetrics.SUBMIT_TEXT_SP);
    }
}

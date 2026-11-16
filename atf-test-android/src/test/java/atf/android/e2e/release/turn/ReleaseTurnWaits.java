package atf.android.e2e.release.turn;

import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.driver.waiting.UiStateAwaiter;
import java.time.Duration;

import static atf.android.e2e.driver.waiting.UiStateAwaiter.Progress.*;

/** Waits for signed-app turns using fresh hierarchies and the driver's polling clock. */
public final class ReleaseTurnWaits {
    private static final long POLL_MILLIS = 120;
    private final AndroidUiDriver ui;

    public ReleaseTurnWaits(AndroidUiDriver ui) {
        this.ui = ui;
    }

    public String awaitRecommendation(String label, int maximumBid) throws Exception {
        var timeout = Duration.ofSeconds(30);
        return ui.awaitHierarchy(timeout, POLL_MILLIS, xml -> {
            if (!ui.hasText(xml, label)) return WAIT;
            var state = BidChoiceObservation.read(xml, label, maximumBid);
            return state.humanTurn() && state.selected().isPresent() ? MATCH : WAIT;
        }, xml -> "The signed app did not expose a selected human recommendation for '" + label
                + "' within " + timeout + "; texts=" + ui.texts(xml));
    }

    public String awaitHumanCard(Duration timeout) throws Exception {
        return ui.awaitHierarchy(timeout, POLL_MILLIS,
                xml -> ui.legalCardDescriptions(xml).isEmpty() ? WAIT : MATCH,
                xml -> "The signed app did not reach the human's card turn within " + timeout
                        + "; descriptions=" + ui.descriptions(xml));
    }

    public String awaitScore(String expected, Duration timeout, AndroidUiDriver.ViewInterruption notice) throws Exception {
        return ui.awaitHierarchy(timeout, POLL_MILLIS, new UiStateAwaiter.ProgressCondition() {
            private boolean dismissed;

            @Override public UiStateAwaiter.Progress observe(String xml) throws Exception {
                if (!dismissed && notice.dismiss(xml)) {
                    dismissed = true;
                    return RENEW_TIMEOUT;
                }
                return ui.hasText(xml, expected) ? MATCH : WAIT;
            }
        }, xml -> "UI did not show text '" + expected + "' within " + timeout + "; texts=" + ui.texts(xml));
    }

    public String finishOneCardHand(String scoreLabel, AndroidUiDriver.ViewInterruption notice) throws Exception {
        var timeout = Duration.ofSeconds(45);
        return ui.awaitHierarchy(timeout, POLL_MILLIS, xml -> {
            if (notice.dismiss(xml)) return READ_AGAIN;
            if (ui.hasText(xml, scoreLabel)) return MATCH;
            var legalCards = ui.legalCardDescriptions(xml);
            if (legalCards.isEmpty()) return WAIT;
            ui.tapDescription(legalCards.getFirst());
            return READ_AGAIN;
        }, xml -> "Real one-card hand did not reach '" + scoreLabel + "' within " + timeout
                + "; texts=" + ui.texts(xml));
    }
}

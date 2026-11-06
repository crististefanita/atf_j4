package atf.android.e2e.records;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.testkit.AtfComponentTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.whistlab.core.session.GamePhase;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Validates declared E2E preconditions without accessing Android or user files. */
@AtfComponentTest
class RecordsFixturesComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void everyActiveSeatHasALegalDiagnosticBiddingAndPlayingPrecondition() {
        for(int seat=0;seat<4;seat++)for(boolean bidding:List.of(true,false)) {
            var fixture=RecordsFixtures.activeTurn(seat,bidding);var state=fixture.game().snapshot();
            assertEquals(bidding?GamePhase.BIDDING:GamePhase.PLAYING,state.phase());
            assertEquals(seat,bidding?state.biddingSeat():state.currentSeat());
            assertEquals(bidding?seat:4,state.bidsSubmitted());
            assertFalse(fixture.json().path("rankingEligible").asBoolean());
            assertEquals(state,ro.whistlab.core.session.GameSession.restoreComplete(fixture.game().saveState()).snapshot());
        }
    }
    @Test void completeHistoryIsDiagnosticAndOwnsEverySeatAndHand() {
        var fixture=RecordsFixtures.history();var state=fixture.json();
        assertEquals(GamePhase.SCORE,fixture.game().snapshot().phase());
        assertTrue(fixture.game().snapshot().finalHand());
        assertEquals(24,state.path("scores").path("hands").size());
        assertEquals(24,state.path("scores").path("bids").size());
        for(var row:state.path("scores").path("hands"))assertEquals(4,row.size());
        assertFalse(state.path("rankingEligible").asBoolean());
    }

    @Test void finalRankingFixtureStillRequiresExactlyOneActualHumanCard() {
        var fixture=RecordsFixtures.finalHumanCard();var state=fixture.json();
        assertEquals(GamePhase.PLAYING,fixture.game().snapshot().phase());
        assertEquals(0,state.path("currentSeat").asInt());
        assertEquals(23,state.path("scores").path("hands").size());
        assertEquals(3,state.path("currentTrick").size());
        assertEquals(1,state.path("players").get(0).path("hand").size());
        assertTrue(state.path("rankingEligible").asBoolean());
        for(var card:state.path("currentTrick")) {
            assertTrue(card.has("seat")&&card.has("suit")&&card.has("rank"));
            assertFalse(card.has("card"));
        }
        RecordsFixtures.playFirst(fixture.game());
        assertEquals(GamePhase.SCORE,fixture.game().snapshot().phase());
        assertEquals(24,fixture.game().scoreHistory().completedHands());
    }

    @Test void bothSeriesFixturesReachLiteralSymmetricPaymentsFromLegalCards() {
        for(int magnitude:List.of(5,10)) {
            var fixture=RecordsFixtures.series(magnitude);
            assertEquals(7,fixture.json().path("version").asInt());
            assertEquals(magnitude,fixture.json().path("streakPenaltyPoints").asInt());
            RecordsFixtures.finishHand(fixture.game());var state=fixture.game().snapshot();
            assertEquals(GamePhase.SCORE,state.phase());
            assertEquals(List.of(7+magnitude,5+magnitude,5,-1-magnitude),
                    state.players().stream().map(player->player.totalScore()).toList());
            assertEquals(List.of(magnitude,magnitude,0,-magnitude),
                    state.players().stream().map(player->player.streakPoints()).toList());
            assertEquals(List.of(0,0,1,0),state.players().stream().map(player->player.streak()).toList());
            var paid=RecordsFixtures.encode(fixture.game(),false);
            for(boolean romanian:List.of(false,true)) {
                assertTrue(NameHistoryScenarios.description(paid,20,0,romanian).endsWith(
                        (romanian?"Bonus de serie +":"Series bonus +")+magnitude));
                assertTrue(NameHistoryScenarios.description(paid,20,3,romanian).endsWith(
                        (romanian?"Penalizare de serie -":"Series penalty -")+magnitude));
                assertFalse(NameHistoryScenarios.description(paid,20,2,romanian).contains(romanian?"de serie":"Series "));
            }
        }
    }
}

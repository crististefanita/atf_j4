package atf.android.e2e.records;

import atf.testkit.AtfComponentTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.whistlab.core.session.GamePhase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validates declared E2E preconditions without accessing Android or user files. */
@AtfComponentTest
class RecordsFixturesComponentTest {
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
            assertEquals(6,fixture.json().path("version").asInt());
            assertEquals(magnitude,fixture.json().path("streakPenaltyPoints").asInt());
            RecordsFixtures.finishHand(fixture.game());var state=fixture.game().snapshot();
            assertEquals(GamePhase.SCORE,state.phase());
            assertEquals(List.of(7+magnitude,5+magnitude,5,-1-magnitude),
                    state.players().stream().map(player->player.totalScore()).toList());
            assertEquals(List.of(magnitude,magnitude,0,-magnitude),
                    state.players().stream().map(player->player.streakPoints()).toList());
            assertEquals(List.of(0,0,1,0),state.players().stream().map(player->player.streak()).toList());
        }
    }
}

package ro.whistlab.core.ai.information;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.NormalBidPolicy;
import ro.whistlab.core.ai.policy.NormalPlayPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class BlindPolicyObservationTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void bothDifficultiesReceiveOnlyTheActorsHandAndPublicSeatStates() {
        GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 20261104L, true, 10)));
        GameSession.Snapshot snapshot = game.snapshot();
        int actor = snapshot.biddingSeat();

        NormalPlayPolicy.Observation normal = NormalPlayPolicy.observe(snapshot, actor);
        NormalBidPolicy.Observation normalBid = NormalBidPolicy.observe(snapshot, actor);
        MonteCarloAi.View hard = MonteCarloAi.observe(snapshot, actor);



        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actor", actor, "game", game, "hard", hard, "normal", normal, "normalBid", normalBid, "snapshot", snapshot);
        assertEquals("normal.ownHand() must equal snapshot.players().get(actor).hand()", snapshot.players().get(actor).hand(), normal.ownHand());
        assertEquals("normalBid.ownHand() must equal snapshot.players().get(actor).hand()", snapshot.players().get(actor).hand(), normalBid.ownHand());
        assertEquals("hard.hand() must equal snapshot.players().get(actor).hand()", snapshot.players().get(actor).hand(), hard.hand());
        assertEquals("normal.seats().size() must equal 4", 4, normal.seats().size());
        assertEquals("hard.seats().size() must equal 4", 4, hard.seats().size());
        assertFalse("componentNames(NormalPlayPolicy.Observation.class).contains(\"players\") must remain false", componentNames(NormalPlayPolicy.Observation.class).contains("players"));
        assertFalse("componentNames(NormalPlayPolicy.Observation.class).contains(\"opponentHands\") must remain false", componentNames(NormalPlayPolicy.Observation.class).contains("opponentHands"));
        assertFalse("componentNames(NormalBidPolicy.Observation.class).contains(\"opponentHands\") must remain false", componentNames(NormalBidPolicy.Observation.class).contains("opponentHands"));
        assertFalse("componentNames(MonteCarloAi.View.class).contains(\"opponentHands\") must remain false", componentNames(MonteCarloAi.View.class).contains("opponentHands"));
        assertTrue("the declared component operation must hold", Arrays.stream(NormalPlayPolicy.Observation.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
        assertTrue("the declared component operation must hold", Arrays.stream(MonteCarloAi.View.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
        assertTrue("the declared component operation must hold", Arrays.stream(NormalBidPolicy.Observation.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
    }

    private static List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}

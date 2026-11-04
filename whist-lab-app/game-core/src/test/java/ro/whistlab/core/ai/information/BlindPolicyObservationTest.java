package ro.whistlab.core.ai.information;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.NormalBidPolicy;
import ro.whistlab.core.ai.policy.NormalPlayPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GameSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class BlindPolicyObservationTest {
    @Test public void bothDifficultiesReceiveOnlyTheActorsHandAndPublicSeatStates() {
        GameSession game = GameSession.newGame(new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 20261104L, true, 10));
        GameSession.Snapshot snapshot = game.snapshot();
        int actor = snapshot.biddingSeat();

        NormalPlayPolicy.Observation normal = NormalPlayPolicy.observe(snapshot, actor);
        NormalBidPolicy.Observation normalBid = NormalBidPolicy.observe(snapshot, actor);
        MonteCarloAi.View hard = MonteCarloAi.observe(snapshot, actor);

        assertEquals(snapshot.players().get(actor).hand(), normal.ownHand());
        assertEquals(snapshot.players().get(actor).hand(), normalBid.ownHand());
        assertEquals(snapshot.players().get(actor).hand(), hard.hand());
        assertEquals(4, normal.seats().size());
        assertEquals(4, hard.seats().size());
        assertFalse(componentNames(NormalPlayPolicy.Observation.class).contains("players"));
        assertFalse(componentNames(NormalPlayPolicy.Observation.class).contains("opponentHands"));
        assertFalse(componentNames(NormalBidPolicy.Observation.class).contains("opponentHands"));
        assertFalse(componentNames(MonteCarloAi.View.class).contains("opponentHands"));
        assertTrue(Arrays.stream(NormalPlayPolicy.Observation.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
        assertTrue(Arrays.stream(MonteCarloAi.View.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
        assertTrue(Arrays.stream(NormalBidPolicy.Observation.class.getRecordComponents())
                .noneMatch(component -> component.getType() == GameSession.Snapshot.class
                        || component.getType() == GameSession.PlayerSnapshot.class));
    }

    private static List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}

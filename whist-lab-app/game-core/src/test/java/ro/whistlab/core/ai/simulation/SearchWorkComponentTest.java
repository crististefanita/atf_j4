package ro.whistlab.core.ai.simulation;

import static org.junit.Assert.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import ro.whistlab.core.ai.cost.CostPositions;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;

public class SearchWorkComponentTest {
    @Test public void exhaustedWorkAbortsWithoutPartialDecisionOrScopeLeak() {
        var s=CostPositions.create(4,101,CostPositions.Stage.BID_FIRST);
        var before=s.snapshot();
        for(int limit:java.util.List.of(0,1,23))try(var scope=SearchWork.measure(limit)) {
            assertThrows(SearchWork.LimitExceeded.class,()->AiPolicy.chooseBid(s.snapshot(),s.actor(),AiDifficulty.HARD));
            assertEquals(limit,scope.snapshot().workUnits());assertEquals("WORK_LIMIT",scope.snapshot().stopReason());
        }
        assertEquals(before,s.snapshot());
        assertTrue(AiPolicy.chooseBid(s.snapshot(),s.actor(),AiDifficulty.HARD)>=0);
    }

    @Test public void measurementPreservesBothPoliciesAtTwoToEightCards() {
        for(int size=2;size<=8;size++)for(var level:AiDifficulty.values()) {
            var b=CostPositions.create(size,101,CostPositions.Stage.BID_LAST);
            var c=CostPositions.create(size,101,CostPositions.Stage.CARD_FOLLOW);
            int bid=AiPolicy.chooseBid(b.snapshot(),b.actor(),level);
            var card=AiPolicy.chooseCard(c.snapshot(),c.actor(),level);
            try(var scope=SearchWork.measure(10_000_000)) {
                assertEquals(bid,AiPolicy.chooseBid(b.snapshot(),b.actor(),level));
                assertEquals(card,AiPolicy.chooseCard(c.snapshot(),c.actor(),level));
                assertEquals("NONE",scope.snapshot().stopReason());assertTrue(scope.snapshot().workUnits()>0);
            }
        }
    }

    @Test public void cancellationIsDistinctFromWorkExhaustionAndKeepsTheFlag() {
        try(var scope=SearchWork.measure(100)) {
            Thread.currentThread().interrupt();
            assertThrows(CancellationException.class,SearchCancellation::check);
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals("INTERRUPTED",scope.snapshot().stopReason());assertEquals(0,scope.snapshot().workUnits());
        } finally {Thread.interrupted();}
    }

    @Test public void scopesAreThreadOwnedNonNestedAndReturnImmutableFrozenMetrics() throws Exception {
        assertThrows(IllegalArgumentException.class,()->SearchWork.measure(-1));
        var scope=SearchWork.measure(2);
        try {
            assertThrows(IllegalStateException.class,()->SearchWork.measure(2));
            SearchCancellation.check();SearchWork.event(SearchWork.Event.SAMPLE);
            var copied=scope.snapshot();
            assertThrows(UnsupportedOperationException.class,()->copied.events().clear());
            AtomicReference<Throwable> failure=new AtomicReference<>();
            var other=new Thread(()->{
                try {scope.close();failure.set(new AssertionError("Foreign owner closed scope"));}
                catch(IllegalStateException expected) {
                    try(var own=SearchWork.measure(1)){SearchCancellation.check();assertEquals(1,own.snapshot().workUnits());}
                    catch(Throwable problem){failure.set(problem);}
                }
            });
            other.start();other.join(5000);assertFalse(other.isAlive());assertNull(failure.get());
            SearchCancellation.check();assertEquals(1,copied.workUnits());
            assertThrows(SearchWork.LimitExceeded.class,SearchCancellation::check);
            assertEquals(2,scope.snapshot().workUnits());
        } finally {scope.close();}
        var closed=scope.snapshot();scope.close();assertEquals(closed,scope.snapshot());
        try(var next=SearchWork.measure(1)){SearchCancellation.check();assertEquals("NONE",next.snapshot().stopReason());}
    }
}

package ro.whistlab.core.ai.simulation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import ro.whistlab.core.ai.cost.CostPositions;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;

public class SearchWorkComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void exhaustedWorkAbortsWithoutPartialDecisionOrScopeLeak() {
        var s=CostPositions.create(4,101,CostPositions.Stage.BID_FIRST);
        var before=s.snapshot();
        for(int limit:java.util.List.of(0,1,23))try(var scope=SearchWork.measure(limit)) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "limit", limit, "s", s);
            assertThrows("The operation rejects its input with SearchWork.LimitExceeded.class", SearchWork.LimitExceeded.class,()->AiPolicy.chooseBid(s.snapshot(),s.actor(),AiDifficulty.HARD));
            assertEquals("scope.snapshot().workUnits() must equal limit", limit,scope.snapshot().workUnits());assertEquals("scope.snapshot().stopReason() must equal \"WORK_LIMIT\"", "WORK_LIMIT",scope.snapshot().stopReason());
        }
        assertEquals("s.snapshot() must equal before", before,s.snapshot());
        assertTrue("AiPolicy.chooseBid(s.snapshot(), s.actor(), AiDifficulty.HARD) >= 0 must hold", AiPolicy.chooseBid(s.snapshot(),s.actor(),AiDifficulty.HARD)>=0);
    }

    @Test public void measurementPreservesBothPoliciesAtTwoToEightCards() {
        for(int size=2;size<=8;size++)for(var level:AiDifficulty.values()) {
            var b=CostPositions.create(size,101,CostPositions.Stage.BID_LAST);
            var c=CostPositions.create(size,101,CostPositions.Stage.CARD_FOLLOW);
            int bid=AiPolicy.chooseBid(b.snapshot(),b.actor(),level);
            var card=AiPolicy.chooseCard(c.snapshot(),c.actor(),level);
            try(var scope=SearchWork.measure(10_000_000)) {

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "b", b, "bid", bid, "c", c, "card", card, "level", level, "size", size);
                assertEquals("AiPolicy.chooseBid(b.snapshot(), b.actor(), level) must equal bid", bid,AiPolicy.chooseBid(b.snapshot(),b.actor(),level));
                assertEquals("AiPolicy.chooseCard(c.snapshot(), c.actor(), level) must equal card", card,AiPolicy.chooseCard(c.snapshot(),c.actor(),level));
                assertEquals("scope.snapshot().stopReason() must equal \"NONE\"", "NONE",scope.snapshot().stopReason());assertTrue("scope.snapshot().workUnits() > 0 must hold", scope.snapshot().workUnits()>0);
            }
        }
    }

    @Test public void cancellationIsDistinctFromWorkExhaustionAndKeepsTheFlag() {
        try(var scope=SearchWork.measure(100)) {
            Thread.currentThread().interrupt();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 100, "declared constant 2", "INTERRUPTED", "declared constant 3", 0);
            assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class,SearchCancellation::check);
            assertTrue("Thread.currentThread().isInterrupted() must hold", Thread.currentThread().isInterrupted());
            assertEquals("scope.snapshot().stopReason() must equal \"INTERRUPTED\"", "INTERRUPTED",scope.snapshot().stopReason());assertEquals("scope.snapshot().workUnits() must equal 0", 0,scope.snapshot().workUnits());
        } finally {Thread.interrupted();}
    }

    @Test public void scopesAreThreadOwnedNonNestedAndReturnImmutableFrozenMetrics() throws Exception {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", -1, "declared constant 2", 2, "declared constant 3", "Foreign owner closed scope", "declared constant 4", 1, "declared constant 5", 5000, "declared constant 6", "NONE");
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->SearchWork.measure(-1));
        var scope=SearchWork.measure(2);
        try {
            assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class,()->SearchWork.measure(2));
            SearchCancellation.check();SearchWork.event(SearchWork.Event.SAMPLE);
            var copied=scope.snapshot();
            assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class,()->copied.events().clear());
            AtomicReference<Throwable> failure=new AtomicReference<>();
            var other=new Thread(()->{
                try {scope.close();failure.set(new AssertionError("Foreign owner closed scope"));}
                catch(IllegalStateException expected) {
                    try(var own=SearchWork.measure(1)){SearchCancellation.check();assertEquals("own.snapshot().workUnits() must equal 1", 1,own.snapshot().workUnits());}
                    catch(Throwable problem){failure.set(problem);}
                }
            });
            other.start();other.join(5000);assertFalse("other.isAlive() must remain false", other.isAlive());assertNull("failure.get() remains absent", failure.get());
            SearchCancellation.check();assertEquals("copied.workUnits() must equal 1", 1,copied.workUnits());
            assertThrows("The operation rejects its input with SearchWork.LimitExceeded.class", SearchWork.LimitExceeded.class,SearchCancellation::check);
            assertEquals("scope.snapshot().workUnits() must equal 2", 2,scope.snapshot().workUnits());
        } finally {scope.close();}
        var closed=scope.snapshot();scope.close();assertEquals("scope.snapshot() must equal closed", closed,scope.snapshot());
        try(var next=SearchWork.measure(1)){SearchCancellation.check();assertEquals("next.snapshot().stopReason() must equal \"NONE\"", "NONE",next.snapshot().stopReason());}
    }
}

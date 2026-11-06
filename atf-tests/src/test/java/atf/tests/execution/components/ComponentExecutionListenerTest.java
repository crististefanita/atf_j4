package atf.tests.execution.components;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.launcher.TestIdentifier;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ComponentExecutionListenerTest {
    @Test void aPassingLeafCannotHideTheOriginalCauseOfAnOwnerClosingFailure() {
        var listener = new ComponentExecutionListener();
        var engineId = UniqueId.forEngine("controlled");
        var leaf = new AbstractTestDescriptor(engineId.append("test", "passed"), "passing leaf") {
            @Override public TestDescriptor.Type getType() { return TestDescriptor.Type.TEST; }
        };
        listener.executionFinished(TestIdentifier.from(leaf), TestExecutionResult.successful());
        var cause = new IllegalArgumentException("original profile violation");
        var closing = new IllegalStateException("owner closing failed", cause);
        closing.addSuppressed(new java.io.IOException("original cleanup diagnostic"));
        listener.executionFinished(TestIdentifier.from(new EngineDescriptor(engineId, "controlled owner")),
                TestExecutionResult.failed(closing));
        assertEquals(1L, listener.counts().get("PASS"));
        assertFalse(listener.successful());
        assertEquals(1, listener.containerFailures().size());
        String diagnostic = listener.containerFailures().getFirst();
        assertTrue(diagnostic.contains("original profile violation"));
        assertTrue(diagnostic.contains("Suppressed: java.io.IOException: original cleanup diagnostic"));
        assertTrue(diagnostic.contains("[engine:controlled]"));
    }
}

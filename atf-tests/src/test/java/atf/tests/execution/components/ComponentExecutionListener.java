package atf.tests.execution.components;

import atf.tests.reporting.aggregate.ComponentReportCoverage.Invocation;
import java.util.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;

/** Independent JUnit receipt; dynamic invocations retain their native unique IDs. */
final class ComponentExecutionListener implements TestExecutionListener {
    private final Map<String, String> rows = new LinkedHashMap<>();
    private final List<String> failures = new ArrayList<>();
    @Override public void testPlanExecutionStarted(TestPlan plan) {
        for (var root : plan.getRoots()) for (var id : plan.getDescendants(root)) if (id.isTest()) rows.put(id.getUniqueId(), "NOT_EXECUTED");
    }
    @Override public void dynamicTestRegistered(TestIdentifier id) { if (id.isTest()) rows.put(id.getUniqueId(), "NOT_EXECUTED"); }
    @Override public void executionSkipped(TestIdentifier id, String reason) {
        if (id.isTest()) rows.put(id.getUniqueId(), "ABORTED"); else failures.add("Skipped container: " + id.getUniqueId() + " / " + reason);
    }
    @Override public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        String outcome = switch (result.getStatus()) {case SUCCESSFUL -> "PASS";case FAILED -> "FAIL";case ABORTED -> "ABORTED";};
        if (id.isTest()) rows.put(id.getUniqueId(), outcome);
        else if (result.getStatus() == TestExecutionResult.Status.FAILED) failures.add(id.getUniqueId() + " / " + result.getThrowable().map(failure -> {
            var diagnostic = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(diagnostic));
            return diagnostic.toString();
        }).orElse("No original throwable supplied"));
    }
    List<Invocation> invocations() { return rows.entrySet().stream().map(e -> new Invocation(e.getKey(),e.getValue())).toList(); }
    List<String> containerFailures() { return List.copyOf(failures); }
    Map<String, Long> counts() {
        var counts = new TreeMap<String, Long>();rows.values().forEach(v -> counts.merge(v,1L,Long::sum));return counts;
    }
    boolean successful() { return !rows.isEmpty() && failures.isEmpty() && rows.values().stream().allMatch("PASS"::equals); }
}

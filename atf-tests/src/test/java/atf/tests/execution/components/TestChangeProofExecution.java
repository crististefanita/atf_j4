package atf.tests.execution.components;

import atf.android.evidence.component.AndroidComponentReports;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import atf.tests.testchange.TestChangeProof;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public final class TestChangeProofExecution {
    private TestChangeProofExecution() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && args[0].equals("--list")) {
            TestChangeReview.requiredProofs(Path.of(args[1]), args[2], args[3], LocalDate.now(ZoneOffset.UTC))
                    .forEach(System.out::println);
            return;
        }
        if (args.length != 7) throw new IllegalArgumentException("expected checkout, stage, output, provenance, base, control and commit");
        Path checkout = Path.of(args[0]).toRealPath();
        Path stage = Path.of(args[1]).toRealPath();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        Path parent = output.getParent().toRealPath();
        if (parent.startsWith(checkout) || parent.startsWith(stage) || !output.getFileName().toString().equals(args[5]))
            throw new IllegalArgumentException("proof output must be external and named after the control");

        var plan = TestChangeReview.proofPlan(checkout, args[4], args[5], LocalDate.now(ZoneOffset.UTC));
        if (!plan.identity().commit().equals(args[6])) throw new IllegalStateException("TEST-CHANGE-PROOF-COMMIT: staged checkout moved");
        Files.createDirectory(output);
        configure(stage, output, Path.of(args[3]), plan.identity());
        var expected = Stream.concat(plan.checks().positive().stream(), plan.checks().negative().stream()).toList();
        var execution = execute(expected);
        var listener = execution.listener();
        var observed = new LinkedHashMap<String, String>();
        for (var invocation : listener.invocations()) observed.put(invocation.id(), invocation.outcome());
        if (!listener.successful() || !observed.keySet().equals(Set.copyOf(execution.ids().values())))
            throw new IllegalStateException("TEST-CHANGE-PROOF-INVOCATIONS: expected exactly declared successes, observed="
                    + observed + " containers=" + listener.containerFailures());

        Path report = canonicalReport(output);
        var failures = ComponentReportCoverage.violations(listener.invocations(), Files.readString(report));
        if (!failures.isEmpty()) throw new IllegalStateException("TEST-CHANGE-PROOF-EXTENT: " + failures);
        if (!plan.equals(TestChangeReview.proofPlan(checkout, args[4], args[5], LocalDate.now(ZoneOffset.UTC))))
            throw new IllegalStateException("TEST-CHANGE-PROOF-DIFF: review changed during execution");

        TestChangeProof.stage(plan, outcomes(plan.checks().positive(), execution.ids(), observed),
                outcomes(plan.checks().negative(), execution.ids(), observed), output);
        System.out.println("PASS " + plan.identity() + " cases=" + expected.size());
    }

    private static void configure(Path stage, Path output, Path provenance, TestChangeProof.Identity identity) throws Exception {
        var mapper = new ObjectMapper();
        ObjectNode context = (ObjectNode) mapper.readTree(provenance.toFile());
        context.set("testChangeReview", mapper.valueToTree(identity));
        System.setProperty("atf.governance.root", stage.toString());
        System.setProperty(AndroidComponentReports.DIRECTORY, output.toString());
        System.setProperty(AndroidComponentReports.PROVENANCE, context.toString());
    }

    private static Execution execute(List<String> expected) throws ClassNotFoundException {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String selector : expected) {
            var parts = selector.split("#", 2);
            Class<?> owner = Class.forName(parts[0]);
            var candidates = Stream.of(owner.getDeclaredMethods()).filter(method -> method.getName().equals(parts[1])).toList();
            if (candidates.size() != 1) throw new IllegalArgumentException("TEST-CHANGE-PROOF-SELECTOR: " + selector);
            request.selectors(selectMethod(owner, candidates.getFirst()));
        }
        request.filters(EngineFilter.includeEngines("junit-jupiter"), TagFilter.includeTags("component"))
                .configurationParameter("atf.component.aggregate", "true")
                .configurationParameter("atf.test.run.scope", "suite")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
        var listener = new ComponentExecutionListener();
        var ids = new LinkedHashMap<String, String>();
        try (var session = LauncherFactory.openSession()) {
            var launcher = session.getLauncher();
            var discovered = launcher.discover(request.build());
            for (var root : discovered.getRoots()) {
                for (var test : discovered.getDescendants(root)) {
                    if (!test.isTest()) continue;
                    if (!(test.getSource().orElse(null) instanceof MethodSource method))
                        throw new IllegalStateException("TEST-CHANGE-PROOF-SELECTOR: expected a component method " + test.getUniqueId());
                    String selector = method.getClassName() + "#" + method.getMethodName();
                    if (ids.putIfAbsent(selector, test.getUniqueId()) != null)
                        throw new IllegalStateException("TEST-CHANGE-PROOF-SELECTOR: multiple invocations for " + selector);
                }
            }
            if (!ids.keySet().equals(Set.copyOf(expected)))
                throw new IllegalStateException("TEST-CHANGE-PROOF-SELECTOR: discovered " + ids.keySet());
            launcher.execute(discovered, listener);
        }
        return new Execution(listener, Map.copyOf(ids));
    }

    private static List<TestChangeProof.Outcome> outcomes(List<String> selectors, Map<String, String> ids,
                                                          Map<String, String> observed) {
        return selectors.stream().map(selector -> new TestChangeProof.Outcome(selector, observed.get(ids.get(selector)))).toList();
    }

    private record Execution(ComponentExecutionListener listener, Map<String, String> ids) { }

    private static Path canonicalReport(Path output) throws Exception {
        try (var files = Files.list(output)) {
            var reports = files.filter(file -> file.toString().endsWith(".html")).toList();
            if (reports.size() != 1) throw new IllegalStateException("TEST-CHANGE-PROOF-EXTENT: expected one report, got " + reports.size());
            return Files.move(reports.getFirst(), output.resolve(TestChangeProof.REPORT));
        }
    }
}

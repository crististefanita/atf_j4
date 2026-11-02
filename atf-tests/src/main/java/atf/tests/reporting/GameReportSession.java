package atf.tests.reporting;

import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/** Consumer decorator around the unchanged physical Reporting JAR. */
public final class GameReportSession implements AtfReportPorts.Session {
    private final AtfReportPorts.Session delegate;
    private final GameReportPresentation presentation;
    private boolean closed, started, optionalReserved;
    public record OptionalContext(long originalItems,long retainedItems,long originalCharacters,long retainedCharacters) {
        public OptionalContext {
            if(originalItems<0||retainedItems<0||retainedItems>originalItems||originalCharacters<0||retainedCharacters<0||retainedCharacters>originalCharacters)
                throw new IllegalArgumentException("Invalid optional context inventory");
        }
    }
    public void reserveOptionalContext(OptionalContext inventory) {
        if(started||closed||optionalReserved)throw new IllegalStateException("Optional context is captured before scenarios");
        if(inventory.retainedItems()>presentation.policy.optionalMaxItems()||inventory.retainedCharacters()>presentation.policy.optionalMaxCharacters())
            throw new IllegalArgumentException("Captured optional context exceeds the shared run budget");
        optionalReserved=true;
        presentation.originalItems=inventory.originalItems();presentation.retainedItems=inventory.retainedItems();
        presentation.originalCharacters=inventory.originalCharacters();presentation.retainedCharacters=inventory.retainedCharacters();
    }
    public GameReportSession(AtfReportPorts.Session delegate, GameReportConfiguration configuration) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.presentation = new GameReportPresentation(configuration.policy());
    }
    public static GameReportSession open(Path report) { return open(report, GameReportConfiguration.capture()); }
    public static GameReportSession open(Path report, GameReportConfiguration configuration) {
        return new GameReportSession(new ExtentReportSession(report, configuration.settings()), configuration);
    }
    @Override public Path reportPath() { return delegate.reportPath(); }
    @Override public AtfReportPorts.Scenario startScenario(String testClass, String scenario, Collection<String> tags) {
        var created = delegate.startScenario(testClass, scenario, tags); started = true;
        return wrap(created);
    }
    @Override public AtfReportPorts.Scenario startScenario(String testClass, String testPackage, String scenario, Collection<String> tags) {
        return wrap(delegate.startScenario(testClass, testPackage, scenario, tags));
    }
    private AtfReportPorts.Scenario wrap(AtfReportPorts.Scenario scenario) {
        return new AtfReportPorts.Scenario() {
            @Override public String id() { return scenario.id(); }
            @Override public AtfReportPorts.Phase startPhase(String name) {
                var phase = new GameReportPhase(scenario.startPhase(name), presentation);
                if (!presentation.policyPublished) {
                    phase.observation(presentation.configuration()); presentation.policyPublished = true;
                }
                return phase;
            }
            @Override public void complete(AtfReportStatus status) { scenario.complete(status); }
            @Override public boolean hasWarnings() { return scenario.hasWarnings(); }
            @Override public void close() { scenario.close(); }
        };
    }
    @Override public void flush() { delegate.flush(); }
    @Override public void close() {
        if (closed) return;
        closed = true; delegate.close();
        if (presentation.refusal != null) throw new IllegalStateException("Game report audit INCOMPLETE", presentation.refusal);
    }
}

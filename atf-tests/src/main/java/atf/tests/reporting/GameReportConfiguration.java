package atf.tests.reporting;

import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportingSettings;
import atf.tests.learning.EvidencePolicy;
import atf.tests.learning.EvidencePolicyYaml;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves the existing J4 evidence YAML and generic Reporting settings once per publication. */
public record GameReportConfiguration(GameReportPolicy policy, EvidencePolicy selectionPolicy) {
    public GameReportConfiguration {
        Objects.requireNonNull(policy, "policy"); Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        if (policy.mode() != selectionPolicy.detailLevel()) throw new IllegalArgumentException("Inconsistent captured detail levels");
    }
    /** Explicit fixture/integration policy keeps its supplied invocation population intact. */
    public GameReportConfiguration(GameReportPolicy policy) {
        this(policy, new EvidencePolicy(policy.mode(), Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                (int) Math.min(Integer.MAX_VALUE, policy.optionalMaxCharacters()), policy.rowsPerGroup(), policy.fieldsPerGroup(), policy.optionalMaxItems()));
    }
    public static GameReportConfiguration capture() {
        ReportingSettings settings = AtfReportConfigurationLoader.load().reportingSettings();
        try { return from(EvidencePolicyYaml.capture(), settings); }
        catch (Exception failure) { throw new IllegalArgumentException("Cannot capture J4 evidence policy", failure); }
    }
    public static GameReportConfiguration load(Path file, ReportingSettings settings) {
        try { return from(EvidencePolicyYaml.load(file), settings); }
        catch (Exception failure) { throw new IllegalArgumentException("Cannot load J4 evidence policy: " + file, failure); }
    }
    public static GameReportConfiguration from(EvidencePolicy source, ReportingSettings settings) {
        if (settings.assertionMaxDisplayCharacters() != 240 || settings.bodyMaxDisplayCharacters() <= 0)
            throw new IllegalArgumentException("J4 requires assertion preview 240 and a positive configured body budget");
        return new GameReportConfiguration(new GameReportPolicy(source.detailLevel(), source.rowsPerGroup(), source.fieldsPerGroup(),
                settings.bodyMaxDisplayCharacters(), source.optionalMaxItems(), source.optionalContextBudgetCharacters()), source);
    }
    public ReportingSettings settings() { return new ReportingSettings(240, policy.bodyMaxCharacters()); }
}

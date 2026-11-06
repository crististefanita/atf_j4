package atf.tests.reporting;

import org.junit.jupiter.api.extension.RegisterExtension;

import atf.testkit.AtfComponentTest;

import atf.tests.learning.EvidencePolicy;

import static atf.testkit.assertions.AuditedAssertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class GameReportPolicyContractTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @TempDir Path folder;
    private static final String BASE = "detailLevel: AUTO\nmaxDetailedInvocations: 64\nmaxDetailedComparisons: 10000\n"
            + "maxDetailedContextCharacters: 250000\npassingExamplesPerFamily: 1\noptionalContextBudgetCharacters: 65536\n";
    @Test void strictPresentationExtensionsRejectWrongTypesOverflowAndUnlimitedSettings() throws Exception {
        for (String extension : new String[] { "rowsPerGroup: 0", "fieldsPerGroup: -1", "optionalMaxItems: 1.5",
                "optionalMaxItems: 2147483648", "unknown: 3", "rowsPerGroup: 2\nrowsPerGroup: 3" }) {
            Path file = folder.resolve("invalid.yaml"); Files.writeString(file, BASE + extension + "\n");
            assertThrows(IllegalArgumentException.class, () -> GameReportConfiguration.load(file,
                    new atf.reporting.configuration.ReportingSettings(240, 512)), extension);
        }
        var evidence = new EvidencePolicy(EvidencePolicy.DetailLevel.AUTO, 2, 2, 100, 1, 100);
        assertThrows(IllegalArgumentException.class, () -> GameReportConfiguration.from(evidence,
                new atf.reporting.configuration.ReportingSettings(240, 0)));
        assertThrows(IllegalArgumentException.class, () -> GameReportConfiguration.from(evidence,
                new atf.reporting.configuration.ReportingSettings(239, 100)));
    }
    @Test void loadedPolicyDoesNotChangeAfterYamlMutation() throws Exception {
        Path file = folder.resolve("game.yaml");
        Files.writeString(file, BASE.replace("AUTO", "SUMMARY") + "rowsPerGroup: 7\n");
        var captured = GameReportConfiguration.load(file, new atf.reporting.configuration.ReportingSettings(240, 512));
        Files.writeString(file, BASE.replace("AUTO", "FULL") + "rowsPerGroup: 50\n");
        assertEquals(EvidencePolicy.DetailLevel.SUMMARY, captured.policy().mode());
        assertEquals(7, captured.policy().rowsPerGroup());
        assertEquals(512, captured.settings().bodyMaxDisplayCharacters());
        assertEquals(240, captured.settings().assertionMaxDisplayCharacters());
    }
    @Test void optionalBudgetHasExactBoundaryAndNeverUsesRawSensitiveValues() {
        for (var mode : EvidencePolicy.DetailLevel.values()) for (int length : new int[] { 7, 8, 9 }) {
            var p = new GameReportPresentation(new GameReportPolicy(mode, 2, 2, 512, 1, 8));
            var evidence = observation("x".repeat(length));
            if (mode == EvidencePolicy.DetailLevel.FULL && length > 8)
                assertThrows(IllegalArgumentException.class, () -> p.retainOptional(evidence));
            else assertEquals(mode != EvidencePolicy.DetailLevel.SUMMARY && length <= 8, p.retainOptional(evidence));
            assertEquals(length, p.originalCharacters);
        }
        var p = new GameReportPresentation(new GameReportPolicy(EvidencePolicy.DetailLevel.AUTO, 2, 2, 512, 1, 14));
        assertTrue(p.retainOptional(new atf.reporting.model.ReportObservation("secret", java.util.List.of(
                new atf.reporting.model.ReportField("password", "sensitive-value-that-must-never-be-counted-as-retained")), java.util.List.of(), java.util.List.of())));
        assertEquals(14, p.retainedCharacters);
    }
    private static atf.reporting.model.ReportObservation observation(String value) {
        return new atf.reporting.model.ReportObservation("optional", java.util.List.of(new atf.reporting.model.ReportField("note", value)),
                java.util.List.of(), java.util.List.of());
    }
}

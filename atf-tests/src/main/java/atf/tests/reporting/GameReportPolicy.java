package atf.tests.reporting;

import java.util.Objects;
import atf.tests.learning.EvidencePolicy;

/** Immutable consumer presentation limits; captured evidence and verdicts are independent. */
public record GameReportPolicy(EvidencePolicy.DetailLevel mode, int rowsPerGroup, int fieldsPerGroup,
                               int bodyMaxCharacters, int optionalMaxItems, long optionalMaxCharacters) {
    public GameReportPolicy {
        Objects.requireNonNull(mode, "mode");
        if (rowsPerGroup <= 0 || fieldsPerGroup <= 0 || bodyMaxCharacters <= 0
                || optionalMaxItems <= 0 || optionalMaxCharacters <= 0)
            throw new IllegalArgumentException("Game report limits must be positive; zero never means unlimited");
    }
    public static GameReportPolicy defaults() {
        return new GameReportPolicy(EvidencePolicy.DetailLevel.AUTO, 100, 20, 32_768, 32, 65_536);
    }
}

package atf.tests.learning;

/** Consumer-owned immutable scope and presentation settings; captured verdicts are independent. */
public record EvidencePolicy(DetailLevel detailLevel, int maxDetailedInvocations, int maxDetailedComparisons,
                             int maxDetailedContextCharacters, int passingExamplesPerFamily, int optionalContextBudgetCharacters,
                             int rowsPerGroup, int fieldsPerGroup, int optionalMaxItems, java.util.Set<String> alwaysIncludeCollections) {
    public enum DetailLevel { AUTO, SUMMARY, FULL }
    public EvidencePolicy(DetailLevel level, int invocations, int comparisons, int context, int examples, int optional) {
        this(level, invocations, comparisons, context, examples, optional, 100, 20, 32);
    }
    public EvidencePolicy(DetailLevel level, int invocations, int comparisons, int context, int examples, int optional, int rows, int fields, int items) {
        this(level, invocations, comparisons, context, examples, optional, rows, fields, items, java.util.Set.of("regression", "documentation"));
    }
    public EvidencePolicy {
        var collections = new java.util.TreeSet<>(java.util.Objects.requireNonNull(alwaysIncludeCollections));
        if (!java.util.Set.of("regression", "documentation", "learning", "e2e").containsAll(collections)) throw new IllegalArgumentException("Unknown full-evidence collection");
        alwaysIncludeCollections = java.util.Collections.unmodifiableSet(collections);
        java.util.Objects.requireNonNull(detailLevel, "detailLevel");
        if (maxDetailedInvocations <= 0 || maxDetailedComparisons <= 0 || maxDetailedContextCharacters <= 0
                || passingExamplesPerFamily <= 0 || optionalContextBudgetCharacters <= 0
                || rowsPerGroup <= 0 || fieldsPerGroup <= 0 || optionalMaxItems <= 0)
            throw new IllegalArgumentException("Evidence limits must be positive; zero is not unlimited");
    }
}

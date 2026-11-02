package atf.tests.learning;

/** Consumer-owned immutable presentation settings. Never changes execution or oracle verdicts. */
public record EvidencePolicy(DetailLevel detailLevel,int maxDetailedInvocations,int maxDetailedComparisons,
        int maxDetailedContextCharacters,int passingExamplesPerFamily,int optionalContextBudgetCharacters) {
    public enum DetailLevel { AUTO, SUMMARY, FULL }
    public EvidencePolicy {
        java.util.Objects.requireNonNull(detailLevel,"detailLevel");
        if(maxDetailedInvocations<=0||maxDetailedComparisons<=0||maxDetailedContextCharacters<=0||passingExamplesPerFamily<=0||optionalContextBudgetCharacters<=0)
            throw new IllegalArgumentException("Evidence limits must be positive; zero is not unlimited");
    }
}

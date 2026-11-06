package atf.validation.ai;

import atf.testkit.assertions.AuditedAssertions;

import atf.validation.ai.grounding.AiGroundingObservation;
import atf.validation.ai.grounding.AiGroundingRequirement;
import atf.validation.ai.grounding.AiSourceReference;
import atf.validation.ai.invocation.AiInvocationEvidence;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.time.Instant;

import static atf.validation.ai.AiEvaluationSnapshot.Verdict.FAIL;
import static atf.validation.ai.AiEvaluationSnapshot.Verdict.NOT_EVALUATED;
import static atf.validation.ai.AiEvaluationSnapshot.Verdict.PASS;
import static atf.validation.ai.AiFieldComparison.Outcome.MATCH;
import static atf.validation.ai.AiFieldComparison.Outcome.MISMATCH;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.ABSENT;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.EXACT;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.PRESENT;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.NORMALIZED_TEXT;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.SET;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.ORDERED_LIST;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.ALLOWED_VARIANTS;
import static atf.validation.ai.AiFieldExpectation.ComparisonMode.UNDECLARED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest
class AiEvaluationEvaluatorTest {

    private final AiEvaluationEvaluator evaluator = new AiEvaluationEvaluator();

    @Test
    void evaluatesExpectedFieldsAndScoresWeightedMatches() {
        var result = evaluator.evaluate(evaluationCase(), Map.of("decision", "REVIEW_REQUIRED", "confidence", 0.82), realInvocation());

        AuditedAssertions.assertEquals(PASS, result.verdict());
        AuditedAssertions.assertEquals(100, result.qualityScore());
        AuditedAssertions.assertEquals(AiEvaluationSnapshot.Lifecycle.COMPLETE, result.lifecycle());
        AuditedAssertions.assertEquals(AiEvaluationPolicy.defaultPolicy(), result.policy());
        AuditedAssertions.assertEquals(List.of(MATCH, MATCH, MATCH), result.comparisons().stream().map(AiFieldComparison::outcome).toList());
    }

    @Test
    void completesAnImmutableSnapshotWithAnInjectedClock() {
        var completedAt = Instant.parse("2026-10-21T15:00:00Z");
        var deterministicEvaluator = new AiEvaluationEvaluator(AiEvaluationPolicy.defaultPolicy(),
                Clock.fixed(completedAt, java.time.ZoneOffset.UTC));

        var snapshot = deterministicEvaluator.evaluate(evaluationCase(),
                Map.of("decision", "REVIEW_REQUIRED", "confidence", 0.82), realInvocation());

        AuditedAssertions.assertEquals(AiEvaluationSnapshot.Lifecycle.COMPLETE, snapshot.lifecycle());
        AuditedAssertions.assertEquals(completedAt, snapshot.completedAt());
        AuditedAssertions.assertEquals(80, snapshot.policy().minimumPassingScore());
        AuditedAssertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.comparisons().clear());
        AuditedAssertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.hardFailures().clear());
    }

    @Test
    void criticalMismatchFailsEvenWhenAggregateScoreIsHigh() {
        var result = evaluator.evaluate(evaluationCase(), Map.of("decision", "APPROVE", "confidence", 0.82), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(91, result.qualityScore());
        AuditedAssertions.assertEquals("VALUE_MISMATCH", result.comparisons().getFirst().reasonCode());
    }

    @Test
    void reportsMissingAndInventedValuesSeparately() {
        var result = evaluator.evaluate(evaluationCase(), Map.of(
                "decision", "REVIEW_REQUIRED", "customer.accountNumber", "invented"), realInvocation());

        AuditedAssertions.assertEquals("MISSING_OBSERVED", result.comparisons().get(1).reasonCode());
        AuditedAssertions.assertEquals("UNEXPECTED_PRESENT", result.comparisons().get(2).reasonCode());
        AuditedAssertions.assertEquals(MISMATCH, result.comparisons().get(1).outcome());
    }

    @Test
    void failsBelowConfiguredScoreThresholdWithoutCallingItHardFail() {
        var result = new AiEvaluationEvaluator(new AiEvaluationPolicy(90))
                .evaluate(evaluationCase(), Map.of("decision", "REVIEW_REQUIRED"), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertFalse(result.hardFail());
        AuditedAssertions.assertEquals(18, result.qualityScore());
    }

    @Test
    void comparesNormalizedSetOrderedRangeAndAllowedVariants() {
        var expectations = List.of(
                expectation("text", NORMALIZED_TEXT, "Approved  value", 1),
                expectation("tags", SET, List.of("read", "write"), 1),
                expectation("steps", ORDERED_LIST, List.of("created", "reviewed"), 1),
                new AiFieldExpectation("confidence", AiFieldExpectation.Comparison.range(0.70, 0.90), false, 1,
                        AiFieldExpectation.InputPresence.PRESENT),
                expectation("decision", ALLOWED_VARIANTS, List.of("REVIEW", "ESCALATE"), 1));
        var observed = Map.of(
                "text", "  Approved   value ",
                "tags", List.of("write", "read"),
                "steps", List.of("created", "reviewed"),
                "confidence", 0.82,
                "decision", "ESCALATE");

        var result = evaluator.evaluate(new AiEvaluationCase("modes", 1, "fixture:modes:v1", expectations), observed, realInvocation());

        AuditedAssertions.assertEquals(PASS, result.verdict());
        AuditedAssertions.assertEquals(100, result.qualityScore());
    }

    @Test
    void distinguishesMissingCriticalFromInventedCriticalAndHardFailsBoth() {
        var evaluationCase = new AiEvaluationCase("critical-presence", 1, "fixture:critical-presence:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW", true, 1,
                        AiFieldExpectation.InputPresence.PRESENT),
                new AiFieldExpectation("account.number", ABSENT, null, true, 1,
                        AiFieldExpectation.InputPresence.ABSENT)));

        var result = evaluator.evaluate(evaluationCase, Map.of("account.number", "invented"), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(2, result.hardFailures().size());
        AuditedAssertions.assertEquals(List.of("MISSING_CRITICAL_FIELD", "INVENTED_CRITICAL_ENTITY"),
                result.hardFailures().stream().map(AiEvaluationSnapshot.HardFailure::ruleId).toList());
        AuditedAssertions.assertEquals(List.of("MISSING_CRITICAL_FIELD", "INVENTED_CRITICAL_ENTITY"),
                result.comparisons().stream().map(AiFieldComparison::reasonCode).toList());
    }

    @Test
    void rejectsUndeclaredOutputFieldsByDefaultWithAnEvidenceLinkedHardFail() {
        var result = evaluator.evaluate(evaluationCase(),
                Map.of("decision", "REVIEW_REQUIRED", "customer.taxId", "invented"), realInvocation());

        var unexpected = result.comparisons().getLast();
        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(UNDECLARED, unexpected.comparisonMode());
        AuditedAssertions.assertEquals("UNDECLARED_OUTPUT_FIELD", unexpected.reasonCode());
        AuditedAssertions.assertEquals("UNDECLARED_OUTPUT_FIELD", result.hardFailures().getFirst().ruleId());
        AuditedAssertions.assertEquals("evidence:output", unexpected.evidenceReference());
    }

    @Test
    void explicitlyAllowedUndeclaredOutputIsStillVisibleAndLowersTheScore() {
        var permissive = new AiEvaluationEvaluator(new AiEvaluationPolicy(80, false));
        var evaluationCase = new AiEvaluationCase("allow-extra", 1, "fixture:allow-extra:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW_REQUIRED", false, 1)));

        var result = permissive.evaluate(evaluationCase,
                Map.of("decision", "REVIEW_REQUIRED", "customer.taxId", "extra"), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(50, result.qualityScore());
        AuditedAssertions.assertFalse(result.hardFail());
        AuditedAssertions.assertEquals(UNDECLARED, result.comparisons().getLast().comparisonMode());
        AuditedAssertions.assertEquals("UNDECLARED_OUTPUT_FIELD", result.comparisons().getLast().reasonCode());
    }

    @Test
    void mockAndFallbackNeverReceiveAiQualityScores() {
        for (var mode : List.of(AiInvocationEvidence.Mode.MOCK_MODEL, AiInvocationEvidence.Mode.EXPLICIT_FALLBACK)) {
            var result = evaluator.evaluate(evaluationCase(), Map.of("decision", "REVIEW_REQUIRED"),
                    new AiInvocationEvidence(mode, "run-1", "fixture", "fixture-model", true,
                            null, null, List.of()));

            AuditedAssertions.assertEquals(NOT_EVALUATED, result.verdict());
            AuditedAssertions.assertNull(result.qualityScore());
        }
    }

    @Test
    void realInvocationWithoutCompleteEvidenceHardFailsInsteadOfScoring() {
        var incomplete = new AiInvocationEvidence(AiInvocationEvidence.Mode.REAL_AI,
                "run-1", "provider", "model", true, Instant.now(), Instant.now(), List.of());

        var result = evaluator.evaluate(evaluationCase(), Map.of("decision", "REVIEW_REQUIRED"), incomplete);

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertNull(result.qualityScore());
        AuditedAssertions.assertEquals("MISSING_INVOCATION_EVIDENCE", result.hardFailures().getFirst().ruleId());
    }

    @Test
    void rejectsExpectedMissingWhenTheSourceActuallyContainsTheCriticalField() {
        var evaluationCase = new AiEvaluationCase("source-presence", 1, "fixture:source-presence:v1", List.of(
                new AiFieldExpectation("taxId", ABSENT, null, true, 1,
                        AiFieldExpectation.InputPresence.PRESENT)));

        var result = evaluator.evaluate(evaluationCase, Map.of(), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals("SOURCE_PRESENT_BUT_EXPECTED_ABSENT", result.hardFailures().getFirst().ruleId());
    }

    @Test
    void preservesTypedInvocationEvidenceOnTheEvaluationResult() {
        var invocation = realInvocation();

        var result = evaluator.evaluate(evaluationCase(), Map.of("decision", "REVIEW_REQUIRED"), invocation);

        AuditedAssertions.assertEquals(invocation, result.invocationEvidence());
        AuditedAssertions.assertEquals(AiInvocationEvidence.Mode.REAL_AI, result.invocationEvidence().mode());
        AuditedAssertions.assertEquals(4, result.invocationEvidence().references().size());
    }

    @Test
    void recordsSupportedGroundingPerFieldWithoutEmbeddingSourcePayload() {
        var evaluationCase = new AiEvaluationCase("grounded", 1, "fixture:grounded:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW", true, 1,
                        AiFieldExpectation.InputPresence.PRESENT,
                        AiGroundingRequirement.requiredFrom("source:decision"))));
        var source = new AiSourceReference("source:decision", AiSourceReference.SourceType.INPUT_FIELD,
                "input:customer", "excerpt:decision", "decision");

        var result = evaluator.evaluate(evaluationCase, Map.of("decision", "REVIEW"), realInvocation(),
                Map.of("decision", source));

        AuditedAssertions.assertEquals(PASS, result.verdict());
        AuditedAssertions.assertEquals(AiGroundingObservation.Result.SUPPORTED,
                result.groundingObservations().getFirst().result());
        AuditedAssertions.assertEquals(source, result.groundingObservations().getFirst().sourceReference());
        AuditedAssertions.assertThrows(UnsupportedOperationException.class, () -> result.groundingObservations().clear());
    }

    @Test
    void missingCriticalGroundingFailsEvenWhenTheFieldValueMatches() {
        var evaluationCase = new AiEvaluationCase("missing-grounding", 1, "fixture:missing-grounding:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW", true, 1,
                        AiFieldExpectation.InputPresence.PRESENT,
                        AiGroundingRequirement.requiredFrom("source:decision"))));

        var result = evaluator.evaluate(evaluationCase, Map.of("decision", "REVIEW"), realInvocation());

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(AiGroundingObservation.Result.MISSING_REFERENCE,
                result.groundingObservations().getFirst().result());
        AuditedAssertions.assertEquals("MISSING_GROUNDING_REFERENCE", result.hardFailures().getFirst().ruleId());
    }

    @Test
    void rejectsAReferenceOutsideTheExpectedSourceAllowlist() {
        var evaluationCase = new AiEvaluationCase("unsupported-grounding", 1, "fixture:unsupported-grounding:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW", true, 1,
                        AiFieldExpectation.InputPresence.PRESENT,
                        AiGroundingRequirement.requiredFrom("source:decision"))));
        var source = new AiSourceReference("source:other", AiSourceReference.SourceType.API_RESPONSE,
                "response:1", "excerpt:decision", "decision");

        var result = evaluator.evaluate(evaluationCase, Map.of("decision", "REVIEW"), realInvocation(),
                Map.of("decision", source));

        AuditedAssertions.assertEquals(FAIL, result.verdict());
        AuditedAssertions.assertEquals(AiGroundingObservation.Result.UNSUPPORTED,
                result.groundingObservations().getFirst().result());
        AuditedAssertions.assertEquals("UNSUPPORTED_GROUNDING", result.hardFailures().getFirst().ruleId());
    }

    @Test
    void keepsApprovedExpectationsIndependentAndImmutable() {
        var expectations = new java.util.ArrayList<>(List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW_REQUIRED", true, 4)));
        var evaluationCase = new AiEvaluationCase("review-case", 2, "fixture:review-case:v2", expectations);
        expectations.clear();

        AuditedAssertions.assertEquals(1, evaluationCase.expectations().size());
        AuditedAssertions.assertThrows(UnsupportedOperationException.class, () -> evaluationCase.expectations().clear());
        AuditedAssertions.assertEquals("REVIEW_REQUIRED", evaluationCase.expectations().getFirst().comparison().expectedValue());
    }

    @Test
    void freezesNestedExpectedAndObservedValuesAtTheBoundary() {
        var allowed = new java.util.ArrayList<>(List.of("review"));
        var expected = new java.util.HashMap<String, Object>();
        expected.put("allowed", allowed);
        var expectation = new AiFieldExpectation("decision", EXACT, expected, true, 1);
        allowed.add("approve");

        var result = evaluator.evaluate(new AiEvaluationCase("nested", 1, "fixture:nested:v1", List.of(expectation)),
                Map.of("decision", Map.of("allowed", List.of("review"))), realInvocation());

        AuditedAssertions.assertEquals(MATCH, result.comparisons().getFirst().outcome());
        AuditedAssertions.assertThrows(UnsupportedOperationException.class,
                () -> ((List<?>) ((Map<?, ?>) expectation.comparison().expectedValue()).get("allowed")).clear());
    }

    @Test
    void rejectsDuplicateExpectedFieldPaths() {
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> new AiEvaluationCase("duplicate", 1, "fixture:duplicate:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW", true, 1),
                new AiFieldExpectation("decision", PRESENT, null, false, 1))));
    }

    private static AiEvaluationCase evaluationCase() {
        return new AiEvaluationCase("review-case", 1, "fixture:review-case:v1", List.of(
                new AiFieldExpectation("decision", EXACT, "REVIEW_REQUIRED", true, 1),
                new AiFieldExpectation("confidence", PRESENT, null, false, 9),
                new AiFieldExpectation("customer.accountNumber", ABSENT, null, false, 1)));
    }

    private static AiFieldExpectation expectation(String field, AiFieldExpectation.ComparisonMode mode,
                                                  Object expected, int weight) {
        return new AiFieldExpectation(field, mode, expected, false, weight,
                AiFieldExpectation.InputPresence.PRESENT);
    }

    private static AiInvocationEvidence realInvocation() {
        var now = Instant.parse("2026-10-21T12:00:00Z");
        var refs = java.util.Arrays.stream(AiInvocationEvidence.ReferenceKind.values())
                .map(kind -> new AiInvocationEvidence.Reference(kind, "evidence:" + kind.name().toLowerCase()))
                .toList();
        return new AiInvocationEvidence(AiInvocationEvidence.Mode.REAL_AI, "inv-1", "provider-x", "model-y",
                true, now, now.plusSeconds(1), refs);
    }
}

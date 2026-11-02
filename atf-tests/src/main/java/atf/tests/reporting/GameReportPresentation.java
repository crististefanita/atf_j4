package atf.tests.reporting;

import atf.tests.learning.EvidencePolicy;

import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.ArrayList;
import java.util.List;

/** Run-owned projection and optional-context accounting; no process-global mutable policy. */
final class GameReportPresentation {
    final GameReportPolicy policy;
    final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    long originalItems, retainedItems, originalCharacters, retainedCharacters;
    boolean policyPublished;
    IllegalArgumentException refusal;
    GameReportPresentation(GameReportPolicy policy) { this.policy = policy; }
    List<ReportObservation> required(ReportObservation observation) {
        var fields = new ArrayList<>(observation.fields().stream()
                .map(f -> new ReportField(f.name(), redactor.redactField(f.name(), f.value()))).toList());
        if (!observation.artifacts().isEmpty()) fields.add(new ReportField("Diagnostic references original / retained / omitted",
                observation.artifacts().size() + " / 0 / " + observation.artifacts().size() + "; public evidence remains inline"));
        var bodies = new ArrayList<>(observation.bodies());
        for (var field : fields) if (field.value().length() > 240 && observation.bodies().stream()
                .noneMatch(body -> redactor.redactField(body.label(), body.text()).equals(field.value())))
            bodies.add(new ReportObservation.Body("Full field · " + field.name(), field.value(), ReportObservation.Format.TEXT));
        var prepared = new ArrayList<ReportObservation.Body>();
        for (var body : bodies) prepared.addAll(GameEvidenceBodies.prepare(body, policy.bodyMaxCharacters(), redactor));
        var result = new ArrayList<ReportObservation>();
        int groups = Math.max(1, (int) (((long) fields.size() + policy.fieldsPerGroup() - 1) / policy.fieldsPerGroup()));
        for (int i = 0; i < groups; i++) {
            int start = i * policy.fieldsPerGroup(), end = Math.min(fields.size(), start + policy.fieldsPerGroup());
            String title = groups == 1 ? observation.title() : observation.title() + " · fields " + (start + 1) + "–" + end + " of " + fields.size();
            result.add(new ReportObservation(title, fields.subList(start, end), i == 0 ? prepared : List.of(), List.of()));
        }
        return List.copyOf(result);
    }
    boolean retainOptional(ReportObservation observation) {
        long characters = 0;
        for (var field : observation.fields()) characters = Math.addExact(characters, redactor.redactField(field.name(), field.value()).length());
        for (var body : observation.bodies()) characters = Math.addExact(characters, redactor.redactField(body.label(), body.text()).length());
        originalItems = Math.addExact(originalItems, 1);
        originalCharacters = Math.addExact(originalCharacters, characters);
        boolean fits = retainedItems < policy.optionalMaxItems() && characters <= policy.optionalMaxCharacters() - retainedCharacters;
        boolean keep = policy.mode() != EvidencePolicy.DetailLevel.SUMMARY && fits;
        if (!keep && policy.mode() == EvidencePolicy.DetailLevel.FULL)
            throw new IllegalArgumentException("FULL optional evidence exceeds the configured run budget; audit INCOMPLETE");
        if (keep) { retainedItems++; retainedCharacters = Math.addExact(retainedCharacters, characters); }
        return keep;
    }
    ReportObservation configuration() {
        return new ReportObservation("Game report presentation — effective policy", List.of(
                new ReportField("Mode", policy.mode().name()), new ReportField("Assertion preview", "240 characters"),
                new ReportField("Rows / fields per group", policy.rowsPerGroup() + " / " + policy.fieldsPerGroup()),
                new ReportField("Body character budget", Integer.toString(policy.bodyMaxCharacters())),
                new ReportField("Optional items / characters per run", policy.optionalMaxItems() + " / " + policy.optionalMaxCharacters()),
                new ReportField("Mandatory evidence", "All comparisons and required inputs in the declared scope, original outcomes, causes and PNGs are retained."),
                new ReportField("Public profile", "External diagnostic links are counted and excluded in every mode; they are never substitutes for inline evidence."),
                new ReportField("Selection scope", "Learning declares whole-invocation selection separately; legacy invocation scope is unchanged."),
                new ReportField("Budget scope", "Optional context only. SUMMARY omits optional context; AUTO retains within budget; FULL refuses an incomplete projection.")),
                List.of(), List.of());
    }
    ReportObservation inventory() {
        return new ReportObservation("Optional game context — cumulative run inventory", List.of(
                new ReportField("Original / retained / omitted items", originalItems + " / " + retainedItems + " / " + (originalItems - retainedItems)),
                new ReportField("Original / retained / omitted characters",
                        originalCharacters + " / " + retainedCharacters + " / " + (originalCharacters - retainedCharacters)),
                new ReportField("Reason", policy.mode() == EvidencePolicy.DetailLevel.SUMMARY ? "SUMMARY deliberately excludes optional context"
                        : "AUTO/FULL applies positive run-wide optional budgets; mandatory evidence is independent")), List.of(), List.of());
    }
}

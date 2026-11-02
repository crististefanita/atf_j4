# J4 game reports — scope, detail and completeness

One immutable consumer policy controls AUTO, SUMMARY and FULL for the six J4 publishers.
Core C3 and Reporting J16 remain unchanged; J4 prepares evidence and uses the existing
native renderer ports, tables, disclosures and screenshot viewer.

## One configuration, captured once

| Configuration | Responsibility |
|---|---|
| config/game-evidence.yml | consumer scope and presentation limits; explicit override: atf.evidence.policy |
| atf.yaml / atf.yml | generic assertion preview and body budget; explicit override: atf.report.config |
| EvidencePolicy / GameReportConfiguration | resolve both once at composition; later file/property changes do not alter the captured run |

The evidence YAML keeps its six existing required keys and adds three optional presentation
keys with backward-compatible defaults. Unknown keys, duplicate keys, multiple documents,
invalid enums/types, overflow and nonpositive limits are refused.

~~~yaml
# config/game-evidence.yml
detailLevel: AUTO
maxDetailedInvocations: 64
maxDetailedComparisons: 10000
maxDetailedContextCharacters: 250000
passingExamplesPerFamily: 1
optionalContextBudgetCharacters: 65536
rowsPerGroup: 100
fieldsPerGroup: 20
optionalMaxItems: 32
~~~

~~~yaml
# atf.yaml: the shared Reporting vocabulary stays unchanged
reporting:
  assertionMaxDisplayCharacters: 240
  bodyMaxDisplayCharacters: 32768
~~~

The current defaults are in [game-evidence.yml](../config/game-evidence.yml).

## Mode semantics

| Level | Learning invocation scope | Common presentation |
|---|---|---|
| AUTO | retain the supplied population within inclusive thresholds; otherwise deterministic whole examples per family, all non-PASS and quality gates | retain optional context within the captured run budget; group every retained comparison |
| SUMMARY | representative whole passing examples, all non-PASS and quality gates; declared original/retained/omitted IDs | omit optional context and fingerprints; retain all required facts in the declared scope |
| FULL | every supplied invocation and checkpoint | retain requested optional detail within positive budgets or refuse complete-audit acceptance |

- The five legacy publishers retain their original selected invocation scope; no receipt is sampled implicitly from an aggregate report.
- All comparisons in the declared scope retain original names, expected/actual, verdict and order; the display policy does not run the game or recalculate an oracle.
- A selected learning HTML explicitly states that omitted passing games are not individually audited by that artifact.

## Native tables and complete evidence

| Surface | Contract |
|---|---|
| component comparisons | native assertion steps, at most rowsPerGroup rows; following groups state their original row range |
| fragment verdict | computed from its retained rows; the terminal fragment also preserves a caller-supplied group outcome |
| assertion cells | native 240-character preview; complete long values remain attached to the original group/checkpoint |
| observation fields | at most fieldsPerGroup rows; long values retain a complete body, reusing an exact body already in that observation |
| TEXT bodies | ordered parts, preferring line boundaries and preserving CR/LF, Unicode and literal entities |
| JSON bodies | complete typed fragments at object/array boundaries; an oversized indivisible scalar/array item is refused |
| Android images | original complete PNGs and checkpoint identity; the policy does not crop, replace or discard a screenshot |
| observations without comparisons | explicit media API, one Gherkin keyword and zero invented business assertions |

JSON fragments are complete documents, not a prefix that happens to parse. Core C3 validates whole documents strictly; original numeric spellings survive native formatting and fragmentation. The presentation contracts check ordered parts and complete typed values; LearningReportHtmlAudit also verifies original checkpoint ownership, final rows and verdicts.

- A missing or reordered part cannot certify complete evidence.

## Optional data and audit refusal

- Callers explicitly mark optional diagnostics; labels or filenames do not decide whether assertion evidence may disappear.
- Fingerprints selected upstream and optional observations share one cumulative item/character budget, with original/retained/omitted counts and reasons.
- Required setup, compared inputs, original outcomes, failure causes and images do not consume an optional omission allowance.
- External diagnostic references are counted and excluded from the public HTML in every mode; an external file cannot substitute for required inline evidence.
- INCOMPLETE remains a publication refusal through session close, even if a caller catches an earlier body/budget error; captured business outcomes remain separate.

| Budget unit | Boundary |
|---|---|
| retained content | UTF-16 character counts; body limits follow the native Reporting length policy |
| HTML overhead | escaping, labels and renderer markup add size beyond the content budget |
| required evidence | may still produce a large artifact; publication does not promise unlimited memory or a fixed total HTML size |

## Responsibilities and verification

The composition layer lives in atf-tests main Java. Existing business producers and
independent oracles remain at their owners; Reporting runtime implementation and generic
browser regressions remain in J0.

[Owner contracts](../atf-tests/README.md) and [verification evidence](ATF_Game_Report_Verification.md)
separate source tests, physical-JAR rendering, archived execution data and new live journeys.
[Active feedback](feedback/ATF_Android_Alignment_Feedback.md) retains the remaining receipt,
legacy business-audit, timing and device criteria.

# J4 acceptance and report composition

## Pe scurt — purpose and ownership

This consumer module verifies Core/Reporting binaries and composes captured component,
learning and Android evidence. Runtime implementation and generic renderer regressions
remain in J0; J4 owns receipt selection, business meaning and publication acceptors.

## Shared presentation policy

All six publishers use the same immutable configuration and native Reporting ports.
Reusable implementation is in main Java; JUnit entrypoints remain in test sources.

| Owner | Responsibility |
|---|---|
| EvidencePolicy / EvidencePolicyYaml | strict consumer YAML; AUTO, SUMMARY, FULL and positive limits |
| EvidenceSelection | whole learning invocations, all non-PASS and quality gates; original/retained/omitted inventory |
| GameReportConfiguration | capture the policy and generic settings once; preview 240 and a positive body budget |
| GameReportSession / GameReportPhase | native scopes, bounded tables, required values, optional context and cumulative accounting |
| GameEvidenceBodies | complete JSON fragments or ordered TEXT parts; refuse indivisible over-budget JSON |
| GameReportTextAudit / LearningReportHtmlAudit | decode TEXT once and compare complete JSON; the learning acceptor also checks original checkpoint/row ownership |

[Game report presentation](../docs/ATF_Game_Report_Presentation.md) defines configuration,
mode semantics, completeness limits and consumer boundaries.

## Publisher coverage

| Entrypoint | Scope |
|---|---|
| RuleComponentExtentReportTest | recorded rule/component selection and independent trace |
| WhistCorpusExtentReportTest | evaluator outcomes, detailed reference replay and historical media |
| WhistAiTrainingExtentReportTest | component/AI acceptance and captured Android duel |
| FullMatchExtentReportTest | original and parallel Oh Hell evidence |
| RomanianExpertExtentReportTest | recorded complete Romanian Expert evidence |
| LearningRunReport | typed receipts, whole-case scope selection and native comparisons |

- Legacy publishers retain their original invocation scope; they do not infer business verdicts or missing comparisons.
- Learning declares its selected scope; all supplied required comparisons, inputs, failure causes and PNGs remain independent of optional-context display.
- Native units keep their separate reporting category.

## Component verification

Run from an owned TMP checkout with its physical lib pairs:

~~~powershell
mvn -B -pl atf-tests -am "-Dtest=GameReportComponentSuite,LearningRunReportContractTest,EvidenceSelectionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

- GameReportComponentSuite covers policy boundaries, redaction, JSON/TEXT parts and individual/combined Extent/fallback reports.
- LegacyUiMediaReportContractTest covers one Gherkin keyword, original PNGs and observation-only media without fabricated comparisons.
- LearningRunReportContractTest checks original rows, long values, timing and deliberate owner/verdict/last-row/fragment mutations.
- EvidenceSelectionContractTest retains the independent scope, threshold, YAML and incomplete-publication controls.
- CoreReportingConsumerContractTest and AndroidInlineScreenshotReportContractTest remain separate binary/adapter contracts.

[The verification record](../docs/ATF_Game_Report_Verification.md) distinguishes fresh
contracts, archived-evidence republication and independent J1/J2/J3 sanity checks.

## Aggregate and device execution

DeepSuiteExtentReportTest remains an opt-in aggregate publisher.

- Its manifest adapter uses the native UI API, without a private renderer, stylesheet or JavaScript.
- Android execution requires explicit activation and an exclusive device lease.
- Offline republication is separate from a new device journey.

Build and delivery follow the [owned TMP process](../../atf_j0/docs/ATF_Build_And_Execution_Safety.md).

# J4 report audit — components and captured E2E

## Scope

The initial J15 review and single-artifact prototype below are dated evidence. The later reader-first review uses J16 for generic viewer acceptance; consumer composition and device acceptance remain separate.

Review date: 2 November 2026; physical Core C3 / Reporting J15. All builds and generated evidence stayed in owned TMP staging; source code, runtime libraries and Android state were not changed by this audit.

- Fresh component execution is distinct from reconstruction of a previously completed Android match.
- New device/UI execution remains deferred while the shared-device lease is active, as agreed with the user.
- Native unit tests retain their separate category; they do not automatically inherit the component/E2E reporting contract.

## Executed checks

| Selection | Result | Reporting acceptance |
|---|---|---|
| Android adapter, independent oracles and focused renderer contracts | 27 distinct invocations PASS, including the refreshed observation-only PNG contract | focused contracts; not a complete business-report proof |
| GameRulesTest alone | 17 native cases PASS | diagnostic only; no new requirement that native units publish E2E-style reports |
| RomanianWhistModelTest alone | 88 model cases PASS | publisher fails before HTML because the unrelated AI tournament receipt is absent |
| GameRulesTest, RomanianStreakContractTest, RomanianWhistModelTest and AiTournamentTest | 130 cases PASS; 88 complete games, 40,304 plays and 8,712 tricks | component publisher PASS; presentation/scope gaps remain |
| recorded complete Expert Android evidence | fresh offline reconstruction PASS for 24 hands and 376 plays | no new Android execution |
| recorded E2E publisher | HTML produced; publisher verification FAIL | stale serialized-HTML matcher rejects a correctly encoded CR payload; candidate is not accepted |

The standalone model and the combined selection have separate evidence directories. The later model run does not reuse the combined AI receipt; the missing-dependency failure is genuine.

## Exact data and rendering audit

| Surface | Observed result |
|---|---|
| E2E checkpoints | all 287 checked in their original phase and order |
| assertion rows | all 11,047 names, values and captured verdicts checked against the original receipt |
| PNG ownership | 177 canonical embedded PNGs plus 110 references; all 287 decoded checkpoint payloads match their captured image bytes |
| long assertion cells | four complete values remain in the matching metadata observation; this does not close the generic bounded-cell criterion 133 |
| TEXT fidelity | the rejected observed value has 2,856 characters and 152 CR; the original-owner DOM preserves it exactly once |
| component assertions | zero standard assertion tables; the model trajectory uses fields and bodies instead of evaluated comparison rows |
| component timing | 39.742 s summed from the four JUnit suites versus about 0.579 s shown for the two composed report scenarios |
| declared coverage | constants 6,375 / 4,960 appear under Executed coverage although the dedicated exhaustive classes were not selected |
| external references | 578 file href references in the captured E2E candidate; the required public inline profile is not yet met |

The PNG viewer renders the final 1080×1920 score sheet correctly, including the captured winner and final totals. Duplicate references preserve image bytes; counting only data URIs would incorrectly report 110 missing captures.

## Deliberate negative controls

| Control | Observed behavior | Owner |
|---|---|---|
| empty receipt, PASS and zero phases | PASS scenario with no checkpoints, assertions or PNG | J4 receipt acceptance |
| one captured testcase changed to SKIP | case observation still says PASS; aggregate skipped=1 and final publisher acceptance fails | J4 per-case result projection |
| same PNG in two synthetic scenarios | opening the last duplicate changes active owner 1 to 0; 82 open nodes become zero | J0 Reporting |
| close duplicate PNG viewer after long scroll | content collapses but the reader stays below it, producing an apparently blank pane | J0 Reporting |

These controls are synthetic diagnostics, not new business failures or device runs. Negative publisher exits are retained as evidence; they are not relabeled as green execution gates.

## Remediation boundary

| Owner | Required work |
|---|---|
| J4 | strict receipt/result mapping, independent per-invocation comparisons, dynamic metrics and captured execution timing |
| J4 | individual model report without a mandatory unrelated benchmark; semantic inline facts instead of file links or raw dumps |
| J4 | parse rendered TEXT once and retain typed JSON comparison; distinguish captured business status from publisher verification |
| J0 | preserve active owner, disclosure state and scroll when viewing a referenced PNG; permanent neutral source/JAR/browser regressions |

[Active J4 criteria](feedback/ATF_Android_Alignment_Feedback.md#audit-componentă-și-randare--probe-noi-din-2-noiembrie) and [J0 viewer defect 137](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-return-137--png-reutilizat-schimbă-ownerul-și-pierde-expandările) track separate owners.

## Reproduction and evidence

| Receipt in the owned audit root | Meaning |
|---|---|
| snapshot-ready.json and framework-refresh.json | frozen live inputs, Reporting hash and subsequent selective refresh |
| selection-gradle.status.json and component-selection/execution-inventory.json | exact fresh selection, not a full-reactor claim |
| model-individual-publisher.log | missing AI receipt after the standalone model passed |
| checkpoint-owner-audit.json | per-checkpoint assertion and decoded-PNG verification |
| text-fidelity-probe.json | serialized matcher false, exact DOM match true |
| negative-skip/mutation.json | one deliberate testcase mutation and before/after digests |
| recorded-expert/review-provenance.json | captured execution C3/J12, current publication J15; no new device run |

The temporary probe sources are diagnostic evidence. Permanent regression families are explicitly required in the owner feedback and are not claimed as implemented by these probes.

Concurrent AI-training and Gradle edits were detected and preserved. The audit does not claim execution of later AI-training additions; the inspected report entrypoints and their hashes remain recorded in the snapshot.

## Single-artifact review — explicit capture boundary

A second diagnostic prototype composes the frozen 130-case selection through the unchanged public J15 API. It produces one autonomous HTML with four original class owners and 130 invocation scenarios, without external evidence links or a synthetic PASS summary test. This is a report-composition review, not a new execution or a production migration.

| Independent check | Result and boundary |
|---|---|
| original selection | 130 unique class/method/parameter identities and original outcomes |
| timing | method intervals sum to 39.542 s; suite intervals sum to 39.742 s and have a different scope |
| model summaries | all facts from 88 invocation-specific receipts are inline |
| AI benchmark | all 144 original paired results are inline; no raw properties dump |
| representative comparisons | 425 original comparisons in 119 standard collapsible groups, attached only to players=4, AI=NORMAL, seed=0 |
| original context | expected, actual, phase, rules, ordinal and full context retained per recorded comparison |
| standalone model | 88 invocations compose without ai-tournament.properties |
| controls | one SKIP stays SKIP; zero cases and a contradictory completed trace are refused |
| browser | first and final comparison groups render; Space closes, Enter reopens; adjacent group remains closed |
| portability | HTML-only copy has no file evidence links, external script/stylesheet dependency or remote CSS URL |

The frozen model producer writes its representative trace only for that exact parameter set, after completed assertEquals calls, and marks the receipt PASS only after the method completes. The diagnostic projection verifies that producer snapshot, the passed invocation and the completed receipt; it does not rerun the game oracle. This narrowly attests the 425 persisted comparisons, not every internal assertion or the other 87 model traces.

Legacy outcome-only invocations retain their original result and an explicit missing-detail notice. Full audit acceptance requires [typed execution receipts and one composition root](ATF_Android_Report_Contract.md#one-artifact-original-execution-identities), including partial failures, independent verdicts and checkpoint ownership. The prototype decoder is temporary review support and must not become a production inference from arbitrary JUnit PASS.

### Screenshot scope added to the contract

[Full-display capture](ATF_Android_Report_Contract.md#full-display-android-screenshots) preserves the complete Android screen and original PNG bytes. [Viewer criterion 138](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-viewport-138--imagine-integrală-în-spațiul-disponibil) requires the available browser viewport with fit/contain, preserving aspect ratio and all edges; this is not implemented by the current J15 viewer. Side margins for portrait images are legitimate, while zoom/original-size is an explicit inspection mode.

The screenshot-return defect 137 remains independently open. The device lease was observed released during this document increment; no fresh ADB journey is claimed by the report prototype or these offline checks.

| Diagnostic receipt | Scope |
|---|---|
| owned TMP root | ATF_SINGLE_ARTIFACT_REVIEW_ACTIVE_DO_NOT_CLEAN_gLg9Oh |
| prototype-validation.json | independent exact artifact checks |
| projection-controls.json | deliberate input mutations and expected refusals |
| prototype-receipt.json | captured execution outcomes; REVIEW_ONLY_PARTIAL_CAPTURE |

No runtime library changed, so this increment does not publish a new JAR or replace the deployed C3/J15 pairs.

## Reader-first rendering review — live publisher inputs

The current corpus artifact has nine root owners, 3,222 comparison rows and no file links. Its first evaluator-summary table contains 1,137 PASS/PASS rows under validation/request/phase nesting; the independent game inputs and rule checks are not explained by those summary cells. Twelve observation bodies include a raw body of 26,511 characters, so a physically autonomous HTML is not yet a complete reader-oriented audit.

| Owner | Required improvement |
|---|---|
| J4 | meaningful purpose, effective setup/input/state, explicit rule variant, independent expected values and inline notation legend |
| J4 | native component assertion groups, scoped evaluator summaries and readable grouping without invented JUnit tests |
| J4 | measured action timing and full decoded Android capture dimensions; optional deduplicated technical provenance |
| J0 | common screenshot viewport, exact local captions and stable return context; neutral permanent mock/browser regressions |

The refined temporary reader prototype moves the source checksum into optional class-level provenance, adds setup/oracle explanations and the classic-rule/notation legend, and retains all 425 original captured comparisons. Its 130 outcomes and legacy missing-detail notices are preserved; it remains a diagnostic review, not a migration of the live J4 publishers.

The [J16 library evidence](ATF_Reporting_J16_Verification.md) tracks the generic fix and separate adoption. [J4-RPT-07 through J4-RPT-13](feedback/ATF_Android_Alignment_Feedback.md#reader-autonomy--input-uri-pentru-implementarea-j4) are implementation inputs for the consumer; replacing its JAR does not supply absent business receipts. No fresh Android execution is claimed by this review.

### Historical observation-only frames

The frozen J13 AI run 20261102-143525 retains its original publication receipt: eight Instructions screenshots were supplied as THEN, with an OBSERVE action and no business assertions. The explicit uiValidationAsMedia route preserves observation-only images without manufacturing comparisons. This is historical consumer evidence, not a declaration that J16 loses those frames.

The original component publication also contains 15,336 series-domain comparisons from 5,112 combinations and 425 model-state comparisons. These are comparison populations, not distinct JUnit tests, and they do not replace captured setup/input details. Full receipts remain in the original run archive; the HTML audit must carry the necessary facts independently.

## Concurrent consumer evidence — J15 learning publication

The following evidence was recorded by the consumer agent during this review and preserved without a new device or learning execution here. It extends the capture context; it does not close the legacy publisher criteria above.

| Consumer witness | Original result and boundary |
|---|---|
| J15 round 3 | 3,222 native comparisons, complete independent replay and three exact historical PNGs via uiValidationAsMedia; rejected experiments retain WARNING/REGRESSION |
| J15 learning V4 | 256 parameterized JUnit invocations plus one Node gate; 136,202 native comparisons, invocation-owned receipts and captured timing |
| publication attempt 01 | 2 GiB publication failed in FreeMarker StringWriter/AbstractStringBuilder before HTML |
| attempt 02 | acceptance initially rejected a compacted JSON preview; the revised oracle compares prefix tokens and full typed owner-local values |
| retained publication | 3 GiB, 144,325,590 HTML bytes after exact long-value deduplication within each checkpoint; no assertion or capture removal |
| data acceptance | incremental DOM checks cover owner/order/value/status/timing; no new interactive browser acceptance was recorded for this V4 artifact |
| runtime identity | physical Core C3 / Reporting J15; Reporting SHA-256 7B3C9DF7D22B4DD50EEFEB1E72358B4BAEC35CC3B24AC13D531BD1A101D3ADF0 |

The original archives are C:/work/whist_back/ai-training/20261102-round3/ and C:/work/whist_back/ai-training/20261102-171252-round4/. Their corpus-report-verification.properties, publication-attempt-01/02 and learning-heldout-extent-r2.html retain the detailed receipts. The agent reported file-URL policy rejection and did not bypass it; the artifact remains outside this review’s interactive acceptance.

This is a stress witness, not an automatic classification of every large report as a J15 defect. J4 owns adaptive selection/grouping and budgets; J0 owns any independently reproduced neutral publication limit. The current J16 delivery must not rewrite the recorded J15 execution or imply a fresh game run.

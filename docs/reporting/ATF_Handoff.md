# Handoff and acceptance of Android results

## Overview

The surrender shall publish the results in an external repository and maintain the test, republication and acceptance of an end separately. The identity of the original execution shall be kept at any subsequent publication.

## Publication flow

```mermaid
flowchart LR
    TRACE[Original trace] -->|link to the descriptor| DESC[Explicit descriptor]
    DESC -->|validates| CHECK[Integrity and purpose]
    CHECK -->|records imhumbly| ARCH[External registration]
    ARCH -->|update atomic| POINTER[Pointer pe scop]
```

<details>
<summary>Legend: publication and acceptance</summary>

A record may describe success, failure or refusal of execution. Acceptance requires the controls set out by purpose; the mere existence of an HTML or JUnit crossing does not by default update the acceptance.

</details>

## The contract of the descriptor

| Group | Responsibility |
|---|---|
| Identitate | Run ID, source of republication, package, scenario and purpose declarat/efectiv. |
| Implementation | Native verdict, captured beginning / end, temporal basis and limits. |
| Application and sources | Identity of the installed binary, sources and libraries used. |
| Content | HTML, manifest, checkpoints and associations of actions, observations and comparisons. |
| Review | The checks required for the purpose, related to the hash of the inspected content. |

Missing data remains explicitly marked. An estimated time, later reading identity or a interrupted distribution shall not become full original catches.

## Identity and time of measurement

The descriptor separates the data captured on execution from those checked on offline revision. [whist_ui_handoff.py](../../atf-test-android/tools/whist_ui_handoff.py), A JAR identified from the native rankings can have hash available only when revising; that hash does not become retrospective a measurement from execution.

- Keep the moment and source of each identity, including when the original information is missing.
- Correlate the interactive revision with the exact hash of the verified HTML.
- Treat acceptance of the republican descriptor and a new Android execution as separate-purpose records.

## External name spaces

| Contract | Meaning |
|---|---|
| runs and metadata | Immunobable records for each execution or republication. |
| latest-attempt pe scop | The latest recorded test, including failure. |
| latest-accepted pe scop | The most recent acceptance that meets the criteria of purpose. |
| public-trace | The comments allowed for the reply, distinct from the ex post data of the oracle. |
| indexes | Indices of convenience, without authority to declare acceptance. |

## Operational invariants

- The identification of a registration is immutable; identical re-importation is idempotent.
- Pointerele are ordered after execution, with one lock and atomic replacement.
- An old republican doesn't become a new execution by the date of the copy.
- The aim and package demarcate the comparable results.
- The data reserved for the oracle does not reach the Player's API.
- The external root shall be transmitted explicitly, independently of the traineeship director.
- Automatic indexation refuses checkout destinations and does not rewrite editorial READMEs.

## Compact handoff and exceptions

The target of surrender is an auditable HTML that includes mandatory information, full values and catches associated with each checkpoint. The files already incorporated are not only given final copies for navigation; a director or ZIP with the report's annexes is not autonomous.

| Content | Lifting policy |
|---|---|
| Mandatory data of the verdict | Body accessible in HTML with identity and association; a hash or file name does not replace content. |
| Identical copy incorporated | Delete only after the octet confrontation and the check of the seconded report. |
| Diagnosis or auxiliary result for separate purposes | Preserved for specific reasons, consumer or diagnostic question and identity; cannot hide any information necessary for HTML audit. |
| Distribution metadata | The index and integrity manifest remain compact, with verifiable role for navigation or governance; do not substitute the input, comparison or comments accessible in HTML. |
| Unitary proof still external | Preserved until full publication; dependency remains explicit in feedback. |

[Publicatorul Android](../../atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowReportPublisher.java) includes NGPs, manifests and text bodies provided or declared by checkpoints. [Migrated component suites](../testing/ATF_DisplayComponents.md#families-using-the-same-audit) includes inputs and comparisons captured; selections without adaptor keep the audit qualification distinct.

- **History: ** Native reports produced prior to this incorporation keep the annexes unique; the change of the publisher does not update them retrospectively.
- **Moment: ** hash and text body come from the same reading on publication; this moment remains distinct from the Android execution and subsequent revision.
- **Closure:** [the consolidation criterion](../feedback/ATF_Reporting.md#rpt-android-html-only---audit-delivery-without-auxiliary-copies) require the qualification of the inventory and associations of each manufacturer prior to the removal of single proof.

## Implementation and limits

| Implementare | Contract and limit |
|---|---|
| [whist_ui_handoff.py](../../atf-test-android/tools/whist_ui_handoff.py) | Implement offline republication with external descriptor and root. His type of record should not be used to claim a new Android execution. |
| [WhistExtentFeedback](../../atf-test-android/src/main/java/atf/android/whist/evidence/WhistExtentFeedback.java) | Publish the index of an audited run in the external space indexes. The Handoff root and index directory are configurable; the source directory is not a valid destination. |
| [Hand-over components](../../atf-test-android/tools/test_whist_ui_handoff.py) | Check the refusal of corrupt data and the preservation of acceptance to failure or old republican. The Java index indicates the last published audit; the purpose descriptor and the pointer shall determine the acceptance, including when the last test failed. |

[Publication architecture](README.md) · [Contractul Android](ATF_Android.md) · [criteria for handoff](../feedback/ATF_Reporting.md)

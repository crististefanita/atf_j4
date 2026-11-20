# Whist 0.22.1 audit

## Overview

The RAG plan selects component evidence from the source diff. It reruns the affected governance suite, reuses nine unchanged module groups with original provenance, and selects no Android E2E flow.

[Open the canonical ATF Extent report](component/atf-tests/component-report.html)

## Identity

| Item | Value |
|---|---|
| APK | WhistLab-0.22.1.apk |
| versionCode | 46 |
| Qualified source | `a58a1233c49de858378e6c03ef92cbcc1f56fc5b` |
| APK SHA-256 | `cf3510909002e7190ad0dfab3305cfced84bd7d435b1b4190f6b7ad9e727f740` |
| APK size | 1029553 bytes |
| Android native execution | NOT_RUN |

## Component suites

| Collection | Invocations | Selection | Report |
|---|---:|---|---|
| atf-validation | 19 | REUSE | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | REUSE | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 555 | RUN | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | REUSE | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | REUSE | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | REUSE | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | REUSE | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | REUSE | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | REUSE | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | REUSE | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1521** component/governance invocations; all selected executions passed. Reused groups retain their original execution receipts and source fingerprints.

## Android selection

E2E: **NOT_RUN**. The recomputed source diff contains release policy, qualification and documentation changes plus version metadata only; no game, screen, interaction or persistence contract changed. E2E is not represented as passed.

## Provenance and publication

- [Component execution and reuse receipt](component/source-component-run.json)
- [Original component snapshots](component/snapshots/)
- [APK build and identity checks](diagnostics/)
- Distribution and download receipts are added after a successful authenticated asset download.

[Release manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

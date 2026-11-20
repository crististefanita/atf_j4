# Whist 0.22 audit

## Overview

Components are qualified against identified sources, with canonical Extent reports and original receipts. The diff-derived selection requires no E2E for this maintenance edition.

[Open the canonical framework report](component/atf-tests/component-report.html)

## Identity

| Item | Value |
|---|---|
| APK | WhistLab-0.22.apk |
| versionCode | 45 |
| Qualified source | `df7d1954a0b4098f9678ed54acc911a57600adff` |
| Integrity | [Manifest](../release-validation.json) |

## Components

| Collection | Invocations | Verdict | Report |
|---|---:|---|---|
| atf-validation | 19 | PASS | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | PASS | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 549 | PASS | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | PASS | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | PASS | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | PASS | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | PASS | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | PASS | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | PASS | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | PASS | [Extent](component/whist-lab-app-rules/component-report.html) |

Total: **1515 unit/component invocations**. Supplemental source controls overlap the primary collection and do not add to this total.

## Android selection

E2E: **NOT_APPLICABLE**. The diff changes release validation, documentation and only the literal Android version values; game rules, interface and user flows are preserved.

## Provenance and reading

- Reports retain original execution inputs, comparisons and inline Base64 images.
- The [component receipt](component/source-component-run.json) and original snapshots describe selection and reuse.
- The [CI execution](diagnostics/cloud-qualification.json) identifies the fresh framework report.
- [APK identity](diagnostics/apk-identity.json) and [signing](diagnostics/signing-compatibility.json) are checked separately.
- [TWO-PART-RELEASE-AND-AUDIT](supplemental/test-change-controls/TWO-PART-RELEASE-AND-AUDIT/component-report.html)

[Manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

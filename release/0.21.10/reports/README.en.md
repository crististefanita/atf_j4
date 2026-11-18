# Whist 0.21.10 — evidence

## Overview

The reports bind observed assertions, component provenance and headless native captures to the signed APK. Native PNGs are inline in HTML. Reused suites retain their original execution and input hashes.

## Component suites

| Selection | Cases | Report |
|---|---:|---|
| atf-validation | 19 | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 468 | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 44 | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | [Extent](component/whist-lab-app-rules/component-report.html) |

## Native flows

| Flow | Report |
|---|---|
| history-release-sanity | [Extent](e2e/history-release-sanity/report.html) |
| responsive-ui-sanity | [Extent](e2e/responsive-ui-sanity/report.html) |
| signed-release-achievements | [Extent](e2e/signed-release-achievements/report.html) |
| signed-release-auto-game | [Extent](e2e/signed-release-auto-game/report.html) |
| signed-release-bidding | [Extent](e2e/signed-release-bidding/report.html) |
| signed-release-home-profile | [Extent](e2e/signed-release-home-profile/report.html) |
| signed-release-smoke | [Extent](e2e/signed-release-smoke/report.html) |

## Shared UI adoption

[Captured integration assertions](supplemental/extent-focused-admission.html) · [Rentz native observations](supplemental/extent-native-rentz.html) · [Binary equality](supplemental/library-distribution.json)

## Evidence admission

[Original-capture validator regressions](supplemental/extent-validator-correction.html) · [Correction provenance](supplemental/validator-correction.json)

[Manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

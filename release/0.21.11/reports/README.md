# Whist 0.21.11 — evidence

## Overview

Component reports retain original inputs, comparisons and reuse provenance. Seven headless Android flows identify the signed APK and embed their PNG captures inline.

## Component suites

| Selection | Cases | Report |
|---|---:|---|
| atf-validation | 19 | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 474 | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 45 | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
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

## Typography and library provenance

[Prior typography comparisons](supplemental/card-rank-parity.html) · [Admission on matching classes](supplemental/card-typography-admission.json) · [Library equality](supplemental/library-distribution.json)

The earlier glyph measurements are separate from new APK installation and UI acceptance. Their four tested face classes match the release build byte for byte.

## Execution profile and limits

Nine completed component groups were retained from their validated checkpoint; the transport group was rerun with a silent JVM scope argument. Android execution uses API 28 at the three declared portrait resolutions; other Android versions have separate qualification.

[Profile receipt](supplemental/component-runtime-profile.json) · [Manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

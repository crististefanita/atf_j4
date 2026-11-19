# Whist 0.21.12 — evidence

## Overview

Original inputs, comparisons and inline PNGs remain in each Extent suite. Component evidence and signed-APK flows retain their own source identities.

## Component suites

| Selection | Cases | Execution | Report |
|---|---:|---|---|
| atf-validation | 19 | Verified reuse | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | Verified reuse | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 519 | New execution | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | Verified reuse | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | Verified reuse | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | Verified reuse | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | Verified reuse | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | Verified reuse | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 51 | Verified reuse | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | Verified reuse | [Extent](component/whist-lab-app-rules/component-report.html) |

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

## Shared score adapter

[Web and package comparisons](supplemental/score-adapter/extent-browser.html) compare the compiled browser adapter and the delivered runtime JAR/AAR. Source component tests measure both languages, narrow and wide layouts, accessible text sizes, actions and scrollable score columns.

## Publication-note controls

The post-build validator revision is checked separately in [English](supplemental/note-controls/NOTE-SUMMARY-MAIN/component-report.html) and [Romanian](supplemental/note-controls/NOTE-SUMMARY-RO/component-report.html). These controls preserve the APK source identity and do not replace its primary component evidence.

## Provenance and limits

The framework collection has a complete accepted execution on an isolated Windows runner. Nine other collections retain their exact accepted reports, original execution receipts and source origins after input-scope validation. Rejected timeout attempts remain separate diagnostics and contribute no passing evidence.

The APK build was reused only across two editorial control files; [build admission](diagnostics/build-reuse.json) identifies the original build and runtime fingerprints. Seven headless native flows run on the signed APK, API 28 and the declared portrait profiles; broader Android/API and same-device Chrome acceptance remain separately tracked.

[Manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

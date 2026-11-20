# Whist 0.21.13 — evidence

## Overview

The component reports retain captured inputs, actual comparisons and inline PNGs. Reused suites preserve their original execution receipts and source identities. Signed-APK flows are qualified separately.

## Component suites

| Selection | Cases | Execution | Report |
|---|---:|---|---|
| atf-validation | 19 | Verified reuse | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | Verified reuse | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 528 | New execution on isolated CI | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | Verified reuse | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | Verified reuse | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | Verified reuse | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | Verified reuse | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | Verified reuse | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | Verified reuse | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
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

## Public Web

[Published Web comparisons](supplemental/public-web/extent-browser.html) contain RO/EN home captures at 414×896 in headless Edge with mobile emulation. Same-device Android Chrome acceptance remains separately tracked.

## Source-control proofs

- [NATIVE-WEB-ADMISSION-COLLECTOR](supplemental/test-change-controls/NATIVE-WEB-ADMISSION-COLLECTOR/component-report.html)
- [SCORE-TYPOGRAPHY-MAIN](supplemental/test-change-controls/SCORE-TYPOGRAPHY-MAIN/component-report.html)
- [SHARED-MENU-AUDIT-BASE-MAIN](supplemental/test-change-controls/SHARED-MENU-AUDIT-BASE-MAIN/component-report.html)

## Provenance and limits

The [component receipt](component/source-component-run.json) records the complete invocation census, stability check and reuse decisions. The framework collection has a fresh [isolated CI execution](diagnostics/cloud-qualification.json); nine other collections retain their original passing reports after scope validation. Rejected timeout attempts remain separate diagnostics and contribute no passing result.

[Final-source build admission](diagnostics/final-source-build-reuse.json) confirms a real rebuild with identical Android binary bytes after validator and documentation updates. Seven headless native flows ran on that signed APK. Their original execution source and receipts remain unchanged; the current validator records Web-only admission separately. Broader Android/API and same-device Chrome acceptance remain separate qualifications.

[Manifest](../release-validation.json) · [Release catalog](../../README.md) · [Test strategy](../../../docs/testing/README.md)

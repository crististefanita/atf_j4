# Whist 0.21.11 — dovezi

## Pe scurt

Rapoartele componentelor păstrează intrările, comparațiile și proveniența reutilizării. Șapte fluxuri Android headless identifică APK-ul semnat și includ capturile PNG direct în HTML.

## Suite de componentă

| Selecție | Cazuri | Raport |
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

## Fluxuri native

| Flux | Raport |
|---|---|
| history-release-sanity | [Extent](e2e/history-release-sanity/report.html) |
| responsive-ui-sanity | [Extent](e2e/responsive-ui-sanity/report.html) |
| signed-release-achievements | [Extent](e2e/signed-release-achievements/report.html) |
| signed-release-auto-game | [Extent](e2e/signed-release-auto-game/report.html) |
| signed-release-bidding | [Extent](e2e/signed-release-bidding/report.html) |
| signed-release-home-profile | [Extent](e2e/signed-release-home-profile/report.html) |
| signed-release-smoke | [Extent](e2e/signed-release-smoke/report.html) |

## Tipografie și proveniența bibliotecilor

[Comparațiile anterioare de tipografie](supplemental/card-rank-parity.html) · [Admiterea pe clase identice](supplemental/card-typography-admission.json) · [Egalitatea bibliotecilor](supplemental/library-distribution.json)

Măsurătorile anterioare ale glifelor sunt separate de instalarea și acceptanța UI ale noului APK. Cele patru clase grafice testate coincid byte cu byte cu buildul ediției.

## Profilul execuției și limite

Nouă grupuri de componente finalizate au fost păstrate din checkpoint-ul validat; grupul de transport a fost reluat cu argumentul JVM de domeniu, fără text suplimentar în fluxuri. Execuția Android folosește API 28 la cele trei rezoluții portret declarate; alte versiuni Android au calificare separată.

[Chitanța profilului](supplemental/component-runtime-profile.json) · [Manifest](../release-validation.json) · [Catalogul versiunilor](../../README.md) · [Strategia de testare](../../../docs/testing/README.md)

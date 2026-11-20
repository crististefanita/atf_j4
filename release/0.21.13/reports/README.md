# Whist 0.21.13 — dovezi

## Pe scurt

Rapoartele de componentă păstrează intrările capturate, comparațiile reale și imaginile PNG inline. Suitele reutilizate păstrează receipt-urile originale și identitatea surselor. Fluxurile APK-ului semnat sunt calificate separat.

## Suite de componentă

| Selecție | Cazuri | Execuție | Raport |
|---|---:|---|---|
| atf-validation | 19 | Reutilizare verificată | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | Reutilizare verificată | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 528 | Execuție nouă pe CI izolat | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-achievements | 28 | Reutilizare verificată | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-bidding | 255 | Reutilizare verificată | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | Reutilizare verificată | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | Reutilizare verificată | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | Reutilizare verificată | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-atf-card-table-ui | 56 | Reutilizare verificată | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
| whist-lab-app-rules | 40 | Reutilizare verificată | [Extent](component/whist-lab-app-rules/component-report.html) |

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

## Web public

[Comparațiile Web publicate](supplemental/public-web/extent-browser.html) conțin capturi ale meniului RO/EN la 414×896 în Edge headless, cu emulare mobilă. Acceptanța Chrome pe același Android rămâne urmărită separat.

## Probe pentru controalele sursei

- [NATIVE-WEB-ADMISSION-COLLECTOR](supplemental/test-change-controls/NATIVE-WEB-ADMISSION-COLLECTOR/component-report.html)
- [SCORE-TYPOGRAPHY-MAIN](supplemental/test-change-controls/SCORE-TYPOGRAPHY-MAIN/component-report.html)
- [SHARED-MENU-AUDIT-BASE-MAIN](supplemental/test-change-controls/SHARED-MENU-AUDIT-BASE-MAIN/component-report.html)

## Proveniență și limite

[Receipt-ul componentelor](component/source-component-run.json) păstrează inventarul complet al invocărilor, stabilitatea și deciziile de reutilizare. Colecția cadrului are o [execuție CI izolată](diagnostics/cloud-qualification.json) nouă; celelalte nouă colecții păstrează rapoartele originale trecute după validarea perimetrului. Încercările respinse prin timeout rămân diagnostice separate și nu contribuie la rezultate trecute.

[Admiterea build-ului final](diagnostics/final-source-build-reuse.json) confirmă reconstruirea reală și octeții Android identici după actualizarea validatorului și a documentației. Șapte fluxuri native headless au rulat pe acel APK semnat. Sursele și receipt-urile execuțiilor originale rămân neschimbate; validatorul curent înregistrează separat admiterea schimbărilor exclusiv Web. Alte API-uri Android și acceptanța Chrome pe același dispozitiv rămân calificări separate.

[Manifest](../release-validation.json) · [Catalogul livrărilor](../../README.md) · [Strategia testării](../../../docs/testing/README.md)

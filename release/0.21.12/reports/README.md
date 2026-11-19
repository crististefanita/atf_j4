# Whist 0.21.12 — dovezi

## Pe scurt

Intrările originale, comparațiile și imaginile PNG inline rămân în fiecare suită Extent. Dovezile de componentă și fluxurile APK-ului semnat își păstrează identitățile surselor.

## Suite de componentă

| Selecție | Cazuri | Execuție | Raport |
|---|---:|---|---|
| atf-validation | 19 | Reutilizare verificată | [Extent](component/atf-validation/component-report.html) |
| atf-test-android | 277 | Reutilizare verificată | [Extent](component/atf-test-android/component-report.html) |
| atf-tests | 519 | Execuție nouă | [Extent](component/atf-tests/component-report.html) |
| whist-lab-app-bidding | 255 | Reutilizare verificată | [Extent](component/whist-lab-app-bidding/component-report.html) |
| whist-lab-app-commands | 123 | Reutilizare verificată | [Extent](component/whist-lab-app-commands/component-report.html) |
| whist-lab-app-display | 107 | Reutilizare verificată | [Extent](component/whist-lab-app-display/component-report.html) |
| whist-lab-app-history | 61 | Reutilizare verificată | [Extent](component/whist-lab-app-history/component-report.html) |
| whist-lab-app-achievements | 28 | Reutilizare verificată | [Extent](component/whist-lab-app-achievements/component-report.html) |
| whist-lab-app-atf-card-table-ui | 51 | Reutilizare verificată | [Extent](component/whist-lab-app-atf-card-table-ui/component-report.html) |
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

## Adaptorul comun al scorului

[Comparațiile Web și ale pachetelor](supplemental/score-adapter/extent-browser.html) verifică adaptorul browser compilat și pachetele JAR/AAR livrate. Testele de componentă măsoară ambele limbi, ferestre înguste și largi, text mărit, acțiuni și coloane de scor derulabile.

## Controalele notelor de publicare

Revizia validatorului de după build este verificată separat în [engleză](supplemental/note-controls/NOTE-SUMMARY-MAIN/component-report.html) și [română](supplemental/note-controls/NOTE-SUMMARY-RO/component-report.html). Aceste controale păstrează identitatea sursei APK-ului și nu înlocuiesc dovezile sale primare de componentă.

## Proveniență și limite

Colecția framework are o execuție completă acceptată pe un runner Windows izolat. Alte nouă colecții păstrează rapoartele acceptate, receipt-urile originale și proveniența surselor după validarea perimetrului intrărilor. Încercările respinse prin timeout rămân diagnostice separate și nu contribuie dovezi de succes.

Buildul APK a fost reutilizat numai peste două fișiere editoriale de control; [admiterea buildului](diagnostics/build-reuse.json) identifică buildul original și amprentele runtime. Șapte fluxuri native headless rulează pe APK-ul semnat, API 28 și profilurile portret declarate; acceptanța Android/API mai largă și Chrome pe același dispozitiv rămân urmărite separat.

[Manifest](../release-validation.json) · [Catalogul livrărilor](../../README.md) · [Strategia de testare](../../../docs/testing/README.md)

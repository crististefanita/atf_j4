# Suite de componente 0.19.1

## Pe scurt

Fiecare selecție are un singur raport Extent HTML, un receipt și aceeași amprentă de sursă.

| Grup | Raport canonic | Receipt |
|---|---|---|
| atf-validation | [Extent](atf-validation/component-report.html) | [Execuție](atf-validation/execution.json) |
| atf-test-android | [Extent](atf-test-android/component-report.html) | [Execuție](atf-test-android/execution.json) |
| atf-tests | [Extent](atf-tests/component-report.html) | [Execuție](atf-tests/execution.json) |
| whist-lab-app-achievements | [Extent](whist-lab-app-achievements/component-report.html) | [Execuție](whist-lab-app-achievements/execution.json) |
| whist-lab-app-bidding | [Extent](whist-lab-app-bidding/component-report.html) | [Execuție](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | [Extent](whist-lab-app-commands/component-report.html) | [Execuție](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | [Extent](whist-lab-app-display/component-report.html) | [Execuție](whist-lab-app-display/execution.json) |
| whist-lab-app-history | [Extent](whist-lab-app-history/component-report.html) | [Execuție](whist-lab-app-history/execution.json) |

## Legarea dovezilor

`source-inputs.json`, `source-provenance.json` și `source-component-run.json` leagă raportările de snapshot-ul aceleiași surse. Capturile sunt încorporate în Extent; XML-urile JUnit și imaginile intermediare nu sunt duplicate în pachet.

E2E nu a fost rulat; această ediție este calificată prin selecțiile de componente.
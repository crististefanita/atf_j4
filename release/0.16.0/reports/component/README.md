# Rapoartele componentelor pe sursa fixată

## Pe scurt

Cele șapte selecții primare Maven și Gradle au trecut pe același snapshot ca APK-ul: 1.099 teste, zero eșecuri, erori sau ignorări. Aceste rezultate califică sursa; E2E-ul APK-ului instalat este păstrat separat.

| Selecție | Rezultat | Raport | Execuție |
|---|---:|---|---|
| atf-validation | 19/19 | [Extent](atf-validation/component-report.html) | [Receipt](atf-validation/execution.json) |
| atf-test-android | 257/257 | [Extent](atf-test-android/component-report.html) | [Receipt](atf-test-android/execution.json) |
| atf-tests | 316/316 | [Extent](atf-tests/component-report.html) | [Receipt](atf-tests/execution.json) |
| whist-lab-app-bidding | 248/248 | [Extent](whist-lab-app-bidding/component-report.html) | [Receipt](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | 112/112 | [Extent](whist-lab-app-commands/component-report.html) | [Receipt](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | 86/86 | [Extent](whist-lab-app-display/component-report.html) | [Receipt](whist-lab-app-display/execution.json) |
| whist-lab-app-history | 61/61 | [Extent](whist-lab-app-history/component-report.html) | [Receipt](whist-lab-app-history/execution.json) |

source-inputs.json, source-provenance.json și source-component-run.json leagă rapoartele de sursa calificată, de bibliotecile ATF și de execuția agregată. XML-ul JUnit și fișierele intermediare de captură nu sunt duplicate aici. E2E-ul APK-ului instalat este în [raportul AutoGame](../e2e/signed-release-auto-game/report.html) și [receipt](../e2e/signed-release-auto-game/execution.json).

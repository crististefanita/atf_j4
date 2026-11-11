# Rapoartele componentelor pe sursa fixată

## Pe scurt

Această execuție verifică suitele primare Maven și colecțiile Gradle declarate de catalogul aplicației. Nu califică un APK și nu folosește un dispozitiv; fiecare selecție are Extent și receipt proprii.

| Selecție | Raport | Metadate |
|---|---|---|
| atf-validation | [Extent](atf-validation/component-report.html) | [Execuție](atf-validation/execution.json) |
| atf-test-android | [Extent](atf-test-android/component-report.html) | [Execuție](atf-test-android/execution.json) |
| atf-tests | [Extent](atf-tests/component-report.html) | [Execuție](atf-tests/execution.json) |
| whist-lab-app-achievements | [Extent](whist-lab-app-achievements/component-report.html) | [Execuție](whist-lab-app-achievements/execution.json) |
| whist-lab-app-bidding | [Extent](whist-lab-app-bidding/component-report.html) | [Execuție](whist-lab-app-bidding/execution.json) |
| whist-lab-app-commands | [Extent](whist-lab-app-commands/component-report.html) | [Execuție](whist-lab-app-commands/execution.json) |
| whist-lab-app-display | [Extent](whist-lab-app-display/component-report.html) | [Execuție](whist-lab-app-display/execution.json) |
| whist-lab-app-history | [Extent](whist-lab-app-history/component-report.html) | [Execuție](whist-lab-app-history/execution.json) |

`source-inputs.json`, `source-provenance.json` și `source-component-run.json` leagă toate selecțiile de aceeași amprentă și de receipt-ul nativ de proveniență. XML-ul JUnit rămâne temporar pentru confruntare; nu se păstrează ca raport duplicat în dosarul final. E2E rămâne separat și neexecutat.

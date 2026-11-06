# Testele adaptorului și ale aplicației Android

## Pe scurt

Acest catalog explică intrările JUnit ale adaptorului Android. [Strategia comună](../../../docs/testing/README.md) deține alegerea nivelului și acceptarea rezultatului; testul deține comparația, iar infrastructura deține comanda și observația.

## Selecții și resurse

| Intrare | Scop și precondiții |
|---|---|
| [AndroidComponentSuite](java/atf/android/suites/AndroidComponentSuite.java) | Suită primară: toate componentele Maven ale adaptorului, fără emulator. |
| [AndroidDisplayComponentSuite](java/atf/android/suites/AndroidDisplayComponentSuite.java) | Profile, driver, istoric și oracole geometrice cu intrări controlate. |
| [AndroidHistoryComponentSuite](java/atf/android/suites/AndroidHistoryComponentSuite.java) | Selecție focalizată a istoricului; împărtășește clase cu selecția display. |
| [AndroidUiFlowTimingComponentSuite](java/atf/android/e2e/support/AndroidUiFlowTimingComponentSuite.java) | Intervalele măsurate ale fluxului, cu ceas și procese controlate. |
| [AndroidGameplayE2eSuite](java/atf/android/suites/AndroidGameplayE2eSuite.java) | Interacțiune pe APK-ul identificat; necesită activare, dispozitiv rezervat și restaurarea stării. |

[Catalogul](../../../config/atf-test-suites.json) declară suitele primare pentru fiecare nivel și părinții selecțiilor focalizate. Display, istoric și timing sunt submulțimi ale suitei primare de componentă; gameplay este submulțime a intrării E2E, care rămâne activată separat.

## Fluxul verificării

```mermaid
flowchart LR
    SELECT[Selecție declarată] -->|controlează| INPUT[Proces sau stare]
    INPUT -->|produce| OBS[Observație]
    EXPECT[Așteptare independentă] -->|confruntă| OBS
    OBS -->|alimentează| REPORT[Raportul selecției]
```

<details>
<summary>Legendă: o singură execuție și două tipuri de mediu</summary>

Componentele controlează procesele, ceasul și observațiile. E2E capturează o interacțiune nouă pe dispozitiv; republicarea unei observații existente păstrează identitatea originală și nu execută din nou Android.

</details>

## Execuție și interpretare

Folosiți [intrările izolate ale modulului](../../README.md#verificare-fără-dispozitiv) și [ghidul componentelor](../../../docs/testing/ATF_DisplayComponents.md). Selectați suita sau clasele sale, cu suprapunerea declarată; directoarele temporare și rapoartele aparțin rulării externe.

- **Audit:** [AndroidComponentAudit](java/atf/android/component/AndroidComponentAudit.java) leagă invocarea de Kit. [Contractele adaptorului](java/atf/android/component/AndroidComponentReportContractTest.java) verifică publicarea și refuzurile; eșecurile intenționate sunt delimitate de rezultatul produsului.

- **Rezultat:** JUnit identifică execuția, iar HTML-ul auditat păstrează inputul, expected/actual și verdictul. [Criteriul agregării](../../../docs/feedback/ATF_Reporting.md#rpt-android-component-html--comparații-capturate-nu-doar-junit-xml) rămâne distinct de trecerea JUnit.

[Modulul Android](../../README.md) · [Strategia comună](../../../docs/testing/README.md) · [Auditul rapoartelor](../../../docs/reporting/ATF_Audit.md)

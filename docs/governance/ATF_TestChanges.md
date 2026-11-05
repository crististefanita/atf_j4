# Guvernanța schimbărilor în teste

O modificare a testelor păstrează sensul contractului și motivul pentru care o intrare este admisă sau refuzată. Schimbarea implementării nu justifică ajustarea rezultatului așteptat doar pentru a obține succes.

## Criterii

| Suprafață | Contract |
|---|---|
| Expected | Derivat din cerință, invariant sau oracol independent. |
| Control negativ | Eșecul intenționat demonstrează că testul detectează defectul vizat. |
| Identitate | Cazul, sursele, configurația și domeniul sunt explicite. |
| Verdict | Cauza originală se păstrează prin captură și publicare. |
| Populație | Selecția și omisiunile sunt declarate fără numărători istorice fixate în ghid. |
| Rezultate | Rulările concrete și comparațiile de versiuni se păstrează în arhiva externă. |

## Revizia unei schimbări

1. Formulați proprietatea generală și frontierele ei.
2. Reproduceți defectul într-un caz mic cu date controlate.
3. Verificați eșecul controlului negativ și succesul cazului valid.
4. Reconciliați sursele concurente înaintea calificării.
5. Actualizați colecția, contractul și documentația mecanismului în aceeași revizie.

[Guvernanța documentației](ATF_Documentation.md) · [Colecții](../testing/ATF_Collections.md)

<details>
<summary>Control activ: acceptanța nativă a acțiunilor și a istoricului</summary>

```yaml
testChanges:
  - id: PORTRAIT-ACTION-REGIONS-AND-HISTORY
    status: VERIFIED
    base: 5ae98e8cc1863c7513f0d7b8396087f19c0bc6cb
    expires: 2026-12-05
    reason: Distanța se măsoară față de chenar și acțiunea lui observată, păstrând refuzul suprapunerilor; adaugă dimensiuni exacte, localizare, actori pregătiți legal și accesul la toate rândurile și coloanele.
    proof: 'gradlew.bat :app:testDebugUnitTest -Pwhist.displayComponents=true; mvn -Dtest=AndroidDisplayComponentSuite,RecordsRunContractComponentTest,RecordsFixturesComponentTest,AndroidUiFlowReportPublisherTest test; mvn -Dtest=WhistRecordsE2eTest#historyPresentationAcrossDeclaredPortraitProfiles -Datf.android.enabled=true test'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/SyntheticTableHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/TableViewportObservationComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveMenuChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveEvidence.java
      - atf-test-android/src/test/java/atf/android/e2e/support/AndroidUiFlowTrace.java
      - atf-test-android/src/test/java/atf/android/e2e/records/WhistRecordsE2eTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixtures.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixturesComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsContext.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsRunContract.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsRunContractComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/NameHistoryScenarios.java
```

Un reper simulat arată cum ignorarea acțiunii inversează comparația distanțelor pe aceleași coordonate. Controalele negative păstrează erorile de text, actor, dimensiune și identitate a rândului; rularea nativă cere metadatele instalării, rezervarea și arhiva privată definite în [contractul Android](../testing/ATF_Android.md#profil-istoric-clasamente-și-serii).

</details>

<details>
<summary>Control activ: proporțiile mesei și acțiunea deasupra numelui</summary>

```yaml
testChanges:
  - id: DISPLAY-PROPORTIONS-ACTION-AND-HISTORY
    status: VERIFIED
    base: 8933374e33ab6eee9cb4217ed1a6112aebacc9b3
    expires: 2026-12-05
    reason: Aplică cerința de reducere a înălțimii fețelor jucate și lățimii atuului, mărire a chenarelor și separare completă a acțiunii de nume; păstrează geometria fără suprapuneri și identitatea cronologică a scorului.
    proof: 'gradlew.bat :app:testDebugUnitTest -Pwhist.displayComponents=true --offline --no-daemon; pwsh -NoProfile -File scripts/run-whist-responsive-sanity.ps1 -ComponentsOnly'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrickCardMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TableViewportPolicyTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/localization/WhistUiTextTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/seat/PlayerSeatMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfile.java
      - atf-test-android/src/test/java/atf/android/e2e/display/profile/ResponsiveDisplayProfileComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/support/TableViewportObservation.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/SyntheticTableHierarchy.java
      - atf-test-android/src/test/java/atf/android/e2e/display/component/TableViewportObservationComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveTableState.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveMenuChecks.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerTableScenario.java
```

Oracolul dimensiunilor urmează cerința de prezentare, iar controalele negative refuză înălțimea depășită, acțiunea lipsă, textul prescurtat și suprapunerea peste nume. Verificarea nativă rămâne distinctă de observația simulată și de mecanismul JVM.

</details>

## Mutarea proprietății unei verificări

Transferul unei implementări către o bibliotecă păstrează toate contractele native la producător. Consumatorul păstrează acceptanța API-ului folosit, identitatea JAR-ului încărcat și refuzurile pentru metadate ori livrări inconsistente.

- Cererea de schimbare identifică sursele afectate, proprietarul nou, înlocuirea echivalentă și criteriile independente de acceptare.
- Inventarele concrete, bazele Git, hash-urile și rezultatele transferului sunt înregistrări externe, legate de livrarea verificată.
- Ștergerea copiei locale nu suprimă un caz negativ și nu transformă o verificare de bibliotecă într-o acceptanță Android.
- Documentația curentă descrie limita de responsabilitate, fără liste istorice de teste șterse.

[Guvernanța calității](README.md) verifică perimetrul curent și interzice sursele paralele ale bibliotecilor binare.


<details>
<summary><strong>Ghiduri ale bibliotecilor — index scurt și ghiduri per bibliotecă</strong></summary>

```yaml
testChanges:
  - id: LIBRARY-USAGE-GUIDES-J4
    status: VERIFIED
    base: 52270cdbff4a7234c2cc6581c910d3431105f76e
    expires: 2026-11-30
    reason: Keep the exact flat binary inventory and require only the index plus four named usage guides; retain independent missing, directory and extra-file negatives, physical CodeSource and source/feedback ownership.
    proof: 'mvn -B -ntp -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest,LibraryDocumentationExampleTest,DocumentationPolicyGovernanceTest#libraryMaintenanceFooterDoesNotExemptBodyOtherOwnersOrOtherFiles+libraryUsageGuidesAreGovernedAndKeepSourceOwnerOnlyInMaintenanceFooter test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/GovernanceProject.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryContractTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryIntegrityTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibrarySurface.java
```

Exemplele sunt extrase din README-uri și verificate pe JAR-urile fizice. Schimbarea păstrează perechile publicate și oracolele existente.

</details>

<details>
<summary>Control activ: adresa tehnică a descărcării publice</summary>

```yaml
testChanges:
  - id: DOCUMENTATION-PUBLIC-RELEASE-URI
    status: VERIFIED
    base: cd4e62e4fd89bb5134f874aee1912aa086a5bda2
    expires: 2026-12-05
    reason: Distinge destinația tehnică a linkului stabil al propriului repository de proza vizibilă; păstrează refuzul identificatorilor din etichete, alte proiecte, căi de sursă și URL-uri modificate.
    proof: 'mvn -B -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - config/atf-governance-policy.json
```

Reproducerea este refuzul linkului public valid ca referință editorială; controalele negative păstrează etichetele, alte proiecte și variantele necanonice în perimetrul verificat. Contractul [documentației](ATF_Documentation.md#conținut-stabil-și-date-variabile) definește limita, fără excepții generale pentru URL-uri.

</details>

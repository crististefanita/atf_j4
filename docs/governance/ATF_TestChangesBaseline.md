# Registrul controalelor verificate ale testelor

## Pe scurt

Acest registru păstrează controalele tehnice verificate, cu baza Git și căile lor exacte. [Ghidul schimbărilor de teste](ATF_TestChanges.md) stabilește regulile comune și separă cererea activă de înregistrarea verificată.

## Controale verificate

<details>
<summary>Control verificat: acceptanța nativă a acțiunilor și a istoricului</summary>

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
<summary>Control verificat: proporțiile mesei și acțiunea deasupra numelui</summary>

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

<details>
<summary>Control verificat: măsurarea istoricului și invariantele registrului</summary>

```yaml
testChanges:
  - id: HISTORY-MEASUREMENT-AND-LEDGER-INVARIANTS
    status: VERIFIED
    base: 180ea08cb639fd09291d191278e18fa93ae6db96
    expires: 2026-12-05
    reason: Extrage agregarea măsurătorilor din vederea Android și adaugă refuzuri pentru locuri invalide, pierderea levatelor și dimensiuni imposibile; păstrează sufixele necunoscute, imutabilitatea și măsurarea ultimei celule.
    proof: 'gradlew.bat :game-core:test --tests ro.whistlab.core.session.ScoreHistoryInvariantTest --offline; gradlew.bat :app:testDebugUnitTest --tests ro.whistlab.app.score.ScoreHistory*Test --offline'
    paths:
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/ScoreHistoryInvariantTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryMeasureComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
```

Regresiile se confruntă cu sursa anterioară înaintea corecției. Furnizorii de font sunt injectați în componente; calificarea rasterului și a fontului Android rămâne separată. Un diagnostic de classpath nu este reclasificat ca defect al produsului.

</details>

<details>
<summary>Control verificat: istoric compact și rezultatul explicit al contractului</summary>

```yaml
testChanges:
  - id: COMPACT-HISTORY-EXPLICIT-CONTRACT-OUTCOME
    status: VERIFIED
    base: 6e8f3f38e94f0513597b197bc8fa60ce73ee371b
    expires: 2026-12-05
    reason: Aplică geometria compactă agreată și schema cu levate explicite; păstrează prefixul necunoscut, comparațiile independente de scor, conservarea levatelor și verificarea celulelor complete în ambele limbi.
    proof: 'gradlew.bat :app:testDebugUnitTest :app:assembleDebug -Pwhist.displayComponents=true --offline --no-daemon; mvn -Dtest=RecordsFixturesComponentTest,RecordsRunContractComponentTest,AndroidUiFlowReportPublisherTest test; mvn -Dtest=WhistRecordsE2eTest#compactHistoryAndSlashedContractsRemainReadable -Datf.android.enabled=true test'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryGridMetricsComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryTableComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/score/ScoreHistoryOutcomeComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixtures.java
      - atf-test-android/src/test/java/atf/android/e2e/records/RecordsFixturesComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/records/NameHistoryScenarios.java
      - atf-test-android/src/test/java/atf/android/e2e/records/WhistRecordsE2eTest.java
```

Zero, ratarea cu puncte pozitive, datele necunoscute și o mână neîncheiată au rezultate distincte. Controalele negative refuză levatele fracționare, negative, neconservate sau contradictorii, iar măsurătorile injectate nu înlocuiesc randarea nativă.

</details>

<details>
<summary>Control verificat: indicator de atu comun și depunere discretă</summary>

```yaml
testChanges:
  - id: UNIFORM-TRUMP-AND-COMPACT-BID-SUBMISSION
    status: VERIFIED
    base: e183df449ad54d4281925f905ce3a65cf4b6cdf3
    expires: 2026-12-05
    reason: Elimină rândul redundant despre cartea neîntoarsă și folosește aceeași ramă cu două regiuni în toate modurile; păstrează valoarea vizibilă, identitatea accesibilă și ținta tactilă. Sincronizează dimensiunile mâinii și indicii locurilor cu politica de afișaj modificată în paralel.
    proof: 'gradlew.bat :app:testDebugUnitTest :app:assembleDebug -Pwhist.displayComponents=true --offline --no-daemon; pwsh -NoProfile -File scripts/run-whist-responsive-sanity.ps1 -ComponentsOnly'
    paths:
      - whist-lab-app/app/src/test/java/ro/whistlab/app/localization/WhistUiTextTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/TrumpIndicatorComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/table/GameTableLayoutComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/ThirteenCardBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/SixPlayerBiddingCheck.java
      - atf-test-android/src/test/java/atf/android/e2e/gameplay/presentation/fan/FanSetupPhase.java
      - atf-test-android/src/test/java/atf/android/e2e/display/flow/responsive/ResponsiveGameFlow.java
```

Controalele păstrează cartea sau culoarea, lizibilitatea, localizarea și descrierea accesibilă; absența textului redundant nu înlocuiește prezența valorii. Măsurătorile de font injectate califică politica comună, iar verificarea nativă compară aceeași geometrie în stări cu și fără atu.

</details>

<details>
<summary><strong>Ghiduri ale bibliotecilor — index scurt și ghiduri per bibliotecă</strong></summary>

```yaml
testChanges:
  - id: LIBRARY-USAGE-GUIDES
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
<summary>Control verificat: adresa tehnică a descărcării publice</summary>

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

<details>
<summary>Control verificat: pagina versiunii declarate</summary>

```yaml
testChanges:
  - id: DOCUMENTATION-DECLARED-RELEASE-PAGE
    status: VERIFIED
    base: a5eebec1805b760c876f03d2439da8b73d8e3cec
    expires: 2026-12-06
    reason: Recunoaște strict pagina tag-ului livrării declarate, împreună cu APK-ul și checksum-ul; păstrează verificarea prozei vizibile, a altor versiuni și a adreselor modificate.
    proof: 'mvn -B -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/main/java/atf/tests/distribution/ApkDownloadTarget.java
```

[Identitatea livrării](../platform/ATF_AndroidDelivery.md) stabilește versiunea; [politica documentară](ATF_Documentation.md#conținut-stabil-și-date-variabile) clasifică numai destinațiile canonice. Probele refuză pagina nedeclarată, altă versiune, alt repository, imaginea și URL-urile cu modificări; eticheta vizibilă nu primește excepție.

</details>

<details>
<summary>Control verificat: proprietatea și documentația bibliotecilor</summary>

```yaml
testChanges:
  - id: LIBRARY-OWNERSHIP-README
    status: VERIFIED
    base: d46948264e7b35347ad812be1e67e9fba30eef05
    expires: 2026-11-30
    reason: Require exactly four flat JAR/POM pairs plus the dedicated README and common producer ownership of library source and feedback; preserve physical binary identity, independent negative oracles and consumer acceptance.
    proof: 'mvn -B -ntp -pl atf-tests -am -Dtest=PlatformLibraryContractTest,PlatformLibraryIntegrityTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryContractTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibraryIntegrityTest.java
      - atf-tests/src/test/java/atf/libraries/PlatformLibrarySurface.java
```

Controalele negative resping README-ul absent, documentele suplimentare, un director în locul README-ului și atribuirea codului ori feedback-ului unui consumer. Bibliotecile distribuite își păstrează coordonatele și hash-urile.

Inventarul de la baza Git declarată este cel al incrementului verificat. Perechile și ghidurile curente sunt definite în [contractul lib](../../lib/README.md).

</details>

<details>
<summary>Control verificat: autorizarea testelor pe diff-ul Git</summary>

```yaml
testChanges:
  - id: GOVERNANCE-GIT-TEST-CHANGES
    status: VERIFIED
    base: 08706c3a1a3fe0172b706da2c69e9f767fde54bd
    expires: 2026-12-19
    reason: Leagă autorizarea de commitul revizuit, căile exacte și expirarea, cu probe negative pe depozite Git reale.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeReviewComponentTest,TestChangeGovernanceTest,DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/main/java/atf/tests/testchange/GitTestChangeSnapshot.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeControls.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGitFixture.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-atf-governance.ps1
```

</details>

<details>
<summary>Control verificat pentru trasabilitatea probei de schimbare</summary>

```yaml
testChanges:
  - id: GOVERNANCE-TEST-CHANGE-PROOF
    status: VERIFIED
    base: 7a28a0978e6b50c1f9dd85cbad40544aecaea06d
    expires: 2026-12-19
    reason: Leagă proba offline de control, baza revizuită, commit și conținutul diff-ului și cere un control negativ executat.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeProofComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofMatchesExactGitReviewAndDeclaredCases
      negative:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofFromAnotherCommitCannotBeReused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofWithoutPassingNegativeControlIsRefused
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofRequiresTheSameIdentityAndReportBytes
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#verifiedControlNeedsProofAndCleanCommittedInputs
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/GitTestChangeSnapshot.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeControls.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeProofChecks.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeProof.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeProofComponentTest.java
      - scripts/run-component-suites.ps1
```

</details>

<details>
<summary>Control verificat pentru fațada sesiunii și restaurare</summary>

```yaml
testChanges:
  - id: GAME-SESSION-BOUNDARY
    status: VERIFIED
    base: 48a94740184e39bc23de975c3d133bd132620492
    expires: 2026-12-19
    reason: Confruntă mutările, scorul și reluarea prin fațada publică folosind un oracol JVM și verifică refuzul fără mutație.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GameSessionBoundaryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.game.GameSessionBoundaryComponentTest#resumesBiddingPlayingAndScoreAndAdvancesTheSameMatch
      negative:
        - atf.tests.governance.source.game.GameSessionBoundaryComponentTest#illegalCommandsAndCorruptCheckpointsAreRefused
    paths:
      - atf-tests/src/test/java/atf/tests/governance/source/game/GameSessionBoundaryComponentTest.java
      - atf-tests/src/test/resources/session/GameSessionProbe.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/CheckpointHistoryAuditTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/session/MatchSummaryTest.java
```

</details>

<details>
<summary>Control verificat pentru bugetul loturilor Gradle</summary>

```yaml
testChanges:
  - id: CORE-GRADLE-BATCH-BUDGET
    status: VERIFIED
    base: 2b16b7c6b5c0909b59d924189a938683345087a1
    expires: 2026-12-19
    reason: Fixează bugetul comun și separarea loturilor de strategie, cu refuz pentru suprascrierea heap-ului și oprirea la primul caz.
    proof: 'mvn -o -pl atf-tests -am -Dtest=GradleAuditGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#coreBatchesHaveOneDeclaredBudgetAndSeparatePolicyExperiments
      negative:
        - atf.tests.governance.source.gradle.GradleAuditGovernanceTest#coreBatchPolicyDetectsTheOldHeapOverrideAndEarlyExit
    paths:
      - config/test-collections.json
      - atf-tests/src/test/java/atf/tests/governance/source/gradle/GradleAuditGovernanceTest.java
      - whist-lab-app/game-core/build.gradle
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflineMockMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflinePerspectiveMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/match/OfflineStrategyProfileMatchTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistModelTest.java
      - whist-lab-app/gradle/component-audit.gradle
      - whist-lab-app/test-audit/src/test/java/atf/gradle/audit/GradleAuditBridgeComponentTest.java
      - whist-lab-app/test-audit/src/testFixtures/java/atf/gradle/audit/GradleReport.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistExhaustiveTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/RomanianWhistRulesTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/testing/OriginalVariantModelTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/coach/IndependentPlayerModelTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/scoring/SymmetricSeriesComponentTest.java
      - whist-lab-app/game-core/src/test/java/ro/whistlab/core/rules/OriginalVariantContractTest.java
```

</details>

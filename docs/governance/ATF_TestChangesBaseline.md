# Registrul controalelor verificate ale testelor

## Pe scurt

Acest registru păstrează controalele tehnice verificate, cu baza Git și căile lor exacte. [Ghidul schimbărilor de teste](ATF_TestChanges.md) stabilește regulile comune și separă cererea activă de înregistrarea verificată.

## Controale verificate

<details>
<summary>Guvernanța adresei de joc pe GitHub Pages</summary>

```yaml
testChanges:
  - id: WHIST-PAGES-DOCUMENTATION
    status: VERIFIED
    base: 671a35e0b205069168779a8c6901adb939eab29c
    expires: 2026-12-16
    reason: Adresa Pages proprie este o destinație de publicare, iar identificatorii proiectelor străine rămân sub control editorial. Sursele adaptorului intră în inventarul JVM.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationPolicyGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#ownPagesDestinationIsPublicationData
      negative:
        - atf.tests.governance.documentation.DocumentationPolicyGovernanceTest#externalAndNonCanonicalPagesDestinationsRemainSubjectToProseRules
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - config/atf-governance-policy.json
```

</details>

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
    reason: Păstrează inventarul binar plat exact și cere doar indexul plus cele patru ghiduri de utilizare; păstrează refuzurile independente pentru fișiere lipsă, directoare și fișiere suplimentare, identitatea fizică CodeSource și responsabilitatea pentru surse și feedback.
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
    reason: Cere exact patru perechi JAR/POM într-un singur director, README-ul dedicat și responsabilitatea producătorului comun pentru surse și feedback; păstrează identitatea binară fizică, oracolele negative independente și acceptanța consumatorului.
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
      - whist-lab-app/test-audit/src/testFixtures/java/atf/gradle/audit/verification/GradleReportVerifier.java
```

</details>

<details>
<summary>Control verificat pentru execuția probelor și mutarea verificării dialogurilor</summary>

```yaml
testChanges:
  - id: OFFLINE-QUALITY-BOUNDARIES
    status: VERIFIED
    base: f2c1b58d50e57922bbbb9ac44c977149328d7c0c
    expires: 2026-12-19
    reason: Verifică citirea concurentă a ieșirii Node și păstrează selecția manuală în dialoguri suprapuse fără repetarea legendei în traseul de licitare.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#verboseNodeRunDrainsBothStreamsFromTheSelectedDirectory
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#nonZeroNodeExitKeepsTheChildDiagnostic
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - whist-lab-app/app/src/test/java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java
      - atf-test-android/src/test/java/atf/android/e2e/release/ReleaseBidScenario.java
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
```

</details>

<details>
<summary>Control verificat pentru rădăcina temporară a calificării</summary>

```yaml
testChanges:
  - id: RAG-09-TEMP-ROOT
    status: VERIFIED
    base: f2c1b58d50e57922bbbb9ac44c977149328d7c0c
    expires: 2026-12-19
    reason: Calificarea folosește un director temporar extern și blochează căile care intră într-un checkout înainte de copiere sau Maven.
    proof: 'mvn -o -pl atf-tests -am -Dtest=QualificationLauncherComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#aJunctionInTempCannotHideAGitWorktree
    paths:
      - scripts/AtfQualificationSnapshot.ps1
      - scripts/run-atf-governance.ps1
      - scripts/run-component-suites.ps1
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Controale verificate pentru selecția și publicarea lotului</summary>

```yaml
testChanges:
  - id: GOV-06-PLAN-CONTROLS
    status: VERIFIED
    base: 6f36177aae63557abfeb18160b6ec1101dcf509c
    expires: 2026-12-19
    reason: Descoperă controalele VERIFIED ale diff-ului real și păstrează identitatea commitului.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeProofComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#requiredProofsIncludeEveryVerifiedControlOfTheCommittedDiff
      negative:
        - atf.tests.governance.source.changes.TestChangeProofComponentTest#proofFromAnotherCommitCannotBeReused
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeProofComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
  - id: GOV-06-PUBLISH-BATCH
    status: VERIFIED
    base: 6f36177aae63557abfeb18160b6ec1101dcf509c
    expires: 2026-12-19
    reason: Publică lotul complet după verificarea sursei și păstrează rapoartele individuale la refuz.
    proof: 'mvn -o -pl atf-tests -am -Dtest=TestChangeBatchComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.components.TestChangeBatchComponentTest#publishesBothControlsAfterCheckingTheSnapshot
      negative:
        - atf.tests.execution.components.TestChangeBatchComponentTest#failedControlDoesNotStopTheRemainingControlsOrPublishTheirProofs
        - atf.tests.execution.components.TestChangeBatchComponentTest#changedSourcePreventsPublicationOfPassingControls
        - atf.tests.execution.components.TestChangeBatchComponentTest#changedReviewPreventsPublicationOfPassingControls
    paths:
      - scripts/AtfTestChangeBatch.ps1
      - scripts/run-component-suites.ps1
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeBatchComponentTest.java
```

</details>

<details>
<summary>Controale verificate pentru orientarea documentelor în ambele ediții</summary>

```yaml
testChanges:
  - id: DOC-ORIENTATION-EN
    status: VERIFIED
    base: 14c16ebf3b1dea52a15849f506ed81032bb55337
    expires: 2026-12-17
    reason: Impune secțiunea introductivă în limba configurată și respinge cealaltă ediție fără relaxarea verificărilor de structură sau conținut.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationReadmeStructureGovernanceTest,DocumentationPolicyGovernanceTest,DocumentationGovernanceCliComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#ownerDocumentsKeepAReadableStableShape
      negative:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#theOtherEditionHeadingIsRejectedByTheStructuralRule
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationReadmeStructureRules.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
  - id: DOC-ORIENTATION-RO
    status: VERIFIED
    base: 007e3f660c4a2bf7dfa16cc1cb2c1996190c8020
    expires: 2026-12-17
    reason: Impune secțiunea introductivă în limba configurată și respinge cealaltă ediție fără relaxarea verificărilor de structură sau conținut.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationReadmeStructureGovernanceTest,DocumentationPolicyGovernanceTest,DocumentationGovernanceCliComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#ownerDocumentsKeepAReadableStableShape
      negative:
        - atf.tests.governance.documentation.DocumentationReadmeStructureGovernanceTest#theOtherEditionHeadingIsRejectedByTheStructuralRule
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationReadmeStructureRules.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
```

</details>

<details>
<summary>Limba ramurii și proveniența publicării originale</summary>

```yaml
testChanges:
  - id: WHIST-DOCUMENTATION-LANGUAGE
    status: VERIFIED
    base: 129a629bb65cfedf6032b503c1b826fba63d4a42
    expires: 2026-12-18
    reason: Verifică fiecare document Markdown în limba ramurii, inclusiv tabele, texte de diagrame și explicații YAML. Păstrează datele API și octeții publicării originale; confruntă ambele ediții cu declarațiile buildurilor și respinge proza amestecată, legăturile, comenzile și hash-urile modificate.
    proof: 'mvn -pl atf-tests -am -Dtest=DocumentationLanguageComponentTest,DocumentationGovernanceCliComponentTest,DocumentedModuleMapGovernanceTest,ReleasePresentationGovernanceTest,DocumentationArchiveGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#codeAndLinkTargetsRemainTechnicalDataWhileVisibleLabelsAreChecked
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#acceptsSeveralExplicitFilesAndExcludesAnUnselectedDefect
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#rootMapMatchesTheBuildDeclarations
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#everyHandoffKeepsSchematicNotesBoundToItsActualStateAndAsset
        - atf.tests.governance.documentation.DocumentationArchiveGovernanceTest#translatedReleaseGuidesRetainTheOriginalDocumentsAtTheirPinnedGitCommit
      negative:
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#englishBranchRejectsShortForeignLabelsAndDiagramCaptions
        - atf.tests.governance.documentation.language.DocumentationLanguageComponentTest#romanianBranchRejectsAnEnglishGuideOrOwnerHeading
        - atf.tests.governance.documentation.factual.DocumentedModuleMapGovernanceTest#aMissingModuleAChangedScopeAndAnInvertedEdgeAreRejected
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
    paths:
      - atf-test-android/src/test/README.md
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationInventory.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationContentPolicy.java
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationLanguageRules.java
      - atf-tests/src/test/README.md
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationAnchorGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationArchiveGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/DocumentationPolicyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/DocumentedModuleMap.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/factual/DocumentedModuleMapGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationLanguageComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/handoff/ReleaseCommitEvidence.java
      - atf-tests/tools/documentation-editions.mjs
      - atf-tests/tools/documentation-editions.test.mjs
      - atf-tests/tools/release-documentation-provenance.mjs
      - atf-tests/tools/release-documentation-provenance.test.mjs
      - atf-validation/src/test/README.md
      - config/atf-governance-policy.json
      - whist-lab-app/ai-lab/src/test/README.md
      - whist-lab-app/app/src/androidTest/README.md
      - whist-lab-app/app/src/main/java/ro/whistlab/app/README.md
      - whist-lab-app/app/src/rasterTest/README.md
      - whist-lab-app/app/src/test/README.md
      - whist-lab-app/atf-card-table-ui/src/androidTest/README.md
      - whist-lab-app/atf-card-table-ui/src/test/README.md
      - whist-lab-app/game-core/src/test/README.md
      - whist-lab-app/ios-bridge/src/test/README.md
      - whist-lab-app/test-audit/src/test/README.md
      - whist-lab-app/test-audit/src/testFixtures/README.md
```

</details>

<details>
<summary>Profil JVM al raportării limitat la suită</summary>

```yaml
testChanges:
  - id: WHIST-COMPONENT-JVM-MAIN
    status: VERIFIED
    base: e36b62a1e53d3bf593533f6a14c52687be8a3596
    expires: 2026-12-18
    reason: Limitează opțiunile raportării la JVM-ul suitei și refuză variabilele moștenite înaintea pregătirii; păstrează oracolele proceselor copil.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentJvmLauncherComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#scopeBelongsToTheSuiteAndChildBytesStreamsAndExitRemainOriginal
      negative:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#whitespaceGlobalOptionsAreStillRejected
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theComponentJvmProfileCannotChangeWithoutAReviewedControl
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmFixture.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-component-suites.ps1
      - scripts/AtfComponentJvm.ps1
  - id: WHIST-COMPONENT-JVM-RO
    status: VERIFIED
    base: 080da13d8f4edb48637043a0f6b1083ce58bcce5
    expires: 2026-12-18
    reason: Limitează opțiunile raportării la JVM-ul suitei și refuză variabilele moștenite înaintea pregătirii; păstrează oracolele proceselor copil.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ComponentJvmLauncherComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#scopeBelongsToTheSuiteAndChildBytesStreamsAndExitRemainOriginal
      negative:
        - atf.tests.execution.jvm.ComponentJvmLauncherComponentTest#whitespaceGlobalOptionsAreStillRejected
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theComponentJvmProfileCannotChangeWithoutAReviewedControl
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/execution/components/ComponentSuiteExecution.java
      - atf-tests/src/test/java/atf/tests/execution/components/TestChangeProofExecution.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmFixture.java
      - atf-tests/src/test/java/atf/tests/execution/jvm/ComponentJvmLauncherComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
      - scripts/run-component-suites.ps1
      - scripts/AtfComponentJvm.ps1
```

</details>

<details>
<summary>Limita semnării private pentru calificarea surselor</summary>

```yaml
testChanges:
  - id: WHIST-SIGNING-SOURCE-MAIN
    status: VERIFIED
    base: 23de3299a3ddd40a739fc5a444e80b48bb928aef
    expires: 2026-12-18
    reason: Exclude intrările private locale de semnare din identitatea și copierea surselor; păstrează identitatea publică și refuză căile falsificate sau indirecte.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SigningSourceIsolationComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#gitReconstructionMatchesACheckoutContainingALocalKey
      negative:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theSigningExportPolicyRequiresAnExplicitReview
    paths:
      - scripts/AtfSigningSourcePolicy.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/Copy-AtfSourceInputs.ps1
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/LauncherSourceInventoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/SourceInputFixture.java
      - atf-tests/src/test/java/atf/tests/governance/support/SigningSourceIsolationComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
  - id: WHIST-SIGNING-SOURCE-RO
    status: VERIFIED
    base: e25226e090d2a2f55a265d1bfbb8993342d2c126
    expires: 2026-12-18
    reason: Exclude intrările private locale de semnare din identitatea și copierea surselor; păstrează identitatea publică și refuză căile falsificate sau indirecte.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SigningSourceIsolationComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#gitReconstructionMatchesACheckoutContainingALocalKey
      negative:
        - atf.tests.governance.support.SigningSourceIsolationComponentTest#aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#theSigningExportPolicyRequiresAnExplicitReview
    paths:
      - scripts/AtfSigningSourcePolicy.ps1
      - scripts/Get-AtfSourceInputs.ps1
      - scripts/Copy-AtfSourceInputs.ps1
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/LauncherSourceInventoryComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/support/SourceInputFixture.java
      - atf-tests/src/test/java/atf/tests/governance/support/SigningSourceIsolationComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/release-test-scope.test.mjs
```

</details>

<details>
<summary>Sigilarea intrărilor publicării înaintea observării GitHub</summary>

```yaml
testChanges:
  - id: WHIST-PUBLICATION-SEAL-MAIN
    status: VERIFIED
    base: 8032a380bd1e683ac47950216e22a7679ca0b4d4
    expires: 2026-12-18
    reason: Sigilează catalogul și manifestele finale față de Git publicat înaintea inventarului, reverifică-le după observare și califică tagurile locale înaintea reconstrucției surselor; păstrează oracolele proiecției și transportului.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePublicationSealComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#publicationSealsTheManifestBeforeObservingGithub
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#unqualifiedLocalTagsStopBeforeSourceReconstruction
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#publicationSealAndReconstructionRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ReleasePublicationSealComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/collect-github-release-retention.mjs
      - atf-tests/tools/github-release-retention.test.mjs
      - atf-tests/tools/github-retention-fixture.mjs
      - atf-tests/tools/github-retention-seal.mjs
      - atf-tests/tools/github-retention-seal.test.mjs
      - atf-tests/tools/release-git-snapshot.test.mjs
      - atf-tests/tools/verify-release-git-snapshot.mjs
  - id: WHIST-PUBLICATION-SEAL-RO
    status: VERIFIED
    base: fbbe8127088a6015e8288f3290eb09bb430995c5
    expires: 2026-12-18
    reason: Sigilează catalogul și manifestele finale față de Git publicat înaintea inventarului, reverifică-le după observare și califică tagurile locale înaintea reconstrucției surselor; păstrează oracolele proiecției și transportului.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePublicationSealComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#publicationSealsTheManifestBeforeObservingGithub
      negative:
        - atf.tests.governance.documentation.release.rag.scope.ReleasePublicationSealComponentTest#unqualifiedLocalTagsStopBeforeSourceReconstruction
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#publicationSealAndReconstructionRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ReleasePublicationSealComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/collect-github-release-retention.mjs
      - atf-tests/tools/github-release-retention.test.mjs
      - atf-tests/tools/github-retention-fixture.mjs
      - atf-tests/tools/github-retention-seal.mjs
      - atf-tests/tools/github-retention-seal.test.mjs
      - atf-tests/tools/release-git-snapshot.test.mjs
      - atf-tests/tools/verify-release-git-snapshot.mjs
```

</details>

<details>
<summary>Proba contractelor vizuale și reuniunea fluxurilor native obligatorii</summary>

```yaml
testChanges:
  - id: WHIST-VISUAL-SCOPE-MAIN
    status: VERIFIED
    base: f7d876687c476c5684d0a3a7a27fc8e8b6956ddb
    expires: 2026-12-18
    reason: Restrânge numai contractele numerice de prezentare verificate; păstrează responsabilii de componentă obligatorii și reunește fiecare flux de interacțiune, persistență, scor și runtime necunoscut cu verificarea nativă.
    proof: 'mvn -o -pl atf-tests -am -Dtest=VisualContractScopeComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#provenTypographyKeepsNativeSanityAndItsComponentOwners
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative
      negative:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#mixedBehaviorAndBinaryChangesCannotLoseMandatoryFlows
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#unprovenContractsKeepEveryNativeFlow
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#visualScopeProofsCannotChangeWithoutReview
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/VisualContractScopeComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-tests/tools/visual-contract-sources.mjs
      - atf-tests/tools/visual-scope-fixture.mjs
      - atf-tests/tools/visual-contracts.test.mjs
  - id: WHIST-VISUAL-SCOPE-RO
    status: VERIFIED
    base: a04c0bda3823620774ee4a431b5c13662f6f7ff8
    expires: 2026-12-18
    reason: Restrânge numai contractele numerice de prezentare verificate; păstrează responsabilii de componentă obligatorii și reunește fiecare flux de interacțiune, persistență, scor și runtime necunoscut cu verificarea nativă.
    proof: 'mvn -o -pl atf-tests -am -Dtest=VisualContractScopeComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#provenTypographyKeepsNativeSanityAndItsComponentOwners
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#theRealPlannerBindsPinnedBytesAndKeepsUnavailableProofsConservative
      negative:
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#mixedBehaviorAndBinaryChangesCannotLoseMandatoryFlows
        - atf.tests.governance.documentation.release.rag.scope.VisualContractScopeComponentTest#unprovenContractsKeepEveryNativeFlow
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#visualScopeProofsCannotChangeWithoutReview
    paths:
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/VisualContractScopeComponentTest.java
      - atf-tests/tools/release-test-scope.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/visual-change-contracts.mjs
      - atf-tests/tools/visual-contract-sources.mjs
      - atf-tests/tools/visual-scope-fixture.mjs
      - atf-tests/tools/visual-contracts.test.mjs
```

</details>

<details>
<summary>Terminologie operațională engleză și deținerea persistentă a utilizatorului temporar</summary>

```yaml
testChanges:
  - id: WHIST-DOC-RECOVERY-MAIN
    status: VERIFIED
    base: 05fde6ecb286184c044b89b161a6036a24357de0
    expires: 2026-12-18
    reason: Verifică terminologia ghidurilor engleze față de codul live și păstrează utilizatorii originali prin deținere persistentă, pregătire întreruptă și recuperare legată de rezervare, cu transport simulat.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationTerminologyComponentTest,DocumentationGovernanceCliComponentTest,AndroidUserOwnershipComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#translationArtifactsHaveTheSameFullAndSelectedDiagnostics
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
      negative:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#linkLabelsAndTableCellsRemainVisible
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#userRecoveryAndEnglishVocabularyRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/tools/android-user-ownership.test.mjs
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.mjs
      - scripts/Invoke-AtfAndroidDisposableUser.ps1
  - id: WHIST-DOC-RECOVERY-RO
    status: VERIFIED
    base: 6ce614b643e76d62b068899871658040f90c7186
    expires: 2026-12-18
    reason: Verifică terminologia ghidurilor engleze față de codul live și păstrează utilizatorii originali prin deținere persistentă, pregătire întreruptă și recuperare legată de rezervare, cu transport simulat.
    proof: 'mvn -o -pl atf-tests -am -Dtest=DocumentationTerminologyComponentTest,DocumentationGovernanceCliComponentTest,AndroidUserOwnershipComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule
        - atf.tests.governance.documentation.cli.DocumentationGovernanceCliComponentTest#translationArtifactsHaveTheSameFullAndSelectedDiagnostics
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
      negative:
        - atf.tests.governance.documentation.language.DocumentationTerminologyComponentTest#linkLabelsAndTableCellsRemainVisible
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#userRecoveryAndEnglishVocabularyRequireReviewedControls
    paths:
      - atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/language/DocumentationTerminologyGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/tools/android-user-ownership.test.mjs
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-test-android/src/test/java/atf/android/e2e/release/README.md
      - atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/cli/DocumentationGovernanceCliComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/release-test-scope.mjs
      - scripts/Invoke-AtfAndroidDisposableUser.ps1
```

</details>

<details>
<summary>Proveniența motorului nativ și contractele portabile Web/Android</summary>

```yaml
testChanges:
  - id: WHIST-NATIVE-ADAPTER-MAIN
    status: VERIFIED
    base: 6016fe842dc7b7110fdf7812a4a54004bdb321b1
    expires: 2026-12-19
    reason: Separă identitatea motorului nativ și a scenariului de validatorul curent; păstrează chitanțele originale și verifică adaptoarele portabile Web/Android cu măsurători controlate.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest,WhistSurfaceAdapterComponentTest,ReleaseRagAndroidComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.ReleaseRagAndroidComponentTest#nativeRuntimeAndValidatorRevisionsRemainIndependentInTheRealEvaluator
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/NativeEvidenceProbe.java
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
      - atf-tests/tools/source-input-identity.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagAndroidComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/ReleaseSuiteReportingGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/android-e2e-evidence.mjs
      - atf-tests/tools/apply-android-e2e.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-android-e2e.mjs
  - id: WHIST-NATIVE-ADAPTER-RO
    status: VERIFIED
    base: 2ea1ccb42cb1d7b34f481438411bbe49b6d61c99
    expires: 2026-12-19
    reason: Separă identitatea motorului nativ și a scenariului de validatorul curent; păstrează chitanțele originale și verifică adaptoarele portabile Web/Android cu măsurători controlate.
    proof: 'mvn -o -pl atf-tests -am -Dtest=NativeSourceProvenanceComponentTest,WhistSurfaceAdapterComponentTest,ReleaseRagAndroidComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.ReleaseRagAndroidComponentTest#nativeRuntimeAndValidatorRevisionsRemainIndependentInTheRealEvaluator
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/NativeEvidenceProbe.java
      - atf-tests/tools/android-e2e-provenance.mjs
      - atf-tests/tools/android-e2e-provenance.test.mjs
      - atf-tests/tools/source-input-identity.mjs
      - atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java
      - atf-ui-browser/tests/whist-surface-contract.test.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/ReleaseRagAndroidComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/NodeReleaseContracts.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/reporting/ReleaseSuiteReportingGovernanceTest.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-tests/tools/android-e2e-evidence.mjs
      - atf-tests/tools/apply-android-e2e.mjs
      - atf-tests/tools/evaluate-release-rag.mjs
      - atf-tests/tools/prepare-android-e2e.mjs
```

</details>

<details>
<summary>Lizibilitatea scorului comun și continuitatea AI/jucător</summary>

```yaml
testChanges:
  - id: SHARED-SCORE-MAIN
    status: VERIFIED
    base: 06badb5679bd9deb2ec87bae0a587128fbcec571
    expires: 2026-12-19
    reason: Revizuiește sincronizarea producătorului comun, adaptoarele portabile, auditul local și tipografia reală Web; refuză ștergerea prematură a levatei.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,WhistSurfaceAdapterComponentTest,NativeSourceProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-ui-browser/tests/fixtures.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/trick-playback.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportAudit.java
      - whist-lab-app/atf-card-table-ui/src/test/README.md
  - id: SHARED-SCORE-RO
    status: VERIFIED
    base: 19fbb34af6791e57e890f4856b3ca3bfbb2a9bf2
    expires: 2026-12-19
    reason: Revizuiește sincronizarea producătorului comun, adaptoarele portabile, auditul local și tipografia reală Web; refuză ștergerea prematură a levatei.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,WhistSurfaceAdapterComponentTest,NativeSourceProvenanceComponentTest,TestChangeReviewComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.documentation.release.rag.nativeproof.WhistSurfaceAdapterComponentTest#webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.nativeproof.NativeSourceProvenanceComponentTest#immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs
        - atf.tests.governance.source.changes.TestChangeReviewComponentTest#nativeSourceAdmissionAndAdapterOraclesCannotBypassReview
    paths:
      - atf-ui-browser/tests/fixtures.mjs
      - atf-tests/src/main/java/atf/tests/testchange/TestChangeReview.java
      - atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/trick-playback.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryGridComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/NativeSourceProvenanceComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportAudit.java
      - config/gradle-audit-collections.properties
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/README.md
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/seat/PlayerSeatMetricsComponentTest.java
```

</details>

<details>
<summary>Tipografia nativă a scorului și verificarea notelor de release</summary>

```yaml
testChanges:
  - id: SCORE-TYPOGRAPHY-MAIN
    status: VERIFIED
    base: 1e078d06f4000d848675d87497302bad3052903d
    expires: 2026-12-19
    reason: Align score adapters and qualify Android API compatibility, complete launcher fixtures, transport line endings and independent component reuse without weakening artifact or unknown-input rejection.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.source.android.AndroidJavaApiGovernanceTest#androidProductionAvoidsTheJava16StreamTerminal
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/publish-github-release.test.mjs
      - atf-ui-browser/tests/fixtures.mjs
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/ScoreTypographyProbe.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/app/build.gradle
      - config/gradle-audit-collections.properties
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
  - id: SCORE-TYPOGRAPHY-RO
    status: VERIFIED
    base: 910439dab4bbcd72afe2d0a18c4fb9f583d339c6
    expires: 2026-12-19
    reason: Apply the same reviewed source and test boundaries to the Romanian edition.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest,ComponentEvidenceScopeComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
        - atf.tests.governance.source.android.AndroidJavaApiGovernanceTest#androidProductionAvoidsTheJava16StreamTerminal
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#governancePassesWithAnIsolatedRootAndThePinnedInputManifest
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation
        - atf.tests.governance.documentation.release.rag.scope.AndroidUserOwnershipComponentTest#durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport
        - atf.tests.governance.documentation.release.rag.scope.QualificationLauncherComponentTest#checkoutTmpStopsBothLaunchersBeforeStagingOrMaven
        - atf.tests.governance.documentation.release.rag.scope.ComponentEvidenceScopeComponentTest#componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/ComponentEvidenceScopeComponentTest.java
      - atf-tests/tools/publish-github-release.test.mjs
      - atf-ui-browser/tests/fixtures.mjs
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/ScoreTypographyProbe.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/boundary/UiLibraryBoundaryComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/app/build.gradle
      - config/gradle-audit-collections.properties
      - scripts/AtfAndroidUserOwnership.ps1
      - atf-tests/tools/android-user-ownership.test.mjs
      - atf-tests/tools/component-evidence-scope.mjs
      - atf-tests/tools/prepare-component-reuse.test.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidUserOwnershipComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/QualificationLauncherComponentTest.java
```

</details>

<details>
<summary>Rezumatele notelor de release</summary>

```yaml
testChanges:
  - id: NOTE-SUMMARY-MAIN
    status: VERIFIED
    base: 25ac9be5697dea9664268b9982e287fc05797b51
    expires: 2026-12-19
    reason: Acceptă un singur paragraf introductiv limitat, păstrând secțiunile canonice, limitele Unicode și notele istorice.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
  - id: NOTE-SUMMARY-RO
    status: VERIFIED
    base: 290590801afd29e201b03ebf43f10c6c4e8ad5a8
    expires: 2026-12-19
    reason: Acceptă un singur paragraf introductiv limitat, păstrând secțiunile canonice, limitele Unicode și notele istorice.
    proof: 'mvn -o -pl atf-tests -am -Dtest=ReleasePresentationGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#translatedPresentationKeepsTheReleaseStructureAssetsAndOriginalPointCount
      negative:
        - atf.tests.governance.documentation.release.ReleasePresentationGovernanceTest#missingEvidenceNonBulletProseAndUnrecordedChangesAreRejected
    paths:
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleasePresentationGovernanceTest.java
```

</details>

<details>
<summary>Tipografia acțiunilor de rezultat</summary>

```yaml
testChanges:
  - id: SCORE-ACTIONS-MAIN
    status: VERIFIED
    base: 30812f432d7e744e6f2acf711e30dbd1848fbacb
    expires: 2026-12-19
    reason: Mărește doar acțiunile de rezultat prin roluri comune și păstrează accesul în ferestre înguste, celelalte fonturi și ordinea callback-urilor.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreActionsRejectStaleRolesClippingAndRepeatedScaling
    paths:
      - atf-ui-browser/Build-WhistPages.ps1
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/score-presentation.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
  - id: SCORE-ACTIONS-RO
    status: VERIFIED
    base: f680c257777611dc19d3cdf24a4295ed9b697029
    expires: 2026-12-19
    reason: Mărește doar acțiunile de rezultat prin roluri comune și păstrează accesul în ferestre înguste, celelalte fonturi și ordinea callback-urilor.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedPresentationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes
      negative:
        - atf.tests.governance.documentation.release.rag.nativeproof.SharedPresentationComponentTest#scoreActionsRejectStaleRolesClippingAndRepeatedScaling
    paths:
      - atf-ui-browser/Build-WhistPages.ps1
      - atf-ui-browser/tests/shared-presentation.test.mjs
      - atf-ui-browser/tests/score-presentation.mjs
      - atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/SharedPresentationComponentTest.java
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/ScoreTypographyComponentTest.java
```

</details>

<details>
<summary>Interfața standard a raportului</summary>

```yaml
testChanges:
  - id: RPT-OVERVIEW-MAIN
    status: VERIFIED
    base: a07e597fb9238e89f6614643442c344058c66362
    expires: 2026-12-20
    reason: Preserve the standard English report interface and original case-owned captures, comparisons and outcomes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserExtentPublicationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#missingCaptureIsRejectedBeforeReportCreation
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#invalidLayerAndImageShapeDoNotCreatePartialReports
    paths:
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
  - id: RPT-OVERVIEW-RO
    status: VERIFIED
    base: 4a7b06cb78b5e98bcca3f45d62d1681073327a9a
    expires: 2026-12-20
    reason: Preserve the standard English report interface and original case-owned captures, comparisons and outcomes.
    proof: 'mvn -o -pl atf-tests -am -Dtest=BrowserExtentPublicationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures
      negative:
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#missingCaptureIsRejectedBeforeReportCreation
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#externalCaptureAndInvalidPngCannotBePublished
        - atf.tests.reporting.browser.BrowserExtentPublicationComponentTest#invalidLayerAndImageShapeDoNotCreatePartialReports
    paths:
      - atf-ui-browser/tests/BrowserExtent.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserReportFixture.java
      - atf-tests/src/test/java/atf/tests/reporting/browser/BrowserExtentPublicationComponentTest.java
      - atf-tests/src/test/java/atf/tests/reporting/GameReportComponentSuite.java
```

</details>

<details>
<summary>Adaptoare comune pentru meniu și istoric</summary>

```yaml
testChanges:
  - id: UI-MENU-HISTORY-MAIN
    status: VERIFIED
    base: 8812885a4ecba84f9dfca3980a2c0b4165e4b900
    expires: 2026-12-20
    reason: Bind native and browser menu intentions and history appearance through shared models, preserving legacy behavior and immutable library coordinates.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#newLibraryCoordinatesUseTwoPartsAndLegacyCoordinatesAreReuseOnly
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
    paths:
      - config/gradle-audit-collections.properties
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/Start-CardTableBrowser.ps1
      - atf-ui-browser/pages/src/main/java/ro/cardtable/browser/PageJson.java
      - atf-ui-browser/src/main/java/ro/cardtable/browser/BrowserGame.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/records.mjs
      - atf-ui-browser/src/main/web/table-menu.mjs
      - atf-ui-browser/src/main/web/table.css
      - atf-ui-browser/src/main/web/table.mjs
      - scripts/export-card-table-ui.ps1
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreHistoryView.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-ui-browser/tests/MenuHistoryProbe.java
      - atf-ui-browser/tests/menu-history-contract.mjs
      - atf-ui-browser/tests/menu-history.test.mjs
      - atf-ui-browser/tests/package-version.test.mjs
      - scripts/CardTableLibraryVersion.psm1
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
  - id: UI-MENU-HISTORY-RO
    status: VERIFIED
    base: 82d9dd9426c5a3ee7826f187bacf29879e30d1e3
    expires: 2026-12-20
    reason: Bind native and browser menu intentions and history appearance through shared models, preserving legacy behavior and immutable library coordinates.
    proof: 'mvn -o -pl atf-tests -am -Dtest=SharedMenuHistoryComponentTest -Dsurefire.failIfNoSpecifiedTests=false test'
    checks:
      positive:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#historyTypographyAndDividersFollowTheNativePolicy
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#menuAdaptersKeepNativeLabelsOrderAndDispatch
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#newLibraryCoordinatesUseTwoPartsAndLegacyCoordinatesAreReuseOnly
      negative:
        - atf.tests.governance.uiadapter.SharedMenuHistoryComponentTest#invalidStylesAreAtomicAndClearingRestoresTheBaseline
    paths:
      - config/gradle-audit-collections.properties
      - atf-ui-browser/Package-CardTableBrowser.ps1
      - atf-ui-browser/Start-CardTableBrowser.ps1
      - atf-ui-browser/pages/src/main/java/ro/cardtable/browser/PageJson.java
      - atf-ui-browser/src/main/java/ro/cardtable/browser/BrowserGame.java
      - atf-ui-browser/src/main/web/index.html
      - atf-ui-browser/src/main/web/records.mjs
      - atf-ui-browser/src/main/web/table-menu.mjs
      - atf-ui-browser/src/main/web/table.css
      - atf-ui-browser/src/main/web/table.mjs
      - scripts/export-card-table-ui.ps1
      - whist-lab-app/atf-card-table-ui/build.gradle
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/menu/TableMenuContent.java
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/ScoreHistoryView.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/menu/TableMenuContentComponentTest.java
      - atf-tests/src/test/java/atf/tests/governance/uiadapter/SharedMenuHistoryComponentTest.java
      - atf-ui-browser/tests/MenuHistoryProbe.java
      - atf-ui-browser/tests/menu-history-contract.mjs
      - atf-ui-browser/tests/menu-history.test.mjs
      - atf-ui-browser/tests/package-version.test.mjs
      - scripts/CardTableLibraryVersion.psm1
      - whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/score/HistoryAppearance.java
      - whist-lab-app/atf-card-table-ui/src/test/java/ro/cardtable/ui/score/HistoryAppearanceComponentTest.java
```

</details>

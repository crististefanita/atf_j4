# Register of verified test controls

## Overview

This register keeps the technical checks checked, with the basis of Git and their exact paths. [Guide to test changes](ATF_TestChanges.md) sets out common rules and separates the active application for the registration checked.

## Verified controls

<details>
<summary>Governance of the game address on GitHub Pages</summary>

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
<summary>Check: Native acceptance of actions and history</summary>

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

A simulated landmark shows how the action ignore the comparison of distances on the same coordinates. Negative controls keep text, actor, size and identity errors of the row; native running requires installation metadata, reservation and private archive defined in the [contractul Android](../testing/ATF_Android.md#profile-history-leaderboards-and-series).

</details>

<details>
<summary>Check: mass proportions and action above the name</summary>

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

The oracle of dimensions follows the requirement for presentation and negative controls refuse the exceeded height, the missing action, the abbreviated text and the overlap over the name. The native verification remains distinct from the simulated observation and JVM mechanism.

</details>

<details>
<summary>Check control: historical measurement and log invariants</summary>

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

Regression is faced with the previous source prior to the correction. Font providers are injected into components; the qualification of the raster and Android font remains separate. A diagnosis of the classSPath is not reclassified as a defect of the product.

</details>

<details>
<summary>Checked control: compact history and explicit outcome of contract</summary>

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

Zero, failure with positive points, unknown data and an unfinished hand have distinct results. Negative controls refuse fractional, negative, unserviced or contradictory levatates, and injected measurements do not replace native rendering.

</details>

<details>
<summary>Check control: common asset indicator and discrete deposit</summary>

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

The controls shall keep the card or colour, legibility, location and accessible description; the absence of redundant text shall not replace the presence of value. Injected font measurements qualify for the common policy, and native verification compares the same geometry in states with and without trump.

</details>

<details>
<summary><strong>Library guides - short index and guides per librarian </strong></summary>

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

Examples are extracted from READMEs and checked on physical JARs. Change keeps published pairs and existing oracles.

</details>

<details>
<summary>Check: technical address of public discharge</summary>

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

Reproduction is refusal of the public link valid as editorial reference; negative controls keep labels, other projects and non-anonymous variants within the checked perimeter. The contract [documentation](ATF_Documentation.md#stable-content-and-variable-data) defines the limit without general exceptions for URLs.

</details>

<details>
<summary>Check: page of the declared version</summary>

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

[Delivery identity](../platform/ATF_AndroidDelivery.md) sets out the version; [Documentary policy](ATF_Documentation.md#stable-content-and-variable-data) classifies only canonical destinations. The samples shall refuse the undeclared page, another version, another repository, image and URLs with changes; the visible label shall be exempt.

</details>

<details>
<summary>Check: library ownership and documentation</summary>

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

Negative controls reject the missing READMEM, additional documents, a director instead of README- and the assignment of the code or feedback to a consumer. The distributed libraries keep their coordinates and hashes.

The inventory at Git base declared is that of the checked increment. The current pairs and guides are defined in [contractul lib](../../lib/README.md).

</details>

<details>
<summary>Control check: test authorization on the Git diff</summary>

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
<summary>Checked for traceability of the change sample</summary>

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
<summary>Checked control for session facade and restoration</summary>

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
<summary>Checked control for Gradle lots budget</summary>

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
<summary>Checked control for the execution of the evidence and movement of the verification of the dialogues</summary>

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
<summary>Checked control for temporary root of qualification</summary>

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
<summary>Checks verified for selection and publication of the consignment</summary>

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
<summary>Verified controls for document orientation in both editions</summary>

```yaml
testChanges:
  - id: DOC-ORIENTATION-EN
    status: VERIFIED
    base: 14c16ebf3b1dea52a15849f506ed81032bb55337
    expires: 2026-12-17
    reason: Enforce the configured document orientation and reject the other edition without weakening structure or content checks.
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
    reason: Enforce the configured document orientation and reject the other edition without weakening structure or content checks.
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

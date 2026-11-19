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
    reason: The project Pages address is a publication destination; foreign project identifiers remain governed prose. Adapter sources belong to the JVM inventory.
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
    reason: Measure distance from the player frame and its observed action, rejecting overlap; cover exact dimensions, localization, legally prepared actors, and access to every row and column.
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

A simulated landmark shows how the action ignore the comparison of distances on the same coordinates. Negative controls keep text, actor, size and identity errors of the row; native running requires installation metadata, reservation and private archive defined in the [Android contract](../testing/ATF_Android.md#profile-history-leaderboards-and-series).

</details>

<details>
<summary>Check: table proportions and action above the name</summary>

```yaml
testChanges:
  - id: DISPLAY-PROPORTIONS-ACTION-AND-HISTORY
    status: VERIFIED
    base: 8933374e33ab6eee9cb4217ed1a6112aebacc9b3
    expires: 2026-12-05
    reason: Reduce played-card height and trump width, enlarge player frames, and separate actions from names; retain non-overlapping geometry and chronological score identity.
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
    reason: Extract measurement aggregation from the Android view and reject invalid seats, lost tricks, and impossible dimensions; retain unknown suffixes, immutability, and measurement of the final cell.
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
    reason: Apply the agreed compact geometry and explicit trick schema; retain unknown prefixes, independent score comparisons, trick conservation, and complete cells in both languages.
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
    reason: Remove the redundant unturned-card row and use one two-region frame in every mode; retain the visible value, accessible identity, and touch target. Synchronize hand dimensions and seat indexes with concurrent display-policy changes.
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
    reason: Distinguish the technical destination of the project repository link from visible prose; continue rejecting identifiers in labels, other projects, source paths, and modified URLs.
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
    reason: Recognize only the declared release tag page, APK, and checksum; continue checking visible prose, other versions, and modified addresses.
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

Negative controls reject the missing README, additional documents, a director instead of README- and the assignment of the code or feedback to a consumer. The distributed libraries keep their coordinates and hashes.

The inventory at Git base declared is that of the checked increment. The current pairs and guides are defined in [library contract](../../lib/README.md).

</details>

<details>
<summary>Control check: test authorization on the Git diff</summary>

```yaml
testChanges:
  - id: GOVERNANCE-GIT-TEST-CHANGES
    status: VERIFIED
    base: 08706c3a1a3fe0172b706da2c69e9f767fde54bd
    expires: 2026-12-19
    reason: Bind authorization to the reviewed commit, exact paths, and expiry, with negative checks on real Git repositories.
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
    reason: Bind offline evidence to its control, reviewed base, commit, and diff content, and require an executed negative control.
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
    reason: Compare moves, scoring, and restoration through the public facade against an independent JVM oracle; verify rejection without mutation.
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
    reason: Fix the shared memory budget and separate strategy batches; reject heap overrides and stop at the first failed case.
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
    reason: Verify concurrent draining of Node output and preserve manual selection under overlapping dialogs without repeating the legend in the bidding flow.
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
    reason: Qualification uses an external temporary directory and rejects paths inside a checkout before copying or Maven execution.
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
    reason: Discover VERIFIED controls in the real diff and preserve commit identity.
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
    reason: Publish the complete batch after checking source identity; retain individual reports when publication is rejected.
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

<details>
<summary>Branch language and original publication provenance</summary>

```yaml
testChanges:
  - id: WHIST-DOCUMENTATION-LANGUAGE
    status: VERIFIED
    base: 129a629bb65cfedf6032b503c1b826fba63d4a42
    expires: 2026-12-18
    reason: Govern every Markdown path in the configured branch language, including tables, diagram captions and YAML explanations. Keep API data and original publication bytes intact; verify both editions against build declarations and reject mixed prose, changed links, commands and source hashes.
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
<summary>Suite-local JVM reporting profile</summary>

```yaml
testChanges:
  - id: WHIST-COMPONENT-JVM-MAIN
    status: VERIFIED
    base: e36b62a1e53d3bf593533f6a14c52687be8a3596
    expires: 2026-12-18
    reason: Scope reporting options to the suite JVM and reject inherited option variables before preparation; preserve child-process oracles.
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
    reason: Scope reporting options to the suite JVM and reject inherited option variables before preparation; preserve child-process oracles.
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
<summary>Private signing boundary for source qualification</summary>

```yaml
testChanges:
  - id: WHIST-SIGNING-SOURCE-MAIN
    status: VERIFIED
    base: 23de3299a3ddd40a739fc5a444e80b48bb928aef
    expires: 2026-12-18
    reason: Exclude private local signing inputs from source identity and copying; keep public identity and refuse forged or indirect source paths.
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
    reason: Exclude private local signing inputs from source identity and copying; keep public identity and refuse forged or indirect source paths.
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
<summary>Seal publication inputs before observing GitHub</summary>

```yaml
testChanges:
  - id: WHIST-PUBLICATION-SEAL-MAIN
    status: VERIFIED
    base: 8032a380bd1e683ac47950216e22a7679ca0b4d4
    expires: 2026-12-18
    reason: Seal final catalog and manifests against published Git before inventory, recheck them after observation, and qualify local tags before reconstructing sources; retain projection and transport oracles.
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
    reason: Seal final catalog and manifests against published Git before inventory, recheck them after observation, and qualify local tags before reconstructing sources; retain projection and transport oracles.
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
<summary>Visual contract proof and mandatory native flow union</summary>

```yaml
testChanges:
  - id: WHIST-VISUAL-SCOPE-MAIN
    status: VERIFIED
    base: f7d876687c476c5684d0a3a7a27fc8e8b6956ddb
    expires: 2026-12-18
    reason: Narrow only verified numeric presentation contracts; keep mandatory component owners and union every interaction, persistence, scoring and unknown-runtime flow with native sanity.
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
    reason: Narrow only verified numeric presentation contracts; keep mandatory component owners and union every interaction, persistence, scoring and unknown-runtime flow with native sanity.
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
<summary>English operational terminology and durable temporary-user ownership</summary>

```yaml
testChanges:
  - id: WHIST-DOC-RECOVERY-MAIN
    status: VERIFIED
    base: 05fde6ecb286184c044b89b161a6036a24357de0
    expires: 2026-12-18
    reason: Check English guide terminology against live code and preserve original users through durable ownership, interrupted preparation and lease-bound recovery with mock transport.
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
    reason: Check English guide terminology against live code and preserve original users through durable ownership, interrupted preparation and lease-bound recovery with mock transport.
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
<summary>Native runtime provenance and portable Web/native adapter contracts</summary>

```yaml
testChanges:
  - id: WHIST-NATIVE-ADAPTER-MAIN
    status: VERIFIED
    base: 6016fe842dc7b7110fdf7812a4a54004bdb321b1
    expires: 2026-12-19
    reason: Separate native runtime and scenario identity from the running validator; preserve original receipts and verify real Web/native portable adapters with controlled measurements.
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
    reason: Separate native runtime and scenario identity from the running validator; preserve original receipts and verify real Web/native portable adapters with controlled measurements.
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
<summary>Shared score readability and AI/human trick continuity</summary>

```yaml
testChanges:
  - id: SHARED-SCORE-MAIN
    status: VERIFIED
    base: 06badb5679bd9deb2ec87bae0a587128fbcec571
    expires: 2026-12-19
    reason: Review shared producer synchronization, native/Web portable adapters, local audit ownership and actual Web score typography; reject premature collection clearing.
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
    reason: Review shared producer synchronization, native/Web portable adapters, local audit ownership and actual Web score typography; reject premature collection clearing.
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
<summary>Native score typography and release-note preflight</summary>

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

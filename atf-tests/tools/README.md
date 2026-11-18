# Entry tools for publication

## Overview

These Node.js tools belong to the reporting consumer and to the delivery governance. They validate the identity of the captured records, select full claims and record the integrity of the fixed file without recalculating the outcome of the game.

## Contract and verification

[whist-report-input.mjs](whist-report-input.mjs) has the selection and inventory of the native population. [Its tests](whist-report-input.test.mjs) checks duplicate identities, contradictory sources, omissions and the preservation of declared failures.

From the root of the project:

```powershell
node --test atf-tests/tools/whist-report-input.test.mjs
```

The selected data remains the same objects evaluated in the declared order. [Presentation policy](../../docs/reporting/ATF_Presentation.md) and [publication audit](../../docs/reporting/ATF_Audit.md) define the subsequent stages.

## Registration of delivery integrity

[record-release-integrity.mjs](record-release-integrity.mjs) reads the active version of the index and records the size and the HA- 256 for all its files without the self-referential manifest. Refuse symbolic pathways and change of inventory or manifest during preparation; writing uses a temporary file and atomic replacement.

```powershell
node atf-tests/tools/record-release-integrity.mjs
```

`--project <root>` and `--version <version>` allow an explicit root and edition. The tool does not perform tests and does not promote qualification; [verifier Java](../src/test/java/atf/tests/governance/documentation/release/integrity/ReleaseIntegrity.java) face the bytes and results of JUnit separately. [Manifesto components](../src/test/java/atf/tests/governance/documentation/release/integrity/ManifestIntegrityComponentTest.java) exercise the actual launcher on temporary files, including paths with spaces and diacritics.

## Candidate RAG evaluation

[Selector](plan-release-tests.mjs) compare the inventory fixed with the last relevant published source in the catalogue. Generate the plane fields v2 and the E2E groups required before the build. [Evaluator](evaluate-release-rag.mjs) recalculate the selection and confront the plan, the manifest, payload and the APK snapshots. [Selection command](../../docs/testing/ATF_ReleaseQualification.md#selection-of-diff-tests)

- [Scope suite](component-evidence-scope.mjs) determines when an execution can be reused. For Gradle collections, the documentation and entries exclusively of Maven or Android E2E do not invalidate the proof of the application; sources, compilation configuration, dependencies and unknown pathways require new selection. [Preparation of evidence](prepare-component-reuse.mjs) and the evaluator use the same checks for receipts and hashes. The component launcher shall use this training on request.
- The version policy sets out Maven and Gradle mandatory groups. Each group has its own Extent and Receipt.

```powershell
$ProjectRoot = (Get-Location).Path
node .\atf-tests\tools\evaluate-release-rag.mjs `
  --project-root $ProjectRoot `
  --candidate-dir $CandidateDir `
  --source-root $SourceSnapshotDir `
  --plan-file $QualificationPlan `
  --output $VerdictFile
```

The verdict is JSON with citations by way, SHA- 256 and JSON Pointer/line. Include bytes plan in Base64 UTF- 8 to `qualificationPlan.contentBase64`; hash retains the identity of the plan and allows revision without an external temporary copy. The only writing is `$VerdictFile`, except for all entries.

- Code `0` means `READY_FOR_HUMAN_REVIEW`; `2` means `BLOCKED`; `1` indicates invalid entry or invocation.
- Before the human revision are asked for hashrelated receipts for components, build APK, identity and signature.
- The published status also requires a receipt for discharge into the declared access mode. For GitHub, the retention of the two editions, including tags and assets.[Collection command](../../docs/testing/ATF_ReleaseQualification.md#retention-on-github-after-publication)
- [Length of the track](prepare-android-e2e.mjs) and the evaluator use the same checks for HTML, PNG and receipts. [Import](apply-android-e2e.mjs) brings new proof in the fixed candidate. [The contract of the launcher](../../docs/testing/ATF_ReleaseQualification.md#starting-and-resuming-a-game-on-the-signed-apk)
- E2E not executed remains `NOT_RUN`; changing a user stream requires E2E Extent groups past.

The evaluator confronts the complete inventory of snapshots with the manifest. The operator review checks the Git index, the promotion tree and publication. The contract and the oracles are described in the [the transmission qualification](../../docs/testing/ATF_ReleaseQualification.md) and [evaluator's regression](../../docs/testing/ATF_ReleaseQualification.md#evaluator-regression-tests).

[Integration module](../README.md) · [AI lab](../../whist-lab-app/ai-lab/README.md)

## Measurement and reproduction

- [Chronology](assemble-release-timeline.mjs) combines process receipts and keeps the overlapping intervals. [Qualification procedure](../../docs/testing/ATF_ReleaseQualification.md#qualification-timeline)
- [Verifier Git](verify-release-git-snapshot.mjs) confronts inventory and bytes, including EOL, and checks the payload published prior to cleaning. [Withdrawal contract](../../docs/platform/ATF_ReleaseRetention.md#local-cleaning-after-delivery)

- For a separate checkout, option `--local-release` synchronise only the current edition after the check of the verdict, tag and payload in Git. [Synchronisation procedure](../../docs/platform/ATF_ReleaseRetention.md#synchronisation-of-local-file-with-current-version)

# Governance of test changes

## Overview

A change in the tests shall preserve the meaning of the contract and the reason for which an entry is admitted or refused. The change in implementation does not justify adjusting the expected result only to achieve success.

## Criteria

| Surface | Contract |
|---|---|
| Expected | Derived from requirement, invariant or independent oracle. |
| Control negativ | The intended failure demonstrates that the test detects the defect concerned. |
| Identitate | The case, sources, configuration and domain are explicit. |
| Verdict | The original cause shall be retained by catch and publication. |
| Population | The selection and omissions shall be declared without historical counting fixed in the guide. |
| Rezultate | Concrete runs and version comparisons shall be kept in the external archive. |

## Revision of a change

1. Form general ownership and its borders.
2. Reproduce the defect in a small case with controlled data.
3. Check negative control failure and successful valid case.
4. Reconcile competing sources prior to qualification.
5. Update the collection, contract and documentation of the mechanism in the same revision.

[Governance of documentation](ATF_Documentation.md) · [Collections](../testing/ATF_Collections.md)

## Checking the Git diff

Run [Governance launcher](../../scripts/run-atf-governance.ps1) from the revised checkout. Choose the joint communique before change. The launcher checks the tests on an isolated copy and reads the difference in the Git checkout indicated.

```powershell
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Contract | Verification |
|---|---|
| Base | A local commit prior to or equal to head. In CI, you declare the explicit and prior basis of head. Locally, without a parameter, HEAD only delimits uncommitted changes. |
| Git inputs | Differences from base, index, work files and new files. Includes protected entries ignored by Git. |
| Perimetru | Maven and Gradle sets of tests and resources, POMS, Gradle configuration, suite selections and executable inputs of governance and qualification. |
| Authorisation | Only one entry eligible for exact route, base SHAUS and UTC date. The expiry date is inclusive. |
| Refuz | Absence or foreign base, uncovered path, expired control, ambiguous eligibility, invalid metadata or unaccepted Git status. |
| Redenumire | Erase the old path plus add the new path. The deletion explicitly requires allowDelete. |

The launcher shall transmit the base by property atf.government ance.testChanges.baseRef. For the selection of components, set ATF_TEST_CHANGE_BASE_REF prior to launch. The entire branch's review always uses a commit prior to head.

## Life cycle of control

| Step | Place and criterion |
|---|---|
| ACTIVE | The [feedback responsabil](../feedback/README.md) document keeps the reason, the order of the sample and the exact list of pathways. |
| VERIFIED | [Registrul tehnic](ATF_TestChangesBaseline.md) keeps the same ID and sample selection. The current diff authorization requires verified proof, basis and expiration. |
| DRAFT AND CLOSED | The structured entry may record the proposal or closure. These states have zero eligibility. |
| Retragere | Face control with the basis of the review. Remove the entrance only when the diff targeted no longer needs it. |

- The YAML block has the `testChanges` key and a blank list. Each item declares `id`, `status`, `base`, `expires`, `reason`, `proof` and `paths`. Field `allowDelete` is optional and implicitly false.
- Relative pathways use the `/` separator. Each ID shall be unique in the documents inspected.

The proof command expresses the planned verification and runs separately. [Component samples](../../atf-tests/src/test/java/atf/tests/governance/source/changes/TestChangeReviewComponentTest.java) builds local Git histories and qualifies the mechanism without device.

## Proof of verified control

Keep the control and its test selection in the commit being assessed. Write the proof to a new directory outside the checkout. The launcher builds an isolated copy and publishes evidence after verifying that its inputs did not change.

```powershell
.\scripts\run-component-suites.ps1 -AllTestChangeControls -TestChangeBaseRef $reviewBase -RunRoot $runRoot
$env:ATF_TEST_CHANGE_PROOF_ROOT = Join-Path $runRoot 'proofs'
.\scripts\run-atf-governance.ps1 -TestChangeBaseRef $reviewBase
```

| Element | Contract |
|---|---|
| Selectori | Declare checks.positive and negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative checks.negative check.positive checks.negative check.positive check.positive |
| Control negativ | The method injects the defect and affirms the expected refusal. In evidence, both the valid case and the case of refusal must have a PASS verdict at JUnit level. |
| Identitate | The receipt fixes the IDA, base SHAA, verified commit SHAA and the SHA-256 of the gross Git diff. Includes blobs and file modes. |
| Lot of diff | A single compilation feeds all required VERIFIED controls. Each control has its own JVM, log and HTML. The publication of the Director of `proofs` shall be made after the whole lot has passed and the selection and source has been reconfirmed. |
| Rulare | The launcher shall perform the exact selection declared and shall face the summons with Extent. The receipt and HTML are stored together in the directory with the control IDE. |
| Verification | Other commit, other diff, other control, missing or missed samples and HTML with different hash blocks the VERIFIED authorisation. A modified checkout requires a new candidate. |
| Istoric | Old controls are kept in the register. Using a VERIFIED control for the revised diff requires accurate selection and proof. |

When failing or changing source, see the logs per control and the `proofs.pending` diagnostic folder. For a spot test, `-TestChangeControl` keeps the receipt under `<RunRoot>/<ID>`. Reuse the receipt and report for the same commit and diff.

## Transferring ownership of a check

The transfer of an implementation to a library shall keep all native contracts to the manufacturer. The consumer shall retain the acceptance of the API used, the identity of the loaded JARs and the refusal of metadata or inconsistent deliveries.

- The request for change shall identify the affected sources, the new owner, the equivalent replacement and the independent acceptance criteria.
- The transfers and catches shall be linked to delivery. The technical check shall keep the base of Git and its paths in the register.
- Wiping the local copy does not suppress a negative case and does not transform a library check into an Android acceptance.
- The current documentation describes the liability limit without historical lists of deleted tests.

[Quality governance](README.md) checks the current perimeter and prohibits parallel sources of binary libraries.

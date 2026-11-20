# Re-use of governance and Code Cohesion

## Overview

The project owns Android-boundary and adapter checks. Reusable contracts are requested through the [library ownership register](README.md), without copying producer implementations into consumers.

## Cost of syntactic inspection

| Priority | Observation and source | Closure criterion |
|---|---|---|
| P2 | The [syntax parser](../../atf-tests/src/test/java/atf/tests/governance/source/JavaTestDeclarations.java) already disables annotation processing and uses empty class and processor paths. The cost of this configuration has not been measured against the same inventory. | Measure parsing cost with an identified environment and inventory. Preserve independent controls for composed annotations, ambiguous imports, syntax errors and parser lifecycle; do not claim a speedup from configuration alone. |


## Cohesion and package development

[Executable policy](../../config/atf-governance-policy.json) enforces the package limit without exceptions. [Package responsibilities](../governance/ATF_Packages.md) separates public APIs, shared support and test selections.

[Quality governance](../governance/README.md) · [Good documentation practices](../governance/ATF_Documentation.md)

## Qualification of claims and launchers

[RAG evaluator regressions](../testing/ATF_ReleaseQualification.md#evaluator-regression-tests) verify qualified-source attestations. [Release integrity governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) separately verifies APK and payload bytes in the recorded dossiers.

| Priority | Owner and gap | Verification level and closure criterion |
|---|---|---|
| P2 | Launcher coordination: entry points check preconditions inconsistently. | Controlled components for Java, Maven, Gradle, Node and Python must check required capabilities before costly operations and report missing capabilities with source identity. Distinguish the Python installation alias from an executable interpreter. For the [PowerShell ADB module](../../atf-test-android/tools/AndroidDeviceSetup.psm1), verify availability, refusal, timeout and process state separately from parsing and the Java client. |
| P2 | Qualification cost: snapshot preparation repeatedly copies and hashes retained reports, and the framework suite remains a coarse reuse unit. | Measure source hashing, release hashing, copying, JVM preparation and execution separately. Qualify any finer reuse through immutable input ownership and negative controls for changed or missing bytes; never relabel a historical execution or omit an affected case. |

[Inline-image retention governance](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/ReleaseEvidenceRetentionGovernanceTest.java) separately checks for duplicated PNG files beside HTML. It does not prove data autonomy, image readability or APK integrity; the [adapter criterion](ATF_Reporting.md#rpt-android-html-only---audit-delivery-without-auxiliary-copies) remains separate.

## Improving guidance guides

The [documentation editorial grid](../governance/ATF_Documentation.md#editorial-verification-and-evaluation) evaluates documents and their stated limits separately from code maturity. Scores and reviewed inventories belong to the external audit; this register retains only work that can improve comprehension or reproducibility.

| Priority | Owner and area | Closure criterion |
|---|---|---|
| P2 | Distributed library guides: producer-side sample reuse. | [Local example acceptance](../platform/ATF_Integration.md#verification-of-examples-in-guides) separates compilation, execution and verdict. Promote a generic evidence contract in the shared-ownership register and synchronize consumer guides. Producer regression and each consumer's acceptance remain separate. |

## Download via browser

[Distribution contract](../platform/ATF_AndroidDelivery.md#automatic-distribution-checks) distinguishes API checks, completed browser downloads and anonymous access. Record browser verification and the received file's hash in the [active release dossier](../../release/README.md).

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | Authenticated browser verification depends on an operator and a working GitHub session. | After publication, verify the version URL, received file and SHA-256, then open the audit at the commit linked in the release notes. Record anonymous responses and local browser restrictions separately. |

Android Download Manager transfer and certificate checks belong to the tested release dossier. [Android transfer contract](../platform/ATF_AndroidDelivery.md#separating-access-from-android-transfer) defines the required data; [anonymous public access](../platform/ATF_AndroidDelivery.md#anonymous-access-and-public-route) has a separate closure criterion.

## Native acceptance of distribution

| Priority | Bounded gap | Closure criterion |
|---|---|---|
| P2 | Native transfer evidence includes sources in Extent, but launching it still requires an operator-managed temporary operation. | Version a native adapter that reads candidate identity, owns its Android permissions and separates access from transfer. Inject 404, truncation, wrong hash or CRC, absent or mismatched certificates and timeouts. Keep original comparisons, causes and durations, omit signed redirects, and remove only owned resources. One native transfer qualifies delivery; do not repeat it for every UI resolution. |

## Release qualification boundaries

Native source and validator separation belongs to the [qualification contract](../testing/ATF_ReleaseQualification.md#native-execution-and-validator-provenance). Reuse remains conditional on unchanged runtime, scenario, APK and original evidence.

## Qualification cost and invocation recovery

Snapshot and cache validation consume time even when test execution is reused. Measure those costs separately from execution and lease waiting.

- Checkpoint each accepted invocation with its source, compiled dependencies, identity, original comparisons and report hashes.
- Resume unresolved invocations only after those immutable inputs are verified. Preserve rejected attempts as diagnostics.
- Prove parity of the complete JUnit identity set against a full run; reject changed sources, missing cases, duplicate identities and altered evidence.
- Keep timeout failures explicit until a real retry succeeds. A passing neighbouring case or a rewritten receipt is not replacement evidence.

## Platform-specific selection cost

The conservative fallback includes Web-only resources, retired browser packages and packaging tools in Android flow selection. Keep that safety boundary until dependency-aware component checks prove a narrower selection.

- Declare platform ownership for resources and packaging; unknown shared-runtime changes must still select every affected flow.
- Mock Web-only changes, removal of unused versions, Android menu changes and runtime dependency changes. Require a valid neighbor and a negative control for every exclusion.
- Measure inventory, copying, compilation, execution and device waiting separately. Keep the source-only RAG snapshot distinct from component workspaces containing the release catalog and build outputs.

## testChanges admission before collection preparation

Reject missing or stale source-control evidence before copying caches, compiling or launching a long collection. A late governance failure still protects correctness, but wastes the accepted preparation and unrelated test work.

- Discover the required control IDs from the reviewed Git diff and validate their external evidence root explicitly.
- Use the same proof validator for admission and final governance: control ID, base, commit, diff, positive and negative selectors, and original report hash must match.
- Generate unresolved controls with the existing compilation before starting the primary collection; pass the resulting root explicitly to the suite process.
- Cover absent configuration, another commit, a missing negative control, altered report bytes and a source change between admission and execution. Missing evidence must fail before Maven; changed sources must still invalidate admission.
- Measure rejected preparation separately from accepted execution. Do not replace a failed original report with a passing neighbour or edit an execution receipt.

- Make the review base an explicit input for local and isolated launches. Check the control plan before dependency preparation; an incompatible historical base must fail with the same diagnostic before compilation.

The isolated workflow now receives the review base explicitly. Admission of unresolved source-control evidence before dependency preparation remains open; selecting the right base alone does not close that boundary.

## Published audit commit admission

The publication flow can advertise a prepared dossier commit before its manifest contains the qualified source commit. Final governance rejects this link, but the public release then needs a metadata correction.

- Validate the audit commit before publishing its URL: require the qualified source, identical APK identity and canonical report hash, with a direct report link in the committed index.
- Cover prepared manifests with an absent source, another source commit, missing report bytes and a changed report hash through component tests.
- Preserve original execution receipts and published assets when correcting presentation metadata. Check the final GitHub release body and the committed dossier independently.
- Reuse the existing compiled governance selection for a metadata correction; do not repeat unchanged component or Android execution.

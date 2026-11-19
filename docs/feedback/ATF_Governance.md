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

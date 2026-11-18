# Qualification of local testing infrastructure

## Overview

The consumer owns API validation, process checks and local adoption; the [ownership register](README.md#common-test-support) routes generic Kit primitives. Module READMEs define implemented contracts; binary-library and native Android acceptance remain separate.

## Test kit concurrency

[Shared support](README.md#common-test-support) owns generic primitives and controlled tests. The consumer retains Android-adapter acceptance; transferring code does not qualify concurrency or close that criterion.

## Cost of validating API

| Priority | Gap and source | Verification level and closure criterion |
|---|---|---|
| P2 | [JsonSchemaCapabilities](../../atf-validation/src/main/java/atf/validation/contract/schema/JsonSchemaCapabilities.java) and [JsonSchemaValidator](../../atf-validation/src/main/java/atf/validation/contract/JsonSchemaValidator.java) declare no evaluation-depth or volume budget. | Use controlled depth, cardinality and repeated-rule inputs with an identified environment. Document explicit limits and rejection behavior in the contract. |

## Stability of process verification

### Disposable Android profile readiness

Durable ownership and recovery now belong to the [release qualification contract](../testing/ATF_ReleaseQualification.md#disposable-user-recovery). Controlled components qualify interruption and identity checks; native profile acceptance remains separate.

| Priority | Qualification limit | Closure criterion |
|---|---|---|
| P2 | [CommandProcessComponentTest](../../atf-test-android/src/test/java/atf/android/process/CommandProcessComponentTest.java) combines explicit synchronization with tight wall-clock thresholds. | Separate subprocess preparation from transport duration, and completion or truncation checks from latency qualification. Keep timeouts and negative controls; do not relax thresholds merely to obtain PASS. |
| P2 | [Mocked ADB preflight](../../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/scope/AndroidReleasePreflightComponentTest.java) includes PowerShell startup within the refusal timeout. Under load it can fail before the mock verdict. | Signal fixture readiness and transport refusal separately. Verify controlled clock and process behavior, plus a bounded test of the actual launcher. |

[Test kit](../../lib/README.TestKit.md#purpose-and-integration) · [Validation API](../../atf-validation/README.md) · [Governance](ATF_Governance.md)

## Launcher composition

| Priority | Gap and owner | Closure criterion |
|---|---|---|
| P2 | Android adapter: [AndroidUiDriver](../../atf-test-android/src/test/java/atf/android/e2e/driver/AndroidUiDriver.java) combines control selection, command retries and action reporting. Similar blocks make dispatch and timing changes costly. | Extract shared execution and reporting behind explicit ports while preserving the driver facade and domain oracle. Controlled process and clock tests must prove command order, original cause, duration and setup/body attribution, including timeout and cancellation. |

## RAG cost rating

| Step | Remaining criterion |
|---|---|
| Android | Measure lease waiting, profile preparation, transport and scenario execution separately. Correlate the active window to attribute interference from another game correctly. [Android acceptance](ATF_Android.md) |
| Reports | Measure evidence collection and handoff separately from execution on the same invocation inventory. [Reporting register](ATF_Reporting.md) |

The [RAG resume contract](../testing/ATF_ReleaseQualification.md#resume-validated-steps) defines checkpoints and snapshot reuse. Concrete execution timelines belong to the external run dossier.

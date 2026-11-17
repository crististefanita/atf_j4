# Integration of common libraries

## Overview

The consumer uses APIs delivered through declared dependencies with a verifiable binary identity. The application code and its adapters shall retain the responsibility of domain semantics.

## Integration limit

```mermaid
flowchart LR
    POM[Declared dependencies] -->|select| BINARY[JAR and metadata]
    BINARY -->|exposes| API[API public]
    ADAPTER[Adaptor consumer] -->|calls| API
    TEST[The term "Financial Account" means any Financial Account that is a Financial Institution that is a Financial Institution.] -->|checks| ADAPTER
```

<details>
<summary>Legend: contract consumed</summary>

The coordinates select delivery and the physical identity of the binary shall be checked separately. The consumer validates the contract on which it depends without introducing a copy of the generic implementation.

</details>

## Principii

[Consumption of common libraries](README.md) holds the binary inventory, physical identity and consumer-producer boundaries. The integration of an API follows a distinct route:

1. Identify the public contract used and the actual dependence in the POM.
2. Build the adapter with API types and the semantics of the explicitly preserved domain.
3. Check the nominal case and the refusal by independent consumer acceptance.
4. Reconcile the change of API with configuration, charged binary and affected adapters.
5. Direct generic change by [feedback](../feedback/README.md), keeping the acceptance of the contract used locally.

[CoreReportingConsumerContractTest](../../atf-tests/src/test/java/atf/tests/publishing/CoreReportingConsumerContractTest.java) checks for joint integration. The domain contracts and [publication audit](../reporting/ATF_Audit.md) add their own oracles.

## Verification of examples in guides

[LibraryReadmeExamplesIntegrationTest](../../atf-tests/src/test/java/atf/libraries/LibraryReadmeExamplesIntegrationTest.java) compiles the current Java fragments from the four guides, with the physical JARs from `lib` before the transitive dependencies. The execution has a temporary director of his own and compares observable results; negative controls distinguish the invalid compilation from the success of the example.

If a full temporary copy with JDK 21, modules and the Maven warehouse of its own:

```powershell
mvn -B -pl atf-tests -am '-Dtest=LibraryReadmeExamplesIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

The sample verifies consumer integration and documented fragments; does not change the JARI and does not replace the manufacturer's regression. [Responsabilul comun](../feedback/README.md) keeps packing and synchronizing the guides in the following deliveries.

## Competitive changes

An execution uses a copy of the build with sources and libraries identified at first. Their change in checkout does not retrospectively qualify the copy already executed.

The configuration, binary and sources shall be checked together. An equal textual version does not justify the use of different bytes under the same identity.

[Integration module](../../atf-tests/README.md) · [Publication of results](../reporting/README.md)

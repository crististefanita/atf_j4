# Integration and governance tests

## Overview

The module verifies the integration of common binaries, publication data and project contracts. [Common strategy](../../../docs/testing/README.md) defines the levels; here are chosen entries and their limits.

## Collections and responsibilities

[Catalogul](../../../config/atf-test-suites.json) declares the primary entries `AtfUnitSuite`, `AtfComponentSuite` and `AtfIntegrationSuite`. Reporting and governance selections are focused with explicit parents; they are not again over the primary inventory.

| Familie | Entry and limit |
|---|---|
| Publication Whist | [GameReportComponentSuite](java/atf/tests/reporting/GameReportComponentSuite.java) is a focused selection for the policy and data captured on the physical JARI. |
| Governance | [AtfQualityGovernanceSuite](java/atf/tests/governance/AtfQualityGovernanceSuite.java) reads the code, documents and contracts of binary; Android is not running. |
| Integritate release | [ReleaseIntegrityGovernanceTest](java/atf/tests/governance/documentation/release/ReleaseIntegrityGovernanceTest.java) checks the real folder; [probele mici](java/atf/tests/governance/documentation/release/integrity/) injects defects in temporary fixtures. |
| Igiena workspace | [WorkspaceHygieneGovernanceTest](java/atf/tests/governance/source/workspace/WorkspaceHygieneGovernanceTest.java) detects temporary directories and files generated in the root without erasing anything. |

## How to pick evidence

- Pure policies are checked unitary, with valid and invalid neighbours.
- Components exercise files, serialization, publication or actual JUnit cycle on controlled data.
- Binary integration verifies the physical surface and examples of guides; the identity of the library does not qualify for Android interaction.

In his own temporary copy, [module controls](../../README.md#focused-verification) choose the suites. The governance launcher shall maintain the inventory and refuse to assign the result to sources exchanged in parallel.

## Reading of the result

A controlled failure qualifies the validator's refusal without becoming a new failure of the application. The component report shall publish the data evaluated; the results of an execution and audit notes shall be the external file or the identified relay, in accordance with the common strategy.

[Integration module](../../README.md) · [Common strategy](../../../docs/testing/README.md) · [Governance](../../../docs/governance/README.md)

# Validation contract tests

## Overview

The module faces the observed response with a contract, keeping the differences and the original cause. [Common strategy](../../../docs/testing/README.md) separates pure policy from integration with documents and reporting.

## Checked properties

| Family | Oracle and border |
|---|---|
| Equality and result | Type of value, difference observed and verdict of policy. |
| Contract of Response | JSON document controlled, mandatory fields and localized messages. |
| Scheme | The declared capabilities and refusal of the unaccepted rules without turning the lack of support into success. |
| Evidence | Preview, drafting and preserving the values necessary to explain the failure. |

## Execution and limits

[Catalog](../../../config/atf-test-suites.json) separate `ValidationUnitSuite` ed `ValidationComponentSuite`. [Shared launcher](../../../scripts/run-component-suites.ps1) publish only the selection of the component; the units shall retain their separate input and result.

- Use [input module](../../README.md) in a temporary copy of the dependencies identified.
- The negative samples alter a single relevant condition and keep the witness valid; a defect in the fixture does not demonstrate the requested refusal.

Reporting tests check the data transmitted, separate from the renderer's acceptance. [Budget of evaluation](../../../docs/feedback/ATF_TestInfrastructure.md#cost-of-validating-api) remains a separate qualification from functional correctness.

[Validation API](../../README.md) · [Common strategy](../../../docs/testing/README.md)

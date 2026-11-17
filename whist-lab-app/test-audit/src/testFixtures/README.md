# Common fixtures for Gradle audit

## Overview

This set of sources provides test infrastructure for Gradle modules. The component tests choose the inputs and the oracles and the adapter records the comparisons and confronts the ratio with JUnit invoicing. [Common strategy](../../../../docs/testing/README.md)

| Responsibility | Contract |
|---|---|
| Capture | Retains the actual value and capture time for each declared input. |
| Comparison | It associates every check with the citation that assessed it. |
| Verification | Confront identities and results between HTML and JUnit. |

The fixtures enter the consumer test variants. Their own verification is in [component catalogue](../test/README.md).

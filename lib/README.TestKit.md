# Test Kit - classification and audit JUnit

## Overview

Kit offers annotations, evaluated comparisons and neutral lifecycle. JAR does not start browser, broker or ADB and does not publish Extent alone.

| Need | API and limit |
|---|---|
| Test level | @ AtfUnitTest, @ AtfComponentTest, @ AtfIntegrationTest, @ AtfE2eTest; classification, without automatic launch. |
| Auditable comparisons | RecordedAssertions; JUnit's verdict precedes the 240, preview that keeps the Unicode pairs complete. |
| Existing JUnit Assertions | AuditedAssertions in the adapter scope; diagnosis lazy, identity and original cause. Direct JUnit calls are not automatically captured in the audit. |
| Complete values | The AssertionValue Display can receive an observer Projection with a role of expected/actual and text drawn up before preview; the consumer holds the retention. |
| Phases and cleanup | ComponentAuditExtension / ComponentAuditAdapter, TestRunInitialization, FailureAwareCleanup. |
| Policies and resources | Atf Resolvers * Policy; readResourceUtf8 for classpath/JAR, requiredPath for actual file. |

## Minimal example

The adapter holds the recorder and publishes the result; a mismatch retains the original JUnit exception:

```java
import atf.testkit.assertions.RecordedAssertions;

public static void verifyState(RecordedAssertions audit, String actual) {
    audit.equals("State", "ready", actual);
}
```

## Purpose and integration

- Addiction atf.api: atf-test-kit with atf.testkit.version of the project POM. Use the scope test in the product; compile is justified only for the main support of a test module. Do not include Kit in APK/aplicatie.
- JUnit Jupiter API and Jackson are transitive. Runner declares engine junit-jupiter-engine and its selection; level annotation does not replace @ Test or consumer extension.
- For Extent: the adapter sends the checks to the AssertionAuditReport; AssertionAuditRun holds the joint session. Kit does not copy the renderer. [Integration Reporting](README.Reporting.md#purpose-and-integration).
- The Sanitizer is injected via the AssertionValue Display prior to preview. The comparison runs once; the stored values are limited and do not constitute full archive. Pure units keep their own policy.
- Purposes and resources are per invoicing; close them and keep the original cause. Global policies do not become isolated between external readers through a local lock.

---

The tests of this project fix the integration used, the lifecycle and the causes of observable failure. Keep for any incompatibility version and hash JAR/POM, minimum configuration and result; local process described in [Guide libraries](README.md#maintenance-and-feedback).

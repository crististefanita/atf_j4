# Core - joint contracts and mechanisms

## Overview

`atf-core` provides the ATF's vocabulary and reusable mechanisms. Use it to compose adapters and executions, keeping the application rules in the consumer.

| You need | Start API |
|---|---|
| HTTP applications, replies and execution | `ApiRequest`, `ApiResponse`, `ApiTransport`, `ApiExecutor` |
| Configuration captured for a rolling | `AtfConfiguration`, `ApiSettingsSnapshot`, `AtfRuntimeSnapshot` |
| JSON strict and drafting sensitive data | `JsonDocuments`, `SensitiveDataRedactor` |
| Releasing resources in reverse order | `CleanupRegistry` |
| Selective timing with baseline verification | `AtfPromotionManifest`, `AtfPromotionGuard` |

## Minimal example

Fragment in a method that can propagate `IOException`; build data without sending a request:

```java
import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiUrlBuilder;
import atf.core.json.JsonDocuments;
import java.net.URI;

var request = ApiRequest.of("GET", "/orders/42");
var url = new ApiUrlBuilder(URI.create("https://example.test/api"))
        .build(request);
var body = JsonDocuments.read("{\"total\":19.90}");
var total = body.path("total").decimalValue();
```

`url` is `https://example.test/api/orders/42`. `JsonDocuments` rejects duplicate keys and the content left behind; decimal numbers do not pass through `double`.

## Use in ATF

- Inject a `ApiTransport` in `ApiExecutor`; actual transport or muck is explicitly chosen for composition.
- `HttpClientTransport` and `UrlConnectionTransport` propagate cancellation to the operation held; the fallback includes the body in the deadline. Cancellation does not prove rollback and does not authorise rephrase.
- Reuse the running configuration snapshots; the oracle, assertions and business verdict belong to the consumer.
- Keep the original model for comparison; the drafting and preview apply to the publication of the diagnosis.
- Core exposes neutral ports; [Console](README.Console.md), [Mock](README.Mock.md) and [Reporting](README.Reporting.md) they have distinct roles.

## Verification of use

| Action | Observed result |
|---|---|
| Compile the fragment with JDK 21 and the dependencies in the POM. | The types and methods shall be resolved from the chosen binary delivery. |
| Execute the fragment. | The URL is based on and the path of the application; `total` is numerically equal to `19.90` by `BigDecimal`. |
| Provide duplicate or root content JSON keys. | The parser rejects the document without partially accepted result. |

The example builds local data; does not execute a HTTP call. The transport check shall be made separately by means of the actual adapter or explicitly selected muck.

---

The Core is an external binary fixed in the POM and the home register. This guide only holds the contract consumed here; report an incompatibility with version, hash, minimum fragment and observed result. [Local procedure](README.md#maintenance-and-feedback)

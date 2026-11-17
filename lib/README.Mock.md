# Mock — transport HTTP determinist

## Overview

`atf-mock` replaces Core `ApiTransport` port with a local catalogue of responses. Use it for offline components and contracts with controlled inputs and an explicit fallback.

## Exemplu minim

`mock.yaml` in the working directory:

```yaml
mock:
  routes:
    - {method: GET, path: /health, status: 200, body: '{"status":"ready"}'}
  fallback: {status: 500, body: '{}'}
```

Fragment in a method; the transport remains offline:

```java
import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockTransport;
import java.nio.file.Path;

var mock = AtfMockTransport.fromConfiguration(Path.of("mock.yaml"));
var response = mock.send(ApiRequest.of("GET", "/health"),
        "https://offline.test/health", "none").join();
```

`response.status()` is `200`; an unrouted application receives the `500` fallback. The test independently compares the response with the expected result.

## Catalogue and Matching

| Necesitate | API or contract |
|---|---|
| Catalog YAML explicit | `AtfMockTransport.fromConfiguration(Path)` |
| Project configuration | `fromProjectConfiguration()`: `atf.mock.config` > `ATF_MOCK_CONFIG` > `atf-mock.yaml/yml` |
| Requiest conditions | Method, path, auth profile, query, requested heads and body expected in `MockRoute` |
| Corp de request | Input mode `json`, negative sample MIME `wrong-content-type`; numerical fit without conversion by `double` |
| Query repetat | `ApiQueryParameters`; includes the required values, multiplication |

- Without configuration, `fromProjectConfiguration()` uses the `defaults()` fixture; the application catalogue remains local.
- The en route duration is metadata of the response, not a simulated real delay.
- A mock sample shall verify integration; the acceptance of the service, broker or actual device shall remain separate.

## Verification of use

| Action | Rezultat observabil |
|---|---|
| Save YAML as `mock.yaml`, in the trial director. | The example reads exactly that local catalogue. |
| Send application `GET /health`. | The response has status `200`; the body corresponds to the configured route. |
| Send a route without route to the same catalogue. | The response has the fallback status `500`. |

Address `offline.test` is the context of the application for mock transport. The successful sample qualifies the matching of the catalogue and the contract consumed without confirming the availability of an external service.

---

Mock is an external binary fixed in the POM and the home register. The tests here check the route and the fallback used by the project; a defect is reported with the version, hashness, minimum catalogue and the expected / observed response in [registrul local](../docs/feedback/README.md).

# J4 acceptance and architecture catalog

This J4-owned module verifies consumer compatibility with the shared Core and
Reporting binaries and publishes the combined component/Android Extent report.
It does not copy or own their implementation; source-level Core/Reporting tests
remain in J0.

## Consumer contracts

`CoreReportingConsumerContractTest` checks the Core duration parser and writes a
small Extent HTML report through Reporting J10. Run these checks with:

```powershell
mvn -pl atf-tests test
```

## Aggregate Extent publisher

`DeepSuiteExtentReportTest` is opt-in. It reads the Surefire XML/text outputs
from each module and the PNG/XML emulator evidence, then publishes one Extent
report with UI checkpoints and inline Base64 screenshots through the J10 UI
reporting API. Use
[`scripts/run-deep-tests.ps1`](../scripts/run-deep-tests.ps1) to run every
component separately, execute the app checks, collect results, and invoke the
publisher. The emulator tests need a booted device and host ADB bridge; scrcpy
does not need to be open.

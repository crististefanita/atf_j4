# ATF J4 — Android / Contract Whist

J4 reuses the shared ATF foundation for local Android acceptance testing. The
current target is Contract Whist (`com.qsoft.android.whist`) in the Android
emulator hosted by Podman on the physical machine and displayed in the Windows
VM.

## Reused foundation

- `atf-test-kit`, `atf-mock`, `atf-validation`, `atf-console`: source snapshot
  selected from J3 after comparing J1/J2/J3 and checking that the shared Java
  sources match the current J0 baseline.
- `lib/atf-core-1.0.0-c2.{jar,pom}` and
  `lib/atf-reporting-1.0.0-j10.{jar,pom}`: binary consumer dependencies. J4
  records their hashes and source lineage in `library-provenance.json`.
- Core and Reporting implementation and contract tests remain owned by J0.
  J4 owns only Android integration and consumer-specific report composition.

See [Android test notes](docs/ATF_Android_Whist_Testing.md) for the VM/host
connection, what was actually exercised, and known display limitations.

## Modules

- `atf-android`: bounded ADB process adapter with explicit device serial.
- `atf-test-android`: opt-in Contract Whist Android UI acceptance check.
- `atf-tests`: J4-owned cross-module acceptance and architecture catalog.
- The four copied shared foundation modules and Core/Reporting binary pairs.

## Component and Android tests

Requires JDK 21 and Maven. The default build does not contact the emulator.
For the complete headless run, with the Podman emulator already booted and the
app installed, run:

```powershell
.\scripts\run-deep-tests.ps1
```

The script runs each module's component tests, then the named Android component
and live-gameplay suites. `scrcpy` can stay closed; the emulator and host ADB
bridge must remain up.
It saves a timestamped run folder under `artifacts/deep-app-test/runs/` and
publishes one Extent HTML report. Android screenshots are attached to named UI
checkpoints through Reporting J10, with inline Base64 data URIs; Surefire XML,
text output, and UI hierarchy files remain linked as diagnostics.

Each run writes final-extent-report.html inside its timestamped run folder. The
latest verified run is artifacts/deep-app-test/runs/20261101-164823/: both
Android E2E tests passed, and the Extent report contains 13 inline PNG Base64
data URIs generated through Reporting J10. The report publisher contract test
also passed against the exact screenshot payloads. See the [final Extent
report](artifacts/deep-app-test/runs/20261101-164823/final-extent-report.html)
and [Android test notes](docs/ATF_Android_Whist_Testing.md) for coverage and
limits.

The app has a serious visual rendering problem on this emulator (neon-green
table/background, oversized and overlapping content, and hand cards clipped at
the bottom). The Extent report marks visual acceptance as WARNING and contains
the screenshots and UI trees. Multiplayer was not entered and full-match
scoring was not tested. The app was not reinstalled, and the Google account was
not modified.

For the package layout, activation properties, report flow, and standalone
commands, see [Android test organization](docs/ANDROID_TESTING.md).

When adding a new business consumer, copy only the specific adapter or tests
it needs. Keep generic harness, backend contracts, and Reporting implementation
in their owning modules.

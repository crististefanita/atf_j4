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
  `lib/atf-reporting-1.0.0-j9.{jar,pom}`: binary consumer dependencies. J4
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

## Build and device check

Requires JDK 21 and Maven. Run from this directory:

```powershell
mvn package
```

The Android device check is deliberately skipped unless enabled. To run it after
the emulator is available:

```powershell
$env:ADB_SERVER_SOCKET='tcp:192.168.56.1:5038'
$env:ATF_ANDROID_ADB='X:\scrcpy-win64-v5.0.1\adb.exe'
$env:ATF_ANDROID_SERIAL='emulator-5554'
mvn -pl atf-test-android -am test "-Datf.android.enabled=true"
```

The test requires the package to have already been installed through Play
Store. It does not install apps or authenticate accounts. It checks the menu,
instructions, high-score name prompt, game setup selectors, bidding, and one
hand-card interaction, and resume rendering for six seconds after app restart.
A run stores UI hierarchies, screenshots, and a Reporting J9 HTML report in
`atf-test-android/target/android-artifacts/`. The captured run
from 1 November 2026 is preserved in
[`artifacts/deep-app-test/`](artifacts/deep-app-test/android-gameplay.html).
Do not collect logcat by default: it can contain personal or account data.

The app has a serious visual rendering problem on this emulator (neon-green
table/background, oversized and overlapping content, and hand cards clipped at
the bottom). The interaction test passes, but the screenshots are not a visual
acceptance pass. Multiplayer and full-game scoring are not covered yet.
The Multiplayer entry currently prompts to edit the player name; the probe
pressed Cancel and did not enter an online session.

When adding a new business consumer, copy only the specific adapter or tests
it needs. Keep generic harness, backend contracts, and Reporting implementation
in their owning modules.

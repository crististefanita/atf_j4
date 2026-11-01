# Android test organization

J4 follows the component/E2E split used by J1–J3 and keeps Android-specific
work in J4.

## Test layers

- **Component:** `atf-android` owns `AdbClient` and the screenshot evidence
  adapter. `atf.android.suites.AndroidComponentSuite` runs these contracts
  locally and does not need an emulator.
- **E2E:** `atf-test-android` owns Contract Whist original-app flows and the
  Whist Lab local-app adapter under `atf.android.e2e`.
  `ContractWhistE2eSuite` targets the original app; serialized
  `AndroidGameplayE2eSuite` runs both apps against the shared emulator.
- **Final report:** `atf-tests` summarizes Surefire results, diagnostics, the
  live-game outcome, and visual findings in one Extent report.

## Running

The ordinary Maven build does not contact a device. Run the local adapter suite
with:

```powershell
mvn -pl atf-android -Dtest=AndroidComponentSuite test
```

Run the original app flow only when the emulator is booted and ADB is reachable:

```powershell
mvn -pl atf-test-android -Dtest=ContractWhistE2eSuite `
  -Datf.android.enabled=true `
  -Datf.android.serial=emulator-5554 `
-Datf.android.commandTimeoutSeconds=90 `
  -Datf.android.artifacts=artifacts/android `
  test
```

Run the combined original and local app acceptance suite (install Whist Lab
first, or use the full runner below):

```powershell
mvn -pl atf-test-android -Dtest=AndroidGameplayE2eSuite `
  -Datf.android.enabled=true `
  -Datf.android.serial=emulator-5554 `
  -Datf.android.artifacts=artifacts/android `
  test
```

The standard full run is `scripts/run-deep-tests.ps1`. It builds the Whist Lab
core and APK, installs the APK, configures the host ADB bridge, runs module and
named-suite results for both apps, stores timestamped evidence, and publishes
the aggregate report. `scrcpy` is optional; Android and ADB must stay
available. The remote host ADB bridge gets a 90-second command timeout by
default; override it with `-AdbTimeoutSeconds`.

## Screenshot/report contract

`AndroidScreenshotEvidence` turns each device PNG into a validated
`ReportScreenshot` (checkpoint title plus Base64 PNG). The Android test sends
that screenshot through `UiExtentReportSession` and the J10 `ExtentReportSession`
as a normal UI step. Reporting owns its inline media markup and status. J4 does
not append HTML or inject a separate image gallery after the report is closed.

Original PNG and UI Automator XML files stay under the run's evidence folder for
debugging. The HTML embeds each PNG inline in the corresponding checkpoint.

## Current product coverage

The original live flow checks menu content, instructions and scoring copy,
setup options, bidding, two human card plays separated by an AI turn, and
saved-game rendering after settling. Whist Lab runs a deterministic full trick,
verifies the exact-bid score, and resumes an in-progress trick after the Android
process is force-stopped. Its pure Java tests simulate complete descending
matches for 3–6 players and restore checkpoints from bidding, mid-trick, and
score phases. The original app's full match and computed score are not yet
verified; multiplayer remains untested. The original app's display defects
remain a WARNING in the report.

## Latest verified run

Run 20261101-195037 completed on 2026-11-01. The Android component suite
passed 5/5 contracts. The combined Android E2E suite passed 3/3 flows: the
original app passed both menu/setup/gameplay and saved-game rendering checks;
Whist Lab passed the exact-bid trick after a force-stop and resume. The setup
test inspects every player-count, game-type, and AI choice, closes selectors
without changing them, and confirms the starting selections remain intact.

The J4 reactor produced 494 Surefire cases: 493 passed and one report publisher
test was intentionally skipped in the ordinary module pass. The publisher then
ran explicitly and passed. Separately, Whist Lab's Gradle core suite passed
20/20 tests and `:app:assembleDebug` succeeded. The final Extent report is
7,076,637 bytes and contains 15 inline PNG Base64 images; the run folder retains
the PNGs, UI hierarchy XML, module XML/TXT results, and logs. The report
includes the verified `You | Bid 1 | Won 1 | +11 | Total: 11` row.

The original app's display defect warning remains: cards and content are
clipped or oversized on this emulator. Its full match, computed score, and
multiplayer session were not verified.

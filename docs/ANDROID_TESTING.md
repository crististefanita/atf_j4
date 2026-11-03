# Android test organization

J4 follows the component/E2E split used by J1–J3 and keeps Android-specific work in J4.

## Test layers

- **Component:** `atf-android` owns `AdbClient` and screenshot evidence. `AndroidComponentSuite` runs without an emulator.
- **E2E:** `atf-test-android` owns Contract Whist and Whist Lab UI flows. The parallel app project is nested at `whist-lab-app/`.
- **Final report:** `atf-tests` aggregates Surefire results, UI flow manifests, diagnostics, visual observations, and inline screenshots through Reporting J12.

## Current complete-game acceptance

The [acceptance record](COMPLETE_GAME_ACCEPTANCE.md) binds the final report,
package hashes, test inventories, reproduction and limits. Original Oh Hell
and Whist Lab Android each completed 19 hands, 109 tricks and 436 card plays,
with every state checked independently and final totals `[89,126,145,119]`.
The exact original inputs also passed the pure Java replay. Normal Romanian
Whist + Expert initially passed one unscripted hand and the next transition.
The subsequent [normal Romanian Expert acceptance](ROMANIAN_EXPERT_ACCEPTANCE.md)
passed all 24 hands, 94 tricks and 376 cards on Android, ending at
`[12,87,91,61]`, with an offline audit of 260 retained snapshots.

That separate run has 233 accepted cases, 11,047 UI assertions and 287 screenshot
checkpoints in native Extent, with every PNG inline Base64. It also expands
the game/AI components to 210; the original/replay run below retains its own
dated inventory and package provenance.

There are 219 accepted JUnit/Gradle cases, zero failures/errors/skips, with
publishers separate. The Android Extent report has 6,655 assertions, 581
action/observation records and 325 screenshot checkpoints. Every PNG remains
inline Base64. Functional acceptance is PASS and original rendering is WARNING.
The native HTML's browser visual audit was blocked by MCP's local-file policy;
automated data/image fidelity checks do not substitute for a DOM visual audit.

Run component acceptance first, then complete-game acceptance:

```powershell
.\scripts\run-rule-components.ps1
.\scripts\run-complete-match.ps1 -Serial '127.0.0.1:5555'
```

The current ADB client is `C:\tools\whist-android-sdk\platform-tools\adb.exe`,
with `ADB_SERVER_SOCKET=tcp:192.168.56.1:5038`. The serial after host restart is
`127.0.0.1:5555`; older examples below retain their historical serial.

After a host restart, connect the loopback serial through the host ADB server
before issuing device commands:

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
$adb = 'C:\tools\whist-android-sdk\platform-tools\adb.exe'
& $adb connect 127.0.0.1:5555
& $adb -s 127.0.0.1:5555 get-state
```

J4 live runners now perform this preflight automatically. Each ADB command has a
15-second bound; a missing host bridge stops the run before install or UI work.

## Running

The ordinary Maven build does not contact a device. Run the Android adapter contracts with:

```powershell
mvn -pl atf-android -Dtest=AndroidComponentSuite test
```

Run the original app flow when the emulator is booted and ADB is reachable:

```powershell
mvn -pl atf-test-android -Dtest=ContractWhistE2eSuite `
  -Datf.android.enabled=true `
  -Datf.android.adb=C:\tools\whist-android-sdk\platform-tools\adb.exe `
  -Datf.android.serial=127.0.0.1:5555 `
  -Datf.android.adbServerSocket=tcp:192.168.56.1:5038 `
  -Datf.android.commandTimeoutSeconds=90 `
  -Datf.android.artifacts=C:\work\whist_back\deep-app-test\probes\original-app `
  test
```

The standard full run is `scripts/run-deep-tests.ps1`. It builds `game-core` and the APK, installs Whist Lab, configures the host ADB bridge, runs all component and Android suites, and publishes one aggregate report. `scrcpy` can stay closed. Android and ADB must stay available.

The live runner uses the local SDK client because `X:` is not mounted in this
shell. Its default serial is the current host-forwarded endpoint:

```powershell
.\scripts\run-deep-tests.ps1 `
  -AdbExecutable 'C:\tools\whist-android-sdk\platform-tools\adb.exe' `
  -Serial '127.0.0.1:5555' -AdbServerSocket 'tcp:192.168.56.1:5038'
```

The runner's default evidence root is `C:\work\whist_back\deep-app-test\runs\<run-id>`, outside the J4 source tree. Override it with `-EvidenceRoot` if needed.

## Screenshot/report contract

`AndroidScreenshotEvidence` validates each device PNG as a `ReportScreenshot`. Live flows write atomic manifests with `SETUP` / `TEST BODY` / `TEARDOWN` phases, ordered Gherkin checkpoints, actions, assertions, and image paths. Individual Whist Lab flows publish through the J18 `ExtentReportSession` and `UiExtentReportSession` API: every checkpoint is one expandable Gherkin UI step containing the standard collapsed action table, its inline full-display PNG, and its evaluated assertion table. `ACTION` records measure the ADB dispatch only; the settling wait is excluded, and the later UI result stays in assertions/observations. The receipt identifies the run and checkpoint/screenshot counts; supporting hierarchy XML and manifests are represented by collapsed SHA-256 fingerprints, without workstation file links or raw dumps. A failure before the first checkpoint remains an explicit diagnostic in the report. The aggregate `atf-tests` report is a separate publication path; J4 does not append HTML or inject a second screenshot gallery after publication.

## Earlier discovery coverage — historical

The original app flows cover menu and full instruction text, High Scores state, every player/game/AI setup option, bid grids at 3–6 seats, saved-game rendering after resume, and a guarded 4-player Oh Hell hand-end. The score flow selects human `Bid 0`, stops probing as soon as `Next Hand` appears, and verifies that the following hand exposes `Bid 0` through `Bid 9` with `Bid 10` absent. The adapter's raster interaction cycles do not identify card faces or prove the exact number of accepted human cards.

That discovery run covered deterministic exact-bid scoring, process-death resume and default four-player Oh Hell setup in Whist Lab. Full-match, winner and numeric-score limitations of this baseline are superseded by the current acceptance above. Multiplayer remains untested.

## Discovery baseline — 1 November

Run `20261101-222629` completed on 2026-11-01. Android component contracts passed 6/6. Android E2E passed 6/6 tests: four original-app flows and two Whist Lab flows. The J4 reactor produced 498 Surefire cases: 497 passed, zero failures/errors, and one opt-in publisher test skipped in the ordinary module pass. The Extent publisher then ran explicitly and passed. Whist Lab's Gradle core passed 29/29 tests; `:app:assembleDebug` succeeded and the APK installed on API 28.

The final Extent report is 16,805,609 bytes with 42 inline PNG Base64 data URIs. Its run folder retains 56 PNGs, 50 UI hierarchy XML files, four UI flow manifests, module results, and logs:

`C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html`

The first-hand score sheet was manually transcribed as Player 5, Hilary 0, Hubert 1, and Phyllis 4. The row sums to 10, conditionally consistent with ten trick points and no exact-bid bonus. This does not verify each player's tricks or bids. In the next hand, the hierarchy lists the complete `Bid 0`–`Bid 9` domain and disabled `Bid 1`; the screenshot shows 9 cards and previous bids totaling 8, so `8 + 1 = 9` reconciles with the last-bid exclusion rule. The visible row and bidding counters are manual visual evidence, not OCR output.

The discovery report remains WARNING because the original app has severe display defects: neon-green background, oversized/overlapping labels, distorted cards, and clipped hand cards. The later complete-game report preserves the rendering WARNING and proves completion, trick winners and scores. Multiplayer remains open.

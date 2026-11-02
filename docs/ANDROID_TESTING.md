# Android test organization

J4 follows the component/E2E split used by J1–J3 and keeps Android-specific work in J4.

## Test layers

- **Component:** `atf-android` owns `AdbClient` and screenshot evidence. `AndroidComponentSuite` runs without an emulator.
- **E2E:** `atf-test-android` owns Contract Whist and Whist Lab UI flows. The parallel app project is nested at `whist-lab-app/`.
- **Final report:** `atf-tests` aggregates Surefire results, UI flow manifests, diagnostics, visual observations, and inline screenshots through Reporting J10.

## Running

The ordinary Maven build does not contact a device. Run the Android adapter contracts with:

```powershell
mvn -pl atf-android -Dtest=AndroidComponentSuite test
```

Run the original app flow when the emulator is booted and ADB is reachable:

```powershell
mvn -pl atf-test-android -Dtest=ContractWhistE2eSuite `
  -Datf.android.enabled=true `
  -Datf.android.serial=emulator-5554 `
  -Datf.android.commandTimeoutSeconds=90 `
  -Datf.android.artifacts=C:\work\whist_back\deep-app-test\probes\original-app `
  test
```

The standard full run is `scripts/run-deep-tests.ps1`. It builds `game-core` and the APK, installs Whist Lab, configures the host ADB bridge, runs all component and Android suites, and publishes one aggregate report. `scrcpy` can stay closed. Android and ADB must stay available.

This VM run used the local SDK client because `X:` was not mounted in the shell:

```powershell
.\scripts\run-deep-tests.ps1 `
  -AdbExecutable 'C:\tools\whist-android-sdk\platform-tools\adb.exe' `
  -AdbServerSocket 'tcp:192.168.56.1:5038'
```

The runner's default evidence root is `C:\work\whist_back\deep-app-test\runs\<run-id>`, outside the J4 source tree. Override it with `-EvidenceRoot` if needed.

## Screenshot/report contract

`AndroidScreenshotEvidence` validates each device PNG as a `ReportScreenshot`. Live flows write atomic manifests with `SETUP` / `TEST BODY` / `TEARDOWN` phases, ordered Gherkin checkpoints, actions, assertions, and image paths. The publisher consumes those manifests through J10 `ExtentReportSession` and `UiExtentReportSession`; each PNG is attached inline as Base64 to its checkpoint. UI hierarchy XML and original PNGs remain as linked audit evidence. J4 does not append HTML or inject a second gallery after publication.

## Current product coverage

The original app flows cover menu and full instruction text, High Scores state, every player/game/AI setup option, bid grids at 3–6 seats, saved-game rendering after resume, and a guarded 4-player Oh Hell hand-end. The score flow selects human `Bid 0`, stops probing as soon as `Next Hand` appears, and verifies that the following hand exposes `Bid 0` through `Bid 9` with `Bid 10` absent. The adapter's raster interaction cycles do not identify card faces or prove the exact number of accepted human cards.

Whist Lab covers deterministic exact-bid scoring, process-death resume, and its default four-player Oh Hell setup. `game-core` simulates complete descending matches for 3–6 players and tests checkpoint restoration from bidding, mid-trick, and score phases. The original app's full match, individual trick winners, and numeric score formula remain unverified; multiplayer is untested.

## Latest verified run

Run `20261101-222629` completed on 2026-11-01. Android component contracts passed 6/6. Android E2E passed 6/6 tests: four original-app flows and two Whist Lab flows. The J4 reactor produced 498 Surefire cases: 497 passed, zero failures/errors, and one opt-in publisher test skipped in the ordinary module pass. The Extent publisher then ran explicitly and passed. Whist Lab's Gradle core passed 29/29 tests; `:app:assembleDebug` succeeded and the APK installed on API 28.

The final Extent report is 16,805,609 bytes with 42 inline PNG Base64 data URIs. Its run folder retains 56 PNGs, 50 UI hierarchy XML files, four UI flow manifests, module results, and logs:

`C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html`

The first-hand score sheet was manually transcribed as Player 5, Hilary 0, Hubert 1, and Phyllis 4. The row sums to 10, conditionally consistent with ten trick points and no exact-bid bonus. This does not verify each player's tricks or bids. In the next hand, the hierarchy lists the complete `Bid 0`–`Bid 9` domain and disabled `Bid 1`; the screenshot shows 9 cards and previous bids totaling 8, so `8 + 1 = 9` reconciles with the last-bid exclusion rule. The visible row and bidding counters are manual visual evidence, not OCR output.

The report remains WARNING because the original app has severe display defects: neon-green background, oversized/overlapping labels, distorted cards, and clipped hand cards. Full-match completion, repeated controlled scoring cases, trick winner decoding, and multiplayer remain open.

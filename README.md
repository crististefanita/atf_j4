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
  `lib/atf-reporting-1.0.0-j12.{jar,pom}`: binary consumer dependencies. J4
  records their hashes and source lineage in `library-provenance.json`.
- Core and Reporting implementation and contract tests remain owned by J0.
  J4 owns only Android integration and consumer-specific report composition.

See [Android test notes](docs/ATF_Android_Whist_Testing.md) for the VM/host
connection, what was actually exercised, and known display limitations.

## Rules and component-first development

Whist Lab now has an explicit `ROMANIAN_WHIST` profile alongside `OH_HELL`.
Romanian Whist requires trump when void, uses a reduced deck and a 1–8–1
schedule, and scores exact contracts as `5 + tricks`, otherwise minus the bid
error. These rules must not replace the distinct original-app Oh Hell oracle.
The [game-core README](whist-lab-app/game-core/README.md) maps each rule to
sources, component cases, exhaustive domains, independent full-game checks,
and the Expert AI's information boundary and paired benchmark.

```powershell
.\scripts\run-rule-components.ps1
```

This offline pipeline runs the rules and AI components, measures JaCoCo
coverage, builds the APK and publishes native Extent J11. It does not start or
install into an emulator. Evidence and the report go outside J4, under
`C:\work\whist_back\rules-components\<run>\`. Android acceptance follows
component acceptance; its screenshots remain exact inline Base64 PNGs.

The 2 November component acceptance is
`C:\work\whist_back\rules-components\20261102-card-acceptance\component-extent-report.html`:
187 component/benchmark cases passed, plus the explicitly run publisher.
The independent model checked 88 games and 40,304 card plays. GameRules reached
100% measured lines/branches; the complete game-core reached 97.22% / 91.07%.
Expert outscored Hard at every supported table size across 144 paired
comparisons. Exact results and reproduction are in the game-core README.

## Modules

- `atf-android`: bounded ADB process adapter with explicit device serial.
- `atf-test-android`: opt-in Contract Whist Android UI acceptance check.
- `atf-tests`: J4-owned cross-module acceptance and architecture catalog.
- `whist-lab-app/`: the local parallel Android app and its independently tested
  `game-core` rules engine.
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
It saves evidence outside the source tree under
`C:\work\whist_back\deep-app-test\runs\` and
publishes one Extent HTML report. Android screenshots are attached to named UI
checkpoints through Reporting J12, with inline Base64 data URIs; Surefire XML,
text output, and UI hierarchy files remain linked as diagnostics.

The latest clean full run is
`C:\work\whist_back\deep-app-test\runs\20261101-222629\`. J4 reported 498
Surefire cases: 497 passed, no failures or errors, and one opt-in publisher test
skipped in the ordinary module pass. The publisher then ran explicitly and
passed. Android acceptance passed 6/6 flows (4 original-app flows and 2 Whist
Lab flows); the Android adapter component suite passed 6/6. Whist Lab's
`game-core` passed 29/29 Gradle tests and the debug APK built and installed.

The final Extent report is 16,805,609 bytes with 42 inline PNG Base64 data URIs;
the run folder retains the original PNGs, UI hierarchies, Surefire results, and
logs. It records the original app's 10-card score sheet and verifies that
`Next Hand` opens a 9-card bid range from 0 through 9. Score cells are manual
visual evidence and the arithmetic is explicitly conditional; card identities,
trick winners, and the original app's full match are not machine-verified.
See `C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html`
and [Android test notes](docs/ATF_Android_Whist_Testing.md) for the report and
limits.

The original app has serious display defects on this emulator: neon-green
background, oversized/overlapping content, distorted cards, and clipped hand
cards. Extent marks visual acceptance as WARNING and embeds the UI screenshots.
Multiplayer and full-match completion remain untested. The app was not
reinstalled during this run, and the Google account was not modified.

For the package layout, activation properties, report flow, and standalone
commands, see [Android test organization](docs/ANDROID_TESTING.md).

When adding a new business consumer, copy only the specific adapter or tests
it needs. Keep generic harness, backend contracts, and Reporting implementation
in their owning modules.

[Reporting J12](docs/ATF_Reporting_J12_Verification.md) records explicit artifact intent, timing sources and assertion indicators; J11 evidence remains historical.

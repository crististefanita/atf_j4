# ATF J4 — Android / Contract Whist

J4 reuses the shared ATF foundation for local Android acceptance testing. The
current target is Contract Whist (`com.qsoft.android.whist`) in the Android
emulator hosted by Podman on the physical machine and displayed in the Windows
VM.

## Reused foundation

- `atf-test-kit`, `atf-mock`, `atf-validation`, `atf-console`: source snapshot
  selected from J3 after comparing J1/J2/J3 and checking that the shared Java
  sources match the current J0 baseline.
- `lib/atf-core-1.0.0-c3.{jar,pom}` and
  `lib/atf-reporting-1.0.0-j15.{jar,pom}`: binary consumer dependencies. J4
  records their hashes and source lineage in `library-provenance.json`.
- Core and Reporting implementation and contract tests remain owned by J0.
  J4 owns only Android integration and consumer-specific report composition.

See [Android test notes](docs/ATF_Android_Whist_Testing.md) for the VM/host
connection, what was actually exercised, and known display limitations.

## Rules and component-first development

[Offline game corpus](docs/AI_CORPUS.md) keeps complete games as simple JSON,
including every bid, trick, series payment and final score. Replay, validation
and training-example export run without Android or a UI. Development and heldout
deals are kept separate; losses are retained.

[V4 acceptance](docs/AI_CORPUS_ROUND4_ACCEPTANCE.md) improves both policies using
public one-card bids. On fresh heldout deals, Expert gains 190 points / four win
shares and the independent player 242 points / five win shares, per 128 paired
comparisons each. The corpus now has 2,161 complete games. The new Extent projects
257 real invocations and 136,202 comparisons; DOM/data acceptance is distinct
from the interactive browser audit, blocked by the MCP file-URL policy.

[V3 offline acceptance](docs/AI_CORPUS_ROUND3_ACCEPTANCE.md): 1,137 complete
games independently replayed, 333 JVM and four Node contracts passed. The
accepted Expert improves heldout win share from 28 to 31 on 128 paired games,
with almost unchanged total points. Rejected planner changes remain diagnostic
evidence; only its proved feasible-deal fallback is kept.

[V2 AI and Android acceptance](docs/AI_TRAINING_V2_ACCEPTANCE.md) records the
completed 24-hand Expert game, series +10/−5 checks and the native Extent report:
349 accepted cases, 30,028 comparisons and 352 inline-image checkpoints.

The original app's entire Instructions menu is now mapped in [the Instructions coverage contract](docs/ORIGINAL_INSTRUCTIONS_COVERAGE.md).

- Whist Lab adds the missing local variants and a Romanian five-hand series selector: positive bonus **5 or 10**, default **10**, with a fixed −5 missed-series penalty.
- The menu selection and counters persist; old saves retain their classic profile.
- Per-contract scoring remains `5 + tricks` for an exact bid.

The [AI training protocol](docs/AI_TRAINING_PROTOCOL.md) records frozen policies,
paired development/final seeds, rejected experiments and the independent
public-information challenger. The new pipeline keeps sources/build output in
an isolated TMP stage and all evidence outside J4:

```powershell
.\scripts\run-whist-ai-validation.ps1
.\scripts\run-whist-ai-validation.ps1 -WithAndroid -PositiveSeriesBonus 10
```

The second command also checks the real Android menu and resume, plays all
24 Romanian hands with three Expert bots, reconstructs every move/score offline,
and publishes a native J0 Extent report with complete text and exact inline
Base64 screenshots. This measured adversarial evaluation does not prove perfect
play. Historical reports below preserve the rule profiles and APKs used then.

Whist Lab now has an explicit `ROMANIAN_WHIST` profile alongside `OH_HELL`.

- Romanian Whist requires trump when void, uses a reduced deck and a 1–8–1 schedule, and scores exact contracts as `5 + tricks`, otherwise minus the bid error.
- These rules must not replace the distinct original-app Oh Hell oracle.
- The [game-core README](whist-lab-app/game-core/README.md) maps each rule to sources, component cases, exhaustive domains, independent full-game checks, and the Expert AI's information boundary and paired benchmark.

```powershell
.\scripts\run-rule-components.ps1
```

This offline pipeline runs the rules and AI components, measures JaCoCo coverage, builds the APK and publishes native Extent with the current Reporting library.

- It does not start or install into an emulator.
- Evidence and the report go outside J4, under `C:\work\whist_back\rules-components\<run>\`.
- Android acceptance follows component acceptance; its screenshots remain exact inline Base64 PNGs.

The first 2 November component acceptance, retained as a historical baseline, is `C:\work\whist_back\rules-components\20261102-card-acceptance\component-extent-report.html`: 187 component/benchmark cases passed, plus the explicitly run publisher.

- The independent model checked 88 games and 40,304 card plays.
- GameRules reached 100% measured lines/branches; the complete game-core reached 97.22% / 91.07%.
- Expert outscored Hard at every supported table size across 144 paired comparisons.
- Exact results and reproduction are in the game-core README.

## Modules

- `atf-android`: bounded ADB process adapter with explicit device serial.
- `atf-test-android`: opt-in Contract Whist Android UI acceptance check.
- `atf-tests`: J4-owned cross-module acceptance and architecture catalog.
- `whist-lab-app/`: the local parallel Android app and its independently tested
  `game-core` rules engine.
- The four copied shared foundation modules and Core/Reporting binary pairs.

## Complete normal Romanian Expert game — 2 November

[Romanian Expert acceptance](docs/ROMANIAN_EXPERT_ACCEPTANCE.md) records a subsequent complete normal Whist Lab Android game: 24 hands, 94 tricks and 376 card plays, with three real Expert opponents and no replay fixture or injected deal.

- Every move and score matched the independent J4 oracle.
- Final totals: `[12,87,91,61]`; Miles won with 91 points.
- A separate offline audit reconstructed 260 retained snapshots and every visible score row.

The accepted inventory is 233 cases: 210 game/AI components, 21 Android/report contracts, the live game and the offline audit.

- Publishers run separately.
- GameRules remains at 100% lines/branches; whole-core coverage is now 97.17% lines / 90.44% branches after the added guards and explanations.
- The 144-pair Expert benchmark remains 14,604 vs Hard's 7,689 points.

Native Extent: 11,047 assertions, zero failures and 287 screenshot checkpoints, all inline Base64, with complete-value and decoded-PNG hash verification.

- Report: `C:\work\whist_back\romanian-expert\20261102-131344\romanian-expert-extent-report.html` (41,278,016 bytes).
- The component report is in that run's `component-acceptance\component-extent-report.html`.

```powershell
.\scripts\run-romanian-expert.ps1
```

The runner builds in a fresh TMP stage, finishes components before Android,
installs only Whist Lab, preserves failure diagnostics and publishes native
Extent with inline Base64 screenshots. Reports and the tested APK remain in
`C:\work\whist_back\romanian-expert\<run>\`. The original app is untouched.

## Complete original and parallel game acceptance — 2 November

The [complete-game acceptance record](docs/COMPLETE_GAME_ACCEPTANCE.md) is the current result.

- The real original and Whist Lab Android each completed all 19 hands, 109 tricks and 436 card plays.
- An independent oracle checked each move and score; both ended at `[89,126,145,119]`.
- The same inputs also passed the pure Java engine replay.
- A subsequent normal Romanian Whist + Expert Android hand verified leaving debug replay, scoring and the next-hand transition.

The accepted inventory contains 219 JUnit/Gradle cases with no failures, errors or skips; report publishers ran separately.

- The native Extent J12 report has 6,655 UI rule assertions, 581 action/observation records and 325 screenshot checkpoints.
- Exact PNG bytes and complete expected/actual values were checked against the published HTML.
- All images are inline Base64; the HTML contains 307 PNG data URIs with 305 distinct byte hashes.
- Functional acceptance is PASS; the original's rendering remains a visual WARNING.

Report: `C:\work\whist_back\deep-app-test\runs\20261101-231857-full-match\final-extent-report.html` (107,332,432 bytes).

- The report's own browser visual audit was blocked by MCP's local-file policy; automated HTML fidelity checks and direct PNG review passed.

```powershell
.\scripts\run-complete-match.ps1 -Serial '127.0.0.1:5555'
```

This starts new acceptance games.

- The completed evidence can be republished without replaying Android; see the acceptance record.
- The measured games and final publisher used Core C2 / Reporting J12.
- The separately delivered Core C3 is recorded below; that delivery does not relabel the earlier execution.

## Component and Android tests

Requires JDK 21 and Maven. The default build does not contact the emulator.
For the complete headless run, with the Podman emulator already booted and the
app installed, run:

```powershell
.\scripts\run-deep-tests.ps1
```

The script runs each module's component tests, then the named Android component and live-gameplay suites.

- `scrcpy` can stay closed; the emulator and host ADB bridge must remain up.
- It saves evidence outside the source tree under `C:\work\whist_back\deep-app-test\runs\` and publishes one Extent HTML report.
- Android screenshots are attached to named UI checkpoints through the current Reporting library, with inline Base64 data URIs; Surefire XML, text output, and UI hierarchy files remain linked as diagnostics.

The earlier discovery baseline is `C:\work\whist_back\deep-app-test\runs\20261101-222629\`.

- J4 reported 498 Surefire cases: 497 passed, no failures or errors, and one opt-in publisher test skipped in the ordinary module pass.
- The publisher then ran explicitly and passed.
- Android acceptance passed 6/6 flows (4 original-app flows and 2 Whist Lab flows); the Android adapter component suite passed 6/6.
- Whist Lab's `game-core` passed 29/29 Gradle tests and the debug APK built and installed.

The final Extent report is 16,805,609 bytes with 42 inline PNG Base64 data URIs; the run folder retains the original PNGs, UI hierarchies, Surefire results, and logs.

- It records the original app's 10-card score sheet and verifies that `Next Hand` opens a 9-card bid range from 0 through 9.
- Score cells are manual visual evidence and the arithmetic is explicitly conditional; card identities, trick winners, and the original app's full match are not machine-verified.
- See `C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html` and [Android test notes](docs/ATF_Android_Whist_Testing.md) for the report and limits.

The original app has serious display defects on this emulator: neon-green background, oversized/overlapping content, distorted cards, and clipped hand cards.

- Extent marks visual acceptance as WARNING and embeds the UI screenshots.
- Multiplayer remains untested.
- The later full-match acceptance above supersedes the discovery baseline's limitations on card identities, trick winners, scoring and completion.
- The original app was not reinstalled during this run, and the Google account was not modified.

For the package layout, activation properties, report flow, and standalone
commands, see [Android test organization](docs/ANDROID_TESTING.md).

When adding a new business consumer, copy only the specific adapter or tests
it needs. Keep generic harness, backend contracts, and Reporting implementation
in their owning modules.

[Reporting J12](docs/ATF_Reporting_J12_Verification.md) records explicit artifact intent, timing sources and assertion indicators; J11 evidence remains historical.

## Core C3 — verified library delivery

[Core C3 verification](docs/ATF_Core_C3_Verification.md) binds the new JAR/POM pair to native source/JAR regressions and separate consumer acceptance. The C3 acceptance used Reporting J12; the current Reporting delivery is recorded separately below.

[Reporting J15](docs/ATF_Reporting_J15_Verification.md) preserves complete inline PNGs and collapsible redacted ACTION/OBSERVE values, with the 240-character preview. Core C3 stays byte-identical; source/JAR and consumer acceptance are recorded separately.

J14 shares ACTION/OBSERVE row projection and one redaction policy, including the screenshot title.

- Complete cells retain captured whitespace; the default 240-character preview and original verdict remain unchanged.
- Historical device runs retain their recorded Reporting version.

## J14 consumer audit limits

| Path | Current acceptance boundary |
|---|---|
| Individual bid-grid | JUnit and trace exist; standalone HTML is not published |
| Legacy resume | JUnit PASS is contradicted by the captured menu and disabled Resume Game; pixel count does not prove a resumed game |
| Legacy report | Two inline PNGs render; evaluated assertions and ADB execution timing are not exported |
| Shared device | Respect the active external Android lease; artifact rendering does not prove isolated device acceptance |

[The J14 audit](docs/ATF_Reporting_J14_Verification.md) separates library
consumption from device acceptance. [The J4 feedback](docs/feedback/ATF_Android_Alignment_Feedback.md#j14--criterii-active-demonstrate) owns these consumer fixes.

## Auditable inline report composition

[The Android/component report contract](docs/ATF_Android_Report_Contract.md)
defines how to keep each invocation, evaluated comparison, captured execution interval and
original PNG in one HTML report. Required facts must be readable inline; XML,
JSON and properties files remain execution diagnostics outside the public report.

The publisher migration remains open in [J4 feedback](docs/feedback/ATF_Android_Alignment_Feedback.md); the table describes the required contract.

| Report family | Required presentation |
|---|---|
| Component | own case identity, inputs/state and standard collapsible assertion tables; no screenshot required |
| E2E | measured steps, independent comparisons and original inline PNG at the correct checkpoint |
| Aggregate | one shared report with actual invocations; counts remain distinct from observations, moves and images |

J15 also preserves literal CR/LF and leading newlines in inline TEXT bodies.

The [current report audit](docs/ATF_J4_Report_Audit.md) separates fresh component checks, archived E2E reconstruction, deliberate negative controls and the remaining consumer/renderer gaps.

## Current screenshot viewer — J16

[Reporting J16](docs/ATF_Reporting_J16_Verification.md) fits complete PNG evidence to the browser viewport and preserves the clicked checkpoint, local caption, disclosures and focus when closing the viewer. Core C3, the 240-character preview and the existing UI/API/SQL evidence contracts remain unchanged. Producer-side setup, inputs, independent expected values and capture completeness retain their own acceptance.

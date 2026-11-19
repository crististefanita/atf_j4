# Card Table Browser adapter

## Overview

Whist uses Java engine and portable table contracts in both APK and browser. Web runs either via local JVM server or as a static site with the engine compiled via TeaVM. Rentz and Hold'em integrate fixed versions of the Web library through their own adapters.

**Quick links** · [Android–Web contract](#what-android-and-web-share) · [Run on the Web](#web-execution-paths) · [JAR and AAR](#adapter-library) · [Verification](#verification)

## What Android and Web share

| Shared presentation contract | Responsibility |
|---|---|
| Actor label | An optional consumer-owned `actorLabel` preserves the visible game action, including the human seat. Hidden headings are excluded before layout. |
| Seat text | `SeatTypography` supplies roles; measured text fits horizontally without changing row height. |
| Frame identity | Prepared redraws commit geometry, facts and accessible actor state to existing seat objects. Incompatible tables keep fresh frames. |
| Trick continuity | Native-style tables attach controls only for visible actions. Automatic-only and action-free states create no empty sibling or extra flex gap; real bidding controls and legacy prompts remain visible. |
| Component boundary | Checks execute the delivered constructor and adapters. Native raster and painted geometry have separate platform acceptance. |

The two interfaces share game rules and table portable calculations. Each adapter measures and draws with the tools of its platform.

```mermaid
flowchart LR
    Android[Android interface]
    Web[Web Interface]
    Engine[Whist engine]
    Table[Table model]
    Android -->|command| Engine
    Web -->|command| Engine
    Android -->|apply| Table
    Web -->|apply| Table
```

<details>
<summary>Legend on rules, geometry and rendering</summary>

- The engine validates the bids and the cards, declines the score and sets the actor. Adaptors publish the visible table status and player options.
- The table model includes geometry, fan, card appearance and timing. Android applies it through Views, and Web through font measurements and DOM elements.
- Typography and system insets shall be measured separately. Samples with the same inputs check the formulae, and the APK-browser comparison on the device measures the rendered result. [Display components](../docs/testing/ATF_DisplayComponents.md)

</details>

## Web execution paths

```mermaid
flowchart LR
    Page[Table in browser]
    Http[Server JVM]
    Worker[Web Worker]
    Adapter[Adaptor Whist]
    Engine[Java engine]
    Page -->|send through| Http
    Page -->|send through| Worker
    Http -->|calls| Adapter
    Worker -->|calls| Adapter
    Adapter -->|run into| Engine
```

<details>
<summary>Legend on the route of controls and local data</summary>

- JVM server runs the game in local process and serves Web resources. The session of the match belongs to the server and remains associated with the tab.
- The static site loads the adapter and engine transcribed into JavaScript. The messages to Web Worker remain on the device. Local transport keeps the profile and checkpoint by storing the browser.
- The diagram follows the command. The answer returns the revision, legal options and public status on the same route. [Transport of the static edition](pages/web/local-transport.mjs)

</details>

| Layer | Responsibility |
|---|---|
| [The game contract](src/main/java/ro/cardtable/browser/BrowserGame.java) | Publish the places, the cards allowed, the legal orders and scores. Validation remains in the engine of each game. |
| [Server](src/main/java/ro/cardtable/browser/BrowserServer.java) | Serves the resources of the same origin. Isolate the games on the tab, validate the revisions and reject repeated commands. |
| [Geometry](src/main/java/ro/cardtable/browser/BrowserGeometry.java) | Apply the Java plan used in Android for seats and fans. The phone frame uses the CSS viewport 1:1. On desktop, the useful height sets uniform scale when width permits; a readable floor is kept only when both axes allow it; narrow windows otherwise fit both axes without overflow. |
| [Adapter Whist](whist/src/main/java/ro/whistlab/browser/WhistBrowser.java) | Use distribution, AI policies and engine settlement. The recommendation comes from the same human selection as on Android. |

## Whist on GitHub Pages

The static edition distributes the Java engine compiled with TeaVM in a Web Worker. The table sends the same commands and reads the same public projection through local messages. After loading resources, bidding, AutoGame and settlement shall not make requests to an API.

| Contract taken out of Android | Application in browser |
|---|---|
| [Header typography](../whist-lab-app/app/src/main/java/ro/whistlab/app/theme/GameHeaderTypography.java) and [palette](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/theme/TableColors.java) | The adapter transmits the text's roles and colors. The order applies to the header, menu and table. |
| [Table reservation](../whist-lab-app/app/src/main/java/ro/whistlab/app/table/TableLayoutMetrics.java) | Measure the window and keep the same centre reserved for all hands of the variant. |
| [Bidding checks](../whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidGridMetrics.java) | The figure and confirmation shall keep the tactile target of 48 logical units. The outline is withdrawn inside, and the number of columns and removable row come from APK policy. |
| [The pace of the moves](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/turn/TurnPacing.java) | Each automatic card appears after the delay defined by APK. The adapter plays the actor, card and collection in order produced by the engine. |

[Table contract](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/TableGeometry.java) receives the measured dimensions and phase reservation. Android applies the rectangle in the ViewGroup, and the web applies it in the DOM.
[Collection curve](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/animation/TrickCollectionMotion.java) assesses progress on Android and provides web-based frames that the browser interpolates.

| Date | Retention in the static edition |
|---|---|
| Language | The Romanian or English option is saved in the browser. The menu, rules, table status and achievements use the texts of the APK. |
| Profiles, Achievements and Rankings | The browser saves the name, progress and leaderboards in the same local record as the game. The leaderboards use the top, the filter by configuration and the AI and Example markings in the APK. |
| Party | The menu allows for the resumption after reopening. The browser saves the checkpoint at each accepted command and uses the same validation as the APK. |
| Site data | Wiping the site storage or changing the browser starts a new profile. If storage is disabled, the session uses the memory of the tab. |

- [Checkpoint codeine](../whist-lab-app/app/src/main/java/ro/whistlab/app/persistence/GameCheckpointCodec.java) restores distribution, tricks and history at the last accepted command. The identity of the match remains stable and prevents duplication of results. A damaged checkpoint can be removed independently of profile and progress.
- Web Locks serializes the writings between compatible tabs. A tab with an outdated revision reloads the checkpoint before its next command. If the browser refuses writing, the game can continue in memory and display the storage error.
- The history displays all hands and total after each hand in column P A red diagonal marks the missed bid. In column P, a box marks the serial bonus, and a red box marks the penalty.
- Settings use [Same default values as APK](../whist-lab-app/app/src/main/java/ro/whistlab/app/screens/MatchSetup.java). The change of language keeps the options on the screen, and the rules dialogue shows the schedule of the option for the selected number of players.
- In the game menu, the current score opens up the history when there are hands closed. At the last score, **Play noun ** retains the configuration and replaces the local game with a new distribution and identifier.
- The chenars of the table take over the name, bid and levatates of the APK projection. The gold tag shows the playable cards. The total appears in score and history.
- [Hand adapter](src/main/java/ro/cardtable/browser/BrowserHandAdapter.java) sends gestures to the common model used in the APK. The refusal highlights the pressed card and legal options for 1.100 ms. When choosing a legal card, the deposit follows the raise of 150 ms. API projects the same curve and layers of the fan as in APK.
- The face of the Whist takes the mark from the Android hand. The rank and symbol in the corner, the central symbol and the inverted corner shall be proportionately scaled in the trick. Collection copies face off the table. Accessible text includes the player and the card.
- The main menu of Whist takes over the dedication from the location used on Android. The site uses the same photo, copied to the building from the application's resources.
- [The contract of the trump box](../whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/table/trump/TrumpTileMetrics.java) provides the basic dimensions and typography. Android measures with native font, and Web with Canvas. [Same cases of classification](../whist-lab-app/app/src/test/resources/whist-trump-text-fit.tsv) checks both adapters.
- The collection shall apply the time-setting in the common module. The complete trick and the winner's current trick count are in the collection event. The hand is reviewed immediately after deposit, and the stable table contains the cards in the current trick. Resize can remove the temporary drawing, and the next card will respect the already scheduled range.
- AutoGame is activated for current hand. The debunking cancels the next decision instead of the player. The accepted movements shall be terminated and the menus and dialogues shall suspend the following decision for as long as they are open. The score or return to the main menu turn off the tick.

- The building requires PowerShell 7, JDK 21, Maven and the environment of generating the catalogue. `$WorkRoot` and `$EvidenceRoot` indicate external directories.
- The Node.js test the public inventory and play through the compiled engine. For gestures and profile reopening, run [scenarios Playwright](#verification).

```powershell
$Site = .\atf-ui-browser\Build-WhistPages.ps1 -WorkRoot $WorkRoot
node .\atf-ui-browser\pages\verify.mjs $Site $EvidenceRoot
```

- For bidding changes, AutoGame, language or achievements, run `node .\atf-ui-browser\pages\verify-controls.mjs $Site $EvidenceRoot`. The sample uses the engine compiled and replaces the timer and the browser storage in controlled cases.
- Add `--ui-only` to geometry and rhythm of the moves. To include the visual path, cross the path to Chrome or Edge before option. `--browser-only` selects only the visual course and asks the executable.
- For score history and checkpoint compatibility, generate legal data with [WhistRecordsFixture](pages/WhistRecordsFixture.java) and run [checking Pages](pages/verify-records.mjs) with site directory, evidence file and JSON data file. The fixture compiles with the JVM launcher and `ScoredHandFixture` of the application tests.
- For language, settings, navigation and Play again, run `node .\atf-ui-browser\pages\verify-navigation.mjs $Site $EvidenceRoot`. The sample also produces a local checkpoint for visual check of the retake from the final score.
- For bookmarking, trick order and resize, run `node .\atf-ui-browser\pages\verify-effects.mjs $Site $EvidenceRoot`.
- For reaction to card pressing, lifting and trick face, run `node .\atf-ui-browser\pages\verify-card-adapter.mjs $Site $EvidenceRoot`The JVM test and test of the UI module consume the same [gestures scenario](../whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv).
- Use `--contract-only` to check only geometry on phases and curve samples from the compiled artifact. For this selection, omit the browser executable.

The workflow [.github/workflows/whist-pages.yml](https://github.com/crististefanita/whist/blob/main/.github/workflows/whist-pages.yml) builds and verifies the static site before deploying it from `main` through GitHub Actions. After deployment, open the public URL in an anonymous session and verify the assets and the start of a hand.

The artifacts of static distribution are on the publishing branch. The [TeaVM dependencies](pages/teavm-dependencies.xml) file serves the Pages building independently of the Maven ATF reactor.

## Running locally

Run the project command with PowerShell 7, JDK 21 and the environment Gradle Android configured for the catalog generation. `$WorkRoot` indicates the external directory for the build and profile data. Open the full terminal address in Edge or Chrome.

```powershell
.\atf-ui-browser\Start-WhistBrowser.ps1 -WorkRoot $WorkRoot
```

For a phone connected to the test network, `-ListenAddress $LocalLanAddress` selects the local address accessible. Open the full address in your phone browser.

## Data in the JVM server

| Date | Where to store |
|---|---|
| Name and identifier | On the device, for the same web origin as defined by protocol, host and port. In a new tab, Whist gets his profile back. The browser copy can remake the name if his file is missing from the server. |
| Achievements | In `$WorkRoot/whist-profile-data`, associated with the identifier and credited from the results confirmed by the engine. |
| Party | Independent on the tab. The same tab can resume the game by recharging while the server process is running. |

- Wiping the site data, changing the browser or changing the origin creates a new profile. In private mode, retention follows browser policy.
- Whist uses Android's task of generating the catalogue and dependence `org.json:json:20240205`. `-JsonLibrary` selects JAR from another directory, and `-CatalogSource` reuses the output generated from the same checkout.

## Adapter library

Whist builds Web and APK from sources. The library for other games is exported in two formats with own checks on each consumer.

```mermaid
flowchart LR
    Shared[Portable calculations]
    Native[Android Views]
    Browser[Server and DOM]
    Aar[AAR Android]
    Jar[JAR Web]
    Native -->|include in| Aar
    Shared -->|enter| Aar
    Shared -->|enter| Jar
    Browser -->|include in| Jar
```

<details>
<summary>Legend on the content of the packages</summary>

- [Android AAR](../whist-lab-app/atf-card-table-ui/README.md#packaging-and-consumption) contains the widgets, themes, resources and portable calculations selected by Gradle.
- [Web launcher](Start-CardTableBrowser.ps1) compiles an explicit selection of portable sources, including the contents of the seats, table cards, colors and the rhythm of the moves. The JAR includes the server, adaptor contract and DOM resources. Each game provides the engine and adapter.
- The Web test archive is exported separately. It checks resources, revisions, menus, card lift, collection and browser frames. Exported JARs have adjacent SHA-256 files.
- Whist site uses [alt build](Build-WhistPages.ps1), which produces static resources from the Whist adapter and engine. Pages test and JARs acceptance by Rentz or Hold'em have different inputs.

</details>

### Current qualified package: 0.2.10

The adjacent `browser-build.json` binds this release to its source commit, input inventory and artifact hashes.

- Runtime: [Card Table Browser 0.2.10](lib/atf-ui-browser-0.2.10.jar) · [SHA-256](lib/atf-ui-browser-0.2.10.jar.sha256)
- Shared tests: [Card Table Browser tests 0.2.10](lib/atf-ui-browser-0.2.10-tests.jar) · [SHA-256](lib/atf-ui-browser-0.2.10-tests.jar.sha256)
- Provenance: [browser-build.json](lib/browser-build.json)

The viewport contract preserves a 414 × 896 logical frame on desktop, scales uniformly to fit the usable browser height and width, and retains a 0.68 readability floor only when both dimensions permit it. Phone browsers use their CSS viewport 1:1. Rentz and Hold’em consume the same runtime and test-archive bytes, verified against this receipt.

```powershell
.\atf-ui-browser\Package-CardTableBrowser.ps1 -WorkRoot $WorkRoot -Version $BrowserLibraryVersion
```

- Choose `$BrowserLibraryVersion` and start out from a clean checkout Git. The exhibition writes the two JARs, fingerprints and `browser-build.json` in the external directory of the version. The registry links the files to the source comma and the input inventory.
- Check the commit and SHA-256 at adoption. The manifest installed near the two JARs shows which version each adapter uses.
- Export shall re-use the Director of the same version after confrontation with entries and bytes. Changing entries requires a new version. The integration challenge starts the first match through the physical JARs, adaptor and game dependencies.
- Promote the same bytes and same fingerprints in qualified consumers. The version of the HTML cache aims at updating the site's resources. [Consumer verification](#verification)

### Portable Web and APK adapter contracts

Whist uses the same native table policy through [WhistBoardAdapter](whist/src/main/java/ro/whistlab/browser/presentation/WhistBoardAdapter.java). The browser supplies measured CSS units and root text scale; Android supplies density and system text scale. A browser reference therefore compares logical regions rather than raw physical pixels.

| Component boundary | Checked contract |
|---|---|
| Table geometry | All modes, 3–6 seats, compact and wide widths, 1–17 cards and enlarged text; bidding and play retain identical seat anchors. |
| Unit adaptation | Normalize Android density to Web logical coordinates. The rounding bound accounts for repeated dimension conversions and integer placement in each renderer; unit-density coordinates must match exactly. Desktop zoom remains in the viewport adapter. |
| Card presentation | All 52 rank/suit combinations retain visible marks, color role and proportional sizing through the actual hand adapters. |
| Interaction | The shared gesture fixture checks rejection, legal highlights, stale expiry and one accepted deposit; collection keyframes are compared through both adapters. |
| Acceptance limit | These components execute portable production code, not Android Views or browser font rasterization. Same-device visual acceptance remains separate. |

[Adapter components](tests/whist-surface-contract.test.mjs) compile only the portable dependency closure with the JDK, without Gradle, SDK or emulator. Their [JUnit owner](../atf-tests/src/test/java/atf/tests/governance/documentation/release/rag/nativeproof/WhistSurfaceAdapterComponentTest.java) captures actual observations in Extent. The default text scale remains unchanged; invalid scale inputs are rejected at the layout boundary.

## Verification

The table menu exposes score history directly in Whist and Rentz through the common menu adapter. Opening history pauses automatic scheduling; closing the dialog resumes the same table without replaying commands. Component checks cover localized labels, one callback per choice, outside-click dismissal and immutable menu plans.

- The Web adapter enlarges the 2–A ranks of regular cards by 50%. It centres indices in the same band, measured with `10`, and limits compact-face ranks to the available space. Card faces, suit symbols and the trump retain their reference dimensions.
- The width of the visible index sets the overlap in the fan, including `10` and 17 cards.
- The Web header retains the APK anchors for the title, progress and trump. Progress and bid summaries, the game menu, the main menu and settings use text enlarged by 50% on Web. The trump tile retains the natural height of its two rows and constrains text to the available width.
- In the Web, the button is the player's only visual prompt to manually bid.
- Touching the table outside the game menu closes the menu. Probe inside and outside targets separately, including after redrawing the table.
- After opponents move, the visible board retains its geometry until the next state is laid out. The component fixture delays measurement just before the player's turn returns. The trump tile retains its position and colours during manual play, AutoGame and collection.
- Player frames retain the same coordinates during bidding, play and collection. The component fixture compares every frame in one-card hands with five and six players, during manual play and AutoGame.
- The symbols of the cards use the same face ink in hand, on the table and when collecting. The theme provides the colors, and the Web adapter requires the rendering of symbols as text.
- In Whist Web, bid summaries use 90% of the enlarged row font size, and dropdowns use 75% of the label font size. The check at 360 and 414 px measures the Normal/Hard selector on the same row as its Romanian label.
- The viewport contract compares phone portrait/landscape, desktop height-limited and width-limited sizes, monotonic scale, narrow-window fallback, and invalid measurements. The browser check reads the actual frame after resize and confirms its centering and scale from the useful viewport.

Use `-BuildOnly` with the three launchers and pass the returned classpaths to the component suite. JVM processes serve the adapters on loopback; the suite sends HTTP requests and checks public state. `$EvidenceRoot` identifies an existing external directory dedicated to the run.

```powershell
node .\atf-ui-browser\tests\run-components.mjs $EvidenceRoot $WhistClasses $RentzClasses $HoldemClasses
java -cp $WhistClasses .\atf-ui-browser\tests\WhistParity.java > (Join-Path $EvidenceRoot 'whist-parity.json')
```

For UI checks, prepare the Pages edition in `$Site` and generate `hand-fixture.json` with `verify-card-adapter.mjs`. The run uses Playwright Java with headless Chromium, role locators and an isolated context for each scenario. [Playwright locators and expectations](https://playwright.dev/docs/locators)

`-BrowserExecutable` allows the choice of a Chrome or Edge installation. Compatible chromium should be available when the parameter is omitted.

```powershell
node .\atf-ui-browser\pages\verify-card-adapter.mjs $Site $AdapterEvidence
.\atf-ui-browser\tests\Run-BrowserUi.ps1 -Site $Site -HandFixture (Join-Path $AdapterEvidence 'hand-fixture.json') -EvidenceRoot $EvidenceRoot
.\atf-ui-browser\tests\Publish-BrowserExtent.ps1 -EvidenceRoot $EvidenceRoot -MavenClasspathFile $MavenClasspath
```

- `-Check viewport` runs the focused resize contract in Edge or Chrome across phone portrait/landscape and desktop height-/width-limited sizes, including narrow windows.
- The browser check waits for page content and the resize event, then verifies the logical frame, CSS transform and centering.
- The shared JAR includes the independent Node component contract for phone, desktop, monotonic resize, invalid dimensions and overflow on both axes.

`$MavenClasspath` is the dependency list produced by the Maven [component gate](../docs/testing/README.md). The report links comparisons and inline images to the browser run. [Open parity criteria](../docs/feedback/ATF_UiBrowser.md)

| Test layer | Responsibility |
|---|---|
| Component | Check revisions, refusals, geometry, profile and settlement through HTTP contract or Pages engine with controlled storage and timing. |
| UI Web | Check the Chromium index `10` and access to the last card in a hand of 17 cards, including text at 200%. Check the keyboard bid, rejected touch, AutoGame and post-reload profile. Write `browser-ui-checks.json` and the captures in the running directory. |
| UI desktop | Check in Chrome and Edge with browser zoom 100% and 125%, frame at 1280 × 720, 1920 × 1080 and in a narrow window. Measure the useful viewport and frame position. Confirm visible focus and profile, bid and legal card path from the keyboard. |
| Comparison on Android | Compare APK and Chrome at the same viewport during bidding and play. Check header and trump positions in EN and RO, with card, symbol and no-trump presentations, including enlarged text. Record viewport, density and APK SHA in `paired-android.json`. |
| Reporting | Extent reads the results of each level and attaches the captures of PNG inline. |

The trump tile loads a local subset of Noto Sans Symbols 2 containing the four suit symbols. [OFL licence](src/main/web/card-suits-OFL.txt)

### Action-label clearance

| Contract | Responsibility |
|---|---|
| Measurement | The shell supplies `actorClearance` for the label above the seat frame. |
| DOM probe | An invisible node is measured in CSS units and removed immediately; scene scaling does not change the reservation. |
| Adapter | A dense board may exclude the reserved band without moving common seat anchors. |
| Compatibility | Games that do not consume the parameter retain their existing geometry. |
| Components | Visible or hidden labels, different heights and probe removal. |

### Score text and shared playback checks

- Web score captions, summaries and actions use the shared text scale once, with a default factor of 1.5. Scoring-detail and live-score dialogs have explicit presentation roles; unrelated dialogs retain their text size.
- A focusable scroll region keeps enlarged score columns accessible in narrow windows. Android widgets and game scoring are unchanged.

- The shared tests execute the real Web constructor and collection code with a controlled DOM and clock. Actor cues precede cards; measured faces retain their origins, dimensions and destination until collection finishes.
- Premature clearing is a negative control. Game-specific trick winners, awards and sequence rules remain in each consumer's tests.

- `shared-presentation.test.mjs` is shipped in the test JAR. Set `ATF_BROWSER_WEB_ROOT` to the extracted runtime's Web resources and `ATF_BROWSER_EXECUTABLE` to Edge or Chrome, then run `node --test shared-presentation.test.mjs`. Browser profiles use the external temporary root; audio is muted. Typography checks use controlled game content and the production DOM/CSS, rather than an Android raster.
- Browser teardown requests a graceful exit and waits for the owned process. Profile cleanup is asynchronous with bounded retries so Windows handle release can finish before directory removal.

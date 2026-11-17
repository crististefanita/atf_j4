# Match history and interface qualification

## Overview

The engine holds the game data, and the Android app holds their persistence and presentation. The changes in the interface shall qualify for the actual installed binary with the status and purpose of the declared scenario.

## Native acceptance of proportions and order

The display profiles guide holds geometry and text. The common interface registry holds the extraction of reusable components. [Profiluri](../testing/ATF_Display.md#profiles-and-field-of-qualification) · [Common interface](ATF_CardUi_Extraction.md)

| Priority | Rated rating | Closure criterion |
|---|---|---|
| P1 | Extension of native qualification beyond the compact profile; components do not substitute the fonts and Android interaction. | High profile and increased text: 13 cards, fully levitated to 5 seats and the actors' succession to 3/5/6 places, with legible names and indices. Keep the catches, XMLs and APK hashs without repeating a full game on each profile. |
| P1 | Extend AutoGame's compact sample to the native matrix of the actor and positions. | On the identified APK, check the bidding and the game at 3–6 places, in RO/EN and on compact profile plus increased text: only the current actor has badge, the badgeul remains visible when AutoGame plays the human player's seat, the manual controls remain locked, and the collection of the trick and the score screen does not mark an actor. The places do not move when the phase changes, and the face and the lifting of the card remain visible. Components check projection and geometry; only Android observation with APK/hash and catch can close the criterion. |

Delivery files keep the sample compact with 13 cards and audit of the indices, as well as the AutoGame test at four places with a card, the hand-game marking and the score screen. Acceptance on hand 17 cards check horizontal roll and box stability. The marking remains open during the fast automatic move and the matrix with increased text. [Release catalog](../../release/README.md)

## The compatibility of history

### P1 - Native qualification of the two-column history

The native prefix confirms a complete row and the resumption in Romanian and English. The matrix below follows the rest of the population and the raster markings. The components qualify for projection, persistence and geometry.

| Surface | Closure criterion |
|---|---|
| Two columns | The exact bid and the total score after each hand per player; column `P` displays the balance of the row, including zero and negative values, without a third column. RO/EN, text normal and enlarged, first and last hand and all places, on an APK that includes this contract. |
| Serial Rectangle | Normal contour only after five consecutive eligible outputs; red contours after five consecutive eligible failures, including when the total remains positive. Bonuses and penalties ± 5 / ± 10 shall be set in total only by payment row, exclusively in cell `P`; the bid, header, neighbor and unpaid results shall not be rectangle. |
| Randare | The original catch confirms the four sides, the legibility of the number and the sign, the space to the contour and the lack of intersections, including the increased text. The presence of the node in XML does not demonstrate the rectangle and a report of an earlier APK does not qualify for the new marking. |
| Blunt-cut | A failed bid has a visible mark in the Android raster, including zero and increased text; only the red colour or accessible description does not certify the cut. Compare with a successful contract and keep the cipher readable. |
| Integritate | Exploration and resumption shall retain the totals and all rows; the marking shall remain in the right hand and place. |
| Separatoare | Target of 2,1 dp, rounded in pixels once only; all player boundaries remain aligned between headers and body. The component NGL qualifies geometry and color in JVM rendering; Android font and pixels require APK identified. |

[ScoreHistory](../../whist-lab-app/game-core/src/main/java/ro/whistlab/core/session/ScoreHistory.java) keeps known suffixes of bids and tricks. The points derive from the hips and the failure compares the bid with the tricks; they do not deduce from the color or mark of the score.

| Priority | Remaining requirement | Closure criterion |
|---|---|---|
| P2 | The native acceptance of the import of an old register remains distinct from the components of migration. | The resumption shall retain the outstanding and the unfamiliar prefix; only the demonstrable result shall be marked. Check the hand-auctioned-point-total combination. |

A balance or a difference of points does not justify rebuilding some missing levatates. The contract implemented and the migration limit remain in [model of history](../game/ATF_ScoreHistory.md).

## Menu, Help and Configuration

[Componentele regulilor](../testing/ATF_GradleComponents.md) connects the aid RO/EN to the engine. The components of the checkpoint check active and disabled series settings for both Romanian variants and both values. The Android scenario keeps the opening and running of aid, the selection of the bonus and the resumption of the same game after restart.

For Achievements, the presentation component reopens the progress in simulated storage and verifies the first chapter and the localized condition of the target. The native session demonstrates the menu route, catalogue, detail and return.

## Persistence and coordination

[Orders and rescue](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md#commands-persistence-and-errors) have port components for refusal, checkpoint, rankings and continuation. The yield is not performing; the recovery of native storage and the cost per device shall be kept separate.

| Priority | Lacquer and source | Level and criterion of closure |
|---|---|---|
| P1 | Recovery of Android storage: checkpoint `SharedPreferences` and ranking `AtomicFile` do not have native acceptance after corruption or interruption. | Android on APK identified: controlled interruption, recovery or explicit error, user data stored; serialisation components and storage ports remain separate checks. |
| P2 | The latency of the syncron checkpoint is not qualified on the device; the order of components does not certify reactivity. | Environment and status identified, cost of writing separately from the calculation of AI and rendering; keep order and cause of error before any movement of I/O between executors. |

## Qualification of visual changes

| Priority | Surface | Required check prior to acceptance |
|---|---|---|
| P2 | trick collection: native binding of qualified ports through components | APK identified at 3–6 places: measurement and clipping match the scene, transformations return to cancellation, and the last trick precedes the score. Confront `onPause` / restart, menu and overlapping dialogues, consecutive redimensioning, configuration notifications, detachment and disabled animations. Only the last geometry can receive the new scene; a delayed callback does not resume the abandoned route and does not consume the suspended time. The portrait policy remains the one in the manifest; the watch and event tests do not qualify pixels or fluidity. |
| P1 | Table, card and trump | Profiles declared, text enlarged, real intersections, logically coordinated mapped at raster and revision of original catch. |
| P1 | History and Resume | All hands closed and all accessible places without repeated points or advance the match. |
| P1 | Accessibility and location | Ordnance assisted, labels of unknown or negative values, first and last removable in both languages. In the enlarged text, face the full title of dialogue and the names of prima/ultima column separately before and after the course; the qualification of the region of legend does not demonstrate the legibility of these regions. |
| P2 | Configured name and shift badge | Check the name at the maximum accepted length in all positions and with the active badge; the visible identity must be legible and the abbreviation must have an explicit rule and access to the full name. |

- **Region: ** the action of the shift has a region measured above the box; the components check the dimensions and separation for the increased text.
- **Observation: ** E2E uses the same decoder for playing and bidding; native acceptance remains necessary for glyphs, actual division by rows and catches in RO/EN.

- **Sanity: ** notice the full action above the name at all active places, including after changing shift and returning from break.
- **History: ** check the first and last column after running, the numerical tags of the cards and continuity of the separators between the players.

A change of geometry or style is linked to the sources and APK inspected. The device qualification and component audit are separate records, kept externally.

- **Common: ** Sanityul compares the size of the frame at the same density and scale of text in all ways with a turned card, a color and lack of the edge. Check the two complete regions and the accessible identity without any additional line about the absence of the card.
- **Deposit:** check the text button RO/EN complete, at normal font and enlarged; the withdrawn outline remains separate from the tactile target. [Contractul display](../testing/ATF_Display.md) be of common size and criteria.

## Native acceptance on Android 17

Window protection and calculation of useful space are implemented; the acceptance on API 37 remains distinct from the components and verification on another Android system. The comparison between versions requires the identity of both APKs and the device.

| Priority | Remaining requirement | Closure criterion |
|---|---|---|
| P1 | API 37: window, rerun and memory | On an identified device: navigation through gestures and three buttons, decoupage, keyboard, restart and some hands; record memory, reason of output and APK hash without changing system boundaries. |

[Delivery contract](../platform/ATF_AndroidDelivery.md) distinguishes the Padding with the IME from the measurement of the window without the IME and defines the restoration and diagnosis of memory. Components shall verify the model and the native criterion shall remain open until the callbacks and scenario are executed on the target system.

## Adapter memory

The memory limit of the adapter remains distinct from the geometry of the interface.

| Priority | Lacquer and source | Level and criterion of closure |
|---|---|---|
| P2 | [CommandProcess](../../atf-test-android/src/main/java/atf/android/process/CommandProcess.java) reads the flows in full, without separate budgets for text and binary. | Components with controlled output: explicit outflow of each budget, demarcated memory and incomplete PNG refused prior to publication; the term and initial cause shall be maintained. |

## Owning contracts

- [Profiling, history, leaderboards and series scenarios](../testing/ATF_Android.md#profile-history-leaderboards-and-series) has automated mechanisms and preconditions; the existence of the scenario does not qualify for a version.
- [Componentele display](../testing/ATF_DisplayComponents.md) holds mockery and cachet insulation; fonts and native interaction have separate verification.
- [Qualification of publication](ATF_Reporting.md) keeps the gap in the early validation of checkpoint; [handing over results](../reporting/ATF_Handoff.md) holds the distinction between functional verdict and publication.
- [Governance of publication](../testing/ATF_ReleaseQualification.md#evidence-freshness) follows the launcher and resumes after an incomplete GitHub response. [Ghidul RAG](../testing/ATF_ReleaseQualification.md#snapshot-and-preflight-before-maven) defines the identity of the file and the connection of the published audit.

[Testarea Android](../testing/ATF_Android.md) · [Screen profiles](../testing/ATF_Display.md) · [Interface Atlas](../../whist-lab-app/app/src/main/java/ro/whistlab/app/README.md)

## Cost of acceptance on device

- Measure the reservation, profile preparation, ADB commands, execution and restoration separately. Face the active window to correctly assign a interruption.
- Validate each Extent reference to the original catch. Resume the flows without accepted proof for the same APK, snapshot and profile. [Qualification of delivery](../testing/ATF_ReleaseQualification.md#work-profiles-for-resumption)

## ANI-02 · Hand of 17 cards on compact screen

- Extend [APK's responsive evidence](../../release/README.md) at the remaining combinations of the matrix 3–6 places, RO/EN, text enlarged and AutoGame. Watch for access to the cards in the removable area, the actor's signaling and fixed positions between bid, game and collection. [Display strategy](../testing/ATF_Display.md)

## Android scenario stability

- [ ] Qualify on the device the notification of achievements that occurs after the score. At component level, the order of observations and gestures shall be checked with simulated DBD.
- [ ] The process of preparation shall confirm the active window of the temporary profile. A service in that profile opened the Android screen with details of an application during history. Isolate third-game processes in the temporary profile and keep the exact cause in the report.

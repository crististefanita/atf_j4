# JVM components of the Android app

## Overview

These tests check the application policies and models without installation or emulator. [Common strategy](../../../../docs/testing/README.md) holds acceptance of the result; [application architecture](../main/java/ro/whistlab/app/README.md) delimits the remaining Android integration.

## Verification families

| Family | Controlled entry and oracle |
|---|---|
| Layout and text | Dimensions, density, font and number of seats; reservation of maximum full schedule, including 17 cards in three places, stable anchors between phases and the title accessible on no more than two rows. |
| Common IU adapters | [Texts and callbacks](java/ro/whistlab/app/screens/WhistMenuContentComponentTest.java) and [all card faces](java/ro/whistlab/app/cards/WhistCardFaceComponentTest.java) keep the Whist semantics at the library border. |
| Bidding | [Contextual matrix](java/ro/whistlab/app/bidding/HumanBidMatrixComponentTest.java): 1–8 cards, 3–6 places and each bidding position; public observation, Hard assessment and independent arithmetic oracle for legality. |
| Bidding actions | [Layout components](java/ro/whistlab/app/bidding/AutoGameBidActionComponentTest.java) checks the centred rows, the reduced contour separately from the tactile area, the densities and the recovered space for the increased text. [Minium component](java/ro/whistlab/app/screens/GameMenuContentComponentTest.java) checks the stop option in EN and EN only during automation. |
| Continuity of the bid | [Rescue and Resettlement](java/ro/whistlab/app/bidding/HumanBidResumeComponentTest.java): actual JSON codec, recalculated recommendation, priority manual choice and contract confirmed retained; delayed deliveries do not change the session. |
| Uncertainty bid | [Context factors](java/ro/whistlab/app/bidding/HumanUncertainBidComponentTest.java): return card and trump card, public adverse targets, relative score and payment of series. Oracles distinguish the legitimate change of the usefulness of accidental change of simulated worlds. |
| Achievements | [Facts, rules and persistence](java/ro/whistlab/app/achievements/WhistAchievementComponentTest.java), [text projection](java/ro/whistlab/app/achievements/WhistAchievementPresentationComponentTest.java) and [coordination of notifications](java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java): complete catalogue, persistent IDs, eligibility by variant / table / rules, exact thresholds, simultaneous progress, double credit-free resumption, format RO/EN The Gradle YAML Validator is attached to the collection, with positive and negative vectors prior to Java generation. |
| Calibration of outputs | [Offline pair games](java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java) uses real AI policies on repetitive seedals, compares the measures per game with cumulative progress and verifies persistence after the reply. The results are excluded from the quick collection; `:app:testDebugAchievementCalibration` runs separately, on external and disjunct intervals, for 3–6 places and Romanian variants 1–8–1/8–1–8. The results measure self-play, eligibility and achievements per match and do not estimate human success. |
| Persistence and history | Legal orders, checkpoint JSON, balances and migration; refusal of corrupt data. |
| Life Cycle | Clock, executor, navigation and controlled callbacks; cancellation and resumption without repeated effect. |
| Actor table | [Projection of Whist](java/ro/whistlab/app/table/content/WhistTableContentComponentTest.java) checks the actor at bidding and playing for 3–6 seats, the AutoGame marking, the lack of marking at tricks collection and coherent disabling of human commands. |
| Collection of levatates | Geometry, players, last levitate and delayed events; the game status is maintained. |

## Execution and limits

[UI library components](../../../atf-card-table-ui/README.md#verification) have their own Gradle task. The application display filter includes the adapters of `screens` and `cards`; existing geometry tests remain at the integration of Whist and are not counted again as new tests of the library.

- **Focused selection: ** `:app:testDebugUnitTest --tests 'ro.whistlab.app.bidding.*'` includes matrix, resumption and small cases of estimation. The fixtures advance the engine through legal commands, with actual score and card turned, without external archive or emulator.

- **Achievements: ** `:app:testDebugAchievementsComponents` is the primary collection for catalogue, measurements and status. `:app:testDebugCommandsComponents` covers the order in which the complete match persists the derived results.

- **Optional Calibration: ** `:app:testDebugAchievementCalibration` receives `whist.achievementCalibrationPhase`, `whist.achievementCalibrationFirstSeed`, `whist.achievementCalibrationMatches` and `whist.achievementCalibrationOutput`. `whist.achievementCalibrationPlayers` accepts 3–6, `whist.achievementCalibrationMode` accepts `ROMANIAN_WHIST` or `LONG_ROMANIAN` and `whist.achievementCalibrationSeriesBonus` accepts 5 or 10; the configured value controls the bonus and the series penalty. Series and Game to 0 can be switched through `whist.achievementCalibrationStreaks` and `whist.achievementCalibrationZero`. `whist.auditOutput` and `whist.auditSource` choose the Extent ratio and source identity. The output remains outside the warehouse; comparison 5/10 uses the same seedings in each phase, and the holdout uses new seedings. The load is not included in the primary collection and does not run E2E.

- **Execution:** of a temporary copy of the application, [Gradle configuration](../../build.gradle) select the components by `whist.displayComponents=true`.SDK is required to compile; tests run on JVM without ADB commands.

- **RASTER:** [dedicated collection](../rasterTest/README.md) uses the complete graphics JDK and a distinct task. His images are component samples, separated from the font, pixels and actual Android interaction.

- **Evidence:** The artifacts and results belong to the external execution. [Qualification of a relay](../../../../docs/testing/ATF_ReleaseQualification.md) decide on the necessary native selection after local checks.

[Application](../main/java/ro/whistlab/app/README.md) · [Common strategy](../../../../docs/testing/README.md) · [Components display](../../../../docs/testing/ATF_DisplayComponents.md)

## Whist projection of the common table

[WhistTableContentComponentTest](java/ro/whistlab/app/table/content/WhistTableContentComponentTest.java) checks all Whist modes and 3–6 seats: facts and accessibility, actor at bidding, play, AutoGame and collection, order and identity of cards, disabled human control and completed public trick. The test uses the actual Whist engine and stays in the application.

The components in the library do not import this session. They check the neutral presentation; each adapter keeps the oracles of its own rules, choices, sequences and scores.

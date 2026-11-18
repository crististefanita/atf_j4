# Whist Achievements - criteria remaining

## Overview

YAML validation at the building and compatibility of progress are documented in [contract of achievements](../game/ATF_Achievements.md). The works below aim at restoring the Android screen and calibration with human players.

## P1 · Restoration of the status of the common screen on Android

The Whist Adapter creates a new state of the screen at each opening. The components of the catalogue and detail alone do not demonstrate that the position of the list and the selected objective are retained when recreating the screen or after returning from the notification.

- Keep navigation status and selection in the owner of Android according to life cycle without changing order or content of the catalogue.
- Check RO/EN, select a target, run up to a row, recreate the screen and return to the same position and selection.
- The components shall cover the retention of the tail upon closure, the reappearance of unconfirmed notification and the refusal of an outdated confirmation. On APK, recreate activity before and after confirmation and face each appearance with tail status and saved progress. [Components of notification](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementNoticeCoordinatorComponentTest.java)
- Use existing checkpoints for catalogue and detail. Extend the sample with the recreation of activity in both languages and check for the return to the same objective, the same position and with the correct visible labels.

[Android adapter](../../whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementsScreen.java) · [Shared screen](../../whist-lab-app/atf-card-table-ui/README.md#achievements) · [Acceptance of Android](../../atf-test-android/src/test/java/atf/android/e2e/release/WhistReleaseHomeE2eTest.java)

## P2 · Calibration of Order with Human Players

Overflow simulation measures opportunities and rates for declared policies, not how natural order is for human players. It keeps human observation separate from AI calibration and changes thresholds only after a repeated signal.

- Uses RO/EN protocols and uniform criteria for understanding descriptions, difficulty and progress.
- Do not mix human results, development data and simulation holdout.
- Any change of threshold or eligibility keeps the IDs and passes through a proof of compatibility of the saved progress.

**Sources** · [Calibration model and protocol](../game/ATF_Achievements.md#verification) · [Offline suite](../../whist-lab-app/app/src/test/java/ro/whistlab/app/achievements/WhistAchievementOfflineCalibrationTest.java).

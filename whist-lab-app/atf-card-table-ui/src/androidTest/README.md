# Native components of table and card

## Overview

The instrumented tests check the actual geometry of the rows in the Android layout for representative font sizes and scalings.

The matrix measures the common Android components at 3–6 places, 240, 320 and 393 dp, with normal font, 1,5 × and 2 ×. The names with diacritics, the Whist statistics, the complete labels and the symbols of the "y 'are in the same box.

Each row shall have a full text, without ellipse, in the allotted width. The batches shall be mass-mounted and shall not intersect.

Build in the external workspace, install the isolated test APK and start the native runner.

```powershell
.\gradlew.bat --offline :atf-card-table-ui:assembleDebugAndroidTest
adb shell am instrument -w ro.cardtable.ui.test/ro.cardtable.ui.seat.SeatFrameComponentInstrumentation
```

Line `ATF_NATIVE_SEATS` records the measurements and result of each row. Field `cardHighlights` verifies the fan at 280 and 393 dp with font 1, 1,5 and 2. Proba compares the same limits and actions with and without marking and measures the lifting of an revealed card.

`gameTitleBars` checks 320, 360, 393 and 600 dp with font 1, 1,5 and 2, in Romanian and English. Each case measures the tick position, the tactile target and a single intention after the touch.

[Common test strategy](../../../../docs/testing/README.md)

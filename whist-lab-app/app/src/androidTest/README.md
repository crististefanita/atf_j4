# Native Android graphics components

## Overview

[Marking test](java/ro/whistlab/app/score/nativecomponent/HistoryMarksComponentInstrumentation.java) builds the production classes in a real `FrameLayout`, without activity, shoot or private data. Android measures text and applies internal running before drawing through parent; direct drawing of child on an empty Canvas does not reproduce this contract.

| Level | Oracle and limit |
|---|---|
| NATURAL Component | The presence of distinct ink of the cut for the missed contract and its absence for the exact contract; the point frame appears exclusively for adjustment, with different colors to the bonus and penalty. |
| Matrice | Bid values, width and font stairs; the numerical text has neutral ink to prevent substitution of the marking in pixel verification. |
| Evidence | Each case publishes the entry, the deployment of Android, the number of pixels, the verdict and the native PNG base64. These cases are not JUnit invoicing and do not increase the total JVM tests. |

## Implementation

Build in an identified external workshop with the existing SDK and development key:

```powershell
gradle :app:assembleDebugAndroidTest --offline
adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w ro.whistlab.app.test/ro.whistlab.app.score.nativecomponent.HistoryMarksComponentInstrumentation
```

Gradle configuration declares the runner itself; the default runner does not perform these samples. Check the identity and certificate of the target APK, book the device and keep the application status as per [relay qualification](../../../../docs/testing/ATF_ReleaseQualification.md).

- Tests run on the UI thread, keep the device's overall configuration and do not start navigation.
- Reflective access to the bid manufacturer remains exclusively in the sample: the production class has package visibility and does not become public API for testing.
- The runner returns the negative code to success; the verdict is read from `failures` and the result of the instrumentation, not only from the ADB trial code.

[JVM components](../test/README.md) · [Raster alternativ](../rasterTest/README.md) · [Common strategy](../../../../docs/testing/README.md)

# Application iOS Whist Lab

## Overview

The native target iOS is separated from the Android APK and requires qualification on Xcode and simulator prior to distribution.

## Status

The native target iOS is in skill. The project uses SwiftUI for interface
and J2ObjC to sweat out the existing Java engine, so that the rules,
bids, score, opponents and validation of actions to remain implemented a
only once. The adapter only displays the status required for the screen to Swift, and
human actions; the hands of the opponents are not included in the public state.

Configuration declares iOS 17 as a target deployment target and simulator iOS 27.2 as target
current qualification. Support gets qualified after a green run of the workshop
`iOS simulator qualification`; the verdict of the simulator must be attached to the commit
release before the support was declared qualified.

## Structure

| Cale | Rol |
|---|---|
| `WhistLab/` | Swiftui interface, screen model, local checkpoint and C-Objective deck. |
| `WhistLabTests/` | Smoke test on the engine translated, run in simulator. |
| `project.yml` | XcodeGen configuration, iOS identifier and deployment target. |
| `scripts/prepare-shared-core.sh` | Translate Java sources and check J2ObjC artifacts. |
| `../whist-lab-app/ios-bridge/` | API iOS and JVM tests for the bridge contract. |

## Development and verification

- **Requirements: ** macOS, Xcode 27.2, JDK 21, Homebrew, XcodeGen and J2ObjC 3.1.
  Windows Development Machine does not include Xcode.
- **Workflow: ** public runner `xcode-27` builds J2ObjC distribution from
  the set tag, run engine and adapter tests, then the application cylinder
  and test in simulator iOS 27.2.
- **Native dependencies: ** project connects `libjre_emul` and `libiconv`, necessary
  J2ObjC runtime. See [ghidul iOS](../docs/platform/ATF_iOSDelivery.md).

```sh
cd whist-lab-app
./gradlew :game-core:test :ios-bridge:test
```

- The simulator cylinder is not an IPA signed and does not install it on a iPhone.
- Publication via TestFlight or App Store requires Apple archiving and signing;
  distribution steps iOS remain separate from Android APK.

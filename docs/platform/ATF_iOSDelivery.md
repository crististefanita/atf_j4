# Compatibility and delivery iOS

## Overview

The apk of the Whist Lab is an Android package and does not install on iPhone or iPad.
Project iOS is a separate SwiftUI interface application and uses the engine
Joint Java by J2ObjC translation. The original engine continues to be the source of the rules
game, AI, bidding, score and save; adapter iOS does not implement a second
variant of these rules.

The Depletion Targetul is iOS 17.0. Current Qualification uses iOS 27.2 and
Xcode 27.2, latest stable versions found on 7 November 2026; iOS 27.3
It's still beta. Apple versions change; simultaneously update the runner,
The simulator runtime and this page after checking official documentation.
[Versiunile Apple](https://developer.apple.com/news/releases/) ·
[Xcode Matrix and SDK](https://developer.apple.com/xcode/system-requirements/)

## Qualification on GitHub Actions

A macOS hosted runner allows build and tests without local Mac.

- For public repositoryes, standard runners are free and unlimited.
- For private repositorys, consumption uses the minutes included, then is charged at the plan rate. Check visibility and billing before estimating cost.
- The image `xcode-27` is public preview and has no guarantees of a stable runner.
- On 7 November 2026, the image includes Xcode 27.2 and the simulator iOS 27.2. the workflower fixes the version instead of choosing `latest`.
[Runners and conditions GitHub](https://docs.github.com/en/actions/reference/runners/github-hosted-runners) ·
[Current image macOS Xcode 27](https://github.com/actions/runner-images/blob/main/images/macos/xcode-27-arm64-Readme.md) ·
[Preview Xcode 27](https://github.blog/changelog/2026-09-10-xcode-27-runner-image-now-runs-on-macos-27/)

The [iOS](../../.github/workflows/ios.yml) workshop checks on the same commit:

1. JVM tests of the native engine and adapter;
2. translation of Java sources with J2ObjC;
3. The application building for the simulator;
4. Swift test smoke in simulator iOS 27.2.

J2ObjC 3.1 is fixed in workflow and built from source because distribution
pre-compiled is no longer published by the project. J2ObjC is beta and translates logic,
not the interface; SwiftUI remains the native layer of IU. The runner cylinder qualifies
compatibility of the compilation and automatic test for iOS 27.2, not each
intermediate version and no experience on physical devices.
[J2ObjC - project and limits](https://github.com/google/j2objc) ·
[J2ObjC building](https://developers.google.com/j2objc/guides/building-j2objc)

## Delivery steps

1. Run the `iOS simulator qualification` workflow on the source compost of the relay.
2. Check the test results and keep the `xcresult`the attached to the run.
3. For TestFlight or App Store, archive with Xcode, sign with identity
   be able to use the system and validate installation on devices iPhone and iPad.
4. Publish the iOS app separately through Apple's distribution flow. The Android APK and hash
   serve only as Android artifacts and evidence.

The simulator runner builds the app without Apple signing. Creation of IPA
Distribuible and upload it in TestFlight/App Store asking for an Apple Developer
Team, certificates, profiling profile and Apple release steps; these
Accreditation is not stored in the reposition or in the public workshop.

[Livrarea Android](ATF_AndroidDelivery.md) · [Proiectul iOS](../../ios/README.md)

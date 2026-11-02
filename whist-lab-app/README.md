# Whist Lab

Whist Lab is a local Android counterpart for learning and testing the Contract
Whist gameplay flow. It has a pure Java rules module and a small Android UI;
the original `com.qsoft.android.whist` package remains separate and untouched.

## Current behavior

- `Whist romanesc (1-8-1)` is a separate selectable mode with mandatory trump
  when void, reduced deck, eight-card no-trump hands, and `5 + tricks` for an
  exact bid or a negative bid-error penalty. See the
  [rule contract, component coverage and AI tests](game-core/README.md).
- 3–6 seats, one human plus local AI opponents.
- Oh Hell mode; original screenshots show 10 cards for 3–5 players and 8 for 6.
  The retained four-player live game advanced 10→1→8 before the pause in hand
  17 of its 19-hand 10→1→10 schedule. Full original completion is still pending.
- Descending mode remains separate; it starts at the largest whole hand that
  fits in the 52-card deck and table size.
- Oh Hell: follow suit when possible; otherwise discard or play trump.
- Oh Hell trump comes from an undealt turned card. Descending uses the fixed
  spades/hearts/clubs/diamonds/no-trump cycle. The mode distinction resolves
  the previously unexplained varying initial trump and transitions.
- Oh Hell and Descending score one per trick plus ten for an exact bid.
- Normal AI uses a basic card-strength estimate and plays its lowest legal card.
- Hard AI estimates bids with trump strength and tries to manage its remaining
  trick requirement while playing.
- Expert AI samples possible hidden deals using its own cards and public
  information only, then compares expected contract points. The component
  tournament compares it against Hard on paired deals and rotated seats.
- A one-card practice hand supports deterministic end-to-end testing.
- The current game checkpoint is saved locally after bids and card plays;
  `Resume game` restores it after Android kills or restarts the app process.

Oh Hell evidence comes from the original app's instructions and captured
version-145 game states, checked by an independent integer-card oracle.
Romanian Whist uses the separately sourced contract in the game-core README;
it is not a claim about the Android original's behavior. Checkpoints use a
versioned JSON record in private SharedPreferences and are validated when
restored. Version 2 includes the public played-card history; version 1 remains
readable with only its retained last/current trick known. Online multiplayer
and unobserved original-app variants remain outside acceptance. The test fixture is debug-only and
uses a fixed deal: human A♥, AI K♥/Q♥/J♥, Spades trump. Bidding 1 and playing A♥
must produce the human score `+11`, total `11`.

## Structure

- `game-core`: deterministic rules, session state, AI policy, and JVM tests.
- `app`: Android activity, setup, play, score UI, and process-death checkpoint
  storage.
- J4 `atf-android`: ADB and screenshot adapters.
- J4 `atf-test-android`: opt-in live acceptance tests for both the original app
  and Whist Lab, with Extent screenshots embedded inline as Base64.

## Build and install

Start with component acceptance and the native Extent report, without Android:

```powershell
C:\work\1\atf_j4\scripts\run-rule-components.ps1
```

PowerShell, with Android SDK at `C:\tools\whist-android-sdk`:

```powershell
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat --no-daemon --console=plain :game-core:test :app:assembleDebug
```

Install the debug APK on the API 28 emulator:

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
C:\tools\whist-android-sdk\platform-tools\adb.exe connect 127.0.0.1:5555
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 install -r `
  C:\work\1\atf_j4\whist-lab-app\app\build\outputs\apk\debug\app-debug.apk
```

Launch the deterministic practice flow:

```powershell
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 shell am force-stop ro.whistlab.app
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 shell am start -W `
  -n ro.whistlab.app/.MainActivity --ez debug_fixture true
```

The full J4 run builds, installs, and tests Whist Lab and runs the original app
acceptance suite before publishing the combined Extent report:

```powershell
C:\work\1\atf_j4\scripts\run-deep-tests.ps1
```

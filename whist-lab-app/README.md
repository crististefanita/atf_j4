# Whist Lab

Whist Lab is a local Android counterpart for learning and testing the Contract
Whist gameplay flow. It has a pure Java rules module and a small Android UI;
the original `com.qsoft.android.whist` package remains separate and untouched.

## Current behavior

- Romanian setup includes a five-hand streak option and a positive reward selector:
  **5 points (original)** or **10 points**, default **10**. Five missed contracts
  still deduct 5. One-card hands do not advance a streak, but an opposite result
  breaks it. Rewards reset the series. The per-contract score remains `5 + tricks`.
  The selected rule and counters survive process restart; old saves retain classic
  scoring without invented historical series. See the
  [complete original Instructions coverage](../docs/ORIGINAL_INSTRUCTIONS_COVERAGE.md).
- Down & Up, Maximum, Long Romanian (8–1–8), and Reverse Oh Hell are separate
  modes. Descending supports the full 17-card maximum with three players.
- `Whist romanesc (1-8-1)` is a separate selectable mode with mandatory trump
  when void, reduced deck, eight-card no-trump hands, and `5 + tricks` for an
  exact bid or a negative bid-error penalty. See the
  [rule contract, component coverage and AI tests](game-core/README.md).
- 3–6 seats, one human plus local AI opponents.
- Oh Hell mode; original screenshots show 10 cards for 3–5 players and 8 for 6.
  The retained four-player live game completed its 19-hand 10→1→10 schedule.
  All 436 card plays and 109 trick winners passed the independent oracle;
  exact replay in this engine and Android UI ended at the original totals
  `[89,126,145,119]`.
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
- Normal `New game` and `Play again` clear debug replay. The normal Romanian
  Expert mobile check played a complete first hand, verified its winner and
  score independently, then checked dealer rotation and cumulative totals.
- The active mode explains its rules during setup. During play the app explains
  following suit, mandatory trump and optional overtrumping, shows match progress,
  and announces final winners including ties. Complete v2/v3 checkpoints audit
  every public historical card; partial legacy saves retain their v1 marker.

See the [complete-game acceptance record](../docs/COMPLETE_GAME_ACCEPTANCE.md)
for native Extent reports, inline Base64 screenshots, exact APK provenance,
component coverage and the distinction between full replay and normal-game checks.
The [Romanian Expert acceptance](../docs/ROMANIAN_EXPERT_ACCEPTANCE.md) records
the subsequent component expansion and full normal Android game: all 24 hands,
94 tricks and 376 cards passed, with final totals `[12,87,91,61]`. The app
ended at that completed score sheet; Miles won with 91 points. That historical
APK is archived outside the repository with its exact hash. The current AI
training run uses a separate APK, a public-information challenger and the
configurable series profile; its receipts stay outside J4.

Oh Hell evidence comes from the original app's instructions and captured
version-145 game states, checked by an independent integer-card oracle.
Romanian Whist also has a full reading of the original menu, including its
variant-specific sections and five-hand series rule. Instructions coverage
separates original value 5, the requested value 10, and classic without series.
Checkpoints use a
versioned JSON record in private SharedPreferences and are validated when
restored. Version 3 adds rule configuration, series counters and earned series
points; version 2 has full history and remains readable. Version 1 retains only
its known last/current trick. Online multiplayer and ratings remain outside
acceptance. The test fixture is debug-only and
uses a fixed deal: human A♥, AI K♥/Q♥/J♥, Spades trump. Bidding 1 and playing A♥
must produce the human score `+11`, total `11`.

## Structure

- `game-core`: deterministic rules, session state, AI policy, and JVM tests.
- `app`: Android activity, setup, play, score UI, and process-death checkpoint
  storage.
- `ai-lab`: independent test-only player shared by JVM tournaments and Android
  acceptance. It imports no game-core classes and is not included in the APK.
- J4 `atf-android`: ADB and screenshot adapters.
- J4 `atf-test-android`: opt-in live acceptance tests for both the original app
  and Whist Lab, with Extent screenshots embedded inline as Base64.

## Build and install

The current AI/rule pipeline stages sources and flat binaries in an isolated TMP
folder, keeps evidence outside J4, and runs components before optional Android:

```powershell
C:\work\1\atf_j4\scripts\run-whist-ai-validation.ps1
C:\work\1\atf_j4\scripts\run-whist-ai-validation.ps1 -WithAndroid -PositiveSeriesBonus 10
```

The Android option reads the original Instructions, checks all menu options,
plays a complete normal Expert game with an independent challenger, reconstructs
it offline, and publishes native Extent with inline Base64 PNGs. It preserves
regressions and losses. A measured win does not imply perfect hidden-information
play; details are in the [AI protocol](../docs/AI_TRAINING_PROTOCOL.md).

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

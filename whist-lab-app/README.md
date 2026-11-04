# Whist Lab

Whist Lab este versiunea Android locală pentru învățarea și verificarea fluxului
Contract Whist. Include un modul de reguli Java pur și o interfață Android
compactă. Pachetul aplicației originale, `com.qsoft.android.whist`, rămâne
separat și nemodificat.

## Distribuție și acceptanță

Versiunea 0.1.5 (cod 6) folosește `targetSdk 36` și `minSdk 24`. Foaia de scor păstrează licitațiile, punctele și totalurile pe mâini. În `dist` se păstrează ultimul APK verificat și suma sa SHA-256. Pentru binarul
instalat, matricea Android și limitele probei, consultați [acceptanța versiunii](../docs/WHIST_UI_015_ACCEPTANCE.md).

[Atlasul interfeței](app/src/main/java/ro/whistlab/app/README.md) leagă fiecare
regulă de layout de componenta care o aplică. [Feedback-ul UX](../docs/feedback/WHIST_LAB_User_Experience_Feedback.md)
păstrează prioritățile și criteriile de închidere.

## Comportament actual

- Interfața poate fi folosită în engleză sau română. Engleza este limba implicită
  pentru instalările noi. Alegerea se face din selectorul de limbă de pe ecranul
  principal, configurarea mesei sau regulile de joc și se păstrează între porniri.
  Selectorul schimbă împreună meniurile, interfața partidei și regulile.
- Configurarea Whistului românesc include o serie opțională de cinci mâini și un
  bonus pozitiv de **5 puncte (original)** sau **10 puncte**, implicit **10**.
  Cinci contracte ratate scad tot 5 puncte. Mâinile cu o carte nu avansează seria,
  dar un rezultat opus o întrerupe. Bonusul se resetează după acordare; contractul
  exact valorează în continuare `5 + levate`. Alegerea și contoarele se păstrează
  după repornirea procesului; salvările vechi nu primesc retroactiv serii
  inventate. Vezi [acoperirea completă a regulilor originale](../docs/ORIGINAL_INSTRUCTIONS_COVERAGE.md).
- Meniul de partidă nouă oferă Oh Hell, Descending, Whist românesc, Down & Up, Long Romanian și Reverse Oh Hell. Partidele salvate în Maximum și Practice Hand se pot relua. Descending poate ajunge
  la 17 cărți cu trei jucători.
- `Whist românesc (1–8–1)` este o variantă separată: pachet redus, atu obligatoriu
  când jucătorul nu are culoarea cerută și deține atu, mâinile de opt cărți sunt
  fără atu, iar contractul exact valorează `5 + levate`; ratarea scade diferența
  absolută. Vezi [contractul regulilor și testele de componentă](game-core/README.md).
- Masa suportă 3–6 jucători: un om și adversari locali controlați de AI.
- Oh Hell: capturile originale arată 10 cărți pentru 3–5 jucători și 8 pentru 6.
  Partida istorică păstrată cu patru jucători a parcurs programul de 19 mâini
  `10→1→10`. Oracolul independent a verificat toate cele 436 de cărți jucate și
  109 câștiguri de levată; reluarea exactă în motor și Android a produs totalurile
  originale `[89,126,145,119]`.
- Descending rămâne separat și începe cu cea mai mare mână care încape în pachetul
  standard de 52 de cărți și la numărul de jucători ales.
- Oh Hell: trebuie urmată culoarea dacă este disponibilă; altfel se poate decarta
  sau tăia cu atu. Atuul vine dintr-o carte neîmpărțită, întoarsă. Mâinile de opt
  cărți ale Whistului românesc nu au atu; Descending folosește ciclul fix pică,
  cupă, treflă, caro, fără atu.
- Oh Hell și Descending acordă un punct pentru fiecare levată și încă zece pentru
  contractul exact.
- Meniul oferă exact două niveluri, Normal și Hard. Normal păstrează politica
  simplă; Hard folosește motorul acceptat și garda publică R14. `EXPERT` este
  numai un alias istoric migrat la Hard. [Contractul nivelurilor](../docs/TWO_GAME_LEVELS.md)
  descrie dispatch-ul, compatibilitatea salvărilor și limitele demonstrațiilor.
- Mâna de antrenament cu o singură carte permite verificări Android deterministe.
- Titlul identifică varianta, iar atuul ocupă un singur loc în antet. O carte
  întoarsă își arată rangul și culoarea. Rundele cu un atu stabilit prin regulă
  indică explicit absența cărții întoarse.
- Configurarea reunește seria și valoarea bonusului pe același rând. `Rules &
  schedule` / `Reguli și program` deschide regulile și distribuția exactă pentru
  numărul de jucători ales. Limba se selectează din dropdown-ul cu chevron.

## Principii de layout pentru jucător

| Decizia jucătorului | Ce trebuie să vadă |
|---|---|
| Cui îi vine rândul? | Numele și insigna activă ocupă o poziție stabilă. Fiecare scaun arată separat licitația și levatele câștigate. |
| Cât licitez? | Opțiunile legale și confirmarea sunt în centru. Ținta tactilă are cel puțin 48 dp. Grila și cele 3–6 scaune se măsoară împreună. |
| Ce carte joc? | Mâna stă jos pe un singur rând. Rangul este deasupra simbolului în banda expusă. La selectare, cartea se ridică în spatele vecinelor. |
| Unde urmăresc levata? | Cărțile publice apar în centru și ultima levată completă rămâne 1.500 ms înainte de continuare. |
| Cum văd scorul? | Deschideți **Meniu joc → Scorul partidei → Istoricul scorului**. Alegeți un jucător pentru licitația, punctele și totalul fiecărei mâini. Aceeași opțiune apare după încheierea mâinii. |

Pâsla este continuă. Omul rămâne în sud, iar adversarii formează conturul în
jurul zonei centrale. La patru jucători, flancurile stau la vest și est când
centrul încape între ele. Pentru o grilă lată, urcă spre nord-vest și nord-est.
Poziția rezultă din dreptunghiurile măsurate. Selectarea unui bid păstrează
pozițiile deja calculate.

Lățimea utilă, înălțimea disponibilă, densitatea și textul influențează separat
randarea. Ferestrele scunde recuperează spațiu din separatoare, mesajul secundar
și rezerva de ridicare în faza de licitare. Fețele cărților păstrează 98 dp.
Pentru text mărit și mâini foarte lungi, rândul poate derula orizontal.

Pentru reproiectare, porniți de la [atlasul cu diagramă și invariante](app/src/main/java/ro/whistlab/app/README.md)
și de la [criteriile de acceptanță ale ecranelor](../docs/WHIST_UI_013_ACCEPTANCE.md).
Vezi [acceptanța partidei complete](../docs/COMPLETE_GAME_ACCEPTANCE.md) pentru
rapoartele Extent native, capturile Base64 încorporate, proveniența APK-ului,
acoperirea de componentă și diferența dintre reluarea integrală și verificarea
unei partide normale. [Acceptanța Expert în română](../docs/ROMANIAN_EXPERT_ACCEPTANCE.md)
documentează extinderea ulterioară a testelor de componentă și partida Android
normală: 24 de mâini, 94 de levate și 376 de cărți verificate, cu totalurile
finale `[12,87,91,61]`. Aplicația a rămas pe foaia de scor finală; Miles a
câștigat cu 91 de puncte. APK-ul istoric este arhivat în afara repository-ului,
împreună cu hash-ul exact. Rularea curentă de antrenare AI folosește alt APK, un
adversar cu informație publică și profilul de serii configurabil; dovezile rămân
în afara J4.

Dovezile Oh Hell provin din instrucțiunile aplicației originale și din stările
capturate ale versiunii 145, verificate cu un oracol independent pentru cărți
întregi. Whistul românesc are și o analiză completă a meniului original, inclusiv
secțiunile specifice variantei și seria de cinci mâini. Acoperirea instrucțiunilor
separă valoarea originală 5, valoarea cerută 10 și profilul clasic fără serie.
Checkpoint-urile folosesc JSON versionat în `SharedPreferences` privat și sunt
validate la restaurare. Versiunea 5 adaugă ofertele la registrul de scor introdus în v4. Acesta păstrează totalurile fiecărui loc după fiecare mână încheiată.
Diferența dintre două totaluri succesive include și punctele seriei. O salvare
v1–v3 păstrează soldul anterior cunoscut. Dacă mâna salvată tocmai s-a încheiat,
tabela recuperează și punctele ei din licitație, levate și bonusul înregistrat.
[Contractul și replay-ul pe pilotul Android](../docs/WHIST_SCORE_HISTORY.md) descriu migrarea.
Versiunea 1 păstrează doar levata cunoscută, curentă sau ultima. Multiplayer-ul
online și clasamentele nu fac parte din acceptanță. Fixture-ul de test este
disponibil numai în build-ul de depanare și folosește o distribuție fixă: A♥
pentru om, K♥/Q♥/J♥ pentru AI și pică atu. Licitarea 1 și jocul A♥ trebuie să
producă scorul omului `+11`, total `11`.

## Structură

- `game-core`: reguli deterministe, starea sesiunii, politica AI și teste JVM.
- `app`: activitatea Android, configurarea, jocul, scorul și salvarea checkpoint-
  ului la oprirea procesului.
- `ai-lab`: jucător independent numai pentru teste, partajat de turneele JVM și
  acceptanța Android. Nu importă clase din game-core și nu intră în APK.
- J4 `atf-android`: adaptoare ADB și capturi de ecran.
- J4 `atf-test-android`: teste live de acceptanță opționale pentru aplicația
  originală și Whist Lab, cu capturi Extent încorporate ca Base64.

## Build și instalare

Pipeline-ul actual pentru reguli și AI copiază sursele și binarele într-un
director TMP izolat, păstrează dovezile în afara J4 și rulează componentele
înaintea verificărilor Android opționale:

```powershell
C:\work\1\atf_j4\scripts\run-whist-ai-validation.ps1
C:\work\1\atf_j4\scripts\run-whist-ai-validation.ps1 -WithAndroid -PositiveSeriesBonus 10
```

Opțiunea Android citește instrucțiunile originale, verifică toate opțiunile din
meniu, joacă o partidă normală Expert cu un adversar independent, reconstruiește
partida offline și publică Extent nativ cu capturi PNG Base64 încorporate.
Regresiile și pierderile se păstrează. O victorie măsurată nu înseamnă joc
perfect cu informație ascunsă; detaliile sunt în [protocolul AI](../docs/AI_TRAINING_PROTOCOL.md).

`WhistLabGameplayE2eTest` păstrează probele de setup, navigare și scor, iar
`thirteenCardFanKeepsOneRowAndMovesTheChosenCardIntoTheTable` adaugă un singur
flow UI nou: deschide fixture-ul determinist de 13 cărți, atinge o carte legală,
verifică levata centrală, mâna rămasă de 12 cărți și poziția neschimbată a
locului omului. Un flag de test ține AI-ul pe loc numai în fixture-ul de debug,
ca să nu transforme proba într-o partidă completă.
Testele de componentă `GameTableLayoutComponentTest` și `WhistUiTextTest` fixează
geometria mâinii, rândurile de licitare, locurile și textele localizate. Pentru
contractele UI fără device, se rulează `:app:testDebugUnitTest`; pentru regulile,
`:game-core:test`.

Build-ul Android se face într-o copie de lucru sub `%TEMP%`, nu în J4. Exemplu:

```powershell
$source = 'C:\work\1\atf_j4\whist-lab-app'
$stage = Join-Path $env:TEMP ('whist-j4-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
robocopy $source $stage /E /XD '.gradle' 'build' 'algorithm-build' | Out-Null
Push-Location $stage
try { .\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :game-core:test :app:assembleDebug }
finally { Pop-Location }
```

Testele E2E folosesc APK-ul construit în același staging TMP; capturile și
raportul Extent sunt scrise în `%TEMP%` implicit sau în `atf.android.artifacts`.
Raportul folosește același API UI Extent ca testele Playwright: fiecare checkpoint
Gherkin are propriul pas expandabil, tabel ACTION/TARGET/EXPECTED/OBSERVED/
TIME/RESULT dacă s-a expediat o interacțiune reală, captura PNG full-display,
zona colapsabilă de observații și tabelul ASSERTION/EXPECTED/ACTUAL/RESULT.
Rezultatul tap-ului rămâne observație/assert, nu se deduce din acceptarea comenzii
ADB; timpul măsurat exclude pauza fixă de stabilizare UI.
Receipt-ul păstrează identitatea rulării și numărătorile, iar XML/properties sunt
fingerprint-uri SHA-256 colapsate, fără dump-uri sau linkuri către fișiere locale.
Fiecare checkpoint păstrează o observație; tabelul ACTION apare numai dacă a fost
expediată o interacțiune reală. Verificările fără click au aserțiunile și captura
proprii, fără click-uri simulate. Testele componente acoperă asocierea dovezilor,
starea doar-observare și diagnosticul FAIL fără checkpoint. E2E-ul verifică
rapoartele live pentru masa cu 8 cărți și mâna cu 13 cărți.

Începe cu acceptanța de componentă și raportul Extent nativ, fără Android:

```powershell
C:\work\1\atf_j4\scripts\run-rule-components.ps1
```

Din copia `whist-lab-app` pregătită în TMP, cu SDK-ul Android local:

```powershell
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug
```

Configurația curentă a APK-ului este `minSdk 24`, `compileSdk 36`,
`targetSdk 36`, `versionCode 6` / `versionName 0.1.5`. `targetSdk 36`
înlocuiește API 28: țintele cu peste două niveluri API sub versiunea Android a
telefonului pot declanșa avertizarea Play Protect „Unsafe app blocked / built
for an older version of Android”. Ridică `targetSdk` la nivelul cerut la acea
dată și verifică separat comportamentele Android noi. Emulatorul API 28 confirmă
funcționarea pe această versiune. API 24 și Android 16 necesită probe proprii. Google Play cere
API 36 pentru trimiteri noi și actualizări începând cu 31 august 2026 ([detalii](https://developer.android.com/google/play/requirements/target-sdk)).

APK-urile din `dist` sunt build-uri debug, nu pachete de release. Actualizările
trebuie semnate consecvent cu cheia de release; altă semnătură produce
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, iar dezinstalarea pachetului vechi poate
șterge datele sale.

Instalează APK-ul de depanare pe emulatorul API 28:

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
C:\tools\whist-android-sdk\platform-tools\adb.exe connect 127.0.0.1:5555
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 install -r `
  C:\work\1\atf_j4\dist\WhistLab-0.1.5-debug.apk
```

Pornește fluxul determinist de antrenament:

```powershell
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 shell am force-stop ro.whistlab.app
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 shell am start -W `
  -n ro.whistlab.app/.MainActivity --ez debug_fixture true
```

Rularea completă J4 construiește și instalează Whist Lab, rulează acceptanța
aplicației originale și publică raportul Extent combinat:

```powershell
C:\work\auto\atf_j4\scripts\run-deep-tests.ps1
```

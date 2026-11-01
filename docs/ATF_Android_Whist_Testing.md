# J4 — Contract Whist în emulator Android local

## Scop

Păstrăm o cale repetabilă pentru a porni și testa local aplicația gratuită
Contract Whist (`com.qsoft.android.whist`) pe emulatorul Android rulat prin
Podman pe gazda fizică și controlat din Windows VM.

## Configurația observată

| Componentă | Valoare / stare | Proveniență |
|---|---|---|
| Aplicație | Contract Whist (Oh Hell), pachet `com.qsoft.android.whist`, versiunea 4.5 (145) | verificare ADB |
| Compatibilitate instalată | minSdk 24, emulator API 28, targetSdk 36 | metadate pachet ADB |
| Instalare | Instalată; Play Store a afișat `Uninstall` și `Play`, cu verificarea Play Protect | observat vizual în emulator la 1 noiembrie 2026 |
| Lansare | `pm path` a găsit `base.apk`; `monkey` a pornit pachetul | ADB prin gazdă, 1 noiembrie 2026 |
| Emulator | imagine Google Play, API 28; `emulator-5554` apare `device` | ADB prin gazdă, 1 noiembrie 2026 |
| ADB în VM | client `X:\scrcpy-win64-v5.0.1\adb.exe`, server `tcp:192.168.56.1:5038` | verificat în VM, 1 noiembrie 2026 |
| Afișare | scrcpy cu serialul `emulator-5554` | verificat în VM, 1 noiembrie 2026 |
| Runda locală | 4 jucători, Descending, AI Normal; Bid 0, două mutări ale jucătorului în ture consecutive, cu AI-ul încheind levata dintre ele | test UI ADB headless, 1 noiembrie 2026 |
| High Scores | dialogul de nume este dependent de starea persistentă: prima rulare îl închide cu Cancel; rulările următoare verifică întoarcerea la meniu fără ștergerea datelor | capturi UI, 1 noiembrie 2026 |
| Resume Game | după force-stop/start a redeschis aceeași partidă; testul separat a găsit același număr de pixeli ai cărților imediat și după 6 secunde (485301) | test UI și capturi, 1 noiembrie 2026 |
| Multiplayer | butonul deschide promptul de editare a numelui; Cancel revine la meniu. Nu s-a creat sau accesat o sesiune online. | explorare UI, 1 noiembrie 2026 |
| Randare | defect sever: fundal verde neon, text foarte mare și suprapus, cărți distorsionate, rândurile mâinii tăiate/deplasate spre bara de navigare | capturile testului din 1 noiembrie 2026 |
| Podman healthcheck | raportat `unhealthy`, deși ambele transporturi observate au pornit Android; check-ul intern folosește ADB fără selector și vede transporturi multiple | diagnostic raportat anterior de pe gazdă, 1 noiembrie 2026 |
| Rețea | host-only; accesul ADB raportat limitat la VM `192.168.56.101`; Kafka/ruta lui nemodificate | configurare raportată de operator |

Autentificarea Google a fost făcută de utilizator, nu de agent. ADB direct la
`192.168.56.1:5555` a dat anterior `unauthorized` pentru cheia VM. Calea folosită
acum este serverul ADB al gazdei pe portul 5038 și serialul explicit.

## Test UI în J4

`atf-android` oferă un client ADB cu timeout și selectare explicită de serial,
acoperit de 5 teste de contract. `atf-test-android` conține
`ContractWhistGameplayE2eTest`, activat prin `ContractWhistE2eSuite`; suita
comună serializată `AndroidGameplayE2eSuite` rulează originalul și Whist Lab:

1. verifică instalarea și deschide meniul;
2. citește instrucțiunile pentru licitare, joc și punctaj;
3. verifică promptul inițial High Scores sau revenirea la meniu dacă numele a
   fost deja salvat de o rulare anterioară;
4. citește selecțiile curente, deschide și enumeră opțiunile pentru jucători,
   variante de joc și AI, apoi închide selectorii fără să le schimbe și verifică
   faptul că toate selecțiile inițiale au rămas intacte;
5. pornește jocul, verifică opțiuni numerice de licitare și licitează Bid 0;
   în Oh Hell se afișează `Bid 0`–`Bid 10`, fără butonul `Bid Other`;
6. licitează Bid 0, probează centrele cărților până când prima mutare este
   acceptată, așteaptă AI-ul să încheie levata, apoi verifică o a doua mutare;
7. separat, redeschide jocul salvat din Resume Game și verifică păstrarea
   vizibilității cărților după șase secunde.

Ultima rulare completă este `20261101-195037`. Reactorul J4 a produs 494 de
cazuri Surefire: 493 trecute, 0 eșecuri, 0 erori și un test publisher omis
intenționat în rularea obișnuită a modulelor; publisher-ul a rulat apoi explicit
și a trecut. Android component suite a trecut 5/5. Android E2E a trecut 3/3:
originalul 2/2, Whist Lab 1/1. Separat, motorul `game-core` a trecut 20/20 teste
Gradle, iar APK-ul debug s-a construit și instalat.

Rularea live a originalului a verificat meniurile, instrucțiunile, toate
opțiunile de configurare fără să schimbe setările salvate, licitarea, două
mutări cu un tur AI între ele și vizibilitatea jocului salvat după stabilizare.
Nu s-a verificat o partidă completă sau scorul calculat de original. În Whist
Lab, testul a licitat 1, a oprit forțat procesul Android, a reluat aceeași tură
din `Resume game`, a jucat A♥ și a verificat `+11`, total `11`.

Raportul Extent final este
`artifacts/deep-app-test/runs/20261101-195037/final-extent-report.html`.
Are 7.076.637 bytes și 15 imagini PNG inline `data:image/png;base64,...`.
Dosarul păstrează PNG-urile originale, XML-urile UI, rezultatele Surefire și
logurile. J10/UiExtentReportSession au atașat capturile inline la pașii UI,
iar publisher-ul a trecut verificarea payload-urilor exacte. Raportul rămâne
WARNING pentru defectele vizuale ale aplicației originale. La rerulare, scrcpy
poate rămâne închis; sunt suficiente emulatorul pornit și ADB.

Tabla și cărțile sunt desenate într-un singur `android.view.View`, fără text sau
controale de carte accesibile prin UIAutomator. Captura observată pentru
configurația cu patru jucători arată zece cărți, în două rânduri de câte cinci;
vechea notă despre treisprezece cărți și împărțirea 6+7 era incorectă și a fost
eliminată. Adapterul folosește centrele a cinci sloturi per rând și cere atât o
schimbare stabilă a mâinii, cât și una în zona de joc pentru a număra mutarea.
Geometria rămâne calibrată la emulatorul 1080×1920; distribuția din mâini mai
mici și alte densități trebuie confirmată separat.

Interacțiunea de bază funcționează: două cărți au fost jucate în ture succesive,
iar AI-ul a încheiat levata dintre ele. Testul nu validează independent
oracolul regulilor sau punctajul.
Capturile nu trec însă acceptanța vizuală: masa are
fundal verde neon, panourile și literele sunt supradimensionate, imaginile
cărților apar distorsionate, iar mâna este tăiată la marginea de jos. Testul nu
verifică încă integral corectitudinea punctajului, toate turele, finalizarea
rundelor sau multiplayer.

## Aplicația locală în paralel — Whist Lab

Codul aplicației este izolat în `C:\work\1\whist_lab_app`: `game-core` conține
regulile, sesiunea, AI-ul și testele JVM, iar `app` conține UI-ul Android.
Pachetul `ro.whistlab.app` este separat de original. Regulile confirmate în
instrucțiunile originale (licitație, culoare, atu și scor) sunt implementate
local; checkpoint-ul partidei se salvează ca JSON versionat în SharedPreferences
după fiecare mutare/licitație (inclusiv AI) și după trecerea la următoarea mână.
Reluarea după oprirea procesului este verificată live pe API 28. Variantele
neobservate și multiplayer nu sunt încă acoperite.

`WhistLabGameplayE2eTest` exercită fixture-ul deterministic pe API 28: omul
licitează 1, procesul este oprit forțat, `Resume game` restaurează exact tura de
joc, omul joacă A♥, AI-ul termină levata, iar tabelul de scor arată `+11`, total
`11`. Cele patru capturi (licitație, joc, reluare și scor) sunt incluse inline
Base64 în `whist-lab-gameplay.html`. Testul live a trecut 1/1 în rularea
`20261101-195037`. J4 folosește același `AdbClient`, adapterul UI Automator și
J10 Extent Reporting pentru ambele pachete.

`run-deep-tests.ps1` construiește APK-ul și testele core, îl instalează în
emulator, rulează `AndroidGameplayE2eSuite` (original plus Whist Lab), apoi
agregă toate rezultatele și capturile într-un singur Extent final. Parametrul
`-WhistLabProject` poate indica altă locație a proiectului.

Setările acceptă proprietăți Java sau variabile de mediu:

| Proprietate Java | Variabilă de mediu | Implicit |
|---|---|---|
| `atf.android.adb` | `ATF_ANDROID_ADB` | `adb` din PATH |
| `atf.android.serial` | `ATF_ANDROID_SERIAL` | `emulator-5554` |
| `atf.android.adbServerSocket` | `ADB_SERVER_SOCKET` | valoare moștenită din environment sau implicit local ADB |
| `atf.android.commandTimeoutSeconds` | `ATF_ANDROID_TIMEOUT_SECONDS` | `30` |

Rularea componentelor, flow-ul Android și raportul cu capturi se pornesc din
rădăcina `C:\work\1\atf_j4`:

```powershell
.\scripts\run-deep-tests.ps1
```

Parametrii ADB se pot schimba dacă VM-ul folosește altă literă sau alt serial:

```powershell
.\scripts\run-deep-tests.ps1 -AdbExecutable 'X:\scrcpy-win64-v5.0.1\adb.exe' `
  -Serial 'emulator-5554' -AdbServerSocket 'tcp:192.168.56.1:5038'
```

Default build nu contactează emulatorul. Nu se salvează credentiale Google și
nu se arhivează logcat implicit. Capturile UI pot include informații afișate
în ecran; păstrează artefactele local și inspectează-le înainte de partajare.

## Comenzi manuale în VM

```powershell
$env:ADB_SERVER_SOCKET='tcp:192.168.56.1:5038'
X:\scrcpy-win64-v5.0.1\adb.exe -s emulator-5554 shell pm path com.qsoft.android.whist
X:\scrcpy-win64-v5.0.1\adb.exe -s emulator-5554 shell monkey -p com.qsoft.android.whist 1
```

Launcherul din `X:\Start-AndroidEmulator.cmd` deschide pagina Play Store și
pornește scrcpy. Pentru conectare manuală:

```powershell
$env:ADB_SERVER_SOCKET='tcp:192.168.56.1:5038'
X:\scrcpy-win64-v5.0.1\scrcpy.exe --serial=emulator-5554 --tunnel-host=192.168.56.1
```

## Ce urmează pentru testare profundă

1. Diagnostichează randarea înainte de extinderea testelor vizuale: compară
   rezoluția/densitatea, rendererul grafic, accelerarea hardware și compatibilitatea
   API 28 cu versiunea 4.5. Păstrează capturile curente ca reper.
2. Adaugă verificări de reguli cu oracole independente: restricția de culoare,
   ordinea atuului, licitația interzisă ultimului jucător și punctajul exact.
3. Acoperă mai multe mâini, variantele de joc, nivelul AI, jocuri complete și
   compară exact starea salvată înainte/după reluare.
4. Testează Multiplayer separat, cu strategie pentru două instanțe și conturi;
   rețeaua/sincronizarea nu este acoperită acum.
5. Păstrează codul generic de raportare și testele sale în J0; J4 rămâne
   consumator și extinde doar integrarea și scenariile Contract Whist.

## Proveniență și limite

Core `1.0.0-c2` și Reporting `1.0.0-j10` sunt importate ca perechi JAR/POM în
`lib/`, cu SHA-256 consemnate în `library-provenance.json`. Codul sursă comun
pentru Kit/Mock/Validation/Console a fost luat din J3, după comparație cu J1/J2
și baseline-ul J0; manifestul notează și această proveniență.

- [Contract Whist în Google Play](https://play.google.com/store/apps/details?id=com.qsoft.android.whist)
- [Google Android Emulator Container Scripts](https://github.com/google/android-emulator-container-scripts/blob/master/README.md#requirements)
- [scrcpy pentru Windows](https://github.com/Genymobile/scrcpy/blob/master/doc/windows.md)
- [Conectarea scrcpy prin TCP/IP](https://github.com/Genymobile/scrcpy/blob/master/doc/connection.md)


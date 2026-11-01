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
| Runda locală | 4 jucători, Descending, AI Normal; Bid 0, faza de joc și o carte jucată din mână | test UI ADB, 1 noiembrie 2026 |
| High Scores | deschiderea cere confirmarea/editarea numelui `Player`; testul a folosit Cancel, fără schimbarea profilului | captură UI, 1 noiembrie 2026 |
| Resume Game | după force-stop/start a redeschis aceeași partidă; testul separat a găsit același număr de pixeli ai cărților imediat și după 6 secunde (485301) | test UI și capturi, 1 noiembrie 2026 |
| Multiplayer | butonul deschide promptul de editare a numelui; Cancel revine la meniu. Nu s-a creat sau accesat o sesiune online. | explorare UI, 1 noiembrie 2026 |
| Randare | defect sever: fundal verde neon, text foarte mare și suprapus, cărți distorsionate, rândurile mâinii tăiate/deplasate spre bara de navigare | capturile testului din 1 noiembrie 2026 |
| Podman healthcheck | raportat `unhealthy`, deși ambele transporturi observate au pornit Android; check-ul intern folosește ADB fără selector și vede transporturi multiple | diagnostic raportat anterior de pe gazdă, 1 noiembrie 2026 |
| Rețea | host-only; accesul ADB raportat limitat la VM `192.168.56.101`; Kafka/ruta lui nemodificate | configurare raportată de operator |

Autentificarea Google a fost făcută de utilizator, nu de agent. ADB direct la
`192.168.56.1:5555` a dat anterior `unauthorized` pentru cheia VM. Calea folosită
acum este serverul ADB al gazdei pe portul 5038 și serialul explicit.

## Test UI în J4

`atf-android` oferă un client ADB cu timeout și selectare explicită de serial.
`atf-test-android` conține `ContractWhistSmokeTest`, activat doar cu
`-Datf.android.enabled=true`. Deși clasa păstrează numele inițial, scenariul
acoperă acum mai mult decât pornirea aplicației:

1. verifică instalarea și deschide meniul;
2. citește instrucțiunile pentru licitare, joc și punctaj;
3. deschide High Scores și închide promptul de nume cu Cancel;
4. enumeră opțiunile jucători, variante de joc și AI, apoi restabilește
   valorile implicite (4 / Descending / Normal);
5. pornește jocul, licitează Bid 0 și așteaptă intrarea în joc;
6. probează cărțile din mână până când o carte este jucată, iar mâna/masa se
   schimbă persistent în două capturi consecutive;
7. separat, redeschide jocul salvat din Resume Game și verifică păstrarea
   vizibilității cărților după șase secunde.

Rularea opt-in integrată a trecut: **133 teste Test Kit și 2 teste UI Android**
(fluxul local și stabilitatea Resume Game), fără eșecuri. Capturile XML/PNG, rapoartele
Reporting J9 și rapoartele Surefire sunt păstrate în `artifacts/deep-app-test/`.
Indexul artefactelor și comenzile de repetare sunt în
[`artifacts/deep-app-test/README.md`](../artifacts/deep-app-test/README.md).
La finalul verificărilor automate aplicația a rămas în meniul principal; după
explorarea manuală finală, Resume Game este deschis pentru testare.

Tabla și cărțile sunt desenate într-un singur `android.view.View`, fără text sau
controale accesibile prin UIAutomator. Testul de mutare este astfel calibrat la
emulatorul 1080×1920 și la mâna inițială de 13 cărți (două rânduri de 6 și 7); el
încearcă centrele cărților și compară regiunea mâinii, inclusiv o verificare
stabilă după animație. Nu este un page object generic pentru alte rezoluții sau
dimensiuni de mână.

Interacțiunea de bază funcționează: o carte a fost scoasă din mână și masa s-a
actualizat. Testul nu validează independent oracolul regulilor sau punctajul.
Capturile nu trec însă acceptanța vizuală: masa are
fundal verde neon, panourile și literele sunt supradimensionate, imaginile
cărților apar distorsionate, iar mâna este tăiată la marginea de jos. Testul nu
verifică încă integral corectitudinea punctajului, toate turele, finalizarea
rundelor sau multiplayer.

Setările acceptă proprietăți Java sau variabile de mediu:

| Proprietate Java | Variabilă de mediu | Implicit |
|---|---|---|
| `atf.android.adb` | `ATF_ANDROID_ADB` | `adb` din PATH |
| `atf.android.serial` | `ATF_ANDROID_SERIAL` | `emulator-5554` |
| `atf.android.adbServerSocket` | `ADB_SERVER_SOCKET` | valoare moștenită din environment sau implicit local ADB |
| `atf.android.commandTimeoutSeconds` | `ATF_ANDROID_TIMEOUT_SECONDS` | `30` |

Comanda pentru testul opt-in, din rădăcina `C:\work\1\atf_j4`:

```powershell
$env:ADB_SERVER_SOCKET='tcp:192.168.56.1:5038'
$env:ATF_ANDROID_ADB='X:\scrcpy-win64-v5.0.1\adb.exe'
$env:ATF_ANDROID_SERIAL='emulator-5554'
mvn -pl atf-test-android -am test -Datf.android.enabled=true
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

Core `1.0.0-c2` și Reporting `1.0.0-j9` sunt importate ca perechi JAR/POM în
`lib/`, cu SHA-256 consemnate în `library-provenance.json`. Codul sursă comun
pentru Kit/Mock/Validation/Console a fost luat din J3, după comparație cu J1/J2
și baseline-ul J0; manifestul notează și această proveniență.

- [Contract Whist în Google Play](https://play.google.com/store/apps/details?id=com.qsoft.android.whist)
- [Google Android Emulator Container Scripts](https://github.com/google/android-emulator-container-scripts/blob/master/README.md#requirements)
- [scrcpy pentru Windows](https://github.com/Genymobile/scrcpy/blob/master/doc/windows.md)
- [Conectarea scrcpy prin TCP/IP](https://github.com/Genymobile/scrcpy/blob/master/doc/connection.md)


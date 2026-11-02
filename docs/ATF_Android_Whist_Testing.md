# J4 — Contract Whist în emulator Android local

## Scop

Păstrăm o cale repetabilă pentru a porni și testa local aplicația gratuită
Contract Whist (`com.qsoft.android.whist`) pe emulatorul Android rulat prin
Podman pe gazda fizică și controlat din Windows VM.

## Acceptanța curentă — 2 noiembrie 2026

[Acceptanța completă](COMPLETE_GAME_ACCEPTANCE.md) înlocuiește limitele
explorării istorice de mai jos: originalul și Whist Lab au terminat fiecare
19 mâini, 109 levate și 436 de cărți, cu aceleași totaluri `[89,126,145,119]`.
Auditul independent verifică fiecare carte, actor, câștigător și scor.
Cele 219 cazuri acceptate au zero eșecuri/erori/omiteri; cele 187 de componente
reguli/AI au fost încheiate înaintea acceptanței Android.

Raportul Extent J12 din `20261101-231857-full-match` are 6.655 aserțiuni și
325 checkpoint-uri cu PNG inline Base64. PASS-ul funcțional păstrează separat
WARNING-ul randării originale. Prima integrare normală Romanian Expert a trecut
o mână și tranziția. [Acceptanța ulterioară](ROMANIAN_EXPERT_ACCEPTANCE.md) a
încheiat toate cele 24 de mâini, 94 de levate și 376 de cărți pe Android,
cu scorurile `[12,87,91,61]`; aplicația proprie rămâne la scorul final.
Core C3 livrat ulterior are o probă separată de consum; nu relabelăm jocurile
măsurate pe C2/J12.

Configurația ADB actuală este `tcp:192.168.56.1:5038`, serial
`127.0.0.1:5555`, client `C:\tools\whist-android-sdk\platform-tools\adb.exe`.
Scrcpy poate rămâne închis. Pentru o partidă nouă verificată se folosește
`scripts/run-complete-match.ps1 -Serial '127.0.0.1:5555'`.

## Configurația observată la explorarea din 1 noiembrie — istoric

| Componentă | Valoare / stare | Proveniență |
|---|---|---|
| Aplicație | Contract Whist (Oh Hell), pachet `com.qsoft.android.whist`, versiunea 4.5 (145) | verificare ADB |
| Compatibilitate instalată | minSdk 24, emulator API 28, targetSdk 36 | metadate pachet ADB |
| Instalare | Instalată; Play Store a afișat `Uninstall` și `Play`, cu verificarea Play Protect | observat vizual în emulator la 1 noiembrie 2026 |
| Lansare | `pm path` a găsit `base.apk`; `monkey` a pornit pachetul | ADB prin gazdă, 1 noiembrie 2026 |
| Emulator | imagine Google Play, API 28; `emulator-5554` apare `device` | ADB prin gazdă, 1 noiembrie 2026 |
| ADB în VM | client `X:\scrcpy-win64-v5.0.1\adb.exe`, server `tcp:192.168.56.1:5038` | verificat în VM, 1 noiembrie 2026 |
| Afișare | scrcpy cu serialul `emulator-5554` | verificat în VM, 1 noiembrie 2026 |
| Runda locală | 4 jucători, Oh Hell, AI Normal; testul guarded ajunge la scor și verifică mâna următoare cu 9 cărți și `Bid 0`–`Bid 9`. Ciclurile raster nu sunt număr de cărți acceptate. | E2E UI ADB headless, 1 noiembrie 2026 |
| Deal Oh Hell pe locuri | Primele capturi: 3/4/5 jucători = 10 cărți; 6 = 8. Grid-urile arată Bid 0–10 pentru 3/4/5 și Bid 0–8 pentru 6. În captura 4p, suma AI 7 dezactivează Bid 3. | discovery E2E live și citire vizuală, 1 noiembrie 2026 |
| Atu inițial | Capturile primei mâini: 3 locuri=caro, 4=inimă, 5=treflă, 6=pică. La 4p, mâna următoare a trecut inimă→caro; originea și tranziția sunt neconfirmate. | PNG-uri citite vizual; instrucțiunile enumeră pică, inimă, treflă, caro, fără atu |
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
acoperit de 6 teste. `atf-test-android` conține
`ContractWhistGameplayE2eTest`, activat prin `ContractWhistE2eSuite`; suita
comună serializată `AndroidGameplayE2eSuite` rulează originalul și Whist Lab:

1. verifică instalarea și deschide meniul;
2. verifică în instrucțiunile originale regulile de licitare, ofertă exactă,
   restricția ultimei oferte, culoare, atu, conducerea levatei, scor și tabel;
3. verifică promptul inițial High Scores sau revenirea la meniu dacă numele a
   fost deja salvat de o rulare anterioară;
4. citește selecțiile curente, deschide și enumeră opțiunile pentru jucători,
   variante de joc și AI, apoi închide selectorii fără să le schimbe și verifică
   faptul că toate selecțiile inițiale au rămas intacte;
5. pornește jocuri Oh Hell pentru 3–6 locuri, fotografiază primul grid și
   verifică intervalul Bid N; capturile arată primele deal-uri 10/10/10/8;
6. pornește mâna originală Oh Hell cu 4 locuri și 10 cărți; probează sloturile
   prin diferențe raster stabile, se oprește când UI-ul expune `Next Hand`, apoi
   verifică foaia de scor și domeniul următoarei licitații `Bid 0`–`Bid 9`;
   numărul și identitatea cărților acceptate nu sunt decodate semantic;
7. separat, redeschide jocul salvat din Resume Game și verifică păstrarea
   vizibilității cărților după șase secunde.

Rularea istorică de explorare este `20261101-222629`. Reactorul J4 a produs 498 de
cazuri Surefire: 497 trecute, 0 eșecuri, 0 erori și un test publisher omis
intenționat în rularea obișnuită; publisher-ul a rulat separat și a trecut.
Android component suite a trecut 6/6. Android E2E a trecut 6/6: originalul 4/4,
Whist Lab 2/2. Motorul `game-core` a trecut 29/29 teste Gradle, iar APK-ul debug
s-a construit și s-a instalat pe API 28.

Rularea originalului a acoperit meniuri și instrucțiunile complete, toate
opțiunile de setup, bid-grid-urile pentru 3–6 locuri, Resume Game, o mână
Oh Hell cu 4 jucători până la foaia de scor și tranziția la licitarea cu 9 cărți.
Captura scorului se citește manual: Player 5, Hilary 0, Hubert 1, Phyllis 4.
Valorile însumează 10; sub formula afișată, sunt compatibile condițional cu zece
puncte de levată și niciun bonus exact în acest rând. Nu demonstrează câte levate
a câștigat fiecare loc.

În următoarea licitație, UIAutomator a găsit `Bid 0`–`Bid 9` și butonul `Bid 1`
dezactivat. PNG-ul arată 9 cărți, total anterior de bid-uri 8, ofertele AI 4, 4,
0 și marcajul de dealer la Player; `8 + 1 = 9` explică dezactivarea conform
regulii ultimei oferte. Sumele și ofertele sunt citire vizuală manuală; starea
butonului și domeniul sunt capturate de test.

Raportul Extent final este
`C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html`.
Are 16.804.407 bytes și 42 imagini PNG inline `data:image/png;base64,...`;
folderul de dovezi păstrează 56 PNG-uri, 50 XML-uri UI, rezultatele Surefire și
logurile. Publisher-ul J10 a verificat payload-urile Base64 exacte. Raportul
rămâne WARNING pentru defectele vizuale. Ciclurile raster nu sunt raportate ca
număr semantic de cărți jucate.
Tabla și cărțile sunt desenate într-un singur `android.view.View`, fără text sau
controale de carte accesibile prin UIAutomator. Captura observată pentru
configurația cu patru jucători arată zece cărți, în două rânduri de câte cinci;
vechea notă despre treisprezece cărți și împărțirea 6+7 era incorectă și a fost
eliminată. Adapterul folosește centrele a cinci sloturi per rând și cere atât o
schimbare stabilă a mâinii, cât și una în zona de joc pentru a confirma un eveniment raster; acest prag nu identifică rangul, culoarea sau legalitatea cărții.
Geometria rămâne calibrată la emulatorul 1080×1920; distribuția din mâini mai
mici și alte densități trebuie confirmată separat.

Flow-ul E2E guarded a oprit probele când UIAutomator a expus `Next Hand`, apoi a verificat următoarea licitație `Bid 0`–`Bid 9`; nu susține că a decodat zece cărți umane. Oracolul legalității fiecărei cărți, câștigătorul fiecărei levate și scorurile exacte ale originalului nu sunt decodate independent din suprafața desenată. Capturile nu trec acceptanța vizuală: masa are fundal verde neon, panourile și literele sunt supradimensionate, imaginile cărților apar distorsionate, iar mâna este tăiată la marginea de jos. Testul nu verifică încă toate turele, finalizarea partidei sau multiplayer.

## Aplicația locală în paralel — Whist Lab

Codul aplicației este izolat în `C:\work\1\atf_j4\whist-lab-app`: `game-core` conține
regulile, sesiunea, AI-ul și testele JVM, iar `app` conține UI-ul Android.
Pachetul `ro.whistlab.app` este separat de original. Regulile confirmate în
instrucțiunile originale (licitație, culoare, atu și scor) sunt implementate
local; mărimile primei mâini observate 10/10/10/8 sunt codificate, iar
progresia cap-coadă 10→1 este verificată local pentru toate cele patru numere
de locuri. Originea ciclului de atu este deschisă și raportată ca WARNING;
checkpoint-ul partidei se salvează ca JSON versionat în SharedPreferences
după fiecare mutare/licitație (inclusiv AI) și după trecerea la următoarea mână.
Reluarea după oprirea procesului este verificată live pe API 28. Variantele
neobservate și multiplayer nu sunt încă acoperite.

`WhistLabGameplayE2eTest` are două flow-uri live pe API 28. Primul exercită fixture-ul deterministic: omul licitează 1, procesul este oprit forțat, `Resume game` restaurează exact tura, omul joacă A♥, AI-ul termină levata, iar tabelul de scor arată `+11`, total `11`. Al doilea verifică setup-ul default Oh Hell cu patru jucători și înregistrează WARNING-ul pentru originea atuului, deoarece originalul arată pică/inimă/treflă/caro la 3/4/5/6 locuri în capturile inițiale. Ambele flow-uri au trecut în rularea `20261101-222629`; J4 folosește același `AdbClient`, adapterul UI Automator și J10 Extent Reporting pentru ambele pachete.

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

## Restanțe după acceptanța completă

1. Diagnostichează randarea înainte de extinderea testelor vizuale: compară
   rezoluția/densitatea, rendererul grafic, accelerarea hardware și compatibilitatea
   API 28 cu versiunea 4.5. Păstrează capturile curente ca reper.
2. Extinde acceptanța originalului dincolo de partida Oh Hell cu patru locuri,
   AI Normal. Atuul Oh Hell provine din cartea neîmpărțită, nu dintr-un ciclu fix.
3. Păstrează oracolul independent și cazurile de restart la extinderea spre
   alte variante. Partida curentă are toate cele 436 de cărți și scorurile auditate.
4. Extinde integrarea mobilă Romanian Expert spre alte dispozitive și mese
   de 3/5/6 locuri. Partida normală Android cu patru locuri și toate cele
   24 de mâini este încheiată; componentele acoperă deja 3–6 locuri.
5. Testează Multiplayer separat, cu strategie pentru două instanțe și conturi;
   rețeaua/sincronizarea nu este acoperită acum.
6. Păstrează codul generic de raportare și testele sale în J0; J4 rămâne
   consumator și extinde doar integrarea și scenariile Contract Whist.

## Proveniență și limite

Core `1.0.0-c3` și Reporting `1.0.0-j12` sunt acum perechile JAR/POM din
`lib/`, cu SHA-256 consemnate în `library-provenance.json`. Explorarea de mai
sus folosea C2/J10; partida completă și publisherul ei au folosit C2/J12.
Codul sursă comun
pentru Kit/Mock/Validation/Console a fost luat din J3, după comparație cu J1/J2
și baseline-ul J0; manifestul notează și această proveniență.

- [Contract Whist în Google Play](https://play.google.com/store/apps/details?id=com.qsoft.android.whist)
- [Google Android Emulator Container Scripts](https://github.com/google/android-emulator-container-scripts/blob/master/README.md#requirements)
- [scrcpy pentru Windows](https://github.com/Genymobile/scrcpy/blob/master/doc/windows.md)
- [Conectarea scrcpy prin TCP/IP](https://github.com/Genymobile/scrcpy/blob/master/doc/connection.md)


## Reluare după restartul gazdei — 2 noiembrie 2026

Containerul existent `atf-android-emulator-api28` a fost pornit, fără recreare
sau wipe. Serverul ADB remote a avut inițial lista goală. Cu
`ADB_SERVER_SOCKET=tcp:192.168.56.1:5038`, comanda
`adb connect 127.0.0.1:5555` a reconectat emulatorul; ținta loopback este
interpretată de serverul ADB al gazdei. Serialul observat acum este
`127.0.0.1:5555`, nu `emulator-5554`. Folosiți serialul efectiv în variabila
`ATF_ANDROID_SERIAL` și parametrul `-Serial` al scripturilor.

Boot-ul este complet. APK-ul original v4.5/145 păstrează SHA-256
`884c492e4410d197199d23051d5405dd478620122ee70124426abd4a2e3d8d05`.
Nu s-au modificat contul Google, regulile de firewall sau containerele J2.

La SCORE, salvarea originalului poate avea indexul/mărimea mâinii următoare,
dar cărțile jucate și scorul mâinii precedente. Cititorul parsează layout-ul
cu mărimea serializată și deduce separat mărimea jucată din istoricul complet,
levate și mâini goale. Fixture-ul real `v4.5-resumed-score-next-size.save2`
acoperă cazul 8 cărți jucate / 9 serializate. Continuarea nu avansează acest
scor înainte de a recupera ultima carte acceptată înaintea pauzei.

### Continuare Whist Lab dintr-o limită verificată

După întreruperea harness-ului la ecranul de licitare al următoarei mâini,
`-Datf.match.continueLab=true` permite reluarea testului
`CompleteWhistLabMatchE2eTest` fără reinstalare și fără restartul aplicației.
Testul cere ca portul de observare real să confirme BIDDING, indexul următoarei
mâini și istoric de mutări gol. Revalidează din fișiere toate mâinile încheiate,
cu cărți, oferte, câștigători, totaluri și rândurile de scor UI. Pașii reținuți
sunt etichetați explicit; nu trimite tap-uri noi pentru ei. Se verifică toate
condițiile înainte de a interacționa cu prima mână rămasă.

Această cale nu reface o partidă din rezultate așteptate: instanța Android
curentă și totalurile calculate de motor rămân în uz. Dacă aplicația a fost
închisă/reinstalată sau starea vie nu coincide, continuarea refuză execuția.
După o partidă deja încheiată se publică din dovezi, nu se cere Resume.

# Whist Lab

Whist Lab este versiunea Android locală pentru învățarea și verificarea fluxului
Contract Whist. Include un modul de reguli Java pur și o interfață Android
compactă. Pachetul aplicației originale, `com.qsoft.android.whist`, rămâne
separat și nemodificat.

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
- Oh Hell, Descending, Practice Hand, Whist românesc, Down & Up, Maximum, Long
  Romanian și Reverse Oh Hell sunt variante distincte. Descending poate ajunge
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
- AI Normal folosește o estimare simplă a forței cărților și joacă cea mai mică
  dintre cărțile legale. Dificil estimează licitația ținând cont de atu și încearcă
  să-și gestioneze levatele rămase. Expert eșantionează distribuții ascunse numai
  din cărțile proprii și informația publică, apoi compară punctajele estimate.
  Turneul de componentă îl compară cu Dificil pe distribuții pereche și locuri
  rotite.
- Mâna de antrenament cu o singură carte permite verificări Android deterministe.
- Ecranul partidei identifică varianta prin `Whist · [variantă]`. Atuul are o
  singură zonă persistentă, mărită în antetul deasupra mesei; licitația nu îl
  repetă. Locurile arată numele, licitația și levatele câștigate pe două rânduri
  aliniate la stânga (`Bid` / `Won`, respectiv `Licitat` / `Levate`). Scorurile sunt
  consultate în foaia de scor. Rezumatul separă licitațiile depuse de **totalul
  licitat**, raportat la mărimea mâinii. Locul activ primește un badge discret
  `BID NOW` sau `PLAY NOW` (în română, `LICITEAZĂ` sau `JOACĂ ACUM`); locul
  rezervă aceeași înălțime pentru badge și când nu este activ, astfel încât
  schimbarea turei nu mută jucătorii. Cărțile
  adversarilor rămân ascunse. Locurile folosesc ancore stabile în jurul zonei
  centrale pentru 3–6 jucători, iar omul rămâne în sud, în centru. Pâsla este
  continuă și nu desenează o ramă ovală. La șase jucători, conturul
  urmează pozițiile `N; NW/NE; SW/SE; S`; pentru configurațiile mai mici sunt
  eliminate perechile simetrice care nu mai sunt necesare. Ancorele rămân aceleași
  între licitare și joc; flancurile au inset de 16% în licitarea compactă, 18% în
  licitarea normală și 23% în joc. Reducerea insetului lasă loc grilei înguste fără
  să mute omul din sud.
- Licitația păstrează forma 3 × 3 din mock când lățimea tablei este de cel puțin
  400 dp. Între 260 și 399 dp, 4–9 valori folosesc cinci coloane și două rânduri;
  sub 260 dp revin la trei coloane. Seturile cu mai mult de nouă valori folosesc
  șase coloane de la 312 dp, altfel trei. Fiecare valoare are țintă tactilă de cel
  puțin 48 × 48 dp și cip vizual compact. Confirmarea conturată folosește
  `Place bid` în engleză sau `Licitează` în română și stă în același grup,
  imediat sub valori. În timpul jocului, levata
  curentă și numele celor care au jucat stau în centrul mesei; prima carte este
  centrată singură, iar mutările următoare completează grupul în două coloane. Meniul
  hamburger din stânga sus oferă `Score sheet` și `Main menu`. Foaia de scor nu
  schimbă licitația sau mâna; revenirea la home păstrează partida pentru reluare.
  După completarea unei levate, toate cele patru cărți rămân vizibile împreună
  timp de exact 1.500 ms, fără input pentru următoarea carte. Apoi masa se golește,
  iar câștigătorul levatei primește tura următoare.
- Mâna rămâne pe un singur rând: cărțile se suprapun controlat, iar eticheta
  fiecăreia rămâne expusă. Pe lățimea de referință de 353 dp, 13 cărți încap
  fără derulare. Mâinile de 14–17 cărți rămân tot pe un rând și permit derulare
  orizontală când avansul minim vizibil nu mai încape; nu se rearanjează în
  grilă. Fiecare carte are 98 dp înălțime și 28 dp rezervați deasupra pentru
  animația de ridicare. Cartea atinsă se ridică 20 dp, se scalează la 1,12 și se
  desenează sub vecine; dimensiunea mâinii și locul omului nu se schimbă. Masa
  folosește pâsla continuă a ecranului, fără oval sau ramă desenată. Levata rămâne
  într-un grup compact de două coloane în centrul suprafeței, iar atuul apare
  numai în antet. Pe ecrane înalte, masa și mâna
  se centrează împreună în zona de joc; slotul instrucțiunii rămâne rezervat și când
  mută AI-ul, astfel încât locurile să nu sară între ture.
- La configurare, programul mâinilor este calculat din regulile efective pentru
  varianta și numărul de jucători selectate. Se actualizează când se schimbă una
  dintre aceste alegeri; notația explică dimensiunea mâinii și repetările.
- După licitații și cărți jucate se salvează un checkpoint local. `Reia partida`
  îl restaurează după închiderea sau oprirea procesului Android.
- `Joc nou` și `Joacă din nou` șterg reluarea de depanare. Verificarea mobilă
  Expert în română a jucat o mână completă, a verificat câștigătorul și scorul
  independent, apoi a verificat rotația împărțitorului și totalurile cumulative.
- La configurare sunt explicate regulile variantei selectate. În timpul jocului
  sunt explicate urmarea culorii, atuul obligatoriu și supratatuarea opțională;
  progresul și câștigătorii finali, inclusiv egalitățile, sunt afișați. Checkpoint-
  urile v2/v3 verifică tot istoricul public; salvările parțiale v1 își păstrează
  marcajul de versiune.

## Principii de layout pentru jucător

- Pâsla continuă este ancora vizuală dintre antet și mâna omului; locurile
  jucătorilor formează conturul fără să fie desenată o masă ovală.
  Locul omului rămâne jos, la centru, iar adversarii stau pe contur. Geometria
  mesei depinde de numărul de locuri și de conținutul selectorului de licitare,
  nu de ridicarea unei cărți. Antetul ține numele variantei, progresul și un
  indicator de atu mai mare decât vechiul marcaj central.

```text
6 jucători                    4 jucători
       N                            N
  NW        NE                 W        E
  SW        SE                      S · Tu
       S · Tu
```

Pozițiile sunt ancore de perimetru, nu celule care se rearanjează după fază.
Insetul lateral se schimbă între faze și între grila normală și cea compactă;
locul omului rămâne fix în sud. În licitarea compactă, flancurile folosesc inset
de 16% pentru a lăsa spațiu suplimentar sub și deasupra grilei.

- Starea publică este grupată după rost: nume, bid și levate la fiecare loc;
  progresul și totalul licitației în antet; atuul în antet; cărțile deja jucate în centrul
  pâslei; scorul în foaia consultativă. Nu repetăm atuul în selecția de bid.
  Înainte de joc rămâne vizibilă numai mâna omului.
- Alegerea activă este lângă acțiunea ei: 4–9 biduri folosesc cinci coloane pe
  mesele de 260–399 dp și forma 3 × 3 de la 400 dp; seturile mai mari folosesc
  șase coloane de la 312 dp. Pragul de cinci coloane evită intersectarea cu
  locurile laterale observată la 353 dp; țintele tactile rămân 48 dp. Confirmarea
  este imediat sub ele. Cipul folosește auriu mat
  când e selectat și verde salvie când e disponibil; cadrul tactil rămâne 48 dp.
  Cărțile legale se disting prin contur, iar atingerea ridică numai cartea aleasă.
- Mâna folosește aceeași singură linie pentru 1–17 cărți. Avansul dintre cărți
  se calculează din lățimea disponibilă, numărul de cărți și scala textului;
  etichetele rămân vizibile, iar derularea este orizontală numai când e necesară.
  Containerul rezervă permanent spațiul pentru ridicarea cărții, astfel încât
  mâna, levata centrală și locul de jos nu sar la selectare.
- Scorul și navigarea rămân accesibile, dar secundare. Un buton hamburger cu trei
  linii, în colțul stânga sus al jocului, deschide foaia de scor sau meniul
  principal. Foaia este doar consultativă: nu confirmă licitația și nu schimbă
  levata; închiderea ei păstrează selecția, iar întoarcerea la home păstrează
  checkpoint-ul.
- Paleta leagă fundalul cald de fildeș de pâsla verde-pădure, contururile salvie,
  textul verde închis și accentul auriu mat. Simbolurile roșii ale cărților rămân
  roșii pentru identificarea rapidă a culorii; și dialogurile Android folosesc
  accentul verde al aplicației.
- În timpul partidei, pâsla verde continuă pe toată fereastra, inclusiv în zona
  sistemului și în jurul zonei centrale. Mâna și masa folosesc aceeași suprafață;
  `Place bid` / `Licitează` rămâne conturat în centrul ei. Foaia de
  scor este un tabel compact cu jucător, licitație, levate, rezultat pe mâna
  curentă și total cumulativ. Nu pretinde să fie istoricul complet al partidei:
  starea salvată nu păstrează încă rezultatele fiecărei mâini.
- Eliminăm titlurile și instrucțiunile redundante. Păstrăm denumiri contextuale,
  contrast suficient și descrieri accesibile pentru locuri, cărți și controale.
- Validarea responsive din 3 noiembrie 2026 a rulat aceleași două flow-uri E2E
  pentru șase locuri/opt cărți și patru locuri/13 cărți la 320, 393 și 600 dp.
  Fiecare flow verifică limitele reale din Android, meniul și scorul, un singur
  rând de cărți, atuul, centrul levatei, opacitatea mâinii și stabilitatea locului
  omului. La 353 dp, măsurătorile au găsit o intersectare reală între grila 3 × 3
  și locurile laterale; grila compactă și insetul de 16% o elimină. La 600 dp,
  masa și mâna se centrează în înălțimea disponibilă. Textul mărit și TalkBack
  rămân în matricea de validare.

Reperele de interfață și scor au fost evaluate comparativ cu cinci capturi reale:
[Romanian Whist Gold](https://mwm.ai/apps/romanian-whist-gold/733772257),
[Classic Bid Whist](https://mwm.ai/de/apps/classic-bid-whist/1546101065),
[Whist pentru iPhone](https://whist-card-game.en.softonic.com/iphone),
[World of Card Games — Whist](https://worldofcardgames.com/whist) și
[Minnesota Whist de la Coppercod](https://coppercod.games/game/minnesota-whist/).
Scorurile, raționamentul și capturile locale sunt păstrate în pachetul temporar
`WHIST_J4_UI_REFERENCES_20261103`, cu sursa fiecărei imagini. Capturile nu sunt
copiate în repository. Acestea sunt repere de ierarhie și utilizare, nu modele
de copiat ca stil sau reguli.

Build-ul TMP din 3 noiembrie a trecut 19 teste de componentă Android. Testul E2E
`completedTrickRemainsRenderedBeforeTheNextTrickBecomesInteractive` a trecut pe
emulator API 28: verifică cele patru fețe în capturi de pe dispozitiv, stabilitatea
lor între 900 și 1.400 ms, golirea mesei și tura câștigătorului. Raportul Extent
include acțiunile, aserțiunile și capturile PNG inline. Ambele
flow-uri UI au trecut la fiecare lățime, iar cele șase rapoarte Extent păstrează
capturile inline, manifestele `PASS` și aserțiunile expandabile. APK-ul instalat
pe emulatorul API 28 a fost testat cu aceste viewport-uri logice la 440 dpi:
`880×1920`
(320 dp), `1080×1920` (393 dp) și `1650×2560` (600 dp). Pentru override-ul de
320/600 dp, emulatorul capturează fizic `1080×1920` și letterboxează imaginea;
testele mapă coordonatele ierarhiei UI în PNG după dimensiunea raportată de
`wm size`. APK-ul debug instalabil și hash-ul verificabil sunt în
`../dist/WhistLab-0.1.2-debug.apk` și `../dist/WhistLab-0.1.2-debug.apk.sha256`
(minSdk 24, fără biblioteci native dependente de ABI).
Rapoartele curente sunt în `%TEMP%`:

- `whist-j4-display-final-v13-320dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v13-320dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`
- `whist-j4-display-final-v12-393dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v12-393dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`
- `whist-j4-display-final-v11-600dp-20261103\table-presentation\whist-lab-table-presentation.html`
- `whist-j4-display-final-v11-600dp-20261103\thirteen-card-fan\whist-lab-thirteen-card-fan.html`
- `ATF_J4_TRICK_PAUSE_DEVICE_SAMPLER_R9_20261103\completed-trick-display-pause\whist-lab-trick-display-pause.html`

Testele de componentă verifică 13 cărți fără derulare pe 353 dp și 17 cărți
pe un singur rând cu derulare orizontală.
Vezi
[feedback-ul de utilizare](../docs/feedback/WHIST_LAB_User_Experience_Feedback.md).

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
validate la restaurare. Versiunea 3 adaugă configurarea regulilor, contoarele
seriei și punctele acordate; versiunea 2, cu istoricul public complet al levatelor
mâinii active, rămâne lizibilă. Acest istoric nu este registru de scor pe mâini:
checkpoint-ul păstrează totalurile cumulative, dar nu rezultatele mâinilor deja
încheiate.
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
Gherkin are propriul pas expandabil, tabelul colapsabil ACTION/TARGET/EXPECTED/
OBSERVED/TIME/RESULT, captura PNG full-display și tabelul ASSERTION/EXPECTED/
ACTUAL/RESULT. Rezultatul tap-ului rămâne observație/assert, nu se deduce din
acceptarea comenzii ADB; timpul măsurat exclude pauza fixă de stabilizare UI.
Receipt-ul păstrează identitatea rulării și numărătorile, iar XML/properties sunt
fingerprint-uri SHA-256 colapsate, fără dump-uri sau linkuri către fișiere locale.
Testele componente acoperă tabelul comun, asocierea capturii cu pasul și
diagnosticul FAIL fără checkpoint. E2E-ul verifică rapoartele live pentru masa
cu 8 cărți și mâna cu 13 cărți.

Începe cu acceptanța de componentă și raportul Extent nativ, fără Android:

```powershell
C:\work\1\atf_j4\scripts\run-rule-components.ps1
```

PowerShell, cu SDK-ul Android în `C:\tools\whist-android-sdk`:

```powershell
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat --no-daemon --console=plain :game-core:test :app:assembleDebug
```

Configurația curentă a APK-ului este `minSdk 24`, `compileSdk 36`,
`targetSdk 36`, `versionCode 3` / `versionName 0.1.2`. `targetSdk 36`
înlocuiește API 28: țintele cu peste două niveluri API sub versiunea Android a
telefonului pot declanșa avertizarea Play Protect „Unsafe app blocked / built
for an older version of Android”. Ridică `targetSdk` la nivelul cerut la acea
dată și verifică separat comportamentele Android noi; emulatorul API 28 confirmă
compatibilitatea minimă, nu acceptanța pe un telefon actual. Google Play cere
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
  C:\work\1\atf_j4\dist\WhistLab-0.1.2-debug.apk
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

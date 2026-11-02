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
- Înainte de licitare și în timpul jocului, masa arată toate locurile, licitațiile,
  levatele câștigate și scorurile individuale. Linia de stare separă numărul
  licitațiilor depuse de **totalul licitat**, raportat la mărimea mâinii. Mâna
  completă a jucătorului este vizibilă; cărțile adversarilor rămân ascunse.
- Licitația se alege prin butoane numerice în centrul mesei; atuul rămâne vizibil
  în același grup. Cipul desenat este compact, dar fiecare opțiune păstrează o
  țintă tactilă de cel puțin 48 dp; o singură acțiune confirmă alegerea. Meniul
  hamburger din stânga sus oferă `Score sheet` și `Main menu`. Foaia de scor nu
  schimbă licitația sau mâna; revenirea la home păstrează partida pentru reluare.
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

- Masa este ancora vizuală a partidei. Locurile sunt distribuite în jurul
  suprafeței de joc, iar înălțimea acesteia se adaptează numărului de jucători și
  dimensiunii textului. Nu micșorăm cărțile sau informațiile jucătorilor doar ca
  să păstrăm spațiu gol.
- Starea publică rămâne grupată logic: licitația, levatele câștigate și scorul
  apar la fiecare loc; licitațiile depuse și suma lor sunt vizibile în rezumatul
  mâinii; levata curentă și atuul sunt în centrul mesei. Atuul se afișează o
  singură dată. Doar mâna jucătorului este vizibilă înainte de licitare și la joc.
- Alegerea activă trebuie să fie lângă acțiunea ei principală. Pentru licitație,
  valorile numerice apar în centrul mesei ca cipuri discrete de aproximativ
  34 dp, în interiorul unor ținte tactile de 48 dp; valoarea aleasă folosește
  auriu mat, iar opțiunile disponibile verde salvie. Pentru joc, cărțile legale
  sunt marcate prin semn și contur, nu numai prin culoare sau transparență.
- Mâinile de cinci până la opt cărți folosesc un singur rând egalizat pe lățimea
  mesei, cu etichete de 14 sp și înălțime de 58 dp. Mâinile mai scurte sau mai
  lungi păstrează grila de patru coloane; această regulă evită micșorarea
  excesivă a mâinilor lungi.
- Scorul și navigarea rămân accesibile, dar secundare. Un buton hamburger cu trei
  linii, în colțul stânga sus al jocului, deschide foaia de scor sau meniul
  principal. Foaia este doar consultativă: nu confirmă licitația și nu schimbă
  levata; închiderea ei păstrează selecția, iar întoarcerea la home păstrează
  checkpoint-ul.
- Paleta leagă fundalul cald de fildeș de pâsla verde-pădure, contururile salvie,
  textul verde închis și accentul auriu mat. Simbolurile roșii ale cărților rămân
  roșii pentru identificarea rapidă a culorii; și dialogurile Android folosesc
  accentul verde al aplicației.
- Eliminăm titlurile și instrucțiunile redundante. Păstrăm denumiri contextuale,
  contrast suficient și descrieri accesibile pentru locuri, cărți și controale.
- Proba live la `393×698 dp` a confirmat opt cărți pe un rând, nouă valori de
  licitare pentru mâna de opt cărți, șase locuri, meniul și foaia de scor fără
  derulare în ecranul principal. Mâinile lungi păstrează grila centrală adaptivă
  și pot necesita derulare verticală. Ecranele înguste, textul mărit, TalkBack și
  toate combinațiile de locuri/mâini rămân în matricea de validare.

Reperele de interfață au fost comparate cu [Whist-ul Arkadium](https://support.arkadium.com/support/solutions/articles/44002656380--whist-the-game-main-menu-how-to-play-scoring),
care separă masa de opțiunile pentru tabel și scor, cu [captura Spades+](https://mwm.ai/apps/spades/904702317),
care păstrează licitațiile și levatele lângă jucători, și cu [Whist-ul CardGames.io](https://cardgames.io/whist/).
Împrumutăm numai ierarhia vizuală utilă jocurilor de levate; identitatea cromatică
și regulile rămân proprii Whist Lab.

Verificarea manuală live a acoperit șase jucători și opt cărți, cu alegerea unei
licitații și deschiderea foii de scor, la `393×698 dp` (`1080×1920`, `440 dpi`).
La trei jucători, Descending și 17 cărți au fost verificate separat: toate cele
18 opțiuni apar în trei rânduri, fără să acopere locurile; mâna lungă cere
derulare verticală. Ecranele mai mici, textul mărit, TalkBack și toate combinațiile
de locuri și mâini rămân de validat; vezi
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
seriei și punctele acordate; versiunea 2, cu istoric complet, rămâne lizibilă.
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

Pentru verificarea strictă a inițializării și prezentării, fără să se joace o
partidă, `WhistLabGameplayE2eTest` are două probe Android opt-in: testul implicit
oprește fluxul la prima licitare în Oh Hell cu patru jucători; testul
`sixPlayerEightCardHandFitsOneRowAndMenuKeepsScoreAndHomeAccessible` verifică
mâna de opt cărți, țintele licitației, hamburgerul, foaia de scor doar pentru
citire și întoarcerea la home. Nu confirmă licitația și nu joacă o carte.
Rularea E2E folosește APK-ul construit în același staging TMP; fișierele UI și
raportul Extent sunt scrise în `%TEMP%` implicit sau în `atf.android.artifacts`.

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

Instalează APK-ul de depanare pe emulatorul API 28:

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
C:\tools\whist-android-sdk\platform-tools\adb.exe connect 127.0.0.1:5555
C:\tools\whist-android-sdk\platform-tools\adb.exe -s 127.0.0.1:5555 install -r `
  C:\work\1\atf_j4\whist-lab-app\app\build\outputs\apk\debug\app-debug.apk
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
C:\work\1\atf_j4\scripts\run-deep-tests.ps1
```

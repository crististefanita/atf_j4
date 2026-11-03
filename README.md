# ATF J4 — Android / Contract Whist

J4 folosește fundația ATF partajată pentru acceptanța locală a aplicației Android
Contract Whist (`com.qsoft.android.whist`). Aplicația rulează în emulatorul
Android găzduit de Podman pe mașina fizică și afișat în mașina virtuală Windows.

## Fundația reutilizată

- `atf-test-kit`, `atf-mock`, `atf-validation`, `atf-console`: snapshot-ul de
  surse selectat din J3 după compararea J1/J2/J3 și verificarea compatibilității
  surselor Java cu baseline-ul curent J0.
- `lib/atf-core-1.0.0-c3.{jar,pom}` și
  `lib/atf-reporting-1.0.0-j18.{jar,pom}`: dependențe binare pentru consumatori.
  Hash-urile și proveniența surselor sunt în `library-provenance.json`.
- Implementarea și testele contractuale Core și Reporting aparțin J0. J4 deține
  integrarea Android și compoziția rapoartelor specifice consumatorului.

Vezi [notele testelor Android](docs/ATF_Android_Whist_Testing.md) pentru
conexiunea VM/host, exercițiile efectuate și limitele de afișare cunoscute.

## Reguli și dezvoltare cu prioritate pe componente

- [Arena AI și acceptanța pe rol](docs/AI_STRATEGY_ARENA.md) documentează mesele
  virtuale cu strategii diferite, aceleași împărțiri și locuri rotite. Politicile
  curente acceptate sunt Expert Safety10 și independent V8; varianta independentă
  Safety10 a fost respinsă. [Catalogul de onoruri și acoperiri](docs/rules/AI_HAND_PATTERNS_RO.md)
  explică ipotezele contextuale, ponderile de cercetare și limitele lor.

- Iterația offline din 3 noiembrie are 500 de cazuri în regresia completă, trei
  reluări de binding și o ligă de 128 de partide. [Raportul Extent J17](C:/work/whist_back/ai-training/20261103-safety10/reports/final-extent-report.html)
  publică o selecție explicită de 56 de capturi întregi, cu inventarul populației,
  deciziile pe rol și 2.879 de comparații; auditul static este trecut. Această
  iterație nu revendică o execuție nouă pe Android.

[Corpusul offline](docs/AI_CORPUS.md) păstrează partidele complete ca JSON simplu,
cu fiecare licitație, levată, plată de serie și scor final. Reluarea, validarea și
exportul exemplelor de antrenare rulează fără Android sau interfață. Distribuțiile
de dezvoltare și heldout sunt separate; pierderile se păstrează.

Rapoartele offline diferențiază populațiile de partide, comparațiile politicilor
și verificările de publicare:

| Evidență | Rezultat înregistrat |
|---|---|
| [Acceptanța V4](docs/AI_CORPUS_ROUND4_ACCEPTANCE.md) | Licitații publice de o carte; Expert +190 puncte / patru ponderi de victorie, adversarul +242 / cinci, câte 128 de comparații heldout în perechi; corpusul conține 2.161 de partide |
| Limita publicării V4 | 257 de invocări / 136.202 comparații; acceptanța datelor este separată de auditul interactiv original, blocat |
| [Acceptanța V3](docs/AI_CORPUS_ROUND3_ACCEPTANCE.md) | 1.137 de partide reluate independent; 333 contracte JVM și patru Node; ponderea victoriilor Expert heldout 28→31 în 128 de perechi; experimentele de planificare respinse se păstrează |
| [Acceptanța V2](docs/AI_TRAINING_V2_ACCEPTANCE.md) | Partidă Android Expert de 24 de mâini, verificări serii +10/−5; 349 de cazuri acceptate, 30.028 comparații și 352 checkpoint-uri cu imagini |

Întregul meniu Instructions al aplicației originale este mapat în [contractul de
acoperire a instrucțiunilor](docs/ORIGINAL_INSTRUCTIONS_COVERAGE.md).

- Whist Lab adaugă variante locale și selectorul românesc de serii de cinci mâini:
  bonus pozitiv **5 sau 10**, implicit **10**, cu penalizare fixă −5 pentru seria
  ratată.
- Opțiunea din meniu și contoarele se păstrează; salvările vechi își mențin
  profilul clasic.
- Contractul exact continuă să valoreze `5 + levate`.
- Interfața Whist Lab poate fi configurată în engleză sau română; engleza este
  implicită la instalările noi. Alegerea se aplică meniurilor, jocului și
  regulilor. Masa afișează licitația, levatele câștigate și scorul fiecărui loc,
  plus numărul licitațiilor depuse și totalul acestora. Mâna omului rămâne vizibilă
  înainte de licitare și în joc, iar cărțile AI rămân ascunse. Foaia de scor este
  disponibilă fără ieșirea din mână sau avansarea jocului. Vezi
  [feedback-ul de utilizare](docs/feedback/WHIST_LAB_User_Experience_Feedback.md).
- La configurare, Whist Lab afișează programul exact al mâinilor pentru varianta
  și numărul de jucători selectate. În timpul licitării, valorile disponibile
  sunt butoane în centrul mesei, iar atuul rămâne vizibil alături de ele.

[Protocolul de antrenare AI](docs/AI_TRAINING_PROTOCOL.md) consemnează politicile
înghețate, seed-urile de dezvoltare/finale în perechi, experimentele respinse și
adversarul independent cu informație publică. Pipeline-ul copiază sursele și
build-urile într-o etapă TMP izolată și păstrează toate dovezile în afara J4:

```powershell
.\scripts\run-whist-ai-validation.ps1
.\scripts\run-whist-ai-validation.ps1 -WithAndroid -PositiveSeriesBonus 10
```

A doua comandă verifică și meniul Android real și reluarea, joacă toate cele 24
de mâini românești cu trei boți Expert, reconstruiește fiecare mutare/scor offline
și publică Extent nativ J0 cu text complet și capturi Base64 încorporate. Evaluarea
adversarială măsurată nu dovedește joc perfect. Rapoartele istorice de mai jos
păstrează profilurile de reguli și APK-urile folosite atunci.

Whist Lab are profilul explicit `ROMANIAN_WHIST` alături de `OH_HELL`.

- Whistul românesc impune atu când jucătorul nu are culoarea cerută, folosește un
  pachet redus și programul 1–8–1; contractul exact valorează `5 + levate`, altfel
  se scade eroarea de licitare.
- Aceste reguli nu înlocuiesc oracolul distinct al aplicației originale Oh Hell.
- [README-ul game-core](whist-lab-app/game-core/README.md) leagă fiecare regulă
  de surse, cazuri de componentă, domenii exhaustive, verificări independente de
  partidă completă și limita informațională a AI-ului Expert.

```powershell
.\scripts\run-rule-components.ps1
```

Acest pipeline offline rulează componentele de reguli și AI, măsoară acoperirea
JaCoCo, construiește APK-ul și publică Extent nativ cu biblioteca Reporting
curentă.

- Nu pornește și nu instalează nimic în emulator.
- Dovezile și raportul rămân în afara J4, în
  `C:\work\whist_back\rules-components\<run>\`.
- Acceptanța Android urmează după cea de componentă; capturile sale rămân PNG-uri
  Base64 încorporate exact.

Prima acceptanță de componente din 2 noiembrie, păstrată ca baseline istoric,
este `C:\work\whist_back\rules-components\20261102-card-acceptance\component-extent-report.html`:
187 de cazuri componentă/benchmark au trecut, împreună cu publisher-ul rulat
explicit.

- Modelul independent a verificat 88 de partide și 40.304 jocuri de carte.
- `GameRules` a atins 100% linii/ramuri măsurate; `game-core` complet a atins
  97,22% / 91,07%.
- Expert a obținut mai multe puncte decât Dificil la toate dimensiunile mesei,
  în 144 de comparații în perechi.
- Rezultatele exacte și pașii de reproducere sunt în README-ul game-core.

## Module

- `atf-android`: adaptor ADB cu limite explicite și serial de dispozitiv.
- `atf-test-android`: verificări UI Android Contract Whist, opt-in.
- `atf-tests`: catalog J4 de acceptanță cross-module și arhitectură.
- `whist-lab-app/`: aplicația Android paralelă locală și motorul său de reguli
  `game-core`, testat independent.
- Cele patru module copiate din fundația comună și perechile binare Core/Reporting.

## Partidă Expert normală completă în română — 2 noiembrie

[Acceptanța Expert în română](docs/ROMANIAN_EXPERT_ACCEPTANCE.md) consemnează o
partidă Android normală Whist Lab completă: 24 de mâini, 94 de levate și 376 de
cărți jucate, împotriva a trei adversari Expert reali, fără fixture de replay sau
distribuție injectată.

- Oracolul independent a confirmat fiecare mutare și scor.
- Totalurile finale: `[12,87,91,61]`; Miles a câștigat cu 91 de puncte.
- Un audit offline separat a reconstruit 260 de snapshot-uri păstrate și fiecare
  rând de scor vizibil.

Inventarul acceptat are 233 de cazuri: 210 componente joc/AI, 21 contracte
Android/raportare, partida live și auditul offline.

- Publisher-ele au rulat separat.
- `GameRules` rămâne la 100% linii/ramuri; acoperirea `game-core` este acum
  97,17% linii / 90,44% ramuri după gardurile și explicațiile adăugate.
- Benchmark-ul Expert în 144 de perechi rămâne la 14.604 față de 7.689 puncte
  pentru Dificil.

Extent nativ: 11.047 aserțiuni, zero eșecuri și 287 checkpoint-uri cu capturi,
toate Base64 încorporate și cu verificarea hash-urilor valorilor complete și a
PNG-urilor decodate.

- Raport: `C:\work\whist_back\romanian-expert\20261102-131344\romanian-expert-extent-report.html` (41.278.016 bytes).
- Raportul de componente este în același run, la
  `component-acceptance\component-extent-report.html`.

```powershell
.\scripts\run-romanian-expert.ps1
```

Runner-ul construiește într-o etapă TMP nouă, termină componentele înainte de
Android, instalează numai Whist Lab, păstrează diagnosticul eșecurilor și publică
Extent nativ cu capturi Base64 încorporate. Rapoartele și APK-ul testat rămân în
`C:\work\whist_back\romanian-expert\<run>\`. Aplicația originală nu este atinsă.

## Acceptanța completă a jocului original și paralel — 2 noiembrie

[Acceptanța partidei complete](docs/COMPLETE_GAME_ACCEPTANCE.md) este rezultatul
curent.

- Atât aplicația originală, cât și Whist Lab au încheiat 19 mâini reale, 109 levate
  și 436 de cărți jucate.
- Oracolul independent a verificat fiecare mutare și scor; ambele au încheiat cu
  `[89,126,145,119]`.
- Aceleași intrări au trecut și reluarea motorului Java pur.
- O verificare normală ulterioară pentru Whist românesc + Expert a confirmat
  ieșirea din replay-ul de depanare, punctarea și tranziția la mâna următoare.

Inventarul acceptat conține 219 cazuri JUnit/Gradle fără eșecuri, erori sau teste
sărite; publisher-ele au rulat separat.

- Raportul Extent nativ J12 are 6.655 de aserțiuni UI, 581 de înregistrări
  acțiune/observație și 325 checkpoint-uri cu capturi.
- Octeții PNG exacți și valorile expected/actual complete au fost confruntate cu
  HTML-ul publicat.
- Imaginile sunt Base64 încorporate; HTML-ul conține 307 URI-uri PNG data, cu
  305 hash-uri distincte ale octeților.
- Acceptanța funcțională este PASS; randarea aplicației originale rămâne WARNING
  vizual.

Raport: `C:\work\whist_back\deep-app-test\runs\20261101-231857-full-match\final-extent-report.html` (107.332.432 bytes).

- Auditul vizual al raportului în browser a fost blocat de politica MCP pentru
  fișiere locale; verificările automate de fidelitate HTML și inspecția directă
  a PNG-urilor au trecut.

```powershell
.\scripts\run-complete-match.ps1 -Serial '127.0.0.1:5555'
```

Comanda pornește partide noi.

- Dovezile deja obținute pot fi republicate fără replay Android; vezi raportul de
  acceptanță.
- Partidele măsurate și publisher-ul final au folosit Core C2 / Reporting J12.
- Core C3 livrat separat este înregistrat mai jos; livrarea nu schimbă versiunea
  execuției istorice.

## Teste de componentă și Android

Sunt necesare JDK 21 și Maven. Build-ul implicit nu contactează emulatorul. Pentru
rularea headless completă, cu emulatorul Podman deja pornit și aplicația instalată:

```powershell
.\scripts\run-deep-tests.ps1
```

Scriptul rulează testele de componentă ale fiecărui modul, apoi suitele Android
nominalizate pentru componente și joc live.

- `scrcpy` poate rămâne închis; emulatorul și puntea ADB host trebuie să fie active.
- După restartul gazdei, serverul ADB poate să nu aibă încă serialul
  `127.0.0.1:5555` conectat. Runner-ele live execută `adb connect`, apoi cer
  `get-state` și continuă doar dacă primesc `device`; fiecare comandă de setup
  are limită de 15 secunde, iar indisponibilitatea oprește flow-ul înainte de
  instalare sau UI. Pașii manuali și endpointul curent sunt în
  [testarea Android](docs/ANDROID_TESTING.md).
- Dovezile se salvează în afara surselor, în
  `C:\work\whist_back\deep-app-test\runs\`, și se publică într-un singur raport
  Extent HTML.
- Capturile Android sunt atașate checkpoint-urilor UI nominalizate prin biblioteca
  Reporting curentă, ca URI-uri Base64 încorporate. XML Surefire, textul, ierarhia
  UI și jurnalele rămân diagnostice externe execuției; cei șase publisheri
  adaptivi exclud linkurile lor din HTML-ul public.

Baseline-ul istoric de descoperire este
`C:\work\whist_back\deep-app-test\runs\20261101-222629\`.

- J4 a raportat 498 de cazuri Surefire: 497 trecute, fără eșecuri sau erori, iar
  un publisher opt-in a fost sărit în rularea obișnuită a modulelor.
- Publisher-ul a fost apoi rulat și a trecut explicit.
- Acceptanța Android a trecut 6/6 fluxuri (patru pentru aplicația originală și
  două pentru Whist Lab); suita de componente a adaptorului Android a trecut 6/6.
- `game-core` Whist Lab a trecut 29/29 teste Gradle; APK-ul de depanare s-a
  construit și instalat.

Raportul Extent final are 16.805.609 bytes și 42 URI-uri Base64 PNG; folderul
rulării păstrează PNG-urile originale, ierarhiile UI, rezultatele Surefire și
jurnalele.

- Înregistrează foaia de scor originală cu 10 cărți și confirmă că `Next Hand`
  deschide intervalul de licitație 0–9 pentru mâna cu nouă cărți.
- Celulele de scor sunt dovezi vizuale manuale, iar calculul este explicit
  condițional; identitățile cărților, câștigătorii levatelor și partida completă
  originală nu sunt verificate automat.
- Vezi `C:\work\whist_back\deep-app-test\runs\20261101-222629\final-extent-report.html`
  și [notele testelor Android](docs/ATF_Android_Whist_Testing.md) pentru raport și limite.

Aplicația originală are defecte serioase de afișare în acest emulator: fundal
verde-neon, conținut supradimensionat/suprapus, cărți deformate și cărți din mână
tăiate.

- Extent marchează acceptanța vizuală WARNING și încorporează capturile UI.
- Multiplayer-ul nu a fost testat.
- Acceptanța ulterioară a partidei complete de mai sus înlocuiește limitele
  baseline-ului de descoperire pentru identitatea cărților, câștigătorii
  levatelor, scor și completarea partidei.
- Aplicația originală nu a fost reinstalată în acea rulare, iar contul Google nu
  a fost modificat.

Pentru structura pachetelor, proprietățile de activare, fluxul rapoartelor și
comenzile individuale, vezi [organizarea testelor Android](docs/ANDROID_TESTING.md).

## APK Whist Lab pentru instalare directă

APK-ul actual este [WhistLab-0.1.1-debug.apk](dist/WhistLab-0.1.1-debug.apk).
Declară `minSdk 24`, `compileSdk 36` și `targetSdk 36`; nu include biblioteci
native sau split-uri ABI. Build-ul 0.1.1 a fost instalat și lansat pe emulatorul
API 28 x86_64. Testele UI și instalarea pe telefon fizic nu au fost repetate pe
această versiune.

`targetSdk 36` înlocuiește ținta veche API 28, care producea pe telefon avertizarea
Play Protect „Unsafe app blocked / built for an older version of Android”.
Google spune că avertizarea apare când ținta este cu peste două niveluri API în
urma versiunii curente; actualizarea țintei nu garantează că Play Protect nu va
afișa alte avertizări pentru un APK instalat din afara Play. Vezi
[ghidul Google Play Protect](https://developers.google.com/android/play-protect/warning-dev-guidance)
și [cerințele curente Play pentru target API](https://developer.android.com/google/play/requirements/target-sdk).
Pentru trimiteri noi/actualizări pe Google Play, cerința din 31 august 2026 este
Android 16 / API 36.

Build-ul 0.1.1 folosește aceeași interfață ca 0.1.0; schimbarea acestei versiuni
este ținta API și numărul versiunii. Instalarea și lansarea pe emulator au reușit,
dar fluxurile UI nu au fost rerulate după schimbarea target SDK.

```powershell
$env:ADB_SERVER_SOCKET = 'tcp:192.168.56.1:5038'
& 'C:\tools\whist-android-sdk\platform-tools\adb.exe' -s 127.0.0.1:5555 `
  install -r 'dist/WhistLab-0.1.1-debug.apk'
```

APK-ul este semnat cu cheia debug locală; SHA-256 este în
[`WhistLab-0.1.1-debug.apk.sha256`](dist/WhistLab-0.1.1-debug.apk.sha256).
Nu folosi semnătura debug pentru distribuție. Pentru actualizări păstrează aceeași
cheie de release; o semnătură diferită blochează instalarea peste pachetul vechi,
iar dezinstalarea poate șterge datele locale.

La adăugarea unui consumator nou, copiază numai adaptorul sau testele de care are
nevoie. Păstrează harness-ul generic, contractele backend și implementarea
Reporting în modulele lor responsabile.

[Reporting J12](docs/ATF_Reporting_J12_Verification.md) consemnează intenția
explicită a artefactelor, sursele de timing și indicatorii de aserțiune; dovezile
J11 rămân istorice.

## Livrarea verificată Core C3

[Verificarea Core C3](docs/ATF_Core_C3_Verification.md) leagă perechea JAR/POM
nouă de regresiile native sursă/JAR și acceptanța separată a consumatorului.
Acceptanța C3 a folosit Reporting J12; livrarea Reporting curentă este consemnată
separat mai jos.

[Reporting J15](docs/ATF_Reporting_J15_Verification.md) păstrează PNG-urile
complete încorporate și valorile ACTION/OBSERVE redactate, expandabile, cu
previzualizarea de 240 de caractere. Core C3 rămâne identic la nivel de octeți;
acceptanța sursei/JAR-ului și cea a consumatorului sunt înregistrate separat.

J14 împarte proiecția rândurilor ACTION/OBSERVE și o singură politică de redactare,
inclusiv pentru titlul capturii.

- Celulele expandate păstrează spațiile capturate; previzualizarea implicită de
  240 de caractere și verdictul original rămân neschimbate.
- Rulările istorice pe dispozitive păstrează versiunea Reporting înregistrată.

## Limitele auditului consumatorilor J14

| Flux | Limita acceptanței curente |
|---|---|
| Grila de licitații individuale | Există JUnit și trace; nu este publicat HTML standalone |
| Reluare legacy | PASS-ul JUnit este contrazis de meniul capturat și butonul Resume Game dezactivat; numărul de pixeli nu dovedește reluarea partidei |
| Raport legacy | Două PNG-uri inline se randă; aserțiunile evaluate și timing-ul execuției ADB nu sunt exportate |
| Dispozitiv partajat | Se respectă lease-ul Android extern activ; randarea artefactelor nu dovedește acceptanța izolată pe dispozitiv |

[Auditul J14](docs/ATF_Reporting_J14_Verification.md) separă consumarea
bibliotecii de acceptanța pe dispozitiv. [Feedback-ul J4](docs/feedback/ATF_Android_Alignment_Feedback.md#j14--criterii-active-demonstrate)
deține aceste remedieri pentru consumator.

## Compoziția auditabilă a raportului inline

[Contractul rapoartelor Android/componentă](docs/ATF_Android_Report_Contract.md)
definește cum rămân într-un singur HTML fiecare invocare, comparație evaluată,
interval capturat de execuție și PNG original. Informațiile obligatorii trebuie
să fie lizibile inline; XML, JSON și fișierele properties rămân diagnostice ale
execuției, în afara raportului public.

Migrarea publisher-ului rămâne deschisă în
[feedback-ul J4](docs/feedback/ATF_Android_Alignment_Feedback.md); tabelul
descrie contractul cerut.

| Familie de raport | Prezentare obligatorie |
|---|---|
| Componentă | identitatea cazului, intrări/stare și tabele standard de aserțiuni expandabile; captura nu este obligatorie |
| E2E | pași măsurați, comparații independente și PNG original inline la checkpoint-ul corect |
| Agregat | un raport comun cu invocările reale; numerele rămân distincte de observații, mutări și imagini |

J15 păstrează și CR/LF literal și newline-urile inițiale în corpurile TEXT inline.
[Auditul raportului curent](docs/ATF_J4_Report_Audit.md) separă verificările
proaspete de componente, reconstrucția E2E arhivată, controalele negative
deliberate și lacunele rămase la consumator/randare.

## Vizualizatorul de capturi — introdus în J16

[Reporting J16](docs/ATF_Reporting_J16_Verification.md) încadrează dovezile PNG
complete în viewport-ul browserului și păstrează checkpoint-ul selectat,
subtitrarea locală, secțiunile expandate și focusul la închiderea vizualizatorului.
Core C3, previzualizarea de 240 de caractere și contractele UI/API/SQL existente
rămân neschimbate. Setup-ul producătorului, intrările, valorile expected
independente și completitudinea capturii își păstrează acceptanța proprie.

## Colecții de teste și ajutor bilingv — J16

Vezi [colecțiile de teste](docs/TEST_COLLECTIONS.md), [Cum se joacă](docs/rules/WHIST_RO.md)
și [How to play](docs/rules/WHIST_EN.md). Regresia curentă exclude turneele de
învățare; rapoartele de învățare selectează exemple complete și toate eșecurile,
cu domeniul și omisiunile explicite. Artefactele de testare Android și partidele
de antrenare rămân în afara proiectului; copia APK-ului de instalare directă este
publicată în `dist/`.

## Rapoarte adaptive de joc — AUTO, SUMMARY și FULL

[Contractul de prezentare](docs/ATF_Game_Report_Presentation.md) definește
politica unică folosită de toți cei șase publisheri. Grupurile native de
aserțiuni, valorile obligatorii complete, bugetele pozitive ale corpului și
PNG-urile originale păstrează stilul Reporting comun și previzualizarea de 240
de caractere.

- [Modul proprietar](atf-tests/README.md): suport în `main` Java, entrypoint-uri
  publisher și contracte permanente de componentă.
- [Verificare](docs/ATF_Game_Report_Verification.md): rulare individuală/în suită,
  Extent/fallback, dovezi arhivate și sanity J1/J2/J3.
- [Feedback rămas](docs/feedback/ATF_Android_Alignment_Feedback.md): dovezile
  business legacy lipsă, timing-ul și acceptanța pe dispozitiv rămân explicite.

- R6 offline algorithms: [analysis and measured limits](docs/AI_ROUND6_ANALYSIS.md).
- Twenty small regressions protect the late-card correction.
- Statistical bid-evidence weights failed untouched evaluation and are not deployed.
- UI acceptance remains a separate iteration.

## Large-report navigation — J17

[J17 acceptance](docs/ATF_Reporting_J17_Verification.md) records the retained-evidence
navigation fix, permanent source/JAR regressions and this workspace's own sanity.
The full Android artifact retains its supplied comparison rows and original PNGs;
closed tables are measured when opened, and timing/view indexes are reused only
within their aggregation pass. Preview 240, independent verdicts and the shared
UI/API/SQL table formatting remain part of the contract.

## Cold-start report readiness

[Reporting J18 verification](docs/ATF_Reporting_J18_Verification.md) records
single initialization for the requested view, truthful readiness and native
navigation-link repair. PNGs, audit comparisons and preview 240 are retained;
readiness/action and navigation deadlines remain 10 s and 45 s.

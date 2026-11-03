# Feedback de aliniere ATF J4 Android

Revizie live: 2026-11-03; Core C3 și Reporting J18.

Politica comună a celor șase publicatoare este implementată și verificată în [contractul oficial](../ATF_Game_Report_Presentation.md) și [probele de regresie/randare](../ATF_Game_Report_Verification.md). Secțiunile J14/J15 de mai jos păstrează probe istorice; criteriile active nu includ remediile de prezentare deja livrate.
Biblioteca și testele generice sunt în J0; cerințele de mai jos aparțin consumerului.

## Revizie consumer UI — 3 noiembrie, APK 0.1.2 din sursele live

[Acceptanța curentă](../WHIST_UI_012_ACCEPTANCE.md) păstrează separat verdictul
jocului, publicarea Extent și revizia vizuală. J0 și JAR-ul J18 nu sunt modificate.

| Defect confirmat | Corecție / probă |
|---|---|
| Auditul cerea Base64 repetat pentru imagini identice | Linkul checkpoint-ului este rezolvat către PNG-ul canonic; componentele resping referința coruptă pentru imagini identice și distincte |
| Listele JSON din OBSERVE erau comparate cu formatarea sursei | Comparație exactă per rând numerotat și celulă, după redactorul/normalizatorul public; fără eliminarea arbitrară a spațiilor |
| Primul `</details>` trunchia observația lungă | DOM-ul păstrează disclosure-ul imbricat și textul complet, inclusiv CR/LF; coruperea ultimei valori este respinsă |
| `finish(PASS)` ștergea FAIL-ul capturat | Șase teste de trace, cu negative pentru excepție, checkpoint, assertion, acțiune și acțiune încă neatașată |
| Profilul fizic fără override era declarat neaplicat | Rezoluția efectivă folosește override-ul dacă există, altfel valoarea fizică; profilul și densitatea sunt assertions și condiții JUnit explicite |
| Jocul trecea, dar publicarea eșua fără delimitarea cauzei | Receipt-ul pilotului păstrează `businessStatus`, `reportStatus` și `failureStage`; verdictul global rămâne FAIL la eșecul raportării |

Cele 16 teste focalizate publisher/trace/profil au trecut. Pilotul Hard cu șase
mâini a trecut; HTML-ul său, copiat singur, a fost verificat în Edge: 68 de noduri
și 87 de disclosures expandate, 31 de tabele de assertions, 23 de acțiuni și
31 de observații. Viewerul a deschis atât PNG-ul canonic, cât și o referință
reutilizată, a păstrat proporțiile și expandările după revenire.

**Nu închideți prin aceste probe:** RPT-05 pentru intervalele originale ale
fazelor, migrarea receipt-urilor legacy, oracolul vechi resume bazat pe pixeli
sau lizibilitatea tuturor rangurilor din mâna de 13 cărți. Capturile noi au
indici parțial acoperiți, deși XML-ul îi conține; criteriul vizual rămâne deschis
în [feedback-ul jocului](WHIST_LAB_User_Experience_Feedback.md).

### P1 — Oracolele de geometrie trebuie să includă conținutul expus

Un test care verifică un singur rând și 13 noduri accesibile nu dovedește că
rangul și culoarea se văd. Introduceți o probă deterministă pentru banda efectiv
expusă, rangul 10, toate culorile și cartea ridicată; verificați separat nume,
hitbox-uri și textul acoperit de vecini. Raportul trebuie să numească exact
afirmația dovedită, nu să folosească «perfect display» pentru bounds valide.

### P2 — Suportul de acceptanță și scenariul de display trebuie descompuse

`WhistLabGameplayE2eTest` depășește 1.800 de linii și amestecă profile de display,
geometrie, eșantionarea levatei, GIF și scenarii. Extrageți întâi oracole pure și
operațiile de display în suport J4 tipizat, cu teste negative și fără dependență
de renderer. Numai mecanismele ADB reutilizabile aparțin `atf-android/main`;
regulile Whist și assertions de acceptanță rămân la consumer. Refactorizarea
nu trebuie să schimbe numărătorile, ordinea checkpoint-urilor sau verdictul.

## UI checkpoints Extent — rezolvat 2026-11-03

Publisherul Whist Lab separa capturile de pașii UI și nu oferea aceeași
corelare per pas ca raportarea Playwright. `AndroidUiFlowReportPublisher` din
J4 folosește acum API-ul public `UiExtentReportSession.UiPhase.step`: fiecare
checkpoint Gherkin deține tabelul standard de acțiuni, captura PNG inline și
tabelul de aserțiuni evaluat. Receipt-ul publică identitatea și numărătorile,
iar XML/properties rămân fingerprint-uri colapsate fără linkuri locale sau dump
brut. Failure-ul de dinaintea primului checkpoint rămâne un diagnostic FAIL.

Timpul Tap/Back/Swipe fusese măsurat împreună cu pauza fixă de stabilizare UI;
timerul este acum oprit imediat după comanda ADB, iar testul componentă verifică
că așteptarea de 500 ms nu intră în TIME. Probele componente au trecut 3/3, iar
cele două flow-uri live pe API 28 au trecut: 6 checkpoint-uri pentru opt cărți și
2 pentru treisprezece cărți, cu raport Extent individual per flow. Rapoartele de
verificare sunt în `%TEMP%\ATF_J4_REPORT_E2E_20261103_1609`; nu s-au modificat
sursele Reporting J0.

## J14 — criterii active demonstrate

Owner: J4 pentru oracol, compoziție și instrumentarea execuției; J1 coordonează.
J0 Reporting nu poate reconstrui comparații ori intervale pe care callerul nu le publică.

| Probă live din TMP | Observație verificată |
|---|---|
| bid-grid individual, 3–6 jucători | JUnit PASS; PNG/XML și ui-flow properties sunt produse, fără HTML individual |
| resume individual | JUnit PASS în 37,476 s; HTML nativ arată circa 209 ms ai construirii/publicării |
| false PASS resume — prioritate ridicată | PNG și XML arată meniul cu Resume Game enabled=false; pixelii luminoși trec totuși pragul de 5.000 |
| assertions resume | două PNG inline exacte și metadata; zero comparații expected/actual trimise către Reporting |
| contracte Android | două comparații screenshot adapter și un contract renderer trec; nu sunt acceptanță business |

- [ ] Refuzați meniul și Resume Game dezactivat ca dovadă de joc reluat. Introduceți oracol independent de stare/identitate înaintea heuristicei raster, cu negative pentru meniu, buton disabled, XML stale și captură schimbată concurent. Nu validați cărți numai prin pixeli luminoși.
- [ ] Publicați HTML și pentru trace-ul individual, la închiderea ownerului local, inclusiv la failure; reutilizați compoziția consumerului fără renderer HTML privat.
- [ ] Trimiteți comparațiile deja evaluate în grupul standard colapsabil; nu înlocuiți rezultatul JUnit cu PASS-uri de screenshot și nu reevaluați business-ul în Reporting.
- [ ] Capturați intervalul execuției ADB și publicați-l prin contractul existent de timing. Separați publicarea/flush-ul; nu prezentați durata construirii raportului drept journey.
- [ ] Păstrați probă componentă a adaptorului și individual/suită, pe același JAR, cu valori și verdict independente; păstrați preview 240.

Dovadă: [audit J14](../ATF_Reporting_J14_Verification.md), consumer-journeys/j4-adb și j4-resume în staging-ul datat. Randarea celor două PNG trece; acceptanța izolată pe device nu este dedusă din artifact.

## Compoziție inline — constatări suplimentare din codul live

| Criteriu | Dovadă actuală | Acceptanță cerută |
|---|---|---|
| populații | RuleComponentExtentReportTest pune cazurile JUnit în observations și creează două scenarii globale | un scenariu per invocare; numărători JUnit/model/assertions/PNG separate |
| assertions componente | expected/actual sunt fields/bodies fără verdict pe rând pentru traiectoria modelului | comparații evaluate exportate în tabelul standard; faptele nu inventează PASS |
| fișiere | publisherii construiesc Artifact(path.toUri()) pentru XML, properties și alte fișiere | profilul J4 public fără linkuri ori căi necesare deciziei; fapte complete inline |
| raw dump | WhistAiTrainingExtentReportTest.observeFile citește întregul fișier ca TEXT și atașează linkul | proiecție semantică lizibilă; fără dumps XML/JaCoCo/source ca substitut al contextului |
| acceptor | căutare globală de text și set global de hash-uri PNG | corelare exactă per invocare/fază/pas, inclusiv multiplicitate și ordinea dovezilor |
| contract local | AndroidInlineScreenshotReportContractTest verifică legitim PNG-ul, fără comparații business | familie complementară de contracte J4 pentru compoziția per invocare, assertions, timing și failure parțial; nu lărgiți artificial testul focalizat PNG |
| lipsă date | observeFile ignoră fișierul absent; unele grupuri au doar status din JUnit | dovezile obligatorii absente/invalide, zero cazuri și all-SKIP nu produc acceptanță PASS |

- [ ] Aplicați [contractul de compoziție Android](../ATF_Android_Report_Contract.md), prin API-ul J0 existent; fără renderer privat sau parsare Android în Reporting.
- [ ] Păstrați XML/JSON/properties ca diagnostice externe ale execuției, fără referințe afișate în profilul public cerut. Datele necesare verificării sunt deja proiectate în HTML.
- [ ] Înregistrați la testul de componentă valorile și rezultatul evaluării unice; un rezultat JUnit agregat nu reconstruiește assertions pierdute.
- [ ] Migrați acceptorii de la contains global la inventar pe owner, cu negative last-row, cross-owner, FAIL→PASS, missing receipt și PNG mutat la sibling.

Rularea AI folosește snapshot-ul înghețat C3/J13, conform library-binaries.json; nu este relabelat ca J14/J15. Nu se modifică rularea sau lease-ul activ.

[Auditul J15](../ATF_Reporting_J15_Verification.md) confirmă în TAB MCP cele două scenarii/189 referințe locale și zero tabele assertions în componenta recompusă. JAR-ul corectează TEXT; criteriile consumer de mai sus rămân deschise.

## Audit componentă și randare — probe noi din 2 noiembrie

[Auditul reproductibil](../ATF_J4_Report_Audit.md) distinge execuțiile noi, reconstrucția E2E arhivată și controalele deliberat negative. Rularea Android nouă este amânată până la eliberarea lease-ului; nu este declarată PASS prin skip ori prin republicarea capturilor vechi.

| ID / prioritate | Dovadă chirurgicală | Criteriu de închidere |
|---|---|---|
| J4-RPT-01 / P0 | publishUiFlow acceptă flow.status=PASS cu phaseCount=0; HTML arată PASS fără checkpoint, assertions ori PNG | acceptorul J4 refuză receipt-ul gol pentru un journey de acceptanță; păstrează observațiile media legitime fără assertions inventate |
| J4-RPT-02 / P1 | un testcase transformat controlat în SKIP apare cu RESULT=PASS în observation, deși sumarul are skipped=1 și publisherul refuză global acceptanța | mapare distinctă PASS/FAIL/ERROR/SKIP per invocare; negative pentru un SKIP, all-SKIP și zero cazuri |
| J4-RPT-03 / P1 — rest de corelare legacy | TEXT decodat o dată, JSON complet și fragmentele sunt reparate și testate; acceptorii legacy încă indexează global valori și PNG-uri | inventar per invocare/fază/pas, cu negative cross-owner și multiplicitate; nu închideți criteriul doar pentru că decodarea trece |
| J4-RPT-04 / P1 | selecția are 130 cazuri în patru clase, fără clasele exhaustive dedicate; Executed coverage afișează totuși constantele 6.375 / 4.960 | fiecare metrică executată provine dintr-o receipt legată de selecție; domeniul normativ potențial se declară separat |
| J4-RPT-06 / P1 | RomanianWhistModelTest singur trece 88 de cazuri; publisherul cade înainte de HTML pentru ai-tournament.properties absent | raport component individual fără dependențe de benchmark-uri neexecutate; unitățile native rămân categorie separată |
| J4-RPT-05 / P1 | suma intervalelor suitelor JUnit este 39,742 s, iar a metodelor 39,542 s; cele două scenarii Extent afișează circa 0,579 s de compoziție | executionTiming din intervalele capturate, fără confundarea publicării cu execuția; test negativ schimbă numai timing-ul |

- [ ] Pentru model/component rulat individual, eliminați dependența obligatorie de un benchmark AI fără legătură cu selecția; păstrați clasificarea separată pentru unit tests native, care nu primesc automat contractul E2E.
- [ ] Stocați compoziția reutilizabilă în main Java, cu modele tipizate per invocare și oracole independente; testele rămân teste, fără un renderer privat ori un modul nou.
- [ ] Verificați candidate HTML înaintea promovării ca raport acceptat. La eșecul acceptorului, păstrați separat verdictul business capturat și starea verificării raportului; nu publicați un fișier aparent acceptat numai fiindcă sesiunile s-au închis.
- [ ] Înlocuiți cei 578 de href file ai E2E-ului recompus cu fapte semantice inline. Cele patru celule assertions lungi păstrate în metadata nu se elimină înaintea unei alternative complete și corelate; boundary-ul generic 133 rămâne în J0.
- [ ] Păstrați cele 287 de capturi: 177 PNG canonice plus 110 referințe corecte. Verificarea exactă per checkpoint a trecut pentru 11.047 assertions; nu creați un issue de capturi lipsă prin numărarea exclusivă a data URI-urilor.

Problema de revenire din PNG duplicat este [REPORT-SCREENSHOT-RETURN-137 în J0](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-return-137--png-reutilizat-schimbă-ownerul-și-pierde-expandările), reprodusă fără Android. J4 păstrează acceptanța consumerului; nu repară JavaScript-ul bibliotecii prin postprocesarea HTML-ului.

## Un singur artifact — prototip verificabil, migrare încă deschisă

[Contractul actualizat](../ATF_Android_Report_Contract.md#one-artifact-original-execution-identities) cere un HTML autonom per rulare, nu un singur scenariu global. Prototipul din TMP folosește API-ul public J15, fără modificări HTML, și păstrează patru clase/130 invocări reale; nu închide migrarea publisherului live.

- [ ] Înlocuiți compoziția celor două rezumate globale cu scenarii per clasă/metodă/parametri. Atașați traiectoria exact invocării care a evaluat-o; metricele comune au selecție și scope, fără teste PASS fictive.
- [ ] Capturați receipt tipizată per invocare la evaluare: run/case ID, parametri, input fingerprints, expected/actual/status, cauza și ordinea checkpoint-urilor. Păstrați receipt parțială și intervalele originale la failure; nu reconstruiți comparații din JUnit PASS.
- [ ] Completați [RPT-04/05/06](#audit-componentă-și-randare--probe-noi-din-2-noiembrie) cu oracole pentru individual/model fără AI, selecție mixtă, SKIP/ERROR/FAIL, timing și lipsa detaliilor. Portarea decoderului legacy din prototip nu substituie capturarea canonică.
- [ ] Validați HTML-ul copiat singur într-un folder gol: toate datele necesare deciziei sunt inline, în ownerul original, fără dependențe XML/properties/log ori agregarea execuțiilor istorice ca o rulare nouă.

Proba diagnostică leagă 425 de comparații originale de players=4, AI=NORMAL, seed=0, prin snapshot-ul producătorului și receipt-ul completat după assertEquals. Celelalte invocări păstrează rezultatul capturat și limita explicită a detaliilor; nu sunt declarate complet auditabile prin această republicare.

## Adaptive game detail — configurație consumer, fără pierderea verdictului

Rezolvat pentru cele șase publicatoare: un singur config/game-evidence.yml, value objects imuabile, preview 240, body pozitiv, grupuri native și buget opțional cumulat. AUTO/SUMMARY declară invocările learning selectate și omise; FULL refuză acceptanța completă dacă detaliul cerut nu încape. Scope-ul legacy nu este eșantionat implicit.

- Contractul stabil, schema și responsabilitățile sunt în [documentația de proiect](../ATF_Game_Report_Presentation.md); nu duplicăm cerințe închise ca TODO.
- [54 contracte permanente](../ATF_Game_Report_Verification.md) acoperă individual/suită, Extent/fallback, ultima comparație, valori complete, JSON/TEXT, PNG, omisiuni și INCOMPLETE persistent.
- Lipsa receptei business, timing-ul legacy și performanța artifactelor mari rămân criterii separate; un profil de prezentare nu reconstruiește date necapturate.

## Ecran Android integral — captură și afișare

- [ ] Păstrați întregul display Android, toate marginile și orientarea, fără crop ori înlocuire cu regiunea analizată. Verificați dimensiunile fizice și bytes în componenta adaptorului, apoi corelarea cu checkpoint-ul în E2E sub lease.
- [ ] Folosiți viewer-ul generic cu [fit integral 138](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-viewport-138--imagine-integrală-în-spațiul-disponibil) din J16; nu introduceți CSS sau galerie privată în J4. Păstrați acceptanța distinctă pentru [revenirea 137](../../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md#report-screenshot-return-137--png-reutilizat-schimbă-ownerul-și-pierde-expandările).

Codul live capturePng folosește exec-out screencap -p și scrie bytes fără decupare. Aceasta confirmă mecanismul capturii, nu geometria viewer-ului; [contractul complet](../ATF_Android_Report_Contract.md#full-display-android-screenshots) separă cele două responsabilități.

## Reader autonomy — input-uri pentru implementarea J4

**P1; acceptanță din perspectiva cititorului fără checkout J4.** Un HTML fără linkuri externe rămâne insuficient dacă afișează doar nume interne, coduri RW, hashes ori expected PASS / actual PASS. Conținutul trebuie să explice afirmația verificată, inputurile și motivul verdictului.

| ID | Problemă live / input de implementare | Închidere verificabilă |
|---|---|---|
| J4-RPT-08 | corpus proiectează fiecare joc ca expected PASS / actual PASS, deși replay-ul detaliat există numai pentru referință | sumarul declară că este un rezultat de evaluator; fiecare afirmație de audit complet cere inputuri, regulă explicită și comparații capturate per joc, cu identitatea exactă |
| J4-RPT-09 | secțiuni Complete evidence conțin JSON/loguri brute; regula, setup-ul și notația nu sunt explicate consecvent în același artifact | scop scurt, profil/configurație efectivă, stare inițială, inputuri/seed, pași și expected independent; glosar inline pentru termeni, unități și ordinea seat/array |
| J4-RPT-11 | AndroidScreenshotEvidence validează semnătura PNG, fără decodare raster ori dimensiuni fizice | adaptorul verifică imaginea completă, orientarea și dimensiunile capturii originale, cu negative pentru PNG trunchiat/crop; Reporting păstrează bytes, fără parser Android |
| J4-RPT-10 | zero ms în unele acțiuni Android este declarat placeholder în notele publisherului | instrumentați acțiunea la execuție; valoarea necapturată nu este prezentată ca latență măsurată zero; separați timing execuție/captură/publicare |

- [ ] Începeți fiecare invocare cu ce verifică și limita dovezii: componentă JVM, execuție device nouă, replay offline ori cadru istoric. Titlul este semantic, iar clasa/metoda/parametrii rămân identitate tehnică verificabilă.
- [ ] Publicați setup-ul efectiv și precondițiile evaluate, nu numai versiuni JAR sau numele clasei. Pentru model: profil/reguli, jucători, AI, seed, scor/serii și intrările reale; pentru UI: build, configurație, starea inițială și acțiunile ordonate.
- [ ] Păstrați expected-ul independent și valorile actuale în tabelul standard de patru coloane. Un status primit de la alt evaluator este un sumar al acelui evaluator, nu dovada că fiecare regulă din titlu a fost inspectată aici.
- [ ] Proiectați contextul semantic lângă comparații: înainte / acțiune-input / după; explicați termenii hand/trick/bid, indexarea seat, won/totals și varianta exactă a regulilor. Corpurile complete sunt suport de audit, nu înlocuitor al explicației.
- [ ] Mutați checksums, inventare sursă și căi de build în proveniență tehnică opțională, colapsată și deduplicată per selecție. Un SHA-256 identifică bytes; nu dovedește corectitudinea business și nu trebuie repetat la fiecare invocare din același XML.
- [ ] Păstrați un singur HTML autonom, preview 240 și tabelele UI/API/SQL existente; fără CSS/JS privat, dump-uri în loc de explicații ori request-uri fictive pentru grupare. Reutilizarea compoziției rămâne în main Java J4, cu regresii de contract la owner.
- [ ] Adăugați negative pentru lipsă setup/oracol/ultimul pas, glosar absent, profil clasic/serie greșit și date mutate la alt joc. Rulați individual/suită și păstrați FAIL/SKIP/partial, fără a reclasifica respingerea unui candidat AI drept acceptanță funcțională nouă.

Biblioteca J0 deține numai modelele neutre și randarea comună. Viewer-ul și regresiile 137/138 sunt livrate în J16 la J0; aceste cerințe de conținut nu sunt mutate în bibliotecă și nu se închid prin distribuirea unui JAR.

## Ordinea remedierii

1. Refuzați false PASS-ul resume pe meniul capturat/disabled/stale, prin componente offline independente.
2. Capturați receipt per invocare, setup/input/oracol, comparațiile originale, intervalele și failure-ul parțial.
3. Completați contextul semantic și receptele legacy per invocare; pașii assertions nativi, gruparea și bugetele comune sunt livrate, fără a inventa inputuri business.
4. Rezolvați separat încărcarea și interacțiunea artifactelor mari, păstrând politica adaptivă și toate dovezile obligatorii.
5. Regenerați individual și prin suită pe J16; verificați reader acceptance, nominal/FAIL/SKIP/partial, ultimul pas și capturile originale.
6. Rulați E2E Android nou numai sub lease exclusiv; acceptanța generică J0 nu certifică device-ul ori business-ul.

[Coordonarea între proiecte](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md#j14--lacune-în-publicarea-individuală-j4) păstrează avizul pentru dispozitivul comun.

<details>
<summary><strong>Istoric — analiza inițială și închiderile din 1 noiembrie</strong></summary>

Data analizei: 2026-11-01  
Comparație: ATF J1, J2, J3 și consumatorul Reporting din J0.

## Verdict direct

J4 are o probă Android utilă și a ajuns până la un joc real, dar în forma curentă nu este un consumator ATF aliniat. Este un harness ad-hoc: protocolul ADB, scenariile, euristicile vizuale, atașarea probelor și publicarea raportului sunt lipite între ele. Rezultatul se poate rula, însă organizarea și raportarea nu se pot reutiliza coerent cu J1–J3.

Problema de raportare este cea mai urgentă: raportul este randat întâi, apoi `DeepSuiteExtentReportTest` îi rescrie HTML-ul și injectează o galerie cu `data:image/png;base64,...`. Asta reproduce doar aspectul unei dovezi UI; ocolește API-ul UI al Reporting și separă imaginile de pașii și verdictul care le-au produs. E fragil, greu de verificat și nu trebuie să devină un model pentru următoarele teste.

## Constatări cu prioritate

### P0 — Raportul inline este construit în afara contractului Reporting

**Dovadă în J4:** `atf-tests/src/test/java/atf/tests/DeepSuiteExtentReportTest.java` creează raport prin `HtmlReportSession`, apoi metoda `appendInlineScreenshotGallery` modifică direct documentul final. `ContractWhistSmokeTest.publishReport` publică încă un raport separat cu `HtmlReportSession`, iar capturile sunt înregistrate ca artefacte cu URI local.

**Comparație:** J1 `ShopOrderReportProbe` compune `ExtentReportSession` cu `UiExtentReportSession.adaptScenario`, profilează capturile prin `ReportEvidenceProfile` și transmite Base64 în `UiPhase.step`. Reporting deține randarea și politica pentru imagini.

**Impact:** raportul final poate arăta imaginile într-o galerie fără legătură cu pasul, cu assertion-urile sau cu statusul său. HTML-ul rezultat depinde de structura internă a rendererului și postprocesarea poate corupe sau duplica conținutul.

**Cerință:** toate capturile afișate în raport intră prin `UiExtentReportSession` și `ReportScreenshot`/facade-ul UI existent, cu PNG valid serializat ca Base64 și policy explicită. Nicio modificare a HTML-ului după `close()`.

### P0 — Consumatorul folosește o versiune Reporting rămasă în urmă

**Dovadă:** J4 declară `atf.reporting.version=1.0.0-j9`; J1, J2 și J3 declară `1.0.0-j10`. J1/J2/J3 înregistrează același SHA-256 pentru binarul J10 (`CE527BA085FC216656B7E4DFBF11C71193FBACE2CEE17C2B40E2AEDECB0F0AC9`) și pentru POM (`CB2358F7899243A35716EC2ED5B39740E7B94DAF646AD4A88BDDBCB799FF64D2`).

**Impact:** J4 nu consumă aceeași interfață de raportare verificată de celelalte proiecte și a trebuit să-și compenseze lipsurile cu un raport HTML propriu.

**Cerință:** aliniază J4 la artefactul J10 identic, copiază perechea jar/POM din distribuția deja validată și actualizează proveniența. Core și Reporting rămân biblioteci deținute de J0.

### P1 — Ciclul component / E2E nu este lizibil separat

**Dovadă:** `ContractWhistSmokeTest` ține două flow-uri lungi în aceeași clasă și aceeași zonă de test, cu `assumeTrue` repetat pentru opt-in. J4 nu oferă organizarea clară `component`, `e2e`, `suites` pe care J1 o folosește pentru clase și suite precum `AutoExComponentSuite`, `AutoExE2eSuite` și `ShopE2eSuite`.

**Impact:** un test local ADB poate fi confundat cu o componentă izolată; selecția din CI/IDE și analiza raportului cer cunoașterea internă a unui singur test monolitic.

**Cerință:** separă testele contractuale ale adaptorului Android de flow-urile care cer emulatorul. Adaugă suite numite explicit pentru fiecare grup; emulatorul rămâne opt-in și build-ul implicit nu îl accesează.

### P1 — Adaptorul Android nu expune încă o probă reutilizabilă

**Dovadă:** `AndroidUiDriver.capture` salvează perechi XML/PNG în fișiere și întoarce doar căile; logica de test recitește și interpretează capturile prin căi concrete.

**Impact:** următorul test va duplica citirea, validarea și atașarea capturilor; raportarea nu poate trata o captură drept pas UI tipizat.

**Cerință:** păstrează fișierele locale ca arhivă de debugging, dar expune din adaptorul Android captură PNG validată ca `ReportScreenshot` (title + Base64). Codul de raportare nu trebuie să știe unde ADB a salvat fișierul.

### P1 — Raportul agregat parsează rezultate după rulare în loc să le dețină

**Dovadă:** `run-deep-tests.ps1` rulează modulele unul câte unul, copiază Surefire XML/TXT, apoi pornește un test publisher separat. Publisherul reparcurge XML-urile și toate fișierele de probe pentru a reconstrui verdictul.

**Impact:** sunt două surse de adevăr (JUnit/Surefire și raportul compus), iar un rezultat lipsă sau vechi poate produce discrepanțe între ele.

**Cerință:** păstrează logurile și XML-urile drept rezultate native de diagnostic, dar Extent-ul final trebuie compus prin API-ul J10 și să păstreze fiecare checkpoint UI ca pas raportat. Suita trebuie să declare explicit ce testează și ce nu pretinde: run-ul actual nu verifică finalizarea meciului sau scorarea completă.

## Model țintă

1. J0 deține Reporting și contractele comune; J4 deține doar adaptoarele Android și scenariile aplicației.
2. Testele pure ale `AdbClient`/adaptării capturilor rulează fără emulator în suitea de componente.
3. Flow-urile reale ale jocului rulează într-o suită E2E separată, cu activare explicită și ordine serială.
4. Capturile ADB devin `ReportScreenshot` printr-un adaptor mic; testul trimite fiecare checkpoint la `UiExtentReportSession`, ca în testele UI J1.
5. PNG/XML pot rămâne artefacte arhivate pentru debugging, dar imaginea vizibilă în raport este un pas inline randat de Reporting (`data:image/png;base64,...`), fără postprocesare HTML.
6. Un singur flow nu se declară „joc complet” până când nu verifică scorarea și terminarea meciului. Defectele de layout observate rămân verdict vizual separat, nu sunt mascate de trecerea interacțiunilor.

## Criterii de acceptare pentru schimbare

- J4 consumă J10 cu hash-uri care corespund distribuției J1/J2/J3.
- Raportul final este generat de `ExtentReportSession` + `UiExtentReportSession`; sursa nu conține inserare de markup sau rescriere HTML după randare.
- Capturile din flow apar inline în raport cu `data:image/png;base64,` și sunt asociate cu numele checkpoint-ului, verificările și statusul potrivit.
- Contractele locale Android și scenariile care cer emulator sunt separate și selectabile prin suite numite; rularea implicită rămâne fără device.
- Raportul păstrează limitele reale ale acoperirii: jocul complet, scorarea și multiplayer-ul nu sunt declarate verificate fără probe dedicate.

## Implementare și verificare — 2026-11-01

Constatările de mai sus descriu starea inițială. În implementarea curentă:

- **P0 raport inline — rezolvat.** J4 folosește ExtentReportSession și
  UiExtentReportSession/ReportScreenshot; nu mai modifică HTML-ul după
  închiderea raportului. Testul publisher verifică URI-ul data:image/png;base64,
  și payload-ul PNG complet.
- **P0 Reporting J10 — rezolvat.** Perechea binară J10 este aliniată la hash-urile
  J1/J2/J3 și înregistrată în library-provenance.json.
- **P1 component/E2E — rezolvat.** Adaptorul și probele locale au suite de
  componente; jocul are suită E2E opt-in și serializată.
- **P1 adaptor de capturi — rezolvat.** Captura PNG validată devine
  ReportScreenshot; PNG/XML rămân în dosarul de dovezi.
- **P1 publisher agregat — îmbunătățit, cu limită cunoscută.** Extent-ul este
  compus prin API-ul J10, iar Surefire XML rămâne sursa pentru numărul testelor.
  Publisherul nu mai inserează markup HTML. Pipeline-ul agregat încă transferă
  și parsează rezultatele Surefire între module.
- **Validare reală:** rularea 20261101-164823 a trecut 5/5 contracte Android,
  2/2 flow-uri E2E; raportul de 7.1 MB include 13 imagini PNG inline Base64.
  Raportul păstrează WARNING-urile de afișare și limitele: fără meci complet,
  scor final sau multiplayer.

</details>

## Increment learning V4 — scope nou, 2 noiembrie 2026

Run C:/work/whist_back/ai-training/20261102-171252-round4/ adaugă un pipeline learning distinct; nu închide migrarea publisherilor legacy, false PASS resume sau criteriile UI vechi.

- Compoziția este în main Java, atf.tests.learning, cu o invocare per scenariu, recepte capturate în execuție, source hashes, comparații independente și timing original.
- Cele 256 invocări JUnit și un gate Node produc 136.202 rânduri native, fără HTTP fictiv, artifact links, XML/log dumps sau capturi fabricate.
- Controalele consumerului acoperă standalone, SKIP/ERROR, zero/all-SKIP, owner absent, last-row, cross-owner, FAIL→PASS, actual-cell și timing.
- Valorile lungi sunt internate fără pierderi numai în checkpoint-ul propriu; verdictul se copiază, nu se deduce din text.
- Auditul DOM incremental trece. Randarea interactivă a acelui artifact rămâne BLOCKED: agentul a raportat respingerea protocolului file de TAB MCP și nu a ocolit-o.
- J15 fizic a rămas fixat în run; reporting-watch.jsonl păstrează citirile live. PASS-ul jocurilor și al datelor nu închide acceptanța vizuală.

## Increment J16 — colecții, input adaptiv și autonomie a cititorului

Verificat la 2 noiembrie, run C:/work/whist_back/ai-training/20261102-184455-round5. Acest increment închide criterii numai pentru noua compoziție LearningRunReport; publisherii legacy și false PASS resume rămân deschiși. J0 JAR/CSS/JS/renderer nu au fost modificate.

- [x] Politică J4 imuabilă AUTO/SUMMARY/FULL din YAML strict separat; limite pozitive, chei/enum/tipuri/overflow/duplicate/multi-document refuzate. Setarea generică body este pozitivă și respectată; LearningRunReport nu mai impune DISABLE_BODY_DISPLAY_LIMIT.
- [x] Selecție pe invocări întregi: exemple deterministe per familie, toate non-PASS și gate-ul întregii populații. Ordinea originală, toate comparațiile incluse și cauza rămân intacte. Executat/selectat/omis și ID-urile sunt inline; HTML-ul selectat nu pretinde audit individual al omisiunilor.
- [x] Context obligatoriu fără trunchiere; refuz INCOMPLETE înainte de publicare dacă nu încape. Contextul tehnic comun apare o dată; diagnostic opțional deduplicat și plafonat pe înregistrări întregi, cu inventar original/retained/omitted. Nu se fragmentează automat o structură arbitrară peste buget.
- [x] Ghid de citire, reguli efective, glosar hand/trick/bid/seat, notația cărților, motivul comparațiilor și proveniența expected-ului independent. Scenariul include ID-ul real, pentru a evita contopirea unor titluri semantice identice.
- [x] Contracte pentru prag minus/exact/plus unu, mic/mare, FULL/SUMMARY/AUTO, individual/suită, input schimbat, ultima comparație, cross-owner, verdict, timing, context, omisiuni și buget insuficient. Auditul compară același JAR fizic la publicare și verificare.
- [x] Colecții quick/regression/documentation/learning/e2e documentate. Runner offline în TMP propriu; quick verificat real: 105 cazuri, zero failures/errors/skips, aproximativ 46 s măsurate. Regresia completă finală: 375 cazuri, zero failures/errors/skips, build+APK în 1m50s. Turneele learning sunt excluse din testul implicit.
- [x] Ajutor RO/EN din catalogul comun Android/documentație; 32 exemple executabile, 100 comparații în raportul de documentație. Scenariul E2E de meniu a fost extins pentru limbi și redeschidere, compilat; nu a fost executat pe device în acest run.
- [x] artifacts din atf-test-android mutat în C:/work/whist_back/atf-test-android-artifacts-20261102: 38 fișiere, 12.573.359 bytes, fiecare cale/dimensiune/SHA-256 identică. Niciun HTML istoric rescris.

- Probe: documentation/how-to-play-extent-j16.html are 32 invocări/100 comparații, 2.862.580 bytes. large03/selected-development-extent-j16.html are 5 din 129 invocări, 2.138 comparații, 4.739.578 bytes; păstrează FAIL-ul real al gate-ului candidatului respins. Reducerea este o selecție explicită de scope, nu o compresie pretins fără pierderi a tuturor jocurilor.

- Cele patru variante AI au fost respinse de criteriul prestabilit; politica acceptată V4 este restaurată byte-for-byte. Partidele de antrenament și pierderile sunt păstrate extern, iar casebook-ul de development are 14 exemple, fără promovare automată a acțiunilor observate ca mutări optime. Lotul 81000..81015 rămâne nefolosit.

- Limitele acestui increment inițial au fost revizuite separat: politica și body-urile celor cinci publisheri legacy sunt acum migrate, cu probe în contractul oficial. Oracolul resume, receptele legacy, instrumentarea E2E și auditul interactiv complet al artifactelor mari rămân deschise. Nicio republicare de receipts nu este o execuție Android nouă.

## Artifacte mari — performanță verificabilă, nu pierderea dovezii

| ID / prioritate | Probă / limită actuală | Criteriu de închidere |
|---|---|---|
| J4-RPT-14 / P2 | selecția learning primește populația completă deja materializată; profilul limitează publicarea, nu heap-ul încărcării | selectare pe metadate/receipts validate, cu limite explicite de încărcare; păstrați IDs, hashes, toate non-PASS/gates și oracolul independent al selecției |

J4 trebuie să măsoare compoziția și densitatea receipt-urilor. O limită neutră a viewer-ului/DOM-ului se reproduce separat cu date sintetice în J0 înaintea oricărei modificări de bibliotecă; nu atribuim aceste timeouts direct unui defect Reporting.

## R6 — offline algorithm acceptance and live input reconciliation

- See [AI R6 feedback](AI_TRAINING_ROUND6_Feedback.md) and [RO/EN analysis](../AI_ROUND6_ANALYSIS.md).
- The local rank correction is accepted on twenty targeted regressions plus fresh no-regression comparisons; statistical superiority is explicitly unproved.
- The rejected evidence model remains rejected.
- Latest live presentation grouping/budgets were preserved; complete configured collections are now protected from learning sampling.
- No device/UI acceptance is claimed.

## J4-UI-LEASE-16 — enforce exclusive device ownership at every entrypoint

**Priority: P1.** During the 3 November report verification, the shared lease
file declared active=false while a direct WhistLabGameplayE2eTest run was
using serial 127.0.0.1:5555. The report validation runner detected that foreign
process and refused to start its own device session; it did not stop the run.

- [ ] Acquire/release the same exclusive lease from the Android composition root
      used by Maven, IntelliJ and scripts, including failure and cancellation.
- [ ] Refuse a second owner before the first ADB action; stale-lease recovery
      must verify the owner process and preserve another active session.
- [ ] Add a no-device component contract for two competing owners and abnormal
      release. Keep actual app/device execution acceptance separate.

This coordination belongs to J4. Publishing an archived trace or fixing the
Reporting JAR cannot make concurrent device interactions auditable.

## J4-RPT-15 — targeted navigation verified

The [J17 record](../ATF_Reporting_J17_Verification.md) verifies the complete
55.7 MB archived artifact on the physical JAR, with targeted comparison/PNG
opening and unchanged evidence inventories. Full-display fit is visible in
the captured browser rendering. J17 excluded bulk expansion; the later J18 permission is limited to two
explicitly selected original owners.

The user approved publication with the recorded Chromium checks and visually
inspected captures while TAB MCP controls are unavailable. Android LIVE remains separate: the current
ADB precheck returned connection reset. RPT-14 and legacy business/timing gaps
remain open at their existing owners.


## Offline AI / arena — 3 noiembrie 2026

- [Feedbackul pe algoritmi și input](AI_SAFETY_ARENA_Feedback.md) consemnează criteriile distincte pe rol, ligile A/B, respingerea independentului Safety10 și catalogul contextual inactiv. Raportul acestei iterații a consumat J17 fizic: 56 de capturi întregi / 2.879 comparații / 6.613.666 bytes, cu audit static trecut. Din 426 de surse, selecția este făcută înaintea Java; toate ID-urile/hash-urile și limitele sunt declarate. J4-RPT-14 este îmbunătățit pentru această integrare, nu închis pentru loaderul generic.

- Prima publicare a refuzat owner-ul /setup duplicat în capturile de arenă. Producătorul J4 a fost corectat; capturile vechi rămân nemodificate și NOT_PUBLISHABLE. Două partide au fost reexecutate, cu toate cele 24 de mâini și scoruri identice originalelor. Nu s-a relaxat contractul J0 și nu s-a schimbat JAR-ul/rendererul. UI/device, false PASS legacy și acceptanța interactivă a acestui HTML rămân în afara acestei iterații.

## J4-RPT-17 — cold-start readiness corrected at the library owner

[The J18 acceptance](../ATF_Reporting_J18_Verification.md) records single
requested-view initialization, truthful readiness and lightweight native link
repair. Permanent neutral regressions remain in J0 Reporting Tests.

- Readiness/action 10 s, navigation 45 s and preview 240 remain unchanged.
- Android acceptance uses archived receipts and the complete HTML, including canonical PNG bytes and original comparison cells.
- Structural bulk acceptance is limited to original owners 3 and 4; each semantic tree must open completely, while local evidence disclosures retain their own policy.
- Device execution, RPT-14 population budgets and legacy input/oracle/timing gaps remain separate consumer criteria.

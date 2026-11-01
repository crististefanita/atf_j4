# Feedback de aliniere ATF J4 Android

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
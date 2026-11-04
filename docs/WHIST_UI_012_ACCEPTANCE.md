# Whist Lab 0.1.2 — acceptanța build-ului din 3 noiembrie 2026

Fișa păstrează rezultatele versiunii 0.1.2. Distribuția care a înlocuit-o este
documentată în [acceptanța 0.1.3](WHIST_UI_013_ACCEPTANCE.md). Dovezile și copiile de diagnostic rămân în afara
checkout-ului. Un PASS funcțional nu închide automat defectele de afișare.

## Identitatea build-ului

| Câmp | Valoare verificată |
|---|---|
| Pachet | `ro.whistlab.app` |
| Versiune | `0.1.2`, cod `3` |
| Android | minimum 24, compile/target 36 |
| APK | `dist/WhistLab-0.1.2-debug.apk`, semnătură debug locală |
| SHA-256 construit și instalat | `E29BAC633595C31E9B719A5EC93BFF5A2E43D7510BFE25D84D0E18CE1A438BF7` |
| Proveniență surse | 45 de intrări main/configurație Gradle comparate live–staging, zero diferențe |
| Dispozitiv | emulator existent API 28 x86_64, ADB `127.0.0.1:5555` prin host `192.168.56.1:5038` |

Sursele aplicației includ guard-ul mâinii goale, dropdown-ul limbii și titlurile
scurte. R14 rămâne motorul Hard acceptat. Experimentele offline concurente nu
sunt promovate în APK prin simpla lor prezență în sursele de test.

## Verificări focalizate

| Selecție | Rezultat și limită |
|---|---|
| Gradle `GameSessionTest`, `DifficultyContractTest` | 20/20; sesiune, reguli și maparea nivelurilor |
| Gradle `app:testDebugUnitTest` | 20/20; geometrie pură, texte și pauza levatei |
| `RomanianMatchOracleTest` | 9/9; oracol independent și negative |
| Publisher / trace / profil de display | 16/16; PNG identic/distinct, link corupt, valori lungi/JSON, FAIL persistent, rezoluție fizică/override |
| Pilot Android Hard | PASS, șase mâini românești, 36 de mutări, nouă levate, nouă cărți jucate de om prin UI |
| Reluare proces | PASS în pilot; aceeași stare păstrată după repornire |
| Extent pilot | audit automat și verificare interactivă în browser trecute |
| Display / navigare Android | 2/2 PASS; matrice 1/13 cărți și masa cu șase jucători / opt cărți |

Pilotul a produs totalurile `[5, 18, 19, 23]`, în ordinea You/Ava/Miles/Grace.
Acoperă contract exact zero/pozitiv, licitație prea mare/prea mică, urmare de
culoare, atu obligatoriu și decartare. Nu acoperă integral cele 24 de mâini,
toate variantele sau toate combinațiile de reguli/serii.

Probele anterioare rămân diagnostice: pilotul Normal a terminat jocul selectat,
dar a eșuat la auditul publicării; prima matrice de display a expus problemele
auditorului. Aceste execuții nu sunt reclasificate retroactiv în PASS.

## Profilele de afișare

| Profil logic | Densitate | Lățime logică aproximativă |
|---|---:|---:|
| 1080×1920 | 440 dpi | 393 dp |
| 720×1280 | 320 dpi | 360 dp |
| 1080×2448 | 440 dpi, simulată | 393 dp |
| 1200×1920 | 320 dpi | 600 dp |

Profilul înalt folosește rezoluția din [specificația ASUS ROG 7](https://rog.asus.com/nz/phones/rog-phone-7/spec/).
Densitatea este o alegere de test; telefonul fizic nu a fost verificat. PNG-ul
rămâne captura fizică integrală de 1080×1920, cu letterboxing pentru alte
rapoarte de aspect. Nu confundați lățimea întregului display cu lățimea utilă
a mesei după padding.

Scenariile selectate verifică 13 cărți la licitare, o carte centrată la licitare
și joc, respectiv șase jucători cu opt cărți, hamburger, scor și revenire.
Rezoluția și densitatea efective sunt comparate explicit cu profilul cerut;
baseline-ul fizic nu cere o linie `Override size` inventată de ADB.
La încheiere au fost restaurate `1080×1920 / 440 dpi`; aplicațiile Whist Lab și
audit au fost oprite, iar emulatorul a rămas online, pe ecranul Android Home.

## Raportul și cititorul

Raportul pilotului a fost copiat singur într-un folder și deschis în Edge.
Verificarea interactivă a expandat 68 de noduri și 87 de disclosures; a găsit
31 de tabele de assertions, 23 de acțiuni și 31 de observații și a ajuns la
ultimul rând al fiecărui tabel de assertions. PNG-ul canonic și referința
reutilizată s-au deschis integral, cu proporțiile păstrate; schimbarea modului
de afișare și închiderea viewerului au păstrat expandările existente.

Au trecut și cele două rapoarte de display, deschise separat: matricea are
19 noduri și 27 de disclosures expandate, navigarea 20 și 26. Fiecare are câte
șapte tabele de assertions, acțiuni și observații; ultimul rând și viewerul
integral au fost verificate. Capturile au fost revizuite vizual, iar problemele
de lizibilitate rămân explicit în feedback.

Valorile lungi se citesc în subpanoul lor complet, iar preview-ul rămâne 240.
Auditul verifică rândul, celula, verdictul și PNG-ul checkpoint-ului. Un FAIL
înregistrat nu este șters de `finish(PASS)`. Pilotul separă verdictul business
de cel al publicării. Timpii fazelor construite la publicare rămân o limită
deschisă: durata afișată a scenariului nu certifică durata întregii execuții ADB.

## Ce rămâne de îmbunătățit

[Feedback-ul UX](feedback/WHIST_LAB_User_Experience_Feedback.md) este registrul
activ. Prioritățile sunt intersecția licitării cu nouă opțiuni la patru jucători,
indicii parțial acoperiți în mâna de 13 cărți și lipsa unui oracol pentru textul
efectiv expus. Urmează încadrarea configurării, aspectul dropdown-ului, titlurile
repetitive, densitatea panourilor și foaia de scor prea încărcată.

Nu sunt acoperite aici ROG 7 fizic, Android 16 real, TalkBack, landscape sau
întreaga matrice EN/RO cu text mărit. Guard-ul mâinii goale are dovadă E2E;
contractul direct al rendererului pentru zero cărți rămâne de adăugat.

## Dovezi locale și reproducere

Rădăcina verificării este `%TEMP%\ATF_J4_FINAL_UI_20261103`:

- `apk-provenance.json`, `final-live-build-inputs.json`, `gradle-build.log` și
  `gradle-final-sync.log`: build, identitate și comparația cu live.
- `hard-six-hands/romanian-match-extent.html`, `romanian-match-result.properties`
  și `hard-six-hands-junit.xml`: pilotul nou și verdictul independent.
- `accepted-browser/browser-audit.json`: interacțiunile și geometria viewerului.
- `display-browser/browser-audit.json`, `table-browser/browser-audit.json`:
  auditul interactiv al celor două rapoarte de display.
- `display/`: prima execuție, păstrată ca diagnostic; capturile susțin și
  feedback-ul vizual chiar dacă auditorul de atunci a respins publicarea.
- `display-verified/` și `display-verified-junit.xml`: reverificarea după
  corecțiile consumerului, fără modificarea aplicației între aceste probe.

Intrările și responsabilitățile testelor sunt în [README-ul Android](../atf-test-android/README.md).
Build-ul și Maven rulează în staging TMP; serialul, profilul, folderul de probe
și numărul de mâini se declară explicit. APK-ul din `dist` se înlocuiește numai
după compararea hash-ului cu binarul instalat, verificat și cu sursele curente.

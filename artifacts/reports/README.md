# Dovezile de testare atașate release-ului Whist Lab

Directorul păstrează **numai cele mai recente rezultate relevante** care documentează APK-ul curent. Rapoartele, capturile, rezultatele automate și proveniența binarului formează un singur pachet de acceptanță.

La un release nou, înlocuiți pachetul și actualizați acest registru. Build-urile și istoricul complet rămân în `%TEMP%` și în depozitul extern; un diagnostic negativ se păstrează aici când explică o corecție și reverificarea ei.

## APK documentat

| Proprietate | Valoare |
|---|---|
| Binar | [WhistLab-0.1.9.apk](../../dist/WhistLab-0.1.9.apk) |
| Versiune | `0.1.9`, `versionCode 10` |
| Mărime | 771.391 bytes; fișierul de 14 bytes cu `404: Not Found` nu este un APK. |
| Platformă | `minSdk 24`, `targetSdk 36`, `compileSdk 36` |
| SHA-256 | `811e119e9dc3e5190c49b4ef38973e1890174f9e5b77d8a8961e15611aba4066` |
| Semnare | Keystore Android local de dezvoltare; semnături v2/v3 și aliniere verificate. |

APK-ul release este destinat instalării directe; cheia nu este una de publicare Google Play. [Manifestul release-ului](WhistLab-0.1.9/release-validation.json) leagă binarul, testele, restaurarea datelor și limitele verificării.

[Descarcă APK-ul din GitHub Release](https://github.com/crististefanita/atf_j4/releases/download/v0.1.9/WhistLab-0.1.9.apk) · [SHA-256 publicat](https://github.com/crististefanita/atf_j4/releases/download/v0.1.9/WhistLab-0.1.9.apk.sha256). Copia locală este publicată atomic după verificarea CRC; descărcarea asset-ului este confruntată separat cu binarul original.

## Rapoarte Extent

| Dovadă | Raport | Domeniu demonstrat |
|---|---|---|
| Sanity live 0.1.9 — PASS | [Masa compactă](WhistLab-0.1.9/compact-table/responsive-ui-sanity.html) | 4 checkpoint-uri, 39 de aserții: 13 cărți, levată completă cu 12 rămase, 9 licitații și selecția lui 8 fără deplasarea chenarelor. |
| Sanity live API 36 — PASS | [Masa compactă cu protecția ferestrei](WhistLab-0.1.9/api36-table/responsive-ui-sanity.html) | Aceleași patru stări, pe noul emulator identificat separat; acțiunile, comparațiile și capturile aparțin acestei execuții. |
| Diagnostic API 36 inițial — FAIL | [Oracolul fizic anterior](WhistLab-0.1.9/api36-initial-diagnostic/responsive-ui-sanity.html) | Păstrează respingerea rasterului logic complet și diagnosticul inițial, separat de acceptanța corectată. |
| Reper 0.1.8, republicat offline | [Meniu și masă pe două profiluri](WhistLab-0.1.9/baseline-0.1.8-matrix/responsive-ui-sanity-replayed.html) | 20 de checkpoint-uri și 160 de comparații trecute; acțiunile, capturile și timpii provin din execuția live 0.1.8. |

Sanity-urile curente folosesc `720x1280@360` pe API 28 și API 36, pe dispozitive distincte. Reperul 0.1.8 acoperă și `1080x1920@440`; nu reprezintă o nouă execuție pe APK-ul 0.1.9.

Publicarea inițială a matricei 0.1.8 a eșuat deoarece sumarul unei aserții depășea limita efectivă de 240 de caractere. Rezultatul JUnit inițial este **ERROR**, distinct de comparațiile UI trecute. Replay-ul păstrează trace-ul original și valorile complete în observații expandabile, fără acțiuni ADB. Publicarea sanity-ului curent a trecut direct.

Capturile esențiale ale versiunii 0.1.9:

- [13 cărți și toate chenarele](WhistLab-0.1.9/compact-table/720x1280-360dpi-See-thirteen-hand-cards-and-every-seat-without-scrolling.png).
- [Levată completă, 12 cărți și toate chenarele](WhistLab-0.1.9/compact-table/720x1280-360dpi-See-all-four-trick-cards,-twelve-hand-cards-and-four-seats-without-scrolling.png).
- [Nouă opțiuni de licitare](WhistLab-0.1.9/compact-table/720x1280-360dpi-nine-bid-controls.png).
- [Licitația selectată, cu poziții fixe](WhistLab-0.1.9/compact-table/720x1280-360dpi-Select-bid-eight-without-moving-player-frames.png).

## Verificarea release-ului

| Nivel | Rezultat |
|---|---|
| Aplicație JVM | 74 de teste: 73 trecute, 1 skipped, fără eșecuri sau erori. Include geometria compactă, fixture-ul legal și patru teste pentru calculul padding-ului și al spațiului util. Replay-ul opțional al istoricului nu a primit arhiva pilotului și este raportat separat ca skipped. |
| Componente ATF | 38/38 trecute: profiluri, driver, publisher, oracole raster și geometrie. Include șase teste pentru modurile de captură și refuzul tăierii ori scalării arbitrare. Numărul reprezintă teste distincte, fără dublarea rerulărilor. |
| Cazuri negative | Chenar lipsă sau tăiat, carte tăiată cu un pixel, centrare greșită, suprapunere, antet sau levată lipsă și scroll accidental. |
| Audit offline al capturilor | 44/44 comparații trecute pe patru ierarhii native 0.1.8; transportul aruncă eroare la orice operație ADB. Proveniența fiecărui XML este păstrată. |
| Sanity live final | Câte un E2E trecut pe API 28 și API 36, cu câte 4 checkpoint-uri și 39 de aserții; fără scroll vertical și fără intersecții. |
| Build în TMP | Testele aplicației, `assembleDebug`, `assembleRelease` și `lintVitalRelease` au trecut. |
| Smoke release | Upgrade pe API 28 și API 36, activitate principală reluată, proces viu și fără excepție AndroidRuntime pentru procesul verificat; hash-ul instalat coincide cu APK-ul livrat. |
| Datele utilizatorului | Cele 3 fișiere private de pe API 28 și salvarea proprie emulatorului API 36 au fost restaurate și comparate prin hash înaintea smoke-urilor release. Interfețele au fost închise și rezervările eliberate. |
| Sync live | Sursele și configurația aplicației sunt confruntate în manifestul de proveniență; bibliotecile executate coincid cu JAR-urile locale. Selecția JVM adăugată în paralel a fost preluată și release-ul reconstruit, cu APK unsigned identic. |

`test-results/`, `logs/`, [proveniența surselor](WhistLab-0.1.9/source-provenance.json), [sincronizarea configurației](WhistLab-0.1.9/build-config-sync.json) și fișierele de proveniență ale APK-urilor susțin verificarea separată a afirmațiilor. Prima pornire Maven a fost blocată înaintea testelor de un JAR deschis în cache-ul comun; execuția finală a trecut folosind coordonate ATF private.

Prima probă API 36 a respins un PNG nativ logic complet deoarece oracolul cerea exclusiv dimensiunile fizice. Corecția acceptă numai rasterul fizic exact sau rasterul logic activ exact din `wm size`; tăierea cu un pixel rămâne respinsă. Proba negativă și reverificarea sunt păstrate separat. Smoke-ul recunoaște și `topResumedActivity`, denumirea observată pe API 36.

[Snapshot-ul testelor executate](WhistLab-0.1.9/test-source-provenance.json) separă sursele din TMP de refactorările paralele ale suportului live; acestea nu primesc implicit calificarea execuției de release.

## Pregătirea pentru Android nou

Protecția pentru barele sistemului, decupaj și tastatură este implementată pentru API 35+, iar calculul spațiului util folosește `WindowMetrics` de la API 30. Componentele verifică matematica și lipsa acumulării padding-ului. Sanity-ul API 36 verifică nativ masa compactă; tastatura, alte moduri de navigare și decupajele diferite rămân acceptanțe separate.

SDK-urile păstrează contractul versiunii 0.1.5 raportate funcționale de utilizator. **API 37 nu a fost testat**, conform cerinței acestei iterații. Nu s-au modificat limitele de memorie ale sistemului și nu se atribuie vreun crash acestora fără diagnostic. Acceptanța nativă rămâne în [feedback-ul Android](../../docs/feedback/ATF_Android.md).

## Limite

- E2E-ul folosește APK-ul debug identificat prin hash și fixture-uri protejate de `BuildConfig.DEBUG`; APK-ul release primește separat smoke de instalare și pornire.
- Emulatoarele API 28 și API 36 sunt identificate separat; override-urile logice nu reprezintă un test pe ROG 7 fizic.
- Iterația califică afișarea și controalele selectate; nu joacă o partidă completă și nu califică randarea nativă a textului mărit.
- HTML-ul, structura expandabilă și imaginile inline au fost verificate static, iar capturile au fost inspectate vizual. Expand/collapse-ul interactiv nu este confirmat: browserul conectat a respins protocolul `file://`.

[Contractul profilurilor](../../docs/testing/ATF_Display.md) · [Livrarea Android](../../docs/platform/ATF_AndroidDelivery.md) · [Feedback activ](../../docs/feedback/ATF_Android.md)

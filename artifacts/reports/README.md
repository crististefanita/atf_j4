# Dovezile de testare atașate release-ului Whist Lab

Directorul păstrează **numai cele mai recente rezultate relevante** care documentează APK-ul curent. Rapoartele, capturile, rezultatele automate și proveniența binarului formează un singur pachet de acceptanță.

La un release nou, înlocuiți pachetul și actualizați acest registru. Build-urile și istoricul complet rămân în `%TEMP%` și în depozitul extern; un diagnostic negativ se păstrează aici când explică o corecție și reverificarea ei.

## APK documentat

| Proprietate | Valoare |
|---|---|
| Binar | [WhistLab-0.1.8.apk](../../dist/WhistLab-0.1.8.apk) |
| Versiune | `0.1.8`, `versionCode 9` |
| Platformă | `minSdk 24`, `targetSdk 36`, `compileSdk 36` |
| SHA-256 | `9a22a5cec0ad3cf0f6a18d87fd7ab0fd09e8c94fc2cbd9b1a2e81fd3711a8248` |
| Semnare | Keystore Android local de dezvoltare; semnături v2/v3 și aliniere verificate. |

APK-ul release este destinat instalării directe; cheia nu este una de publicare Google Play. [Manifestul release-ului](WhistLab-0.1.8/release-validation.json) leagă binarul, testele, restaurarea datelor și limitele verificării.

## Rapoarte Extent

| Dovadă | Raport | Domeniu demonstrat |
|---|---|---|
| Sanity live compact — PASS | [Masa compactă](WhistLab-0.1.8/compact-table/responsive-ui-sanity.html) | 4 checkpoint-uri, 35 de aserții: 13 cărți, levată completă cu 12 rămase, 9 licitații și selecția lui 8 fără deplasarea chenarelor. |
| Matrice capturată, republicată offline | [Meniu și masă pe două profiluri](WhistLab-0.1.8/responsive-matrix-replay/responsive-ui-sanity-replayed.html) | 20 de checkpoint-uri și 160 de comparații trecute; acțiunile, capturile și timpii provin din execuția live inițială. |

Matricea folosește `720x1280@360` și `1080x1920@440`. Publicarea inițială a acesteia a eșuat deoarece sumarul unei aserții depășea 240 de caractere; rezultatul JUnit inițial este **ERROR**, distinct de comparațiile UI trecute.

Replay-ul folosește trace-ul original nemodificat și păstrează valorile lungi în observații expandabile; nu execută acțiuni ADB. Consumerul live produce acum rezumate scurte, iar sanity-ul compact repetat a trecut inclusiv publicarea și auditul HTML.

Capturile esențiale ale sanity-ului final:

- [13 cărți și toate chenarele](WhistLab-0.1.8/compact-table/720x1280-360dpi-See-thirteen-hand-cards-and-every-seat-without-scrolling.png).
- [Levată completă, 12 cărți și toate chenarele](WhistLab-0.1.8/compact-table/720x1280-360dpi-See-all-four-trick-cards,-twelve-hand-cards-and-four-seats-without-scrolling.png).
- [Licitația selectată, cu poziții fixe](WhistLab-0.1.8/compact-table/720x1280-360dpi-Select-bid-eight-without-moving-player-frames.png).

## Verificarea release-ului

| Nivel | Rezultat |
|---|---|
| Aplicație JVM | 56 de teste: 55 trecute, 1 skipped, fără eșecuri sau erori. Include bugetul ferestrei compacte, intersecțiile și fixture-ul obținut prin joc legal. |
| Componente ATF | 21/21 trecute: profiluri, driver, publisher și oracolul raster, inclusiv fundal diferit și text în punctul vechi de eșantionare. |
| Sanity live final | 1 test E2E trecut pe profilul compact; fără scroll vertical, fără intersecții și cu selecție verificată. |
| Build în TMP | Testele aplicației, `assembleDebug`, `assembleRelease` și `lintVitalRelease` au trecut. |
| Smoke release | Upgrade pe API 28, activitate principală reluată, proces viu și fără excepție AndroidRuntime pentru procesul verificat; hash-ul instalat coincide cu APK-ul livrat. |
| Datele utilizatorului | Cele 3 fișiere private au fost restaurate și comparate prin hash înaintea instalării și smoke-ului release; nu s-au șters datele aplicației. |
| Sync live | Cele 69 de fișiere de sursă/configurație incluse în verificarea build-ului coincid cu live; bibliotecile executate coincid cu JAR-urile locale. |

Directoarele rapoartelor conțin PNG-uri, XML-uri UIAutomator și trace-urile ordonate. `test-results/`, `logs/`, fișierele de proveniență și `source-provenance.json` susțin verificarea separată a afirmațiilor.

## Limite

- E2E-ul folosește APK-ul debug identificat prin hash și fixture-uri protejate de `BuildConfig.DEBUG`; APK-ul release primește separat smoke de instalare și pornire.
- Emulatorul este API 28, cu raster fizic `1080x1920`; override-urile logice nu reprezintă un test pe ROG 7 fizic.
- Iterația califică afișarea și controalele selectate; nu joacă o partidă completă și nu califică textul mărit.
- HTML-ul, structura expandabilă și imaginile inline au fost verificate static, iar capturile au fost inspectate vizual. Expand/collapse-ul interactiv nu este confirmat: browserul conectat a respins protocolul `file://`.

[Contractul profilurilor](../../docs/testing/ATF_Display.md) · [Livrarea Android](../../docs/platform/ATF_AndroidDelivery.md) · [Feedback activ](../../docs/feedback/ATF_Android.md)
# Dovezile de testare atașate release-ului Whist Lab

Directorul păstrează **numai cele mai recente rezultate relevante** care justifică APK-ul curent. Este un pachet de dovezi pentru release, nu o arhivă a tuturor execuțiilor.

La publicarea unei versiuni noi, înlocuiți pachetul de versiune cu rapoartele finale trecute și actualizați acest registru. Build-urile intermediare și rulările istorice rămân în `%TEMP%` și în `C:\work\whist_back\ui-reports\whist-lab\runs\`; nu se copiază aici rezultate eșuate sau fără legătură cu APK-ul livrat.

## APK documentat

| Proprietate | Valoare |
|---|---|
| Binar | [WhistLab-0.1.7.apk](../../dist/WhistLab-0.1.7.apk) |
| Versiune | `0.1.7` (`versionCode 8`) |
| Compatibilitate | `minSdk 24`, `targetSdk 36` |
| SHA-256 APK | `14e88358e342ee5e8a123389a88e7ec1908d3a178c43b3381c216642f47c0b9b` |
| Certificat | SHA-256 `b80c45a9b52ef0c83dcb929ba4aeb8775c836e5e6d587324d7ec725c079fb4c3` |

Pachetul este semnat local cu keystore-ul Android de dezvoltare pentru instalare directă și smoke test. Nu este semnătura de publicare Google Play. Metadatele de build, semnătură și instalare sunt în [release-validation.json](WhistLab-0.1.7/release-validation.json).

## Rapoarte Extent

| Scop | Raport | Conținut verificat |
|---|---|---|
| Meniu și layout responsive | [responsive-ui-sanity.html](WhistLab-0.1.7/responsive-ui-sanity/responsive-ui-sanity.html) | Home fără overflow, clasament, configurare, atu și rama jucătorului activ, locurile, mâna de 13 cărți și cele 9 bid-uri. 16 checkpoint-uri și 116 aserții pe două profile. |
| Cazuri dense | [dense-layout.html](WhistLab-0.1.7/dense-layout/dense-layout.html) | Indicii celor 13 cărți, țintele pentru 9 bid-uri, lipsa intersecțiilor, distanța față de locul uman și stabilitatea locurilor după selectarea unui bid. 4 checkpoint-uri și 35 aserții. |

Fiecare director de raport conține HTML-ul Extent, capturile PNG, ierarhiile UIAutomator XML și proprietățile serializate ale trace-ului. Rapoartele leagă acțiunea, observația, comparația și captura; XML-urile JUnit/Surefire și logurile de build permit verificarea automată a rezultatelor.

## Matricea executată

| Verificare | Rezultat |
|---|---|
| `:app:testDebugUnitTest` | 54 teste; 53 trecute, 0 eșecuri, 0 erori, 1 skipped. XML-urile sunt în `WhistLab-0.1.7/test-results/app-unit/`. |
| Preflight ATF | 18/18 trecute: 5 profiluri responsive, 4 verificări driver și 9 verificări ale publisherului Extent. Logul și XML-urile sunt păstrate. |
| E2E responsive | 1 test trecut; `720x1280@360` și `1080x2448@440`; raport Extent `PASS`. |
| E2E DenseLayout | 1 test trecut; `720x1280@360`; 13 cărți, 9 bid-uri și selecția bid-ului 8; raport Extent `PASS`. |
| Smoke APK release | Instalare upgrade și pornire pe emulator API 28; activitatea principală a pornit, hash-ul APK-ului instalat coincide cu binarul de mai sus, fără excepție AndroidRuntime în verificarea imediată. Datele aplicației nu au fost șterse. |
| Build release | `:app:assembleDebug`, `:app:assembleRelease` și `lintVitalRelease` au trecut; build-ul a rulat în `%TEMP%`. |

### Limitele dovezii

- Profilele de rezoluție sunt override-uri **logice** ale emulatorului. Capturile păstrează rasterul fizic raportat de acesta: `1080x1920`. Pentru profilul logic `1080x2448@440`, emulatorul scalează conținutul în suprafața fizică, ceea ce produce margini negre laterale. Acest profil verifică geometria logică și nu dovedește randarea pe un panou fizic `1080x2448`.
- Pe profilul compact `720x1280@360`, checkpoint-ul pentru stilul cărții centrale și cel pentru toate locurile sunt separate. Captura înainte de scroll lasă textul locului uman la marginea de jos (`responsive-played-card-style.xml`, `y=1170–1172`); checkpoint-ul următor îl expune după scroll și verifică distanțele. Dovezile atestă accesul prin viewport-uri controlate, nu vizibilitatea simultană a mâinii, levatei și tuturor ramelor. Lacuna este înregistrată în [feedback-ul Android](../../docs/feedback/ATF_Android.md).
- Verificarea s-a făcut pe emulatorul API 28 din VM, nu pe un ROG 7 fizic.
- E2E-urile folosesc fixture-uri debug pentru ecrane și nu joacă o partidă completă. APK-ul release a primit separat numai smoke de instalare și pornire.
- Deschiderea interactivă a HTML-ului Extent nu a putut fi verificată în browser: politica browserului conectat a respins protocolul local `file://`. HTML-ul, starea `PASS`, numărul checkpoint-urilor/aserțiilor și capturile inline au fost verificate static, iar PNG-urile atașate au fost inspectate separat; comportamentul expand/collapse în browser rămâne neconfirmat.
- `:game-core:test` nu a fost validat integral pentru acest release. O execuție anterioară întreruptă a raportat eșecuri AI în `CertifiedContractRegressionTest`, `PublicSalvageTest` și `PromotedMasterLossTest`; rezultatul nu este prezentat drept trecere.

Logurile de build și XML-urile de test din acest pachet sunt suport pentru audit; execuțiile și capturile neincluse rămân în directorul extern al rulărilor. Procedura și limitele profilelor sunt descrise în [ATF_Display.md](../../docs/testing/ATF_Display.md) și [ATF_AndroidDelivery.md](../../docs/platform/ATF_AndroidDelivery.md).
